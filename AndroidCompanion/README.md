# PicoCalc Companion for Android

A native Android companion project added separately from the existing firmware tree. Existing firmware, prebuilt binaries, and submodules are preserved.

## Included
- Arithmetic calculator with operator precedence, parentheses, decimals, exponents, and safe parsing (no eval).
- USB serial terminal using Android USB host mode and supported serial adapters.
- Storage Access Framework folder selection/listing, file import picker, and script save picker.
- MicroPython text editor that can send script text to an already-compatible serial REPL.
- PicoCalc-like screen/keyboard UI mock; not a CPU or firmware emulator.
- USB device enumeration, permission request, and serial connection at 115200 8N1.
- Optional MicroPython JSON-line file bridge at `Code/AndroidCompanion/usb_bridge.py` with PING, LIST, READ, and WRITE commands. See [integration notes](../docs/ANDROID_FIRMWARE_INTEGRATION.md).

## Build
Requires JDK 17 and Gradle 8.9:
```sh
gradle testDebugUnitTest
gradle assembleDebug
gradle bundleRelease
```
Outputs are in `app/build/outputs/`. The release AAB is unsigned unless local signing is configured. CI uploads an unsigned AAB and debug APK as workflow artifacts; it does not publish to Google Play.

## Hardware limitations
A USB data cable and OTG host support are required. Direct PicoCalc USB serial behavior varies by attached Pico board and firmware. The serial send feature is not a raw-REPL file-transfer implementation; test with short snippets and verify the REPL state. The editor does not yet automate the bridge's JSON/base64 file protocol. The emulator is a UI mock only.
