package com.ramsiers.graniteapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 901;
    private static final int TAKE_DRAWING_PHOTO = 902;
    private static final int PICK_DRAWING_IMAGE = 903;
    private static final String PREFS = "ramsiers_granite_app";
    private static final String MSI_VISUALIZER = "https://www.roomvo.com/my/msi/?product_type=1&multi_product_visualizer=5";
    private static final String DRAWING_AI_ENDPOINT =
            "https://ramsiers-drawing-ai.nadnad8974.chatgpt.site/api/analyze";
    private static final String PRICE_CUTOUT = "price_cutout";
    private static final String PRICE_EDGE = "price_edge";
    private static final String PRICE_FAUCET = "price_faucet";
    private static final String PRICE_BASKET = "price_basket";
    private static final String PRICE_GRID = "price_grid";
    private static final String DEFAULT_PAGE_ORDER = "0,1,2,3,8,13,15,16,17,20,18,19,11,12,14,6,4";
    private static final String ALL_BUILT_IN_PAGES = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,100,101,102,103,104,105,106";
    private static final int CUSTOM_PAGE_START = 1000;
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
    private static final int PAGE_COOKTOP_CUTOUT = 13;
    private static final int PAGE_EDGE_DETAIL = 14;
    private static final int PAGE_FAUCET = 15;
    private static final int PAGE_BASKETS = 16;
    private static final int PAGE_GRIDS = 17;
    private static final int PAGE_CABINETS = 18;
    private static final int PAGE_BUY_CABINETS = 19;
    private static final int PAGE_WATERFALL = 20;
    private static final int PAGE_SECTION_NAME = 100;
    private static final int PAGE_SECTION_LENGTH = 101;
    private static final int PAGE_SECTION_WIDTH = 102;
    private static final int PAGE_SECTION_QUANTITY = 103;
    private static final int PAGE_ADD_SECTION = 104;
    private static final int PAGE_STOVE_LENGTH = 105;
    private static final int PAGE_STOVE_WIDTH = 106;

    private final ArrayList<SlabSelection> slabs = new ArrayList<>();
    private final ArrayList<CounterSection> sections = new ArrayList<>();
    private final ArrayList<Integer> pageOrder = new ArrayList<>();
    private final ArrayList<CustomPage> customPages = new ArrayList<>();
    private final HashMap<Integer, EditText> customInputs = new HashMap<>();
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
    private EditText cooktopCutoutQuantity;
    private EditText edgeLinearFeet;
    private EditText faucetQuantity;
    private EditText equalDoubleSinkQuantity;
    private EditText offsetDoubleSinkQuantity;
    private EditText singleBowlSinkQuantity;
    private EditText whiteRectangleVanitySinkQuantity;
    private EditText biscuitRectangleVanitySinkQuantity;
    private EditText rectangleVanitySinkLocations;
    private EditText whiteOvalVanitySinkQuantity;
    private EditText biscuitOvalVanitySinkQuantity;
    private EditText ovalVanitySinkLocations;
    private EditText anotherSinkQuantity;
    private EditText undecidedSinkQuantity;
    private EditText basketQuantity;
    private EditText gridQuantity;
    private EditText waterfallQuantity;
    private EditText waterfallComments;
    private EditText cabinetsApproximateDate;
    private EditText cabinetInterestComments;

    private boolean cooktopCutoutYes;
    private boolean basketsYes;
    private boolean gridsYes;
    private boolean cabinetsInYes;
    private boolean wantsToBuyCabinets;
    private String edgeDetail = "Eased and polished";
    private String sinkSelection = "Not selected";
    private double aiDrawingSquareFeet;
    private String aiDrawingConfidence = "";
    private String aiDrawingExplanation = "";
    private String aiDrawingMissingInformation = "";

    private TextView squareFootResult;
    private TextView totalResult;
    private TextView photoStatus;
    private TextView drawingStatus;
    private ImageView roomPhoto;
    private ImageView drawingPhoto;
    private Button analyzeDrawingButton;
    private Uri selectedPhotoUri;
    private Uri drawingPhotoUri;
    private int stepIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadSavedLists();
        loadCustomPages();
        loadPageOrder();
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
        page.setPadding(dp(16), dp(12), dp(16), dp(110));
        scroll.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setGravity(Gravity.CENTER);
        navigation.setPadding(dp(12), dp(8), dp(12), dp(58));
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
        cooktopCutoutQuantity = input("How many cooktop or extra cutouts?", decimalInput());
        cooktopCutoutQuantity.setText("0");
        edgeLinearFeet = input("How many linear feet receive this edge?", decimalInput());
        edgeLinearFeet.setText("0");
        faucetQuantity = quantityInput("How many RAMSIER'S faucets?");
        equalDoubleSinkQuantity = quantityInput("How many equal double-bowl kitchen sinks?");
        offsetDoubleSinkQuantity = quantityInput("How many offset double-bowl kitchen sinks?");
        singleBowlSinkQuantity = quantityInput("How many single-bowl kitchen sinks?");
        whiteRectangleVanitySinkQuantity = quantityInput("How many white rectangle vanity sinks?");
        biscuitRectangleVanitySinkQuantity = quantityInput("How many biscuit rectangle vanity sinks?");
        rectangleVanitySinkLocations = input("Where do the rectangle sinks go? Example: 2 white in master bath, 1 biscuit in boys room", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        whiteOvalVanitySinkQuantity = quantityInput("How many white oval vanity sinks?");
        biscuitOvalVanitySinkQuantity = quantityInput("How many biscuit oval vanity sinks?");
        ovalVanitySinkLocations = input("Where do the oval sinks go? Example: 2 white ovals in master bath", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        anotherSinkQuantity = quantityInput("How many sinks are they providing?");
        undecidedSinkQuantity = quantityInput("How many sinks are undecided?");
        basketQuantity = input("How many baskets?", decimalInput());
        basketQuantity.setText("0");
        gridQuantity = input("How many big grids?", decimalInput());
        gridQuantity.setText("0");
        waterfallQuantity = quantityInput("How many waterfall sides?");
        waterfallComments = input("Waterfall comments, if needed", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        cabinetsApproximateDate = input("Approximate cabinet date", InputType.TYPE_CLASS_TEXT);
        cabinetInterestComments = input("Cabinet comments, if needed", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        squareFootResult = resultLabel("Net square footage: 0.00");
        totalResult = resultLabel("Estimated total: $0.00");
        photoStatus = label("No photo selected.");
        photoStatus.setTextSize(14);
        roomPhoto = new ImageView(this);
        roomPhoto.setAdjustViewBounds(true);
        roomPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        drawingStatus = label("No hand drawing photo selected.");
        drawingStatus.setTextSize(14);
        drawingPhoto = new ImageView(this);
        drawingPhoto.setAdjustViewBounds(true);
        drawingPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);

        setContentView(screen);
        showStep();
    }

    private void showStep() {
        page.removeAllViews();
        navigation.removeAllViews();

        addBrandHeader();
        TextView progress = label("Question " + (stepIndex + 1) + " of " + totalSteps());
        progress.setGravity(Gravity.CENTER);
        progress.setTextColor(Color.GRAY);
        page.addView(progress);

        if (stepIndex >= pageOrder.size()) {
            addReviewStep();
        } else {
            showQuestionPage(pageOrder.get(stepIndex));
        }

        scroll.post(() -> scroll.smoothScrollTo(0, 0));
    }

    private void showQuestionPage(int pageId) {
        switch (pageId) {
            case PAGE_NAME:
                addQuestion(questionText(pageId, "What is the customer's name?"), customerName, true);
                break;
            case PAGE_PHONE:
                addQuestion(questionText(pageId, "What is the customer's phone number?"), customerPhone, true);
                break;
            case PAGE_EMAIL:
                addQuestion(questionText(pageId, "What is the customer's email address?"), customerEmail, true);
                break;
            case PAGE_ADDRESS:
                addAddressStep();
                break;
            case PAGE_NOTES:
                addQuestion(questionText(pageId, "Are there any project notes?"), projectNotes, true);
                break;
            case PAGE_OFFICE_EMAIL:
                addQuestion(questionText(pageId, "What email should receive the quote request?"), officeEmail, true);
                addHelp("This RAMSIER'S office email is saved on this phone.");
                break;
            case PAGE_SLABS:
                addSlabStep();
                break;
            case PAGE_PRICE:
                addQuestion(questionText(pageId, "What is the installed price per square foot?"), pricePerSqFt, true);
                break;
            case PAGE_SINK_CHARGE:
                addSinkSelectionStep();
                break;
            case PAGE_EDGE_CHARGE:
                addQuestion(questionText(pageId, "What is the edge or extra labor charge?"), edgeCharge, true);
                break;
            case PAGE_TEAR_OUT:
                addQuestion(questionText(pageId, "What is the tear-out charge?"), tearOutCharge, true);
                break;
            case PAGE_OTHER_CHARGE:
                addQuestion(questionText(pageId, "Are there any other charges?"), otherCharge, true);
                break;
            case PAGE_COOKTOP_CUTOUT:
                addCooktopCutoutStep();
                break;
            case PAGE_EDGE_DETAIL:
                addEdgeDetailStep();
                break;
            case PAGE_FAUCET:
                addFaucetStep();
                break;
            case PAGE_BASKETS:
                addBasketsStep();
                break;
            case PAGE_GRIDS:
                addGridsStep();
                break;
            case PAGE_WATERFALL:
                addWaterfallStep();
                break;
            case PAGE_CABINETS:
                addCabinetsStep();
                break;
            case PAGE_BUY_CABINETS:
                addBuyCabinetsStep();
                break;
            case PAGE_PHOTO:
                addPhotoStep();
                break;
            case PAGE_SECTION_NAME:
                addQuestion(questionText(pageId, "What should this countertop section be called?"), sectionName, true);
                break;
            case PAGE_SECTION_LENGTH:
                addQuestion(questionText(pageId, "What is the section length in inches?"), lengthIn, true);
                break;
            case PAGE_SECTION_WIDTH:
                addQuestion(questionText(pageId, "What is the section width in inches?"), widthIn, true);
                break;
            case PAGE_SECTION_QUANTITY:
                addQuestion(questionText(pageId, "How many identical sections are there?"), quantity, true);
                addHelp("Tapping Next saves this countertop section.");
                break;
            case PAGE_ADD_SECTION:
                addAnotherSectionStep();
                break;
            case PAGE_STOVE_LENGTH:
                addQuestion(questionText(pageId, "What is the slide-in stove opening length?"), stoveLength, true);
                addHelp("Leave this blank if there is no slide-in stove opening.");
                break;
            case PAGE_STOVE_WIDTH:
                addQuestion(questionText(pageId, "What is the slide-in stove opening width?"), stoveWidth, true);
                addHelp("Leave this blank if there is no slide-in stove opening.");
                break;
            default:
                CustomPage customPage = customPageById(pageId);
                if (customPage != null) {
                    addQuestion(customPage.question, inputForCustomPage(customPage), true);
                } else {
                    addHelp("This page is not available.");
                }
                break;
        }
    }

    private void addCooktopCutoutStep() {
        hideKeyboard();
        page.addView(questionTitle(questionForEdit(PAGE_COOKTOP_CUTOUT)));
        addHelp(money(priceValue(PRICE_CUTOUT, 100)) + " for each cooktop or extra cutout.");
        addQuantityStepper(cooktopCutoutQuantity);
        addInlineNavigation();
    }

    private void addSinkSelectionStep() {
        hideKeyboard();
        page.addView(questionTitle(questionForEdit(PAGE_SINK_CHARGE)));
        addHelp("Enter how many of each sink the job needs. Use 0 for anything they do not want.");

        addSinkQuantityChoice("Equal double-bowl sink", R.drawable.sink_equal_double, equalDoubleSinkQuantity);
        addSinkQuantityChoice("Offset double-bowl sink", R.drawable.sink_offset_double, offsetDoubleSinkQuantity);
        addSinkQuantityChoice("Single-bowl sink", R.drawable.sink_single_bowl, singleBowlSinkQuantity);
        addVanitySinkQuantityChoice(
                "Rectangle bathroom vanity sink",
                R.drawable.vanity_sink_rectangle,
                whiteRectangleVanitySinkQuantity,
                biscuitRectangleVanitySinkQuantity,
                rectangleVanitySinkLocations);
        addVanitySinkQuantityChoice(
                "Oval bathroom vanity sink",
                R.drawable.vanity_sink_oval,
                whiteOvalVanitySinkQuantity,
                biscuitOvalVanitySinkQuantity,
                ovalVanitySinkLocations);
        addSinkTextQuantityChoice("Customer already has / will provide sink", anotherSinkQuantity);
        addSinkTextQuantityChoice("Undecided sink", undecidedSinkQuantity);
        detach(sinkCharge);
        page.addView(sinkCharge);
        addInlineNavigation();
    }

    private EditText quantityInput(String hint) {
        EditText field = input(hint, decimalInput());
        field.setText("0");
        return field;
    }

    private void addSinkQuantityChoice(String label, int imageResource, EditText quantityField) {
        page.addView(sectionHeader(label));
        addProductImage(imageResource, label);
        addQuantityStepper(quantityField);
    }

    private void addVanitySinkQuantityChoice(
            String label,
            int imageResource,
            EditText whiteQuantityField,
            EditText biscuitQuantityField,
            EditText locationField) {
        page.addView(sectionHeader(label));
        addProductImage(imageResource, label);
        addLabeledQuantityStepper("White", whiteQuantityField);
        addLabeledQuantityStepper("Biscuit", biscuitQuantityField);
        detach(locationField);
        page.addView(locationField);
    }

    private void addSinkTextQuantityChoice(String label, EditText quantityField) {
        page.addView(sectionHeader(label));
        addQuantityStepper(quantityField);
    }

    private void addLabeledQuantityStepper(String label, EditText quantityField) {
        TextView text = label(label);
        text.setTextSize(18);
        text.setTextColor(Color.rgb(96, 55, 38));
        text.setPadding(0, dp(12), 0, 0);
        page.addView(text);
        addQuantityStepper(quantityField);
    }

    private void addQuantityStepper(EditText quantityField) {
        detach(quantityField);
        quantityField.setGravity(Gravity.CENTER);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(4), 0, dp(12));

        Button down = secondaryButton("▼");
        down.setTextSize(22);
        down.setOnClickListener(v -> setQuantity(quantityField, Math.max(0, value(quantityField) - 1)));
        row.addView(down, new LinearLayout.LayoutParams(0, dp(58), 1f));

        row.addView(quantityField, new LinearLayout.LayoutParams(0, dp(58), 1f));

        Button up = primaryButton("▲");
        up.setTextSize(22);
        up.setOnClickListener(v -> setQuantity(quantityField, value(quantityField) + 1));
        row.addView(up, new LinearLayout.LayoutParams(0, dp(58), 1f));

        page.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void setQuantity(EditText field, double value) {
        if (value % 1 == 0) {
            field.setText(String.valueOf((int) value));
        } else {
            field.setText(number.format(value));
        }
    }

    private void addAddressStep() {
        page.addView(questionTitle(questionForEdit(PAGE_ADDRESS)));
        detach(projectAddress);
        page.addView(projectAddress);

        Button maps = secondaryButton("Open in Google Maps");
        maps.setOnClickListener(v -> openAddressInMaps());
        page.addView(maps);
        addInlineNavigation();

        projectAddress.requestFocus();
        projectAddress.postDelayed(() -> {
            scroll.smoothScrollTo(0, Math.max(0, projectAddress.getTop() - dp(24)));
            showKeyboard(projectAddress);
        }, 220);
    }

    private void openAddressInMaps() {
        String address = projectAddress.getText().toString().trim();
        if (address.isEmpty()) {
            Toast.makeText(this, "Enter the project address first.", Toast.LENGTH_SHORT).show();
            return;
        }
        hideKeyboard();
        Intent maps = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("geo:0,0?q=" + Uri.encode(address)));
        maps.setPackage("com.google.android.apps.maps");
        try {
            startActivity(maps);
        } catch (Exception ignored) {
            openWebPage("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(address));
        }
    }

    private void addImageChoice(RadioGroup group, String label, int imageResource) {
        RadioButton choice = radioButton(label, label);
        choice.setGravity(Gravity.CENTER);
        choice.setTextSize(17);
        choice.setPadding(dp(8), dp(12), dp(8), dp(16));

        Drawable image = getResources().getDrawable(imageResource);
        int imageWidth = dp(300);
        int imageHeight = dp(185);
        image.setBounds(0, 0, imageWidth, imageHeight);
        choice.setCompoundDrawables(null, image, null, null);
        choice.setCompoundDrawablePadding(dp(8));
        choice.setChecked(label.equals(sinkSelection));
        group.addView(choice, new RadioGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addEdgeDetailStep() {
        hideKeyboard();
        page.addView(questionTitle(questionForEdit(PAGE_EDGE_DETAIL)));
        double edgePrice = priceValue(PRICE_EDGE, 10);
        addHelp(edgeName("eased", "Eased and polished") + " is free. Every other edge is "
                + money(edgePrice) + " per linear foot.");

        RadioGroup choices = new RadioGroup(this);
        choices.setOrientation(RadioGroup.VERTICAL);
        String[] values = {
                "Eased and polished",
                "Small round",
                "Big round",
                "Bevel",
                "Big bevel"
        };
        for (String edgeValue : values) {
            boolean free = "Eased and polished".equals(edgeValue);
            String label = edgeDisplayName(edgeValue)
                    + (free ? " - Free" : " - " + money(edgePrice) + " per linear foot");
            RadioButton choice = radioButton(label, edgeValue);
            choice.setChecked(edgeValue.equals(edgeDetail));
            choices.addView(choice);
        }
        choices.setOnCheckedChangeListener((group, checkedId) -> {
            View selected = group.findViewById(checkedId);
            if (selected != null && selected.getTag() instanceof String) {
                edgeDetail = (String) selected.getTag();
            }
        });
        page.addView(choices);
        detach(edgeLinearFeet);
        page.addView(edgeLinearFeet);
        addInlineNavigation();
    }

    private void addFaucetStep() {
        hideKeyboard();
        page.addView(questionTitle(questionForEdit(PAGE_FAUCET)));
        addHelp("RAMSIER'S faucet: " + money(priceValue(PRICE_FAUCET, 225)) + ".");
        addQuantityStepper(faucetQuantity);
        addInlineNavigation();
    }

    private void addBasketsStep() {
        hideKeyboard();
        page.addView(questionTitle(questionForEdit(PAGE_BASKETS)));
        addProductImage(R.drawable.basket_drain, "Basket drain");
        addHelp("Basket drains are " + money(priceValue(PRICE_BASKET, 35)) + " each.");
        addQuantityStepper(basketQuantity);
        addInlineNavigation();
    }

    private void addGridsStep() {
        hideKeyboard();
        page.addView(questionTitle(questionForEdit(PAGE_GRIDS)));
        addProductImage(R.drawable.sink_grid, "Big sink grid");
        addHelp("Big grids are " + money(priceValue(PRICE_GRID, 70)) + " each.");
        addQuantityStepper(gridQuantity);
        addInlineNavigation();
    }

    private void addWaterfallStep() {
        hideKeyboard();
        page.addView(questionTitle(questionForEdit(PAGE_WATERFALL)));
        addHelp("Use 0 if they do not want a waterfall.");
        addQuantityStepper(waterfallQuantity);
        detach(waterfallComments);
        page.addView(waterfallComments);
        addInlineNavigation();
    }

    private void addProductImage(int imageResource, String description) {
        ImageView image = new ImageView(this);
        image.setImageResource(imageResource);
        image.setContentDescription(description);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setBackgroundColor(Color.WHITE);
        image.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(220));
        params.setMargins(0, dp(6), 0, dp(10));
        page.addView(image, params);
    }

    private void addCabinetsStep() {
        hideKeyboard();
        page.addView(questionTitle(questionForEdit(PAGE_CABINETS)));
        RadioGroup choices = yesNoGroup(cabinetsInYes);
        choices.setOnCheckedChangeListener((group, checkedId) ->
                cabinetsInYes = checkedYes(group, checkedId));
        page.addView(choices);
        detach(cabinetsApproximateDate);
        page.addView(cabinetsApproximateDate);
        addInlineNavigation();
    }

    private void addBuyCabinetsStep() {
        hideKeyboard();
        page.addView(questionTitle(questionForEdit(PAGE_BUY_CABINETS)));
        addHelp("Add cabinet notes only if needed.");
        detach(cabinetInterestComments);
        page.addView(cabinetInterestComments);
        addInlineNavigation();
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
        addInlineNavigation();
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
        page.addView(questionTitle(questionForEdit(PAGE_SLABS)));
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
        addInlineNavigation();
    }

    private void addAnotherSectionStep() {
        hideKeyboard();
        page.addView(questionTitle(questionForEdit(PAGE_ADD_SECTION)));
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
        addInlineNavigation();
    }

    private void addPhotoStep() {
        hideKeyboard();
        page.clearFocus();
        page.addView(questionTitle(questionForEdit(PAGE_PHOTO)));
        addHelp("The photo screen is separate from all typing screens, so the keyboard will not cover it.");

        Button photoButton = primaryButton("Choose kitchen or countertop photo");
        photoButton.setOnClickListener(v -> openPhotoPicker());
        page.addView(photoButton);

        detach(photoStatus);
        page.addView(photoStatus);
        detach(roomPhoto);
        page.addView(roomPhoto, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));

        page.addView(sectionHeader("Hand-drawn countertop plan"));
        addHelp("Take a clear, straight picture that shows every dimension and its unit.");
        Button drawingButton = primaryButton("Take picture of hand drawing");
        drawingButton.setOnClickListener(v -> takeDrawingPhoto());
        page.addView(drawingButton);

        Button drawingUploadButton = secondaryButton("Choose hand drawing photo from phone");
        drawingUploadButton.setOnClickListener(v -> openDrawingPhotoPicker());
        page.addView(drawingUploadButton);

        detach(drawingStatus);
        page.addView(drawingStatus);
        detach(drawingPhoto);
        page.addView(drawingPhoto, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));

        analyzeDrawingButton = primaryButton("Figure square footage with AI");
        analyzeDrawingButton.setEnabled(drawingPhotoUri != null);
        analyzeDrawingButton.setOnClickListener(v -> analyzeDrawing());
        page.addView(analyzeDrawingButton);
        addHelp("AI estimate only. Verify every dimension before using it for a price.");

        Button visualizer = secondaryButton("Open MSI room visualizer");
        visualizer.setOnClickListener(v -> openWebPage(MSI_VISUALIZER));
        page.addView(visualizer);
        addHelp("You may skip the photo and tap Next.");
        addInlineNavigation();
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

        TextView options = label(optionsSummary());
        options.setBackgroundColor(Color.WHITE);
        options.setPadding(dp(12), dp(12), dp(12), dp(12));
        page.addView(options);

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

        addInlineNavigation();

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

    private void addInlineNavigation() {
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

    private String nextButtonText() {
        if (stepIndex >= pageOrder.size()) {
            return "Send email";
        }
        return "Next";
    }

    private void handleNext() {
        int pageId = currentPageId();
        if (pageId == PAGE_NAME && customerName.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Enter the customer's name.", Toast.LENGTH_SHORT).show();
            customerName.requestFocus();
            return;
        }
        if (pageId == PAGE_OFFICE_EMAIL) {
            String email = officeEmail.getText().toString().trim();
            if (email.isEmpty() || !email.contains("@")) {
                Toast.makeText(this, "Enter the RAMSIER'S office email.", Toast.LENGTH_LONG).show();
                officeEmail.requestFocus();
                return;
            }
            prefs.edit().putString("office_email", email).apply();
        }
        if (pageId == PAGE_SECTION_LENGTH && value(lengthIn) <= 0) {
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
        if (pageId == PAGE_EDGE_DETAIL
                && !"Eased and polished".equals(edgeDetail)
                && value(edgeLinearFeet) <= 0) {
            Toast.makeText(this, "Enter the edge linear feet.", Toast.LENGTH_LONG).show();
            edgeLinearFeet.requestFocus();
            showKeyboard(edgeLinearFeet);
            return;
        }
        if (pageId >= CUSTOM_PAGE_START) {
            EditText field = customInputs.get(pageId);
            if (field != null) {
                prefs.edit().putString("custom_answer_" + pageId, field.getText().toString()).apply();
            }
        }
        if (stepIndex >= pageOrder.size()) {
            sendQuoteEmail();
            return;
        }

        hideKeyboard();
        stepIndex += 1;
        showStep();
    }

    private int currentPageId() {
        if (stepIndex >= 0 && stepIndex < pageOrder.size()) {
            return pageOrder.get(stepIndex);
        }
        return -1;
    }

    private int totalSteps() {
        return pageOrder.size() + 1;
    }

    private void showLiveEditScreen() {
        hideKeyboard();
        page.removeAllViews();
        navigation.removeAllViews();
        addBrandHeader();
        page.addView(questionTitle("Edit app"));
        addHelp("Changes save on this phone and appear immediately.");

        Button managePages = primaryButton("Add, remove, rename, or move pages");
        managePages.setOnClickListener(v -> showManagePagesScreen());
        page.addView(managePages);

        page.addView(sectionHeader("Prices"));
        EditText cutoutPrice = editorField(
                "Cooktop or extra cutout - price each",
                priceValue(PRICE_CUTOUT, 100));
        EditText edgePrice = editorField(
                "Paid edge details - price per linear foot",
                priceValue(PRICE_EDGE, 10));
        EditText faucetPrice = editorField(
                "RAMSIER'S faucet price",
                priceValue(PRICE_FAUCET, 225));
        EditText basketPrice = editorField(
                "Basket price each",
                priceValue(PRICE_BASKET, 35));
        EditText gridPrice = editorField(
                "Big grid price each",
                priceValue(PRICE_GRID, 70));

        page.addView(sectionHeader("Edge choice names"));
        EditText easedName = editorTextField(
                "Free edge choice",
                edgeName("eased", "Eased and polished"));
        EditText smallRoundName = editorTextField(
                "Small round choice",
                edgeName("small_round", "Small round"));
        EditText bigRoundName = editorTextField(
                "Big round choice",
                edgeName("big_round", "Big round"));
        EditText bevelName = editorTextField(
                "Bevel choice",
                edgeName("bevel", "Bevel"));
        EditText bigBevelName = editorTextField(
                "Big bevel choice",
                edgeName("big_bevel", "Big bevel"));

        Button save = primaryButton("Save");
        save.setOnClickListener(v -> {
            if (!saveLiveEditorValues(
                    cutoutPrice, edgePrice, faucetPrice, basketPrice, gridPrice,
                    easedName, smallRoundName, bigRoundName, bevelName, bigBevelName)) return;
            hideKeyboard();
            showStep();
            Toast.makeText(this, "App changes saved.", Toast.LENGTH_SHORT).show();
        });

        Button share = secondaryButton("Send");
        share.setOnClickListener(v -> {
            if (!saveLiveEditorValues(
                    cutoutPrice, edgePrice, faucetPrice, basketPrice, gridPrice,
                    easedName, smallRoundName, bigRoundName, bevelName, bigBevelName)) return;
            hideKeyboard();
            shareLiveEditorChanges();
        });

        Button back = secondaryButton("Back");
        back.setOnClickListener(v -> showStep());
        navigation.addView(back, new LinearLayout.LayoutParams(0, dp(54), 1f));

        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, dp(54), 1f);
        saveParams.setMargins(dp(6), 0, 0, 0);
        navigation.addView(save, saveParams);

        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(0, dp(54), 1f);
        shareParams.setMargins(dp(6), 0, 0, 0);
        navigation.addView(share, shareParams);
        scroll.post(() -> scroll.smoothScrollTo(0, 0));
    }

    private boolean saveLiveEditorValues(
            EditText cutoutPrice,
            EditText edgePrice,
            EditText faucetPrice,
            EditText basketPrice,
            EditText gridPrice,
            EditText easedName,
            EditText smallRoundName,
            EditText bigRoundName,
            EditText bevelName,
            EditText bigBevelName) {
        if (value(cutoutPrice) < 0
                || value(edgePrice) < 0
                || value(faucetPrice) < 0
                || value(basketPrice) < 0
                || value(gridPrice) < 0) {
            Toast.makeText(this, "Prices cannot be negative.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (isBlank(easedName)
                || isBlank(smallRoundName)
                || isBlank(bigRoundName)
                || isBlank(bevelName)
                || isBlank(bigBevelName)) {
            Toast.makeText(this, "Enter a name for every edge choice.", Toast.LENGTH_LONG).show();
            return false;
        }
        prefs.edit()
                .putString(PRICE_CUTOUT, cutoutPrice.getText().toString().trim())
                .putString(PRICE_EDGE, edgePrice.getText().toString().trim())
                .putString(PRICE_FAUCET, faucetPrice.getText().toString().trim())
                .putString(PRICE_BASKET, basketPrice.getText().toString().trim())
                .putString(PRICE_GRID, gridPrice.getText().toString().trim())
                .putString("edge_name_eased", easedName.getText().toString().trim())
                .putString("edge_name_small_round", smallRoundName.getText().toString().trim())
                .putString("edge_name_big_round", bigRoundName.getText().toString().trim())
                .putString("edge_name_bevel", bevelName.getText().toString().trim())
                .putString("edge_name_big_bevel", bigBevelName.getText().toString().trim())
                .apply();
        return true;
    }

    private void shareLiveEditorChanges() {
        StringBuilder settings = new StringBuilder();
        settings.append("Please save these RAMSIER'S app settings to GitHub repo ")
                .append("nadnad8974/countertoptop-APP2, build the next APK, ")
                .append("and upload it to my Google Drive.\n\n");
        settings.append("PRICES\n");
        settings.append("Cooktop or extra cutout each: ")
                .append(money(priceValue(PRICE_CUTOUT, 100))).append("\n");
        settings.append("Paid edge per linear foot: ")
                .append(money(priceValue(PRICE_EDGE, 10))).append("\n");
        settings.append("RAMSIER'S faucet: ")
                .append(money(priceValue(PRICE_FAUCET, 225))).append("\n");
        settings.append("Basket each: ")
                .append(money(priceValue(PRICE_BASKET, 35))).append("\n");
        settings.append("Big grid each: ")
                .append(money(priceValue(PRICE_GRID, 70))).append("\n\n");

        settings.append("EDGE CHOICE NAMES\n");
        settings.append("Free: ").append(edgeName("eased", "Eased and polished")).append("\n");
        settings.append("Small round: ").append(edgeName("small_round", "Small round")).append("\n");
        settings.append("Big round: ").append(edgeName("big_round", "Big round")).append("\n");
        settings.append("Bevel: ").append(edgeName("bevel", "Bevel")).append("\n");
        settings.append("Big bevel: ").append(edgeName("big_bevel", "Big bevel")).append("\n\n");

        settings.append("PAGE ORDER AND WORDING\n");
        for (int i = 0; i < pageOrder.size(); i++) {
            int pageId = pageOrder.get(i);
            settings.append(i + 1).append(". ")
                    .append(pageDisplayTitle(pageId))
                    .append(" | ")
                    .append(questionForEdit(pageId))
                    .append("\n");
        }

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, "RAMSIER'S app settings for GitHub");
        share.putExtra(Intent.EXTRA_TEXT, settings.toString());
        try {
            startActivity(Intent.createChooser(share, "Choose ChatGPT, then tap Send"));
        } catch (Exception e) {
            Toast.makeText(this, "A sharing app could not be opened.", Toast.LENGTH_LONG).show();
        }
    }

    private void showManagePagesScreen() {
        showManagePagesScreen(-1);
    }

    private void showManagePagesScreen(int keepIndexVisible) {
        hideKeyboard();
        page.removeAllViews();
        navigation.removeAllViews();
        addBrandHeader();
        page.addView(questionTitle("Manage question pages"));
        addHelp("Add a new page and name it, or add a deleted page back.");

        Button addNewPage = primaryButton("Add new page");
        addNewPage.setOnClickListener(v -> showCustomPageDialog());
        page.addView(addNewPage);

        Button restorePage = secondaryButton("Add deleted page back");
        restorePage.setOnClickListener(v -> showAddPageDialog());
        page.addView(restorePage);

        final View[] keepVisibleRow = new View[1];
        for (int i = 0; i < pageOrder.size(); i++) {
            final int index = i;
            final int pageId = pageOrder.get(i);
            LinearLayout row = itemRow();
            if (index == keepIndexVisible) {
                keepVisibleRow[0] = row;
            }
            row.setOrientation(LinearLayout.VERTICAL);
            TextView text = label((i + 1) + ". " + pageDisplayTitle(pageId));
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
            Button edit = miniButton("EDIT");
            edit.setOnClickListener(v -> showEditPageDialog(pageId));
            buttons.addView(edit, new LinearLayout.LayoutParams(0, dp(44), 1f));
            Button remove = miniButton("REMOVE");
            remove.setOnClickListener(v -> removePage(index));
            buttons.addView(remove, new LinearLayout.LayoutParams(0, dp(44), 1f));
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
            showManagePagesScreen();
        });
        page.addView(reset);
        scroll.post(() -> {
            if (keepVisibleRow[0] != null) {
                scroll.scrollTo(0, Math.max(0, keepVisibleRow[0].getTop() - dp(20)));
            } else {
                scroll.smoothScrollTo(0, 0);
            }
        });
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
        showManagePagesScreen(to);
    }

    private void removePage(int index) {
        if (index < 0 || index >= pageOrder.size()) return;
        pageOrder.remove(index);
        if (stepIndex >= pageOrder.size()) {
            stepIndex = Math.max(0, pageOrder.size() - 1);
        }
        savePageOrder();
        showManagePagesScreen();
    }

    private void showAddPageDialog() {
        ArrayList<Integer> availableIds = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        for (int pageId : allBuiltInPageIds()) {
            if (!pageOrder.contains(pageId)) {
                availableIds.add(pageId);
                labels.add("Add: " + pageDisplayTitle(pageId));
            }
        }
        for (CustomPage customPage : customPages) {
            if (!pageOrder.contains(customPage.id)) {
                availableIds.add(customPage.id);
                labels.add("Add: " + customPage.title);
            }
        }
        if (labels.isEmpty()) {
            Toast.makeText(this, "There are no deleted pages to add back.", Toast.LENGTH_LONG).show();
            return;
        }
        String[] items = labels.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Add deleted page back")
                .setItems(items, (dialog, which) -> {
                    int pageId = availableIds.get(which);
                    pageOrder.add(pageId);
                    savePageOrder();
                    showManagePagesScreen();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditPageDialog(int pageId) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(12), dp(8), dp(12), 0);

        EditText title = input("Short page name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        title.setText(pageDisplayTitle(pageId));
        form.addView(title);

        EditText question = input("Question wording", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        question.setMinLines(3);
        question.setGravity(Gravity.TOP);
        question.setText(questionForEdit(pageId));
        form.addView(question);

        new AlertDialog.Builder(this)
                .setTitle("Edit page")
                .setView(form)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newTitle = title.getText().toString().trim();
                    String newQuestion = question.getText().toString().trim();
                    if (newTitle.isEmpty() || newQuestion.isEmpty()) {
                        Toast.makeText(this, "Enter a page name and question.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (pageId >= CUSTOM_PAGE_START) {
                        CustomPage customPage = customPageById(pageId);
                        if (customPage != null) {
                            customPage.title = newTitle;
                            customPage.question = newQuestion;
                            saveCustomPages();
                        }
                    } else {
                        prefs.edit()
                                .putString("page_title_" + pageId, newTitle)
                                .putString("page_question_" + pageId, newQuestion)
                                .apply();
                    }
                    showManagePagesScreen();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCustomPageDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(12), dp(8), dp(12), 0);

        EditText title = input("Short page name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        form.addView(title);

        EditText question = input("Question wording", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        question.setMinLines(3);
        question.setGravity(Gravity.TOP);
        form.addView(question);

        new AlertDialog.Builder(this)
                .setTitle("Add new page")
                .setView(form)
                .setPositiveButton("Add", (dialog, which) -> {
                    String newTitle = title.getText().toString().trim();
                    String newQuestion = question.getText().toString().trim();
                    if (newTitle.isEmpty() || newQuestion.isEmpty()) {
                        Toast.makeText(this, "Enter a page name and question.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    int pageId = nextCustomPageId();
                    customPages.add(new CustomPage(pageId, newTitle, newQuestion));
                    pageOrder.add(pageId);
                    saveCustomPages();
                    savePageOrder();
                    showManagePagesScreen();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
        double cooktopPrice = priceValue(PRICE_CUTOUT, 100);
        double edgePrice = priceValue(PRICE_EDGE, 10);
        double faucetPrice = priceValue(PRICE_FAUCET, 225);
        double basketPrice = priceValue(PRICE_BASKET, 35);
        double gridPrice = priceValue(PRICE_GRID, 70);
        double cooktopCutoutTotal = value(cooktopCutoutQuantity) * cooktopPrice;
        double edgeDetailTotal = "Eased and polished".equals(edgeDetail)
                ? 0
                : value(edgeLinearFeet) * edgePrice;
        double faucetTotal = value(faucetQuantity) * faucetPrice;
        double basketTotal = value(basketQuantity) * basketPrice;
        double gridTotal = value(gridQuantity) * gridPrice;
        double total = net * value(pricePerSqFt)
                + value(sinkCharge)
                + value(edgeCharge)
                + value(tearOutCharge)
                + value(otherCharge)
                + cooktopCutoutTotal
                + edgeDetailTotal
                + faucetTotal
                + basketTotal
                + gridTotal;

        if (squareFootResult != null) squareFootResult.setText("Net square footage: " + number.format(net));
        if (totalResult != null) totalResult.setText("Estimated total: $" + number.format(total));

        if (showWarnings && gross <= 0) {
            Toast.makeText(this, "Add at least one countertop section first.", Toast.LENGTH_LONG).show();
        }
        return new Estimate(gross, stove, net, total);
    }

    private String optionsSummary() {
        double cooktopPrice = priceValue(PRICE_CUTOUT, 100);
        double edgePrice = priceValue(PRICE_EDGE, 10);
        double faucetPrice = priceValue(PRICE_FAUCET, 225);
        double basketPrice = priceValue(PRICE_BASKET, 35);
        double gridPrice = priceValue(PRICE_GRID, 70);
        double cutoutTotal = value(cooktopCutoutQuantity) * cooktopPrice;
        double edgeTotal = "Eased and polished".equals(edgeDetail) ? 0 : value(edgeLinearFeet) * edgePrice;
        double basketTotal = value(basketQuantity) * basketPrice;
        double gridTotal = value(gridQuantity) * gridPrice;
        return "Cooktop or extra cutouts: " + number.format(value(cooktopCutoutQuantity))
                + " × " + money(cooktopPrice) + " = " + money(cutoutTotal)
                + "\nEdge detail: " + edgeDisplayName(edgeDetail)
                + (!"Eased and polished".equals(edgeDetail)
                ? " - " + number.format(value(edgeLinearFeet)) + " ft × "
                + money(edgePrice) + " = " + money(edgeTotal)
                : " - Free")
                + "\nSink selection: " + sinkSelectionDisplay()
                + "\nRAMSIER'S faucet: " + number.format(value(faucetQuantity))
                + " × " + money(faucetPrice) + " = " + money(value(faucetQuantity) * faucetPrice)
                + "\nBasket drains: " + number.format(value(basketQuantity))
                + " × " + money(basketPrice) + " = " + money(basketTotal)
                + "\nBig grids: " + number.format(value(gridQuantity))
                + " × " + money(gridPrice) + " = " + money(gridTotal)
                + "\nWaterfall sides: " + number.format(value(waterfallQuantity))
                + "\nWaterfall comments: " + textOrNotProvided(waterfallComments)
                + "\nCabinets are in: " + yesNo(cabinetsInYes)
                + "\nApproximate cabinet date: " + textOrNotProvided(cabinetsApproximateDate)
                + "\nCabinet comments: " + textOrNotProvided(cabinetInterestComments)
                + drawingEstimateSummary();
    }

    private String sinkSelectionDisplay() {
        ArrayList<String> sinks = new ArrayList<>();
        addSinkLine(sinks, "Equal double-bowl sink", equalDoubleSinkQuantity, "");
        addSinkLine(sinks, "Offset double-bowl sink", offsetDoubleSinkQuantity, "");
        addSinkLine(sinks, "Single-bowl sink", singleBowlSinkQuantity, "");
        addSinkLine(sinks, "White rectangle bathroom vanity sink", whiteRectangleVanitySinkQuantity, "");
        addSinkLine(sinks, "Biscuit rectangle bathroom vanity sink", biscuitRectangleVanitySinkQuantity, "");
        addSinkLocationLine(sinks, "Rectangle sink locations", rectangleVanitySinkLocations);
        addSinkLine(sinks, "White oval bathroom vanity sink", whiteOvalVanitySinkQuantity, "");
        addSinkLine(sinks, "Biscuit oval bathroom vanity sink", biscuitOvalVanitySinkQuantity, "");
        addSinkLocationLine(sinks, "Oval sink locations", ovalVanitySinkLocations);
        addSinkLine(sinks, "Customer already has / will provide sink", anotherSinkQuantity, "");
        addSinkLine(sinks, "Undecided sink", undecidedSinkQuantity, "");
        if (sinks.isEmpty()) return "No sink selected";
        StringBuilder result = new StringBuilder();
        for (String sink : sinks) {
            if (result.length() > 0) result.append(", ");
            result.append(sink);
        }
        return result.toString();
    }

    private void addSinkLine(ArrayList<String> sinks, String label, EditText quantityField, String color) {
        double count = value(quantityField);
        if (count <= 0) return;
        String quantityText = count % 1 == 0
                ? String.valueOf((int) count)
                : number.format(count);
        String colorText = color == null || color.trim().isEmpty() ? "" : " - " + color;
        sinks.add(quantityText + " × " + label + colorText);
    }

    private void addSinkLocationLine(ArrayList<String> sinks, String label, EditText locationField) {
        String locations = textOrNotProvided(locationField);
        if ("Not provided".equals(locations)) return;
        sinks.add(label + ": " + locations);
    }

    private String drawingEstimateSummary() {
        if (aiDrawingExplanation.isEmpty()) return "";
        return "\nAI drawing estimate: " + number.format(aiDrawingSquareFeet) + " sq ft"
                + "\nAI confidence: " + aiDrawingConfidence
                + "\nAI calculation: " + aiDrawingExplanation
                + (aiDrawingMissingInformation.isEmpty()
                ? ""
                : "\nMissing drawing information: " + aiDrawingMissingInformation);
    }

    private void openPhotoPicker() {
        hideKeyboard();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    private void openDrawingPhotoPicker() {
        hideKeyboard();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_DRAWING_IMAGE);
    }

    private void takeDrawingPhoto() {
        hideKeyboard();
        try {
            File drawingDirectory = new File(getCacheDir(), "drawing_photos");
            if (!drawingDirectory.exists() && !drawingDirectory.mkdirs()) {
                throw new IllegalStateException("Could not create drawing photo folder");
            }
            File drawingFile = new File(
                    drawingDirectory,
                    "countertop-drawing-" + System.currentTimeMillis() + ".jpg");
            drawingPhotoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    drawingFile);
            Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            camera.putExtra(MediaStore.EXTRA_OUTPUT, drawingPhotoUri);
            camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            camera.setClipData(ClipData.newUri(getContentResolver(), "Hand drawing", drawingPhotoUri));
            grantCameraUriPermissions(camera, drawingPhotoUri);
            startActivityForResult(camera, TAKE_DRAWING_PHOTO);
        } catch (Exception e) {
            openDrawingCameraWithoutFileOutput();
        }
    }

    private void grantCameraUriPermissions(Intent camera, Uri uri) {
        List<ResolveInfo> cameras = getPackageManager().queryIntentActivities(camera, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo cameraApp : cameras) {
            grantUriPermission(
                    cameraApp.activityInfo.packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
    }

    private void openDrawingCameraWithoutFileOutput() {
        drawingPhotoUri = null;
        try {
            Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(camera, TAKE_DRAWING_PHOTO);
        } catch (Exception ignored) {
            Toast.makeText(this, "The camera could not be opened.", Toast.LENGTH_LONG).show();
        }
    }

    private void analyzeDrawing() {
        if (drawingPhotoUri == null) {
            Toast.makeText(this, "Take a picture of the hand drawing first.", Toast.LENGTH_LONG).show();
            return;
        }
        analyzeDrawingButton.setEnabled(false);
        drawingStatus.setText("Reading the drawing and calculating square footage...");

        new Thread(() -> {
            try {
                String image = drawingImageDataUrl();
                HttpURLConnection connection =
                        (HttpURLConnection) new URL(DRAWING_AI_ENDPOINT).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(120000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("X-Ramsiers-App", "countertop-quote-v1");

                JSONObject request = new JSONObject();
                request.put("image", image);
                byte[] requestBytes = request.toString().getBytes(StandardCharsets.UTF_8);
                connection.getOutputStream().write(requestBytes);

                int responseCode = connection.getResponseCode();
                InputStream responseStream = responseCode >= 200 && responseCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();
                JSONObject response = new JSONObject(readText(responseStream));
                if (responseCode < 200 || responseCode >= 300) {
                    throw new IllegalStateException(
                            response.optString("error", "The drawing could not be analyzed."));
                }

                double squareFeet = response.optDouble("square_feet", 0);
                String confidence = response.optString("confidence", "low");
                String explanation = response.optString("explanation", "");
                String missingInformation = response.optString("missing_information", "");
                runOnUiThread(() -> showDrawingResult(
                        squareFeet,
                        confidence,
                        explanation,
                        missingInformation));
            } catch (Exception e) {
                String message = e.getMessage() == null
                        ? "The drawing could not be analyzed. Please try again."
                        : e.getMessage();
                runOnUiThread(() -> {
                    drawingStatus.setText(message);
                    analyzeDrawingButton.setEnabled(true);
                });
            }
        }).start();
    }

    private String drawingImageDataUrl() throws Exception {
        Bitmap original;
        try (InputStream input = getContentResolver().openInputStream(drawingPhotoUri)) {
            original = BitmapFactory.decodeStream(input);
        }
        if (original == null) throw new IllegalStateException("The drawing photo could not be read.");

        int width = original.getWidth();
        int height = original.getHeight();
        int largest = Math.max(width, height);
        Bitmap upload = original;
        if (largest > 1600) {
            double scale = 1600.0 / largest;
            upload = Bitmap.createScaledBitmap(
                    original,
                    Math.max(1, (int) Math.round(width * scale)),
                    Math.max(1, (int) Math.round(height * scale)),
                    true);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        upload.compress(Bitmap.CompressFormat.JPEG, 86, output);
        if (upload != original) upload.recycle();
        original.recycle();
        return "data:image/jpeg;base64,"
                + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
    }

    private String readText(InputStream input) throws Exception {
        if (input == null) return "{}";
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private void showDrawingResult(
            double squareFeet,
            String confidence,
            String explanation,
            String missingInformation) {
        aiDrawingSquareFeet = Math.max(0, squareFeet);
        aiDrawingConfidence = confidence;
        aiDrawingExplanation = explanation;
        aiDrawingMissingInformation = missingInformation;
        String result = "AI estimate: " + number.format(aiDrawingSquareFeet) + " sq ft"
                + "\nConfidence: " + confidence
                + "\n" + explanation;
        if (!missingInformation.isEmpty()) {
            result += "\nMissing information: " + missingInformation;
        }
        result += "\nVerify all dimensions before pricing.";
        drawingStatus.setText(result);
        analyzeDrawingButton.setEnabled(true);
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
        if (requestCode == PICK_DRAWING_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            drawingPhotoUri = data.getData();
            try {
                int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(drawingPhotoUri, flags);
            } catch (Exception ignored) {
            }
            drawingPhoto.setImageURI(drawingPhotoUri);
            drawingStatus.setText("Drawing photo ready. Tap the AI button below.");
            if (analyzeDrawingButton != null) analyzeDrawingButton.setEnabled(true);
        }
        if (requestCode == TAKE_DRAWING_PHOTO) {
            if (resultCode == RESULT_OK && drawingPhotoUri != null) {
                drawingPhoto.setImageURI(drawingPhotoUri);
                drawingStatus.setText("Drawing photo ready. Tap the AI button below.");
                if (analyzeDrawingButton != null) analyzeDrawingButton.setEnabled(true);
            } else if (resultCode == RESULT_OK && data != null && data.getExtras() != null
                    && data.getExtras().get("data") instanceof Bitmap) {
                drawingPhotoUri = saveDrawingBitmap((Bitmap) data.getExtras().get("data"));
                if (drawingPhotoUri != null) {
                    drawingPhoto.setImageURI(drawingPhotoUri);
                    drawingStatus.setText("Drawing photo ready. Tap the AI button below.");
                    if (analyzeDrawingButton != null) analyzeDrawingButton.setEnabled(true);
                } else {
                    drawingStatus.setText("The drawing photo could not be saved.");
                    if (analyzeDrawingButton != null) analyzeDrawingButton.setEnabled(false);
                }
            } else {
                drawingPhotoUri = null;
                drawingStatus.setText("No hand drawing photo selected.");
                if (analyzeDrawingButton != null) analyzeDrawingButton.setEnabled(false);
            }
        }
    }

    private Uri saveDrawingBitmap(Bitmap bitmap) {
        try {
            File drawingDirectory = new File(getCacheDir(), "drawing_photos");
            if (!drawingDirectory.exists() && !drawingDirectory.mkdirs()) return null;
            File drawingFile = new File(
                    drawingDirectory,
                    "countertop-drawing-" + System.currentTimeMillis() + ".jpg");
            try (java.io.FileOutputStream output = new java.io.FileOutputStream(drawingFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output);
            }
            return FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    drawingFile);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void sendQuoteEmail() {
        String to = officeEmail.getText().toString().trim();
        if (!to.isEmpty() && to.contains("@")) {
            prefs.edit().putString("office_email", to).apply();
        }
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
        body.append("\nOPTIONS AND ACCESSORIES\n");
        body.append(optionsSummary()).append("\n");
        body.append("Estimated total: $").append(number.format(estimate.total)).append("\n");
        body.append("This is an estimate and needs final verification by RAMSIER'S.\n\n");
        if (!customPages.isEmpty()) {
            body.append("CUSTOM QUESTIONS\n");
            for (CustomPage customPage : customPages) {
                if (pageOrder.contains(customPage.id)) {
                    EditText field = customInputs.get(customPage.id);
                    String answer = field == null
                            ? prefs.getString("custom_answer_" + customPage.id, "")
                            : field.getText().toString();
                    body.append(customPage.question).append("\n");
                    body.append(answer.trim().isEmpty() ? "Not provided" : answer.trim()).append("\n\n");
                }
            }
        }
        body.append("PROJECT NOTES\n").append(text(projectNotes)).append("\n");

        Intent email = new Intent(Intent.ACTION_SEND);
        email.setType(selectedPhotoUri == null ? "text/plain" : "image/*");
        if (!to.isEmpty() && to.contains("@")) {
            email.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
        }
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
        drawingPhotoUri = null;
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
        cooktopCutoutQuantity.setText("0");
        edgeLinearFeet.setText("0");
        faucetQuantity.setText("0");
        basketQuantity.setText("0");
        gridQuantity.setText("0");
        waterfallQuantity.setText("0");
        waterfallComments.setText("");
        cabinetsApproximateDate.setText("");
        cabinetInterestComments.setText("");
        cooktopCutoutYes = false;
        basketsYes = false;
        gridsYes = false;
        cabinetsInYes = false;
        wantsToBuyCabinets = false;
        edgeDetail = "Eased and polished";
        sinkSelection = "Not selected";
        equalDoubleSinkQuantity.setText("0");
        offsetDoubleSinkQuantity.setText("0");
        singleBowlSinkQuantity.setText("0");
        whiteRectangleVanitySinkQuantity.setText("0");
        biscuitRectangleVanitySinkQuantity.setText("0");
        rectangleVanitySinkLocations.setText("");
        whiteOvalVanitySinkQuantity.setText("0");
        biscuitOvalVanitySinkQuantity.setText("0");
        ovalVanitySinkLocations.setText("");
        anotherSinkQuantity.setText("0");
        undecidedSinkQuantity.setText("0");
        aiDrawingSquareFeet = 0;
        aiDrawingConfidence = "";
        aiDrawingExplanation = "";
        aiDrawingMissingInformation = "";
        roomPhoto.setImageDrawable(null);
        drawingPhoto.setImageDrawable(null);
        photoStatus.setText("No photo selected.");
        drawingStatus.setText("No hand drawing photo selected.");
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
        return "https://www.msisurfaces.com/site-search/?key=" + Uri.encode(cleanMsiSearch(query)) + "&ctgy=slab";
    }

    private String cleanMsiSearch(String value) {
        return value.replace("MSI:", "").replace("...", "").trim();
    }

    private String pageTitle(int pageId) {
        switch (pageId) {
            case PAGE_NAME: return "Customer name";
            case PAGE_PHONE: return "Customer phone number";
            case PAGE_EMAIL: return "Customer email";
            case PAGE_ADDRESS: return "Project address";
            case PAGE_NOTES: return "Project notes";
            case PAGE_OFFICE_EMAIL: return "RAMSIER'S office email";
            case PAGE_SLABS: return "Slab choices";
            case PAGE_PRICE: return "Installed price";
            case PAGE_SINK_CHARGE: return "Sink selection";
            case PAGE_EDGE_CHARGE: return "Edge or extra labor charge";
            case PAGE_TEAR_OUT: return "Tear-out charge";
            case PAGE_OTHER_CHARGE: return "Other charges";
            case PAGE_PHOTO: return "Countertop photo";
            case PAGE_COOKTOP_CUTOUT: return "Cooktop or extra cutouts";
            case PAGE_EDGE_DETAIL: return "Edge detail";
            case PAGE_FAUCET: return "RAMSIER'S faucet";
            case PAGE_BASKETS: return "Basket drains";
            case PAGE_GRIDS: return "Big grids";
            case PAGE_WATERFALL: return "Waterfall";
            case PAGE_CABINETS: return "Cabinet status";
            case PAGE_BUY_CABINETS: return "Buy cabinets";
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

    private String pageDisplayTitle(int pageId) {
        if (pageId >= CUSTOM_PAGE_START) {
            CustomPage customPage = customPageById(pageId);
            return customPage == null ? "Custom question" : customPage.title;
        }
        return prefs.getString("page_title_" + pageId, pageTitle(pageId));
    }

    private String questionText(int pageId, String defaultQuestion) {
        return prefs.getString("page_question_" + pageId, defaultQuestion);
    }

    private String questionForEdit(int pageId) {
        if (pageId >= CUSTOM_PAGE_START) {
            CustomPage customPage = customPageById(pageId);
            return customPage == null ? "" : customPage.question;
        }
        switch (pageId) {
            case PAGE_NAME: return questionText(pageId, "What is the customer's name?");
            case PAGE_PHONE: return questionText(pageId, "What is the customer's phone number?");
            case PAGE_EMAIL: return questionText(pageId, "What is the customer's email address?");
            case PAGE_ADDRESS: return questionText(pageId, "What is the project address?");
            case PAGE_NOTES: return questionText(pageId, "Are there any project notes?");
            case PAGE_OFFICE_EMAIL: return questionText(pageId, "What email should receive the quote request?");
            case PAGE_PRICE: return questionText(pageId, "What is the installed price per square foot?");
            case PAGE_SINK_CHARGE: return questionText(pageId, "Which sinks would you like?");
            case PAGE_EDGE_CHARGE: return questionText(pageId, "What is the edge or extra labor charge?");
            case PAGE_TEAR_OUT: return questionText(pageId, "What is the tear-out charge?");
            case PAGE_OTHER_CHARGE: return questionText(pageId, "Are there any other charges?");
            case PAGE_COOKTOP_CUTOUT: return questionText(pageId, "Is a cooktop or extra cutout needed?");
            case PAGE_EDGE_DETAIL: return questionText(pageId, "Which edge detail would you like?");
            case PAGE_FAUCET: return questionText(pageId, "Would you like a RAMSIER'S faucet?");
            case PAGE_BASKETS: return questionText(pageId, "Would you like basket drains?");
            case PAGE_GRIDS: return questionText(pageId, "Would you like big grids?");
            case PAGE_WATERFALL: return questionText(pageId, "Do they want a waterfall?");
            case PAGE_CABINETS: return questionText(pageId, "Are the cabinets in?");
            case PAGE_BUY_CABINETS: return questionText(pageId, "Any cabinet comments?");
            case PAGE_SECTION_NAME: return questionText(pageId, "What should this countertop section be called?");
            case PAGE_SECTION_LENGTH: return questionText(pageId, "What is the section length in inches?");
            case PAGE_SECTION_WIDTH: return questionText(pageId, "What is the section width in inches?");
            case PAGE_SECTION_QUANTITY: return questionText(pageId, "How many identical sections are there?");
            case PAGE_STOVE_LENGTH: return questionText(pageId, "What is the slide-in stove opening length?");
            case PAGE_STOVE_WIDTH: return questionText(pageId, "What is the slide-in stove opening width?");
            case PAGE_SLABS: return "Which slabs does the customer like?";
            case PAGE_PHOTO: return "Would you like to add a countertop photo?";
            case PAGE_ADD_SECTION: return "Would you like to add another countertop section?";
            default: return pageTitle(pageId);
        }
    }

    private EditText inputForCustomPage(CustomPage customPage) {
        EditText field = customInputs.get(customPage.id);
        if (field == null) {
            field = input("Type answer", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            field.setMinLines(3);
            field.setGravity(Gravity.TOP);
            field.setText(prefs.getString("custom_answer_" + customPage.id, ""));
            customInputs.put(customPage.id, field);
        }
        return field;
    }

    private CustomPage customPageById(int pageId) {
        for (CustomPage customPage : customPages) {
            if (customPage.id == pageId) {
                return customPage;
            }
        }
        return null;
    }

    private ArrayList<Integer> allBuiltInPageIds() {
        ArrayList<Integer> ids = new ArrayList<>();
        for (String piece : ALL_BUILT_IN_PAGES.split(",")) {
            try {
                int pageId = Integer.parseInt(piece.trim());
                if (isValidBuiltInPageId(pageId) && !ids.contains(pageId)) {
                    ids.add(pageId);
                }
            } catch (Exception ignored) {
            }
        }
        return ids;
    }

    private int nextCustomPageId() {
        int pageId = prefs.getInt("next_custom_page_id", CUSTOM_PAGE_START);
        while (customPageById(pageId) != null || pageOrder.contains(pageId)) {
            pageId++;
        }
        prefs.edit().putInt("next_custom_page_id", pageId + 1).apply();
        return pageId;
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
        addNewPricingPagesOnce();
        addCabinetSalesPageOnce();
        applyV120PageChangesOnce();
        applyV121PageChangesOnce();
        applyV122PageChangesOnce();
        applyV127PageChangesOnce();
    }

    private void addNewPricingPagesOnce() {
        if (prefs.getBoolean("v1_14_pricing_pages_added", false)) return;
        int after = pageOrder.indexOf(PAGE_SINK_CHARGE);
        int insertAt = after >= 0 ? after + 1 : pageOrder.size();
        int[] newPages = {
                PAGE_COOKTOP_CUTOUT,
                PAGE_EDGE_DETAIL,
                PAGE_FAUCET,
                PAGE_BASKETS,
                PAGE_GRIDS,
                PAGE_WATERFALL,
                PAGE_CABINETS
        };
        for (int pageId : newPages) {
            if (!pageOrder.contains(pageId)) {
                pageOrder.add(insertAt, pageId);
                insertAt++;
            }
        }
        savePageOrder();
        prefs.edit().putBoolean("v1_14_pricing_pages_added", true).apply();
    }

    private void addCabinetSalesPageOnce() {
        if (prefs.getBoolean("v1_19_cabinet_sales_page_added", false)) return;
        if (!pageOrder.contains(PAGE_BUY_CABINETS)) {
            int cabinetStatus = pageOrder.indexOf(PAGE_CABINETS);
            int insertAt = cabinetStatus >= 0 ? cabinetStatus + 1 : pageOrder.size();
            pageOrder.add(insertAt, PAGE_BUY_CABINETS);
            savePageOrder();
        }
        prefs.edit().putBoolean("v1_19_cabinet_sales_page_added", true).apply();
    }

    private void applyV120PageChangesOnce() {
        if (prefs.getBoolean("v1_20_page_changes_applied", false)) return;
        pageOrder.remove(Integer.valueOf(PAGE_OFFICE_EMAIL));
        pageOrder.remove(Integer.valueOf(PAGE_PRICE));
        pageOrder.remove(Integer.valueOf(PAGE_NOTES));
        pageOrder.add(PAGE_NOTES);
        savePageOrder();
        prefs.edit().putBoolean("v1_20_page_changes_applied", true).apply();
    }

    private void applyV121PageChangesOnce() {
        if (prefs.getBoolean("v1_21_page_changes_applied", false)) return;
        pageOrder.remove(Integer.valueOf(PAGE_EDGE_CHARGE));
        pageOrder.remove(Integer.valueOf(PAGE_TEAR_OUT));
        savePageOrder();
        prefs.edit().putBoolean("v1_21_page_changes_applied", true).apply();
    }

    private void applyV122PageChangesOnce() {
        if (prefs.getBoolean("v1_22_page_changes_applied", false)) return;
        movePageBefore(PAGE_SLABS, PAGE_NOTES);
        movePageBefore(PAGE_EDGE_DETAIL, PAGE_NOTES);
        savePageOrder();
        prefs.edit().putBoolean("v1_22_page_changes_applied", true).apply();
    }

    private void applyV127PageChangesOnce() {
        if (prefs.getBoolean("v1_27_page_changes_applied", false)) return;
        if (!pageOrder.contains(PAGE_WATERFALL)) {
            int gridPage = pageOrder.indexOf(PAGE_GRIDS);
            int insertAt = gridPage >= 0 ? gridPage + 1 : pageOrder.size();
            pageOrder.add(insertAt, PAGE_WATERFALL);
        }
        movePageBefore(PAGE_EDGE_DETAIL, PAGE_SLABS);
        savePageOrder();
        prefs.edit().putBoolean("v1_27_page_changes_applied", true).apply();
    }

    private void movePageBefore(int pageId, int beforePageId) {
        pageOrder.remove(Integer.valueOf(pageId));
        int beforeIndex = pageOrder.indexOf(beforePageId);
        if (beforeIndex < 0) {
            pageOrder.add(pageId);
        } else {
            pageOrder.add(beforeIndex, pageId);
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

    private void loadCustomPages() {
        customPages.clear();
        customInputs.clear();
        try {
            JSONArray pageArray = new JSONArray(prefs.getString("custom_pages", "[]"));
            for (int i = 0; i < pageArray.length(); i++) {
                JSONObject object = pageArray.getJSONObject(i);
                int id = object.optInt("id", -1);
                String title = object.optString("title");
                String question = object.optString("question");
                if (id >= CUSTOM_PAGE_START && !title.trim().isEmpty() && !question.trim().isEmpty()) {
                    customPages.add(new CustomPage(id, title, question));
                }
            }
        } catch (Exception ignored) {
            customPages.clear();
        }
    }

    private void saveCustomPages() {
        try {
            JSONArray pageArray = new JSONArray();
            for (CustomPage customPage : customPages) {
                JSONObject object = new JSONObject();
                object.put("id", customPage.id);
                object.put("title", customPage.title);
                object.put("question", customPage.question);
                pageArray.put(object);
            }
            prefs.edit().putString("custom_pages", pageArray.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private boolean isValidPageId(int pageId) {
        return isValidBuiltInPageId(pageId) || customPageById(pageId) != null;
    }

    private boolean isValidBuiltInPageId(int pageId) {
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
                || pageId == PAGE_COOKTOP_CUTOUT
                || pageId == PAGE_EDGE_DETAIL
                || pageId == PAGE_FAUCET
                || pageId == PAGE_BASKETS
                || pageId == PAGE_GRIDS
                || pageId == PAGE_WATERFALL
                || pageId == PAGE_CABINETS
                || pageId == PAGE_BUY_CABINETS
                || pageId == PAGE_SECTION_NAME
                || pageId == PAGE_SECTION_LENGTH
                || pageId == PAGE_SECTION_WIDTH
                || pageId == PAGE_SECTION_QUANTITY
                || pageId == PAGE_ADD_SECTION
                || pageId == PAGE_STOVE_LENGTH
                || pageId == PAGE_STOVE_WIDTH;
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

    private double priceValue(String key, double defaultValue) {
        try {
            return Double.parseDouble(prefs.getString(key, number.format(defaultValue)));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String money(double amount) {
        return "$" + number.format(amount);
    }

    private String edgeName(String key, String defaultName) {
        String saved = prefs.getString("edge_name_" + key, defaultName).trim();
        return saved.isEmpty() ? defaultName : saved;
    }

    private String edgeDisplayName(String value) {
        if ("Small round".equals(value)) return edgeName("small_round", "Small round");
        if ("Big round".equals(value)) return edgeName("big_round", "Big round");
        if ("Bevel".equals(value)) return edgeName("bevel", "Bevel");
        if ("Big bevel".equals(value)) return edgeName("big_bevel", "Big bevel");
        return edgeName("eased", "Eased and polished");
    }

    private EditText editorField(String labelText, double currentValue) {
        addHelp(labelText);
        EditText field = input("Enter price", decimalInput());
        field.setText(number.format(currentValue));
        page.addView(field);
        return field;
    }

    private EditText editorTextField(String labelText, String currentValue) {
        addHelp(labelText);
        EditText field = input("Enter choice name",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        field.setText(currentValue);
        page.addView(field);
        return field;
    }

    private boolean isBlank(EditText field) {
        return field.getText().toString().trim().isEmpty();
    }

    private String textOrNotProvided(EditText editText) {
        String result = editText.getText().toString().trim();
        return result.isEmpty() ? "Not provided" : result;
    }

    private String yesNo(boolean selected) {
        return selected ? "Yes" : "No";
    }

    private RadioGroup yesNoGroup(boolean yesSelected) {
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.HORIZONTAL);
        group.setGravity(Gravity.CENTER);

        RadioButton yes = radioButton("Yes", Boolean.TRUE);
        RadioButton no = radioButton("No", Boolean.FALSE);
        group.addView(yes, new RadioGroup.LayoutParams(0, dp(52), 1f));
        group.addView(no, new RadioGroup.LayoutParams(0, dp(52), 1f));
        if (yesSelected) {
            yes.setChecked(true);
        } else {
            no.setChecked(true);
        }
        return group;
    }

    private RadioButton radioButton(String text, Object value) {
        RadioButton button = new RadioButton(this);
        button.setId(View.generateViewId());
        button.setText(text);
        button.setTextSize(17);
        button.setTag(value);
        button.setPadding(dp(8), dp(4), dp(8), dp(4));
        return button;
    }

    private boolean checkedYes(RadioGroup group, int checkedId) {
        View selected = group.findViewById(checkedId);
        return selected != null && Boolean.TRUE.equals(selected.getTag());
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

    private static class CustomPage {
        final int id;
        String title;
        String question;

        CustomPage(int id, String title, String question) {
            this.id = id;
            this.title = title == null || title.trim().isEmpty() ? "Custom question" : title;
            this.question = question == null || question.trim().isEmpty() ? this.title : question;
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
