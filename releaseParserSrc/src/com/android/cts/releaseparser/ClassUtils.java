/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.cts.releaseparser;

import com.android.cts.releaseparser.ReleaseProto.*;
import com.android.utils.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class ClassUtils {
    public static final char PACKAGE_SEPARATOR = '/';
    public static final char INNER_CLASS_SEPARATOR = '$';
    public static final char SPECIAL_CLASS_CHARACTER = '-';
    public static final char SPECIAL_MEMBER_SEPARATOR = '$';
    public static final char CLASS_MEMBER_SELECTOR = '.';

    public static final char TYPE_VOID = 'V';
    public static final char TYPE_BOOLEAN = 'Z';
    public static final char TYPE_BYTE = 'B';
    public static final char TYPE_CHAR = 'C';
    public static final char TYPE_SHORT = 'S';
    public static final char TYPE_INT = 'I';
    public static final char TYPE_LONG = 'J';
    public static final char TYPE_FLOAT = 'F';
    public static final char TYPE_DOUBLE = 'D';
    public static final char TYPE_CLASS_START = 'L';
    public static final char TYPE_CLASS_END = ';';
    public static final char TYPE_ARRAY = '[';
    public static final String TYPE_ARRAY_ACCESS = "[]";

    private static Map<Character, String> primitiveTypes = new HashMap<>();

    static {
        primitiveTypes.put(TYPE_VOID, "void");
        primitiveTypes.put(TYPE_BOOLEAN, "boolean");
        primitiveTypes.put(TYPE_BYTE, "byte");
        primitiveTypes.put(TYPE_CHAR, "char");
        primitiveTypes.put(TYPE_SHORT, "short");
        primitiveTypes.put(TYPE_INT, "int");
        primitiveTypes.put(TYPE_LONG, "long");
        primitiveTypes.put(TYPE_FLOAT, "float");
        primitiveTypes.put(TYPE_DOUBLE, "double");
    }

    /**
     * Gets canonical name of a class
     *
     * @param name such as: [[Lcom/foo/bar/MyClass$Inner;
     * @return canonical name as: com.foo.bar.MyClass.Inner[][]
     */
    public static String getCanonicalName(String name) {
        int arrDimension = 0;
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) == TYPE_ARRAY) {
                arrDimension++;
            } else {
                break;
            }
        }

        // test the first character.
        final char firstChar = name.charAt(arrDimension);
        if (primitiveTypes.containsKey(firstChar)) {
            name = primitiveTypes.get(firstChar);
        } else if (firstChar == TYPE_CLASS_START) {
            // omit the leading 'L' and the trailing ';'
            name = name.substring(arrDimension + 1, name.length() - 1);

            // replace '/' and '$' to '.'
            name =
                    name.replace(PACKAGE_SEPARATOR, CLASS_MEMBER_SELECTOR)
                            .replace(INNER_CLASS_SEPARATOR, CLASS_MEMBER_SELECTOR);
        }

        // add []'s, if any
        for (int i = 0; i < arrDimension; i++) {
            name += TYPE_ARRAY_ACCESS;
        }

        return name;
    }

    /**
     * Reads a file from Resource and write to a tmp file
     *
     * @param clazz the Class contins resrouce files
     * @param fileName of a resource file
     * @return the File object of a tmp file
     */
    public static File getResrouceFile(Class clazz, String fileName) throws IOException {
        // ToDo: Replace prefix
        File tempFile = File.createTempFile("tmp", fileName);
        tempFile.deleteOnExit();
        try (InputStream input = openResourceAsStream(clazz, fileName);
                OutputStream output = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
        }
        String newFilePath = tempFile.getPath().replace(tempFile.getName(), fileName);
        File newFile = new File(newFilePath);
        boolean b = tempFile.renameTo(newFile);
        return newFile;
    }


    public static String getResrouceDir(Class clazz) throws IOException {
        // create the tempDir
        String resourceDirPath;
        boolean bool = false;
        File directory = new File(System.getProperty("java.io.tmpdir"), "MainTest");
        bool = directory.mkdirs();
        FileUtils.deleteDirectoryContents(directory);
        resourceDirPath = directory.getAbsolutePath();
        directory.deleteOnExit();
        // Copy jar resource files to temp dir
        // ToDo: To copy all resource files.
        String TEST_SO_APK = "CtsJniTestCases.apk";
        File apkFile = ClassUtils.getResrouceFile(clazz, TEST_SO_APK);
        FileUtils.copyFileToDirectory(apkFile,directory);
        return resourceDirPath;
    }

    public static String getTempDir(Class clazz, String tmpDir) throws IOException {
        // create the tempDir
        String getTempDir;
        boolean bool = false;
        File directory = new File(System.getProperty("java.io.tmpdir"), tmpDir);
        bool = directory.mkdirs();
        FileUtils.deleteDirectoryContents(directory);
        getTempDir = directory.getAbsolutePath();
        directory.deleteOnExit();
        return getTempDir;
    }

    /**
     * Gets Resource file content as a String
     *
     * @param clazz the Class contins resrouce files
     * @param fileName of a resource file
     * @return a String of the resrouce file content
     */
    public static String getResrouceContentString(Class clazz, String fileName) throws IOException {
        InputStream inStream = openResourceAsStream(clazz, fileName);
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;

        try (BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(inStream, Charset.forName("UTF-8")))) {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }

        return stringBuilder.toString();
    }

    /**
     * Gets Resource file content as a String
     *
     * @param clazz the Class contins resrouce files
     * @param fileName of a resource file
     * @return a String of the resrouce file content
     */
    public static String getFileContentString(Class clazz, String fileName) throws IOException {
        File initialFile = new File(fileName);
        InputStream inStream = new FileInputStream(initialFile);
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        try (BufferedReader bufferedReader =
                     new BufferedReader(new InputStreamReader(inStream, Charset.forName("UTF-8")))) {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Gets an InputStrem of a file from Resource
     *
     * @param clazz the Class contins resrouce files
     * @param fileName of a resource file
     * @return the (@link InputStream} object of the file
     */
    public static InputStream openResourceAsStream(Class clazz, String fileName) {
        InputStream input = clazz.getResourceAsStream("/" + fileName);
        return input;
    }

    /**
     * Gets an InputStremReader of a file from Resource
     *
     * @param clazz the Class contins resrouce files
     * @param fileName of a resource file
     * @return the (@link InputStream} object of the file
     */
    public static InputStreamReader openResourceAsStreamReader(Class clazz, String fileName) {
        return new InputStreamReader(
                openResourceAsStream(clazz, fileName), Charset.forName("UTF-8"));
    }

    /**
     * Gets/new ApiClass.Builder from a map by the class name
     *
     * @param HashMap<class name, ApiClass.Builder>
     * @param Class name
     * @return ApiClass.Builder
     */
    public static ApiClass.Builder getApiClassBuilder(
            HashMap<String, ApiClass.Builder> apiClassBuilderMap, String name) {
        ApiClass.Builder builder = apiClassBuilderMap.get(name);
        if (builder == null) {
            builder = ApiClass.newBuilder().setName(ClassUtils.getCanonicalName(name));
            apiClassBuilderMap.put(name, builder);
        }
        return builder;
    }

    /**
     * Adds all ApiClass in a map to ApiPackage.Builder
     *
     * @param HashMap<class name, ApiClass.Builder>
     * @param ApiPackage.Builder
     */
    public static void addAllApiClasses(
            HashMap<String, ApiClass.Builder> apiClassBuilderMap,
            ApiPackage.Builder apiPackageBuilder) {
        apiClassBuilderMap
                .values()
                .forEach(
                        value -> {
                            apiPackageBuilder.addClasses(value.build());
                        });
    }
}
