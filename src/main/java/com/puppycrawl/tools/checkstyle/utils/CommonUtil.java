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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Contains utility methods.
 *
 */
public final class CommonUtil {

    /** Default tab width for column reporting. */
    public static final int DEFAULT_TAB_WIDTH = 8;

    /** For cases where no tokens should be accepted. */
    public static final BitSet EMPTY_BIT_SET = new BitSet();
    /** Copied from org.apache.commons.lang3.ArrayUtils. */
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    /** Copied from org.apache.commons.lang3.ArrayUtils. */
    public static final Integer[] EMPTY_INTEGER_OBJECT_ARRAY = new Integer[0];
    /** Copied from org.apache.commons.lang3.ArrayUtils. */
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    /** Copied from org.apache.commons.lang3.ArrayUtils. */
    public static final int[] EMPTY_INT_ARRAY = new int[0];
    /** Copied from org.apache.commons.lang3.ArrayUtils. */
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    /** Copied from org.apache.commons.lang3.ArrayUtils. */
    public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    /** The extension separator. */
    private static final String EXTENSION_SEPARATOR = ".";

    /** Stop instances being created. */
    private CommonUtil() {
    }

    /**
     * Helper method to create a regular expression.
     *
     * @param pattern
     *            the pattern to match
     * @return a created regexp object
     * @throws IllegalArgumentException
     *             if unable to create Pattern object.
     */
    public static Pattern createPattern(String pattern) {
        return createPattern(pattern, 0);
    }

    /**
     * Helper method to create a regular expression with a specific flags.
     *
     * @param pattern
     *            the pattern to match
     * @param flags
     *            the flags to set
     * @return a created regexp object
     * @throws IllegalArgumentException
     *             if unable to create Pattern object.
     */
    public static Pattern createPattern(String pattern, int flags) {
        try {
            return Pattern.compile(pattern, flags);
        }
        catch (final PatternSyntaxException exc) {
            throw new IllegalArgumentException(
                "Failed to initialise regular expression " + pattern, exc);
        }
    }

    /**
     * Returns whether the file extension matches what we are meant to process.
     *
     * @param file
     *            the file to be checked.
     * @param fileExtensions
     *            files extensions, empty property in config makes it matches to all.
     * @return whether there is a match.
     */
    public static boolean matchesFileExtension(File file, String... fileExtensions) {
        boolean result = false;
        if (fileExtensions == null || fileExtensions.length == 0) {
            result = true;
        }
        else {
            // normalize extensions so all of them have a leading dot
            final String[] withDotExtensions = new String[fileExtensions.length];
            for (int index = 0; index < fileExtensions.length; index++) {
                final String extension = fileExtensions[index];
                if (extension.startsWith(EXTENSION_SEPARATOR)) {
                    withDotExtensions[index] = extension;
                }
                else {
                    withDotExtensions[index] = EXTENSION_SEPARATOR + extension;
                }
            }

            final String fileName = file.getName();
            for (final String fileExtension : withDotExtensions) {
                if (fileName.endsWith(fileExtension)) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Returns whether the specified string contains only whitespace up to the specified index.
     *
     * @param index
     *            index to check up to
     * @param line
     *            the line to check
     * @return whether there is only whitespace
     */
    public static boolean hasWhitespaceBefore(int index, String line) {
        boolean result = true;
        for (int charIndex = 0; charIndex < index; charIndex++) {
            if (!Character.isWhitespace(line.charAt(charIndex))) {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * Returns the length of a string ignoring all trailing whitespace.
     * It is a pity that there is not a trim() like
     * method that only removed the trailing whitespace.
     *
     * @param line
     *            the string to process
     * @return the length of the string ignoring all trailing whitespace
     */
    public static int lengthMinusTrailingWhitespace(String line) {
        int len = line.length();
        for (int index = len - 1; index >= 0; index--) {
            if (!Character.isWhitespace(line.charAt(index))) {
                break;
            }
            len--;
        }
        return len;
    }

    /**
     * Returns the length of a String prefix with tabs expanded.
     * Each tab is counted as the number of characters is
     * takes to jump to the next tab stop.
     *
     * @param inputString
     *            the input String
     * @param toIdx
     *            index in string (exclusive) where the calculation stops
     * @param tabWidth
     *            the distance between tab stop position.
     * @return the length of string.substring(0, toIdx) with tabs expanded.
     */
    public static int lengthExpandedTabs(String inputString,
            int toIdx,
            int tabWidth) {
        int len = 0;
        for (int idx = 0; idx < toIdx; idx++) {
            if (inputString.codePointAt(idx) == '\t') {
                len = (len / tabWidth + 1) * tabWidth;
            }
            else {
                len++;
            }
        }
        return len;
    }

    /**
     * Validates whether passed string is a valid pattern or not.
     *
     * @param pattern
     *            string to validate
     * @return true if the pattern is valid false otherwise
     */
    public static boolean isPatternValid(String pattern) {
        boolean isValid = true;
        try {
            Pattern.compile(pattern);
        }
        catch (final PatternSyntaxException ignored) {
            isValid = false;
        }
        return isValid;
    }

    /**
     * Returns base class name from qualified name.
     *
     * @param type
     *            the fully qualified name. Cannot be null
     * @return the base class name from a fully qualified name
     */
    public static String baseClassName(String type) {
        final int index = type.lastIndexOf('.');
        return type.substring(index + 1);
    }

    /**
     * Constructs a relative path between base directory and a given path.
     *
     * @param baseDirectory
     *            the base path to which given path is relativized
     * @param path
     *            the path to relativize against base directory
     * @return the relative normalized path between base directory and
     *     path or path if base directory is null.
     */
    public static String relativizePath(final String baseDirectory, final String path) {
        final String resultPath;
        if (baseDirectory == null) {
            resultPath = path;
        }
        else {
            final Path pathAbsolute = Path.of(path);
            final Path pathBase = Path.of(baseDirectory);
            resultPath = pathBase.relativize(pathAbsolute).toString();
        }
        return resultPath;
    }

    /**
     * Gets constructor of targetClass.
     *
     * @param <T> type of the target class object.
     * @param targetClass
     *            from which constructor is returned
     * @param parameterTypes
     *            of constructor
     * @return constructor of targetClass
     * @throws IllegalStateException if any exception occurs
     * @see Class#getConstructor(Class[])
     */
    public static <T> Constructor<T> getConstructor(Class<T> targetClass,
                                                    Class<?>... parameterTypes) {
        try {
            return targetClass.getConstructor(parameterTypes);
        }
        catch (NoSuchMethodException exc) {
            throw new IllegalStateException(exc);
        }
    }

    /**
     * Returns new instance of a class.
     *
     * @param <T>
     *            type of constructor
     * @param constructor
     *            to invoke
     * @param parameters
     *            to pass to constructor
     * @return new instance of class
     * @throws IllegalStateException if any exception occurs
     * @see Constructor#newInstance(Object...)
     */
    public static <T> T invokeConstructor(Constructor<T> constructor, Object... parameters) {
        try {
            return constructor.newInstance(parameters);
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException exc) {
            throw new IllegalStateException(exc);
        }
    }

    /**
     * Closes a stream re-throwing IOException as IllegalStateException.
     *
     * @param closeable
     *            Closeable object
     * @throws IllegalStateException when any IOException occurs
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch (IOException exc) {
                throw new IllegalStateException("Cannot close the stream", exc);
            }
        }
    }

    /**
     * Puts part of line, which matches regexp into given template
     * on positions $n where 'n' is number of matched part in line.
     *
     * @param template the string to expand.
     * @param lineToPlaceInTemplate contains expression which should be placed into string.
     * @param regexp expression to find in comment.
     * @return the string, based on template filled with given lines
     */
    public static String fillTemplateWithStringsByRegexp(
        String template, String lineToPlaceInTemplate, Pattern regexp) {
        final Matcher matcher = regexp.matcher(lineToPlaceInTemplate);
        String result = template;
        if (matcher.find()) {
            for (int index = 0; index <= matcher.groupCount(); index++) {
                final String group = matcher.group(index);
                if (group != null) {
                    // $n expands comment match like in Pattern.subst().
                    result = result.replaceAll("\\$" + index, Matcher.quoteReplacement(group));
                }
            }
        }
        return result;
    }

    /**
     * Returns file name without extension.
     * We do not use the method from Guava library to reduce Checkstyle's dependencies
     * on external libraries.
     *
     * @param fullFilename file name with extension.
     * @return file name without extension.
     */
    public static String getFileNameWithoutExtension(String fullFilename) {
        final String fileName = Path.of(fullFilename).toFile().getName();
        final int dotIndex = fileName.lastIndexOf('.');
        final String fileNameWithoutExtension;
        if (dotIndex == -1) {
            fileNameWithoutExtension = fileName;
        }
        else {
            fileNameWithoutExtension = fileName.substring(0, dotIndex);
        }
        return fileNameWithoutExtension;
    }

    /**
     * Returns file extension for the given file name
     * or empty string if file does not have an extension.
     * We do not use the method from Guava library to reduce Checkstyle's dependencies
     * on external libraries.
     *
     * @param fileNameWithExtension file name with extension.
     * @return file extension for the given file name
     *         or empty string if file does not have an extension.
     */
    public static String getFileExtension(String fileNameWithExtension) {
        final String fileName = Path.of(fileNameWithExtension).toFile().getName();
        final int dotIndex = fileName.lastIndexOf('.');
        final String extension;
        if (dotIndex == -1) {
            extension = "";
        }
        else {
            extension = fileName.substring(dotIndex + 1);
        }
        return extension;
    }

    /**
     * Checks whether the given string is a valid identifier.
     *
     * @param str A string to check.
     * @return true when the given string contains valid identifier.
     */
    public static boolean isIdentifier(String str) {
        boolean isIdentifier = !str.isEmpty();

        for (int index = 0; isIdentifier && index < str.length(); index++) {
            if (index == 0) {
                isIdentifier = Character.isJavaIdentifierStart(str.charAt(0));
            }
            else {
                isIdentifier = Character.isJavaIdentifierPart(str.charAt(index));
            }
        }

        return isIdentifier;
    }

    /**
     * Checks whether the given string is a valid name.
     *
     * @param str A string to check.
     * @return true when the given string contains valid name.
     */
    public static boolean isName(String str) {
        boolean isName = false;

        final String[] identifiers = str.split("\\.", -1);
        for (String identifier : identifiers) {
            isName = isIdentifier(identifier);
            if (!isName) {
                break;
            }
        }

        return isName;
    }

    /**
     * Checks if the value arg is blank by either being null,
     * empty, or contains only whitespace characters.
     *
     * @param value A string to check.
     * @return true if the arg is blank.
     */
    public static boolean isBlank(String value) {
        return Objects.isNull(value)
                || indexOfNonWhitespace(value) >= value.length();
    }

    /**
     * Method to find the index of the first non-whitespace character in a string.
     *
     * @param value the string to find the first index of a non-whitespace character for.
     * @return the index of the first non-whitespace character.
     */
    public static int indexOfNonWhitespace(String value) {
        final int length = value.length();
        int left = 0;
        while (left < length) {
            final int codePointAt = value.codePointAt(left);
            if (!Character.isWhitespace(codePointAt)) {
                break;
            }
            left += Character.charCount(codePointAt);
        }
        return left;
    }

    /**
     * Converts the Unicode code point at index {@code index} to it's UTF-16
     * representation, then checks if the character is whitespace. Note that the given
     * index {@code index} should correspond to the location of the character
     * to check in the string, not in code points.
     *
     * @param codePoints the array of Unicode code points
     * @param index the index of the character to check
     * @return true if character at {@code index} is whitespace
     */
    public static boolean isCodePointWhitespace(int[] codePoints, int index) {
        //  We only need to check the first member of a surrogate pair to verify that
        //  it is not whitespace.
        final char character = Character.toChars(codePoints[index])[0];
        return Character.isWhitespace(character);
    }

}
