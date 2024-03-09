package com.android.cts.releaseparser;

import com.android.cts.releaseparser.ReleaseProto.*;
import com.google.protobuf.TextFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResolverParser extends FileParser {
    static private String ACTIVITY_RESOLVER_TABLE = "Activity Resolver Table:";
    static private String RECEIVER_RESOLVER_TABLE = "Receiver Resolver Table:";
    static private String SERVICE_RESOLVER_TABLE = "Service Resolver Table:";
    static private String PROVIDER_RESOLVER_TABLE = "Provider Resolver Table:";

    static private String FULL_MIME_TYPE = "Full MIME Types:";
    static private String BASE_MIME_TYPE = "Base MIME Types:";
    static private String WILD_MIME_TYPE = "Wild MIME Types:";
    static private String SCHEMES = "Schemes:";
    static private String NO_DATA_ACTION = "Non-Data Actions:";
    static private String MIME_TYPE_ACTION = "MIME Typed Actions:";

    private Resolver.Builder mResolverBuilder;
    private Scanner mScanner;
    private String mLine;

    static private String ACTION_VIEW = "android.intent.action.VIEW";
    private StringBuilder mActionViewStringBuilder;
    public ResolverParser(File file) {
        super(file);
    }

    public Resolver getResolverInfo() {
        if (mResolverBuilder == null) {
            mActionViewStringBuilder = new StringBuilder();
            mActionViewStringBuilder.append("package,activity,type,category\n");
            parse();
        }
        return mResolverBuilder.build();
    }
    public String getActionViewPackages() {
        getResolverInfo();
        return mActionViewStringBuilder.toString();
    }
    private static boolean isResolverTableHeader(String line) {
        if (line.startsWith(ACTIVITY_RESOLVER_TABLE)) return true;
        if (line.startsWith(RECEIVER_RESOLVER_TABLE)) return true;
        if (line.startsWith(SERVICE_RESOLVER_TABLE)) return true;
        if (line.startsWith(PROVIDER_RESOLVER_TABLE)) return true;
        return false;
    }
    private boolean isACategory(String line) {
        if (line.endsWith(FULL_MIME_TYPE)) return true;
        if (line.endsWith(BASE_MIME_TYPE)) return true;
        if (line.endsWith(WILD_MIME_TYPE)) return true;
        if (line.endsWith(SCHEMES)) return true;
        if (line.endsWith(NO_DATA_ACTION)) return true;
        if (line.endsWith(MIME_TYPE_ACTION)) return true;
        return false;
    }
    private boolean isAType(String line) {
        // "      application/pkix-cert:" to 3 groups:
        return line.startsWith("      ") && line.endsWith(":");
    }
    private String getType(String line) {
        // "      application/pkix-cert:" to 3 groups:
        //  2nd is type
        Pattern pattern = Pattern.compile("^(\\s+)(.+)(:)$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        boolean matchFound = matcher.find();
        if (! matchFound) {
            return null;
        } else {
            return matcher.group(2);
        }
    }

    private boolean isActivity(String line) {
        // "      application/pkix-cert:" to 3 groups:
        return line.startsWith("        ");
    }
    private Activity parseActivity(String line){
        // "        b617a0a com.android.certinstaller/.CertInstallerMain" to 6 groups:
        //  3rd is package & 6th is activity
        Pattern pattern = Pattern.compile("^(\\s+)(.+)(\\s)(.+)(\\/)(.+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        boolean matchFound = matcher.find();
        if (! matchFound) {
            return null;
        }
        Activity.Builder aBuilder = Activity.newBuilder();
        aBuilder.setPackage(matcher.group(4));
        aBuilder.setActivity(matcher.group(6));
        return aBuilder.build();
    }
    private void parse() {
        mResolverBuilder = ReleaseProto.Resolver.newBuilder();
        // adb shell dumpsys package com.android.phone > dumpsysPackagePhone.txt
        // adb shell dumpsys package r > dumpsysPackageR.txt
        try {
            mScanner = new Scanner(mFile);
            mLine = mScanner.nextLine();
            // Each Table: Activity Resolver Table:...
            while (isResolverTableHeader(mLine)) {
                ResolverTable.Builder aResolverTableBuilder = ResolverTable.newBuilder();
                String tableName = mLine;

                mLine = mScanner.nextLine();
                // Each Category: Full MIME Types:...
                while (isACategory(mLine)) {
                    ResolverCategory.Builder aResolverCategoryBuilder = ResolverCategory.newBuilder();
                    String categoryName = mLine.stripLeading();

                    mLine = mScanner.nextLine();
                    // Each type: application/pkix-cert:...
                    while (isAType(mLine)) {
                        ActivityList.Builder activityListBuilder = ActivityList.newBuilder();
                        String type = getType(mLine);
                        while (mScanner.hasNext()) {
                            mLine = mScanner.nextLine();
                            if(isActivity(mLine)) {
                                Activity activity = parseActivity(mLine);
                                if (activity != null) {
                                    activityListBuilder.addActivities(activity);
                                }
                            } else {
                                if (mLine.length() == 0) {
                                    // new Category or Table
                                    mLine = mScanner.nextLine();
                                }
                                break;
                            }
                        }
                        ActivityList aList = activityListBuilder.build();
                        aResolverCategoryBuilder.putTypes(type, activityListBuilder.build());

                        if (type.equals(ACTION_VIEW)) {
                           // System.out.println(ACTION_VIEW);
                           // System.out.println("package,activity");
                           for (Activity act : aList.getActivitiesList()) {
                               String str = String.format("%s,%s,%s,%s\n",
                                       act.getPackage(), act.getActivity(), type, categoryName);
                               mActionViewStringBuilder.append(str);
                               System.out.println(str);
                           }
                        }
                    }
                    aResolverTableBuilder.putResolverCategories(categoryName, aResolverCategoryBuilder.build());
                }
                mResolverBuilder.putResolverTables(tableName, aResolverTableBuilder.build());
            }
            exit("Done!");
        } catch (Exception e) {
            System.out.println("Error");
        }
    }
    private void exit( String message){
        System.out.println(message);
        mScanner.close();
        mScanner = null;
    }

    public static void writeTextFormatMessage(String outputFileName, Resolver resolver)
            throws IOException {
        if (outputFileName != null) {
            FileOutputStream txtOutput = new FileOutputStream(outputFileName);
            txtOutput.write(TextFormat.printToString(resolver).getBytes(Charset.forName("UTF-8")));
            txtOutput.flush();
            txtOutput.close();
        } else {
            System.out.println(TextFormat.printToString(resolver));
        }
    }

    public static void writeToFile(String outputFileName, String content)
            throws IOException {
        if (outputFileName != null) {
            FileOutputStream txtOutput = new FileOutputStream(outputFileName);
            txtOutput.write(content.getBytes(Charset.forName("UTF-8")));
            txtOutput.flush();
            txtOutput.close();
        } else {
            System.out.println(content);
        }
    }
    private static final String USAGE_MESSAGE =
            "Usage: java -jar releaseparser.jar "
                    + ResolverParser.class.getCanonicalName()
                    + " [-options <parameter>]...\n"
                    + "           to prase Resolver data\n"
                    + "Options:\n"
                    + "\t-i PATH\t The file path of the file to be parsed.\n"
                    + "\t-of PATH\t The file path of the output file instead of printing to System.out.\n";

    public static void main(String[] args) {
        try {
            ArgumentParser argParser = new ArgumentParser(args);
            String fileName = argParser.getParameterElement("i", 0);
            String outputFileName = argParser.getParameterElement("of", 0);

            File aFile = new File(fileName);
            ResolverParser aParser = new ResolverParser(aFile);
            Resolver aResolver = aParser.getResolverInfo();
            writeTextFormatMessage(outputFileName, aResolver);
            writeToFile(outputFileName+".csv", aParser.getActionViewPackages());
        } catch (Exception ex) {
            System.out.println(USAGE_MESSAGE);
            ex.printStackTrace();
        }
    }
}