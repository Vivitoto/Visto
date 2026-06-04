#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
for p in [
    Path('app/src/main/AndroidManifest.xml'),
    Path('app/src/main/res/values/strings.xml'),
    Path('app/src/main/res/values/themes.xml'),
    Path('app/src/main/res/drawable/ic_launcher.xml'),
]:
    ET.parse(p)
    print(f'xml ok: {p}')
required = [
    'settings.gradle.kts',
    'build.gradle.kts',
    'gradlew',
    'gradle/wrapper/gradle-wrapper.jar',
    'gradle/wrapper/gradle-wrapper.properties',
    'app/build.gradle.kts',
    'app/src/main/java/app/visto/AppInfo.kt',
    'app/src/main/java/app/visto/MainActivity.kt',
    'app/src/test/java/app/visto/ProjectSmokeTest.kt',
    '.github/workflows/build.yml',
]
for name in required:
    assert Path(name).exists(), name
    print(f'exists: {name}')
PY

if command -v java >/dev/null 2>&1; then
  ./gradlew testDebugUnitTest
else
  echo 'java not found; skipped Gradle unit tests. CI will run tests and assembleRelease.'
fi
