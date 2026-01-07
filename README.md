# Just Player+

This is a modified version of [Just Player](https://github.com/moneytoo/Player) with additional features.

## Changes compared to the original Just Player app

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

<img src="https://github.com/user-attachments/assets/b98be7fa-f38c-4ab5-aea0-d580b69f40e1" width="800">

### Other changes

* Fix subtitle encoding when used as an external player with Nova Video Player
* The Back button hides media controls instead of quitting the app on Android TV

## How to install

Download the APK file from the [Releases page](https://github.com/wasky/just-player/releases) and open it on your device.
