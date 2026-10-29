# PicoCalc Companion for Android

A native Android Java starter app for working alongside the existing PicoCalc firmware tree.

## Build

Requires JDK 17, Android SDK platform 35, Android build-tools 35, and Gradle 8.9.

From this directory:

```sh
gradle assembleDebug bundleRelease testDebugUnitTest
```

Outputs:
- Debug APK: app/build/outputs/apk/debug/
- Release AAB: app/build/outputs/bundle/release/

The release bundle is unsigned for store publishing unless a signing configuration is supplied. Do not store signing keys in the repository.

## Current implementation

- Calculator: arithmetic expression parser supporting +, -, *, /, exponentiation, parentheses, unary signs; rejects division by zero and trailing invalid input.
- Terminal: local console UI prototype; it does not yet stream bytes to/from a connected serial device.
- Files: create, list, load, and save text files in the app-private sandbox. This intentionally does not request broad storage access.
- MicroPython: script editor and local main.py save. Automated board upload is not implemented yet.
- Emulator: interactive screen mockup with navigation into companion tools; not a CPU or firmware emulator.
- USB: lists Android USB-host devices and basic IDs/interface counts. Permission flow and serial endpoint transport are future work and must be tested with actual hardware.

## Firmware bridge

See ../Code/picocalc_android_bridge/README.md. The MicroPython helper is a manual diagnostic example, not an auto-installed service.

## Safety and limitations

This is an initial, build-oriented implementation. Firmware flashing, arbitrary remote execution, and automatic file transfer are deliberately not advertised as working features. Device-specific USB protocols must be implemented and tested before relying on data transfer.
