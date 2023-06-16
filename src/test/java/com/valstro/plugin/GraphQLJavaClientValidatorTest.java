package com.valstro.plugin;

import com.valstro.plugin.util.CodeGenClassHolder;
import com.valstro.plugin.util.MockClassLoaderUtil;
import com.valstro.plugin.validator.FieldValidator;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

public class GraphQLJavaClientValidatorTest extends AbstractMojoTestCase {

    private GraphQLJavaClientValidator mojo;

    protected void setUp() throws Exception {
        super.setUp();
    }

    private void setUpVariable() throws IllegalAccessException {
        var mockClassLoader = new MockClassLoaderUtil();
        var generatedPackage = (String) getVariableValueFromObject(mojo, "generatedPackage");
        var mockGenClassHolder = new CodeGenClassHolder(mockClassLoader, generatedPackage);
        var mockFieldValidator = new FieldValidator(mockGenClassHolder);

        setVariableValueToObject(mojo, "projectClassLoader", mockClassLoader);
        setVariableValueToObject(mojo, "codeGenClassHolder", mockGenClassHolder);
        setVariableValueToObject(mojo, "fieldValidator", mockFieldValidator);
    }

    private GraphQLJavaClientValidator lookUpForMojo(String pomFile) throws Exception {
        File pom = getTestFile(pomFile);
        assertNotNull(pom);
        assertTrue(pom.exists());

        return (GraphQLJavaClientValidator) lookupMojo("validate-graphql-client", pom);
    }

    public void testValidatingMisMatchedFieldType() throws Exception {
        // Given that Bulbasour has List<String> field
        this.mojo = lookUpForMojo("src/test/resources/unit/project-to-test/plugin-mismatch-type-config.xml");
        assertNotNull(mojo);

        setUpVariable();

        // When execute the validation goal
        Exception exception = assertThrows(MojoFailureException.class, () -> mojo.executeGoal());

        // Then expected mojo failure on mismatch class
        String expectedMessage = "Code-gen evolutions field is java.util.List<java.lang.Integer>, does not match java class field type interface java.util.List";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    public void testValidatingMisMatchClassNames() throws Exception {
        // Given that Squirtle is not in code-gen package
        this.mojo = lookUpForMojo("src/test/resources/unit/project-to-test/plugin-mismatch-class-config.xml");
        assertNotNull(mojo);

        setUpVariable();

        // When execute the validation goal
        Exception exception = assertThrows(MojoFailureException.class, () -> mojo.executeGoal());

        // Then expected mojo failure on mismatch class
        String expectedMessage = "Cannot find class Squirtle in com.valstro.plugin.generated";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    public void testValidatingClassReferences() throws Exception {
        // Given that Pikachu has reference to TeamRocket
        // And TeamRocket has reference to Meowth
        // And Meowth has mismatch field name
        this.mojo = lookUpForMojo("src/test/resources/unit/project-to-test/plugin-recursive-config.xml");
        assertNotNull(mojo);

        setUpVariable();

        // When execute the validation goal
        Exception exception = assertThrows(MojoFailureException.class, () -> mojo.executeGoal());

        // Then expected mojo failure on mismatch field [abilities - abilityList]
        String expectedMessage = "Cannot find field name abilityList in code-gen class";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    public void testValidatingMatchingClass() throws Exception {
        // Given that Bulbasour has List<String> field
        this.mojo = lookUpForMojo("src/test/resources/unit/project-to-test/plugin-match-class-config.xml");
        assertNotNull(mojo);

        setUpVariable();

        // When execute the validation goal
        mojo.executeGoal();

        // Then expected no mojo exception
    }
}