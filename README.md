# tome-android-port

Unofficial Android build and touch-control port for Tales of Maj'Eyal (T-Engine 4).

<table>
  <tr>
    <td width="33.33%">
      <img src="https://github.com/user-attachments/assets/899df79c-0641-4709-8251-75a1c4865cbf" alt="Screenshot 1" />
    </td>
    <td width="33.33%">
      <img src="https://github.com/user-attachments/assets/04601cb6-f765-4ad6-807e-819ded627e55" alt="Screenshot 2" />
    </td>
    <td width="33.33%">
      <img src="https://github.com/user-attachments/assets/4f731213-f625-4d9c-a811-0cfc2ecb48b5" alt="Screenshot 3" />
    </td>
  </tr>
</table>

## Android build

The GitHub Actions workflow in `.github/workflows/android.yml` builds an arm64
debug APK. Run it from the Actions tab with **Android** → **Run workflow**, or
push a change to one of the paths watched by the workflow.

On Android 10 and newer, native output is written to
`Download/Tales of Maj'Eyal/te4_log.txt`. The log is replaced on each launch.
Game data remains in the app's private storage.

## Android controls

The game runs in landscape. The touch controls are tailored to Tales of Maj'Eyal:

| Control | Effect |
|---|---|
| 8-direction pad | Move in the selected direction; diagonal directions use the numpad movement keys. Hold to repeat. |
| `↵` at the center of the pad | Send Enter; confirm in menus and change levels while standing on stairs. |
| Hold the pad's center for half a second, then drag | Reposition the pad. Release to save its position for future launches. |
| `PAD` | Show or hide the eight-way pad; the visibility choice is saved. |
| `⌨` | Show or hide the on-screen keyboard. |
| `ESC`, `TAB`, arrow buttons | Send the corresponding game key. |
| `BAR` | Show or hide the shortcut bar; hold and drag it to reposition it. |
| `SAVE` | Open the save folder options; shown only in the main menu. |
| `ADDONS` | Open the add-ons folder/import options; shown only in the main menu. |
| Hold a built-in key on the shortcut bar, then drag | Reposition the shortcut bar. Release to save its position for future launches. |
| `+` | Type and save a one-character shortcut using the on-screen keyboard. Saved shortcuts persist between launches. |
| Hold a custom shortcut | Show its action from the game's Key bindings; use **Delete shortcut** in the dialog to remove it. Built-in controls cannot be deleted. |

## Add-ons on Android

Put DLC archives (`.teaac`) and add-on archives (`.teaa`) in
`Download/Tales of Maj'Eyal/addons`. The app creates this folder automatically
when you first open the add-ons menu. Tap **ADDONS** → **Import Add-ons** to
copy the archives into the game's private `game/addons` folder. Grant storage
permission when prompted the first time. Restart the game after importing to
load them.

## Game saves on Android

The app backs up game saves to `Download/Tales of Maj'Eyal/save` when returning
to the main menu and restores newer files from that folder when the game starts.
Copy saved characters into this folder to restore them after reinstalling. Use
**SAVE** → **Import Saves** to import files while the app is already running,
or **Back Up Now** to copy the current saves immediately. Grant storage
permission when prompted.

## Licensing

The engine and port code use GPL-3.0-only; see [LICENSE](LICENSE). Game content
and bundled third-party components may have separate terms. In particular,
media under `/data/gfx` is restricted to use with Tales of Maj'Eyal; see
[COPYING-MEDIA](COPYING-MEDIA) and retain the notices in the source tree.

This is an unofficial community port and is not affiliated with the game
developers.
