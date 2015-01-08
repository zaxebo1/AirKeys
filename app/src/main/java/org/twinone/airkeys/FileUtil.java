package org.twinone.airkeys;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {

    private static final String TAG = FileUtil.class.getSimpleName();

    public static void copyAssetsFile(Context c, String source, String target) {
        AssetManager assetManager = c.getAssets();
        Log.d(TAG, "Extracting file: " + source);
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(source);
            File outFile = new File(target);

            out = new FileOutputStream(outFile);
            copyFile(in, out);
        } catch (IOException e) {
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}
