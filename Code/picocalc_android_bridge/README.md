# PicoCalc Android companion bridge

This folder defines a proposed device-side protocol and MicroPython helper. It is separate from existing firmware projects.

## Current scope
- The Android app can list USB host devices and provides a local terminal UI.
- Universal USB-serial support and automatic firmware flashing are not implemented.
- USB adapters differ (CDC ACM, CH34x, CP210x, FTDI, and others). A production transport must detect interfaces, request USB permission, handle endpoint I/O, and be tested against hardware.
- Use a compatible USB-to-UART bridge if the PicoCalc USB connection does not expose serial.

## Proposed line protocol
UTF-8, newline-delimited commands:
- PING returns PONG
- INFO returns PICOCALC;runtime=micropython
- TIME returns device ticks in milliseconds
- File transfer and remote execution are not implemented.

Do not send arbitrary commands to unknown devices. Before adding file writes, enforce bounded frames, checksums, transfer timeouts, and explicit user confirmation.

## MicroPython example
bridge.py is a diagnostic helper. It does not alter boot configuration or flash firmware.
