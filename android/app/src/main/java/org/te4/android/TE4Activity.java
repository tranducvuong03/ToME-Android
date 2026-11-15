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
import android.view.ViewConfiguration;
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
    private static final int SDLK_RETURN = 13;
    private static final int SDL_BUTTON_LEFT = 1;
    private static final int SDL_BUTTON_RIGHT = 3;

    private HorizontalScrollView shortcutBar;
    private LinearLayout shortcutKeys;
    private DirectionPadView directionPad;
    private View barToggle;
    private final ArrayList<Shortcut> customShortcuts = new ArrayList<>();
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

    @Override
    public void setOrientationBis(int width, int height, boolean resizable, String hint) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    private static native void nativeSendKey(int keycode);
    private static native void nativeSendMouseButton(int button);
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
        restoreOverlayWhenLaidOut(shortcutBar, "shortcut_bar", true);
        attachDragHandle(shortcutBar, shortcutBar, "shortcut_bar", true);
        shortcutBar.setContentDescription(
                "Shortcut bar; hold a built-in key or blank area to move the bar");

        directionPad = new DirectionPadView(this);
        FrameLayout.LayoutParams directionPadLayout = new FrameLayout.LayoutParams(
                dp(144), dp(144), Gravity.BOTTOM | Gravity.START);
        directionPadLayout.leftMargin = dp(5);
        directionPadLayout.bottomMargin = dp(3);
        addContentView(directionPad, directionPadLayout);
        directionPad.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (directionPad.getWidth() > 0 && directionPad.getHeight() > 0) {
                    directionPad.removeOnLayoutChangeListener(this);
                    directionPad.restorePosition();
                }
            }
        });

        barToggle = makeControl("BAR");
        barToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shortcutBar.setVisibility(shortcutBar.getVisibility() == View.VISIBLE
                        ? View.GONE : View.VISIBLE);
            }
        });
        attachDragHandle(barToggle, barToggle, "bar_toggle", false);
        barToggle.setContentDescription("Show or hide the shortcut bar; hold and drag to move");
        FrameLayout.LayoutParams toggleLayout = new FrameLayout.LayoutParams(
                dp(56), height, Gravity.TOP | Gravity.START);
        addContentView(barToggle, toggleLayout);
        restoreOverlayWhenLaidOut(barToggle, "bar_toggle", false);
    }

    private void addControl(LinearLayout row, String label, final int keycode, boolean keyboard) {
        TextView control = makeControl(label);
        if (keyboard) {
            control.setTextSize(20);
            control.setContentDescription("Toggle on-screen keyboard");
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(46), dp(38));
        params.setMargins(dp(2), 0, dp(2), 0);
        row.addView(control, params);
        control.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (keyboard) {
                    boolean showKeyboard = !SDLActivity.isScreenKeyboardShown();
                    nativeSetTextInput(showKeyboard);
                    if (showKeyboard) SDLActivity.showTextInput(0, 0, 1, 1);
                } else {
                    nativeSendKey(keycode);
                }
            }
        });
        attachDragHandle(control, shortcutBar, "shortcut_bar", true);
    }

    private void renderCustomShortcuts() {
        shortcutKeys.removeAllViews();
        addControl(shortcutKeys, "⌨", 0, true);
        addControl(shortcutKeys, "ESC", 27, false);
        addControl(shortcutKeys, "TAB", 9, false);
        addControl(shortcutKeys, "↑", SDLK_UP, false);
        addControl(shortcutKeys, "↓", SDLK_DOWN, false);
        addControl(shortcutKeys, "←", SDLK_LEFT, false);
        addControl(shortcutKeys, "→", SDLK_RIGHT, false);
        addMouseControl(shortcutKeys, "LMB", SDL_BUTTON_LEFT);
        addMouseControl(shortcutKeys, "RMB", SDL_BUTTON_RIGHT);

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

    private void addMouseControl(LinearLayout row, String label, final int button) {
        TextView control = makeControl(label);
        control.setContentDescription(button == SDL_BUTTON_LEFT
                ? "Left mouse click" : "Right mouse click");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(46), dp(38));
        params.setMargins(dp(2), 0, dp(2), 0);
        row.addView(control, params);
        control.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nativeSendMouseButton(button);
            }
        });
        attachDragHandle(control, shortcutBar, "shortcut_bar", true);
    }

    private void attachDragHandle(View handle, View target, String preferenceKey,
            boolean expandToRight) {
        handle.setOnTouchListener(
                new OverlayDragListener(handle, target, preferenceKey, expandToRight));
    }

    private void restoreOverlayWhenLaidOut(final View target, final String preferenceKey,
            final boolean expandToRight) {
        target.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (target.getWidth() > 0 && target.getHeight() > 0) {
                    target.removeOnLayoutChangeListener(this);
                    restoreOverlayPosition(target, preferenceKey, expandToRight);
                }
            }
        });
    }

    private void restoreOverlayPosition(View target, String preferenceKey, boolean expandToRight) {
        ViewGroup parent = (ViewGroup) target.getParent();
        if (!(parent instanceof FrameLayout) || parent.getWidth() == 0 || parent.getHeight() == 0) {
            return;
        }

        android.content.SharedPreferences preferences =
                getSharedPreferences("te4_controls", MODE_PRIVATE);
        String xKey = preferenceKey + "_x";
        String yKey = preferenceKey + "_y";
        if (!preferences.contains(xKey) || !preferences.contains(yKey)) return;

        int maxX = getOverlayMaxX(target, parent, expandToRight);
        int maxY = Math.max(0, parent.getHeight() - target.getHeight());
        float xFraction = Math.max(0f, Math.min(1f, preferences.getFloat(xKey, 0f)));
        float yFraction = Math.max(0f, Math.min(1f, preferences.getFloat(yKey, 0f)));
        moveOverlay(target, maxX * xFraction, maxY * yFraction, expandToRight);
    }

    private int getOverlayMaxX(View target, ViewGroup parent, boolean expandToRight) {
        if (expandToRight) return Math.max(0, parent.getWidth() - dp(124));
        return Math.max(0, parent.getWidth() - target.getWidth());
    }

    private void moveOverlay(View target, float x, float y, boolean expandToRight) {
        ViewGroup parent = (ViewGroup) target.getParent();
        if (!(parent instanceof FrameLayout)) return;

        int maxX = getOverlayMaxX(target, parent, expandToRight);
        int maxY = Math.max(0, parent.getHeight() - target.getHeight());
        int left = Math.max(0, Math.min(maxX, Math.round(x)));
        int top = Math.max(0, Math.min(maxY, Math.round(y)));
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) target.getLayoutParams();
        params.gravity = Gravity.TOP | Gravity.START;
        params.leftMargin = left;
        params.topMargin = top;
        params.bottomMargin = 0;
        if (expandToRight) {
            params.width = Math.max(dp(120), parent.getWidth() - left - dp(4));
            params.rightMargin = dp(4);
        } else {
            params.rightMargin = 0;
        }
        target.setLayoutParams(params);
    }

    private void saveOverlayPosition(View target, String preferenceKey, boolean expandToRight) {
        ViewGroup parent = (ViewGroup) target.getParent();
        if (!(parent instanceof FrameLayout)) return;

        int maxX = Math.max(1, getOverlayMaxX(target, parent, expandToRight));
        int maxY = Math.max(1, parent.getHeight() - target.getHeight());
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) target.getLayoutParams();
        boolean saved = getSharedPreferences("te4_controls", MODE_PRIVATE).edit()
                .putFloat(preferenceKey + "_x", (float) params.leftMargin / maxX)
                .putFloat(preferenceKey + "_y", (float) params.topMargin / maxY)
                .commit();
        if (!saved) Log.e("TE4", "Could not save " + preferenceKey + " position");
    }

    private final class OverlayDragListener implements View.OnTouchListener {
        private final View handle;
        private final View target;
        private final String preferenceKey;
        private final boolean expandToRight;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private boolean pointerDown;
        private boolean longPressReached;
        private boolean dragging;
        private float dragOffsetX;
        private float dragOffsetY;

        private final Runnable startDragging = new Runnable() {
            @Override
            public void run() {
                if (pointerDown && handle.getParent() != null) {
                    longPressReached = true;
                    handle.getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
        };

        OverlayDragListener(View handle, View target, String preferenceKey,
                boolean expandToRight) {
            this.handle = handle;
            this.target = target;
            this.preferenceKey = preferenceKey;
            this.expandToRight = expandToRight;
        }

        @Override
        public boolean onTouch(View handle, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    int[] location = new int[2];
                    target.getLocationOnScreen(location);
                    dragOffsetX = event.getRawX() - location[0];
                    dragOffsetY = event.getRawY() - location[1];
                    pointerDown = true;
                    longPressReached = false;
                    dragging = false;
                    handler.postDelayed(startDragging, ViewConfiguration.getLongPressTimeout());
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (longPressReached) {
                        dragging = true;
                        int[] parentLocation = new int[2];
                        ViewGroup parent = (ViewGroup) target.getParent();
                        if (parent == null) return true;
                        parent.getLocationOnScreen(parentLocation);
                        moveOverlay(target,
                                event.getRawX() - parentLocation[0] - dragOffsetX,
                                event.getRawY() - parentLocation[1] - dragOffsetY,
                                expandToRight);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    handler.removeCallbacks(startDragging);
                    if (dragging) {
                        saveOverlayPosition(target, preferenceKey, expandToRight);
                    } else if (!longPressReached) {
                        handle.performClick();
                    }
                    reset();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(startDragging);
                    if (dragging) saveOverlayPosition(target, preferenceKey, expandToRight);
                    reset();
                    return true;
                default:
                    return true;
            }
        }

        private void reset() {
            if (handle.getParent() != null) {
                handle.getParent().requestDisallowInterceptTouchEvent(false);
            }
            pointerDown = false;
            longPressReached = false;
            dragging = false;
        }
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

    private static final class DirectionPadView extends View {
        private static final int NO_KEY = -1;
        private static final int REPEAT_DELAY_MS = 170;
        private static final int POSITION_HOLD_MS = 500;
        private static final String PREFERENCES = "te4_controls";
        private static final String[][] DIRECTIONS = {
                { "↖", "↑", "↗" },
                { "←", "↵", "→" },
                { "↙", "↓", "↘" }
        };
        private static final int[][] KEYCODES = {
                { SDLK_KP_7, SDLK_UP, SDLK_KP_9 },
                { SDLK_LEFT, SDLK_RETURN, SDLK_RIGHT },
                { SDLK_KP_1, SDLK_DOWN, SDLK_KP_3 }
        };

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Handler handler = new Handler(Looper.getMainLooper());
        private int activeKeycode = NO_KEY;
        private boolean pointerDown;
        private boolean positioning;
        private float dragOffsetX;
        private float dragOffsetY;

        private final Runnable repeatKey = new Runnable() {
            @Override
            public void run() {
                if (activeKeycode != NO_KEY) {
                    nativeSendKey(activeKeycode);
                    handler.postDelayed(this, REPEAT_DELAY_MS);
                }
            }
        };

        private final Runnable beginPositioning = new Runnable() {
            @Override
            public void run() {
                if (pointerDown && activeKeycode == SDLK_RETURN) {
                    positioning = true;
                    invalidate();
                }
            }
        };

        DirectionPadView(TE4Activity context) {
            super(context);
            setClickable(true);
            setContentDescription("Eight-direction pad; tap the center for Enter, hold the center to move the pad");
        }

        void restorePosition() {
            ViewGroup parent = (ViewGroup) getParent();
            if (parent == null || parent.getWidth() == 0 || parent.getHeight() == 0) return;

            android.content.SharedPreferences preferences =
                    getContext().getSharedPreferences(PREFERENCES, android.content.Context.MODE_PRIVATE);
            if (!preferences.contains("pad_x") || !preferences.contains("pad_y")) return;

            int maxX = Math.max(0, parent.getWidth() - getWidth());
            int maxY = Math.max(0, parent.getHeight() - getHeight());
            moveTo(maxX * clampFraction(preferences.getFloat("pad_x", 0f)),
                    maxY * clampFraction(preferences.getFloat("pad_y", 1f)));
        }

        private float clampFraction(float fraction) {
            return Math.max(0f, Math.min(1f, fraction));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cellWidth = getWidth() / 3f;
            float cellHeight = getHeight() / 3f;
            float gap = dp(2);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(24));
            paint.setTypeface(Typeface.DEFAULT_BOLD);

            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    int keycode = KEYCODES[row][column];
                    if (keycode == NO_KEY) continue;

                    float left = column * cellWidth + gap;
                    float top = row * cellHeight + gap;
                    float right = (column + 1) * cellWidth - gap;
                    float bottom = (row + 1) * cellHeight - gap;
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(keycode == activeKeycode || (positioning && keycode == SDLK_RETURN)
                            ? 0xCC777777 : 0x88707070);
                    canvas.drawRoundRect(left, top, right, bottom, dp(5), dp(5), paint);

                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(1));
                    paint.setColor(0x99FFFFFF);
                    canvas.drawRoundRect(left, top, right, bottom, dp(5), dp(5), paint);

                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.WHITE);
                    String label = positioning && keycode == SDLK_RETURN
                            ? "MOVE" : DIRECTIONS[row][column];
                    paint.setTextSize(positioning && keycode == SDLK_RETURN ? dp(13) : dp(24));
                    float textY = (top + bottom) / 2f
                            - (paint.ascent() + paint.descent()) / 2f;
                    canvas.drawText(label,
                            (left + right) / 2f, textY, paint);
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    pointerDown = true;
                    dragOffsetX = event.getX();
                    dragOffsetY = event.getY();
                    updateDirection(event.getX(), event.getY());
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (positioning) {
                        moveToScreen(event.getRawX(), event.getRawY());
                        return true;
                    }
                    updateDirection(event.getX(), event.getY());
                    return true;
                case MotionEvent.ACTION_UP:
                    if (activeKeycode == SDLK_RETURN && !positioning) {
                        nativeSendKey(SDLK_RETURN);
                    }
                    if (positioning) savePosition();
                    releaseDirection();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_POINTER_UP:
                    if (positioning) savePosition();
                    releaseDirection();
                    return true;
                default:
                    return true;
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            handler.removeCallbacks(repeatKey);
            handler.removeCallbacks(beginPositioning);
            super.onDetachedFromWindow();
        }

        private void updateDirection(float x, float y) {
            if (getWidth() == 0 || getHeight() == 0) return;
            int column = Math.max(0, Math.min(2, (int) (x * 3 / getWidth())));
            int row = Math.max(0, Math.min(2, (int) (y * 3 / getHeight())));
            int nextKeycode = KEYCODES[row][column];
            if (nextKeycode != activeKeycode) {
                handler.removeCallbacks(repeatKey);
                handler.removeCallbacks(beginPositioning);
                activeKeycode = nextKeycode;
                if (activeKeycode == SDLK_RETURN) {
                    handler.postDelayed(beginPositioning, POSITION_HOLD_MS);
                } else if (activeKeycode != NO_KEY) {
                    nativeSendKey(activeKeycode);
                    handler.postDelayed(repeatKey, REPEAT_DELAY_MS);
                }
            }
            invalidate();
        }

        private void releaseDirection() {
            handler.removeCallbacks(repeatKey);
            handler.removeCallbacks(beginPositioning);
            activeKeycode = NO_KEY;
            pointerDown = false;
            positioning = false;
            invalidate();
        }

        private void moveToScreen(float screenX, float screenY) {
            ViewGroup parent = (ViewGroup) getParent();
            if (parent == null) return;
            int[] parentLocation = new int[2];
            parent.getLocationOnScreen(parentLocation);
            moveTo(screenX - parentLocation[0] - dragOffsetX,
                    screenY - parentLocation[1] - dragOffsetY);
        }

        private void moveTo(float x, float y) {
            ViewGroup parent = (ViewGroup) getParent();
            if (parent == null) return;
            int maxX = Math.max(0, parent.getWidth() - getWidth());
            int maxY = Math.max(0, parent.getHeight() - getHeight());
            int left = Math.max(0, Math.min(maxX, Math.round(x)));
            int top = Math.max(0, Math.min(maxY, Math.round(y)));
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
            params.gravity = Gravity.TOP | Gravity.START;
            params.leftMargin = left;
            params.topMargin = top;
            params.rightMargin = 0;
            params.bottomMargin = 0;
            setLayoutParams(params);
        }

        private void savePosition() {
            ViewGroup parent = (ViewGroup) getParent();
            if (parent == null) return;
            int maxX = Math.max(1, parent.getWidth() - getWidth());
            int maxY = Math.max(1, parent.getHeight() - getHeight());
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
            boolean saved = getContext().getSharedPreferences(
                    PREFERENCES, android.content.Context.MODE_PRIVATE).edit()
                    .putFloat("pad_x", (float) params.leftMargin / maxX)
                    .putFloat("pad_y", (float) params.topMargin / maxY)
                    .commit();
            if (!saved) Log.e("TE4", "Could not save direction pad position");
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
