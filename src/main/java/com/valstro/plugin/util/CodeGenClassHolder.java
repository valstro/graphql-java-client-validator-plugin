package com.valstro.plugin.util;

import com.google.common.reflect.ClassPath;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CodeGenClassHolder {

    private static final Map<String, Class<?>> CODE_GEN_CLASSES = new HashMap<>();

    private final ProjectClassLoader projectClassLoader;
    private final String codeGenPackage;

    public CodeGenClassHolder(ProjectClassLoader projectClassLoader, String codeGenPackage) {
        this.projectClassLoader = projectClassLoader;
        this.codeGenPackage = codeGenPackage;
    }

    public void loadCodeGenClasses() throws MojoExecutionException {
        try {
            var codeGenClasses = ClassPath.from(projectClassLoader.getProjectClassLoader())
                    .getTopLevelClasses()
                    .stream()
                    .filter(clazz -> clazz.getPackageName()
                            .equalsIgnoreCase(codeGenPackage))
                    .map(ClassPath.ClassInfo::load)
                    .collect(Collectors.toMap(Class::getName, Function.identity()));
            CODE_GEN_CLASSES.putAll(codeGenClasses);
        } catch (IOException | DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Cannot load code-gen classes from " + codeGenPackage);
        }
    }

    public Class<?> getCodeGenClassFromName(String className) {
        return CODE_GEN_CLASSES.get(codeGenPackage + "." + className);
    }

    public boolean isExist(String className) {
        return CODE_GEN_CLASSES.containsKey(codeGenPackage + "." + className);
    }
}
