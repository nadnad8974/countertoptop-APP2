from pathlib import Path
import re

main_path = Path("app/src/main/java/com/ramsiers/graniteapp/MainActivity.java")
build_path = Path("app/build.gradle")
changed = False

build = build_path.read_text()
new_build = re.sub(r"versionCode\s+\d+", "versionCode 11", build)
new_build = re.sub(r"versionName\s+'[^']+'", "versionName '1.10-test'", new_build)
if new_build != build:
    build_path.write_text(new_build)
    changed = True

main = main_path.read_text()
replacements = {
    "    private void showManagePagesScreen() {\n        hideKeyboard();":
    "    private void showManagePagesScreen() {\n        showManagePagesScreen(-1);\n    }\n\n"
    "    private void showManagePagesScreen(int keepIndexVisible) {\n        hideKeyboard();",
    "        for (int i = 0; i < pageOrder.size(); i++) {\n            final int index = i;\n            final int pageId = pageOrder.get(i);\n            LinearLayout row = itemRow();":
    "        final View[] keepVisibleRow = new View[1];\n"
    "        for (int i = 0; i < pageOrder.size(); i++) {\n            final int index = i;\n            final int pageId = pageOrder.get(i);\n            LinearLayout row = itemRow();\n            if (index == keepIndexVisible) {\n                keepVisibleRow[0] = row;\n            }",
    "        showManagePagesScreen();\n    }\n\n    private void removePage":
    "        showManagePagesScreen(to);\n    }\n\n    private void removePage",
}

for old, new in replacements.items():
    if old in main:
        main = main.replace(old, new, 1)
        changed = True

old_scroll = (
    "        page.addView(reset);\n"
    "        scroll.post(() -> scroll.smoothScrollTo(0, 0));\n"
    "    }\n\n"
    "    private void movePage"
)
new_scroll = (
    "        page.addView(reset);\n"
    "        scroll.post(() -> {\n"
    "            if (keepVisibleRow[0] != null) {\n"
    "                scroll.scrollTo(0, Math.max(0, keepVisibleRow[0].getTop() - dp(20)));\n"
    "            } else {\n"
    "                scroll.smoothScrollTo(0, 0);\n"
    "            }\n"
    "        });\n"
    "    }\n\n"
    "    private void movePage"
)
if old_scroll in main:
    main = main.replace(old_scroll, new_scroll, 1)
    changed = True

if changed:
    main_path.write_text(main)
    print("Applied v1.10 keep manage-pages scroll position.")
else:
    print("v1.10 keep manage-pages scroll position already applied.")
