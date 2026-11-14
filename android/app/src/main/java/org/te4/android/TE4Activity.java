package org.te4.android;

import android.content.ContentUris;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.libsdl.app.SDLActivity;

public final class TE4Activity extends SDLActivity {
    private HorizontalScrollView shortcutBar;
    private boolean textInputEnabled = true;
    private TextView installStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ParcelFileDescriptor logFile = null;
        try {
            logFile = openPublicLogFile();
        } catch (IOException e) {
            Log.e("TE4", "Could not create public game log", e);
        } catch (SecurityException e) {
            Log.e("TE4", "Could not access public game log", e);
        }

        if (!new File(getFilesDir(), ".te4-data-installed").isFile()) {
            File errorMarker = new File(getFilesDir(), ".te4-data-install-error");
            if (errorMarker.exists() && !errorMarker.delete()) {
                Log.e("TE4", "Could not clear previous game data installation error");
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    installGameData();
                }
            }, "TE4 game data installer").start();
        }
        super.onCreate(savedInstanceState);
        if (logFile != null) {
            try {
                if (!nativeRedirectLog(logFile.getFd())) {
                    Log.e("TE4", "Could not redirect native output to the public game log");
                } else {
                    Log.i("TE4", "Native log: Download/Tales of Maj'Eyal/te4_log.txt");
                }
            } finally {
                try {
                    logFile.close();
                } catch (IOException e) {
                    Log.e("TE4", "Could not close the public game log descriptor", e);
                }
            }
        }
        createTouchControls();
        if (!new File(getFilesDir(), ".te4-data-installed").isFile()) {
            installStatus = makeControl("Preparing game data…");
            FrameLayout.LayoutParams statusLayout = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(52), Gravity.CENTER);
            addContentView(installStatus, statusLayout);
        }
    }

    @Override
    protected String[] getLibraries() {
        return new String[] { "SDL2", "main" };
    }

    private static native void nativeSendKey(int keycode);
    private static native void nativeSetTextInput(boolean enabled);
    private static native boolean nativeRedirectLog(int fd);

    private ParcelFileDescriptor openPublicLogFile() throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.w("TE4", "Public game logs require Android 10 or later");
            return null;
        }

        ContentResolver resolver = getContentResolver();
        Uri downloads = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        String displayName = "te4_log.txt";
        String relativePath = Environment.DIRECTORY_DOWNLOADS + "/Tales of Maj'Eyal/";
        String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=? AND "
                + MediaStore.MediaColumns.RELATIVE_PATH + "=?";
        String[] selectionArgs = { displayName, relativePath };
        Uri logUri = null;

        try (Cursor cursor = resolver.query(downloads,
                new String[] { MediaStore.MediaColumns._ID },
                selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID));
                logUri = ContentUris.withAppendedId(downloads, id);
            }
        }

        if (logUri == null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
            logUri = resolver.insert(downloads, values);
        }
        if (logUri == null) {
            throw new IOException("MediaStore did not create the public game log");
        }

        ParcelFileDescriptor descriptor = resolver.openFileDescriptor(logUri, "rwt");
        if (descriptor == null) {
            throw new IOException("MediaStore did not open the public game log");
        }
        return descriptor;
    }

    private void createTouchControls() {
        int height = dp(48);
        shortcutBar = new HorizontalScrollView(this);
        shortcutBar.setHorizontalScrollBarEnabled(false);
        shortcutBar.setBackgroundColor(0xCC202020);

        LinearLayout keys = new LinearLayout(this);
        keys.setGravity(Gravity.CENTER_VERTICAL);
        keys.setOrientation(LinearLayout.HORIZONTAL);
        shortcutBar.addView(keys, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        addControl(keys, "KB", 0, true);
        addControl(keys, "ESC", 27, false);
        addControl(keys, "ENT", 13, false);
        addControl(keys, "TAB", 9, false);
        addControl(keys, "SPC", 32, false);
        addControl(keys, "BKSP", 8, false);
        addControl(keys, "1", '1', false);
        addControl(keys, "2", '2', false);
        addControl(keys, "3", '3', false);
        addControl(keys, "4", '4', false);
        addControl(keys, "5", '5', false);
        addControl(keys, "6", '6', false);
        addControl(keys, "7", '7', false);
        addControl(keys, "8", '8', false);
        addControl(keys, "9", '9', false);
        addControl(keys, "0", '0', false);
        addControl(keys, "↑", 1073741906, false);
        addControl(keys, "↓", 1073741905, false);
        addControl(keys, "←", 1073741904, false);
        addControl(keys, "→", 1073741903, false);

        FrameLayout.LayoutParams barLayout = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height, Gravity.BOTTOM);
        addContentView(shortcutBar, barLayout);

        TextView toggle = makeControl("BAR");
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shortcutBar.setVisibility(shortcutBar.getVisibility() == View.VISIBLE
                        ? View.GONE : View.VISIBLE);
            }
        });
        FrameLayout.LayoutParams toggleLayout = new FrameLayout.LayoutParams(
                dp(56), height, Gravity.TOP | Gravity.START);
        addContentView(toggle, toggleLayout);
    }

    private void addControl(LinearLayout row, String label, final int keycode, boolean keyboard) {
        TextView control = makeControl(label);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(54), dp(42));
        params.setMargins(dp(2), 0, dp(2), 0);
        row.addView(control, params);
        control.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (keyboard) {
                    textInputEnabled = !textInputEnabled;
                    nativeSetTextInput(textInputEnabled);
                } else {
                    nativeSendKey(keycode);
                }
            }
        });
    }

    private TextView makeControl(String label) {
        TextView control = new TextView(this);
        control.setText(label);
        control.setTextColor(Color.WHITE);
        control.setTextSize(14);
        control.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        control.setGravity(Gravity.CENTER);
        control.setBackgroundColor(0xFF424242);
        control.setClickable(true);
        control.setFocusable(true);
        return control;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void installGameData() {
        File marker = new File(getFilesDir(), ".te4-data-installed");
        try {
            copyAssetTree(getAssets(), "bootstrap", new File(getFilesDir(), "bootstrap"));
            copyAssetTree(getAssets(), "game", new File(getFilesDir(), "game"));
            if (!marker.createNewFile()) {
                throw new IOException("Could not create game data marker");
            }
        } catch (IOException e) {
            Log.e("TE4", "Could not install Tales of Maj'Eyal game data", e);
            try {
                new FileOutputStream(new File(getFilesDir(), ".te4-data-install-error")).close();
            } catch (IOException markerError) {
                Log.e("TE4", "Could not record game data installation failure", markerError);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (installStatus != null) {
                        installStatus.setText("Could not install game data");
                    }
                }
            });
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (installStatus != null) installStatus.setVisibility(View.GONE);
            }
        });
    }

    private static void copyAssetTree(AssetManager assets, String assetPath, File destination)
            throws IOException {
        String[] children = assets.list(assetPath);
        if (children != null && children.length > 0) {
            if (!destination.isDirectory() && !destination.mkdirs()) {
                throw new IOException("Could not create " + destination);
            }
            for (String child : children) {
                copyAssetTree(assets, assetPath + "/" + child, new File(destination, child));
            }
            return;
        }

        File parent = destination.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        try (InputStream input = assets.open(assetPath);
             FileOutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        }
    }
}
