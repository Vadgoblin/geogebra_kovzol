#!/usr/bin/env bash
set -euo pipefail

# Ensure running on macOS
if [ "$(uname -s)" != "Darwin" ]; then
  echo "Error: This script must be run on macOS (detected: $(uname -s))." >&2
  exit 1
fi

_HOST_ARCH="$(uname -m)"
case "$_HOST_ARCH" in
  x86_64)
    ARCH="x86_64"
    KEEP_MACOS_NATIVE="x86_64"
    ;;
  arm64|aarch64)
    ARCH="arm64"
    KEEP_MACOS_NATIVE="aarch64"
    ;;
  *)
    echo "Unsupported architecture: $_HOST_ARCH" >&2
    exit 1
    ;;
esac

VERSION="${APP_VERSION:-1.0.0}"
VERSION="${VERSION//_/-}"
SRC_DIR="${SRC_DIR:-$(pwd)}"
OUT_DIR="${OUT_DIR:-${SRC_DIR}/dist}"
GGD_SRC_SHADOW="/tmp/ggd-src"
PACKAGE_DIR="/tmp/ggd-macos-package"
APP_NAME="GeoGebra Discovery"
APP_BUNDLE="${PACKAGE_DIR}/${APP_NAME}.app"

echo ">>> Target version: ${VERSION} (${ARCH})"

# 1. Ensure build tools and JDK 11 are present
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

# 2. Shadow source files
echo ">>> Copying project source files..."
rm -rf "${GGD_SRC_SHADOW}"
mkdir -p "${GGD_SRC_SHADOW}"

for item in desktop gradle gradlew build.gradle init.gradle presettings.gradle \
            settings.gradle retex gradle-scripts carota-web common common-jre \
            jogl2 input3D giac-jni tarski-jni web ggbjdk keyboard-base \
            keyboard-scientific keyboard-web test xr-base; do
  if [ -e "${SRC_DIR}/${item}" ]; then
    cp -RP "${SRC_DIR}/${item}" "${GGD_SRC_SHADOW}/"
  fi
done

# 3. Build project
echo ">>> Building Geogebra-discovery..."
cd "${GGD_SRC_SHADOW}"
./gradlew :desktop:installDist

# 4. Generate a stripped-down bundled JRE using jlink
BUNDLED_RUNTIME="${PACKAGE_DIR}/runtime"
rm -rf "${BUNDLED_RUNTIME}"

echo ">>> Creating bundled Java runtime with jlink..."
"$JAVA_HOME/bin/jlink" \
  --add-modules java.base,java.desktop,java.logging,java.management,java.naming,java.net.http,java.prefs,java.scripting,java.sql,java.xml,jdk.unsupported \
  --strip-debug \
  --no-man-pages \
  --no-header-files \
  --compress=2 \
  --output "${BUNDLED_RUNTIME}"

# 5. Assemble standard macOS .app directory structure
echo ">>> Packaging application bundle..."
rm -rf "${PACKAGE_DIR}/${APP_NAME}.app"
mkdir -p "${APP_BUNDLE}/Contents/MacOS"
mkdir -p "${APP_BUNDLE}/Contents/Resources/Java"

# Move the stripped JRE inside the bundle
mv "${BUNDLED_RUNTIME}" "${APP_BUNDLE}/Contents/runtime"

# Copy desktop build outputs (lib and scripts)
cp -r "${GGD_SRC_SHADOW}/desktop/build/install/desktop/lib" "${APP_BUNDLE}/Contents/Resources/Java/"
cp -r "${GGD_SRC_SHADOW}/desktop/build/install/desktop/bin" "${APP_BUNDLE}/Contents/Resources/"

# 6. Remove cross-platform native libraries
LIB_DIR="${APP_BUNDLE}/Contents/Resources/Java/lib"
rm -f "$LIB_DIR"/*-windows-* \
      "$LIB_DIR"/*-linux-* \
      "$LIB_DIR"/*-solaris-* \
      "$LIB_DIR"/*-android-* \
      "$LIB_DIR"/zspace* \
      "$LIB_DIR"/libpxcclr*

# 7. Create launcher pointing to bundled JRE
cat << 'EOF' > "${APP_BUNDLE}/Contents/MacOS/geogebra-discovery"
#!/usr/bin/env bash
BUNDLE_DIR="$(cd "$(dirname "$0")/.." && pwd)"

# Force launcher to use the bundled Java runtime
export JAVA_HOME="${BUNDLE_DIR}/runtime"
export PATH="${JAVA_HOME}/bin:${PATH}"

exec "${BUNDLE_DIR}/Resources/bin/desktop" "$@"
EOF
chmod +x "${APP_BUNDLE}/Contents/MacOS/geogebra-discovery"

# 8. Create Info.plist
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

if [ -f "${SRC_DIR}/flatpak/gd-512.png" ]; then
  cp "${SRC_DIR}/flatpak/gd-512.png" "${APP_BUNDLE}/Contents/Resources/app.png"
fi

# 9. Output Archive
mkdir -p "${OUT_DIR}"
OUTPUT_ARCHIVE="${OUT_DIR}/geogebra-discovery-${VERSION}-macos-${ARCH}.zip"

echo ">>> Creating ZIP archive: ${OUTPUT_ARCHIVE}"
(cd "${PACKAGE_DIR}" && zip -qyr "${OUTPUT_ARCHIVE}" "${APP_NAME}.app")

echo ">>> Done."