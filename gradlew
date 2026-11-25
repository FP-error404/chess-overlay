#!/bin/sh
# Lightweight Gradle bootstrap for this project.
# Downloads the pinned Gradle distribution on first use, then delegates to it.
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_PROPS="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"
DIST_URL=$(sed -n 's/^distributionUrl=//p' "$WRAPPER_PROPS" | sed 's/\\:/:/g')
if [ -z "$DIST_URL" ]; then
  echo "Missing distributionUrl in $WRAPPER_PROPS" >&2
  exit 1
fi
DIST_ARCHIVE=$(basename "$DIST_URL" .zip)
DIST_NAME=${DIST_ARCHIVE%-bin}
GRADLE_USER_HOME_DIR=${GRADLE_USER_HOME:-"$HOME/.gradle"}
INSTALL_DIR="$GRADLE_USER_HOME_DIR/wrapper/dists/chess-overlay/$DIST_ARCHIVE"
GRADLE_BIN="$INSTALL_DIR/$DIST_NAME/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$INSTALL_DIR"
  ZIP_PATH="$INSTALL_DIR/$DIST_ARCHIVE.zip"
  echo "Downloading $DIST_ARCHIVE (first run only)..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 2 "$DIST_URL" -o "$ZIP_PATH"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP_PATH" "$DIST_URL"
  else
    echo "curl or wget is required to download Gradle." >&2
    exit 1
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "$ZIP_PATH" -d "$INSTALL_DIR"
  else
    echo "unzip is required to extract Gradle." >&2
    exit 1
  fi
  rm -f "$ZIP_PATH"
fi
exec "$GRADLE_BIN" -p "$APP_HOME" "$@"
