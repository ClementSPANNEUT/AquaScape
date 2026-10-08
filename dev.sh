#!/bin/sh
set -e
cd "$(dirname "$0")"
exec java dev/DevLoop.java "$@"
