# Optional PicoCalc USB bridge for the Android companion.
# Run manually from a MicroPython REPL after verifying the target firmware:
#   exec(open("android_bridge.py").read())
# Commands are one JSON object per line; responses are one JSON object per line.
# Only a single filename is accepted. No arbitrary shell/code execution is exposed.

import sys
import os
import ujson

try:
    import ubinascii
except ImportError:
    ubinascii = None

ROOT = "android_sync"
MAX_FILE_BYTES = 65536
MAX_NAME_LENGTH = 64


def _ensure_root():
    try:
        os.stat(ROOT)
    except OSError:
        os.mkdir(ROOT)


def _valid_name(name):
    if not isinstance(name, str) or not name or len(name) > MAX_NAME_LENGTH:
        return False
    if name in (".", "..") or "/" in name or "\\" in name or "\x00" in name:
        return False
    return True


def _response(request_id, ok, **values):
    result = {"id": request_id, "ok": ok}
    result.update(values)
    sys.stdout.write(ujson.dumps(result) + "\n")


def _handle(message):
    request_id = message.get("id")
    command = message.get("cmd")
    if command == "PING":
        return {"ok": True, "firmware": "MicroPython", "bridge": 1}
    if command == "LIST":
        _ensure_root()
        return {"ok": True, "files": os.listdir(ROOT)}
    name = message.get("name")
    if not _valid_name(name):
        return {"ok": False, "error": "Invalid filename"}
    _ensure_root()
    path = ROOT + "/" + name
    if command == "READ":
        with open(path, "rb") as handle:
            data = handle.read(MAX_FILE_BYTES + 1)
        if len(data) > MAX_FILE_BYTES:
            return {"ok": False, "error": "File exceeds 64 KiB limit"}
        if ubinascii is None:
            return {"ok": False, "error": "Base64 support unavailable"}
        return {"ok": True, "data": ubinascii.b2a_base64(data).decode().strip()}
    if command == "WRITE":
        if ubinascii is None:
            return {"ok": False, "error": "Base64 support unavailable"}
        encoded = message.get("data", "")
        if not isinstance(encoded, str) or len(encoded) > ((MAX_FILE_BYTES + 2) // 3) * 4 + 8:
            return {"ok": False, "error": "Payload exceeds 64 KiB limit"}
        data = ubinascii.a2b_base64(encoded)
        if len(data) > MAX_FILE_BYTES:
            return {"ok": False, "error": "Payload exceeds 64 KiB limit"}
        temp_path = path + ".tmp"
        with open(temp_path, "wb") as handle:
            handle.write(data)
        try:
            os.remove(path)
        except OSError:
            pass
        os.rename(temp_path, path)
        return {"ok": True, "bytes": len(data)}
    return {"ok": False, "error": "Unsupported command"}


while True:
    try:
        line = sys.stdin.readline()
        if not line:
            break
        try:
            request = ujson.loads(line)
            result = _handle(request)
            _response(request.get("id"), result.pop("ok", False), **result)
        except Exception as error:
            _response(None, False, error=str(error))
    except KeyboardInterrupt:
        break
