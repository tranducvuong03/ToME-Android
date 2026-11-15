package org.te4.android;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.libsdl.app.SDLActivity;

public final class TE4Activity extends SDLActivity {
    private static final int SDLK_UP = 1073741906;
    private static final int SDLK_DOWN = 1073741905;
    private static final int SDLK_LEFT = 1073741904;
    private static final int SDLK_RIGHT = 1073741903;
    private static final int SDLK_KP_1 = 1073741913;
    private static final int SDLK_KP_3 = 1073741915;
    private static final int SDLK_KP_7 = 1073741919;
    private static final int SDLK_KP_9 = 1073741921;

    private HorizontalScrollView shortcutBar;
    private LinearLayout shortcutKeys;
    private final ArrayList<Shortcut> customShortcuts = new ArrayList<>();
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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
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
        loadCustomShortcuts();

        int height = dp(44);
        shortcutBar = new HorizontalScrollView(this);
        shortcutBar.setHorizontalScrollBarEnabled(false);
        shortcutBar.setBackgroundColor(0x66202020);

        shortcutKeys = new LinearLayout(this);
        shortcutKeys.setGravity(Gravity.CENTER_VERTICAL);
        shortcutKeys.setOrientation(LinearLayout.HORIZONTAL);
        shortcutBar.addView(shortcutKeys, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        renderCustomShortcuts();

        FrameLayout.LayoutParams barLayout = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height, Gravity.BOTTOM | Gravity.END);
        barLayout.leftMargin = dp(146);
        barLayout.rightMargin = dp(4);
        barLayout.bottomMargin = dp(5);
        addContentView(shortcutBar, barLayout);

        JoystickView joystick = new JoystickView(this);
        FrameLayout.LayoutParams joystickLayout = new FrameLayout.LayoutParams(
                dp(136), dp(136), Gravity.BOTTOM | Gravity.START);
        joystickLayout.leftMargin = dp(5);
        joystickLayout.bottomMargin = dp(3);
        addContentView(joystick, joystickLayout);

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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(46), dp(38));
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

    private void renderCustomShortcuts() {
        shortcutKeys.removeAllViews();
        addControl(shortcutKeys, "KB", 0, true);
        addControl(shortcutKeys, "ESC", 27, false);
        addControl(shortcutKeys, "TAB", 9, false);
        addControl(shortcutKeys, "↑", SDLK_UP, false);
        addControl(shortcutKeys, "↓", SDLK_DOWN, false);
        addControl(shortcutKeys, "←", SDLK_LEFT, false);
        addControl(shortcutKeys, "→", SDLK_RIGHT, false);

        for (final Shortcut shortcut : customShortcuts) {
            TextView control = makeControl(shortcut.label);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(44), dp(38));
            params.setMargins(dp(2), 0, dp(2), 0);
            shortcutKeys.addView(control, params);
            control.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    nativeSendKey(shortcut.keycode);
                }
            });
            control.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    confirmDeleteShortcut(shortcut);
                    return true;
                }
            });
        }

        TextView add = makeControl("+");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(44), dp(38));
        params.setMargins(dp(2), 0, dp(4), 0);
        shortcutKeys.addView(add, params);
        add.setContentDescription("Add a keyboard shortcut");
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddShortcutDialog();
            }
        });
    }

    private void showAddShortcutDialog() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setHint("Type one key");

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add shortcut")
                .setMessage("Type a key using your phone keyboard")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface ignored) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String value = input.getText().toString();
                                if (value.codePointCount(0, value.length()) != 1) {
                                    input.setError("Enter exactly one key");
                                    return;
                                }
                                int keycode = value.codePointAt(0);
                                if (Character.isISOControl(keycode)) {
                                    input.setError("That key cannot be saved");
                                    return;
                                }
                                for (Shortcut shortcut : customShortcuts) {
                                    if (shortcut.keycode == keycode) {
                                        Toast.makeText(TE4Activity.this, "Shortcut already exists",
                                                Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                }
                                Shortcut shortcut = new Shortcut(shortcutLabel(keycode), keycode);
                                customShortcuts.add(shortcut);
                                if (!saveCustomShortcuts()) {
                                    customShortcuts.remove(shortcut);
                                    Toast.makeText(TE4Activity.this, "Could not save shortcut",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                renderCustomShortcuts();
                                dialog.dismiss();
                            }
                        });
                input.requestFocus();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        dialog.show();
    }

    private void confirmDeleteShortcut(final Shortcut shortcut) {
        new AlertDialog.Builder(this)
                .setMessage("Delete shortcut \"" + shortcut.label + "\"?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int index = customShortcuts.indexOf(shortcut);
                        customShortcuts.remove(shortcut);
                        if (!saveCustomShortcuts()) {
                            customShortcuts.add(index, shortcut);
                            Toast.makeText(TE4Activity.this, "Could not save changes",
                                    Toast.LENGTH_SHORT).show();
                        }
                        renderCustomShortcuts();
                    }
                })
                .show();
    }

    private void loadCustomShortcuts() {
        String saved = getSharedPreferences("te4_controls", MODE_PRIVATE)
                .getString("shortcuts", "[]");
        try {
            JSONArray shortcuts = new JSONArray(saved);
            for (int i = 0; i < shortcuts.length(); i++) {
                int keycode = shortcuts.getInt(i);
                if (Character.isValidCodePoint(keycode) && !Character.isISOControl(keycode)) {
                    String label = shortcutLabel(keycode);
                    customShortcuts.add(new Shortcut(label, keycode));
                }
            }
        } catch (JSONException e) {
            Log.e("TE4", "Could not load saved keyboard shortcuts", e);
        }
    }

    private String shortcutLabel(int keycode) {
        return keycode == ' ' ? "SPC" : new String(Character.toChars(keycode));
    }

    private boolean saveCustomShortcuts() {
        JSONArray shortcuts = new JSONArray();
        for (Shortcut shortcut : customShortcuts) {
            shortcuts.put(shortcut.keycode);
        }
        if (!getSharedPreferences("te4_controls", MODE_PRIVATE).edit()
                .putString("shortcuts", shortcuts.toString()).commit()) {
            Log.e("TE4", "Could not save keyboard shortcuts");
            return false;
        }
        return true;
    }

    private TextView makeControl(String label) {
        TextView control = new TextView(this);
        control.setText(label);
        control.setTextColor(Color.WHITE);
        control.setTextSize(13);
        control.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        control.setGravity(Gravity.CENTER);
        control.setBackgroundColor(0x99525252);
        control.setClickable(true);
        control.setFocusable(true);
        return control;
    }

    private static final class Shortcut {
        final String label;
        final int keycode;

        Shortcut(String label, int keycode) {
            this.label = label;
            this.keycode = keycode;
        }
    }

    private static final class JoystickView extends View {
        private static final int NO_KEY = -1;
        private static final int REPEAT_DELAY_MS = 170;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Handler handler = new Handler(Looper.getMainLooper());
        private int activeKeycode = NO_KEY;
        private float knobX;
        private float knobY;

        private final Runnable repeatKey = new Runnable() {
            @Override
            public void run() {
                if (activeKeycode != NO_KEY) {
                    nativeSendKey(activeKeycode);
                    handler.postDelayed(this, REPEAT_DELAY_MS);
                }
            }
        };

        JoystickView(TE4Activity context) {
            super(context);
            setClickable(true);
            setContentDescription("Eight-direction joystick");
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) * 0.44f;
            float knobRadius = radius * 0.38f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x66303030);
            canvas.drawCircle(centerX, centerY, radius, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(0x88FFFFFF);
            canvas.drawCircle(centerX, centerY, radius, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(activeKeycode == NO_KEY ? 0x99DDDDDD : 0xCCFFFFFF);
            canvas.drawCircle(knobX == 0 && knobY == 0 ? centerX : knobX,
                    knobX == 0 && knobY == 0 ? centerY : knobY, knobRadius, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    updateDirection(event.getX(), event.getY());
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_POINTER_UP:
                    releaseDirection();
                    return true;
                default:
                    return true;
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            handler.removeCallbacks(repeatKey);
            super.onDetachedFromWindow();
        }

        private void updateDirection(float x, float y) {
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) * 0.44f;
            float dx = x - centerX;
            float dy = y - centerY;
            float distance = (float) Math.hypot(dx, dy);
            float maxKnobOffset = radius * 0.58f;
            if (distance > maxKnobOffset) {
                dx = dx * maxKnobOffset / distance;
                dy = dy * maxKnobOffset / distance;
                distance = maxKnobOffset;
            }
            knobX = centerX + dx;
            knobY = centerY + dy;

            int nextKeycode = NO_KEY;
            if (distance > radius * 0.22f) {
                int sector = Math.round((float) (Math.atan2(dy, dx) / (Math.PI / 4)));
                switch ((sector + 8) % 8) {
                    case 0: nextKeycode = SDLK_RIGHT; break;
                    case 1: nextKeycode = SDLK_KP_3; break;
                    case 2: nextKeycode = SDLK_DOWN; break;
                    case 3: nextKeycode = SDLK_KP_1; break;
                    case 4: nextKeycode = SDLK_LEFT; break;
                    case 5: nextKeycode = SDLK_KP_7; break;
                    case 6: nextKeycode = SDLK_UP; break;
                    case 7: nextKeycode = SDLK_KP_9; break;
                }
            }

            if (nextKeycode != activeKeycode) {
                handler.removeCallbacks(repeatKey);
                activeKeycode = nextKeycode;
                if (activeKeycode != NO_KEY) {
                    nativeSendKey(activeKeycode);
                    handler.postDelayed(repeatKey, REPEAT_DELAY_MS);
                }
            }
            invalidate();
        }

        private void releaseDirection() {
            handler.removeCallbacks(repeatKey);
            activeKeycode = NO_KEY;
            knobX = 0;
            knobY = 0;
            invalidate();
        }

        private float dp(float value) {
            return value * getResources().getDisplayMetrics().density;
        }
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
