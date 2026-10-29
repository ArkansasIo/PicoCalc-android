"""Minimal line-oriented PicoCalc diagnostic helper for MicroPython.

Run manually from a REPL; it does not install itself at boot or write flash.
"""
import sys
import time

def handle(line):
    command = line.strip().upper()
    if command == "PING":
        return "PONG"
    if command == "INFO":
        return "PICOCALC;runtime=micropython"
    if command == "TIME":
        return "TICKS_MS={}".format(time.ticks_ms())
    return "ERR;unsupported-command"

def serve():
    while True:
        line = sys.stdin.readline()
        if not line:
            time.sleep_ms(10)
            continue
        try:
            sys.stdout.write(handle(line) + "\n")
        except Exception:
            sys.stdout.write("ERR;handler-failed\n")

if __name__ == "__main__":
    serve()
