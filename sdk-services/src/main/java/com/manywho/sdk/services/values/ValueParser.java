package com.manywho.sdk.services.values;

import com.google.common.collect.Lists;
import com.manywho.sdk.api.ContentType;
import com.manywho.sdk.api.run.PropertyAware;
import com.manywho.sdk.api.run.elements.type.MObject;
import com.manywho.sdk.services.types.Type;
import com.manywho.sdk.services.types.TypeParser;
import com.manywho.sdk.services.types.TypeRepository;
import com.manywho.sdk.services.types.TypePropertyMismatchException;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ValueParser {
    private final TypeRepository typeRepository;
    private final ValueRepository valueRepository;

    @Inject
    public ValueParser(TypeRepository typeRepository, ValueRepository valueRepository) {
        this.typeRepository = typeRepository;
        this.valueRepository = valueRepository;
    }

    public <T extends Type> Collection<T> fromList(List<MObject> objects, Class<T> type) {
        if (objects == null) {
            return Lists.newArrayList();
        }

        return objects.stream()
                .map(object -> fromObject(object, type))
                .collect(Collectors.toList());
    }

    public <T extends Type> T fromObject(List<MObject> objects, Class<T> type) {
        Type.Element typeAnnotation = type.getAnnotation(Type.Element.class);

        if (objects == null || objects.isEmpty()) {
            throw new RuntimeException("Unable to find an object to parse into " + typeAnnotation.name());
        }

        return fromObject(objects.get(0), type);
    }

    public <T extends Type> T fromObject(MObject object, Class<T> type) {
        try {
            T instance = type.newInstance();

            Field identifierField = typeRepository.findTypeIdentifier(type);

            AccessController.doPrivileged((PrivilegedAction) () -> {
                identifierField.setAccessible(true);
                return null;
            });

            identifierField.set(instance, object.getExternalId());

            Map<String, Field> typeProperties = valueRepository.findTypeProperties(type);

            object.getProperties().stream()
                    .filter(property -> typeProperties.containsKey(property.getDeveloperName()))
                    .forEach(property -> {
                        Field field = typeProperties.get(property.getDeveloperName());

                        Type.Property annotation = field.getAnnotation(Type.Property.class);

                        populateObjectField(instance, field, annotation.contentType(), property);
                    });

            return instance;
        } catch (InstantiationException e) {
            throw new RuntimeException("The type " + type.getAnnotation(Type.Element.class).name() + " does not have a zero-argument constructor");
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean toBoolean(Field field, String value) {
        if (boolean.class.isAssignableFrom(field.getType())) {
            return Boolean.valueOf(value);
        }

        throw new TypePropertyMismatchException(field, boolean.class, ContentType.Boolean);
    }

    public static String toContent(Field field, String value) {
        if (String.class.isAssignableFrom(field.getType())) {
            return value;
        }

        throw new TypePropertyMismatchException(field, String.class, ContentType.Content);
    }

    public static TemporalAccessor toDateTime(Field field, String value) {
        // TODO: Implement support for non-OffsetDateTime field types
        if (TemporalAccessor.class.isAssignableFrom(field.getType())) {
            return OffsetDateTime.parse(value);
        }

        throw new TypePropertyMismatchException(field, TemporalAccessor.class, ContentType.DateTime);
    }

    public static String toEncrypted(Field field, String value) {
        if (String.class.isAssignableFrom(field.getType())) {
            return value;
        }

        throw new TypePropertyMismatchException(field, String.class, ContentType.Encrypted);
    }

    @SuppressWarnings("unchecked")
    private <T extends Type> Collection<T> toList(Field field, List<MObject> list) {
        Class<?> genericType = TypeParser.findGenericType(field.getGenericType());

        if (Type.class.isAssignableFrom(genericType)) {
            return fromList(list, (Class<T>) genericType);
        }

        throw new TypePropertyMismatchException(field, Collection.class, ContentType.List);
    }

    public static Number toNumber(Field field, String value) {
        if (field.getType().equals(Double.class)) {
            return Double.valueOf(value);
        }

        if (field.getType().equals(Float.class)) {
            return Float.valueOf(value);
        }

        if (field.getType().equals(Integer.class)) {
            return Integer.valueOf(value);
        }

        if (field.getType().equals(Long.class)) {
            return Long.valueOf(value);
        }

        if (field.getType().equals(Short.class)) {
            return Short.valueOf(value);
        }

        throw new TypePropertyMismatchException(field, Number.class, ContentType.Number);
    }

    @SuppressWarnings("unchecked")
    private <T extends Type> T toObject(Field field, List<MObject> objects) {
        if (Type.class.isAssignableFrom(field.getType())) {
            if (objects == null) {
                return null;
            }

            if (objects.isEmpty()) {
                throw new RuntimeException("Unable to find an object to parse into " + field.getAnnotation(Type.Property.class).name());
            }

            return fromObject(objects.get(0), (Class<T>) field.getType());
        }

        throw new TypePropertyMismatchException(field, Type.class, ContentType.Object);
    }

    public static String toPassword(Field field, String value) {
        if (String.class.isAssignableFrom(field.getType())) {
            return value;
        }

        throw new TypePropertyMismatchException(field, String.class, ContentType.Password);
    }

    public static String toString(Field field, String value) {
        if (String.class.isAssignableFrom(field.getType())) {
            return value;
        }

        throw new TypePropertyMismatchException(field, String.class, ContentType.String);
    }

    public void populateObjectField(Object object, Field field, ContentType contentType, PropertyAware property) {
        AccessController.doPrivileged((PrivilegedAction) () -> {
            field.setAccessible(true);
            return null;
        });

        try {
            switch (contentType) {
                case Boolean:
                    field.set(object, toBoolean(field, property.getContentValue()));
                    break;
                case Content:
                    field.set(object, toContent(field, property.getContentValue()));
                    break;
                case DateTime:
                    if (StringUtils.isEmpty(property.getContentValue())) {
                        break;
                    }

                    field.set(object, toDateTime(field, property.getContentValue()));
                    break;
                case Encrypted:
                    field.set(object, toEncrypted(field, property.getContentValue()));
                    break;
                case List:
                    field.set(object, toList(field, property.getObjectData()));
                    break;
                case Number:
                    if (StringUtils.isEmpty(property.getContentValue())) {
                        break;
                    }

                    field.set(object, toNumber(field, property.getContentValue()));
                    break;
                case Object:
                    field.set(object, toObject(field, property.getObjectData()));
                    break;
                case Password:
                    field.set(object, toPassword(field, property.getContentValue()));
                    break;
                case String:
                    field.set(object, toString(field, property.getContentValue()));
                    break;
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}