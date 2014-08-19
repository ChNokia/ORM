package junior.databases.homework;

import com.sun.deploy.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.sql.*;
import java.lang.reflect.Constructor;

public abstract class Entity {
    private static String DELETE_QUERY   = "DELETE FROM \"%1$s\" WHERE %1$s_id=?";
    private static String INSERT_QUERY   = "INSERT INTO \"%1$s\" (%2$s) VALUES (%3$s) RETURNING %1$s_id";
    private static String LIST_QUERY     = "SELECT * FROM \"%s\"";
    private static String SELECT_QUERY   = "SELECT * FROM \"%1$s\" WHERE %1$s_id=?";
    private static String CHILDREN_QUERY = "SELECT * FROM \"%1$s\" WHERE %2$s_id=?";
    private static String SIBLINGS_QUERY = "SELECT * FROM \"%1$s\" NATURAL JOIN \"%2$s\" WHERE %3$s_id=?";
    private static String UPDATE_QUERY   = "UPDATE \"%1$s\" SET %2$s WHERE %1$s_id=?";

    private static Connection db = null;

    protected boolean isLoaded = false;
    protected boolean isModified = false;
    private String table = null;
    private Integer id = null;
    protected Map<String, Object> fields = new HashMap<String, Object>();

    private Set<String> changeFields = new HashSet<String>();
    private static String templateId = "%1$s_id";
    private static String templateLeftRight = "%1$s_%2$s";

    public Entity() {
        table = this.getClass().getSimpleName().toLowerCase();
    }

    public Entity(Integer id) {
        this();
        this.id = id;
    }

    public static final void setDatabase(Connection connection) {
        // throws NullPointerException
        if ( connection == null ) {
            throw new NullPointerException();
        }

        db = connection;
    }

    public final int getId() {
        // try to guess youtself
        return id;
    }

    public final java.util.Date getCreated() {
        // try to guess youtself
        return getDate(String.format(templateLeftRight, table, "created"));
    }

    public final java.util.Date getUpdated() {
        // try to guess youtself
        return getDate(String.format(templateLeftRight, table, "updated"));
    }

    public final Object getColumn(String name) {
        // return column name from fields by key
        load();

        return fields.get(String.format(templateLeftRight, table, name));
    }

    public final <T extends Entity> T getParent(Class<T> cls) {
        // get parent id from fields as <classname>_id, create and return an instance of class T with that id
        load();

        String className = cls.getSimpleName().toLowerCase();
        Integer parent_id = (Integer)fields.get(String.format(templateId, className));
        T target = null;

        try {
            Constructor<T> constructTarget = cls.getConstructor(Integer.class);
            target = constructTarget.newInstance(parent_id);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }

        return target;
    }

    public final <T extends Entity> List<T> getChildren(Class<T> cls) throws SQLException {
        // select needed rows and ALL columns from corresponding table
        // convert each row from ResultSet to instance of class T with appropriate id
        // fill each of new instances with column data
        // return list of children instances
        Statement statement = db.createStatement();
        String nameTable = cls.getSimpleName().toLowerCase();

        statement.execute(String.format(CHILDREN_QUERY, nameTable, table));

        ResultSet resultSet = statement.getResultSet();

        return rowsToEntities(cls, resultSet);
    }

    public final <T extends Entity> List<T> getSiblings(Class<T> cls) throws SQLException {
        // select needed rows and ALL columns from corresponding table
        // convert each row from ResultSet to instance of class T with appropriate id
        // fill each of new instances with column data
        // return list of sibling instances
        //"SELECT * FROM \"%1$s\" NATURAL JOIN \"%2$s\" WHERE %3$s_id=?";
        Statement statement =  db.createStatement();
        String leftTab = cls.getSimpleName().toLowerCase();

        statement.execute(String.format(SIBLINGS_QUERY, leftTab, getJoinTableName(leftTab, table), table));

        ResultSet resultSet = statement.getResultSet();

        return rowsToEntities(cls, resultSet);
    }

    public final void setColumn(String name, Object value) {
        // put a value into fields with <table>_<name> as a key
        String key = String.format(templateLeftRight, table, name);

        fields.put(key, value);
        changeFields.add(key);

        isModified = true;
    }

    public final void setParent(String name, Integer id) {
        // put parent id into fields with <name>_<id> as a key
        String columnName = String.format(templateId, name);

        fields.put(columnName, id);
    }

    private void load() {
        // check, if current object is already loaded
        // get a single row from corresponding table by id
        // store columns as object fields with unchanged column names as keys
        if ( isLoaded ) {
            return;
        }
        if ( id == 0 ) {
            throw new NullPointerException();
        }

        try (PreparedStatement ps = db.prepareStatement(String.format(SELECT_QUERY, table))) {

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();
            ResultSetMetaData resultMetaData = rs.getMetaData();
            int numberOfColumns = resultMetaData.getColumnCount();

            rs.next();

            for (int i = 0; i < numberOfColumns; i++) {
                String columnName = resultMetaData.getColumnName(i + 1);

                fields.put(columnName, rs.getObject(columnName));
            }

            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        isLoaded = true;
    }

    private void insert() {
        // execute an insert query, built from fields keys and values
        int numKeys = changeFields.size();

        if ( numKeys > 0 ) {
            String parameters = join(changeFields);
            String values = join(genPlaceholders(numKeys));
            PreparedStatement preparedStatement = null;

            try {
                preparedStatement = db.prepareStatement(String.format(INSERT_QUERY, table, parameters, values));
                int number = 1;

                for( String key : changeFields ) {
                    preparedStatement.setObject(number, fields.get(key));

                    number += 1;
                }
                preparedStatement.execute();

                ResultSet resultSet = preparedStatement.getResultSet();

                if (resultSet.next()) {
                    id = resultSet.getInt(1);
                }

                resultSet.close();
                preparedStatement.close();
                changeFields.clear();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() throws SQLException {
        // execute an update query, built from fields keys and values
        int numKeys = changeFields.size();

        if ( numKeys > 0 ) {
            String values = null;
            String values1 = join(changeFields, "=?");
            String template = "%1$s=\'%2$s\'";
            Collection<String> dataList = new ArrayList<String>();

            for( String key : changeFields ) {
                dataList.add(String.format(template, key, fields.get(key)));
            }

            values = join(dataList);

            try ( PreparedStatement ps = db.prepareStatement(String.format(UPDATE_QUERY, table, values)) ) {
                ps.setInt(1, id);
                ps.execute();
                changeFields.clear();
            }
        }
    }

    public final void delete() throws SQLException {
        // execute a delete query with current instance id
        PreparedStatement ps = null;
        try {
            ps = db.prepareStatement(String.format(DELETE_QUERY, table));
            ps.setInt(1, id);
            ps.execute();
        } finally {
            if (ps != null) {
                ps.close();
                changeFields.clear();
                fields.clear();

                id = null;
                isModified = false;
                isLoaded = false;
            }
        }
    }

    public final void save() throws SQLException {
        // execute either insert or update query, depending on instance id
        if ( isLoaded ) {
            update();
        } else {
            insert();
        }

        isModified = false;
        isLoaded = true;
    }

    protected static <T extends Entity> List<T> all(Class<T> cls) throws SQLException {
        // select ALL rows and ALL columns from corresponding table
        // convert each row from ResultSet to instance of class T with appropriate id
        // fill each of new instances with column data
        // aggregate all new instances into a single List<T> and return it
        Statement statement = db.createStatement();
        String tableName = cls.getSimpleName().toLowerCase();

        statement.execute(String.format(LIST_QUERY, tableName));

        ResultSet resultSet = statement.getResultSet();

        return rowsToEntities(cls, resultSet);
    }

    private static Collection<String> genPlaceholders(int size) {
        // return a string, consisting of <size> "?" symbols, joined with ", "
        // each "?" is used in insert statements as a placeholder for values (google prepared statements)
        return genPlaceholders(size, "?");
    }

    private static Collection<String> genPlaceholders(int size, String placeholder) {
        // return a string, consisting of <size> <placeholder> symbols, joined with ", "
        // each <placeholder> is used in insert statements as a placeholder for values (google prepared statements)
        List<String> resultString = new ArrayList<String>();
        int last = size - 1;
        String filString = String.format("%1$s, ", placeholder);

        for ( int i = 0; i < last; i++ ) {
            resultString.add(filString);
        }

        resultString.add(placeholder);

        return resultString;
    }

    private static String getJoinTableName(String leftTable, String rightTable) {
        // generate the name of associative table for many-to-many relation
        // sort left and right tables alphabetically
        // return table name using format <table>__<table>
        if ( leftTable.compareTo(rightTable) < 0 ) {
            return String.format(templateLeftRight, leftTable, rightTable);
        }

        return String.format(templateLeftRight, rightTable, leftTable);
    }

    private java.util.Date getDate(String column) {
        // pwoerful method, used to remove copypaste from getCreated and getUpdated methods
        load();

        return new java.util.Date((Integer)fields.get(column));
    }

    private static String join(Collection<String> sequence) {
        // join collection of strings with ", " as glue and return a joined string
        return join(sequence, ", ");
    }

    private static String join(Collection<String> sequence, String glue) {
        // join collection of strings with glue and return a joined string
        return StringUtils.join(sequence, glue);
    }

    private static <T extends Entity> List<T> rowsToEntities(Class<T> cls, ResultSet rows) {
        // convert a ResultSet of database rows to list of instances of corresponding class
        // each instance must be filled with its data so that it must not produce additional queries to database to get it's fields
        List<T> resultList = new ArrayList<T>();

        try {
            ResultSetMetaData resultMetaData = rows.getMetaData();
            int numberOfColumns = resultMetaData.getColumnCount();
            Constructor<T> constructTarget = cls.getConstructor(Integer.class);
            String columnId = cls.getSimpleName().toLowerCase() + "_id";

            while ( rows.next() ) {
                T target = null;
                target = constructTarget.newInstance(rows.getInt(columnId));

                for ( int i = 0; i < numberOfColumns; i++ ) {
                    String columnName = resultMetaData.getColumnName(i + 1);

                    if ( target != null ) {
                        target.fields.put(columnName, rows.getObject(columnName));
                    }
                }

                resultList.add(target);
            }
        } catch ( ReflectiveOperationException | SQLException e ) {
            e.printStackTrace();
        }

        return resultList;
    }
}
