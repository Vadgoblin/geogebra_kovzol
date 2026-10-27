#!/usr/bin/env bash
set -euo pipefail

# ensure running on macOS
if [ "$(uname -s)" != "Darwin" ]; then
  echo "Error: This script must be run on macOS (detected: $(uname -s))." >&2
  exit 1
fi

VERSION="${APP_VERSION//_/-}"
SRC_DIR="${SRC_DIR:-$(pwd)}"
OUT_DIR="${OUT_DIR:-${SRC_DIR}/dist}"
GGD_SRC_SHADOW="/tmp/ggd-src"
PACKAGE_DIR="/tmp/ggd-package"

echo ">>> Target version: ${VERSION}"

brew install tree

echo ">>> Copying project source files..."
rm -rf "${GGD_SRC_SHADOW}"
mkdir -p "${GGD_SRC_SHADOW}"

cp -RP "${SRC_DIR}/desktop" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/gradle" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/gradlew" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/build.gradle" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/init.gradle" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/presettings.gradle" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/settings.gradle" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/retex" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/gradle-scripts" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/carota-web" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/common" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/common-jre" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/jogl2" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/input3D" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/giac-jni" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/tarski-jni" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/web" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/ggbjdk" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/keyboard-base" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/keyboard-scientific" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/keyboard-web" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/test" "${GGD_SRC_SHADOW}/"
cp -RP "${SRC_DIR}/xr-base" "${GGD_SRC_SHADOW}/"

echo ">>> Installing java"
brew install --cask temurin@11
export JAVA_HOME="$(/usr/libexec/java_home -v 11)"
export PATH="${JAVA_HOME}/bin:${PATH}"

echo ">>> Building..."
cd "${GGD_SRC_SHADOW}/"
./gradlew :desktop:installDist

cp -r "${GGD_SRC_SHADOW}/desktop/build/install/desktop"/* "$PACKAGE_DIR"

# remove unneeded jars
LIB_DIR="$PACKAGE_DIR/lib"
rm -f "$LIB_DIR"/*-windows-* \
      "$LIB_DIR"/*-linux-* \
      "$LIB_DIR"/zspace* \
      "$LIB_DIR"/libpxcclr*

tree "${GGD_SRC_SHADOW}/"