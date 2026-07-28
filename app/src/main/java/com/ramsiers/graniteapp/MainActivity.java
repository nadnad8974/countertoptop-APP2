package com.ramsiers.graniteapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 901;
    private static final int TOTAL_STEPS = 21;
    private static final String PREFS = "ramsiers_granite_app";
    private static final String MSI_VISUALIZER = "https://www.msisurfaces.com/room-visualizer-tools/";

    private final ArrayList<SlabSelection> slabs = new ArrayList<>();
    private final ArrayList<CounterSection> sections = new ArrayList<>();
    private final DecimalFormat number = new DecimalFormat("0.00");

    private SharedPreferences prefs;
    private ScrollView scroll;
    private LinearLayout page;
    private LinearLayout navigation;
    private LinearLayout slabList;
    private LinearLayout sectionList;

    private EditText customerName;
    private EditText customerPhone;
    private EditText customerEmail;
    private EditText projectAddress;
    private EditText projectNotes;
    private EditText officeEmail;
    private EditText sectionName;
    private EditText lengthIn;
    private EditText widthIn;
    private EditText quantity;
    private EditText stoveLength;
    private EditText stoveWidth;
    private EditText pricePerSqFt;
    private EditText sinkCharge;
    private EditText edgeCharge;
    private EditText tearOutCharge;
    private EditText otherCharge;

    private TextView squareFootResult;
    private TextView totalResult;
    private TextView photoStatus;
    private ImageView roomPhoto;
    private Uri selectedPhotoUri;
    private int stepIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadSavedLists();
        buildUi();
    }

    private void buildUi() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(Color.rgb(248, 246, 243));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(16), dp(12), dp(16), dp(28));
        scroll.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setGravity(Gravity.CENTER);
        navigation.setPadding(dp(12), dp(8), dp(12), dp(10));
        navigation.setBackgroundColor(Color.rgb(248, 246, 243));

        screen.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        screen.addView(navigation, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        customerName = input("Type the customer's full name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        customerPhone = input("Type the customer's phone number", InputType.TYPE_CLASS_PHONE);
        customerEmail = input("Type the customer's email", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        projectAddress = input("Type the project address", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        projectNotes = input("Type any project notes", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        projectNotes.setMinLines(4);
        projectNotes.setGravity(Gravity.TOP);

        officeEmail = input("RAMSIER'S office email", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        officeEmail.setText(prefs.getString("office_email", ""));

        slabList = new LinearLayout(this);
        slabList.setOrientation(LinearLayout.VERTICAL);

        sectionName = input("Example: Island or Main wall", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        lengthIn = input("Length in inches", decimalInput());
        widthIn = input("Width in inches", decimalInput());
        quantity = input("Quantity", decimalInput());
        quantity.setText("1");

        sectionList = new LinearLayout(this);
        sectionList.setOrientation(LinearLayout.VERTICAL);

        stoveLength = input("Leave blank if there is no opening", decimalInput());
        stoveWidth = input("Leave blank if there is no opening", decimalInput());
        pricePerSqFt = input("Installed price per square foot", decimalInput());
        sinkCharge = input("Enter 0 if there is no charge", decimalInput());
        edgeCharge = input("Enter 0 if there is no charge", decimalInput());
        tearOutCharge = input("Enter 0 if there is no charge", decimalInput());
        otherCharge = input("Enter 0 if there is no charge", decimalInput());

        squareFootResult = resultLabel("Net square footage: 0.00");
        totalResult = resultLabel("Estimated total: $0.00");
        photoStatus = label("No photo selected.");
        photoStatus.setTextSize(14);
        roomPhoto = new ImageView(this);
        roomPhoto.setAdjustViewBounds(true);
        roomPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);

        setContentView(screen);
        showStep();
    }

    private void showStep() {
        page.removeAllViews();
        navigation.removeAllViews();

        addBrandHeader();
        TextView progress = label("Question " + (stepIndex + 1) + " of " + TOTAL_STEPS);
        progress.setGravity(Gravity.CENTER);
        progress.setTextColor(Color.GRAY);
        page.addView(progress);

        switch (stepIndex) {
            case 0:
                addQuestion("What is the customer's name?", customerName, true);
                break;
            case 1:
                addQuestion("What is the customer's phone number?", customerPhone, true);
                break;
            case 2:
                addQuestion("What is the customer's email address?", customerEmail, true);
                break;
            case 3:
                addQuestion("What is the project address?", projectAddress, true);
                break;
            case 4:
                addQuestion("Are there any project notes?", projectNotes, true);
                break;
            case 5:
                addQuestion("What email should receive the quote request?", officeEmail, true);
                addHelp("This RAMSIER'S office email is saved on this phone.");
                break;
            case 6:
                addSlabStep();
                break;
            case 7:
                addQuestion("What should this countertop section be called?", sectionName, true);
                break;
            case 8:
                addQuestion("What is the section length in inches?", lengthIn, true);
                break;
            case 9:
                addQuestion("What is the section width in inches?", widthIn, true);
                break;
            case 10:
                addQuestion("How many identical sections are there?", quantity, true);
                addHelp("Tapping Next saves this countertop section.");
                break;
            case 11:
                addAnotherSectionStep();
                break;
            case 12:
                addQuestion("What is the slide-in stove opening length?", stoveLength, true);
                addHelp("Leave this blank if there is no slide-in stove opening.");
                break;
            case 13:
                addQuestion("What is the slide-in stove opening width?", stoveWidth, true);
                addHelp("Leave this blank if there is no slide-in stove opening.");
                break;
            case 14:
                addQuestion("What is the installed price per square foot?", pricePerSqFt, true);
                break;
            case 15:
                addQuestion("What is the sink or cutout charge?", sinkCharge, true);
                break;
            case 16:
                addQuestion("What is the edge or extra labor charge?", edgeCharge, true);
                break;
            case 17:
                addQuestion("What is the tear-out charge?", tearOutCharge, true);
                break;
            case 18:
                addQuestion("Are there any other charges?", otherCharge, true);
                break;
            case 19:
                addPhotoStep();
                break;
            default:
                addReviewStep();
                break;
        }

        addNavigation();
        scroll.post(() -> scroll.smoothScrollTo(0, 0));
    }

    private void addBrandHeader() {
        TextView title = new TextView(this);
        title.setText("RAMSIER'S\nGRANITE AND QUARTZ");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(91, 58, 41));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(8), 0, dp(2));
        page.addView(title);
    }

    private void addQuestion(String title, EditText field, boolean focus) {
        page.addView(questionTitle(title));
        detach(field);
        page.addView(field);
        Button inlineNext = primaryButton(nextButtonText());
        inlineNext.setOnClickListener(v -> handleNext());
        page.addView(inlineNext);
        if (focus) {
            field.requestFocus();
            field.postDelayed(() -> {
                scroll.smoothScrollTo(0, Math.max(0, field.getTop() - dp(24)));
                showKeyboard(field);
            }, 220);
        }
    }

    private void addHelp(String text) {
        TextView help = label(text);
        help.setTextSize(14);
        help.setGravity(Gravity.CENTER);
        help.setTextColor(Color.DKGRAY);
        page.addView(help);
    }

    private void addSlabStep() {
        hideKeyboard();
        page.addView(questionTitle("Which slabs does the customer like?"));
        addHelp("Scan MSI slab QR codes or add a slab color manually. Add as many as needed, then tap Next.");

        Button scanButton = primaryButton("Scan slab QR code");
        scanButton.setOnClickListener(v -> startQrScan());
        page.addView(scanButton);

        Button manualButton = secondaryButton("Add slab manually");
        manualButton.setOnClickListener(v -> showManualSlabDialog());
        page.addView(manualButton);

        detach(slabList);
        page.addView(slabList);
        renderSlabs();
    }

    private void addAnotherSectionStep() {
        hideKeyboard();
        page.addView(questionTitle("Would you like to add another countertop section?"));
        addHelp("The section you just entered has been saved.");

        detach(sectionList);
        page.addView(sectionList);
        renderSections();

        Button addAnother = primaryButton("Yes, add another section");
        addAnother.setOnClickListener(v -> {
            sectionName.setText("");
            lengthIn.setText("");
            widthIn.setText("");
            quantity.setText("1");
            stepIndex = 7;
            showStep();
        });
        page.addView(addAnother);
        addHelp("Tap Next below to continue without adding another section.");
    }

    private void addPhotoStep() {
        hideKeyboard();
        page.clearFocus();
        page.addView(questionTitle("Would you like to add a countertop photo?"));
        addHelp("The photo screen is separate from all typing screens, so the keyboard will not cover it.");

        Button photoButton = primaryButton("Choose kitchen or countertop photo");
        photoButton.setOnClickListener(v -> openPhotoPicker());
        page.addView(photoButton);

        detach(photoStatus);
        page.addView(photoStatus);
        detach(roomPhoto);
        page.addView(roomPhoto, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));

        Button visualizer = secondaryButton("Open MSI room visualizer");
        visualizer.setOnClickListener(v -> openWebPage(MSI_VISUALIZER));
        page.addView(visualizer);
        addHelp("You may skip the photo and tap Next.");
    }

    private void addReviewStep() {
        hideKeyboard();
        page.clearFocus();
        calculateAndDisplay(false);
        page.addView(questionTitle("Review the quote request"));

        TextView customer = label(
                "Customer: " + text(customerName) + "\n" +
                "Phone: " + text(customerPhone) + "\n" +
                "Email: " + text(customerEmail) + "\n" +
                "Address: " + text(projectAddress));
        customer.setBackgroundColor(Color.WHITE);
        customer.setPadding(dp(12), dp(12), dp(12), dp(12));
        page.addView(customer);

        page.addView(sectionHeader("Countertop sections"));
        detach(sectionList);
        page.addView(sectionList);
        renderSections();

        page.addView(sectionHeader("Selected slabs"));
        detach(slabList);
        page.addView(slabList);
        renderSlabs();

        detach(squareFootResult);
        page.addView(squareFootResult);
        detach(totalResult);
        page.addView(totalResult);

        Button reset = secondaryButton("Start a new customer");
        reset.setOnClickListener(v -> confirmReset());
        page.addView(reset);
    }

    private void addNavigation() {
        Button back = secondaryButton("Back");
        back.setEnabled(stepIndex > 0);
        back.setOnClickListener(v -> {
            hideKeyboard();
            stepIndex = Math.max(0, stepIndex - 1);
            showStep();
        });
        navigation.addView(back, new LinearLayout.LayoutParams(0, dp(54), 1f));

        Button next = primaryButton(nextButtonText());
        next.setOnClickListener(v -> handleNext());
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(0, dp(54), 1f);
        nextParams.setMargins(dp(8), 0, 0, 0);
        navigation.addView(next, nextParams);
    }

    private String nextButtonText() {
        if (stepIndex >= TOTAL_STEPS - 1) {
            return "Send email";
        }
        if (stepIndex == 11) {
            return "No, continue";
        }
        return "Next";
    }

    private void handleNext() {
        if (stepIndex == 0 && customerName.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Enter the customer's name.", Toast.LENGTH_SHORT).show();
            customerName.requestFocus();
            return;
        }
        if (stepIndex == 5) {
            String email = officeEmail.getText().toString().trim();
            if (email.isEmpty() || !email.contains("@")) {
                Toast.makeText(this, "Enter the RAMSIER'S office email.", Toast.LENGTH_LONG).show();
                officeEmail.requestFocus();
                return;
            }
            prefs.edit().putString("office_email", email).apply();
        }
        if (stepIndex == 8 && value(lengthIn) <= 0) {
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
        if (stepIndex >= TOTAL_STEPS - 1) {
            sendQuoteEmail();
            return;
        }

        hideKeyboard();
        stepIndex += 1;
        showStep();
    }

    private void startQrScan() {
        hideKeyboard();
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Point the camera at the slab QR code");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.setDesiredBarcodeFormats(Collections.singleton(BarcodeFormat.QR_CODE.toString()));
        integrator.initiateScan();
    }

    private void showManualSlabDialog() {
        final EditText entry = input("Slab color, item number, or note", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        entry.setMinLines(2);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add slab manually")
                .setView(entry)
                .setPositiveButton("Add", (d, which) -> addSlab(entry.getText().toString()))
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(d -> {
            entry.requestFocus();
            entry.postDelayed(() -> showKeyboard(entry), 180);
        });
        dialog.show();
    }

    private void addSlab(String rawValue) {
        String raw = rawValue == null ? "" : rawValue.trim();
        if (raw.isEmpty()) {
            Toast.makeText(this, "No slab information was entered.", Toast.LENGTH_SHORT).show();
            return;
        }
        for (SlabSelection slab : slabs) {
            if (slab.raw.equalsIgnoreCase(raw)) {
                Toast.makeText(this, "That slab is already saved.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        slabs.add(new SlabSelection(nameFromQr(raw), raw));
        saveLists();
        renderSlabs();
        Toast.makeText(this, "Slab saved.", Toast.LENGTH_SHORT).show();
    }

    private String nameFromQr(String raw) {
        try {
            Uri uri = Uri.parse(raw);
            if (uri.getHost() != null && uri.getHost().toLowerCase(Locale.US).contains("msisurfaces.com")) {
                String path = uri.getPath();
                if (path != null) {
                    String[] pieces = path.split("/");
                    for (int i = pieces.length - 1; i >= 0; i--) {
                        if (!pieces[i].trim().isEmpty()) {
                            String clean = pieces[i].replace('-', ' ').replace('_', ' ').trim();
                            return "MSI: " + capitalizeWords(clean);
                        }
                    }
                }
                return "MSI slab selection";
            }
        } catch (Exception ignored) {
        }
        return raw.length() > 55 ? raw.substring(0, 52) + "..." : raw;
    }

    private String capitalizeWords(String text) {
        StringBuilder result = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (word.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) result.append(word.substring(1));
        }
        return result.toString();
    }

    private void renderSlabs() {
        if (slabList == null) return;
        slabList.removeAllViews();
        if (slabs.isEmpty()) {
            TextView empty = label("No slabs saved yet.");
            empty.setTextColor(Color.GRAY);
            slabList.addView(empty);
            return;
        }
        for (int i = 0; i < slabs.size(); i++) {
            final int index = i;
            SlabSelection slab = slabs.get(i);
            LinearLayout row = itemRow();
            row.setOrientation(LinearLayout.VERTICAL);
            TextView text = label((i + 1) + ". " + slab.name + "\n" + slab.raw);
            text.setTextSize(14);
            row.addView(text, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout buttons = new LinearLayout(this);
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            if (isWebUrl(slab.raw)) {
                Button open = miniButton("OPEN");
                open.setOnClickListener(v -> openWebPage(slab.raw));
                buttons.addView(open);
            }
            Button msi = miniButton("MSI");
            msi.setOnClickListener(v -> openWebPage(msiUrlForSlab(slab)));
            buttons.addView(msi);
            Button remove = miniButton("REMOVE");
            remove.setOnClickListener(v -> {
                slabs.remove(index);
                saveLists();
                renderSlabs();
            });
            buttons.addView(remove);
            row.addView(buttons);
            slabList.addView(row);
        }
    }

    private boolean addCounterSection() {
        double length = value(lengthIn);
        double width = value(widthIn);
        double qty = value(quantity);
        if (length <= 0 || width <= 0 || qty <= 0) {
            Toast.makeText(this, "Enter a length, width, and quantity.", Toast.LENGTH_LONG).show();
            return false;
        }
        String name = sectionName.getText().toString().trim();
        if (name.isEmpty()) name = "Countertop section " + (sections.size() + 1);
        sections.add(new CounterSection(name, length, width, qty));
        saveLists();
        renderSections();
        calculateAndDisplay(false);
        return true;
    }

    private void renderSections() {
        if (sectionList == null) return;
        sectionList.removeAllViews();
        if (sections.isEmpty()) {
            TextView empty = label("No countertop sections added yet.");
            empty.setTextColor(Color.GRAY);
            sectionList.addView(empty);
            return;
        }
        for (int i = 0; i < sections.size(); i++) {
            final int index = i;
            CounterSection section = sections.get(i);
            LinearLayout row = itemRow();
            String details = section.name + "\n" + number.format(section.length) + " × " + number.format(section.width)
                    + " in × " + number.format(section.quantity) + " = " + number.format(section.squareFeet()) + " sq ft";
            TextView text = label(details);
            text.setTextSize(14);
            row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Button remove = miniButton("REMOVE");
            remove.setOnClickListener(v -> {
                sections.remove(index);
                saveLists();
                renderSections();
                calculateAndDisplay(false);
            });
            row.addView(remove);
            sectionList.addView(row);
        }
    }

    private Estimate calculateAndDisplay(boolean showWarnings) {
        double gross = 0;
        for (CounterSection section : sections) gross += section.squareFeet();

        double stove = (value(stoveLength) * value(stoveWidth)) / 144.0;
        double net = Math.max(0, gross - stove);
        double total = net * value(pricePerSqFt)
                + value(sinkCharge)
                + value(edgeCharge)
                + value(tearOutCharge)
                + value(otherCharge);

        if (squareFootResult != null) squareFootResult.setText("Net square footage: " + number.format(net));
        if (totalResult != null) totalResult.setText("Estimated total: $" + number.format(total));

        if (showWarnings && gross <= 0) {
            Toast.makeText(this, "Add at least one countertop section first.", Toast.LENGTH_LONG).show();
        }
        return new Estimate(gross, stove, net, total);
    }

    private void openPhotoPicker() {
        hideKeyboard();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scan = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scan != null) {
            if (scan.getContents() != null) addSlab(scan.getContents());
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedPhotoUri = data.getData();
            try {
                int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(selectedPhotoUri, flags);
            } catch (Exception ignored) {
            }
            roomPhoto.setImageURI(selectedPhotoUri);
            photoStatus.setText("Photo selected and ready to attach to the email.");
        }
    }

    private void sendQuoteEmail() {
        String to = officeEmail.getText().toString().trim();
        if (to.isEmpty() || !to.contains("@")) {
            Toast.makeText(this, "Enter RAMSIER'S office email first.", Toast.LENGTH_LONG).show();
            stepIndex = 5;
            showStep();
            return;
        }
        prefs.edit().putString("office_email", to).apply();
        Estimate estimate = calculateAndDisplay(false);

        StringBuilder body = new StringBuilder();
        body.append("RAMSIER'S GRANITE AND QUARTZ\n");
        body.append("NEW COUNTERTOP QUOTE REQUEST\n\n");
        body.append("CUSTOMER\n");
        body.append("Name: ").append(text(customerName)).append("\n");
        body.append("Phone: ").append(text(customerPhone)).append("\n");
        body.append("Email: ").append(text(customerEmail)).append("\n");
        body.append("Address: ").append(text(projectAddress)).append("\n\n");

        body.append("SAVED SLABS / QR CODES\n");
        if (slabs.isEmpty()) body.append("No slabs saved.\n");
        for (int i = 0; i < slabs.size(); i++) {
            SlabSelection slab = slabs.get(i);
            body.append(i + 1).append(". ").append(slab.name).append("\n   ").append(slab.raw).append("\n");
        }

        body.append("\nCOUNTERTOP SECTIONS\n");
        if (sections.isEmpty()) body.append("No saved sections.\n");
        for (CounterSection section : sections) {
            body.append("- ").append(section.name).append(": ")
                    .append(number.format(section.length)).append(" × ")
                    .append(number.format(section.width)).append(" in × ")
                    .append(number.format(section.quantity)).append(" = ")
                    .append(number.format(section.squareFeet())).append(" sq ft\n");
        }
        body.append("Gross square feet: ").append(number.format(estimate.gross)).append("\n");
        body.append("Stove opening subtracted: ").append(number.format(estimate.stove)).append(" sq ft\n");
        body.append("Net square feet: ").append(number.format(estimate.net)).append("\n");
        body.append("Estimated total: $").append(number.format(estimate.total)).append("\n");
        body.append("This is an estimate and needs final verification by RAMSIER'S.\n\n");
        body.append("PROJECT NOTES\n").append(text(projectNotes)).append("\n");

        Intent email = new Intent(Intent.ACTION_SEND);
        email.setType(selectedPhotoUri == null ? "text/plain" : "image/*");
        email.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
        email.putExtra(Intent.EXTRA_SUBJECT, "New countertop quote request - " + text(customerName));
        email.putExtra(Intent.EXTRA_TEXT, body.toString());
        if (selectedPhotoUri != null) {
            email.putExtra(Intent.EXTRA_STREAM, selectedPhotoUri);
            email.setClipData(ClipData.newUri(getContentResolver(), "Countertop photo", selectedPhotoUri));
            email.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        try {
            startActivity(Intent.createChooser(email, "Send quote request by email"));
        } catch (Exception e) {
            Toast.makeText(this, "No email app was found on this phone.", Toast.LENGTH_LONG).show();
        }
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("Start a new customer?")
                .setMessage("This clears the current customer, scanned slabs, measurements, and photo. The office email stays saved.")
                .setPositiveButton("Clear", (dialog, which) -> resetQuote())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetQuote() {
        slabs.clear();
        sections.clear();
        selectedPhotoUri = null;
        customerName.setText("");
        customerPhone.setText("");
        customerEmail.setText("");
        projectAddress.setText("");
        projectNotes.setText("");
        sectionName.setText("");
        lengthIn.setText("");
        widthIn.setText("");
        quantity.setText("1");
        stoveLength.setText("");
        stoveWidth.setText("");
        pricePerSqFt.setText("");
        sinkCharge.setText("");
        edgeCharge.setText("");
        tearOutCharge.setText("");
        otherCharge.setText("");
        roomPhoto.setImageDrawable(null);
        photoStatus.setText("No photo selected.");
        squareFootResult.setText("Net square footage: 0.00");
        totalResult.setText("Estimated total: $0.00");
        stepIndex = 0;
        saveLists();
        showStep();
        Toast.makeText(this, "Ready for a new customer.", Toast.LENGTH_SHORT).show();
    }

    private void showKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        View current = getCurrentFocus();
        if (current == null) current = page;
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(current.getWindowToken(), 0);
        }
        current.clearFocus();
    }

    private void openWebPage(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "A web browser could not be opened.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isWebUrl(String value) {
        return value != null && (value.startsWith("https://") || value.startsWith("http://"));
    }

    private String msiUrlForSlab(SlabSelection slab) {
        if (isWebUrl(slab.raw) && slab.raw.toLowerCase(Locale.US).contains("msisurfaces.com")) {
            return slab.raw;
        }
        String query = slab.name == null || slab.name.trim().isEmpty() ? slab.raw : slab.name;
        if (query == null || query.trim().isEmpty()) query = "quartz";
        return "https://www.msisurfaces.com/search/?search=" + Uri.encode(cleanMsiSearch(query));
    }

    private String cleanMsiSearch(String value) {
        return value.replace("MSI:", "").replace("...", "").trim();
    }

    private void saveLists() {
        try {
            JSONArray slabArray = new JSONArray();
            for (SlabSelection slab : slabs) {
                JSONObject object = new JSONObject();
                object.put("name", slab.name);
                object.put("raw", slab.raw);
                slabArray.put(object);
            }
            JSONArray sectionArray = new JSONArray();
            for (CounterSection section : sections) {
                JSONObject object = new JSONObject();
                object.put("name", section.name);
                object.put("length", section.length);
                object.put("width", section.width);
                object.put("quantity", section.quantity);
                sectionArray.put(object);
            }
            prefs.edit()
                    .putString("slabs", slabArray.toString())
                    .putString("sections", sectionArray.toString())
                    .apply();
        } catch (Exception ignored) {
        }
    }

    private void loadSavedLists() {
        slabs.clear();
        sections.clear();
        try {
            JSONArray slabArray = new JSONArray(prefs.getString("slabs", "[]"));
            for (int i = 0; i < slabArray.length(); i++) {
                JSONObject object = slabArray.getJSONObject(i);
                slabs.add(new SlabSelection(object.optString("name"), object.optString("raw")));
            }
            JSONArray sectionArray = new JSONArray(prefs.getString("sections", "[]"));
            for (int i = 0; i < sectionArray.length(); i++) {
                JSONObject object = sectionArray.getJSONObject(i);
                sections.add(new CounterSection(
                        object.optString("name"),
                        object.optDouble("length"),
                        object.optDouble("width"),
                        object.optDouble("quantity", 1)));
            }
        } catch (Exception ignored) {
            slabs.clear();
            sections.clear();
        }
    }

    private String text(EditText editText) {
        String value = editText.getText().toString().trim();
        return value.isEmpty() ? "Not provided" : value;
    }

    private double value(EditText editText) {
        try {
            return Double.parseDouble(editText.getText().toString().trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int decimalInput() {
        return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
    }

    private TextView questionTitle(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(24);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(Color.rgb(91, 58, 41));
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(8), dp(34), dp(8), dp(18));
        return view;
    }

    private TextView sectionHeader(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(18);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(Color.WHITE);
        view.setBackgroundColor(Color.rgb(91, 58, 41));
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(18), 0, dp(8));
        view.setLayoutParams(params);
        return view;
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(16);
        view.setTextColor(Color.rgb(45, 45, 45));
        view.setPadding(dp(4), dp(6), dp(4), dp(6));
        return view;
    }

    private TextView resultLabel(String text) {
        TextView view = label(text);
        view.setTextSize(21);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(Color.rgb(91, 58, 41));
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(8), dp(10), dp(8), dp(10));
        return view;
    }

    private EditText input(String hint, int type) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setInputType(type);
        editText.setTextSize(19);
        editText.setSingleLine((type & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        editText.setPadding(dp(14), dp(14), dp(14), dp(14));
        editText.setBackgroundColor(Color.WHITE);
        editText.setSelectAllOnFocus(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(8), 0, dp(12));
        editText.setLayoutParams(params);
        return editText;
    }

    private Button primaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.rgb(182, 132, 69));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        params.setMargins(0, dp(6), 0, dp(6));
        button.setLayoutParams(params);
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTextColor(Color.rgb(91, 58, 41));
        button.setBackgroundColor(Color.rgb(239, 230, 220));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        params.setMargins(0, dp(4), 0, dp(4));
        button.setLayoutParams(params);
        return button;
    }

    private Button miniButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(10);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(5), 0, dp(5), 0);
        return button;
    }

    private LinearLayout itemRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(7), dp(5), dp(7));
        row.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(3), 0, dp(3));
        row.setLayoutParams(params);
        return row;
    }

    private void detach(View view) {
        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class SlabSelection {
        final String name;
        final String raw;

        SlabSelection(String name, String raw) {
            this.name = name == null || name.trim().isEmpty() ? "Slab selection" : name;
            this.raw = raw == null ? "" : raw;
        }
    }

    private static class CounterSection {
        final String name;
        final double length;
        final double width;
        final double quantity;

        CounterSection(String name, double length, double width, double quantity) {
            this.name = name == null || name.trim().isEmpty() ? "Countertop section" : name;
            this.length = length;
            this.width = width;
            this.quantity = quantity;
        }

        double squareFeet() {
            return (length * width * quantity) / 144.0;
        }
    }

    private static class Estimate {
        final double gross;
        final double stove;
        final double net;
        final double total;

        Estimate(double gross, double stove, double net, double total) {
            this.gross = gross;
            this.stove = stove;
            this.net = net;
            this.total = total;
        }
    }
}
