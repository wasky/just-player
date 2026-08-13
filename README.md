# Just Player Morveus

Personal fork of [Just Player+](https://github.com/wasky/just-player-plus) (itself a modified [Just Player](https://github.com/moneytoo/Player)), tweaked for my own setup.

## Changes compared to Just Player+

* Renamed to **Just Player Morveus** with application id `com.morveus.player`, so it installs alongside the original Just Player+ without any conflict
* Subtitle delay adjusts in **100 ms** steps instead of 200 ms
* **Fixed subtitle delay for embedded subtitles**: the delay is now applied at render time instead of parse time. Previously, negative delays could not move embedded subtitles (MKV) earlier: they only shortened the on-screen duration, and past roughly -2 s subtitles disappeared entirely. It now works in both directions, for embedded, external and image-based (PGS/VobSub) subtitles
* Positive delay values are shown with a `+` prefix in the OSD

Subtitle delay is reachable by long-pressing the subtitle icon on the playback screen. Right arrow (+) shows subtitles later, left arrow (-) shows them earlier, like in Kodi.

## Features inherited from Just Player+

### Additional subtitle settings on the playback screen

* Experimental: Subtitle delay/advance
* Experimental: Support for MicroDVD and MPL2 subtitles
* Subtitle settings (size, style, position) can be configured by long-pressing the subtitle icon on the playback screen
* New `Outline & shadow` subtitle edge style
* New `Medium` font style in addition to Regular and Bold (requires Android 9+)
* On Android TV, you can achieve a Netflix / Nova Player–like subtitle style by setting:
  * Position: `+3`
  * Size: `-8`
  * Edge type: `Outline & shadow`
  * Typeface: `Medium`
* Custom fonts for subtitles

### Other changes

* Fix subtitle encoding when used as an external player with Nova Video Player
* The Back button hides media controls instead of quitting the app on Android TV

## How to build

```
./gradlew :app:assembleLatestUniversalRelease
```

The APK ends up in `app/build/outputs/apk/latestUniversal/release/just_player_morveus.apk`.
