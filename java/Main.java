package junior.databases.homework;

import junior.databases.homework.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    private static Connection connection = null;

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        //http://java-course.ru/student/book1/database/
        initDatabase();

        System.out.println("initDatabase!");
        Entity.setDatabase(connection);
        Section sect = new Section(46);
        Section sect2 = new Section();

        System.out.println("Id: " + sect.getId() + ": Title" + sect.getTitle());
        System.out.println("Created: " + sect.getCreated() + ": Title" + sect.getTitle());
        System.out.println("Updated: " + sect.getUpdated() + ": Title" + sect.getTitle());

        sect.setTitle("News43");
        sect2.setTitle("News3");
        sect2.save();
        sect.save();
        //sect2.delete();
        for ( Section section : Section.all() ) {
            System.out.println("Id: " + section.getId() + " Title: " + section.getTitle());
            System.out.println("Created: " + section.getCreated() + ": Title: " + section.getTitle());
            System.out.println("Updated: " + section.getUpdated() + ": Title: " + section.getTitle());

            if ( section.getId() > 5 ) {
                //section.delete();
            }
        }
/*
        for ( Post post : Post.all() ) {
            System.out.println(post.getId() + ": " + post.getTitle());

            for ( Tag tag : post.getTags() ) {
                System.out.println("  " + tag.getName());

                for ( Post p : tag.getPosts() ) {
                    System.out.println("    " + p.getId() + ": " + p.getTitle());
                }
            }
        }
*/
    }

    private static void initDatabase() throws SQLException, ClassNotFoundException {

        Class.forName("org.postgresql.Driver");

        String url = "jdbc:postgresql://localhost:2323/copydb";
        String name = "copydb";
        String password = "12345";
        
        connection = DriverManager.getConnection(url,name,password);
    }
}
