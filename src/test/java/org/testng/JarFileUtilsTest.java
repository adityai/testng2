package org.testng;

import org.testng.annotations.Test;
import org.testng.xml.IPostProcessor;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JarFileUtilsTest {
    private static final File jar = new File("src/test/resources/testng-tests.jar");

    @Test
    public void testWithValidTestNames() throws MalformedURLException {
        JarFileUtils utils = newJarFileUtils(Collections.singletonList("testng-tests-child1"));
        runTest(utils, 1,
                new String[]{"testng-tests-child1"},
                new String[]{"org.testng.SampleTest1"}
        );
    }

    @Test
    public void testWithNoTestNames() throws MalformedURLException {
        JarFileUtils utils = newJarFileUtils(null);
        runTest(utils, 3,
                new String[]{"testng-tests-child1", "testng-tests-child2", "testng-tests-child3"},
                new String[]{"org.testng.SampleTest1", "org.testng.SampleTest2", "org.testng.SampleTest3"}
        );
    }

    @Test(expectedExceptions = TestNGException.class,
            expectedExceptionsMessageRegExp = "\nThe test\\(s\\) <\\[testng-tests-child11\\]> cannot be found.")
    public void testWithInvalidTestNames() throws MalformedURLException {
        JarFileUtils utils = newJarFileUtils(Collections.singletonList("testng-tests-child11"));
        runTest(utils, 1,
                new String[]{"testng-tests-child1"},
                new String[]{"org.testng.SampleTest1"}
        );
    }

    @Test
    public void testWithInvalidXmlFile() throws MalformedURLException {
        JarFileUtils utils = newJarFileUtils("invalid-testng-tests.xml",
                Collections.singletonList("testng-tests-child11"));
        runTest(utils, 1,
                null,
                new String[]{"org.testng.SampleTest1", "org.testng.SampleTest2", "org.testng.SampleTest3"},
                "Jar suite");
    }

    private static void runTest(JarFileUtils utils,
                                int numberOfTests,
                                String[] expectedTestNames,
                                String[] expectedClassNames) throws MalformedURLException {
        runTest(utils, numberOfTests, expectedTestNames, expectedClassNames, "testng-tests-suite");
    }

    private static void runTest(JarFileUtils utils,
                                int numberOfTests,
                                String[] expectedTestNames,
                                String[] expectedClassNames,
                                String expectedSuiteName) throws MalformedURLException {
        List<XmlSuite> suites = utils.extractSuitesFrom(jar);
        assertThat(suites).hasSize(1);
        XmlSuite suite = suites.get(0);
        assertThat(suite.getName()).isEqualTo(expectedSuiteName);
        assertThat(suite.getTests()).hasSize(numberOfTests);
        List<String> testNames = new LinkedList<>();
        List<String> classNames = new LinkedList<>();
        for (XmlTest xmlTest : suite.getTests()) {
            if (expectedTestNames != null) {
                testNames.add(xmlTest.getName());
            }
            for (XmlClass xmlClass : xmlTest.getXmlClasses()) {
                classNames.add(xmlClass.getName());
            }
        }
        if (expectedTestNames != null) {
            assertThat(testNames).containsExactly(expectedTestNames);
        }
        assertThat(classNames).contains(expectedClassNames);
    }

    public static class FakeProcessor implements IPostProcessor {

        @Override
        public Collection<XmlSuite> process(Collection<XmlSuite> suites) {
            return suites;
        }
    }

    private static JarFileUtils newJarFileUtils(List<String> testNames) throws MalformedURLException {
        return newJarFileUtils("testng-tests.xml", testNames);
    }

    private static JarFileUtils newJarFileUtils(String suiteXmlName, List<String> testNames) throws MalformedURLException {
        URL url = jar.toURI().toURL();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{url}, ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        return new JarFileUtils(new FakeProcessor(), suiteXmlName, testNames);
    }

}
