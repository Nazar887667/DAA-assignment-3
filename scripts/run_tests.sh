#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
./scripts/build.sh
java -cp out tests.TestRunner
