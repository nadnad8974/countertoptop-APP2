from pathlib import Path
import re
import subprocess
import sys

main_path = Path("app/src/main/java/com/ramsiers/graniteapp/MainActivity.java")
build_path = Path("app/build.gradle")
changed = False

build = build_path.read_text()
new_build = re.sub(r"versionCode\s+\d+", "versionCode 8", build)
new_build = re.sub(r"versionName\s+'[^']+'", "versionName '1.7-test'", new_build)
if new_build != build:
    build_path.write_text(new_build)
    changed = True

main = main_path.read_text()
if "showManagePagesScreen" not in main:
    patch = r'''diff --git a/app/src/main/java/com/ramsiers/graniteapp/MainActivity.java b/app/src/main/java/com/ramsiers/graniteapp/MainActivity.java
index a72baa5..a07e190 100644
--- a/app/src/main/java/com/ramsiers/graniteapp/MainActivity.java
+++ b/app/src/main/java/com/ramsiers/graniteapp/MainActivity.java
@@ -33,6 +33,7 @@ import org.json.JSONObject;
 import java.text.DecimalFormat;
 import java.util.ArrayList;
 import java.util.Collections;
+import java.util.HashMap;
 import java.util.Locale;
 
 public class MainActivity extends Activity {
@@ -40,6 +41,8 @@ public class MainActivity extends Activity {
     private static final String PREFS = "ramsiers_granite_app";
     private static final String MSI_VISUALIZER = "https://www.msisurfaces.com/room-visualizer-tools/";
     private static final String DEFAULT_PAGE_ORDER = "0,1,2,3,4,5,6,7,8,9,10,11,12";
+    private static final String ALL_BUILT_IN_PAGES = "0,1,2,3,4,5,6,7,8,9,10,11,12,100,101,102,103,104,105,106";
+    private static final int CUSTOM_PAGE_START = 1000;
     private static final int PAGE_NAME = 0;
     private static final int PAGE_PHONE = 1;
     private static final int PAGE_EMAIL = 2;
@@ -64,6 +67,8 @@ public class MainActivity extends Activity {
     private final ArrayList<SlabSelection> slabs = new ArrayList<>();
     private final ArrayList<CounterSection> sections = new ArrayList<>();
     private final ArrayList<Integer> pageOrder = new ArrayList<>();
+    private final ArrayList<CustomPage> customPages = new ArrayList<>();
+    private final HashMap<Integer, EditText> customInputs = new HashMap<>();
     private final DecimalFormat number = new DecimalFormat("0.00");
 
     private SharedPreferences prefs;
@@ -103,6 +108,7 @@ public class MainActivity extends Activity {
         super.onCreate(savedInstanceState);
         prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
         loadSavedLists();
+        loadCustomPages();
         loadPageOrder();
         buildUi();
     }
@@ -186,8 +192,8 @@ public class MainActivity extends Activity {
         progress.setTextColor(Color.GRAY);
         page.addView(progress);
 
-        Button movePages = secondaryButton("Move pages");
-        movePages.setOnClickListener(v -> showMovePagesScreen());
+        Button movePages = secondaryButton("Manage pages");
+        movePages.setOnClickListener(v -> showManagePagesScreen());
         page.addView(movePages);
 
         if (stepIndex >= pageOrder.size()) {
@@ -202,71 +208,76 @@ public class MainActivity extends Activity {
     private void showQuestionPage(int pageId) {
         switch (pageId) {
             case PAGE_NAME:
-                addQuestion("What is the customer's name?", customerName, true);
+                addQuestion(questionText(pageId, "What is the customer's name?"), customerName, true);
                 break;
             case PAGE_PHONE:
-                addQuestion("What is the customer's phone number?", customerPhone, true);
+                addQuestion(questionText(pageId, "What is the customer's phone number?"), customerPhone, true);
                 break;
             case PAGE_EMAIL:
-                addQuestion("What is the customer's email address?", customerEmail, true);
+                addQuestion(questionText(pageId, "What is the customer's email address?"), customerEmail, true);
                 break;
             case PAGE_ADDRESS:
-                addQuestion("What is the project address?", projectAddress, true);
+                addQuestion(questionText(pageId, "What is the project address?"), projectAddress, true);
                 break;
             case PAGE_NOTES:
-                addQuestion("Are there any project notes?", projectNotes, true);
+                addQuestion(questionText(pageId, "Are there any project notes?"), projectNotes, true);
                 break;
             case PAGE_OFFICE_EMAIL:
-                addQuestion("What email should receive the quote request?", officeEmail, true);
+                addQuestion(questionText(pageId, "What email should receive the quote request?"), officeEmail, true);
                 addHelp("This RAMSIER'S office email is saved on this phone.");
                 break;
             case PAGE_SLABS:
                 addSlabStep();
                 break;
             case PAGE_PRICE:
-                addQuestion("What is the installed price per square foot?", pricePerSqFt, true);
+                addQuestion(questionText(pageId, "What is the installed price per square foot?"), pricePerSqFt, true);
                 break;
             case PAGE_SINK_CHARGE:
-                addQuestion("What is the sink or cutout charge?", sinkCharge, true);
+                addQuestion(questionText(pageId, "What is the sink or cutout charge?"), sinkCharge, true);
                 break;
             case PAGE_EDGE_CHARGE:
-                addQuestion("What is the edge or extra labor charge?", edgeCharge, true);
+                addQuestion(questionText(pageId, "What is the edge or extra labor charge?"), edgeCharge, true);
                 break;
             case PAGE_TEAR_OUT:
-                addQuestion("What is the tear-out charge?", tearOutCharge, true);
+                addQuestion(questionText(pageId, "What is the tear-out charge?"), tearOutCharge, true);
                 break;
             case PAGE_OTHER_CHARGE:
-                addQuestion("Are there any other charges?", otherCharge, true);
+                addQuestion(questionText(pageId, "Are there any other charges?"), otherCharge, true);
                 break;
             case PAGE_PHOTO:
                 addPhotoStep();
                 break;
             case PAGE_SECTION_NAME:
-                addQuestion("What should this countertop section be called?", sectionName, true);
+                addQuestion(questionText(pageId, "What should this countertop section be called?"), sectionName, true);
                 break;
             case PAGE_SECTION_LENGTH:
-                addQuestion("What is the section length in inches?", lengthIn, true);
+                addQuestion(questionText(pageId, "What is the section length in inches?"), lengthIn, true);
                 break;
             case PAGE_SECTION_WIDTH:
-                addQuestion("What is the section width in inches?", widthIn, true);
+                addQuestion(questionText(pageId, "What is the section width in inches?"), widthIn, true);
                 break;
             case PAGE_SECTION_QUANTITY:
-                addQuestion("How many identical sections are there?", quantity, true);
+                addQuestion(questionText(pageId, "How many identical sections are there?"), quantity, true);
                 addHelp("Tapping Next saves this countertop section.");
                 break;
             case PAGE_ADD_SECTION:
                 addAnotherSectionStep();
                 break;
             case PAGE_STOVE_LENGTH:
-                addQuestion("What is the slide-in stove opening length?", stoveLength, true);
+                addQuestion(questionText(pageId, "What is the slide-in stove opening length?"), stoveLength, true);
                 addHelp("Leave this blank if there is no slide-in stove opening.");
                 break;
             case PAGE_STOVE_WIDTH:
-                addQuestion("What is the slide-in stove opening width?", stoveWidth, true);
+                addQuestion(questionText(pageId, "What is the slide-in stove opening width?"), stoveWidth, true);
                 addHelp("Leave this blank if there is no slide-in stove opening.");
                 break;
             default:
-                addHelp("This page is not available.");
+                CustomPage customPage = customPageById(pageId);
+                if (customPage != null) {
+                    addQuestion(customPage.question, inputForCustomPage(customPage), true);
+                } else {
+                    addHelp("This page is not available.");
+                }
                 break;
         }
     }
@@ -306,7 +317,7 @@ public class MainActivity extends Activity {
 
     private void addSlabStep() {
         hideKeyboard();
-        page.addView(questionTitle("Which slabs does the customer like?"));
+        page.addView(questionTitle(questionForEdit(PAGE_SLABS)));
         addHelp("Scan MSI slab QR codes or add a slab color manually. Add as many as needed, then tap Next.");
 
         Button scanButton = primaryButton("Scan slab QR code");
@@ -324,7 +335,7 @@ public class MainActivity extends Activity {
 
     private void addAnotherSectionStep() {
         hideKeyboard();
-        page.addView(questionTitle("Would you like to add another countertop section?"));
+        page.addView(questionTitle(questionForEdit(PAGE_ADD_SECTION)));
         addHelp("The section you just entered has been saved.");
 
         detach(sectionList);
@@ -347,7 +358,7 @@ public class MainActivity extends Activity {
     private void addPhotoStep() {
         hideKeyboard();
         page.clearFocus();
-        page.addView(questionTitle("Would you like to add a countertop photo?"));
+        page.addView(questionTitle(questionForEdit(PAGE_PHOTO)));
         addHelp("The photo screen is separate from all typing screens, so the keyboard will not cover it.");
 
         Button photoButton = primaryButton("Choose kitchen or countertop photo");
@@ -476,6 +487,12 @@ public class MainActivity extends Activity {
         if (pageId == PAGE_SECTION_QUANTITY && !addCounterSection()) {
             return;
         }
+        if (pageId >= CUSTOM_PAGE_START) {
+            EditText field = customInputs.get(pageId);
+            if (field != null) {
+                prefs.edit().putString("custom_answer_" + pageId, field.getText().toString()).apply();
+            }
+        }
         if (stepIndex >= pageOrder.size()) {
             sendQuoteEmail();
             return;
@@ -497,20 +514,24 @@ public class MainActivity extends Activity {
         return pageOrder.size() + 1;
     }
 
-    private void showMovePagesScreen() {
+    private void showManagePagesScreen() {
         hideKeyboard();
         page.removeAllViews();
         navigation.removeAllViews();
         addBrandHeader();
-        page.addView(questionTitle("Move question pages"));
-        addHelp("Use Up and Down to change the order. Review and Send always stay last.");
-        addHelp("The measurement pages are still saved in the app code for later.");
+        page.addView(questionTitle("Manage question pages"));
+        addHelp("Use Up and Down to move pages. Remove hides a page. Add Page can bring hidden pages back.");
+
+        Button addPage = primaryButton("Add page");
+        addPage.setOnClickListener(v -> showAddPageDialog());
+        page.addView(addPage);
 
         for (int i = 0; i < pageOrder.size(); i++) {
             final int index = i;
+            final int pageId = pageOrder.get(i);
             LinearLayout row = itemRow();
             row.setOrientation(LinearLayout.VERTICAL);
-            TextView text = label((i + 1) + ". " + pageTitle(pageOrder.get(i)));
+            TextView text = label((i + 1) + ". " + pageDisplayTitle(pageId));
             text.setTypeface(Typeface.DEFAULT_BOLD);
             row.addView(text);
 
@@ -525,6 +546,12 @@ public class MainActivity extends Activity {
             down.setEnabled(index < pageOrder.size() - 1);
             down.setOnClickListener(v -> movePage(index, index + 1));
             buttons.addView(down, new LinearLayout.LayoutParams(0, dp(44), 1f));
+            Button edit = miniButton("EDIT");
+            edit.setOnClickListener(v -> showEditPageDialog(pageId));
+            buttons.addView(edit, new LinearLayout.LayoutParams(0, dp(44), 1f));
+            Button remove = miniButton("REMOVE");
+            remove.setOnClickListener(v -> removePage(index));
+            buttons.addView(remove, new LinearLayout.LayoutParams(0, dp(44), 1f));
             row.addView(buttons);
             page.addView(row);
         }
@@ -541,7 +568,7 @@ public class MainActivity extends Activity {
             loadDefaultPageOrder();
             savePageOrder();
             stepIndex = 0;
-            showMovePagesScreen();
+            showManagePagesScreen();
         });
         page.addView(reset);
         scroll.post(() -> scroll.smoothScrollTo(0, 0));
@@ -559,7 +586,128 @@ public class MainActivity extends Activity {
         } else if (from > stepIndex && to <= stepIndex) {
             stepIndex += 1;
         }
-        showMovePagesScreen();
+        showManagePagesScreen();
+    }
+
+    private void removePage(int index) {
+        if (index < 0 || index >= pageOrder.size()) return;
+        pageOrder.remove(index);
+        if (stepIndex >= pageOrder.size()) {
+            stepIndex = Math.max(0, pageOrder.size() - 1);
+        }
+        savePageOrder();
+        showManagePagesScreen();
+    }
+
+    private void showAddPageDialog() {
+        ArrayList<Integer> availableIds = new ArrayList<>();
+        ArrayList<String> labels = new ArrayList<>();
+        for (int pageId : allBuiltInPageIds()) {
+            if (!pageOrder.contains(pageId)) {
+                availableIds.add(pageId);
+                labels.add("Add: " + pageDisplayTitle(pageId));
+            }
+        }
+        for (CustomPage customPage : customPages) {
+            if (!pageOrder.contains(customPage.id)) {
+                availableIds.add(customPage.id);
+                labels.add("Add: " + customPage.title);
+            }
+        }
+        labels.add("Create new custom question");
+        String[] items = labels.toArray(new String[0]);
+        new AlertDialog.Builder(this)
+                .setTitle("Add page")
+                .setItems(items, (dialog, which) -> {
+                    if (which < availableIds.size()) {
+                        int pageId = availableIds.get(which);
+                        pageOrder.add(pageId);
+                        savePageOrder();
+                        showManagePagesScreen();
+                    } else {
+                        showCustomPageDialog();
+                    }
+                })
+                .setNegativeButton("Cancel", null)
+                .show();
+    }
+
+    private void showEditPageDialog(int pageId) {
+        LinearLayout form = new LinearLayout(this);
+        form.setOrientation(LinearLayout.VERTICAL);
+        form.setPadding(dp(12), dp(8), dp(12), 0);
+
+        EditText title = input("Short page name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
+        title.setText(pageDisplayTitle(pageId));
+        form.addView(title);
+
+        EditText question = input("Question wording", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
+        question.setMinLines(3);
+        question.setGravity(Gravity.TOP);
+        question.setText(questionForEdit(pageId));
+        form.addView(question);
+
+        new AlertDialog.Builder(this)
+                .setTitle("Edit page")
+                .setView(form)
+                .setPositiveButton("Save", (dialog, which) -> {
+                    String newTitle = title.getText().toString().trim();
+                    String newQuestion = question.getText().toString().trim();
+                    if (newTitle.isEmpty() || newQuestion.isEmpty()) {
+                        Toast.makeText(this, "Enter a page name and question.", Toast.LENGTH_LONG).show();
+                        return;
+                    }
+                    if (pageId >= CUSTOM_PAGE_START) {
+                        CustomPage customPage = customPageById(pageId);
+                        if (customPage != null) {
+                            customPage.title = newTitle;
+                            customPage.question = newQuestion;
+                            saveCustomPages();
+                        }
+                    } else {
+                        prefs.edit()
+                                .putString("page_title_" + pageId, newTitle)
+                                .putString("page_question_" + pageId, newQuestion)
+                                .apply();
+                    }
+                    showManagePagesScreen();
+                })
+                .setNegativeButton("Cancel", null)
+                .show();
+    }
+
+    private void showCustomPageDialog() {
+        LinearLayout form = new LinearLayout(this);
+        form.setOrientation(LinearLayout.VERTICAL);
+        form.setPadding(dp(12), dp(8), dp(12), 0);
+
+        EditText title = input("Short page name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
+        form.addView(title);
+
+        EditText question = input("Question wording", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
+        question.setMinLines(3);
+        question.setGravity(Gravity.TOP);
+        form.addView(question);
+
+        new AlertDialog.Builder(this)
+                .setTitle("Create custom page")
+                .setView(form)
+                .setPositiveButton("Add", (dialog, which) -> {
+                    String newTitle = title.getText().toString().trim();
+                    String newQuestion = question.getText().toString().trim();
+                    if (newTitle.isEmpty() || newQuestion.isEmpty()) {
+                        Toast.makeText(this, "Enter a page name and question.", Toast.LENGTH_LONG).show();
+                        return;
+                    }
+                    int pageId = nextCustomPageId();
+                    customPages.add(new CustomPage(pageId, newTitle, newQuestion));
+                    pageOrder.add(pageId);
+                    saveCustomPages();
+                    savePageOrder();
+                    showManagePagesScreen();
+                })
+                .setNegativeButton("Cancel", null)
+                .show();
     }
 
     private void startQrScan() {
@@ -817,6 +965,19 @@ public class MainActivity extends Activity {
         body.append("Net square feet: ").append(number.format(estimate.net)).append("\n");
         body.append("Estimated total: $").append(number.format(estimate.total)).append("\n");
         body.append("This is an estimate and needs final verification by RAMSIER'S.\n\n");
+        if (!customPages.isEmpty()) {
+            body.append("CUSTOM QUESTIONS\n");
+            for (CustomPage customPage : customPages) {
+                if (pageOrder.contains(customPage.id)) {
+                    EditText field = customInputs.get(customPage.id);
+                    String answer = field == null
+                            ? prefs.getString("custom_answer_" + customPage.id, "")
+                            : field.getText().toString();
+                    body.append(customPage.question).append("\n");
+                    body.append(answer.trim().isEmpty() ? "Not provided" : answer.trim()).append("\n\n");
+                }
+            }
+        }
         body.append("PROJECT NOTES\n").append(text(projectNotes)).append("\n");
 
         Intent email = new Intent(Intent.ACTION_SEND);
@@ -943,6 +1104,92 @@ public class MainActivity extends Activity {
         }
     }
 
+    private String pageDisplayTitle(int pageId) {
+        if (pageId >= CUSTOM_PAGE_START) {
+            CustomPage customPage = customPageById(pageId);
+            return customPage == null ? "Custom question" : customPage.title;
+        }
+        return prefs.getString("page_title_" + pageId, pageTitle(pageId));
+    }
+
+    private String questionText(int pageId, String defaultQuestion) {
+        return prefs.getString("page_question_" + pageId, defaultQuestion);
+    }
+
+    private String questionForEdit(int pageId) {
+        if (pageId >= CUSTOM_PAGE_START) {
+            CustomPage customPage = customPageById(pageId);
+            return customPage == null ? "" : customPage.question;
+        }
+        switch (pageId) {
+            case PAGE_NAME: return questionText(pageId, "What is the customer's name?");
+            case PAGE_PHONE: return questionText(pageId, "What is the customer's phone number?");
+            case PAGE_EMAIL: return questionText(pageId, "What is the customer's email address?");
+            case PAGE_ADDRESS: return questionText(pageId, "What is the project address?");
+            case PAGE_NOTES: return questionText(pageId, "Are there any project notes?");
+            case PAGE_OFFICE_EMAIL: return questionText(pageId, "What email should receive the quote request?");
+            case PAGE_PRICE: return questionText(pageId, "What is the installed price per square foot?");
+            case PAGE_SINK_CHARGE: return questionText(pageId, "What is the sink or cutout charge?");
+            case PAGE_EDGE_CHARGE: return questionText(pageId, "What is the edge or extra labor charge?");
+            case PAGE_TEAR_OUT: return questionText(pageId, "What is the tear-out charge?");
+            case PAGE_OTHER_CHARGE: return questionText(pageId, "Are there any other charges?");
+            case PAGE_SECTION_NAME: return questionText(pageId, "What should this countertop section be called?");
+            case PAGE_SECTION_LENGTH: return questionText(pageId, "What is the section length in inches?");
+            case PAGE_SECTION_WIDTH: return questionText(pageId, "What is the section width in inches?");
+            case PAGE_SECTION_QUANTITY: return questionText(pageId, "How many identical sections are there?");
+            case PAGE_STOVE_LENGTH: return questionText(pageId, "What is the slide-in stove opening length?");
+            case PAGE_STOVE_WIDTH: return questionText(pageId, "What is the slide-in stove opening width?");
+            case PAGE_SLABS: return "Which slabs does the customer like?";
+            case PAGE_PHOTO: return "Would you like to add a countertop photo?";
+            case PAGE_ADD_SECTION: return "Would you like to add another countertop section?";
+            default: return pageTitle(pageId);
+        }
+    }
+
+    private EditText inputForCustomPage(CustomPage customPage) {
+        EditText field = customInputs.get(customPage.id);
+        if (field == null) {
+            field = input("Type answer", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
+            field.setMinLines(3);
+            field.setGravity(Gravity.TOP);
+            field.setText(prefs.getString("custom_answer_" + customPage.id, ""));
+            customInputs.put(customPage.id, field);
+        }
+        return field;
+    }
+
+    private CustomPage customPageById(int pageId) {
+        for (CustomPage customPage : customPages) {
+            if (customPage.id == pageId) {
+                return customPage;
+            }
+        }
+        return null;
+    }
+
+    private ArrayList<Integer> allBuiltInPageIds() {
+        ArrayList<Integer> ids = new ArrayList<>();
+        for (String piece : ALL_BUILT_IN_PAGES.split(",")) {
+            try {
+                int pageId = Integer.parseInt(piece.trim());
+                if (isValidBuiltInPageId(pageId) && !ids.contains(pageId)) {
+                    ids.add(pageId);
+                }
+            } catch (Exception ignored) {
+            }
+        }
+        return ids;
+    }
+
+    private int nextCustomPageId() {
+        int pageId = prefs.getInt("next_custom_page_id", CUSTOM_PAGE_START);
+        while (customPageById(pageId) != null || pageOrder.contains(pageId)) {
+            pageId++;
+        }
+        prefs.edit().putInt("next_custom_page_id", pageId + 1).apply();
+        return pageId;
+    }
+
     private void loadPageOrder() {
         pageOrder.clear();
         String saved = prefs.getString("page_order", DEFAULT_PAGE_ORDER);
@@ -979,7 +1226,45 @@ public class MainActivity extends Activity {
         prefs.edit().putString("page_order", value.toString()).apply();
     }
 
+    private void loadCustomPages() {
+        customPages.clear();
+        customInputs.clear();
+        try {
+            JSONArray pageArray = new JSONArray(prefs.getString("custom_pages", "[]"));
+            for (int i = 0; i < pageArray.length(); i++) {
+                JSONObject object = pageArray.getJSONObject(i);
+                int id = object.optInt("id", -1);
+                String title = object.optString("title");
+                String question = object.optString("question");
+                if (id >= CUSTOM_PAGE_START && !title.trim().isEmpty() && !question.trim().isEmpty()) {
+                    customPages.add(new CustomPage(id, title, question));
+                }
+            }
+        } catch (Exception ignored) {
+            customPages.clear();
+        }
+    }
+
+    private void saveCustomPages() {
+        try {
+            JSONArray pageArray = new JSONArray();
+            for (CustomPage customPage : customPages) {
+                JSONObject object = new JSONObject();
+                object.put("id", customPage.id);
+                object.put("title", customPage.title);
+                object.put("question", customPage.question);
+                pageArray.put(object);
+            }
+            prefs.edit().putString("custom_pages", pageArray.toString()).apply();
+        } catch (Exception ignored) {
+        }
+    }
+
     private boolean isValidPageId(int pageId) {
+        return isValidBuiltInPageId(pageId) || customPageById(pageId) != null;
+    }
+
+    private boolean isValidBuiltInPageId(int pageId) {
         return pageId == PAGE_NAME
                 || pageId == PAGE_PHONE
                 || pageId == PAGE_EMAIL
@@ -1224,6 +1509,18 @@ public class MainActivity extends Activity {
         }
     }
 
+    private static class CustomPage {
+        final int id;
+        String title;
+        String question;
+
+        CustomPage(int id, String title, String question) {
+            this.id = id;
+            this.title = title == null || title.trim().isEmpty() ? "Custom question" : title;
+            this.question = question == null || question.trim().isEmpty() ? this.title : question;
+        }
+    }
+
     private static class Estimate {
         final double gross;
         final double stove;
'''
    result = subprocess.run(["git", "apply", "--whitespace=nowarn"], input=patch, text=True, capture_output=True)
    if result.returncode != 0:
        print(result.stdout)
        print(result.stderr, file=sys.stderr)
        sys.exit(result.returncode)
    changed = True
    print("Applied v1.7 editable page manager.")
else:
    print("v1.7 editable page manager already applied.")

if changed:
    print("v1.7 page editor source prepared.")
