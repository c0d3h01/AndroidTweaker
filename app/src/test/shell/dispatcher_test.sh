#!/bin/sh
set -eu
engine=$(cd app/src/main/assets/tweaker && pwd -P)
fixture=$(mktemp -d)
trap 'rm -rf "$fixture"' EXIT
mkdir "$fixture/engine with spaces"
cp -R "$engine/." "$fixture/engine with spaces/"
# Invalid commands load every module but must never apply device settings.
if (cd /; unset MODDIR; sh "$fixture/engine with spaces/dispatcher.sh" invalid) > "$fixture/output" 2>&1; then
  echo 'Invalid command unexpectedly succeeded' >&2
  exit 1
fi
grep -q '^usage: dispatcher.sh' "$fixture/output"
if grep -E 'not found|cannot open|No such file' "$fixture/output"; then
  echo 'Dispatcher failed to load modules' >&2
  exit 1
fi
echo 'Dispatcher loading OK'
