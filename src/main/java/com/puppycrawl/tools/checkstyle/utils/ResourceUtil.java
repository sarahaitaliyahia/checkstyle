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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.xml.sax.InputSource;

import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

/**
 * Utility methods for resolving URLs, files, and classpath resources.
 */
public final class ResourceUtil {

    /** Pseudo URL protocol for loading from the class path. */
    public static final String CLASSPATH_URL_PROTOCOL = "classpath:";

    /** Prefix for the exception when unable to find resource. */
    private static final String UNABLE_TO_FIND_EXCEPTION_PREFIX = "Unable to find: ";

    /** Stop instances being created. */
    private ResourceUtil() {
    }

    /**
     * Creates an input source from a file.
     *
     * @param filename name of the file.
     * @return input source.
     * @throws CheckstyleException if an error occurs.
     */
    public static InputSource sourceFromFilename(String filename) throws CheckstyleException {
        // figure out if this is a File or a URL
        final URI uri = getUriByFilename(filename);
        return new InputSource(uri.toASCIIString());
    }

    /**
     * Resolve the specified filename to a URI.
     *
     * @param filename name of the file
     * @return resolved file URI
     * @throws CheckstyleException on failure
     */
    public static URI getUriByFilename(String filename) throws CheckstyleException {
        URI uri = getWebOrFileProtocolUri(filename);

        if (uri == null) {
            uri = getFilepathOrClasspathUri(filename);
        }

        return uri;
    }

    /**
     * Resolves the specified filename containing 'http', 'https', 'ftp',
     * and 'file' protocols (or any RFC 2396 compliant URL) to a URI.
     *
     * @param filename name of the file
     * @return resolved file URI or null if URL is malformed or non-existent
     */
    public static URI getWebOrFileProtocolUri(String filename) {
        URI uri;
        try {
            uri = URI.create(filename);
            uri = uri.toURL().toURI();
        }
        catch (IllegalArgumentException | MalformedURLException | URISyntaxException ignored) {
            uri = null;
        }
        return uri;
    }

    /**
     * Resolves the specified local filename, possibly with 'classpath:'
     * protocol, to a URI.  First we attempt to create a new file with
     * given filename, then attempt to load file from class path.
     *
     * @param filename name of the file
     * @return resolved file URI
     * @throws CheckstyleException on failure
     */
    private static URI getFilepathOrClasspathUri(String filename)
            throws CheckstyleException {
        final URI uri;
        if (filename.startsWith(CLASSPATH_URL_PROTOCOL)) {
            uri = getResourceFromClassPath(
                filename.substring(CLASSPATH_URL_PROTOCOL.length()));
        }
        else {
            final Path path = Path.of(filename);

            if (Files.exists(path)) {
                uri = path.toFile().toURI();
            }
            else {
                uri = getResourceFromClassPath(filename);
            }
        }
        return uri;
    }

    /**
     * Gets a resource from the classpath.
     *
     * @param filename name of file
     * @return URI of file in classpath
     * @throws CheckstyleException on failure
     */
    public static URI getResourceFromClassPath(String filename) throws CheckstyleException {
        final URL configUrl;
        if (filename.charAt(0) == '/') {
            configUrl = getCheckstyleResource(filename);
        }
        else {
            configUrl = ClassLoader.getSystemResource(filename);
        }

        if (configUrl == null) {
            throw new CheckstyleException(UNABLE_TO_FIND_EXCEPTION_PREFIX + filename);
        }

        final URI uri;
        try {
            uri = configUrl.toURI();
        }
        catch (final URISyntaxException exc) {
            throw new CheckstyleException(UNABLE_TO_FIND_EXCEPTION_PREFIX + filename, exc);
        }

        return uri;
    }

    /**
     * Finds a resource with a given name in the Checkstyle resource bundle.
     * This method is intended only for internal use in Checkstyle tests for
     * easy mocking to gain 100% coverage.
     *
     * @param name name of the desired resource
     * @return URI of the resource
     */
    public static URL getCheckstyleResource(String name) {
        // Preserve the original class loader and package anchor for resource lookup.
        return CommonUtil.class.getResource(name);
    }

}
