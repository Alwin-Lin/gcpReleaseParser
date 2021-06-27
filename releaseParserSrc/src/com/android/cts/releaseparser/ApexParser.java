package com.android.cts.releaseparser;

import java.io.File;
import com.android.cts.releaseparser.ReleaseProto.*;


public class ApexParser extends ApkParser {
    public ApexParser(File file) {
        super(file);
    }

    @Override
    public Entry.EntryType getType() {
        return ReleaseProto.Entry.EntryType.APEX;
    }
}

