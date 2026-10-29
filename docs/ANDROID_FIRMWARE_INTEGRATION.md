# Android Companion / Firmware Integration

## Scope and preservation
The Android app is isolated in `AndroidCompanion/`. Existing firmware source, UF2 images, and submodules are preserved. An optional MicroPython bridge is added at `Code/AndroidCompanion/usb_bridge.py`; it is not auto-installed or auto-run by existing firmware.

## USB serial
The app uses Android USB host mode and usb-serial-for-android. Default serial settings are 115200 baud, 8 data bits, no parity, one stop bit. Actual compatibility depends on the Pico board, firmware, USB bridge, OTG support, and cable. A serial connection does not prove the target is running a MicroPython REPL.

## Optional MicroPython file bridge
Copy `Code/AndroidCompanion/usb_bridge.py` to the PicoCalc's accessible filesystem, connect to its MicroPython REPL, and run it manually, for example:
```python
exec(open("usb_bridge.py").read())
```
The bridge then reads one JSON object per line and responds with one JSON object per line. Example requests that can be sent from the Android Terminal:

```json
{"id":1,"cmd":"PING"}
{"id":2,"cmd":"LIST"}
{"id":3,"cmd":"READ","name":"hello.py"}
{"id":4,"cmd":"WRITE","name":"hello.py","data":"cHJpbnQoJ2hpJykK"}
```

Files are restricted to single names under the `android_sync/` directory, with a 64 KiB per-file limit. `WRITE` accepts base64 data, writes a temporary file, then renames it. Supported commands are PING, LIST, READ, and WRITE; the bridge does not expose arbitrary shell or code-execution commands. Stop the bridge with Ctrl+C to return to the REPL. Exact behavior depends on the MicroPython build and its stdin/stdout routing. Test on noncritical files first.

## Current limitations
- The app's MicroPython editor sends raw script text; it does not automatically encode the bridge's JSON/base64 protocol yet.
- The app's screen emulator is a UI mock, not an RP2040/RP2350 CPU or firmware emulator.
- The file tab uses Android's Storage Access Framework to browse a user-selected storage provider; it is not yet a remote PicoCalc SD-card browser.
- The app does not flash UF2 firmware or perform OTA updates.

## Follow-up improvements
- Add in-app bridge protocol support and device capability/version negotiation.
- Add integration tests for LCD, keyboard, SD card, USB, and RP2040/RP2350 variants.
- Never flash firmware or execute arbitrary update commands automatically from the companion app.

CI checks the bridge's Python syntax, runs JVM unit tests, and builds a debug APK and release AAB. Physical USB and board integration still require hardware testing.
