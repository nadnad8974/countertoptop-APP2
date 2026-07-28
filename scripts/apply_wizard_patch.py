from pathlib import Path
import re

main_path = Path("app/src/main/java/com/ramsiers/graniteapp/MainActivity.java")
build_path = Path("app/build.gradle")
changed = False

build = build_path.read_text()
new_build = re.sub(r"versionCode\s+\d+", "versionCode 4", build)
new_build = re.sub(r"versionName\s+'[^']+'", "versionName '1.3-test'", new_build)
if new_build != build:
    build_path.write_text(new_build)
    changed = True

main = main_path.read_text()
if "Button inlineNext = primaryButton(nextButtonText());" not in main:
    main = main.replace(
        "        detach(field);\n        page.addView(field);\n        if (focus) {",
        "        detach(field);\n        page.addView(field);\n        Button inlineNext = primaryButton(nextButtonText());\n        inlineNext.setOnClickListener(v -> handleNext());\n        page.addView(inlineNext);\n        if (focus) {"
    )
    changed = True

old_nav = """        String nextText;
        if (stepIndex >= TOTAL_STEPS - 1) {
            nextText = \"Send email\";
        } else if (stepIndex == 11) {
            nextText = \"No, continue\";
        } else {
            nextText = \"Next\";
        }

        Button next = primaryButton(nextText);
        next.setOnClickListener(v -> handleNext());
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(0, dp(54), 1f);
        nextParams.setMargins(dp(8), 0, 0, 0);
        navigation.addView(next, nextParams);
    }

    private void handleNext() {"""
new_nav = """        Button next = primaryButton(nextButtonText());
        next.setOnClickListener(v -> handleNext());
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(0, dp(54), 1f);
        nextParams.setMargins(dp(8), 0, 0, 0);
        navigation.addView(next, nextParams);
    }

    private String nextButtonText() {
        if (stepIndex >= TOTAL_STEPS - 1) {
            return \"Send email\";
        }
        if (stepIndex == 11) {
            return \"No, continue\";
        }
        return \"Next\";
    }

    private void handleNext() {"""
if "private String nextButtonText()" not in main and old_nav in main:
    main = main.replace(old_nav, new_nav)
    changed = True

if changed:
    main_path.write_text(main)
    print("Applied keyboard-safe Next button fix.")
else:
    print("Keyboard-safe Next button fix already applied.")
