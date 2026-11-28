#!/data/data/com.termux/files/usr/bin/bash
set -e

STUDIO_DIR="${TERMUX_STUDIO_HOME:-$HOME/.termux_studio}"
CONFIG_FILE="${PWD}/build.config"
ARG="${1:-}"
DISTRO_ARG="${2:-debian}"

write_config() {
  local platform="$1"
  local pkg_format="$2"
  local distro="$3"
  local compiler="$4"
  local arch="$5"
  local gpu_api="$6"

  cat > "$CONFIG_FILE" <<EOF
# Termux studio App Builder Configuration (Supports C, C++, Zig, ASM, Rust via .o object linking)
APP_NAME=MyTermuxApp
PACKAGE_ID=com.termux.studio.myapp
VERSION=1.0.0
TARGET_PLATFORM=${platform}
LINUX_DISTRO=${distro}
PACKAGE_FORMAT=${pkg_format}
TARGET_ARCH=${arch}
COMPILER=${compiler}
ENTRY_SOURCE=main.cpp
EXTRA_OBJECTS=
INCLUDE_DIRS=.,include,libs/app,libs/raylib/include,${STUDIO_DIR}/include
EXTRA_LIBS=-lm -lpthread -ldl
ENABLE_RAYLIB=false
ENABLE_GPU_DRIVERS=true
GPU_API=${gpu_api}
OUTPUT_DIR=dist
EOF
  echo "[App] Wrote build.config for ${platform} (${pkg_format}, distro=${distro}, gpu=${gpu_api})"
}

compile_to_object() {
  local src="$1"
  local obj_out="$2"
  local inc_flags="$3"

  case "$src" in
    *.zig)
      echo "[App] Compiling Zig -> object: $src"
      zig build-obj "$src" -femit-bin="$obj_out" -lc $inc_flags
      ;;
    *.rs)
      echo "[App] Compiling Rust -> object: $src"
      rustc --emit=obj -O "$src" -o "$obj_out"
      ;;
    *.asm)
      echo "[App] Assembling NASM -> object: $src"
      nasm -f elf64 "$src" -o "$obj_out"
      ;;
    *.s|*.S)
      echo "[App] Assembling Gas/Clang -> object: $src"
      clang -c "$src" -o "$obj_out"
      ;;
    *.c)
      echo "[App] Compiling C -> object: $src"
      clang -c -fPIC $inc_flags "$src" -o "$obj_out"
      ;;
    *.cpp|*.cc|*.cxx)
      echo "[App] Compiling C++ -> object: $src"
      clang++ -std=c++20 -c -fPIC $inc_flags "$src" -o "$obj_out"
      ;;
    *.o|*.a)
      cp -f "$src" "$obj_out"
      ;;
    *)
      echo "[App] Unsupported source extension for object build: $src"
      exit 1
      ;;
  esac
}

build_linked_objects() {
  local obj_dir="$1"
  local inc_flags="$2"
  mkdir -p "$obj_dir"

  local app_rt=""
  if [ -f "libs/app/app_runtime.c" ]; then
    app_rt="libs/app/app_runtime.c"
  elif [ -f "${STUDIO_DIR}/include/app/app_runtime.c" ]; then
    app_rt="${STUDIO_DIR}/include/app/app_runtime.c"
  fi

  LINK_OBJS=""
  if [ -n "$app_rt" ]; then
    echo "[App] Compiling app runtime -> app_runtime.o"
    clang -c -fPIC $inc_flags "$app_rt" -o "$obj_dir/app_runtime.o"
    LINK_OBJS="$obj_dir/app_runtime.o"
  fi

  local idx=0
  local sources_raw="${ENTRY_SOURCES:-${ENTRY_SOURCE:-main.cpp}}"
  for src in $(echo "$sources_raw" | tr ',' ' '); do
    [ -z "$src" ] && continue
    local obj_path="$obj_dir/unit_${idx}.o"
    compile_to_object "$src" "$obj_path" "$inc_flags"
    LINK_OBJS="$LINK_OBJS $obj_path"
    idx=$((idx + 1))
  done

  if [ -n "${EXTRA_OBJECTS:-}" ]; then
    LINK_OBJS="$LINK_OBJS ${EXTRA_OBJECTS}"
  fi
}

case "$ARG" in
  Android|android)
    write_config "Android" "apk" "android" "clang++" "aarch64-linux-android" "GLES3+Vulkan"
    ;;
  Linux|linux)
    PKG="deb"
    case "$DISTRO_ARG" in
      fedora|rhel|centos) PKG="rpm" ;;
      arch|manjaro) PKG="pkg.tar.zst" ;;
      appimage|AppImage) PKG="AppImage" ;;
      *) PKG="deb" ;;
    esac
    write_config "Linux" "$PKG" "$DISTRO_ARG" "clang++" "aarch64-linux-gnu" "OpenGL+Vulkan+Zink"
    ;;
  Window|window|Windows|windows)
    write_config "Window" "exe" "windows" "x86_64-w64-mingw32-g++" "x86_64-w64-mingw32" "OpenGL+DirectX"
    ;;
  macOS|macos|MacOS)
    write_config "macOS" "app_bundle" "darwin" "clang++" "arm64-apple-darwin" "Metal+OpenGL"
    ;;
  build)
    if [ ! -f "$CONFIG_FILE" ]; then
      echo "[App] Error: build.config not found. Run: app Android | app Linux | app Window | app macOS"
      exit 1
    fi
    set -a
    # shellcheck disable=SC1090
    source "$CONFIG_FILE"
    set +a

    APP_NAME="${APP_NAME:-MyTermuxApp}"
    PACKAGE_ID="${PACKAGE_ID:-com.termux.studio.myapp}"
    VERSION="${VERSION:-1.0.0}"
    OUTPUT_DIR="${OUTPUT_DIR:-dist}"
    mkdir -p "$OUTPUT_DIR"

    INC_FLAGS="-I. -Iinclude -Ilibs -Ilibs/app -Ilibs/raylib/include -Ilibs/raylib -I${STUDIO_DIR}/include -I${STUDIO_DIR}/include/app"
    LIB_FLAGS="-Llibs -Llibs/raylib/lib ${EXTRA_LIBS:--lm -lpthread -ldl}"
    if [ "${ENABLE_RAYLIB:-false}" = "true" ]; then
      LIB_FLAGS="-Llibs/raylib/lib -lraylib -lGLESv2 -lEGL ${LIB_FLAGS}"
    fi
    if [ "${ENABLE_GPU_DRIVERS:-true}" = "true" ]; then
      export TERMUX_STUDIO_GPU=1
      export GALLIUM_DRIVER="${GALLIUM_DRIVER:-zink}"
      export MESA_LOADER_DRIVER_OVERRIDE="${MESA_LOADER_DRIVER_OVERRIDE:-zink}"
    fi

    echo "[App] Building ${APP_NAME} v${VERSION} (${ENTRY_SOURCES:-$ENTRY_SOURCE}) for ${TARGET_PLATFORM}..."
    OBJ_STAGE="${OUTPUT_DIR}/obj_stage"
    rm -rf "$OBJ_STAGE"
    build_linked_objects "$OBJ_STAGE" "$INC_FLAGS"

    LINKER_BIN="${COMPILER:-clang++}"
    command -v "$LINKER_BIN" >/dev/null 2>&1 || LINKER_BIN="clang++"
    command -v "$LINKER_BIN" >/dev/null 2>&1 || LINKER_BIN="g++"

    case "$TARGET_PLATFORM" in
      Android)
        OUT_SO="${OUTPUT_DIR}/lib${APP_NAME}.so"
        OUT_APK="${OUTPUT_DIR}/${APP_NAME}.apk"
        $LINKER_BIN -shared -fPIC $LINK_OBJS $LIB_FLAGS -o "$OUT_SO"
        STAGE="${OUTPUT_DIR}/apk_stage"
        rm -rf "$STAGE" && mkdir -p "$STAGE/lib/arm64-v8a"
        cp "$OUT_SO" "$STAGE/lib/arm64-v8a/"
        cat > "$STAGE/AndroidManifest.xml" <<MANIFEST
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="${PACKAGE_ID}" android:versionName="${VERSION}">
    <uses-feature android:glEsVersion="0x00030000" android:required="false" />
    <application android:label="${APP_NAME}" android:hasCode="false">
        <activity android:name="android.app.NativeActivity" android:exported="true" />
    </application>
</manifest>
MANIFEST
        if command -v zip >/dev/null 2>&1; then
          (cd "$STAGE" && zip -qr "../${APP_NAME}.apk" .)
        else
          tar -cf "$OUT_APK" -C "$STAGE" .
        fi
        echo "[App] Packaged Android APK: ${OUT_APK}"
        ;;
      Linux)
        BIN_PATH="${OUTPUT_DIR}/${APP_NAME}"
        $LINKER_BIN $LINK_OBJS $LIB_FLAGS -o "$BIN_PATH"
        if [ "${PACKAGE_FORMAT:-deb}" = "deb" ]; then
          DEB_STAGE="${OUTPUT_DIR}/${APP_NAME}_${VERSION}_arm64"
          rm -rf "$DEB_STAGE" && mkdir -p "$DEB_STAGE/DEBIAN" "$DEB_STAGE/usr/local/bin"
          cp "$BIN_PATH" "$DEB_STAGE/usr/local/bin/${APP_NAME}"
          chmod 755 "$DEB_STAGE/usr/local/bin/${APP_NAME}"
          cat > "$DEB_STAGE/DEBIAN/control" <<CTRL
Package: $(echo "$APP_NAME" | tr '[:upper:]_' '[:lower:]-')
Version: ${VERSION}
Section: utils
Priority: optional
Architecture: arm64
Maintainer: Termux studio
Description: ${APP_NAME} built via App multi-language object linker
CTRL
          OUT_DEB="${OUTPUT_DIR}/${APP_NAME}_${VERSION}_arm64.deb"
          if command -v dpkg-deb >/dev/null 2>&1; then
            dpkg-deb --build "$DEB_STAGE" "$OUT_DEB"
          else
            tar -czf "$OUT_DEB" -C "$DEB_STAGE" .
          fi
          echo "[App] Packaged Linux DEB: ${OUT_DEB}"
        else
          echo "[App] Built Linux (${LINUX_DISTRO}) artifact in ${OUTPUT_DIR}/"
        fi
        ;;
      Window|Windows)
        OUT_EXE="${OUTPUT_DIR}/${APP_NAME}.exe"
        if command -v x86_64-w64-mingw32-g++ >/dev/null 2>&1 && [[ "${ENTRY_SOURCES:-$ENTRY_SOURCE}" == *.cpp || "${ENTRY_SOURCES:-$ENTRY_SOURCE}" == *.c ]]; then
          x86_64-w64-mingw32-g++ -std=c++20 $INC_FLAGS ${ENTRY_SOURCES:-$ENTRY_SOURCE} -static $LIB_FLAGS -o "$OUT_EXE"
        else
          $LINKER_BIN $LINK_OBJS $LIB_FLAGS -o "${OUTPUT_DIR}/${APP_NAME}"
          cp "${OUTPUT_DIR}/${APP_NAME}" "$OUT_EXE"
        fi
        echo "[App] Packaged Windows EXE: ${OUT_EXE}"
        ;;
      macOS)
        BUNDLE="${OUTPUT_DIR}/${APP_NAME}.app"
        mkdir -p "$BUNDLE/Contents/MacOS" "$BUNDLE/Contents/Resources"
        $LINKER_BIN $LINK_OBJS $LIB_FLAGS -o "$BUNDLE/Contents/MacOS/${APP_NAME}"
        cat > "$BUNDLE/Contents/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>CFBundleName</key><string>${APP_NAME}</string>
  <key>CFBundleIdentifier</key><string>${PACKAGE_ID}</string>
  <key>CFBundleVersion</key><string>${VERSION}</string>
  <key>CFBundleExecutable</key><string>${APP_NAME}</string>
</dict></plist>
PLIST
        echo "[App] Packaged macOS .app bundle: ${BUNDLE}"
        ;;
      *)
        echo "[App] Unknown TARGET_PLATFORM: $TARGET_PLATFORM"
        exit 1
        ;;
    esac
    ;;
  *)
    echo "Usage: app <Android | Linux [debian|ubuntu|fedora|arch|appimage] | Window | macOS | build>"
    exit 1
    ;;
esac
