# tome-android-port

Unofficial Android build and touch-control port for Tales of Maj'Eyal (T-Engine 4).

## Android build

The GitHub Actions workflow in `.github/workflows/android.yml` builds an arm64
debug APK. Run it from the Actions tab with **Android** → **Run workflow**, or
push a change to one of the paths watched by the workflow.

Large game archives are stored with Git LFS. Install Git LFS before cloning so
the game data is available locally.

## Licensing

The engine and port code use GPL-3.0-only; see [LICENSE](LICENSE). Game content
and bundled third-party components may have separate terms. In particular,
media under `/data/gfx` is restricted to use with Tales of Maj'Eyal; see
[COPYING-MEDIA](COPYING-MEDIA) and retain the notices in the source tree.

This is an unofficial community port and is not affiliated with the game
developers.