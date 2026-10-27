#!/usr/bin/env bash
set -euo pipefail

# Ensure running on macOS
if [ "$(uname -s)" != "Darwin" ]; then
  echo "Error: This script must be run on macOS (detected: $(uname -s))." >&2
  exit 1
fi

VERSION="${APP_VERSION//_/-}"
SRC_DIR="${SRC_DIR:-$(pwd)}"
OUT_DIR="${OUT_DIR:-${SRC_DIR}/dist}"
GGD_SRC_SHADOW="/tmp/ggd-src"
PACKAGE_DIR="/tmp/ggd-macos-package"
APP_NAME="GeoGebra Discovery"
APP_BUNDLE="${PACKAGE_DIR}/${APP_NAME}.app"

echo ">>> Target version: ${VERSION}"

# 1. Install build tools and dependencies if missing
if ! command -v brew >/dev/null 2>&1; then
  echo "Error: Homebrew is required to install dependencies." >&2
  exit 1
fi

if ! brew list temurin@11 >/dev/null 2>&1; then
  echo ">>> Installing Temurin OpenJDK 11..."
  brew install --cask temurin@11
fi

export JAVA_HOME="$(/usr/libexec/java_home -v 11)"
export PATH="${JAVA_HOME}/bin:${PATH}"

# 2. Copy source files to shadow directory
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

# 3. Build with Gradle
echo ">>> Building Geogebra-discovery..."
cd "${GGD_SRC_SHADOW}"
./gradlew :desktop:installDist

# 4. Assemble macOS .app Bundle Structure
echo ">>> Packaging application bundle..."
rm -rf "${PACKAGE_DIR}"
mkdir -p "${APP_BUNDLE}/Contents/MacOS"
mkdir -p "${APP_BUNDLE}/Contents/Resources/Java"

# Copy distribution artifacts into bundle
cp -r "${GGD_SRC_SHADOW}/desktop/build/install/desktop/lib" "${APP_BUNDLE}/Contents/Resources/Java/"
cp -r "${GGD_SRC_SHADOW}/desktop/build/install/desktop/bin" "${APP_BUNDLE}/Contents/Resources/"

# 5. Clean up non-macOS native libraries and extraneous platforms
LIB_DIR="${APP_BUNDLE}/Contents/Resources/Java/lib"
rm -f "$LIB_DIR"/*-windows-* \
      "$LIB_DIR"/*-linux-* \
      "$LIB_DIR"/*-solaris-* \
      "$LIB_DIR"/*-android-* \
      "$LIB_DIR"/zspace* \
      "$LIB_DIR"/libpxcclr*

# 6. Create Launch Executable
cat << 'EOF' > "${APP_BUNDLE}/Contents/MacOS/geogebra-discovery"
#!/usr/bin/env bash
DIR="$(cd "$(dirname "$0")/../Resources" && pwd)"

# Resolve system Java
if [ -z "${JAVA_HOME:-}" ]; then
  if /usr/libexec/java_home -v 11 >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 11)"
  elif /usr/libexec/java_home >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home)"
  fi
fi

exec "${DIR}/bin/desktop" "$@"
EOF
chmod +x "${APP_BUNDLE}/Contents/MacOS/geogebra-discovery"

# 7. Generate Info.plist metadata
cat << EOF > "${APP_BUNDLE}/Contents/Info.plist"
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>geogebra-discovery</string>
    <key>CFBundleIdentifier</key>
    <string>org.geogebra.discovery</string>
    <key>CFBundleName</key>
    <string>GeoGebra Discovery</string>
    <key>CFBundleDisplayName</key>
    <string>GeoGebra Discovery</string>
    <key>CFBundleVersion</key>
    <string>${VERSION}</string>
    <key>CFBundleShortVersionString</key>
    <string>${VERSION}</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>NSHighResolutionCapable</key>
    <true/>
</dict>
</plist>
EOF

# Optional: Convert or copy icon if present
if [ -f "${SRC_DIR}/flatpak/gd-512.png" ]; then
  cp "${SRC_DIR}/flatpak/gd-512.png" "${APP_BUNDLE}/Contents/Resources/app.png"
fi

# 8. Archive Output
mkdir -p "${OUT_DIR}"
OUTPUT_ARCHIVE="${OUT_DIR}/geogebra-discovery-${VERSION}-macos.zip"

echo ">>> Creating ZIP distribution: ${OUTPUT_ARCHIVE}"
(cd "${PACKAGE_DIR}" && zip -qyr "${OUTPUT_ARCHIVE}" "${APP_NAME}.app")

ls OUTPUT_ARCHIVE

echo ">>> Build completed successfully."