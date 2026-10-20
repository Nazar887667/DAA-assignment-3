#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
./scripts/build.sh
java -cp out app.Experiments results/tables
python3 scripts/plot.py