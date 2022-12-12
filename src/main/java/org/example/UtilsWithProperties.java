package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class UtilsWithProperties {
    /*
     Don't work
     */
    public static <T> T loadFromProperties(Class<T> cls, Path propertiesPath) throws IOException, IllegalAccessException {
        Properties properties;
        T object;

        try {
            object = cls.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException("Class should have no argument constructor");
        }

        try(FileInputStream is = new FileInputStream(propertiesPath.toFile())) {
            properties = new Properties();
            properties.load(is);
        }

        for (Field field : cls.getDeclaredFields()) {
            field.setAccessible(true);
            String key;

            if (field.isAnnotationPresent(Property.class) && !field.getAnnotation(Property.class).name().equals("")) {
                key = field.getAnnotation(Property.class).name();
            } else {
                key = field.getName();
            }

            if (!properties.containsKey(key) || !properties.get(key).getClass().equals(field.getType())) {
                throw new RuntimeException("Property is not present in " + propertiesPath);
            }

            parseAndSetValue(object, field, key, properties);

        }

        return object;
    }

    private static <T> void parseAndSetValue(T object, Field field, String key, Properties properties) throws IllegalAccessException {
        if (field.getType().equals(Integer.class) || field.getType().equals(int.class) || (field.getType().equals(String.class))) {
            field.set(object, properties.get(key));

        } else if (field.getType().equals(Instant.class)) {
            field.set(object, LocalDateTime.parse(properties.getProperty(key), DateTimeFormatter
                            .ofPattern(field.getAnnotation(Property.class).dateFormat()))
                    .toInstant(ZoneOffset.UTC));
        } else {
            throw new RuntimeException("Type of field (" + field.getType() + ") unsupported!" );
        }
    }
}
