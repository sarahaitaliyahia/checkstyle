///////////////////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code and other text files for adherence to a set of rules.
// Copyright (C) 2001-2026 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
///////////////////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.utils;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.puppycrawl.tools.checkstyle.internal.utils.TestUtil.getExpectedThrowable;
import static com.puppycrawl.tools.checkstyle.internal.utils.TestUtil.isUtilsClassHasPrivateConstructor;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import com.puppycrawl.tools.checkstyle.AbstractPathTestSupport;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;

public final class ResourceUtilTest extends AbstractPathTestSupport {

    @TempDir
    public Path temporaryFolder;

    @Override
    public String getPackageLocation() {
        return "com/puppycrawl/tools/checkstyle/utils/commonutil";
    }

    @Test
    public void testIsProperUtilsClass() throws ReflectiveOperationException {
        assertWithMessage("Constructor is not private")
                .that(isUtilsClassHasPrivateConstructor(ResourceUtil.class))
                .isTrue();
    }

    @Test
    public void testGetUriByFilenameFindsAbsoluteResourceOnClasspath() throws Exception {
        final String filename =
            "/" + getPackageLocation() + "/InputCommonUtilTest_empty_checks.xml";
        final URI uri = ResourceUtil.getUriByFilename(filename);

        final Properties properties = System.getProperties();
        final Configuration config = ConfigurationLoader.loadConfiguration(uri.toASCIIString(),
            new PropertiesExpander(properties));
        assertWithMessage("Unexpected config name!")
            .that(config.getName())
            .isEqualTo("Checker");
    }

    @Test
    public void testGetUriByFilenameFindsRelativeResourceOnClasspath() throws Exception {
        final String filename =
            getPackageLocation() + "/InputCommonUtilTest_empty_checks.xml";
        final URI uri = ResourceUtil.getUriByFilename(filename);

        final Properties properties = System.getProperties();
        final Configuration config = ConfigurationLoader.loadConfiguration(uri.toASCIIString(),
            new PropertiesExpander(properties));
        assertWithMessage("Unexpected config name!")
            .that(config.getName())
            .isEqualTo("Checker");
    }

    /**
     * This test illustrates #6232.
     * Without fix, the assertion will fail because the URL under test
     * "com/puppycrawl/tools/checkstyle/utils/commonutil/InputCommonUtilTest_resource.txt"
     * will be interpreted relative to the current package
     * "com/puppycrawl/tools/checkstyle/utils/"
     */
    @Test
    public void testGetUriByFilenameFindsResourceRelativeToRootClasspath() throws Exception {
        final String filename =
                getPackageLocation() + "/InputCommonUtilTest_resource.txt";
        final URI uri = ResourceUtil.getUriByFilename(filename);
        assertWithMessage("URI is null for: %s", filename)
            .that(uri)
            .isNotNull();
        final String uriRelativeToPackage =
                "com/puppycrawl/tools/checkstyle/utils/"
                        + getPackageLocation() + "/InputCommonUtilTest_resource.txt";
        assertWithMessage("URI is relative to package %s", uriRelativeToPackage)
            .that(uri.toASCIIString())
            .doesNotContain(uriRelativeToPackage);
        final String content = IOUtils.toString(uri.toURL(), StandardCharsets.UTF_8);
        assertWithMessage("Content mismatches for: %s", uri.toASCIIString())
            .that(content)
            .startsWith("good");
    }

    @Test
    public void testGetUriByFilenameClasspathPrefixLoadConfig() throws Exception {
        final String filename = ResourceUtil.CLASSPATH_URL_PROTOCOL
            + getPackageLocation() + "/InputCommonUtilTestWithChecks.xml";
        final URI uri = ResourceUtil.getUriByFilename(filename);

        final Properties properties = System.getProperties();
        final Configuration config = ConfigurationLoader.loadConfiguration(uri.toASCIIString(),
            new PropertiesExpander(properties));
        assertWithMessage("Unexpected config name!")
            .that(config.getName())
            .isEqualTo("Checker");
    }

    @Test
    public void testGetUriByFilenameFindsRelativeResourceOnClasspathPrefix() throws Exception {
        final String filename = ResourceUtil.CLASSPATH_URL_PROTOCOL
            + getPackageLocation() + "/InputCommonUtilTest_empty_checks.xml";
        final URI uri = ResourceUtil.getUriByFilename(filename);

        final Properties properties = System.getProperties();
        final Configuration config = ConfigurationLoader.loadConfiguration(uri.toASCIIString(),
            new PropertiesExpander(properties));
        assertWithMessage("Unexpected config name!")
            .that(config.getName())
            .isEqualTo("Checker");
    }

    @Test
    public void testLoadSuppressionsUriSyntaxException() throws Exception {
        final URL configUrl = mock();
        when(configUrl.toURI()).thenThrow(URISyntaxException.class);
        try (MockedStatic<ResourceUtil> utilities =
                     mockStatic(ResourceUtil.class, CALLS_REAL_METHODS)) {
            final String fileName = "/suppressions_none.xml";
            utilities.when(() -> ResourceUtil.getCheckstyleResource(fileName))
                    .thenReturn(configUrl);

            final CheckstyleException ex = getExpectedThrowable(CheckstyleException.class, () -> {
                ResourceUtil.getUriByFilename(fileName);
            });
            assertWithMessage("Invalid exception cause")
                    .that(ex)
                    .hasCauseThat()
                            .isInstanceOf(URISyntaxException.class);
            assertWithMessage("Invalid exception message")
                    .that(ex)
                    .hasMessageThat()
                            .isEqualTo("Unable to find: " + fileName);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com/config.xml", "ftp://example.com/config.xml",
        "file:/tmp/config.xml"})
    public void testProtocolUri(String filename) throws Exception {
        assertThat(ResourceUtil.getUriByFilename(filename)).isEqualTo(URI.create(filename));
    }

    @Test
    public void testFileAndInputSource() throws Exception {
        final Path file = Files.createFile(temporaryFolder.resolve("config " + "\u00e9" + ".xml"));
        assertThat(ResourceUtil.getUriByFilename(file.toString()))
                .isEqualTo(file.toFile().toURI());
        assertThat(ResourceUtil.sourceFromFilename(file.toString()).getSystemId())
                .isEqualTo(file.toFile().toURI().toASCIIString());
    }

    @Test
    public void testMissingResource() {
        final String filename = "checkstyle-missing-resource.xml";
        final CheckstyleException exception = getExpectedThrowable(CheckstyleException.class,
                () -> {
                    ResourceUtil.getUriByFilename(filename);
                });
        assertThat(exception).hasMessageThat().isEqualTo("Unable to find: " + filename);
        assertThat(exception).hasCauseThat().isNull();
    }

    @Test
    public void testPackageRelativeResourceAnchor() {
        final String filename = "commonutil/InputCommonUtilTest_resource.txt";
        assertThat(ResourceUtil.getCheckstyleResource(filename))
                .isEqualTo(CommonUtil.class.getResource(filename));
        assertThat(ResourceUtil.getCheckstyleResource(filename)).isNotNull();
    }

}
