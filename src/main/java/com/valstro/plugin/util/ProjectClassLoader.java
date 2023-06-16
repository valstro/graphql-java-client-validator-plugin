package com.valstro.plugin.util;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;

public class ProjectClassLoader {

    private final MavenProject project;
    private final PluginDescriptor descriptor;

    public ProjectClassLoader(MavenProject project, PluginDescriptor descriptor) {
        this.project = project;
        this.descriptor = descriptor;
    }

    public Class<?> loadClientClass(String clientClass) throws MojoExecutionException {
        try {
            return getProjectClassLoader().loadClass(clientClass);
        } catch (ClassNotFoundException | MalformedURLException | DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Could not load client class " + clientClass);
        }
    }

    ClassLoader getProjectClassLoader() throws DependencyResolutionRequiredException, MalformedURLException {
        List<String> runtimeClasspathElements = project.getRuntimeClasspathElements();
        ClassRealm realm = descriptor.getClassRealm();

        for (String element : runtimeClasspathElements)
        {
            File elementFile = new File(element);
            realm.addURL(elementFile.toURI().toURL());
        }

        return realm;
    }
}
