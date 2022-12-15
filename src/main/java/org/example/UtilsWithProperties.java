package org.example;

import org.example.annotation.Property;
import org.example.exception.UnsupportedTypeException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Properties;

public class UtilsWithProperties {

    public static <T> T loadFromProperties(Class<T> cls, Path propertiesPath) throws IOException {
        T object = getEmptyInstance(cls);
        Properties properties = getProperties(propertiesPath);

        for (Field field : cls.getDeclaredFields()) {
            field.setAccessible(true);
            String propertyName = getFieldName(field);

            if (!properties.containsKey(propertyName)) {
                throw new RuntimeException("Properties file no contains property with name " + propertyName);
            }

            try {

                if (field.getType().equals(int.class)) {
                    int value = Integer.parseInt(properties.getProperty(propertyName));
                    setValue(object, value, field, int.class);

                }else if(field.getType().equals(Integer.class) ) {
                    Integer value = Integer.parseInt(properties.getProperty(propertyName));
                    setValue(object, value, field, Integer.class);

                }else if (field.getType().equals(String.class)) {
                    String value = properties.getProperty(propertyName);
                    setValue(object, value, field, String.class);

                } else if (field.getType().equals(Instant.class)) {
                    DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern(field.isAnnotationPresent(Property.class) ?
                            field.getAnnotation(Property.class).dateFormat() :
                            "dd.MM.yyyy HH:mm");

                    Instant value = LocalDateTime.parse(properties.getProperty(propertyName), dateTimeFormat).toInstant(ZoneOffset.UTC);
                    setValue(object, value, field, Instant.class);
                } else {
                    throw new UnsupportedTypeException();
                }

            } catch (NumberFormatException e) {
                throw new RuntimeException("The value of property is not a integer for field with name " + propertyName);
            } catch (IllegalArgumentException e){
                throw new RuntimeException("Provided pattern of DateTime is invalid for field with name " + propertyName);
            } catch (DateTimeParseException e)  {
                throw new RuntimeException("Property value contains no compiled value by DateTimeFormat pattern for field with name " + propertyName);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("The setter threw an exception when was invoked for field with name " + propertyName);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("The class no contains setter for field with name " + propertyName);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("No accessible setter for field with name " + propertyName);
            } catch (UnsupportedTypeException e) {
                throw new  RuntimeException("The type of field is not supported");
            }
        }
        return object;
    }

    private static <T> T getEmptyInstance(Class<T> cls) {
        T object;

        try {
            Constructor<T> constructor = cls.getDeclaredConstructor();
            constructor.setAccessible(true);
            object = constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Your class should contain no-argument constructor");
        } catch (InvocationTargetException e) {
            throw new RuntimeException("The no-argument constructor threw an exception when was invoked");
        } catch (InstantiationException e) {
            throw new RuntimeException("The class represents an abstract class, an interface, an array class, a primitive type, or void");
        } catch (IllegalAccessException e) {
            throw new RuntimeException("No accessible no-argument constructor");
        }
        return object;
    }

    private static Properties getProperties(Path propertiesPath) throws IOException {
        Properties properties;

        try (InputStream inputStream = Files.newInputStream(propertiesPath.toFile().toPath())) {
            properties = new Properties();
            properties.load(inputStream);
        }
        return properties;
    }

    private static String getFieldName(Field field) {
        if (!field.isAnnotationPresent(Property.class) || field.getAnnotation(Property.class).name().equals("")) {
            return field.getName();
        } else {
            return field.getAnnotation(Property.class).name();
        }
    }

    private static <T, V> void setValue(T object, V value, Field field, Class<V> valueClass)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method setter = object.getClass().getDeclaredMethod(getSetterName(field.getName()), valueClass);
        setter.setAccessible(true);
        setter.invoke(object, value);
    }

    private static String getSetterName(String fieldName) {
        return "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }
}
