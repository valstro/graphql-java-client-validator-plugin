package com.valstro.plugin.util;

import org.apache.maven.plugin.MojoExecutionException;

public class MockClassLoaderUtil extends ProjectClassLoader {

    public MockClassLoaderUtil() {
        super(null, null);
    }

    @Override
    public Class<?> loadClientClass(String clientClass) throws MojoExecutionException {
        try {
            return getClass().getClassLoader().loadClass(clientClass);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("cannot load test client");
        }
    }

    @Override
    ClassLoader getProjectClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }
}
