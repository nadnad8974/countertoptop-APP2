from pathlib import Path
import re

main_path = Path("app/src/main/java/com/ramsiers/graniteapp/MainActivity.java")
build_path = Path("app/build.gradle")
changed = False

build = build_path.read_text()
new_build = re.sub(r"versionCode\s+\d+", "versionCode 9", build)
new_build = re.sub(r"versionName\s+'[^']+'", "versionName '1.8-test'", new_build)
if new_build != build:
    build_path.write_text(new_build)
    changed = True

main = main_path.read_text()
replacements = {
    '        addHelp("Use Up and Down to move pages. Remove hides a page. Add Page can bring hidden pages back.");\n\n'
    '        Button addPage = primaryButton("Add page");\n'
    '        addPage.setOnClickListener(v -> showAddPageDialog());\n'
    '        page.addView(addPage);':
    '        addHelp("Add a new page and name it, or add a deleted page back.");\n\n'
    '        Button addNewPage = primaryButton("Add new page");\n'
    '        addNewPage.setOnClickListener(v -> showCustomPageDialog());\n'
    '        page.addView(addNewPage);\n\n'
    '        Button restorePage = secondaryButton("Add deleted page back");\n'
    '        restorePage.setOnClickListener(v -> showAddPageDialog());\n'
    '        page.addView(restorePage);',
    '        labels.add("Create new custom question");\n'
    '        String[] items = labels.toArray(new String[0]);\n'
    '        new AlertDialog.Builder(this)\n'
    '                .setTitle("Add page")\n'
    '                .setItems(items, (dialog, which) -> {\n'
    '                    if (which < availableIds.size()) {\n'
    '                        int pageId = availableIds.get(which);\n'
    '                        pageOrder.add(pageId);\n'
    '                        savePageOrder();\n'
    '                        showManagePagesScreen();\n'
    '                    } else {\n'
    '                        showCustomPageDialog();\n'
    '                    }\n'
    '                })':
    '        if (labels.isEmpty()) {\n'
    '            Toast.makeText(this, "There are no deleted pages to add back.", Toast.LENGTH_LONG).show();\n'
    '            return;\n'
    '        }\n'
    '        String[] items = labels.toArray(new String[0]);\n'
    '        new AlertDialog.Builder(this)\n'
    '                .setTitle("Add deleted page back")\n'
    '                .setItems(items, (dialog, which) -> {\n'
    '                    int pageId = availableIds.get(which);\n'
    '                    pageOrder.add(pageId);\n'
    '                    savePageOrder();\n'
    '                    showManagePagesScreen();\n'
    '                })',
    '                .setTitle("Create custom page")': '                .setTitle("Add new page")',
}
for old, new in replacements.items():
    if old in main:
        main = main.replace(old, new)
        changed = True

if changed:
    main_path.write_text(main)
    print("Applied v1.8 direct add-new-page button.")
else:
    print("v1.8 direct add-new-page button already applied.")
