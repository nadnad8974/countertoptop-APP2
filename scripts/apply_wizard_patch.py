from pathlib import Path
import re

main_path = Path("app/src/main/java/com/ramsiers/graniteapp/MainActivity.java")
build_path = Path("app/build.gradle")
changed = False

build = build_path.read_text()
new_build = re.sub(r"versionCode\s+\d+", "versionCode 6", build)
new_build = re.sub(r"versionName\s+'[^']+'", "versionName '1.5-test'", new_build)
if new_build != build:
    build_path.write_text(new_build)
    changed = True

main = main_path.read_text()

for old, new in {
    "    private static final int TOTAL_STEPS = 21;\n": "",
    "    private static final int TOTAL_STEPS = 14;\n": "",
    "    private static final int TOTAL_STEPS = 13;\n": "",
    "page.setPadding(dp(16), dp(12), dp(16), dp(28));": "page.setPadding(dp(16), dp(12), dp(16), dp(110));",
    "navigation.setPadding(dp(12), dp(8), dp(12), dp(10));": "navigation.setPadding(dp(12), dp(8), dp(12), dp(58));",
}.items():
    if old in main:
        main = main.replace(old, new)
        changed = True

if "DEFAULT_PAGE_ORDER" not in main:
    marker = '    private static final String MSI_VISUALIZER = "https://www.msisurfaces.com/room-visualizer-tools/";\n'
    constants = marker + '''    private static final String DEFAULT_PAGE_ORDER = "0,1,2,3,4,5,6,7,8,9,10,11,12";
    private static final int PAGE_NAME = 0;
    private static final int PAGE_PHONE = 1;
    private static final int PAGE_EMAIL = 2;
    private static final int PAGE_ADDRESS = 3;
    private static final int PAGE_NOTES = 4;
    private static final int PAGE_OFFICE_EMAIL = 5;
    private static final int PAGE_SLABS = 6;
    private static final int PAGE_PRICE = 7;
    private static final int PAGE_SINK_CHARGE = 8;
    private static final int PAGE_EDGE_CHARGE = 9;
    private static final int PAGE_TEAR_OUT = 10;
    private static final int PAGE_OTHER_CHARGE = 11;
    private static final int PAGE_PHOTO = 12;
    private static final int PAGE_SECTION_NAME = 100;
    private static final int PAGE_SECTION_LENGTH = 101;
    private static final int PAGE_SECTION_WIDTH = 102;
    private static final int PAGE_SECTION_QUANTITY = 103;
    private static final int PAGE_ADD_SECTION = 104;
    private static final int PAGE_STOVE_LENGTH = 105;
    private static final int PAGE_STOVE_WIDTH = 106;
'''
    main = main.replace(marker, constants)
    changed = True

if "private final ArrayList<Integer> pageOrder" not in main:
    main = main.replace(
        "    private final ArrayList<CounterSection> sections = new ArrayList<>();\n",
        "    private final ArrayList<CounterSection> sections = new ArrayList<>();\n"
        "    private final ArrayList<Integer> pageOrder = new ArrayList<>();\n"
    )
    changed = True

if "loadPageOrder();" not in main:
    main = main.replace(
        "        loadSavedLists();\n        buildUi();",
        "        loadSavedLists();\n        loadPageOrder();\n        buildUi();"
    )
    changed = True

main = main.replace(
    'TextView progress = label("Question " + (stepIndex + 1) + " of " + TOTAL_STEPS);',
    'TextView progress = label("Question " + (stepIndex + 1) + " of " + totalSteps());'
)

switch_pattern = re.compile(r'''        switch \(stepIndex\) \{
.*?
        addNavigation\(\);''', re.S)
new_flow = '''        Button movePages = secondaryButton("Move pages");
        movePages.setOnClickListener(v -> showMovePagesScreen());
        page.addView(movePages);

        if (stepIndex >= pageOrder.size()) {
            addReviewStep();
        } else {
            showQuestionPage(pageOrder.get(stepIndex));
        }

        addNavigation();'''
main2 = switch_pattern.sub(new_flow, main, count=1)
if main2 != main:
    main = main2
    changed = True

if "private void showQuestionPage(int pageId)" not in main:
    marker = "    private void addBrandHeader() {"
    show_question = '''    private void showQuestionPage(int pageId) {
        switch (pageId) {
            case PAGE_NAME:
                addQuestion("What is the customer's name?", customerName, true);
                break;
            case PAGE_PHONE:
                addQuestion("What is the customer's phone number?", customerPhone, true);
                break;
            case PAGE_EMAIL:
                addQuestion("What is the customer's email address?", customerEmail, true);
                break;
            case PAGE_ADDRESS:
                addQuestion("What is the project address?", projectAddress, true);
                break;
            case PAGE_NOTES:
                addQuestion("Are there any project notes?", projectNotes, true);
                break;
            case PAGE_OFFICE_EMAIL:
                addQuestion("What email should receive the quote request?", officeEmail, true);
                addHelp("This RAMSIER'S office email is saved on this phone.");
                break;
            case PAGE_SLABS:
                addSlabStep();
                break;
            case PAGE_PRICE:
                addQuestion("What is the installed price per square foot?", pricePerSqFt, true);
                break;
            case PAGE_SINK_CHARGE:
                addQuestion("What is the sink or cutout charge?", sinkCharge, true);
                break;
            case PAGE_EDGE_CHARGE:
                addQuestion("What is the edge or extra labor charge?", edgeCharge, true);
                break;
            case PAGE_TEAR_OUT:
                addQuestion("What is the tear-out charge?", tearOutCharge, true);
                break;
            case PAGE_OTHER_CHARGE:
                addQuestion("Are there any other charges?", otherCharge, true);
                break;
            case PAGE_PHOTO:
                addPhotoStep();
                break;
            case PAGE_SECTION_NAME:
                addQuestion("What should this countertop section be called?", sectionName, true);
                break;
            case PAGE_SECTION_LENGTH:
                addQuestion("What is the section length in inches?", lengthIn, true);
                break;
            case PAGE_SECTION_WIDTH:
                addQuestion("What is the section width in inches?", widthIn, true);
                break;
            case PAGE_SECTION_QUANTITY:
                addQuestion("How many identical sections are there?", quantity, true);
                addHelp("Tapping Next saves this countertop section.");
                break;
            case PAGE_ADD_SECTION:
                addAnotherSectionStep();
                break;
            case PAGE_STOVE_LENGTH:
                addQuestion("What is the slide-in stove opening length?", stoveLength, true);
                addHelp("Leave this blank if there is no slide-in stove opening.");
                break;
            case PAGE_STOVE_WIDTH:
                addQuestion("What is the slide-in stove opening width?", stoveWidth, true);
                addHelp("Leave this blank if there is no slide-in stove opening.");
                break;
            default:
                addHelp("This page is not available.");
                break;
        }
    }

'''
    main = main.replace(marker, show_question + marker)
    changed = True

old_inline = '''        Button inlineNext = primaryButton(nextButtonText());
        inlineNext.setOnClickListener(v -> handleNext());
        page.addView(inlineNext);'''
if old_inline in main:
    main = main.replace(old_inline, "        addInlineNavigation();")
    changed = True

if "private void addInlineNavigation()" not in main:
    marker = "    private String nextButtonText() {"
    inline_method = '''    private void addInlineNavigation() {
        LinearLayout inline = new LinearLayout(this);
        inline.setOrientation(LinearLayout.HORIZONTAL);
        inline.setGravity(Gravity.CENTER);
        inline.setPadding(0, dp(6), 0, dp(10));

        Button back = secondaryButton("Back");
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

'''
    main = main.replace(marker, inline_method + marker)
    changed = True

main = main.replace("if (stepIndex >= TOTAL_STEPS - 1)", "if (stepIndex >= pageOrder.size())")
main = main.replace('''        if (stepIndex == 11) {
            return "No, continue";
        }
''', "")
main = main.replace('''        if (stepIndex == 8 && value(lengthIn) <= 0) {
            Toast.makeText(this, "Enter a length greater than zero.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (stepIndex == 9 && value(widthIn) <= 0) {
            Toast.makeText(this, "Enter a width greater than zero.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (stepIndex == 10 && !addCounterSection()) {
            return;
        }
''', "")

main = main.replace(
    "        if (stepIndex == 0 && customerName.getText().toString().trim().isEmpty()) {",
    "        int pageId = currentPageId();\n        if (pageId == PAGE_NAME && customerName.getText().toString().trim().isEmpty()) {"
)
main = main.replace("        if (stepIndex == 5) {", "        if (pageId == PAGE_OFFICE_EMAIL) {")
if "pageId == PAGE_SECTION_LENGTH" not in main:
    main = main.replace(
        "        if (stepIndex >= pageOrder.size()) {\n            sendQuoteEmail();",
        '''        if (pageId == PAGE_SECTION_LENGTH && value(lengthIn) <= 0) {
            Toast.makeText(this, "Enter a length greater than zero.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pageId == PAGE_SECTION_WIDTH && value(widthIn) <= 0) {
            Toast.makeText(this, "Enter a width greater than zero.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pageId == PAGE_SECTION_QUANTITY && !addCounterSection()) {
            return;
        }
        if (stepIndex >= pageOrder.size()) {
            sendQuoteEmail();'''
    )
    changed = True

if "private int currentPageId()" not in main:
    marker = "    private void startQrScan() {"
    reorder_methods = '''    private int currentPageId() {
        if (stepIndex >= 0 && stepIndex < pageOrder.size()) {
            return pageOrder.get(stepIndex);
        }
        return -1;
    }

    private int totalSteps() {
        return pageOrder.size() + 1;
    }

    private void showMovePagesScreen() {
        hideKeyboard();
        page.removeAllViews();
        navigation.removeAllViews();
        addBrandHeader();
        page.addView(questionTitle("Move question pages"));
        addHelp("Use Up and Down to change the order. Review and Send always stay last.");
        addHelp("The measurement pages are still saved in the app code for later.");

        for (int i = 0; i < pageOrder.size(); i++) {
            final int index = i;
            LinearLayout row = itemRow();
            row.setOrientation(LinearLayout.VERTICAL);
            TextView text = label((i + 1) + ". " + pageTitle(pageOrder.get(i)));
            text.setTypeface(Typeface.DEFAULT_BOLD);
            row.addView(text);

            LinearLayout buttons = new LinearLayout(this);
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            Button up = miniButton("UP");
            up.setEnabled(index > 0);
            up.setOnClickListener(v -> movePage(index, index - 1));
            buttons.addView(up, new LinearLayout.LayoutParams(0, dp(44), 1f));

            Button down = miniButton("DOWN");
            down.setEnabled(index < pageOrder.size() - 1);
            down.setOnClickListener(v -> movePage(index, index + 1));
            buttons.addView(down, new LinearLayout.LayoutParams(0, dp(44), 1f));
            row.addView(buttons);
            page.addView(row);
        }

        Button done = primaryButton("Done");
        done.setOnClickListener(v -> {
            stepIndex = Math.min(stepIndex, pageOrder.size());
            showStep();
        });
        page.addView(done);

        Button reset = secondaryButton("Reset page order");
        reset.setOnClickListener(v -> {
            loadDefaultPageOrder();
            savePageOrder();
            stepIndex = 0;
            showMovePagesScreen();
        });
        page.addView(reset);
        scroll.post(() -> scroll.smoothScrollTo(0, 0));
    }

    private void movePage(int from, int to) {
        if (from < 0 || to < 0 || from >= pageOrder.size() || to >= pageOrder.size()) return;
        int pageId = pageOrder.remove(from);
        pageOrder.add(to, pageId);
        savePageOrder();
        if (stepIndex == from) {
            stepIndex = to;
        } else if (from < stepIndex && to >= stepIndex) {
            stepIndex -= 1;
        } else if (from > stepIndex && to <= stepIndex) {
            stepIndex += 1;
        }
        showMovePagesScreen();
    }

'''
    main = main.replace(marker, reorder_methods + marker)
    changed = True

if "private String pageTitle(int pageId)" not in main:
    marker = "    private void saveLists() {"
    page_order_methods = '''    private String pageTitle(int pageId) {
        switch (pageId) {
            case PAGE_NAME: return "Customer name";
            case PAGE_PHONE: return "Customer phone number";
            case PAGE_EMAIL: return "Customer email";
            case PAGE_ADDRESS: return "Project address";
            case PAGE_NOTES: return "Project notes";
            case PAGE_OFFICE_EMAIL: return "RAMSIER'S office email";
            case PAGE_SLABS: return "Slab choices";
            case PAGE_PRICE: return "Installed price";
            case PAGE_SINK_CHARGE: return "Sink or cutout charge";
            case PAGE_EDGE_CHARGE: return "Edge or extra labor charge";
            case PAGE_TEAR_OUT: return "Tear-out charge";
            case PAGE_OTHER_CHARGE: return "Other charges";
            case PAGE_PHOTO: return "Countertop photo";
            case PAGE_SECTION_NAME: return "Countertop section name";
            case PAGE_SECTION_LENGTH: return "Section length";
            case PAGE_SECTION_WIDTH: return "Section width";
            case PAGE_SECTION_QUANTITY: return "Section quantity";
            case PAGE_ADD_SECTION: return "Add another section";
            case PAGE_STOVE_LENGTH: return "Stove opening length";
            case PAGE_STOVE_WIDTH: return "Stove opening width";
            default: return "Unknown page";
        }
    }

    private void loadPageOrder() {
        pageOrder.clear();
        String saved = prefs.getString("page_order", DEFAULT_PAGE_ORDER);
        for (String piece : saved.split(",")) {
            try {
                int pageId = Integer.parseInt(piece.trim());
                if (isValidPageId(pageId) && !pageOrder.contains(pageId)) {
                    pageOrder.add(pageId);
                }
            } catch (Exception ignored) {
            }
        }
        if (pageOrder.isEmpty()) {
            loadDefaultPageOrder();
        }
    }

    private void loadDefaultPageOrder() {
        pageOrder.clear();
        for (String piece : DEFAULT_PAGE_ORDER.split(",")) {
            try {
                pageOrder.add(Integer.parseInt(piece.trim()));
            } catch (Exception ignored) {
            }
        }
    }

    private void savePageOrder() {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < pageOrder.size(); i++) {
            if (i > 0) value.append(',');
            value.append(pageOrder.get(i));
        }
        prefs.edit().putString("page_order", value.toString()).apply();
    }

    private boolean isValidPageId(int pageId) {
        return pageId == PAGE_NAME
                || pageId == PAGE_PHONE
                || pageId == PAGE_EMAIL
                || pageId == PAGE_ADDRESS
                || pageId == PAGE_NOTES
                || pageId == PAGE_OFFICE_EMAIL
                || pageId == PAGE_SLABS
                || pageId == PAGE_PRICE
                || pageId == PAGE_SINK_CHARGE
                || pageId == PAGE_EDGE_CHARGE
                || pageId == PAGE_TEAR_OUT
                || pageId == PAGE_OTHER_CHARGE
                || pageId == PAGE_PHOTO
                || pageId == PAGE_SECTION_NAME
                || pageId == PAGE_SECTION_LENGTH
                || pageId == PAGE_SECTION_WIDTH
                || pageId == PAGE_SECTION_QUANTITY
                || pageId == PAGE_ADD_SECTION
                || pageId == PAGE_STOVE_LENGTH
                || pageId == PAGE_STOVE_WIDTH;
    }

'''
    main = main.replace(marker, page_order_methods + marker)
    changed = True

if changed:
    main_path.write_text(main)
    print("Applied v1.5 movable page order.")
else:
    print("v1.5 movable page order already applied.")
