/*
 * Copyright (C) 2017 The Android Open Source Project
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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** {@link DefaultHandler} */
class XmlHandler extends DefaultHandler {
    // Root Element Tag
    public static final String PERMISSIONS_TAG = "permissions";

    // Element Tag
    /**
     * <permissions>
     *     <permission name="android.permission.BLUETOOTH_ADMIN" >
     *         <group gid="net_bt_admin" />
     *     </permission>
     *
     *     <permission name="android.permission.BLUETOOTH" >
     *         <group gid="net_bt" />
     *     </permission>
     * </permissions>
     */
    public static final String PERMISSION_TAG = "permission";
    public static final String DENY_PERMISSION_TAG = "deny-permission";

    /**
     * <permissions>
     *     <assign-permission name="android.permission.UPDATE_APP_OPS_STATS" uid="statsd" />
     * </permissions>
     */
    public static final String ASSIGN_PERMISSION_TAG = "assign-permission";

    /**
     * <permissions>
     *    <allow-in-power-save package="com.android.providers.downloads" />
     * </permissions>
     */
    public static final String ALLOW_IN_POWER_SAVE_TAG = "allow-in-power-save";

    /**
     * <permissions>
     *    <allow-in-power-save-except-idle package="com.android.providers.calendar" />
     * </permissions>
     */
    public static final String ALLOW_IN_POWER_SAVE_EXCEPT_IDLE_TAG = "allow-in-power-save-except-idle";

    /**
     * <permissions>
     *    <allow-in-data-usage-save package="com.android.providers.downloads" />
     * </permissions>
     */
    public static final String ALLOW_IN_DATA_USAGE_SAVE_TAG = "allow-in-data-usage-save";

    /**
     * <permissions>
     *   <system-user-whitelisted-app package="com.android.settings" />
     * </permissions>
     */
    public static final String SYSTEM_USER_ALISTED_APP_TAG = "system-user-whitelisted-app";

    /**
     * <permissions>
     *    <system-user-blacklisted-app package="com.android.wallpaper.livepicker" />
     * </permissions>
     */
    public static final String SYSTEM_USER_BLISTED_APP_TAG = "system-user-blacklisted-app";

    /**
     * <permissions>
     *     <feature name="android.hardware.vulkan.version" version="4198400" />
     * </permissions>
     */
    public static final String FEATURE_TAG = "feature";

    /**
     * <permissions>
     *     <library name="android.test.base"
     *             file="/system/framework/android.test.base.jar" />
     * </permissions>
     */
    public static final String LIBRARY_TAG = "library";

    /**
     * <permissions>
     *     <privapp-permissions package="android.ext.services">
     *         <permission name="android.permission.PROVIDE_RESOLVER_RANKER_SERVICE" />
     *         <permission name="android.permission.MONITOR_DEFAULT_SMS_PACKAGE" />
     *     </privapp-permissions>
     * </permissions>
     */
    public static final String PRIVAPP_PERMISSIONS_TAG = "privapp-permissions";

    /**
     * <permissions>
     *      <split-permission name="com.google.android.gms.permission.ACTIVITY_RECOGNITION"
     *                       targetSdk="29">
     *         <new-permission name="android.permission.ACTIVITY_RECOGNITION" />
     *     </split-permission>
     * </permissions>
     */
    public static final String SPLIT_PERMISSIONS_TAG = "split-permission";
    public static final String NEW_PERMISSION_TAG = "new-permission";

    // Attribute Tag
    private static final String NAME_TAG = "name";
    private static final String GID_TAG = "gid";
    private static final String UID_TAG = "uid";
    private static final String GROUP_TAG = "group";
    private static final String FILE_TAG = "file";
    private static final String PACKAGE_TAG = "package";
    private static final String VERSION_TAG = "version";
    private static final String TARGET_SDK_TAG = "targetSdk";

    private String mFileName;
    private HashMap<String, PermissionList> mPermissions;
    private Permission.Builder mPermissionBuilder;
    private PermissionList.Builder mPermissionListBuilder;
    private Permission.Builder mPrivPermissionBuilder;
    private PermissionList.Builder mPrivPermissionListBuilder;
    private Permission.Builder mSplitPermissionBuilder;
    private PermissionList.Builder mSplitPermissionListBuilder;

    XmlHandler(String fileName) {
        mFileName = fileName;
        mPermissions = new HashMap<String, PermissionList>();
        newPermissionListBuilder();
        newPrivPermissionListBuilder();
    }

    private void newPermissionListBuilder() {
        mPermissionListBuilder = PermissionList.newBuilder();
        mPermissionListBuilder.setName(PERMISSION_TAG);
    }

    private void newPrivPermissionListBuilder() {
        mPrivPermissionListBuilder = PermissionList.newBuilder();
        mPrivPermissionListBuilder.setName(PRIVAPP_PERMISSIONS_TAG);
    }

    private void newSplitPermissionListBuilder() {
        mSplitPermissionListBuilder = PermissionList.newBuilder();
        mSplitPermissionListBuilder.setName(PRIVAPP_PERMISSIONS_TAG);
    }

    public HashMap<String, PermissionList> getPermissions() {
        return mPermissions;
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes)
            throws SAXException {
        super.startElement(uri, localName, name, attributes);

        switch (localName) {
            // 1
            case PERMISSION_TAG:
            case DENY_PERMISSION_TAG:
            case ASSIGN_PERMISSION_TAG:
            case ALLOW_IN_POWER_SAVE_TAG:
            case ALLOW_IN_POWER_SAVE_EXCEPT_IDLE_TAG:
            case ALLOW_IN_DATA_USAGE_SAVE_TAG:
            case SYSTEM_USER_ALISTED_APP_TAG:
            case SYSTEM_USER_BLISTED_APP_TAG:
            case FEATURE_TAG:
            case LIBRARY_TAG:
            case SPLIT_PERMISSIONS_TAG:
                mPermissionBuilder = Permission.newBuilder();
                mPermissionBuilder.setTag(localName);
                try {
                    mPermissionBuilder.setName(attributes.getValue(NAME_TAG));
                } catch (Exception e) {
                    // allow-in-power-save, etc. have package instead of name
                    mPermissionBuilder.setName(attributes.getValue(PACKAGE_TAG));
                }
                // processes attributes
                // uid for assign-permission
                addEle(UID_TAG, attributes);
                // version for feature
                addEle(VERSION_TAG, attributes);
                // file for library
                addEle(FILE_TAG, attributes);
                // targetSdk for library
                addEle(TARGET_SDK_TAG, attributes);
                break;
            // 2
            case GROUP_TAG:
                if (mPermissionBuilder != null) {
                    Element.Builder eleBuilder = Element.newBuilder();
                    eleBuilder.setName(GID_TAG);
                    eleBuilder.setValue(attributes.getValue(GID_TAG));
                    mPermissionBuilder.addElements(eleBuilder.build());
                }
                break;
            case NEW_PERMISSION_TAG:
                if (mPermissionBuilder != null) {
                    Element.Builder eleBuilder = Element.newBuilder();
                    eleBuilder.setName(NEW_PERMISSION_TAG);
                    eleBuilder.setValue(attributes.getValue("name"));
                    mPermissionBuilder.addElements(eleBuilder.build());
                }
                break;
            // 0.5
            case PRIVAPP_PERMISSIONS_TAG:
                mPrivPermissionBuilder = Permission.newBuilder();
                mPrivPermissionBuilder.setTag(PRIVAPP_PERMISSIONS_TAG);
                mPrivPermissionBuilder.setName(attributes.getValue(PACKAGE_TAG));
                break;
            // 0
            case PERMISSIONS_TAG:
                // start permissions tree
                break;
            default:
                getLogger().log(Level.WARNING, "File: " + mFileName);
                getLogger().log(Level.WARNING, "ToDo: to parse xml element: " + localName);
        }
    }

    private void addEle(String tag, Attributes attributes) {
        String v = attributes.getValue(tag);
        if (v != null) {
            Element.Builder eleBuilder = Element.newBuilder();
            eleBuilder.setName(tag);
            eleBuilder.setValue(v);
            mPermissionBuilder.addElements(eleBuilder.build());
        }
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
        super.endElement(uri, localName, name);
        switch (localName) {
            // 1
            case PERMISSION_TAG:
            case DENY_PERMISSION_TAG:
            case ASSIGN_PERMISSION_TAG:
            case ALLOW_IN_POWER_SAVE_TAG:
            case ALLOW_IN_POWER_SAVE_EXCEPT_IDLE_TAG:
            case ALLOW_IN_DATA_USAGE_SAVE_TAG:
            case SYSTEM_USER_ALISTED_APP_TAG:
            case SYSTEM_USER_BLISTED_APP_TAG:
            case FEATURE_TAG:
            case LIBRARY_TAG:
            case SPLIT_PERMISSIONS_TAG:
                mPermissionListBuilder.addPermissions(mPermissionBuilder.build());
                mPermissionBuilder = null;
                break;

            //0.5
            case PRIVAPP_PERMISSIONS_TAG:
                if (mPermissionListBuilder.getPermissionsList().size() > 0) {
                    convertToPrivPermission();
                    mPrivPermissionListBuilder.addPermissions(mPrivPermissionBuilder.build());
                    mPrivPermissionBuilder = null;
                    newPermissionListBuilder();
                }
                break;
            // 0
            case PERMISSIONS_TAG:
                if (mPermissionListBuilder.getPermissionsList().size() > 0) {
                    mPermissions.put(PERMISSION_TAG, mPermissionListBuilder.build());
                }
                if (mPrivPermissionListBuilder.getPermissionsList().size() > 0) {
                    mPermissions.put(PERMISSION_TAG, mPrivPermissionListBuilder.build());
                }
                break;
        }
    }

    // add permissions in mPermissionListBuilder as elements in mPrivPermissionBuilder
    private void convertToPrivPermission() {
        for (Permission per : mPermissionListBuilder.getPermissionsList()) {
            Element.Builder eleBuilder = Element.newBuilder();
            eleBuilder.setName(per.getName());
            mPrivPermissionBuilder.addElements(eleBuilder.build());
        }
    }

    private static Logger getLogger() {
        return Logger.getLogger(XmlHandler.class.getSimpleName());
    }
}
