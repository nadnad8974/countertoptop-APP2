package com.ramsiers.graniteapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
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
    private static final String PREFS = "ramsiers_granite_app";
    private static final String MSI_VISUALIZER = "https://www.msisurfaces.com/room-visualizer-tools/";

    private final ArrayList<SlabSelection> slabs = new ArrayList<>();
    private final ArrayList<CounterSection> sections = new ArrayList<>();
    private final DecimalFormat number = new DecimalFormat("0.00");

    private SharedPreferences prefs;
    private ScrollView scroll;
    private LinearLayout root;
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
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(24));
        root.setBackgroundColor(Color.rgb(248, 246, 243));
        scroll.addView(root);
        customerName = input("Customer name", InputType.TYPE_CLASS_TEXT);
        customerPhone = input("Phone number", InputType.TYPE_CLASS_PHONE);
        customerEmail = input("Customer email", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        projectAddress = input("Project address", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        projectNotes = input("Project notes", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        projectNotes.setMinLines(3);
        officeEmail = input("RAMSIER'S office email (saved on this phone)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        officeEmail.setText(prefs.getString("office_email", ""));
        slabList = new LinearLayout(this);
        slabList.setOrientation(LinearLayout.VERTICAL);
        sectionName = input("Section name, such as Island", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        lengthIn = input("Length in inches", decimalInput());
        widthIn = input("Width in inches", decimalInput());
        quantity = input("Quantity", decimalInput());
        quantity.setText("1");
        sectionList = new LinearLayout(this);
        sectionList.setOrientation(LinearLayout.VERTICAL);
        stoveLength = input("Stove opening length in inches", decimalInput());
        stoveWidth = input("Stove opening width in inches", decimalInput());
        pricePerSqFt = input("Installed price per square foot", decimalInput());
        sinkCharge = input("Sink / cutout charge", decimalInput());
        edgeCharge = input("Edge or extra labor charge", decimalInput());
        tearOutCharge = input("Tear-out charge", decimalInput());
        otherCharge = input("Other charges", decimalInput());
        squareFootResult = resultLabel("Net square footage: 0.00");
        totalResult = resultLabel("Estimated total: $0.00");
        photoStatus = label("No photo selected.");
        photoStatus.setTextSize(14);
        roomPhoto = new ImageView(this);
        roomPhoto.setAdjustViewBounds(true);
        roomPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        setContentView(scroll);
        showStep();
    }

    private void showStep() {
        root.removeAllViews();
        TextView title = new TextView(this);
        title.setText("RAMSIER'S\nGRANITE AND QUARTZ");
        title.setTextSize(27);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(91, 58, 41));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(10), 0, dp(2));
        root.addView(title);

        TextView progress = label("Question " + (stepIndex + 1) + " of 16");
        progress.setGravity(Gravity.CENTER);
        progress.setTextColor(Color.GRAY);
        root.addView(progress);

        switch (stepIndex) {
            case 0:
                addQuestion("Customer name", customerName);
                break;
            case 1:
                addQuestion("Phone number", customerPhone);
                break;
            case 2:
                addQuestion("Customer email", customerEmail);
                break;
            case 3:
                addQuestion("Project address", projectAddress);
                break;
            case 4:
                addQuestion("Project notes", projectNotes);
                break;
            case 5:
                addQuestion("RAMSIER'S office email", officeEmail);
                break;
            case 6:
                addSlabStep();
                break;
            case 7:
                addQuestion("Countertop section name", sectionName);
                break;
            case 8:
                addQuestion("Length in inches", lengthIn);
                break;
            case 9:
                addQuestion("Width in inches", widthIn);
                break;
            case 10:
                addQuestion("Quantity", quantity);
                TextView saveHelp = label("Tap OK to save this countertop section.");
                saveHelp.setGravity(Gravity.CENTER);
                root.addView(saveHelp);
                break;
            case 11:
                addTwoFieldQuestion("Slide-in stove opening to subtract", stoveLength, stoveWidth, "Leave both blank if there is no stove opening.");
                break;
            case 12:
                addQuestion("Installed price per square foot", pricePerSqFt);
                break;
            case 13:
                addChargeStep();
                break;
            case 14:
                addPhotoStep();
                break;
            default:
                addReviewStep();
                break;
        }
        addNavigation();
        scroll.post(() -> scroll.smoothScrollTo(0, 0));
    }

    private void addQuestion(String title, EditText input) {
        root.addView(sectionHeader(title));
        addField(input);
        input.requestFocus();
    }

    private void addTwoFieldQuestion(String title, EditText first, EditText second, String helpText) {
        root.addView(sectionHeader(title));
        TextView help = label(helpText);
        help.setTextSize(14);
        root.addView(help);
        addField(first);
        addField(second);
        first.requestFocus();
    }

    private void addSlabStep() {
        root.addView(sectionHeader("Slab choices"));
        TextView scanHelp = label("Scan MSI slab QR codes or type a slab color. MSI links will open from the saved list.");
        scanHelp.setTextSize(14);
        root.addView(scanHelp);

        Button scanButton = primaryButton("Scan slab QR code");
        scanButton.setOnClickListener(v -> startQrScan());
        root.addView(scanButton);

        Button manualButton = secondaryButton("Add slab manually");
        manualButton.setOnClickListener(v -> showManualSlabDialog());
        root.addView(manualButton);

        detach(slabList);
        root.addView(slabList);
        renderSlabs();
    }

    private void addChargeStep() {
        root.addView(sectionHeader("Extra charges"));
        addField(sinkCharge);
        addField(edgeCharge);
        addField(tearOutCharge);
        addField(otherCharge);
        sinkCharge.requestFocus();
    }

    private void addPhotoStep() {
        root.addView(sectionHeader("Countertop photo"));
        Button photoButton = secondaryButton("Upload kitchen / countertop photo");
        photoButton.setOnClickListener(v -> openPhotoPicker());
        root.addView(photoButton);
        detach(photoStatus);
        root.addView(photoStatus);
        detach(roomPhoto);
        root.addView(roomPhoto, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));

        Button visualizer = secondaryButton("Open MSI room visualizer");
        visualizer.setOnClickListener(v -> openWebPage(MSI_VISUALIZER));
        root.addView(visualizer);
    }

    private void addReviewStep() {
        calculateAndDisplay(false);
        root.addView(sectionHeader("Review and send"));
        detach(sectionList);
        root.addView(sectionList);
        renderSections();
        detach(slabList);
        root.addView(slabList);
        renderSlabs();
        detach(squareFootResult);
        root.addView(squareFootResult);
        detach(totalResult);
        root.addView(totalResult);

        Button reset = secondaryButton("Start a new customer");
        reset.setOnClickListener(v -> confirmReset());
        root.addView(reset);
    }

    private void addNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, dp(16), 0, 0);

        Button back = secondaryButton("Back");
        back.setEnabled(stepIndex > 0);
        back.setOnClickListener(v -> {
            stepIndex = Math.max(0, stepIndex - 1);
            showStep();
        });
        nav.addView(back, new LinearLayout.LayoutParams(0, dp(52), 1f));

        Button next = primaryButton(stepIndex >= 15 ? "Send email" : "OK");
        next.setOnClickListener(v -> handleNext());
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(0, dp(52), 1f);
        nextParams.setMargins(dp(8), 0, 0, 0);
        nav.addView(next, nextParams);
        root.addView(nav);
    }

    private void handleNext() {
        if (stepIndex == 5) {
            prefs.edit().putString("office_email", officeEmail.getText().toString().trim()).apply();
        }
        if (stepIndex == 10 && !addCounterSection()) {
            return;
        }
        if (stepIndex >= 15) {
            sendQuoteEmail();
            return;
        }
        stepIndex += 1;
        showStep();
    }

    private void addField(View view) {
        detach(view);
        root.addView(view);
    }

    private void detach(View view) {
        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }

    private void startQrScan() {
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
        new AlertDialog.Builder(this)
                .setTitle("Add slab manually")
                .setView(entry)
                .setPositiveButton("Add", (dialog, which) -> addSlab(entry.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
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
            row.addView(text, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
        sectionName.setText("");
        lengthIn.setText("");
        widthIn.setText("");
        quantity.setText("1");
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

        if (sections.isEmpty()) {
            double currentLength = value(lengthIn);
            double currentWidth = value(widthIn);
            double currentQty = Math.max(1, value(quantity));
            if (currentLength > 0 && currentWidth > 0) gross = (currentLength * currentWidth * currentQty) / 144.0;
        }

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
            officeEmail.requestFocus();
            Toast.makeText(this, "Enter RAMSIER'S office email first. It will be saved for next time.", Toast.LENGTH_LONG).show();
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
        saveLists();
        renderSlabs();
        renderSections();
        Toast.makeText(this, "Ready for a new customer.", Toast.LENGTH_SHORT).show();
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
        return value.replace("MSI:", "")
                .replace("...", "")
                .trim();
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

    private TextView sectionHeader(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(20);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(Color.WHITE);
        view.setBackgroundColor(Color.rgb(91, 58, 41));
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(20), 0, dp(8));
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
        editText.setTextSize(16);
        editText.setPadding(dp(10), dp(7), dp(10), dp(7));
        editText.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(4), 0, dp(4));
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        params.setMargins(0, dp(5), 0, dp(5));
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(3), 0, dp(3));
        row.setLayoutParams(params);
        return row;
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
