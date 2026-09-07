#!/usr/bin/env bash
# Builds and signs DM Only without Android Studio or the Android SDK.
#
# Needs: a JDK (17+), curl, unzip, zip. Everything else is downloaded once into tools/.cache:
#   - android.jar (API 34)        from the Sable/android-platforms mirror on GitHub
#   - aapt2                        extracted from the Apktool release jar
#   - dx (dexer)                   from Maven Central (jakewharton repackaged build)
#   - uber-apk-signer              zipalign + v1/v2/v3 signing in one jar
#
# Output: app/build/manual/dmonly-debug.apk, signed with the repo's debug.keystore so it upgrades
# cleanly over an Android Studio build of the same project.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="$ROOT/tools/.cache"
OUT="$ROOT/app/build/manual"
SRC="$ROOT/app/src/main"

ANDROID_JAR_URL="https://raw.githubusercontent.com/Sable/android-platforms/master/android-34/android.jar"
APKTOOL_URL="https://github.com/iBotPeaches/Apktool/releases/download/v2.9.3/apktool_2.9.3.jar"
DX_URL="https://repo.maven.apache.org/maven2/com/jakewharton/android/repackaged/dalvik-dx/14.0.0_r21/dalvik-dx-14.0.0_r21.jar"
SIGNER_URL="https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar"

fetch() { # url dest
  if [ ! -s "$2" ]; then
    echo "downloading $(basename "$2")"
    curl -sS -L --fail -o "$2" "$1"
  fi
}

mkdir -p "$CACHE"
fetch "$ANDROID_JAR_URL" "$CACHE/android.jar"
fetch "$APKTOOL_URL" "$CACHE/apktool.jar"
fetch "$DX_URL" "$CACHE/dx.jar"
fetch "$SIGNER_URL" "$CACHE/uber-apk-signer.jar"

case "$(uname -s)" in
  Darwin) AAPT2_IN_JAR="prebuilt/macosx/aapt2_64" ;;
  *)      AAPT2_IN_JAR="prebuilt/linux/aapt2_64" ;;
esac
AAPT2="$CACHE/$AAPT2_IN_JAR"
if [ ! -x "$AAPT2" ]; then
  unzip -o -q "$CACHE/apktool.jar" "$AAPT2_IN_JAR" -d "$CACHE"
  chmod +x "$AAPT2"
fi

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes"

echo "compiling resources"
"$AAPT2" compile --dir "$SRC/res" -o "$OUT/res.zip"

# Gradle takes the package name from build.gradle.kts; aapt2 on its own needs it in the manifest.
sed 's#<manifest #<manifest package="app.dmonly" #' "$SRC/AndroidManifest.xml" > "$OUT/AndroidManifest.xml"

echo "linking resources"
"$AAPT2" link -o "$OUT/unsigned.apk" -I "$CACHE/android.jar" \
  --manifest "$OUT/AndroidManifest.xml" --java "$OUT/gen" \
  --min-sdk-version 26 --target-sdk-version 34 --version-code 1 --version-name 1.0 \
  "$OUT/res.zip"

echo "compiling java"
find "$SRC/java" "$OUT/gen" -name '*.java' > "$OUT/sources.txt"
javac -source 8 -target 8 -Xlint:-options -bootclasspath "$CACHE/android.jar" \
  -d "$OUT/classes" @"$OUT/sources.txt"

echo "dexing"
java -cp "$CACHE/dx.jar" com.android.dx.command.Main --dex --min-sdk-version=26 \
  --output="$OUT/classes.dex" "$OUT/classes"
(cd "$OUT" && zip -q -j unsigned.apk classes.dex)

echo "aligning and signing"
java -jar "$CACHE/uber-apk-signer.jar" --apks "$OUT/unsigned.apk" --out "$OUT/signed" \
  --ks "$ROOT/debug.keystore" --ksAlias androiddebugkey --ksPass android --ksKeyPass android
mv "$OUT"/signed/*-aligned-*signed.apk "$OUT/dmonly-debug.apk"

echo
echo "built: $OUT/dmonly-debug.apk"
