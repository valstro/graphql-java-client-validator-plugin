package com.valstro.plugin.validator;

import com.valstro.plugin.util.CodeGenClassHolder;
import org.apache.maven.plugin.MojoFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
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

            if (clazzType.isEnum()) {
                checkEnumValues(clazzType);
                continue;
            }

            if (typeIsAClassReference(clazzType)) {
                validate(clazzType, holder.getCodeGenClassFromName(clazzType.getSimpleName()));
                continue;
            }

            // checking field type
            final Class<?> genClazzType = resolveFieldType(codeGenField);
            if (!genClazzType.isPrimitive() && clazzType.isPrimitive()) {
                // check Integer against int
                var primitiveType = resolvePrimitiveType(genClazzType);
                checkFieldType(primitiveType, clazzType);
            } else {
                checkGenericFieldType(codeGenField, field);
            }
        }
    }

    private void checkEnumValues(final Class<?> clazzType) throws MojoFailureException {
        var codeGen = this.holder.getCodeGenClassFromName(clazzType.getSimpleName());
        var enumValuesClass = clazzType.getEnumConstants();

        Set<String> enumValues = Arrays.stream(codeGen.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.toSet());

        LOG.info("Enum - CodeGen name: {} Class name: {}", codeGen.getName(), clazzType.getName());

        for (Object value : enumValuesClass) {
            if (!enumValues.contains(value.toString())) {
                throw new MojoFailureException(String.format("Enum value %s does not match code gen %s", value, String.join(", ", enumValues)));
            }
        }

        if (enumValuesClass.length != enumValues.size()) {
            throw new MojoFailureException(String.format("Enum value size elements %d does not match code gen size %d", enumValuesClass.length, enumValues.size() ));
        }
    }

    private static void checkFieldExist(Map<String, Field > codeGenFields, Field classField) throws MojoFailureException {
        if (!codeGenFields.containsKey(classField.getName()))
            throw new MojoFailureException(String.format("Cannot find field name %s in code-gen class", classField.getName()));
    }

    private static void checkFieldName(Field codeGenField, Field classField) throws MojoFailureException {
        if (!classField.getName().equals(codeGenField.getName()))
            throw new MojoFailureException(String.format("Codegen field name %s does not match java class field name %s", codeGenField.getName(), classField.getName()));
    }

    private static void checkGenericFieldType(Field codeGenField, Field classField) throws MojoFailureException {
        if (!classField.getGenericType().equals(codeGenField.getGenericType()))
            throw new MojoFailureException(String.format("Code-gen %s field is %s, does not match java class field type %s",codeGenField.getName(), codeGenField.getGenericType(),  classField.getType()));
    }

    private static void checkFieldType(String primitiveType, Class<?> classFieldType) throws MojoFailureException {
        if (!primitiveType.equals(classFieldType.getName()))
            throw new MojoFailureException(String.format("Code-gen field type %s does not match class field type %s", classFieldType.getSimpleName(), classFieldType.getSimpleName()));
    }

    private static String resolvePrimitiveType(Class<?> codeGenFieldType) throws MojoFailureException {
        try {
            return ((Class<?>) codeGenFieldType.getField("TYPE").get(null)).getName();
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new MojoFailureException(String.format("Expected a java wrapper type but it is not %s", codeGenFieldType.getName()));
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
