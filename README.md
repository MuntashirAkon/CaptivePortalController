# Captive Portal Controller

Control how Android detects captive portal.

**NOTE:** In many OEM provided ROMs, it is NOT a privacy feature because these ROMs are able to bypass captive portal settings.

## Features
- Enable/disable captive portal detection
- Set captive portal servers (with presets from [GrapheneOS](https://grapheneos.org/faq#default-connections), [Kuketz](https://www.kuketz-blog.de/empfehlungsecke/#captive-portal) and Google)
- A quick setting tile to toggle captive portal detection
- Less than 100KB app as you don't need to use it frequently.

## Grant permission WRITE_SECURE_SETTINGS via adb
adb shell
pm grant io.github.muntashirakon.captiveportalcontroller android.permission.WRITE_SECURE_SETTINGS
