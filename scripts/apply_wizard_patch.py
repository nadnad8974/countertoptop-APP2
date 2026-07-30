from pathlib import Path
import re

main_path = Path("app/src/main/java/com/ramsiers/graniteapp/MainActivity.java")
build_path = Path("app/build.gradle")
changed = False

build = build_path.read_text()
new_build = re.sub(r"versionCode\s+\d+", "versionCode 10", build)
new_build = re.sub(r"versionName\s+'[^']+'", "versionName '1.9-test'", new_build)
if new_build != build:
    build_path.write_text(new_build)
    changed = True

main = main_path.read_text()
old_url = 'return "https://www.msisurfaces.com/search/?search=" + Uri.encode(cleanMsiSearch(query));'
new_url = 'return "https://www.msisurfaces.com/site-search/?key=" + Uri.encode(cleanMsiSearch(query)) + "&ctgy=slab";'
if old_url in main:
    main = main.replace(old_url, new_url)
    changed = True

if changed:
    main_path.write_text(main)
    print("Applied v1.9 MSI slab search URL fix.")
else:
    print("v1.9 MSI slab search URL fix already applied.")
