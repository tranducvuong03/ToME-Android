# tome-android-port

Unofficial Android build and touch-control port for Tales of Maj'Eyal (T-Engine 4).

Upstream: https://te4.org/download

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
| `ESC`, `TAB`, `g`, `i`, `p`, `r` | Send the corresponding game key. |
| Hold a fixed key without dragging | Show its assigned game action; fixed keys cannot be deleted. |
| Shortcut bar | Shows up to 8 buttons at a time; swipe left or right to reach the rest. |
| `BAR` | Show or hide the shortcut bar; hold and drag it to reposition it. |
| `SAVE` | Open the save folder options; shown only in the main menu. |
| `ADDONS` | Open the add-ons folder/import options; shown only in the main menu. |
| Hold a built-in key on the shortcut bar, then drag | Reposition the shortcut bar. Release to save its position for future launches. |
| `+` | Type and save a one-character shortcut using the on-screen keyboard. Saved shortcuts persist between launches. |
| Hold a custom shortcut | Show its action from the game's Key bindings; use **Delete shortcut** in the dialog to remove it. |

On startup, a loading screen blocks input until game initialization is complete and the first menu frame has rendered.

## Add-ons

1. **Setup:** Select `Download/Tales of Maj'Eyal` (or any folder) on first launch, or later via the **ADDONS** menu. The app will create an `addons` subfolder inside.
2. **Install:** Put `.teaa` or `.teaac` files into `Download/Tales of Maj'Eyal/addons` (or folder you choose with `addons` inside), open game → tap **ADDONS** → **Import Add-ons**, and restart the game.

## Game Saves

1. **Auto-Sync:** Saves automatically back up to the `save` subfolder when returning to the main menu, and restore when the game starts.
2. **Manual:** Copy old character files into `save` to restore them. Use **SAVE** → **Import Saves** (to load immediately) or **Back Up Now** (to force a backup).

## Licensing

The engine and port code use GPL-3.0-only; see [LICENSE](LICENSE). Game content
and bundled third-party components may have separate terms. In particular,
media under `/data/gfx` is restricted to use with Tales of Maj'Eyal; see
[COPYING-MEDIA](COPYING-MEDIA) and retain the notices in the source tree.

This is an unofficial community port and is not affiliated with the game
developers.
