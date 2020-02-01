package cz.cuni.mff.tablecreator;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableCreator {

  private static final String DEFAULT_VARCHAR_SIZE = "255";

  private static void processFieldBasics(Field f, Map<String, String> attributes) {
    String colName = f.getName();
    try {
      Column c = f.getAnnotation(Column.class);
      colName = c.name();
      if (colName.equals("")) {
        colName = f.getName();
      }
      if (!c.nullable()) {
        attributes.put("nullable", " NOT NULL");
      }
      attributes.put("fieldLength", String.valueOf(c.length()));
    } catch (NullPointerException e) {
    }
    attributes.put("colName", colName);
  }

  private static void processAnnotations(Annotation[] fieldAnnotations, Map<String, String> attributes) {
    for (Annotation an : fieldAnnotations) {
      if (an.annotationType() == Id.class) {
        attributes.put("primaryKey", " PRIMARY KEY");
      } else if (an.annotationType() == GeneratedValue.class) {
        attributes.put("autoIncrement", " AUTO_INCREMENT");
      }
    }
  }

  private static void processTypes(Field f, Map<String, String> attributes) {
    String type = "";
    if (f.getType() == long.class) {
      type = " BIGINT";
    } else if (f.getType() == String.class) {
      type = " VARCHAR(" + attributes.get("fieldLength") + ")";
    } else if (f.getType() == int.class) {
      type = " INTEGER";
    } else {
      if (f.isAnnotationPresent(Enumerated.class)) {
        if (f.getAnnotation(Enumerated.class).value().name().equals("STRING")) {
          type = " VARCHAR(255)";
        } else if (f.getAnnotation(Enumerated.class).value().name().equals("ORDINAL")) {
          type = " INTEGER";
        }
      }
    }
    attributes.put("type", type);
  }

  private static String getAttribute(String parameter, Map<String, String> attributes) {
    return attributes.getOrDefault(parameter, "");
  }

  public static String formatColumnString(Map<String, String> attributes) {
    String columnString = getAttribute("colName", attributes) +
            getAttribute("type", attributes) +
            getAttribute("primaryKey", attributes) +
            getAttribute("nullable", attributes) +
            getAttribute("autoIncrement", attributes);
    return columnString;
  }

  public static void getColumn(Field f, List<String> columns) {
    if (!Modifier.isFinal(f.getModifiers()) & !f.isAnnotationPresent(Transient.class)) {
      Map<String, String> attributes = new HashMap<>();
      attributes.put("fieldLength", DEFAULT_VARCHAR_SIZE);

      processFieldBasics(f, attributes);
      processAnnotations(f.getAnnotations(), attributes);
      processTypes(f, attributes);

      String columnString = formatColumnString(attributes);
      columns.add(columnString);
    }
  }

  public static String process(Class<?> cl) throws IllegalArgumentException {
    if (cl.isAnnotationPresent(Entity.class)) {
      String tableName = cl.getAnnotation(Entity.class).name();
      if (tableName.isEmpty()) {
        tableName = cl.getSimpleName();
      }
      String SQLstring = "CREATE TABLE " + tableName;
      Field[] fields = cl.getDeclaredFields();
      List<String> columns = new ArrayList<>();
      for (Field f : fields) {
        getColumn(f, columns);
      }
      String s = String.join(", ", columns);
      SQLstring += "(" + s + ");";
      return SQLstring;
    } else throw new IllegalArgumentException();
  }
}
