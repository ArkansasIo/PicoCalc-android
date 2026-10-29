# Android Companion / Firmware Integration

## Scope and preservation
The Android app is isolated in `AndroidCompanion/`. Existing firmware source, UF2 images, and submodules are not replaced or modified.

## USB serial
The app uses Android USB host mode and usb-serial-for-android. Default serial settings are 115200 baud, 8 data bits, no parity, one stop bit. Actual compatibility depends on the Pico board, firmware, USB bridge, OTG support, and cable. A serial connection does not prove the target is running a MicroPython REPL.

## MicroPython workflow
1. Flash firmware for the exact Pico board and PicoCalc hardware revision.
2. Connect using a data-capable cable and OTG adapter if needed.
3. Grant permission to the detected USB serial device.
4. Confirm the REPL in Terminal before sending snippets.
5. Use the Android file picker to save scripts locally or to a selected storage provider.

The current editor sends text to serial; it is not a reliable script transfer protocol or firmware flasher.

## Proposed firmware improvements (not implemented)
- Add a version/capability handshake for a confirmed firmware/board profile.
- Add a framed, length-limited file-transfer protocol with command allow-list, timeout, response status, and integrity checking.
- Write incoming files to a temporary path and rename only after validation.
- Add device tests for LCD, keyboard, SD card, USB, and RP2040/RP2350 variants.
- Never flash firmware or execute arbitrary update commands automatically from the companion app.

CI runs JVM unit tests and builds a debug APK and release AAB. Physical USB and board integration still require hardware testing.
