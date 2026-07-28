from pathlib import Path
import subprocess
import sys

if "Question " in Path("app/src/main/java/com/ramsiers/graniteapp/MainActivity.java").read_text():
    print("Wizard patch already applied.")
    sys.exit(0)

patch = r'''diff --git a/app/build.gradle b/app/build.gradle
index f0cea48..a2cca93 100644
--- a/app/build.gradle
+++ b/app/build.gradle
@@ -10,8 +10,8 @@ android {
         applicationId 'com.ramsiers.graniteapp'
         minSdk 23
         targetSdk 35
-        versionCode 2
-        versionName '1.1-test'
+        versionCode 3
+        versionName '1.2-test'
     }
 
     compileOptions {
diff --git a/app/src/main/AndroidManifest.xml b/app/src/main/AndroidManifest.xml
index 7d93c81..2c5b377 100644
--- a/app/src/main/AndroidManifest.xml
+++ b/app/src/main/AndroidManifest.xml
@@ -19,7 +19,8 @@
         <activity
             android:name=".MainActivity"
             android:exported="true"
-            android:screenOrientation="portrait">
+            android:screenOrientation="portrait"
+            android:windowSoftInputMode="adjustResize">
             <intent-filter>
                 <action android:name="android.intent.action.MAIN" />
                 <category android:name="android.intent.category.LAUNCHER" />
diff --git a/app/src/main/java/com/ramsiers/graniteapp/MainActivity.java b/app/src/main/java/com/ramsiers/graniteapp/MainActivity.java
index 50b824f..ce9cc80 100644
--- a/app/src/main/java/com/ramsiers/graniteapp/MainActivity.java
+++ b/app/src/main/java/com/ramsiers/graniteapp/MainActivity.java
@@ -43,6 +43,8 @@ public class MainActivity extends Activity {
     private final DecimalFormat number = new DecimalFormat("0.00");
 
     private SharedPreferences prefs;
+    private ScrollView scroll;
+    private LinearLayout root;
     private LinearLayout slabList;
     private LinearLayout sectionList;
     private EditText customerName;
@@ -67,6 +69,7 @@ public class MainActivity extends Activity {
     private TextView photoStatus;
     private ImageView roomPhoto;
     private Uri selectedPhotoUri;
+    private int stepIndex = 0;
 
     @Override
     protected void onCreate(Bundle savedInstanceState) {
@@ -77,29 +80,13 @@ public class MainActivity extends Activity {
     }
 
     private void buildUi() {
-        ScrollView scroll = new ScrollView(this);
+        scroll = new ScrollView(this);
         scroll.setFillViewport(true);
-        LinearLayout root = new LinearLayout(this);
+        root = new LinearLayout(this);
         root.setOrientation(LinearLayout.VERTICAL);
-        root.setPadding(dp(16), dp(12), dp(16), dp(40));
+        root.setPadding(dp(16), dp(12), dp(16), dp(24));
         root.setBackgroundColor(Color.rgb(248, 246, 243));
         scroll.addView(root);
-
-        TextView title = new TextView(this);
-        title.setText("RAMSIER'S\nGRANITE AND QUARTZ");
-        title.setTextSize(27);
-        title.setTypeface(Typeface.DEFAULT_BOLD);
-        title.setTextColor(Color.rgb(91, 58, 41));
-        title.setGravity(Gravity.CENTER);
-        title.setPadding(0, dp(10), 0, dp(5));
-        root.addView(title);
-
-        TextView subtitle = label("Scan slabs • Measure countertops • Request pricing");
-        subtitle.setGravity(Gravity.CENTER);
-        subtitle.setTextColor(Color.DKGRAY);
-        root.addView(subtitle);
-
-        root.addView(sectionHeader("1. Customer information"));
         customerName = input("Customer name", InputType.TYPE_CLASS_TEXT);
         customerPhone = input("Phone number", InputType.TYPE_CLASS_PHONE);
         customerEmail = input("Customer email", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
@@ -108,123 +95,230 @@ public class MainActivity extends Activity {
         projectNotes.setMinLines(3);
         officeEmail = input("RAMSIER'S office email (saved on this phone)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
         officeEmail.setText(prefs.getString("office_email", ""));
-        root.addView(customerName);
-        root.addView(customerPhone);
-        root.addView(customerEmail);
-        root.addView(projectAddress);
-        root.addView(projectNotes);
-        root.addView(officeEmail);
-
-        root.addView(sectionHeader("2. Scan and save slab choices"));
-        TextView scanHelp = label("At MSI, scan every slab QR code the customer likes. The original QR information is saved even if it is not an MSI web link.");
-        scanHelp.setTextSize(14);
-        root.addView(scanHelp);
-
-        Button scanButton = primaryButton("SCAN SLAB QR CODE");
-        scanButton.setOnClickListener(v -> startQrScan());
-        root.addView(scanButton);
-
-        Button manualButton = secondaryButton("ADD A SLAB MANUALLY");
-        manualButton.setOnClickListener(v -> showManualSlabDialog());
-        root.addView(manualButton);
-
         slabList = new LinearLayout(this);
         slabList.setOrientation(LinearLayout.VERTICAL);
-        root.addView(slabList);
-        renderSlabs();
-
-        root.addView(sectionHeader("3. Countertop square footage"));
-        TextView measurementHelp = label("Enter each countertop piece separately. Sink openings stay included. Enter a slide-in stove opening below only when it should be subtracted.");
-        measurementHelp.setTextSize(14);
-        root.addView(measurementHelp);
-
         sectionName = input("Section name, such as Island", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
         lengthIn = input("Length in inches", decimalInput());
         widthIn = input("Width in inches", decimalInput());
         quantity = input("Quantity", decimalInput());
         quantity.setText("1");
-        root.addView(sectionName);
-        root.addView(lengthIn);
-        root.addView(widthIn);
-        root.addView(quantity);
-
-        Button addSection = primaryButton("ADD COUNTERTOP SECTION");
-        addSection.setOnClickListener(v -> addCounterSection());
-        root.addView(addSection);
-
         sectionList = new LinearLayout(this);
         sectionList.setOrientation(LinearLayout.VERTICAL);
-        root.addView(sectionList);
-        renderSections();
-
-        TextView stoveLabel = label("Optional slide-in stove opening to subtract:");
-        stoveLabel.setTypeface(Typeface.DEFAULT_BOLD);
-        root.addView(stoveLabel);
         stoveLength = input("Stove opening length in inches", decimalInput());
         stoveWidth = input("Stove opening width in inches", decimalInput());
-        root.addView(stoveLength);
-        root.addView(stoveWidth);
-
-        root.addView(sectionHeader("4. Estimated price"));
         pricePerSqFt = input("Installed price per square foot", decimalInput());
         sinkCharge = input("Sink / cutout charge", decimalInput());
         edgeCharge = input("Edge or extra labor charge", decimalInput());
         tearOutCharge = input("Tear-out charge", decimalInput());
         otherCharge = input("Other charges", decimalInput());
-        root.addView(pricePerSqFt);
-        root.addView(sinkCharge);
-        root.addView(edgeCharge);
-        root.addView(tearOutCharge);
-        root.addView(otherCharge);
-
-        Button calculate = primaryButton("CALCULATE ESTIMATE");
-        calculate.setOnClickListener(v -> calculateAndDisplay(true));
-        root.addView(calculate);
-
         squareFootResult = resultLabel("Net square footage: 0.00");
         totalResult = resultLabel("Estimated total: $0.00");
-        root.addView(squareFootResult);
-        root.addView(totalResult);
-
-        root.addView(sectionHeader("5. Countertop photo and visualizer"));
-        Button photoButton = secondaryButton("UPLOAD KITCHEN / COUNTERTOP PHOTO");
-        photoButton.setOnClickListener(v -> openPhotoPicker());
-        root.addView(photoButton);
-
         photoStatus = label("No photo selected.");
         photoStatus.setTextSize(14);
-        root.addView(photoStatus);
-
         roomPhoto = new ImageView(this);
         roomPhoto.setAdjustViewBounds(true);
         roomPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
+        setContentView(scroll);
+        showStep();
+    }
+
+    private void showStep() {
+        root.removeAllViews();
+        TextView title = new TextView(this);
+        title.setText("RAMSIER'S\nGRANITE AND QUARTZ");
+        title.setTextSize(27);
+        title.setTypeface(Typeface.DEFAULT_BOLD);
+        title.setTextColor(Color.rgb(91, 58, 41));
+        title.setGravity(Gravity.CENTER);
+        title.setPadding(0, dp(10), 0, dp(2));
+        root.addView(title);
+
+        TextView progress = label("Question " + (stepIndex + 1) + " of 16");
+        progress.setGravity(Gravity.CENTER);
+        progress.setTextColor(Color.GRAY);
+        root.addView(progress);
+
+        switch (stepIndex) {
+            case 0:
+                addQuestion("Customer name", customerName);
+                break;
+            case 1:
+                addQuestion("Phone number", customerPhone);
+                break;
+            case 2:
+                addQuestion("Customer email", customerEmail);
+                break;
+            case 3:
+                addQuestion("Project address", projectAddress);
+                break;
+            case 4:
+                addQuestion("Project notes", projectNotes);
+                break;
+            case 5:
+                addQuestion("RAMSIER'S office email", officeEmail);
+                break;
+            case 6:
+                addSlabStep();
+                break;
+            case 7:
+                addQuestion("Countertop section name", sectionName);
+                break;
+            case 8:
+                addQuestion("Length in inches", lengthIn);
+                break;
+            case 9:
+                addQuestion("Width in inches", widthIn);
+                break;
+            case 10:
+                addQuestion("Quantity", quantity);
+                TextView saveHelp = label("Tap OK to save this countertop section.");
+                saveHelp.setGravity(Gravity.CENTER);
+                root.addView(saveHelp);
+                break;
+            case 11:
+                addTwoFieldQuestion("Slide-in stove opening to subtract", stoveLength, stoveWidth, "Leave both blank if there is no stove opening.");
+                break;
+            case 12:
+                addQuestion("Installed price per square foot", pricePerSqFt);
+                break;
+            case 13:
+                addChargeStep();
+                break;
+            case 14:
+                addPhotoStep();
+                break;
+            default:
+                addReviewStep();
+                break;
+        }
+        addNavigation();
+        scroll.post(() -> scroll.smoothScrollTo(0, 0));
+    }
+
+    private void addQuestion(String title, EditText input) {
+        root.addView(sectionHeader(title));
+        addField(input);
+        input.requestFocus();
+    }
+
+    private void addTwoFieldQuestion(String title, EditText first, EditText second, String helpText) {
+        root.addView(sectionHeader(title));
+        TextView help = label(helpText);
+        help.setTextSize(14);
+        root.addView(help);
+        addField(first);
+        addField(second);
+        first.requestFocus();
+    }
+
+    private void addSlabStep() {
+        root.addView(sectionHeader("Slab choices"));
+        TextView scanHelp = label("Scan MSI slab QR codes or type a slab color. MSI links will open from the saved list.");
+        scanHelp.setTextSize(14);
+        root.addView(scanHelp);
+
+        Button scanButton = primaryButton("Scan slab QR code");
+        scanButton.setOnClickListener(v -> startQrScan());
+        root.addView(scanButton);
+
+        Button manualButton = secondaryButton("Add slab manually");
+        manualButton.setOnClickListener(v -> showManualSlabDialog());
+        root.addView(manualButton);
+
+        detach(slabList);
+        root.addView(slabList);
+        renderSlabs();
+    }
+
+    private void addChargeStep() {
+        root.addView(sectionHeader("Extra charges"));
+        addField(sinkCharge);
+        addField(edgeCharge);
+        addField(tearOutCharge);
+        addField(otherCharge);
+        sinkCharge.requestFocus();
+    }
+
+    private void addPhotoStep() {
+        root.addView(sectionHeader("Countertop photo"));
+        Button photoButton = secondaryButton("Upload kitchen / countertop photo");
+        photoButton.setOnClickListener(v -> openPhotoPicker());
+        root.addView(photoButton);
+        detach(photoStatus);
+        root.addView(photoStatus);
+        detach(roomPhoto);
         root.addView(roomPhoto, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));
 
-        Button visualizer = secondaryButton("OPEN MSI ROOM VISUALIZER");
+        Button visualizer = secondaryButton("Open MSI room visualizer");
         visualizer.setOnClickListener(v -> openWebPage(MSI_VISUALIZER));
         root.addView(visualizer);
+    }
 
-        TextView visualizerNote = label("This first test version opens MSI's visualizer. A built-in countertop masking and stone overlay tool can be added after the scan-and-quote workflow is tested.");
-        visualizerNote.setTextSize(14);
-        root.addView(visualizerNote);
-
-        root.addView(sectionHeader("6. Send the quote request"));
-        Button send = primaryButton("SEND EVERYTHING BY EMAIL");
-        send.setOnClickListener(v -> sendQuoteEmail());
-        root.addView(send);
+    private void addReviewStep() {
+        calculateAndDisplay(false);
+        root.addView(sectionHeader("Review and send"));
+        detach(sectionList);
+        root.addView(sectionList);
+        renderSections();
+        detach(slabList);
+        root.addView(slabList);
+        renderSlabs();
+        detach(squareFootResult);
+        root.addView(squareFootResult);
+        detach(totalResult);
+        root.addView(totalResult);
 
-        Button reset = secondaryButton("START A NEW CUSTOMER");
+        Button reset = secondaryButton("Start a new customer");
         reset.setOnClickListener(v -> confirmReset());
         root.addView(reset);
+    }
 
-        TextView version = label("Test version 1.1 • Android MVP");
-        version.setGravity(Gravity.CENTER);
-        version.setTextSize(12);
-        version.setTextColor(Color.GRAY);
-        version.setPadding(0, dp(18), 0, 0);
-        root.addView(version);
+    private void addNavigation() {
+        LinearLayout nav = new LinearLayout(this);
+        nav.setOrientation(LinearLayout.HORIZONTAL);
+        nav.setGravity(Gravity.CENTER);
+        nav.setPadding(0, dp(16), 0, 0);
 
-        setContentView(scroll);
+        Button back = secondaryButton("Back");
+        back.setEnabled(stepIndex > 0);
+        back.setOnClickListener(v -> {
+            stepIndex = Math.max(0, stepIndex - 1);
+            showStep();
+        });
+        nav.addView(back, new LinearLayout.LayoutParams(0, dp(52), 1f));
+
+        Button next = primaryButton(stepIndex >= 15 ? "Send email" : "OK");
+        next.setOnClickListener(v -> handleNext());
+        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(0, dp(52), 1f);
+        nextParams.setMargins(dp(8), 0, 0, 0);
+        nav.addView(next, nextParams);
+        root.addView(nav);
+    }
+
+    private void handleNext() {
+        if (stepIndex == 5) {
+            prefs.edit().putString("office_email", officeEmail.getText().toString().trim()).apply();
+        }
+        if (stepIndex == 10 && !addCounterSection()) {
+            return;
+        }
+        if (stepIndex >= 15) {
+            sendQuoteEmail();
+            return;
+        }
+        stepIndex += 1;
+        showStep();
+    }
+
+    private void addField(View view) {
+        detach(view);
+        root.addView(view);
+    }
+
+    private void detach(View view) {
+        if (view.getParent() instanceof ViewGroup) {
+            ((ViewGroup) view.getParent()).removeView(view);
+        }
     }
 
     private void startQrScan() {
@@ -310,32 +404,39 @@ public class MainActivity extends Activity {
             final int index = i;
             SlabSelection slab = slabs.get(i);
             LinearLayout row = itemRow();
+            row.setOrientation(LinearLayout.VERTICAL);
             TextView text = label((i + 1) + ". " + slab.name + "\n" + slab.raw);
             text.setTextSize(14);
-            row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
+            row.addView(text, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
+            LinearLayout buttons = new LinearLayout(this);
+            buttons.setOrientation(LinearLayout.HORIZONTAL);
             if (isWebUrl(slab.raw)) {
                 Button open = miniButton("OPEN");
                 open.setOnClickListener(v -> openWebPage(slab.raw));
-                row.addView(open);
+                buttons.addView(open);
             }
+            Button msi = miniButton("MSI");
+            msi.setOnClickListener(v -> openWebPage(msiUrlForSlab(slab)));
+            buttons.addView(msi);
             Button remove = miniButton("REMOVE");
             remove.setOnClickListener(v -> {
                 slabs.remove(index);
                 saveLists();
                 renderSlabs();
             });
-            row.addView(remove);
+            buttons.addView(remove);
+            row.addView(buttons);
             slabList.addView(row);
         }
     }
 
-    private void addCounterSection() {
+    private boolean addCounterSection() {
         double length = value(lengthIn);
         double width = value(widthIn);
         double qty = value(quantity);
         if (length <= 0 || width <= 0 || qty <= 0) {
             Toast.makeText(this, "Enter a length, width, and quantity.", Toast.LENGTH_LONG).show();
-            return;
+            return false;
         }
         String name = sectionName.getText().toString().trim();
         if (name.isEmpty()) name = "Countertop section " + (sections.size() + 1);
@@ -347,6 +448,7 @@ public class MainActivity extends Activity {
         saveLists();
         renderSections();
         calculateAndDisplay(false);
+        return true;
     }
 
     private void renderSections() {
@@ -544,6 +646,21 @@ public class MainActivity extends Activity {
         return value != null && (value.startsWith("https://") || value.startsWith("http://"));
     }
 
+    private String msiUrlForSlab(SlabSelection slab) {
+        if (isWebUrl(slab.raw) && slab.raw.toLowerCase(Locale.US).contains("msisurfaces.com")) {
+            return slab.raw;
+        }
+        String query = slab.name == null || slab.name.trim().isEmpty() ? slab.raw : slab.name;
+        if (query == null || query.trim().isEmpty()) query = "quartz";
+        return "https://www.msisurfaces.com/search/?search=" + Uri.encode(cleanMsiSearch(query));
+    }
+
+    private String cleanMsiSearch(String value) {
+        return value.replace("MSI:", "")
+                .replace("...", "")
+                .trim();
+    }
+
     private void saveLists() {
         try {
             JSONArray slabArray = new JSONArray();
'''

result = subprocess.run(["git", "apply", "--whitespace=nowarn"], input=patch, text=True)
if result.returncode != 0:
    sys.exit(result.returncode)
print("Applied wizard patch.")
