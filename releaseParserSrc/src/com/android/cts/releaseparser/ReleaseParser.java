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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class ReleaseParser{
    private static final String ROOT_FOLDER_TAG = "/";
    // configuration option
    private static final String NOT_SHARDABLE_TAG = "not-shardable";
    // test class option
    private static final String RUNTIME_HIT_TAG = "runtime-hint";
    // com.android.tradefed.testtype.AndroidJUnitTest option
    private static final String PACKAGE_TAG = "package";
    // com.android.compatibility.common.tradefed.testtype.JarHostTest option
    private static final String JAR_NAME_TAG = "jar";
    // com.android.tradefed.testtype.GTest option
    private static final String NATIVE_TEST_DEVICE_PATH_TAG = "native-test-device-path";
    private static final String MODULE_TAG = "module-name";

    private static final String SUITE_API_INSTALLER_TAG =
            "com.android.tradefed.targetprep.suite.SuiteApkInstaller";
    private static final String JAR_HOST_TEST_TAG =
            "com.android.compatibility.common.tradefed.testtype.JarHostTest";
    // com.android.tradefed.targetprep.suite.SuiteApkInstaller option
    private static final String TEST_FILE_NAME_TAG = "test-file-name";
    // com.android.compatibility.common.tradefed.targetprep.FilePusher option
    private static final String PUSH_TAG = "push";

    // test class
    private static final String ANDROID_JUNIT_TEST_TAG =
            "com.android.tradefed.testtype.AndroidJUnitTest";

    private static final String TESTCASES_FOLDER_FORMAT = "testcases/%s";

    private final String mFolderPath;
    private Path mRootPath;
    private final String mFilter;
    private ReleaseContent.Builder mRelContentBuilder;
    private Map<String, Entry> mEntries;
    private Collection<PermissionList> mPermissionList;
    private List<Service> mServiceList;

    ReleaseParser(String folder) {
        this(folder, null);
    }

    ReleaseParser(String folder, String filter) {
        mFolderPath = folder;
        mFilter = filter;
        File fFile = new File(mFolderPath);
        mRootPath = Paths.get(fFile.getAbsolutePath());
        mEntries = new HashMap<String, Entry>();
    }

    public String getReleaseName() throws IOException {
        ReleaseContent relContent = getReleaseContent();
        return getReleaseName(relContent);
    }

    public static String getReleaseName(ReleaseContent relContent) {
        return String.format(
                "%s-%s-%s",
                relContent.getFullname(), relContent.getVersion(), relContent.getBuildNumber());
    }

    public String getReleaseId() throws IOException {
        ReleaseContent releaseContent = getReleaseContent();
        return releaseContent.getReleaseId();
    }

    // Get ReleaseContent, if null, parse release first
    public ReleaseContent getReleaseContent() throws IOException {
        if (mRelContentBuilder == null) {
            mRelContentBuilder = ReleaseContent.newBuilder();
            // default APP_DISTRIBUTION_PACKAGE if no BUILD_PROP nor TEST_SUITE_TRADEFED is found
            mRelContentBuilder.setReleaseType(ReleaseType.APP_DISTRIBUTION_PACKAGE);
            // also add the root folder entry
            Entry.Builder fBuilder = parseFolderWithFilter();

            // Set Rel name
            String fingerPrint = getBuildFingerPrint();
            if (fingerPrint == null){
                System.err.println("Release Name unknown!");
                File file = new File(mFolderPath);
                String name = file.getName();
                mRelContentBuilder.setName(name);
                mRelContentBuilder.setFullname(name);
                mRelContentBuilder.setReleaseId(name);
            } else {
                mRelContentBuilder.setReleaseId(fingerPrint);
                mRelContentBuilder.setReleaseType(ReleaseType.DEVICE_BUILD);
                mRelContentBuilder.setName(getName());
                mRelContentBuilder.setFullname(getFullName());
                mRelContentBuilder.setBuildNumber(getBuildNumber());
                mRelContentBuilder.setVersion(getVersion());
            }
            fBuilder.setRelativePath(ROOT_FOLDER_TAG);
            String relName = getReleaseName(mRelContentBuilder.build());
            fBuilder.setName(relName);
            mRelContentBuilder.setReleaseId(mRelContentBuilder.getReleaseId());
            mRelContentBuilder.setContentId(fBuilder.getContentId());
            mRelContentBuilder.setSize(fBuilder.getSize());
            Entry fEntry = fBuilder.build();
            mEntries.put(fEntry.getRelativePath(), fEntry);
            mRelContentBuilder.putAllEntries(mEntries);
        }
        return mRelContentBuilder.build();
    }

    // Parse folder with filter
    private Entry.Builder parseFolderWithFilter() throws IOException {
        if (mFilter == null || mFilter.isEmpty()){
            return parseFolder(mFolderPath);
        } else {
            String [] children = mFilter.split(" ");
            List<File> fileList = new ArrayList<File>();
            for (String child: children) {
                File file = new File(mFolderPath, child);
                fileList.add(file);
            }
            File [] fileArray = fileList.toArray(new File[0]);
            return parseFolder(fileArray, "");
        }
    }

    // Parse all files in a folder and return the foler entry builder
    private Entry.Builder parseFolder(String fPath) throws IOException {
        File folder = new File(fPath);
        Path folderPath = Paths.get(folder.getAbsolutePath());
        String folderRelativePath = mRootPath.relativize(folderPath).toString();
        File[] fileList = folder.listFiles();
        return parseFolder(fileList, folderRelativePath);
    }

    private Entry.Builder parseFolder(File[] fileList, String folderRelativePath) throws IOException {
        Entry.Builder folderEntry = Entry.newBuilder();
        Long folderSize = 0L;
        List<Entry> entryList = new ArrayList<Entry>();

        // walks through all files
        for (File file : fileList) {
            if (file.isDirectory() && isNotSymbolicLink(file)){
                // Parse every file in list
                Entry.Builder subFolderEntry = parseFolder(file.getAbsolutePath());
                if (folderRelativePath.isEmpty()) {
                    subFolderEntry.setParentFolder(ROOT_FOLDER_TAG);
                } else {
                    subFolderEntry.setParentFolder(folderRelativePath);
                }
                Entry sfEntry = subFolderEntry.build();
                entryList.add(sfEntry);
                mEntries.put(sfEntry.getRelativePath(), sfEntry);
                folderSize += sfEntry.getSize();
            } else {
                // it's a file
                String fileRelativePath =
                        mRootPath.relativize(Paths.get(file.getAbsolutePath())).toString();
                FileParser fParser = FileParser.getParser(file);
                Entry.Builder fileEntryBuilder = fParser.getFileEntryBuilder();
                fileEntryBuilder.setRelativePath(fileRelativePath);

                if (folderRelativePath.isEmpty()) {
                    fileEntryBuilder.setParentFolder(ROOT_FOLDER_TAG);
                } else {
                    fileEntryBuilder.setParentFolder(folderRelativePath);
                }

                Entry.EntryType eType = fParser.getType();
                switch (eType) {
                    case TEST_SUITE_TRADEFED:
                        mRelContentBuilder.setTestSuiteTradefed(fileRelativePath);
                        TestSuiteTradefedParser tstParser = (TestSuiteTradefedParser) fParser;
                        // get [cts]-known-failures.xml
                        mRelContentBuilder.addAllKnownFailures(tstParser.getKnownFailureList());
                        mRelContentBuilder.setName(tstParser.getName());
                        mRelContentBuilder.setFullname(tstParser.getFullName());
                        mRelContentBuilder.setBuildNumber(tstParser.getBuildNumber());
                        mRelContentBuilder.setTargetArch(tstParser.getTargetArch());
                        mRelContentBuilder.setVersion(tstParser.getVersion());
                        mRelContentBuilder.setReleaseType(ReleaseType.TEST_SUITE);
                        break;
                    case BUILD_PROP:
                        BuildPropParser bpParser = (BuildPropParser) fParser;
                       mRelContentBuilder.putAllProperties(bpParser.getProperties());
                        break;
                    default:
                }
                // System.err.println("File:" + file.getAbsoluteFile());
                if (fParser.getDependencies() != null) {
                    fileEntryBuilder.addAllDependencies(fParser.getDependencies());
                }
                if (fParser.getDynamicLoadingDependencies() != null) {
                    fileEntryBuilder.addAllDynamicLoadingDependencies(
                            fParser.getDynamicLoadingDependencies());
                }
                fileEntryBuilder.setAbiBits(fParser.getAbiBits());
                fileEntryBuilder.setAbiArchitecture(fParser.getAbiArchitecture());

                Entry fEntry = fileEntryBuilder.build();
                entryList.add(fEntry);
                mEntries.put(fEntry.getRelativePath(), fEntry);
                folderSize += file.length();
            }
        }
        folderEntry.setName(folderRelativePath);
        folderEntry.setSize(folderSize);
        folderEntry.setType(Entry.EntryType.FOLDER);
        folderEntry.setContentId(getFolderContentId(folderEntry, entryList));
        folderEntry.setRelativePath(folderRelativePath);
        return folderEntry;
    }

    private static String getFolderContentId(Entry.Builder folderEntry, List<Entry> entryList) {
        String id = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (Entry entry : entryList) {
                md.update(entry.getContentId().getBytes(StandardCharsets.UTF_8));
            }
            // Converts to Base64 String
            id = Base64.getEncoder().encodeToString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("NoSuchAlgorithmException:" + e.getMessage());
        }
        return id;
    }

    // Writes file list CSV file
    public void writeFileListCsvFile(String fingerPrint, String csvFile) {
        try {
            FileWriter fileWriter = new FileWriter(csvFile);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            // Header
            printWriter.printf(
                    "build_fingerprint,type,name,size,relative_path,content_id,parent_folder,architecture,bits\n");
            for (Entry entry : getFileEntries()) {
                printWriter.printf(
                        "%s,%s,%s,%d,%s,%s,%s,%s,%d\n",
                        fingerPrint,
                        entry.getType(),
                        entry.getName(),
                        entry.getSize(),
                        entry.getRelativePath(),
                        entry.getContentId(),
                        entry.getParentFolder(),
                        entry.getAbiArchitecture(),
                        entry.getAbiBits());
            }
            printWriter.flush();
            printWriter.close();
        } catch (IOException e) {
            System.err.println("IPExpectation" + e.getMessage());
        }
    }

    // writes release content to a CSV file
    public void writeReleaseContentCstFile(String relNameVer, String csvFile) {
        try {
            FileWriter fWriter = new FileWriter(csvFile);
            PrintWriter pWriter = new PrintWriter(fWriter);
            // Header
            pWriter.printf(
                "release,type,name,size,path,content_id,parent_folder,code_id,architecture,bits,dependencies,dynamic_loading_dependencies,services,package\n");
            for (Entry entry : getFileEntries()) {
                String pkgName = "";
                if (entry.getType() == Entry.EntryType.APK) {
                    pkgName = entry.getAppInfo().getPackageName();
                }
                pWriter.printf(
                        "%s,%s,%s,%d,%s,%s,%s,%s,%s,%d,%s,%s,%s,%s\n",
                        relNameVer,
                        entry.getType(),
                        entry.getName(),
                        entry.getSize(),
                        entry.getRelativePath(),
                        entry.getContentId(),
                        entry.getParentFolder(),
                        entry.getCodeId(),
                        entry.getAbiArchitecture(),
                        entry.getAbiBits(),
                        String.join(" ", entry.getDependenciesList()),
                        String.join(" ", entry.getDynamicLoadingDependenciesList()),
                        RcParser.toString(entry.getServicesList()),
                        pkgName);
            }
            pWriter.flush();
            pWriter.close();
        } catch (IOException e) {
            System.err.println("IOException:" + e.getMessage());
        }
    }

    // Writes apk content to save as CSV file
    public void writeExeListCsvFile(String fingerPrint, String csvFile) {
        // Find the apk directory
        // Feed location to this
        try {
            FileWriter fWriter = new FileWriter(csvFile);
            PrintWriter pWriter = new PrintWriter(fWriter);
            // Header
            pWriter.printf(
                "build_fingerprint,type,name,package,version,folder,size,bit,so_name,so_size\n");
            // Iterate all file entry in a release
            for (Entry entry : getFileEntries()) {
                Entry.EntryType type =entry.getType();
                String pkgName = "";
                String version = "";
                int bit = -1;
                AppInfo appinfo;
                List<String> subPkgs = new ArrayList<String>();

                switch (type) {
                    case APK:
                    case APEX:
                        appinfo = entry.getAppInfo();
                        pkgName = appinfo.getPackageName();
                        version = appinfo.getVersionName();

                        // Get name and size
                        Collection<Entry> subEntries = appinfo.getPackageFileContent().getEntriesMap().values();
                        for (Entry subEntry: subEntries) {
                            if(subEntry.getName().endsWith(".so")) {
                                subPkgs.add(
                                        String.format(
                                                "%d,%s,%d",
                                                subEntry.getAbiBits(),
                                                subEntry.getName(),
                                                subEntry.getSize()));
                            }
                        }
                        break;
                    case EXE:
                        pkgName = entry.getName();
                        break;
                    default:
                        continue;
                }

                if (version.isEmpty()) {
                    version = entry.getContentId();
                }
                String exeCsv = String.format("%s,%s,%s,%s,%s,%s,,%d",
                        fingerPrint,
                        type,
                        entry.getName(),
                        pkgName,
                        version,
                        entry.getParentFolder(),
                        entry.getSize());
                if (subPkgs.isEmpty()) {
                    pWriter.printf("%s,%d,\n",
                            exeCsv,
                            entry.getAbiBits());
                } else {
                    for(String subpkgCsv : subPkgs) {
                        pWriter.printf("%s,%s\n",
                                exeCsv,
                                subpkgCsv);
                    }
                }
            }

            pWriter.flush();
            pWriter.close();
        } catch (IOException e) {
            System.err.println("IOException:" + e.getMessage());
        }
    }

    // Writes app permission and save as CSV
    public void writeFeatureListCsvFile (String fingerprint,String csvFile){
        try {
            FileWriter fWriter = new FileWriter(csvFile);
            PrintWriter pWriter = new PrintWriter(fWriter);
            // Header
            pWriter.printf(
                    "build_fingerprint,name,eleName,eleValue,path\n");
            for (Entry entry : getFileEntries()) {
                if (entry.getType() == Entry.EntryType.XML) {
                    Collection<PermissionList> plc = entry.getDevicePermissionsMap().values();
                    for (PermissionList pl: plc) {
                        for (Permission perm: pl.getPermissionsList()) {
                            if(perm.getElementsList().isEmpty()){
                                pWriter.printf(
                                        "%s,%s,,,%s\n",
                                        fingerprint,
                                        perm.getName(),
                                        entry.getRelativePath());
                            } else {
                                for (Element ele: perm.getElementsList()) {
                                    pWriter.printf(
                                            "%s,%s,%s,%s,%s\n",
                                            fingerprint,
                                            perm.getName(),
                                            ele.getName(),
                                            ele.getValue(),
                                            entry.getRelativePath());
                                }
                            }
                        }
                    }
                }
            }
            pWriter.flush();
            pWriter.close();
        } catch (IOException e) {
            System.err.println("IOException:" + e.getMessage());
        }
    }

    // Writes properties and save as CSV
    public void writePropertiesCsvFile (String relFolder, String fingerprint, String csvFile){
        try {
            //Write into csvFile
            FileWriter fWriter = new FileWriter(csvFile);
            PrintWriter pWriter = new PrintWriter(fWriter);
            // Header
            pWriter.printf("build_fingerprint,properties,values\n");
            Map <String ,String> propertyMap = getProperties();
            for (Map.Entry<String ,String> entry: propertyMap.entrySet()){
                pWriter.printf(
                        "%s,%s,%s\n",
                        fingerprint,
                        entry.getKey(),
                        entry.getValue()
                );
            }
            pWriter.flush();
            pWriter.close();
        } catch (IOException e) {
            System.err.println("IOException:" + e.getMessage());
        }
    }

    // writes known failures to a CSV file
    public void writeKnownFailureCsvFile(String relNameVer, String csvFile) throws IOException {
        ReleaseContent relContent = getReleaseContent();
        if (relContent.getKnownFailuresList().size() == 0) {
            // Skip if no Known Failures
            return;
        }

        try {
            FileWriter fWriter = new FileWriter(csvFile);
            PrintWriter pWriter = new PrintWriter(fWriter);
            //Header
            pWriter.printf("release,compatibility:exclude-filter\n");
            for (String kf : relContent.getKnownFailuresList()) {
                pWriter.printf("%s,%s\n", relNameVer, kf);
            }
            pWriter.flush();
            pWriter.close();
        } catch (IOException e) {
            System.err.println("IOException:" + e.getMessage());
        }
    }

    // Writes services and save as CSV
    public void writeServiceCsvFile (String fingerprint, String csvFile){
        try {
            //Write into csvFile
            FileWriter fWriter = new FileWriter(csvFile);
            PrintWriter pWriter = new PrintWriter(fWriter);
            // Header
            pWriter.printf("fingerprint,properties,values\n");
            for (Service services: getServiceList()){
                pWriter.printf(
                        "%s,%s\n",
                        fingerprint,
                        services
                );
            }
            pWriter.flush();
            pWriter.close();
        } catch (IOException e) {
            System.err.println("IOException:" + e.getMessage());
        }
    }


    public Collection<Entry> getFileEntries() throws IOException {
        return getReleaseContent().getEntries().values();
    }

    public Map<String, String> getProperties() throws IOException {
        return getReleaseContent().getPropertiesMap();
    }

    public void getPermissions() throws IOException {
        Collection<Entry> collectionEntry = getReleaseContent().getEntriesMap().values();
        for (Entry entry: collectionEntry){
            Map<String, PermissionList> permissionList = entry.getDevicePermissionsMap();
            mPermissionList = permissionList.values();
        }
    }

    public Collection<PermissionList> getPermissionList() throws IOException {
        getPermissions();
        return mPermissionList;
    }

    public void getService() throws IOException {
        Collection<Entry> collectionEntry = getReleaseContent().getEntriesMap().values();
        for (Entry entry: collectionEntry){
            List<Service> servicesList =entry.getServicesList();
            mServiceList = servicesList;
        }
    }

    public List<Service> getServiceList() throws IOException {
        getService();
        return mServiceList;
    }

    public boolean isNotSymbolicLink(File file) {
        return ! Files.isSymbolicLink(file.toPath());
    }

    private String getBuildFingerPrint() {
        String fingerPrint = mRelContentBuilder.getPropertiesMap().get("ro.production.fingerprint");
        if (fingerPrint == null) {
            fingerPrint = mRelContentBuilder.getPropertiesMap().get("ro.system.build.fingerprint");
        }
        if (fingerPrint == null) {
            fingerPrint = mRelContentBuilder.getPropertiesMap().get("ro.build.fingerprint");
        }
        return fingerPrint;
    }

    private String getBuildNumber() {
        return mRelContentBuilder.getPropertiesMap().get("ro.build.version.incremental");
    }

    private String getVersion() {
        String version = mRelContentBuilder.getPropertiesMap().get("ro.production.id");
        if (version == null) {
            version = mRelContentBuilder.getPropertiesMap().get("ro.build.id");
        }
        return version;
    }

    private String getName() {
        String name = mRelContentBuilder.getPropertiesMap().get("ro.product.device");
        if (name == null) {
            name = mRelContentBuilder.getPropertiesMap().get("ro.build.product");
        }
        return name;
    }

    private String getFullName() {
        return mRelContentBuilder.getPropertiesMap().get("ro.build.flavor");
    }
}