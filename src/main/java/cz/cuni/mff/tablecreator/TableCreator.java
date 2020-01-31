package cz.cuni.mff.tablecreator;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class TableCreator {

    public static String process(Class<?> cl) throws IllegalArgumentException{
        if(cl.isAnnotationPresent(Entity.class)) {
            String tableName = "";
            tableName = cl.getAnnotation(Entity.class).name();
            if (tableName.equals("")){
                tableName = cl.getSimpleName();
            }
            String SQLstring = "CREATE TABLE " + tableName;
            Field[] fields = cl.getDeclaredFields();
            List<String> columns = new ArrayList<>();
            for (Field f : fields) {
                if(Modifier.toString(f.getModifiers()).indexOf("final")<0&&!f.isAnnotationPresent(Transient.class)) {

                String columnString = "";
                String colName = f.getName();
                String type = "";
                String nullable = "";
                String primaryKey = "";
                String autoIncrement = "";
                int fieldLength = 255;


                Annotation[] fieldAnnotations = f.getAnnotations();
                try {
                    Column c = f.getAnnotation(Column.class);
                    colName = c.name();
                    if(colName.equals("")){
                        colName = f.getName();
                    }
                    if (!c.nullable()) {
                        nullable += " NOT NULL";
                    }
                    try {
                        fieldLength = c.length();
                    } catch (NullPointerException e) {
                    }
                } catch (NullPointerException e) {

                } finally {
                    for (Annotation an : fieldAnnotations) {
                        if (an.annotationType() == Id.class) {
                            primaryKey = " PRIMARY KEY";
                        } else if (an.annotationType() == GeneratedValue.class) {
                            autoIncrement = " AUTO_INCREMENT";
                        }
                    }
                }
                if (f.getType() == long.class) {
                    type = " BIGINT";
                } else if (f.getType() == String.class) {
                    type = " VARCHAR(" + fieldLength + ")";
                } else if (f.getType() == int.class){
                    type = " INTEGER";
                } else
                {
                    if(f.isAnnotationPresent(Enumerated.class)){
                        if(f.getAnnotation(Enumerated.class).value().name().equals("STRING")){
                            type = " VARCHAR(255)";
                        }else if(f.getAnnotation(Enumerated.class).value().name().equals("ORDINAL")){
                            type = " INTEGER";
                        }
                    }
                }

                columnString = colName + type;
                if (primaryKey != null) {
                    columnString += primaryKey;
                }
                if (nullable != null) {
                    columnString += nullable;
                }
                if (autoIncrement != null) {
                    columnString += autoIncrement;
                }
                ;
                columns.add(columnString);
            }
            }
            String s = String.join(", ", columns);
            SQLstring += "(" + s + ");";
            return SQLstring;
        }
        else throw new IllegalArgumentException();
    }
}
