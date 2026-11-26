# Sourced by the dev scripts: pick the single connected device.
# Exports ADB_SERIAL and ANDROID_SERIAL, or prints why not and returns 1.
_dev_list="$(adb devices | tr -d '\r' | awk 'NR > 1 && $2 == "device" { print $1 }')"
if [ -n "${ADB_SERIAL:-}" ]; then
  if ! printf '%s\n' "$_dev_list" | grep -qxF -- "$ADB_SERIAL"; then
    echo "ADB_SERIAL=$ADB_SERIAL is not listed as 'device' by adb devices" >&2
    return 1
  fi
else
  _dev_count="$(printf '%s' "$_dev_list" | grep -c . || true)"
  if [ "$_dev_count" -eq 0 ]; then
    if adb devices | grep -q 'unauthorized'; then
      echo "device unauthorized: unlock the phone and accept the USB debugging prompt" >&2
    else
      echo "no device: pair wireless debugging, then check adb devices -l" >&2
    fi
    return 1
  elif [ "$_dev_count" -gt 1 ]; then
    printf '%s\n' "$_dev_list" >&2
    echo "several devices: set ADB_SERIAL=<serial>" >&2
    return 1
  fi
  ADB_SERIAL="$_dev_list"
fi
export ADB_SERIAL
export ANDROID_SERIAL="$ADB_SERIAL"
unset _dev_list _dev_count
