from pathlib import Path
import re

main_path = Path("app/src/main/java/com/ramsiers/graniteapp/MainActivity.java")
build_path = Path("app/build.gradle")
changed = False

build = build_path.read_text()
new_build = re.sub(r"versionCode\s+\d+", "versionCode 5", build)
new_build = re.sub(r"versionName\s+'[^']+'", "versionName '1.4-test'", new_build)
if new_build != build:
    build_path.write_text(new_build)
    changed = True

main = main_path.read_text()
replacements = {
    "private static final int TOTAL_STEPS = 21;": "private static final int TOTAL_STEPS = 14;",
    "private static final int TOTAL_STEPS = 13;": "private static final int TOTAL_STEPS = 14;",
    "page.setPadding(dp(16), dp(12), dp(16), dp(28));": "page.setPadding(dp(16), dp(12), dp(16), dp(110));",
    "navigation.setPadding(dp(12), dp(8), dp(12), dp(10));": "navigation.setPadding(dp(12), dp(8), dp(12), dp(58));",
}
for old, new in replacements.items():
    if old in main:
        main = main.replace(old, new)
        changed = True

old_inline = """        Button inlineNext = primaryButton(nextButtonText());
        inlineNext.setOnClickListener(v -> handleNext());
        page.addView(inlineNext);"""
if old_inline in main:
    main = main.replace(old_inline, "        addInlineNavigation();")
    changed = True

if "private void addInlineNavigation()" not in main:
    marker = """    private String nextButtonText() {
        if (stepIndex >= TOTAL_STEPS - 1) {"""
    inline_method = """    private void addInlineNavigation() {
        LinearLayout inline = new LinearLayout(this);
        inline.setOrientation(LinearLayout.HORIZONTAL);
        inline.setGravity(Gravity.CENTER);
        inline.setPadding(0, dp(6), 0, dp(10));

        Button back = secondaryButton(\"Back\");
        back.setEnabled(stepIndex > 0);
        back.setOnClickListener(v -> {
            hideKeyboard();
            stepIndex = Math.max(0, stepIndex - 1);
            showStep();
        });
        inline.addView(back, new LinearLayout.LayoutParams(0, dp(54), 1f));

        Button next = primaryButton(nextButtonText());
        next.setOnClickListener(v -> handleNext());
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(0, dp(54), 1f);
        nextParams.setMargins(dp(8), 0, 0, 0);
        inline.addView(next, nextParams);
        page.addView(inline, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

"""
    if marker in main:
        main = main.replace(marker, inline_method + marker)
        changed = True

old_cases = re.compile(r"            case 6:\n                addSlabStep\(\);\n                break;\n            case 7:.*?            default:", re.S)
new_cases = """            case 6:
                addSlabStep();
                break;
            case 7:
                addQuestion(\"What is the installed price per square foot?\", pricePerSqFt, true);
                break;
            case 8:
                addQuestion(\"What is the sink or cutout charge?\", sinkCharge, true);
                break;
            case 9:
                addQuestion(\"What is the edge or extra labor charge?\", edgeCharge, true);
                break;
            case 10:
                addQuestion(\"What is the tear-out charge?\", tearOutCharge, true);
                break;
            case 11:
                addQuestion(\"Are there any other charges?\", otherCharge, true);
                break;
            case 12:
                addPhotoStep();
                break;
            default:"""
main2 = old_cases.sub(new_cases, main)
if main2 != main:
    main = main2
    changed = True

remove_next_special = """        if (stepIndex == 11) {
            return \"No, continue\";
        }
"""
if remove_next_special in main:
    main = main.replace(remove_next_special, "")
    changed = True

validation_block = """        if (stepIndex == 8 && value(lengthIn) <= 0) {
            Toast.makeText(this, \"Enter a length greater than zero.\", Toast.LENGTH_SHORT).show();
            return;
        }
        if (stepIndex == 9 && value(widthIn) <= 0) {
            Toast.makeText(this, \"Enter a width greater than zero.\", Toast.LENGTH_SHORT).show();
            return;
        }
        if (stepIndex == 10 && !addCounterSection()) {
            return;
        }
"""
if validation_block in main:
    main = main.replace(validation_block, "")
    changed = True

if changed:
    main_path.write_text(main)
    print("Applied v1.4 flow and keyboard navigation fix.")
else:
    print("v1.4 flow and keyboard navigation fix already applied.")
