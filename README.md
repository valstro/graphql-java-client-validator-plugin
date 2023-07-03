# VALSTRO GraphQL Validation Maven Plugin

## Background
We use a Java GraphQL plugin to generate Java classes from GraphQL schemas:

```xml
<groupId>com.graphql-java-generator</groupId>
<artifactId>graphql-maven-plugin</artifactId>
```

These code-gen'd Java classes are used throughout the application. However,  using them in SmallRye for GraphQL API calls is problematic because the code-gen'd classes contain more fields than we want to query and, worse, they contain cyclic dependencies that can cause severe runtime errors.
The solution is to "hand roll" POJOs that _mirror_ the code-gen'd classes, but include only the precise subset of fields we want to retrieve from the GraphQL API calls. This prevents issues with cyclic dependencies and makes the queries more efficient.

This works well, but a new problem arises if the GraphQL schema changes. If something changes in the schema, such as renaming a variable, a runtime error will occur if the corresponding POJO version doesn't have the same properties.

`graphql-java-client-validator-plugin` is a maven plugin that solves this problem by **validating** POJO classes against their schema-generated counterparts build-time.




HOW IT WORKS
-------------
Once this maven plugin is added to the project's pom file, the plugin goal will execute during the project's maven compilation phase.

`graphql-java-client-validator-plugin` starts by loading the code-gen classes from a given package, along with the corresponding POJO java classes.  The plugin tries to locate a matching code-gen'd class using the POJO class name. If plugin fails to find a match, the build will immediately fail. If it succeeds in finding a corresponding class, the plugin then checks field names and types match. If there are any unmatching field names or types, the build will fail.

The only exception allowed for unmatching field types is if the code-gen class has java wrapper class type variables and the POJO has as a primitive type. For example, it considers `public int name` to be an acceptable match for `public Integer name`.


PARAMETERS :
-------------------
| Name | Description | Type | Required | Default |
|------| ----------- | -----| -------- |---------|
| generatedPachage | Location of code-gen'd classes | String | Optional | valstro.oms.lib |
| clientClasses |  List of client classes that has the java classes to be validated | String[] | Yes | - |


USAGE :
-------
```
<plugin>
  <groupId>com.valstro.plugin</groupId>
  <artifactId>graphql-java-client-validator-plugin</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <executions>
    <execution>
      <goals>
        <goal>validate-graphql-client</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <clientClasses>
      <clientClass>package.class_name</clientClass>
      <clientClass>package.class_name</clientClass>
    </clientClasses>
  </configuration>
 </plugin>
```


MERGING PRs:
----------------
After each merge to main, the package is built and an attempt is made to release to the Github Java Repository. This only succeeds where the version does not already exist in the repo.

Please make sure the version is increased in the `pom.xml` file before every merge to main.