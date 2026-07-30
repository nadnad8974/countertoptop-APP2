from pathlib import Path
import re

main_path = Path("app/src/main/java/com/ramsiers/graniteapp/MainActivity.java")
build_path = Path("app/build.gradle")
changed = False

build = build_path.read_text()
new_build = re.sub(r"versionCode\s+\d+", "versionCode 13", build)
new_build = re.sub(r"versionName\s+'[^']+'", "versionName '1.12-test'", new_build)
if new_build != build:
    build_path.write_text(new_build)
    changed = True

main = main_path.read_text()
new_visualizer = (
    'private static final String MSI_VISUALIZER = '
    '"https://www.roomvo.com/my/msi/?product_type=1&multi_product_visualizer=5";'
)
updated_main = re.sub(
    r'private static final String MSI_VISUALIZER = "[^"]+";',
    new_visualizer,
    main,
)
if updated_main != main:
    main = updated_main
    changed = True
replacements = {
    "    private void showManagePagesScreen() {\n        hideKeyboard();":
    "    private void showManagePagesScreen() {\n        showManagePagesScreen(-1);\n    }\n\n"
    "    private void showManagePagesScreen(int keepIndexVisible) {\n        hideKeyboard();",
    "        showManagePagesScreen();\n    }\n\n    private void removePage":
    "        showManagePagesScreen(to);\n    }\n\n    private void removePage",
}

for old, new in replacements.items():
    if old in main:
        main = main.replace(old, new, 1)
        changed = True

old_row_loop = (
    "        for (int i = 0; i < pageOrder.size(); i++) {\n"
    "            final int index = i;\n"
    "            final int pageId = pageOrder.get(i);\n"
    "            LinearLayout row = itemRow();"
)
new_row_loop = (
    "        final View[] keepVisibleRow = new View[1];\n"
    "        for (int i = 0; i < pageOrder.size(); i++) {\n"
    "            final int index = i;\n"
    "            final int pageId = pageOrder.get(i);\n"
    "            LinearLayout row = itemRow();\n"
    "            if (index == keepIndexVisible) {\n"
    "                keepVisibleRow[0] = row;\n"
    "            }"
)
if "final View[] keepVisibleRow = new View[1];" not in main and old_row_loop in main:
    main = main.replace(old_row_loop, new_row_loop, 1)
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

def add_navigation_once(method_name, anchor):
    global main, changed
    pattern = (
        r"(    private void " + re.escape(method_name) +
        r"\(\) \{\n)(.*?)(\n    \}\n\n    private void )"
    )
    match = re.search(pattern, main, flags=re.S)
    if not match or "addInlineNavigation();" in match.group(2):
        return
    body = match.group(2)
    if anchor not in body:
        return
    body = body.replace(anchor, anchor + "\n        addInlineNavigation();", 1)
    main = main[:match.start(2)] + body + main[match.end(2):]
    changed = True

add_navigation_once("addSlabStep", "        renderSlabs();")
add_navigation_once("addAnotherSectionStep", "        addHelp(\"Tap Next below to continue without adding another section.\");")
add_navigation_once("addPhotoStep", "        addHelp(\"You may skip the photo and tap Next.\");")
add_navigation_once("addReviewStep", "        page.addView(totalResult);")

if changed:
    main_path.write_text(main)
    print("Applied v1.12 Roomvo MSI visualizer link.")
else:
    print("v1.12 Roomvo MSI visualizer link already applied.")
