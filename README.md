# tome-android-port

Unofficial Android build and touch-control port for Tales of Maj'Eyal (T-Engine 4).

## Android build

The GitHub Actions workflow in `.github/workflows/android.yml` builds an arm64
debug APK. Run it from the Actions tab with **Android** → **Run workflow**, or
push a change to one of the paths watched by the workflow.

Large game archives are stored with Git LFS. Install Git LFS before cloning so
the game data is available locally.

On Android 10 and newer, native output is written to
`Download/Tales of Maj'Eyal/te4_log.txt`. The log is replaced on each launch.
Game data remains in the app's private storage.

## Android controls

The game runs in landscape. The touch controls are tailored to Tales of Maj'Eyal:

| Control | Effect |
|---|---|
| Eight-way pad | Move in the selected direction; diagonal directions use the numpad movement keys. Hold to repeat. |
| `↵` at the center of the pad | Send Enter (confirm in menus and use the game's Enter action). |
| Hold the pad's center for half a second, then drag | Reposition the pad. Release to save its position for future launches. |
| `⌨` | Show or hide the on-screen keyboard. |
| `ESC`, `TAB`, arrow buttons, `<`, `>` | Send the corresponding game key; `<` and `>` change levels when standing on stairs. |
| `BAR` | Show or hide the shortcut bar; hold and drag it to reposition it. |
| Hold a built-in key on the shortcut bar, then drag | Reposition the shortcut bar. Release to save its position for future launches. |
| `+` | Type and save a one-character shortcut using the on-screen keyboard. Saved shortcuts persist between launches; long-press one to delete it. Built-in controls cannot be deleted. |
| `MOUSE` | Show or hide the separate mouse controls. |
| Mouse cursor pad | Hold a direction to move the game pointer; the `LMB` and `RMB` buttons click at that pointer position. |
| `MOVE` on the mouse controls | Hold and drag to reposition the mouse controls; release to save their position. |

Tapping the game area does not send Enter; use the center of the direction pad
instead.

## Licensing

The engine and port code use GPL-3.0-only; see [LICENSE](LICENSE). Game content
and bundled third-party components may have separate terms. In particular,
media under `/data/gfx` is restricted to use with Tales of Maj'Eyal; see
[COPYING-MEDIA](COPYING-MEDIA) and retain the notices in the source tree.

This is an unofficial community port and is not affiliated with the game
developers.