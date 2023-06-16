package com.valstro.plugin.validator;

import com.valstro.plugin.util.CodeGenClassHolder;
import org.apache.maven.plugin.MojoFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FieldValidator {
    private static final Logger LOG = LoggerFactory.getLogger(FieldValidator.class);

    private final CodeGenClassHolder holder;

    public FieldValidator(CodeGenClassHolder holder) {
        this.holder = holder;
    }

    public void validate(Class<?> clazz, Class<?> codeGenClazz) throws MojoFailureException {
        var fields = clazz.getDeclaredFields();
        var codeGenFields = Arrays.stream(codeGenClazz.getDeclaredFields())
                .collect(Collectors.toMap(Field::getName, Function.identity()));

        for (Field field : fields) {
            LOG.info("Checking field name: {}.{}", clazz.getSimpleName(), field.getName());

            checkFieldExist(codeGenFields, field);

            var codeGenField = codeGenFields.get(field.getName());
            checkFieldName(codeGenField, field);

            final Class<?> clazzType = resolveFieldType(field);
            final Class<?> genClazzType = resolveFieldType(codeGenField);

            if (typeIsAClassReference(clazzType)) {
                validate(clazzType, holder.getCodeGenClassFromName(clazzType.getSimpleName()));
                continue;
            }

            // checking field type
            if (!genClazzType.isPrimitive() && clazzType.isPrimitive()) {
                // check Integer against int
                var primitiveType = resolvePrimitiveType(genClazzType);
                checkFieldType(primitiveType, clazzType);
            } else {
                checkGenericFieldType(codeGenField, field);
            }
        }
    }

    private static void checkFieldExist(Map<String, Field > codeGenFields, Field classField) throws MojoFailureException {
        if (!codeGenFields.containsKey(classField.getName()))
            throw new MojoFailureException("Cannot find field name " + classField.getName() + " in code-gen class");
    }

    private static void checkFieldName(Field codeGenField, Field classField) throws MojoFailureException {
        if (!classField.getName().equals(codeGenField.getName()))
            throw new MojoFailureException("Codegen field name " + codeGenField.getName() + " does not match java class field name " + classField.getName());
    }

    private static void checkGenericFieldType(Field codeGenField, Field classField) throws MojoFailureException {
        if (!classField.getGenericType().equals(codeGenField.getGenericType()))
            throw new MojoFailureException("Code-gen " + codeGenField.getName() + " field is " + codeGenField.getGenericType() + ", does not match java class field type " + classField.getType());
    }

    private static void checkFieldType(String primitiveType, Class<?> classFieldType) throws MojoFailureException {
        if (!primitiveType.equals(classFieldType.getName()))
            throw new MojoFailureException("Code-gen field type " + classFieldType.getSimpleName() + " does not match class field type " + classFieldType.getSimpleName());
    }

    private static String resolvePrimitiveType(Class<?> codeGenFieldType) throws MojoFailureException {
        try {
            return ((Class<?>) codeGenFieldType.getField("TYPE").get(null)).getName();
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new MojoFailureException("Expected a java wrapper type but it is not " + codeGenFieldType.getName());
        }
    }

    private static Class<?> resolveFieldType(Field field) {
        return field.getGenericType() instanceof ParameterizedType ?
                (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0] : ((Class<?>) field.getGenericType());
    }

    private boolean typeIsAClassReference(Class<?> clazz) {
        return holder.isExist(clazz.getSimpleName());
    }

}
