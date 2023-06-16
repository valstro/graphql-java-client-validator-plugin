package com.valstro.plugin;

import com.valstro.plugin.util.ProjectClassLoader;
import com.valstro.plugin.util.CodeGenClassHolder;
import com.valstro.plugin.validator.FieldValidator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

@Mojo(name = "validate-graphql-client", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GraphQLJavaClientValidator extends AbstractMojo {
    private static final Logger LOG = LoggerFactory.getLogger(GraphQLJavaClientValidator.class);

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin}", required = true, readonly = true)
    private PluginDescriptor descriptor;

    @Parameter(property = "clientClasses", required = true)
    private String[] clientClasses;

    @Parameter(property = "generatedPackage", defaultValue = "com.valstro.oms.lib")
    private String generatedPackage;

    private ProjectClassLoader projectClassLoader;
    private CodeGenClassHolder codeGenClassHolder;
    private FieldValidator fieldValidator;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        initialize();
        executeGoal();
    }

    private void initialize() {
        this.projectClassLoader = new ProjectClassLoader(project, descriptor);

        this.codeGenClassHolder = new CodeGenClassHolder(projectClassLoader, generatedPackage);
        this.fieldValidator = new FieldValidator(codeGenClassHolder);
    }

    void executeGoal() throws MojoExecutionException, MojoFailureException{
        codeGenClassHolder.loadCodeGenClasses();
        LOG.info("code-gen classes has been loaded from {}", generatedPackage);

        for (String clientClass : clientClasses) {
            Class<?> sourceClazz = projectClassLoader.loadClientClass(clientClass);
            LOG.info("Client class {} has been loaded", clientClass);

            // collect client class returns
            var methods = sourceClazz.getMethods();
            var returnTypes = Arrays.stream(methods)
                    .map(GraphQLJavaClientValidator::resolveMethodReturnType)
                    .collect(Collectors.toSet());

            for (Type returnType : returnTypes) {
                var clazz = ((Class<?>) returnType);
                var codeGenClazz = resolveCodeGenClass(clazz);

                LOG.info("Comparing {} and {}", clazz.getName(), codeGenClazz.getName());
                fieldValidator.validate(clazz, codeGenClazz);
            }
        }
    }

    private Class<?> resolveCodeGenClass(Class<?> clazz) throws MojoFailureException {
        var codeGenClazz = codeGenClassHolder.getCodeGenClassFromName(clazz.getSimpleName());

        if (codeGenClazz == null)
            throw new MojoFailureException("Cannot find class " + clazz.getSimpleName() + " in " + generatedPackage);

        return codeGenClazz;
    }

    private static Type resolveMethodReturnType(Method method) {
        var type = method.getGenericReturnType();
        return (method.getGenericReturnType() instanceof ParameterizedType) ?
                ((ParameterizedType) type).getActualTypeArguments()[0] : type;
    }

}

