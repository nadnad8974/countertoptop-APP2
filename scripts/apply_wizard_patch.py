from pathlib import Path
import re

build_path = Path("app/build.gradle")
build = build_path.read_text()
new_build = re.sub(r"versionCode\s+\d+", "versionCode 8", build)
new_build = re.sub(r"versionName\s+'[^']+'", "versionName '1.7-test'", new_build)
if new_build != build:
    build_path.write_text(new_build)
    print("Applied v1.7 version label.")
else:
    print("v1.7 page editor source already prepared.")
