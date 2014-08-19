import yaml

table = '''CREATE TABLE "{0}" (\n    "{0}_id" SERIAL PRIMARY KEY{1}{2}\n);\n\n'''

linking_table = '''CREATE TABLE "{0}__{1}" (
    "{0}_id" INTEGER NOT NULL,
    "{1}_id" INTEGER NOT NULL,
    PRIMARY KEY ("{0}_id", "{1}_id")
);\n\n'''

many_to_one = '''ALTER TABLE "{0}" ADD "{1}_id" INTEGER NOT NULL,
    ADD CONSTRAINT "fk_{0}_{1}_id" FOREIGN KEY ("{1}_id") REFERENCES "{1}" ("{1}_id");\n\n'''

many_to_many = '''ALTER TABLE "{0}"
    ADD CONSTRAINT "fk_{0}_{1}_id" FOREIGN KEY ("{1}_id") REFERENCES "{1}" ("{1}_id");\n\n'''

timestamps = ''',\n    "{0}_created" timestamp NOT NULL DEFAULT now(),
    "{0}_updated" timestamp NOT NULL DEFAULT now()'''
    
update_timestamp = '''CREATE OR REPLACE FUNCTION update_{0}_timestamp()
RETURNS TRIGGER AS $$
BEGIN
   NEW.{0}_updated = now();
   RETURN NEW;
END;
$$ language 'plpgsql';
CREATE TRIGGER "tr_{0}_updated" BEFORE UPDATE ON "{0}" FOR EACH ROW EXECUTE PROCEDURE update_{0}_timestamp();\n\n'''

class Generator(object):
    def __init__(self):
        self._alters   = set()
        self._tables   = set()
        self._triggers = set()
        self._schema_db = {}
        self._fields = {}
        self._relations = {}

    def __build_tables(self):
        for entity_name, entity_definition in self._fields.items():
            colunms = self.__build_columns(entity_definition)
            fields = ''.join([',\n    "{0}_{1}" {2}'.format(entity_name, field, type) for field, type in colunms])

            self._tables.add(table.format(entity_name, fields, timestamps.format(entity_name)))
            self._triggers.add(entity_name)

    def __build_columns(self, entity):
        columns = [(field.lower(), type) for field, type in entity]
        
        return columns;
    
    def __build_relations(self):
        entitys_relations = {}

        for entity_name, entity_definition in self._relations.items():
            entitys_relations[entity_name] = self.__build_columns(entity_definition)#[(parent.lower(), type) for parent, type in entity_definition['relations'].items()]
        for owner, relations in entitys_relations.items():
            for child in relations:
                for owner2 in entitys_relations[child[0]]:
                    if owner == owner2[0]:
                        if child[1] == 'one':
                            self._alters.add(self.__build_many_to_one(owner, child[0]))
                        if owner2[1] == 'one':
                            self._alters.add(self.__build_many_to_one(child[0], owner))
                        if child[1] == 'many' and owner2[1] == 'many':
                            table_name = [child[0], owner]
                            
                            table_name.sort()
                            self._tables.add(linking_table.format(table_name[0], table_name[1]))
                            self._alters.add(self.__build_many_to_many(child[0], owner))
                            self._alters.add(self.__build_many_to_many(owner, child[0]))


        '''
        keys = list(entitys_relations.keys())
        count_key = len(keys)

        for i in range(count_key):
            for j in range(i + 1, count_key):
                owner1_relation = keys[i]
                owner2_relation = keys[j]

                for relation1 in entitys_relations[owner1_relation]:
                    if (owner2_relation in relation1):
                        for relation2 in entitys_relations[owner2_relation]:
                            if (owner1_relation in relation2):
                                if relation1[1] == 'one':
                                    self._alters.add(self.__build_many_to_one(owner1_relation, owner2_relation))
                                if relation2[1] == 'one':
                                    self._alters.add(self.__build_many_to_one(owner2_relation, owner1_relation))
                                if relation1[1] == 'many' and relation2[1] == 'many':
                                    table_name = [owner2_relation, owner1_relation]
                                    
                                    table_name.sort()
                                    self._tables.add(linking_table.format(table_name[0], table_name[1]))
                                    self._alters.add(self.__build_many_to_many(owner2_relation, owner1_relation))
                                    self._alters.add(self.__build_many_to_many(owner1_relation, owner2_relation))
'''
    def __build_many_to_one(self, child_entity, parent_entity):
        return many_to_one.format(child_entity, parent_entity)
    
    def __build_many_to_many(self, left_entity, right_entity):
        table_name = [left_entity, right_entity]
        
        table_name.sort()
        
        return many_to_many.format('_'.join(table_name), right_entity)
    
    def __build_triggers(self):
        triggers = []
        
        for trigger in self._triggers:
            triggers.append(update_timestamp.format(trigger))
        
        return triggers;

    def build_ddl(self, yaml_filename):
        with open(yaml_filename, "r") as fileIn:
            self._schema_db = yaml.safe_load(fileIn)

        for entity_name, entity_definition in self._schema_db.items():
            entity_name = entity_name.lower()
            self._fields[entity_name] = entity_definition['fields'].items()
            self._relations[entity_name] = entity_definition['relations'].items()

        self.__build_tables()
        self.__build_relations()
        '''    entitys_relations = {}
            
        for entity_name, entity_definition in self._schema_db.items():
            entity_name = entity_name.lower()
            colunms = self.__build_columns(entity_definition['fields'].items())
            fields = ''.join([',\n    "{0}_{1}" {2}'.format(entity_name, field, type) for field, type in colunms])
            entitys_relations[entity_name] = [(parent.lower(), type) for parent, type in entity_definition['relations'].items()]
            
            self._tables.add(table.format(entity_name, fields, timestamps.format(entity_name)))
            self._triggers.add(entity_name)
        
        keys = list(entitys_relations.keys())
        count_key = len(keys)
        
        for i in range(count_key):
            for j in range(i + 1, count_key):
                owner1_relation = keys[i]
                owner2_relation = keys[j]
                
                for relation1 in entitys_relations[owner1_relation]:
                    if (owner2_relation in relation1):
                        for relation2 in entitys_relations[owner2_relation]:
                            if (owner1_relation in relation2):
                                if relation1[1] == 'one':
                                    self._alters.add(self.__build_many_to_one(owner1_relation, owner2_relation))
                                if relation2[1] == 'one':
                                    self._alters.add(self.__build_many_to_one(owner2_relation, owner1_relation))
                                if relation1[1] == 'many' and relation2[1] == 'many':
                                    table_name = [owner2_relation, owner1_relation]
                                    
                                    table_name.sort()
                                    self._tables.add(linking_table.format(table_name[0], table_name[1]))
                                    self._alters.add(self.__build_many_to_many(owner2_relation, owner1_relation))
                                    self._alters.add(self.__build_many_to_many(owner1_relation, owner2_relation))
'''
    def clear(self):
    	self.__init__()
        #self._alters.clear()
        #self._tables.clear()
        #self._triggers.clear()

    def dump(self, sql_filename):
        tables = []
        relations = []

        for table1 in self._tables:
            tables.append(table1)
        for relation in self._alters:
            relations.append(relation)

        with open(sql_filename, "w") as fileOut:
            fileOut.write(''.join(tables))
            fileOut.write(''.join(relations))
            fileOut.write(''.join(self.__build_triggers()))
        
if __name__ == "__main__":
    g = Generator()

    g.build_ddl('schema.yaml')
    g.dump('schema3.sql')

    #g.clear()

    #g.build_ddl('another.yaml')
    #g.dump('another.sql')
