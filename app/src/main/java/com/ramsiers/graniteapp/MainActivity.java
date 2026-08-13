package com.ramsiers.graniteapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.Manifest;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.print.PrintManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.ramsiers.graniteapp.drawing.DrawingApiRequest;
import com.ramsiers.graniteapp.drawing.DrawingMath;
import com.ramsiers.graniteapp.drawing.DrawingRecord;
import com.ramsiers.graniteapp.drawing.DrawingRules;
import com.ramsiers.graniteapp.drawing.DrawingState;
import com.ramsiers.graniteapp.drawing.QuoteSquareFootMath;
import com.ramsiers.graniteapp.print.PdfFilePrintAdapter;
import com.ramsiers.graniteapp.print.QuotePdfGenerator;

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
    private static final int CAMERA_PERMISSION = 904;
    private static final int TAKE_COUNTERTOP_PHOTO = 905;
    private static final int CAMERA_CAPTURE_COUNTERTOP = 1;
    private static final int CAMERA_CAPTURE_DRAWING = 2;
    private static final int MAX_DRAWING_PHOTOS = 6;
    private static final String STATE_DRAWING_URIS = "drawing_uris";
    private static final String STATE_ACTIVE_DRAWING_INDEX = "active_drawing_index";
    private static final String STATE_PENDING_DRAWING_URI = "pending_drawing_uri";
    private static final String STATE_DRAWING_INPUT_REVISION = "drawing_input_revision";
    private static final String STATE_PENDING_CAMERA_CAPTURE = "pending_camera_capture";
    private static final String STATE_STEP_INDEX = "step_index";
    private static final String STATE_DRAWING_JSON = "drawing_state_json";
    private static final String PREFS = "ramsiers_granite_app";
    private static final String PREF_DRAWING_STATE = "drawing_state_v1";
    private static final String DRAWING_AI_ENDPOINT =
            "https://ramsiers-drawing-ai.nadnad8974.chatgpt.site/api/analyze";
    private static final String PRICE_CUTOUT = "price_cutout";
    private static final String PRICE_EDGE = "price_edge";
    private static final String PRICE_FAUCET = "price_faucet";
    private static final String PRICE_BASKET = "price_basket";
    private static final String PRICE_GRID = "price_grid";
    private static final double WATERFALL_PRICE = 300.0;
    private static final String DEFAULT_PAGE_ORDER = "0,1,2,3,12,8,13,15,16,17,20,14,18,19,11,6,4";
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
    private final ArrayList<Uri> countertopPhotoUris = new ArrayList<>();
    private final ArrayList<Uri> drawingPhotoUris = new ArrayList<>();
    private final ArrayList<DrawingRecord> drawingRecords = new ArrayList<>();
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
    private LinearLayout manualMeasurementList;

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
    private EditText cabinetInQuantity;
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
    private boolean wantsToBuyCabinets;
    private String edgeDetail = "Eased and polished";
    private String sinkSelection = "Not selected";
    private double aiDrawingSquareFeet;
    private String aiDrawingConfidence = "";
    private String aiDrawingExplanation = "";
    private String aiDrawingMissingInformation = "";
    private String aiDrawingLastError = "";
    private boolean aiDrawingCanCalculate;
    private boolean aiDrawingEditedByUser;
    private JSONObject aiVerificationDrawing;
    private JSONArray aiDrawingCalculationParts;
    private boolean drawingAnalysisInProgress;
    private volatile int drawingAnalysisRequestId;
    private long drawingAnalysisStartedAt;
    private int drawingProgressFloor;

    private TextView squareFootResult;
    private TextView totalResult;
    private TextView photoStatus;
    private TextView drawingStatus;
    private TextView drawingProgressText;
    private TextView drawingVerificationStatus;
    private TextView manualMeasurementTotal;
    private ImageView roomPhoto;
    private ImageView drawingPhoto;
    private ProgressBar drawingProgressBar;
    private VerificationDrawingView verificationDrawingView;
    private EditText pendingManualLength;
    private EditText pendingManualWidth;
    private Uri selectedPhotoUri;
    private Uri pendingDrawingCaptureUri;
    private Uri displayedDrawingPreviewUri;
    private int stepIndex = 0;
    private int photoAccordionOpen = 0;
    private int activeDrawingIndex;
    private int drawingInputRevision;
    private int aiResultRevision = -1;
    private boolean renderingManualMeasurements;
    private boolean manualPendingCommitted;
    private int pendingCameraCapture = 0;
    private final Handler addressHandler = new Handler(Looper.getMainLooper());
    private final Handler drawingProgressHandler = new Handler(Looper.getMainLooper());
    private Runnable addressLookupRunnable;
    private Runnable drawingProgressRunnable;
    private final Object drawingConnectionLock = new Object();
    private HttpURLConnection activeDrawingConnection;
    private Dialog drawingZoomDialog;
    private Dialog drawingEditDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadSavedLists();
        loadCustomPages();
        loadPageOrder();
        restoreDrawingState(savedInstanceState);
        buildUi();
        if (!drawingPhotoUris.isEmpty()) {
            if (hasActiveDrawingResult()) {
                updateDrawingStatusText();
            } else {
                drawingStatus.setText(drawingPhotoUris.size() + " drawing"
                        + (drawingPhotoUris.size() == 1 ? " is" : "s are")
                        + " ready. New drawings are analyzed automatically.");
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        String drawingStateJson = currentDrawingState().toJson();
        outState.putString(STATE_DRAWING_JSON, drawingStateJson);
        prefs.edit().putString(PREF_DRAWING_STATE, drawingStateJson).apply();
        ArrayList<String> drawingUriStrings = new ArrayList<>();
        for (Uri uri : drawingPhotoUris) {
            if (uri != null) drawingUriStrings.add(uri.toString());
        }
        outState.putStringArrayList(STATE_DRAWING_URIS, drawingUriStrings);
        outState.putInt(STATE_ACTIVE_DRAWING_INDEX, activeDrawingIndex);
        outState.putString(
                STATE_PENDING_DRAWING_URI,
                pendingDrawingCaptureUri == null
                        ? null
                        : pendingDrawingCaptureUri.toString());
        outState.putInt(STATE_DRAWING_INPUT_REVISION, drawingInputRevision);
        outState.putInt(STATE_PENDING_CAMERA_CAPTURE, pendingCameraCapture);
        outState.putInt(STATE_STEP_INDEX, stepIndex);
        super.onSaveInstanceState(outState);
    }

    private void restoreDrawingState(Bundle state) {
        DrawingState restored = DrawingState.fromJson(
                state != null && state.containsKey(STATE_DRAWING_JSON)
                        ? state.getString(STATE_DRAWING_JSON)
                        : prefs.getString(PREF_DRAWING_STATE, ""));
        drawingPhotoUris.clear();
        drawingRecords.clear();
        for (DrawingRecord drawing : restored.drawings) {
            Uri uri = Uri.parse(drawing.uri);
            if (!drawingPhotoUris.contains(uri)) drawingPhotoUris.add(uri);
            if (drawingRecords.size() < drawingPhotoUris.size()) drawingRecords.add(drawing);
        }
        activeDrawingIndex = restored.activeDrawingIndex;
        drawingInputRevision = restored.inputRevision;
        loadActiveDrawingResult(false);

        if (state == null) {
            pendingDrawingCaptureUri = null;
            pendingCameraCapture = 0;
            stepIndex = 0;
            displayedDrawingPreviewUri = null;
            return;
        }
        String pendingUri = state.getString(STATE_PENDING_DRAWING_URI);
        pendingDrawingCaptureUri = pendingUri == null || pendingUri.isEmpty()
                ? null
                : Uri.parse(pendingUri);
        int savedCaptureType = state.getInt(STATE_PENDING_CAMERA_CAPTURE, 0);
        pendingCameraCapture = savedCaptureType == CAMERA_CAPTURE_DRAWING
                || savedCaptureType == CAMERA_CAPTURE_COUNTERTOP
                ? savedCaptureType
                : 0;
        stepIndex = Math.max(
                0,
                Math.min(state.getInt(STATE_STEP_INDEX, 0), pageOrder.size()));
        displayedDrawingPreviewUri = null;
    }

    private DrawingState currentDrawingState() {
        syncActiveDrawingRecord();
        return new DrawingState(
                new ArrayList<>(drawingRecords),
                activeDrawingIndex,
                drawingInputRevision);
    }

    private void persistDrawingState() {
        if (prefs == null) return;
        prefs.edit().putString(PREF_DRAWING_STATE, currentDrawingState().toJson()).apply();
    }

    private void clearActiveDrawingFields() {
        aiResultRevision = -1;
        aiDrawingSquareFeet = 0;
        aiDrawingCanCalculate = false;
        aiDrawingEditedByUser = false;
        aiDrawingConfidence = "";
        aiDrawingExplanation = "";
        aiDrawingMissingInformation = "";
        aiDrawingLastError = "";
        aiDrawingCalculationParts = null;
        aiVerificationDrawing = null;
    }

    private void loadActiveDrawingResult(boolean updateView) {
        clearActiveDrawingFields();
        DrawingRecord drawing = activeDrawingRecord();
        if (drawing != null) aiDrawingLastError = drawing.lastError;
        if (drawing != null && drawing.hasResult()) {
            aiResultRevision = drawing.resultRevision;
            aiDrawingSquareFeet = drawing.squareFeet;
            aiDrawingCanCalculate = drawing.canCalculate;
            aiDrawingEditedByUser = drawing.editedByUser;
            aiDrawingConfidence = drawing.confidence;
            aiDrawingExplanation = drawing.explanation;
            aiDrawingMissingInformation = drawing.missingInformation;
            aiDrawingLastError = drawing.lastError;
            aiDrawingCalculationParts = drawing.calculationParts;
            aiVerificationDrawing = drawing.verificationDrawing;
        }
        if (!updateView || verificationDrawingView == null) return;
        if (aiVerificationDrawing == null) {
            verificationDrawingView.clearDrawing();
            verificationDrawingView.setVisibility(View.GONE);
        } else {
            verificationDrawingView.setVerificationDrawing(aiVerificationDrawing);
        }
    }

    private void syncActiveDrawingRecord() {
        if (drawingRecords.size() != drawingPhotoUris.size()
                || activeDrawingIndex < 0
                || activeDrawingIndex >= drawingRecords.size()) return;
        Uri uri = drawingPhotoUris.get(activeDrawingIndex);
        if (uri == null) return;
        drawingRecords.set(
                activeDrawingIndex,
                new DrawingRecord(
                        uri.toString(),
                        aiResultRevision,
                        aiResultRevision >= 0,
                        aiDrawingSquareFeet,
                        aiDrawingCanCalculate,
                        aiDrawingEditedByUser,
                        aiDrawingConfidence,
                        aiDrawingExplanation,
                        aiDrawingMissingInformation,
                        aiDrawingLastError,
                        aiDrawingCalculationParts,
                        aiVerificationDrawing));
    }

    private DrawingRecord activeDrawingRecord() {
        if (activeDrawingIndex < 0 || activeDrawingIndex >= drawingRecords.size()) return null;
        return drawingRecords.get(activeDrawingIndex);
    }

    private boolean hasActiveDrawingResult() {
        DrawingRecord drawing = activeDrawingRecord();
        return drawing != null && drawing.hasResult();
    }

    private int analyzedDrawingCount() {
        int count = 0;
        for (DrawingRecord drawing : drawingRecords) {
            if (drawing.hasResult()) count++;
        }
        return count;
    }

    private boolean hasDrawingAnalysisError() {
        for (DrawingRecord drawing : drawingRecords) {
            if (drawing != null && !drawing.lastError.isEmpty()) return true;
        }
        return false;
    }

    private boolean hasCompleteAiDrawingEstimate() {
        if (drawingRecords.isEmpty()) return false;
        for (DrawingRecord drawing : drawingRecords) {
            if (!drawing.hasResult() || !drawing.canCalculate) {
                return false;
            }
        }
        return true;
    }

    private int usableDrawingCount() {
        int count = 0;
        for (DrawingRecord drawing : drawingRecords) {
            if (drawing.hasResult() && drawing.canCalculate) count++;
        }
        return count;
    }

    private double calculatedDrawingSubtotal() {
        double total = 0;
        for (DrawingRecord drawing : drawingRecords) {
            if (drawing.hasResult() && drawing.canCalculate) total += drawing.squareFeet;
        }
        return Math.round(total * 100.0) / 100.0;
    }

    private double combinedAiDrawingSquareFeet() {
        if (!hasCompleteAiDrawingEstimate()) return 0;
        return calculatedDrawingSubtotal();
    }

    @Override
    protected void onDestroy() {
        drawingAnalysisRequestId++;
        drawingAnalysisInProgress = false;
        if (drawingProgressRunnable != null) {
            drawingProgressHandler.removeCallbacks(drawingProgressRunnable);
        }
        if (addressLookupRunnable != null) {
            addressHandler.removeCallbacks(addressLookupRunnable);
        }
        HttpURLConnection connection;
        synchronized (drawingConnectionLock) {
            connection = activeDrawingConnection;
            activeDrawingConnection = null;
        }
        if (connection != null) connection.disconnect();
        if (drawingZoomDialog != null) {
            drawingZoomDialog.dismiss();
            drawingZoomDialog = null;
        }
        if (drawingEditDialog != null) {
            drawingEditDialog.dismiss();
            drawingEditDialog = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        persistDrawingState();
        super.onStop();
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
        projectAddress = addressInput("Type the project address");
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

        manualMeasurementList = new LinearLayout(this);
        manualMeasurementList.setOrientation(LinearLayout.VERTICAL);
        manualMeasurementTotal = resultLabel("Manual measurement total: 0.00 sq ft");

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
        cabinetInQuantity = quantityInput("Are the cabinets in?");
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
        cabinetsApproximateDate = input("Approximate cabinet date or notes", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        cabinetInterestComments = input("Cabinet comments, if needed", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        squareFootResult = resultLabel("Net square footage: 0.00");
        totalResult = resultLabel("Estimated total: $0.00");
        photoStatus = label("No photo selected.");
        photoStatus.setTextSize(14);
        roomPhoto = new ImageView(this);
        roomPhoto.setAdjustViewBounds(true);
        roomPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        drawingStatus = label("No countertop drawing selected.");
        drawingStatus.setTextSize(14);
        drawingPhoto = new ImageView(this);
        drawingPhoto.setAdjustViewBounds(true);
        drawingPhoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
        drawingPhoto.setBackgroundColor(Color.WHITE);
        drawingPhoto.setContentDescription("Original countertop drawing. Tap to zoom.");
        drawingPhoto.setOnClickListener(v -> showDrawingZoom());
        drawingProgressBar = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal);
        drawingProgressBar.setMax(100);
        drawingProgressBar.setProgress(0);
        drawingProgressBar.setVisibility(View.GONE);
        drawingProgressText = label("");
        drawingProgressText.setTextSize(14);
        drawingProgressText.setGravity(Gravity.CENTER);
        drawingProgressText.setVisibility(View.GONE);
        drawingVerificationStatus = label("");
        drawingVerificationStatus.setTextSize(14);
        drawingVerificationStatus.setGravity(Gravity.CENTER);
        drawingVerificationStatus.setVisibility(View.GONE);
        verificationDrawingView = new VerificationDrawingView(this);
        verificationDrawingView.setOnClickListener(v -> showDrawingZoom());
        verificationDrawingView.setVisibility(View.GONE);

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
        if ("Bevel".equals(edgeDetail)) edgeDetail = "Big bevel";
        page.addView(questionTitle(questionForEdit(PAGE_EDGE_DETAIL)));
        addProductImage(R.drawable.edge_details, "Countertop edge detail choices");
        double edgePrice = priceValue(PRICE_EDGE, 10);
        addHelp(edgeName("eased", "Eased and polished") + " is free. Every other edge is "
                + money(edgePrice) + " per linear foot.");

        RadioGroup choices = new RadioGroup(this);
        choices.setOrientation(RadioGroup.VERTICAL);
        String[] values = {
                "Eased and polished",
                "Small round",
                "Big round",
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
        addProductImage(R.drawable.ramsiers_faucet, "RAMSIER'S faucet");
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
        addProductImage(R.drawable.waterfall_countertop, "Waterfall countertop example");
        addHelp("Waterfall sides are " + money(WATERFALL_PRICE) + " each. Use 0 if you do not want a waterfall.");
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
        RadioGroup cabinetChoices = yesNoGroup(value(cabinetInQuantity) > 0);
        cabinetChoices.setOnCheckedChangeListener((group, checkedId) ->
                cabinetInQuantity.setText(checkedYes(group, checkedId) ? "1" : "0"));
        page.addView(cabinetChoices);
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
        addHelp("Upload one or more hand-drawn sketches, 20/20 software drawings, or other clear countertop drawings. AI checks each drawing separately and adds the verified estimates.");

        Button drawingPlan = accordionButton("Countertop drawing for AI", photoAccordionOpen == 2);
        drawingPlan.setOnClickListener(v -> togglePhotoAccordion(2));
        page.addView(drawingPlan);
        if (photoAccordionOpen == 2) {
            addHelp("Make sure every dimension and its unit can be read clearly.");
            boolean canAddDrawing = drawingPhotoUris.size() < MAX_DRAWING_PHOTOS;
            Button drawingButton = secondaryButton(
                    drawingPhotoUris.isEmpty()
                            ? "Take a picture of a drawing"
                            : "Take another picture of a drawing");
            drawingButton.setOnClickListener(v -> takeDrawingPhoto());
            drawingButton.setEnabled(canAddDrawing);
            page.addView(drawingButton);

            Button drawingUploadButton = secondaryButton(
                    drawingPhotoUris.isEmpty()
                            ? "Choose drawings from phone"
                            : "Add more drawings from phone");
            drawingUploadButton.setOnClickListener(v -> openDrawingPhotoPicker());
            drawingUploadButton.setEnabled(canAddDrawing);
            page.addView(drawingUploadButton);

            if (!drawingPhotoUris.isEmpty()) {
                activeDrawingIndex = Math.max(
                        0,
                        Math.min(activeDrawingIndex, drawingPhotoUris.size() - 1));
                updateActiveDrawingPreview();
                page.addView(sectionHeader(
                        "Original drawing " + (activeDrawingIndex + 1)
                                + " of " + drawingPhotoUris.size()));

                if (drawingPhotoUris.size() > 1) {
                    LinearLayout drawingNavigation = new LinearLayout(this);
                    drawingNavigation.setOrientation(LinearLayout.HORIZONTAL);
                    drawingNavigation.setGravity(Gravity.CENTER);
                    Button previousDrawing = secondaryButton("Previous");
                    previousDrawing.setEnabled(activeDrawingIndex > 0);
                    previousDrawing.setOnClickListener(v -> selectDrawing(activeDrawingIndex - 1));
                    drawingNavigation.addView(previousDrawing, new LinearLayout.LayoutParams(
                            0, dp(52), 1f));
                    TextView drawingCount = label(
                            (activeDrawingIndex + 1) + " / " + drawingPhotoUris.size());
                    drawingCount.setGravity(Gravity.CENTER);
                    drawingNavigation.addView(drawingCount, new LinearLayout.LayoutParams(
                            dp(72), dp(52)));
                    Button nextDrawing = secondaryButton("Next");
                    nextDrawing.setEnabled(activeDrawingIndex < drawingPhotoUris.size() - 1);
                    nextDrawing.setOnClickListener(v -> selectDrawing(activeDrawingIndex + 1));
                    drawingNavigation.addView(nextDrawing, new LinearLayout.LayoutParams(
                            0, dp(52), 1f));
                    page.addView(drawingNavigation, new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                }

                detach(drawingPhoto);
                page.addView(drawingPhoto, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(300)));
                drawingPhoto.setOnClickListener(v -> showDrawingZoom(activeDrawingIndex));

                LinearLayout drawingActions = new LinearLayout(this);
                drawingActions.setOrientation(LinearLayout.HORIZONTAL);
                drawingActions.setGravity(Gravity.CENTER);
                Button removeDrawingButton = secondaryButton("Remove drawing");
                removeDrawingButton.setOnClickListener(v -> removeDrawing(activeDrawingIndex));
                drawingActions.addView(removeDrawingButton, new LinearLayout.LayoutParams(
                        0, dp(54), 1f));
                Button clearDrawings = secondaryButton("Clear all");
                clearDrawings.setOnClickListener(v -> confirmClearDrawings());
                drawingActions.addView(clearDrawings, new LinearLayout.LayoutParams(
                        0, dp(54), 1f));
                page.addView(drawingActions, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                addHelp(drawingPhotoUris.size() + " drawing"
                        + (drawingPhotoUris.size() == 1 ? " is" : "s are")
                        + " selected. Each drawing gets its own editable redraw. Tap either picture to compare and zoom.");
                if (drawingPhotoUris.size() > 1) {
                    addHelp("Each upload is counted as a separate countertop area. Do not add two photos of the same plan or it would be counted twice.");
                }
                if (!canAddDrawing) {
                    addHelp("Maximum of " + MAX_DRAWING_PHOTOS + " drawings reached.");
                }

                if (hasActiveDrawingResult()) {
                    page.addView(sectionHeader(
                            "AI verification redraw for drawing "
                                    + (activeDrawingIndex + 1)
                                    + " — compare every dimension"));
                    if (aiVerificationDrawing != null) {
                        verificationDrawingView.setVerificationDrawing(aiVerificationDrawing);
                        verificationDrawingView.setVisibility(View.VISIBLE);
                        detach(verificationDrawingView);
                        page.addView(verificationDrawingView, new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, dp(380)));
                        Button editRedraw = secondaryButton("Edit redraw dimensions or add a label");
                        editRedraw.setEnabled(!drawingAnalysisInProgress);
                        editRedraw.setOnClickListener(v -> showVerificationEditor());
                        page.addView(editRedraw);
                        String units = aiVerificationDrawing.optString("units", "unknown");
                        String unitMessage = "unknown".equals(units)
                                ? "Check the units shown on the original drawing."
                                : "Units: " + units + ".";
                        boolean measuredFallback = aiVerificationDrawing.optBoolean(
                                "fallback_generated",
                                false);
                        drawingVerificationStatus.setText(
                                unitMessage
                                        + (measuredFallback
                                        ? " The AI returned measurements without an outline, so the app created this editable measured guide."
                                        : "")
                                        + " Compare this redraw with the original above before accepting the square footage.");
                    } else {
                        verificationDrawingView.clearDrawing();
                        verificationDrawingView.setVisibility(View.GONE);
                        drawingVerificationStatus.setText(
                                "AI could not make a reliable redraw from this image. Verify the measurements manually.");
                    }
                    drawingVerificationStatus.setVisibility(View.VISIBLE);
                    detach(drawingVerificationStatus);
                    page.addView(drawingVerificationStatus);
                }

                if (!drawingAnalysisInProgress && hasDrawingAnalysisError()) {
                    Button retryDrawingAnalysis = secondaryButton("Try AI again");
                    retryDrawingAnalysis.setOnClickListener(v -> analyzeDrawing());
                    page.addView(retryDrawingAnalysis);
                }

                detach(drawingProgressBar);
                LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(14));
                progressParams.setMargins(0, dp(8), 0, dp(2));
                page.addView(drawingProgressBar, progressParams);
                detach(drawingProgressText);
                page.addView(drawingProgressText);
                detach(drawingStatus);
                page.addView(drawingStatus);
                addHelp("AI estimate only. Verify every dimension before using it for a price.");
            }
        }

        Button manualMeasurement = accordionButton("Manual Measurement", photoAccordionOpen == 3);
        manualMeasurement.setOnClickListener(v -> togglePhotoAccordion(3));
        page.addView(manualMeasurement);
        if (photoAccordionOpen == 3) {
            addHelp("L is length in inches. W is width in inches. T is the calculated square feet for that piece. Finish W and the next blank row appears automatically.");
            detach(manualMeasurementList);
            page.addView(manualMeasurementList);
            detach(manualMeasurementTotal);
            page.addView(manualMeasurementTotal);
            renderManualMeasurements(false);
            addHelp("Manual pieces are added to the verified AI drawing total. Use this for extra countertop areas that are not already included in the AI drawing.");
        }

        Button countertopPhoto = accordionButton("Photos of actual countertop", photoAccordionOpen == 1);
        countertopPhoto.setOnClickListener(v -> togglePhotoAccordion(1));
        page.addView(countertopPhoto);
        if (photoAccordionOpen == 1) {
            addHelp("Add photos of the customer's actual kitchen or countertop. These photos go with the email.");
            Button cameraButton = primaryButton("Take photo of countertop");
            cameraButton.setOnClickListener(v -> takeCountertopPhoto());
            page.addView(cameraButton);
            Button photoButton = secondaryButton("Choose countertop photo from phone");
            photoButton.setOnClickListener(v -> openPhotoPicker());
            page.addView(photoButton);
            if (!countertopPhotoUris.isEmpty()) {
                detach(photoStatus);
                page.addView(photoStatus);
                detach(roomPhoto);
                page.addView(roomPhoto, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));
            }

        }
        addHelp("You may skip this page and tap Next.");
        addInlineNavigation();
    }

    private void togglePhotoAccordion(int section) {
        if (photoAccordionOpen == 3) commitPendingManualMeasurement(false);
        photoAccordionOpen = photoAccordionOpen == section ? 0 : section;
        showStep();
    }

    private void renderManualMeasurements(boolean focusNewPiece) {
        if (manualMeasurementList == null) return;
        renderingManualMeasurements = true;
        manualMeasurementList.removeAllViews();
        manualMeasurementList.addView(manualMeasurementHeader());

        for (int i = 0; i < sections.size(); i++) {
            addManualMeasurementRow(i, sections.get(i));
        }

        addBlankManualMeasurementRow();
        renderingManualMeasurements = false;
        updateManualMeasurementTotal(0);

        if (focusNewPiece && pendingManualLength != null) {
            EditText field = pendingManualLength;
            field.postDelayed(() -> {
                if (photoAccordionOpen != 3 || field != pendingManualLength) return;
                field.requestFocus();
                field.requestRectangleOnScreen(
                        new android.graphics.Rect(0, 0, field.getWidth(), field.getHeight()),
                        true);
                showKeyboard(field);
            }, 120);
        }
    }

    private LinearLayout manualMeasurementHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(8), dp(8), dp(8), 0);
        header.addView(manualMeasurementHeaderCell("L\nLength (in)"),
                manualMeasurementColumnParams(0, dp(4)));
        header.addView(manualMeasurementHeaderCell("W\nWidth (in)"),
                manualMeasurementColumnParams(dp(4), dp(4)));
        header.addView(manualMeasurementHeaderCell("T\nTotal sq ft"),
                manualMeasurementColumnParams(dp(4), 0));
        header.addView(new View(this), new LinearLayout.LayoutParams(dp(42), dp(58)));
        return header;
    }

    private TextView manualMeasurementHeaderCell(String text) {
        TextView header = label(text);
        header.setTextSize(13);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setGravity(Gravity.CENTER);
        return header;
    }

    private void addManualMeasurementRow(int index, CounterSection section) {
        LinearLayout row = itemRow();

        EditText length = input("Length in inches", decimalInput());
        EditText width = input("Width in inches", decimalInput());
        styleManualMeasurementInput(length, "L");
        styleManualMeasurementInput(width, "W");
        length.setContentDescription("Piece " + (index + 1) + " length in inches");
        width.setContentDescription("Piece " + (index + 1) + " width in inches");
        length.setText(measurementValue(section.length));
        width.setText(measurementValue(section.width));
        length.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        width.setImeOptions(EditorInfo.IME_ACTION_DONE);
        TextView result = manualMeasurementTotalBox(
                manualPieceTotal(section.length, section.width, section.quantity));
        result.setContentDescription("Piece " + (index + 1) + " total square feet");

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (renderingManualMeasurements || index >= sections.size()) return;
                double enteredLength = value(length);
                double enteredWidth = value(width);
                CounterSection current = sections.get(index);
                CounterSection updated = new CounterSection(
                        current.name,
                        enteredLength,
                        enteredWidth,
                        current.quantity);
                sections.set(index, updated);
                result.setText(manualPieceTotal(
                        updated.length,
                        updated.width,
                        updated.quantity));
                saveLists();
                renderSections();
                updateManualMeasurementTotal(0);
                calculateAndDisplay(false);
            }
        };
        length.addTextChangedListener(watcher);
        width.addTextChangedListener(watcher);

        row.addView(length, manualMeasurementColumnParams(0, dp(4)));
        row.addView(width, manualMeasurementColumnParams(dp(4), dp(4)));
        row.addView(result, manualMeasurementColumnParams(dp(4), 0));
        Button remove = manualMeasurementDeleteButton(index);
        row.addView(remove, new LinearLayout.LayoutParams(dp(42), dp(58)));
        manualMeasurementList.addView(row);
    }

    private void addBlankManualMeasurementRow() {
        LinearLayout row = itemRow();

        EditText length = input("Length in inches", decimalInput());
        EditText width = input("Width in inches", decimalInput());
        styleManualMeasurementInput(length, "L");
        styleManualMeasurementInput(width, "W");
        length.setContentDescription("New piece length in inches");
        width.setContentDescription("New piece width in inches");
        length.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        width.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        TextView result = manualMeasurementTotalBox("0.00\nsq ft");
        result.setContentDescription("New piece total square feet");
        pendingManualLength = length;
        pendingManualWidth = width;
        manualPendingCommitted = false;
        final int[] savedIndex = {-1};
        View trailingSpacer = new View(this);

        TextWatcher previewWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (renderingManualMeasurements) return;
                double enteredLength = value(length);
                double enteredWidth = value(width);
                double pendingSquareFeet = (enteredLength * enteredWidth) / 144.0;
                result.setText(manualPieceTotal(enteredLength, enteredWidth, 1));
                if (savedIndex[0] >= 0 && savedIndex[0] < sections.size()) {
                    CounterSection current = sections.get(savedIndex[0]);
                    sections.set(savedIndex[0], new CounterSection(
                            current.name,
                            enteredLength,
                            enteredWidth,
                            current.quantity));
                    saveLists();
                    renderSections();
                    updateManualMeasurementTotal(0);
                    calculateAndDisplay(false);
                    return;
                }
                if (enteredLength > 0 && enteredWidth > 0) {
                    savedIndex[0] = sections.size();
                    manualPendingCommitted = true;
                    sections.add(new CounterSection(
                            "Piece " + (savedIndex[0] + 1),
                            enteredLength,
                            enteredWidth,
                            1));
                    saveLists();
                    renderSections();
                    updateManualMeasurementTotal(0);
                    calculateAndDisplay(false);
                    row.removeView(trailingSpacer);
                    row.addView(
                            manualMeasurementDeleteButton(savedIndex[0]),
                            new LinearLayout.LayoutParams(dp(42), dp(58)));
                    addBlankManualMeasurementRow();
                } else {
                    updateManualMeasurementTotal(pendingSquareFeet);
                }
            }
        };
        length.addTextChangedListener(previewWatcher);
        width.addTextChangedListener(previewWatcher);
        width.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT
                    || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                if (savedIndex[0] >= 0 && pendingManualLength != null) {
                    pendingManualLength.requestFocus();
                    pendingManualLength.requestRectangleOnScreen(
                            new android.graphics.Rect(
                                    0,
                                    0,
                                    pendingManualLength.getWidth(),
                                    pendingManualLength.getHeight()),
                            true);
                    showKeyboard(pendingManualLength);
                    return true;
                }
                return commitPendingManualMeasurement(true);
            }
            return false;
        });
        View.OnFocusChangeListener finishPieceOnBlur = (view, hasFocus) -> {
            if (!hasFocus) commitPendingManualMeasurement(false);
        };
        length.setOnFocusChangeListener(finishPieceOnBlur);
        width.setOnFocusChangeListener(finishPieceOnBlur);

        row.addView(length, manualMeasurementColumnParams(0, dp(4)));
        row.addView(width, manualMeasurementColumnParams(dp(4), dp(4)));
        row.addView(result, manualMeasurementColumnParams(dp(4), 0));
        row.addView(trailingSpacer, new LinearLayout.LayoutParams(dp(42), dp(58)));
        manualMeasurementList.addView(row);
    }

    private Button manualMeasurementDeleteButton(int index) {
        Button remove = miniButton("×");
        remove.setTextSize(18);
        remove.setContentDescription("Delete piece " + (index + 1));
        remove.setOnClickListener(v -> {
            if (index < 0 || index >= sections.size()) return;
            sections.remove(index);
            renumberGeneratedManualPieces();
            saveLists();
            renderManualMeasurements(false);
            renderSections();
            calculateAndDisplay(false);
        });
        return remove;
    }

    private void styleManualMeasurementInput(EditText field, String hint) {
        field.setHint(hint);
        field.setGravity(Gravity.CENTER);
        field.setTextSize(18);
        field.setMinWidth(0);
        field.setMinimumWidth(0);
        field.setPadding(dp(6), 0, dp(6), 0);
        field.setBackground(manualMeasurementBoxBackground(Color.WHITE));
    }

    private TextView manualMeasurementTotalBox(String text) {
        TextView total = label(text);
        total.setTextSize(16);
        total.setTypeface(Typeface.DEFAULT_BOLD);
        total.setGravity(Gravity.CENTER);
        total.setPadding(dp(4), 0, dp(4), 0);
        total.setBackground(manualMeasurementBoxBackground(Color.rgb(239, 230, 220)));
        return total;
    }

    private android.graphics.drawable.GradientDrawable manualMeasurementBoxBackground(int color) {
        android.graphics.drawable.GradientDrawable background =
                new android.graphics.drawable.GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(4));
        background.setStroke(dp(1), Color.rgb(91, 58, 41));
        return background;
    }

    private LinearLayout.LayoutParams manualMeasurementColumnParams(int leftMargin, int rightMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(58), 1f);
        params.setMargins(leftMargin, dp(4), rightMargin, dp(4));
        return params;
    }

    private boolean commitPendingManualMeasurement(boolean focusNewPiece) {
        if (manualPendingCommitted || pendingManualLength == null || pendingManualWidth == null) {
            return false;
        }
        double enteredLength = value(pendingManualLength);
        double enteredWidth = value(pendingManualWidth);
        if (enteredLength <= 0 || enteredWidth <= 0) return false;

        manualPendingCommitted = true;
        sections.add(new CounterSection(
                "Piece " + (sections.size() + 1),
                enteredLength,
                enteredWidth,
                1));
        saveLists();
        renderSections();
        calculateAndDisplay(false);
        if (manualMeasurementList != null) {
            manualMeasurementList.post(() -> {
                if (photoAccordionOpen == 3 && currentPageId() == PAGE_PHOTO) {
                    renderManualMeasurements(focusNewPiece);
                }
            });
        }
        return true;
    }

    private void updateManualMeasurementTotal(double pendingSquareFeet) {
        if (manualMeasurementTotal == null) return;
        double total = pendingSquareFeet;
        for (CounterSection section : sections) total += section.squareFeet();
        manualMeasurementTotal.setText(
                "Manual measurement total: " + number.format(total) + " sq ft");
    }

    private String manualPieceTotal(double length, double width, double quantity) {
        String quantityLabel = quantity == 1
                ? ""
                : " ×" + measurementValue(quantity);
        return number.format((length * width * quantity) / 144.0)
                + "\nsq ft" + quantityLabel;
    }

    private void renumberGeneratedManualPieces() {
        for (int i = 0; i < sections.size(); i++) {
            CounterSection section = sections.get(i);
            if (section.name.matches("Piece \\d+")) {
                sections.set(i, new CounterSection(
                        "Piece " + (i + 1),
                        section.length,
                        section.width,
                        section.quantity));
            }
        }
    }

    private String measurementValue(double value) {
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return number.format(value);
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

        Button printQuote = primaryButton("Print / Save PDF");
        printQuote.setOnClickListener(v -> printQuoteSummary());
        page.addView(printQuote);

        Button textQuote = primaryButton("Text JPEG & payment link");
        textQuote.setOnClickListener(v -> textQuoteSummary());
        page.addView(textQuote);

        addInlineNavigation();

        Button reset = secondaryButton("Start a new customer");
        reset.setOnClickListener(v -> confirmReset());
        page.addView(reset);
    }

    private void printQuoteSummary() {
        try {
            Estimate estimate = calculateAndDisplay(false);
            QuotePdfGenerator.Data data = buildQuotePdfData(estimate);
            File quoteDirectory = new File(getFilesDir(), "quote_pdfs");
            File quoteFile = new File(
                    quoteDirectory,
                    "Ramsiers-Quote-Request-" + safeFileName(text(customerName)) + ".pdf");
            QuotePdfGenerator.create(quoteFile, data);
            PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
            if (printManager == null) {
                Toast.makeText(this, "Printing is not available on this phone.", Toast.LENGTH_LONG).show();
                return;
            }
            printManager.print(
                    "Ramsier's quote request",
                    new PdfFilePrintAdapter(this, quoteFile),
                    null);
        } catch (Exception exception) {
            Toast.makeText(
                    this,
                    "The quote PDF could not be created. Please try again.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void textQuoteSummary() {
        try {
            Estimate estimate = calculateAndDisplay(false);
            QuotePdfGenerator.Data data = buildQuotePdfData(estimate);
            File quoteDirectory = new File(getFilesDir(), "quote_jpegs");
            File quoteImage = new File(
                    quoteDirectory,
                    "Ramsiers-Final-Quote-" + safeFileName(text(customerName)) + ".jpg");
            QuotePdfGenerator.createJpeg(quoteImage, data);
            Uri imageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    quoteImage);

            String phone = customerPhone.getText().toString().trim();
            String messageText = "Ramsier's Granite and Quartz\n"
                    + "Your finished quote is attached.\n"
                    + "Full amount due when the job is complete: " + money(estimate.total) + "\n"
                    + "ACH payment link: Add the secure Stripe link here after the job is finished.";

            Intent message = new Intent(Intent.ACTION_SEND);
            message.setType("image/jpeg");
            message.putExtra("address", phone);
            message.putExtra("sms_body", messageText);
            message.putExtra(Intent.EXTRA_TEXT, messageText);
            message.putExtra(Intent.EXTRA_STREAM, imageUri);
            message.setClipData(ClipData.newUri(getContentResolver(), "Ramsier's final quote", imageUri));
            message.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(message, "Text the customer's final quote"));
        } catch (Exception exception) {
            Toast.makeText(
                    this,
                    "The quote picture could not be created. Please try again.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private QuotePdfGenerator.Data buildQuotePdfData(Estimate estimate) {
        QuotePdfGenerator.Data data = new QuotePdfGenerator.Data();
        data.customer = text(customerName);
        data.phone = text(customerPhone);
        data.email = text(customerEmail);
        data.address = text(projectAddress);
        data.estimatedTotal = money(estimate.total);

        double cooktopCount = value(cooktopCutoutQuantity);
        double cooktopPrice = priceValue(PRICE_CUTOUT, 100);
        if (cooktopCount > 0) {
            addPdfPriceRow(
                    data,
                    "Cooktop or extra cutouts",
                    quantityLabel(cooktopCount) + " × " + money(cooktopPrice),
                    cooktopCount * cooktopPrice);
        }

        double edgeFeet = value(edgeLinearFeet);
        double edgePrice = priceValue(PRICE_EDGE, 10);
        if ("Eased and polished".equals(edgeDetail)) {
            data.pricedOptions.add(new QuotePdfGenerator.PriceRow(
                    "Edge detail",
                    edgeDisplayName(edgeDetail) + " - Free",
                    "$0.00"));
        } else {
            addPdfPriceRow(
                    data,
                    "Edge detail",
                    edgeDisplayName(edgeDetail) + " - " + quantityLabel(edgeFeet)
                            + " ft × " + money(edgePrice),
                    edgeFeet * edgePrice);
        }

        addQuantityPdfPriceRow(data, "Ramsier's faucets", faucetQuantity, PRICE_FAUCET, 225);
        addQuantityPdfPriceRow(data, "Basket drains", basketQuantity, PRICE_BASKET, 35);
        addQuantityPdfPriceRow(data, "Big grids", gridQuantity, PRICE_GRID, 70);
        double waterfallCount = value(waterfallQuantity);
        if (waterfallCount > 0) {
            addPdfPriceRow(
                    data,
                    "Waterfall sides",
                    quantityLabel(waterfallCount) + " × " + money(WATERFALL_PRICE),
                    waterfallCount * WATERFALL_PRICE);
        }
        if (value(sinkCharge) > 0) addPdfPriceRow(data, "Sink charge", "Additional sink charge", value(sinkCharge));
        if (value(edgeCharge) > 0) addPdfPriceRow(data, "Extra edge labor", "Additional labor", value(edgeCharge));
        if (value(tearOutCharge) > 0) addPdfPriceRow(data, "Tear-out", "Entered charge", value(tearOutCharge));
        if (value(otherCharge) > 0) addPdfPriceRow(data, "Other charge", "Entered charge", value(otherCharge));

        data.sinkSelections = sinkSelectionDisplay().replace(", ", "  |  ");
        data.sinkLocationNote = combinedSinkLocationNote();
        data.waterfall = quantityLabel(value(waterfallQuantity))
                + "  |  Comments: " + textOrNotProvided(waterfallComments);
        data.cabinets = "Cabinets in: " + yesNo(value(cabinetInQuantity) > 0)
                + "  |  Date / notes: " + textOrNotProvided(cabinetsApproximateDate)
                + "  |  Comments: " + textOrNotProvided(cabinetInterestComments);
        data.drawingTotal = number.format(estimate.net) + " sq. ft.";
        data.drawingDetails = printableDrawingDetails(estimate);
        data.countertopSections = printableCountertopSections();
        data.selectedSlabs = printableSelectedSlabs();
        data.projectNotes = textOrNotProvided(projectNotes);
        return data;
    }

    private void addQuantityPdfPriceRow(
            QuotePdfGenerator.Data data,
            String item,
            EditText quantityField,
            String preferenceKey,
            double defaultPrice) {
        double count = value(quantityField);
        if (count <= 0) return;
        double price = priceValue(preferenceKey, defaultPrice);
        addPdfPriceRow(data, item, quantityLabel(count) + " × " + money(price), count * price);
    }

    private void addPdfPriceRow(
            QuotePdfGenerator.Data data,
            String item,
            String details,
            double amount) {
        data.pricedOptions.add(new QuotePdfGenerator.PriceRow(item, details, money(amount)));
    }

    private String combinedSinkLocationNote() {
        ArrayList<String> notes = new ArrayList<>();
        String rectangle = textOrNotProvided(rectangleVanitySinkLocations);
        String oval = textOrNotProvided(ovalVanitySinkLocations);
        if (!"Not provided".equals(rectangle)) notes.add(rectangle);
        if (!"Not provided".equals(oval)) notes.add(oval);
        if (notes.isEmpty()) return "Not provided";
        return android.text.TextUtils.join("  |  ", notes);
    }

    private String printableDrawingDetails(Estimate estimate) {
        if (drawingRecords.isEmpty()) {
            return estimate.manualSquareFeet > 0
                    ? "Manual countertop sections: " + number.format(estimate.manualSquareFeet) + " sq. ft."
                    : "No verified drawing estimate.";
        }
        StringBuilder details = new StringBuilder();
        for (int i = 0; i < drawingRecords.size(); i++) {
            DrawingRecord drawing = drawingRecords.get(i);
            if (details.length() > 0) details.append("  ");
            details.append("Drawing ").append(i + 1).append(": ");
            if (drawing.hasResult() && drawing.canCalculate) {
                details.append(number.format(drawing.squareFeet)).append(" sq. ft.");
            } else {
                details.append("needs manual verification.");
            }
        }
        if (estimate.manualSquareFeet > 0) {
            details.append("  Additional manual: ")
                    .append(number.format(estimate.manualSquareFeet)).append(" sq. ft.");
        }
        return details.toString();
    }

    private String printableCountertopSections() {
        if (sections.isEmpty()) return "No countertop sections added yet.";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < sections.size(); i++) {
            CounterSection section = sections.get(i);
            if (result.length() > 0) result.append("  |  ");
            result.append(section.name).append(": ")
                    .append(quantityLabel(section.length)).append(" × ")
                    .append(quantityLabel(section.width)).append(" in × ")
                    .append(quantityLabel(section.quantity)).append(" = ")
                    .append(number.format(section.squareFeet())).append(" sq. ft.");
            if (i == 2 && sections.size() > 3) {
                result.append("  |  +").append(sections.size() - 3).append(" more");
                break;
            }
        }
        return result.toString();
    }

    private String printableSelectedSlabs() {
        if (slabs.isEmpty()) return "No slabs selected.";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < slabs.size(); i++) {
            if (result.length() > 0) result.append("  |  ");
            result.append(slabs.get(i).name);
            if (i == 2 && slabs.size() > 3) {
                result.append("  |  +").append(slabs.size() - 3).append(" more");
                break;
            }
        }
        return result.toString();
    }

    private String quantityLabel(double value) {
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return number.format(value);
    }

    private String safeFileName(String value) {
        String clean = value == null ? "Customer" : value.trim().replaceAll("[^A-Za-z0-9_-]+", "-");
        return clean.isEmpty() ? "Customer" : clean;
    }

    private void addNavigation() {
        Button back = secondaryButton("Back");
        back.setEnabled(stepIndex > 0);
        back.setOnClickListener(v -> {
            if (currentPageId() == PAGE_PHOTO) commitPendingManualMeasurement(false);
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
            if (currentPageId() == PAGE_PHOTO) commitPendingManualMeasurement(false);
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
        if (pageId == PAGE_PHOTO) commitPendingManualMeasurement(false);
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
                renumberGeneratedManualPieces();
                saveLists();
                renderSections();
                calculateAndDisplay(false);
            });
            row.addView(remove);
            sectionList.addView(row);
        }
    }

    private Estimate calculateAndDisplay(boolean showWarnings) {
        double manualGross = manualCountertopSquareFeet();
        boolean hasAiDrawing = hasCompleteAiDrawingEstimate();
        QuoteSquareFootMath.Result squareFeet = QuoteSquareFootMath.calculate(
                combinedAiDrawingSquareFeet(),
                manualGross,
                (value(stoveLength) * value(stoveWidth)) / 144.0,
                hasAiDrawing);
        double gross = squareFeet.grossSquareFeet;
        double stove = squareFeet.stoveSquareFeet;
        double net = squareFeet.netSquareFeet;
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
        double waterfallTotal = value(waterfallQuantity) * WATERFALL_PRICE;
        double total = net * value(pricePerSqFt)
                + value(sinkCharge)
                + value(edgeCharge)
                + value(tearOutCharge)
                + value(otherCharge)
                + cooktopCutoutTotal
                + edgeDetailTotal
                + faucetTotal
                + basketTotal
                + gridTotal
                + waterfallTotal;

        if (squareFootResult != null) squareFootResult.setText("Net square footage: " + number.format(net));
        if (totalResult != null) totalResult.setText("Estimated total: $" + number.format(total));

        if (showWarnings && gross <= 0) {
            Toast.makeText(this, "Add a countertop section or use the AI drawing estimate first.", Toast.LENGTH_LONG).show();
        }
        return new Estimate(
                squareFeet.aiSquareFeet,
                squareFeet.manualSquareFeet,
                gross,
                stove,
                net,
                total,
                hasAiDrawing);
    }

    private double manualCountertopSquareFeet() {
        double total = 0;
        for (CounterSection section : sections) total += section.squareFeet();
        return total;
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
        double waterfallTotal = value(waterfallQuantity) * WATERFALL_PRICE;
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
                + " × " + money(WATERFALL_PRICE) + " = " + money(waterfallTotal)
                + "\nWaterfall comments: " + textOrNotProvided(waterfallComments)
                + "\nCabinets in: " + yesNo(value(cabinetInQuantity) > 0)
                + "\nCabinet date / notes: " + textOrNotProvided(cabinetsApproximateDate)
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
        if (drawingRecords.isEmpty() || analyzedDrawingCount() == 0) return "";
        StringBuilder result = new StringBuilder();
        if (hasCompleteAiDrawingEstimate()) {
            result.append("\nCombined verified drawing estimate: ")
                    .append(number.format(combinedAiDrawingSquareFeet()))
                    .append(" sq ft");
        } else {
            result.append("\nDrawing estimate is incomplete and was not used for pricing.");
        }
        for (int i = 0; i < drawingRecords.size(); i++) {
            DrawingRecord drawing = drawingRecords.get(i);
            result.append("\nDrawing ").append(i + 1).append(": ");
            if (drawing.hasResult() && drawing.canCalculate) {
                result.append(number.format(drawing.squareFeet)).append(" sq ft");
                if (drawing.editedByUser) result.append(" (edited by user)");
            } else if (drawing.hasResult()) {
                result.append("could not be calculated");
            } else {
                result.append("not analyzed");
            }
            if (!drawing.missingInformation.isEmpty()) {
                result.append(" — missing: ").append(drawing.missingInformation);
            }
        }
        return result.toString();
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
        if (drawingPhotoUris.size() >= MAX_DRAWING_PHOTOS) {
            Toast.makeText(
                    this,
                    "You can use up to " + MAX_DRAWING_PHOTOS + " drawings at one time.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, PICK_DRAWING_IMAGE);
    }

    private void selectDrawing(int index) {
        if (drawingPhotoUris.isEmpty()) return;
        syncActiveDrawingRecord();
        activeDrawingIndex = Math.max(0, Math.min(index, drawingPhotoUris.size() - 1));
        loadActiveDrawingResult(true);
        persistDrawingState();
        updateActiveDrawingPreview();
        showStep();
    }

    private void addDrawingPhotos(List<Uri> newDrawingUris) {
        syncActiveDrawingRecord();
        int originalCount = drawingPhotoUris.size();
        boolean omittedForLimit = false;
        for (Uri uri : newDrawingUris) {
            if (uri == null || drawingPhotoUris.contains(uri)) continue;
            if (drawingPhotoUris.size() >= MAX_DRAWING_PHOTOS) {
                omittedForLimit = true;
                break;
            }
            drawingPhotoUris.add(uri);
            drawingRecords.add(DrawingRecord.empty(uri.toString()));
        }
        if (drawingPhotoUris.size() == originalCount) {
            Toast.makeText(
                    this,
                    originalCount >= MAX_DRAWING_PHOTOS
                            ? "The maximum of " + MAX_DRAWING_PHOTOS + " drawings is already selected."
                            : "Those drawings are already selected.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        cancelDrawingAnalysis();
        drawingInputRevision++;
        activeDrawingIndex = drawingPhotoUris.size() - 1;
        loadActiveDrawingResult(true);
        dismissDrawingDialogs();
        updateActiveDrawingPreview();
        drawingStatus.setText(drawingPhotoUris.size() + " drawing"
                + (drawingPhotoUris.size() == 1 ? " is" : "s are")
                + " ready. AI is starting automatically.");
        photoAccordionOpen = 2;
        persistDrawingState();
        calculateAndDisplay(false);
        showStep();
        if (omittedForLimit) {
            Toast.makeText(
                    this,
                    "Only the first " + MAX_DRAWING_PHOTOS + " drawings were added.",
                    Toast.LENGTH_LONG).show();
        }
        analyzeDrawing();
    }

    private void removeDrawing(int index) {
        if (index < 0 || index >= drawingPhotoUris.size()) return;
        syncActiveDrawingRecord();
        cancelDrawingAnalysis();
        Uri removedDrawingUri = drawingPhotoUris.remove(index);
        if (index < drawingRecords.size()) drawingRecords.remove(index);
        drawingInputRevision++;
        activeDrawingIndex = Math.max(
                0,
                Math.min(index, drawingPhotoUris.size() - 1));
        loadActiveDrawingResult(true);
        dismissDrawingDialogs();
        updateActiveDrawingPreview();
        if (drawingPhotoUris.isEmpty()) {
            drawingStatus.setText("No countertop drawing selected.");
        } else if (hasActiveDrawingResult()) {
            updateDrawingStatusText();
        } else {
            drawingStatus.setText(drawingPhotoUris.size() + " drawing"
                    + (drawingPhotoUris.size() == 1 ? " is" : "s are")
                    + " ready. Run AI for the drawings that do not have an estimate.");
        }
        releaseUnusedPhotoUri(removedDrawingUri);
        persistDrawingState();
        calculateAndDisplay(false);
        showStep();
    }

    private void confirmClearDrawings() {
        if (drawingPhotoUris.isEmpty()) return;
        new AlertDialog.Builder(this)
                .setTitle("Clear all drawings?")
                .setMessage("This removes all selected drawings, saved camera copies, and AI results. Gallery originals stay on your phone.")
                .setPositiveButton("Clear", (dialog, which) -> clearAllDrawings())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllDrawings() {
        cancelDrawingAnalysis();
        clearAiDrawingResult();
        ArrayList<Uri> removedDrawingUris = new ArrayList<>(drawingPhotoUris);
        drawingPhotoUris.clear();
        drawingRecords.clear();
        pendingDrawingCaptureUri = null;
        displayedDrawingPreviewUri = null;
        activeDrawingIndex = 0;
        drawingInputRevision++;
        dismissDrawingDialogs();
        drawingPhoto.setImageDrawable(null);
        drawingStatus.setText("No countertop drawing selected.");
        for (Uri uri : removedDrawingUris) releaseUnusedPhotoUri(uri);
        persistDrawingState();
        calculateAndDisplay(false);
        showStep();
    }

    private void dismissDrawingDialogs() {
        if (drawingZoomDialog != null) {
            drawingZoomDialog.dismiss();
            drawingZoomDialog = null;
        }
        if (drawingEditDialog != null) {
            drawingEditDialog.dismiss();
            drawingEditDialog = null;
        }
    }

    private void updateActiveDrawingPreview() {
        if (drawingPhotoUris.isEmpty()) {
            drawingPhoto.setImageDrawable(null);
            displayedDrawingPreviewUri = null;
            return;
        }
        activeDrawingIndex = Math.max(
                0,
                Math.min(activeDrawingIndex, drawingPhotoUris.size() - 1));
        Uri activeDrawingUri = drawingPhotoUris.get(activeDrawingIndex);
        if (!activeDrawingUri.equals(displayedDrawingPreviewUri)
                || drawingPhoto.getDrawable() == null) {
            loadDrawingIntoImageView(drawingPhoto, activeDrawingUri, 1400);
            displayedDrawingPreviewUri = drawingPhoto.getDrawable() == null
                    ? null
                    : activeDrawingUri;
        }
        drawingPhoto.setContentDescription(
                "Original countertop drawing " + (activeDrawingIndex + 1)
                        + " of " + drawingPhotoUris.size() + ". Tap to compare and zoom.");
    }

    private void loadDrawingIntoImageView(ImageView imageView, Uri uri, int maximumSide) {
        clearDecodedDrawing(imageView);
        try {
            imageView.setImageBitmap(decodeDrawingBitmap(uri, maximumSide));
        } catch (Exception exception) {
            imageView.setImageDrawable(null);
            Toast.makeText(this, "One drawing photo could not be displayed.", Toast.LENGTH_LONG).show();
        }
    }

    private void clearDecodedDrawing(ImageView imageView) {
        if (imageView == null) return;
        // Attached bitmaps can still be referenced by RenderThread after the view is cleared.
        // Let the runtime reclaim them instead of recycling them while a display list may exist.
        imageView.setImageDrawable(null);
    }

    private Bitmap decodeDrawingBitmap(Uri uri, int maximumSide) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IllegalStateException("The drawing photo could not be read.");
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IllegalStateException("The drawing photo could not be read.");
        }
        int sampleSize = 1;
        int largestSide = Math.max(bounds.outWidth, bounds.outHeight);
        while (largestSide / sampleSize > maximumSide * 3 / 2) sampleSize *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        Bitmap decoded;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IllegalStateException("The drawing photo could not be read.");
            decoded = BitmapFactory.decodeStream(input, null, options);
        }
        if (decoded == null) throw new IllegalStateException("The drawing photo could not be read.");
        decoded = applyDrawingOrientation(uri, decoded);
        int largestDecodedSide = Math.max(decoded.getWidth(), decoded.getHeight());
        if (largestDecodedSide <= maximumSide) return decoded;
        double scale = (double) maximumSide / largestDecodedSide;
        Bitmap scaled = Bitmap.createScaledBitmap(
                decoded,
                Math.max(1, (int) Math.round(decoded.getWidth() * scale)),
                Math.max(1, (int) Math.round(decoded.getHeight() * scale)),
                true);
        if (scaled != decoded) decoded.recycle();
        return scaled;
    }

    private Bitmap applyDrawingOrientation(Uri uri, Bitmap bitmap) {
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input != null) {
                orientation = new ExifInterface(input).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);
            }
        } catch (Exception ignored) {
        }

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }

        try {
            Bitmap corrected = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrix,
                    true);
            if (corrected != bitmap) bitmap.recycle();
            return corrected;
        } catch (Exception ignored) {
            return bitmap;
        }
    }

    private boolean isReadableDrawingUri(Uri uri) {
        if (uri == null) return false;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) return false;
            BitmapFactory.decodeStream(input, null, bounds);
            return bounds.outWidth > 0 && bounds.outHeight > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void takeDrawingPhoto() {
        if (drawingPhotoUris.size() >= MAX_DRAWING_PHOTOS) {
            Toast.makeText(
                    this,
                    "You can use up to " + MAX_DRAWING_PHOTOS + " drawings at one time.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        requestCameraCapture(CAMERA_CAPTURE_DRAWING);
    }

    private void takeCountertopPhoto() {
        requestCameraCapture(CAMERA_CAPTURE_COUNTERTOP);
    }

    private void requestCameraCapture(int captureType) {
        hideKeyboard();
        pendingCameraCapture = captureType;
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
            return;
        }
        openRequestedCamera();
    }

    private void openRequestedCamera() {
        if (pendingCameraCapture == CAMERA_CAPTURE_COUNTERTOP) {
            openCountertopCamera();
        } else if (pendingCameraCapture == CAMERA_CAPTURE_DRAWING) {
            openDrawingCamera();
        }
    }

    private void openDrawingCamera() {
        try {
            File drawingDirectory = new File(getFilesDir(), "drawing_photos");
            if (!drawingDirectory.exists() && !drawingDirectory.mkdirs()) {
                throw new IllegalStateException("Could not create drawing photo folder");
            }
            File drawingFile = new File(
                    drawingDirectory,
                    "countertop-drawing-" + System.currentTimeMillis() + ".jpg");
            pendingDrawingCaptureUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    drawingFile);
            Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            camera.putExtra(MediaStore.EXTRA_OUTPUT, pendingDrawingCaptureUri);
            camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            camera.setClipData(ClipData.newUri(getContentResolver(), "Countertop drawing", pendingDrawingCaptureUri));
            grantCameraUriPermissions(camera, pendingDrawingCaptureUri);
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
        pendingDrawingCaptureUri = null;
        try {
            Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(camera, TAKE_DRAWING_PHOTO);
        } catch (Exception ignored) {
            Toast.makeText(this, "The camera could not be opened.", Toast.LENGTH_LONG).show();
        }
    }

    private void openCountertopCamera() {
        try {
            File photoDirectory = new File(getCacheDir(), "countertop_photos");
            if (!photoDirectory.exists() && !photoDirectory.mkdirs()) {
                throw new IllegalStateException("Could not create countertop photo folder");
            }
            File photoFile = new File(
                    photoDirectory,
                    "countertop-photo-" + System.currentTimeMillis() + ".jpg");
            selectedPhotoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile);
            Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            camera.putExtra(MediaStore.EXTRA_OUTPUT, selectedPhotoUri);
            camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            camera.setClipData(ClipData.newUri(getContentResolver(), "Countertop photo", selectedPhotoUri));
            grantCameraUriPermissions(camera, selectedPhotoUri);
            startActivityForResult(camera, TAKE_COUNTERTOP_PHOTO);
        } catch (Exception e) {
            openCountertopCameraWithoutFileOutput();
        }
    }

    private void openCountertopCameraWithoutFileOutput() {
        selectedPhotoUri = null;
        try {
            startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), TAKE_COUNTERTOP_PHOTO);
        } catch (Exception ignored) {
            Toast.makeText(this, "The camera could not be opened.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != CAMERA_PERMISSION) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openRequestedCamera();
        } else {
            pendingCameraCapture = 0;
            Toast.makeText(this, "Allow camera access to take photos.", Toast.LENGTH_LONG).show();
        }
    }

    private void analyzeDrawing() {
        if (drawingPhotoUris.isEmpty()) {
            Toast.makeText(this, "Take or choose at least one countertop drawing first.", Toast.LENGTH_LONG).show();
            return;
        }
        if (drawingAnalysisInProgress) return;

        syncActiveDrawingRecord();
        final ArrayList<Uri> requestDrawingUris = new ArrayList<>();
        final HashMap<String, DrawingRecord> previousRecordsByUri = new HashMap<>();
        for (int i = 0; i < drawingPhotoUris.size(); i++) {
            DrawingRecord drawing = i < drawingRecords.size() ? drawingRecords.get(i) : null;
            if (drawing != null) {
                previousRecordsByUri.put(drawingPhotoUris.get(i).toString(), drawing);
            }
            if (drawing == null || !drawing.hasResult() || !drawing.lastError.isEmpty()) {
                requestDrawingUris.add(drawingPhotoUris.get(i));
            }
        }
        if (requestDrawingUris.isEmpty()) return;
        final int requestRevision = drawingInputRevision;
        final int requestId = ++drawingAnalysisRequestId;
        startDrawingProgress(requestId);
        drawingStatus.setText("AI is checking drawing 1 of " + requestDrawingUris.size() + "...");
        showStep();

        new Thread(() -> {
            int failureCount = 0;
            for (int i = 0; i < requestDrawingUris.size(); i++) {
                if (requestId != drawingAnalysisRequestId
                        || requestRevision != drawingInputRevision) return;
                Uri uri = requestDrawingUris.get(i);
                int drawingNumber = i + 1;
                try {
                    DrawingRecord result = requestSingleDrawingAnalysis(
                            uri,
                            requestId,
                            requestRevision,
                            drawingNumber,
                            requestDrawingUris.size());
                    runOnUiThread(() -> storeDrawingResult(uri, result));
                } catch (Exception exception) {
                    if (requestId != drawingAnalysisRequestId
                            || requestRevision != drawingInputRevision) return;
                    failureCount++;
                    String message = exception.getMessage() == null
                            ? "The drawing could not be analyzed. Please try again."
                            : exception.getMessage();
                    DrawingRecord previous = previousRecordsByUri.get(uri.toString());
                    if (previous == null) previous = DrawingRecord.empty(uri.toString());
                    DrawingRecord failed = previous.withError(message);
                    runOnUiThread(() -> storeDrawingResult(uri, failed));
                }
            }
            final int completedFailureCount = failureCount;
            runOnUiThread(() -> finishDrawingBatch(
                    requestId,
                    requestRevision,
                    completedFailureCount));
        }).start();
    }

    private DrawingRecord requestSingleDrawingAnalysis(
            Uri drawingUri,
            int requestId,
            int requestRevision,
            int drawingNumber,
            int drawingCount) throws Exception {
        postDrawingProgress(
                requestId,
                5 + (int) Math.round(80.0 * (drawingNumber - 1) / drawingCount),
                "Preparing drawing " + drawingNumber + " of " + drawingCount);
        String image = drawingImageDataUrl(drawingUri);
        if (requestId != drawingAnalysisRequestId
                || requestRevision != drawingInputRevision) {
            throw new IllegalStateException("Drawing analysis was canceled.");
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(DRAWING_AI_ENDPOINT).openConnection();
        try {
            synchronized (drawingConnectionLock) {
                if (requestId != drawingAnalysisRequestId
                        || requestRevision != drawingInputRevision) {
                    connection.disconnect();
                    throw new IllegalStateException("Drawing analysis was canceled.");
                }
                activeDrawingConnection = connection;
            }
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(240000);
            connection.setDoOutput(true);
            connection.setChunkedStreamingMode(8192);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Ramsiers-App", "countertop-quote-v1");

            JSONObject request = DrawingApiRequest.singleImage(image);
            postDrawingProgress(
                    requestId,
                    10 + (int) Math.round(80.0 * (drawingNumber - 1) / drawingCount),
                    "Uploading drawing " + drawingNumber + " of " + drawingCount);
            try (java.io.OutputStream outputStream = connection.getOutputStream();
                 java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                         outputStream,
                         StandardCharsets.UTF_8)) {
                writer.write(request.toString());
            }
            postDrawingProgress(
                    requestId,
                    15 + (int) Math.round(80.0 * (drawingNumber - 1) / drawingCount),
                    "Reading drawing " + drawingNumber + " of " + drawingCount);

            int responseCode = connection.getResponseCode();
            InputStream responseStream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            JSONObject response = new JSONObject(readText(responseStream));
            if (responseCode < 200 || responseCode >= 300) {
                throw new IllegalStateException(
                        response.optString("error", "The drawing could not be analyzed."));
            }

            return DrawingRecord.fromServerResponse(
                    drawingUri.toString(),
                    requestRevision,
                    response);
        } finally {
            connection.disconnect();
            synchronized (drawingConnectionLock) {
                if (activeDrawingConnection == connection) activeDrawingConnection = null;
            }
        }
    }

    private void storeDrawingResult(Uri uri, DrawingRecord result) {
        if (uri == null || result == null) return;
        int index = drawingPhotoUris.indexOf(uri);
        if (index < 0 || index >= drawingRecords.size()) return;
        drawingRecords.set(index, result);
        if (index == activeDrawingIndex) loadActiveDrawingResult(true);
        persistDrawingState();
    }

    private void finishDrawingBatch(int requestId, int requestRevision, int failureCount) {
        if (requestId != drawingAnalysisRequestId
                || requestRevision != drawingInputRevision) return;
        loadActiveDrawingResult(true);
        updateDrawingStatusText();
        finishDrawingProgress(requestId);
        calculateAndDisplay(false);
        showStep();
        if (failureCount > 0) {
            Toast.makeText(
                    this,
                    failureCount + " drawing"
                            + (failureCount == 1 ? " needs" : "s need")
                            + " another try. Successful results were kept.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void startDrawingProgress(int requestId) {
        drawingAnalysisInProgress = true;
        drawingAnalysisStartedAt = SystemClock.elapsedRealtime();
        drawingProgressFloor = 5;
        drawingProgressBar.setProgress(5);
        drawingProgressBar.setVisibility(View.VISIBLE);
        drawingProgressText.setVisibility(View.VISIBLE);
        drawingProgressText.setText(
                "Estimated progress: 5%\nPreparing drawing — AI is still working");
        if (drawingProgressRunnable != null) {
            drawingProgressHandler.removeCallbacks(drawingProgressRunnable);
        }
        drawingProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (!drawingAnalysisInProgress || requestId != drawingAnalysisRequestId) return;
                long elapsedSeconds = Math.max(
                        0,
                        (SystemClock.elapsedRealtime() - drawingAnalysisStartedAt) / 1000);
                int estimated = Math.min(
                        90,
                        Math.max(
                                drawingProgressFloor,
                                8 + (int) Math.floor(Math.sqrt(elapsedSeconds) * 10)));
                String stage;
                if (elapsedSeconds < 4) {
                    stage = "Preparing drawing";
                } else if (elapsedSeconds < 12) {
                    stage = "Uploading drawing";
                } else if (elapsedSeconds < 30) {
                    stage = "Reading measurements";
                } else if (elapsedSeconds < 55) {
                    stage = "Calculating square footage";
                } else {
                    stage = "Creating verification redraw";
                }
                drawingProgressBar.setProgress(estimated);
                drawingProgressText.setText(
                        "Estimated progress: " + estimated + "%\n"
                                + stage + " — AI is still working ("
                                + elapsedSeconds + " seconds)");
                drawingProgressHandler.postDelayed(this, 1000);
            }
        };
        drawingProgressHandler.post(drawingProgressRunnable);
    }

    private void postDrawingProgress(int requestId, int minimum, String stage) {
        runOnUiThread(() -> {
            if (!drawingAnalysisInProgress || requestId != drawingAnalysisRequestId) return;
            drawingProgressFloor = Math.max(drawingProgressFloor, Math.min(90, minimum));
            int progress = Math.max(drawingProgressBar.getProgress(), drawingProgressFloor);
            drawingProgressBar.setProgress(progress);
            drawingProgressText.setText(
                    "Estimated progress: " + progress + "%\n"
                            + stage + " — AI is still working");
        });
    }

    private void finishDrawingProgress(int requestId) {
        if (requestId != drawingAnalysisRequestId) return;
        drawingAnalysisInProgress = false;
        if (drawingProgressRunnable != null) {
            drawingProgressHandler.removeCallbacks(drawingProgressRunnable);
        }
        long elapsedSeconds = Math.max(
                0,
                (SystemClock.elapsedRealtime() - drawingAnalysisStartedAt) / 1000);
        drawingProgressBar.setProgress(100);
        drawingProgressBar.setVisibility(View.VISIBLE);
        drawingProgressText.setVisibility(View.VISIBLE);
        String completedWork;
        if (hasCompleteAiDrawingEstimate()) {
            completedWork = drawingRecords.size() == 1
                    ? "measurements and verification redraw finished"
                    : drawingRecords.size() + " verified drawing estimates finished";
        } else if (usableDrawingCount() > 0) {
            completedWork = usableDrawingCount() + " of " + drawingRecords.size()
                    + " drawing estimates finished; the total is incomplete";
        } else {
            completedWork = "drawing review finished without a complete square-foot estimate";
        }
        drawingProgressText.setText(
                "100% complete — " + completedWork + " in "
                        + elapsedSeconds + " seconds");
    }

    private void cancelDrawingAnalysis() {
        drawingAnalysisRequestId++;
        drawingAnalysisInProgress = false;
        if (drawingProgressRunnable != null) {
            drawingProgressHandler.removeCallbacks(drawingProgressRunnable);
        }
        HttpURLConnection connection;
        synchronized (drawingConnectionLock) {
            connection = activeDrawingConnection;
            activeDrawingConnection = null;
        }
        if (connection != null) connection.disconnect();
        drawingProgressFloor = 0;
        drawingProgressBar.setProgress(0);
        drawingProgressBar.setVisibility(View.GONE);
        drawingProgressText.setText("");
        drawingProgressText.setVisibility(View.GONE);
    }

    private void clearAiDrawingResult() {
        for (int i = 0; i < drawingRecords.size(); i++) {
            drawingRecords.set(i, drawingRecords.get(i).withoutResult());
        }
        clearActiveDrawingFields();
        if (verificationDrawingView != null) {
            verificationDrawingView.clearDrawing();
            verificationDrawingView.setVisibility(View.GONE);
        }
        if (drawingVerificationStatus != null) {
            drawingVerificationStatus.setText("");
            drawingVerificationStatus.setVisibility(View.GONE);
        }
        persistDrawingState();
    }

    private String drawingImageDataUrl(Uri drawingUri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = getContentResolver().openInputStream(drawingUri)) {
            if (input == null) {
                throw new IllegalStateException("The drawing photo could not be read.");
            }
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IllegalStateException("The drawing photo could not be read.");
        }

        int sampleSize = 1;
        int largestOriginalSide = Math.max(bounds.outWidth, bounds.outHeight);
        while (largestOriginalSide / sampleSize > 2400) {
            sampleSize *= 2;
        }
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = sampleSize;
        Bitmap original;
        try (InputStream input = getContentResolver().openInputStream(drawingUri)) {
            if (input == null) {
                throw new IllegalStateException("The drawing photo could not be read.");
            }
            original = BitmapFactory.decodeStream(input, null, decodeOptions);
        }
        if (original == null) throw new IllegalStateException("The drawing photo could not be read.");
        original = applyDrawingOrientation(drawingUri, original);

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

    private void showDrawingZoom() {
        showDrawingZoom(activeDrawingIndex);
    }

    private void showDrawingZoom(int requestedDrawingIndex) {
        final int comparisonDrawingIndex = drawingPhotoUris.isEmpty()
                ? 0
                : Math.max(0, Math.min(requestedDrawingIndex, drawingPhotoUris.size() - 1));
        if (!drawingPhotoUris.isEmpty() && comparisonDrawingIndex != activeDrawingIndex) {
            syncActiveDrawingRecord();
            activeDrawingIndex = comparisonDrawingIndex;
            loadActiveDrawingResult(true);
        }
        final int totalOriginalDrawings = drawingPhotoUris.size();
        final ArrayList<Uri> originalUris = new ArrayList<>();
        if (!drawingPhotoUris.isEmpty()) {
            originalUris.add(drawingPhotoUris.get(comparisonDrawingIndex));
        }
        if (originalUris.isEmpty() && aiVerificationDrawing == null) {
            Toast.makeText(this, "No drawings are available yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (drawingZoomDialog != null) drawingZoomDialog.dismiss();

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(dp(12), dp(6), dp(12), dp(8));
        controls.setBackgroundColor(Color.rgb(91, 58, 41));

        TextView title = new TextView(this);
        title.setText("Compare drawings");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        controls.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        LinearLayout controlButtons = new LinearLayout(this);
        controlButtons.setOrientation(LinearLayout.HORIZONTAL);
        controlButtons.setGravity(Gravity.CENTER);

        Button editSizes = new Button(this);
        editSizes.setText("Edit sizes");
        editSizes.setAllCaps(false);
        editSizes.setEnabled(aiVerificationDrawing != null);
        controlButtons.addView(editSizes, new LinearLayout.LayoutParams(0, dp(48), 1f));

        Button reset = new Button(this);
        reset.setText("Reset zoom");
        reset.setAllCaps(false);
        controlButtons.addView(reset, new LinearLayout.LayoutParams(0, dp(48), 1f));

        Button close = new Button(this);
        close.setText("Close");
        close.setAllCaps(false);
        controlButtons.addView(close, new LinearLayout.LayoutParams(0, dp(48), 1f));
        controls.addView(controlButtons, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(controls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView instructions = new TextView(this);
        instructions.setText("Pinch or drag either picture separately");
        instructions.setTextColor(Color.WHITE);
        instructions.setTextSize(13);
        instructions.setGravity(Gravity.CENTER);
        instructions.setPadding(dp(8), dp(5), dp(8), dp(5));
        root.addView(instructions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        ArrayList<ZoomPanFrame> zoomFrames = new ArrayList<>();
        ArrayList<ImageView> decodedOriginalViews = new ArrayList<>();
        if (!originalUris.isEmpty()) {
            final int[] currentOriginalIndex = new int[]{0};
            LinearLayout originalHeading = new LinearLayout(this);
            originalHeading.setOrientation(LinearLayout.HORIZONTAL);
            originalHeading.setGravity(Gravity.CENTER);
            originalHeading.setBackgroundColor(Color.rgb(91, 58, 41));
            Button previousOriginal = new Button(this);
            previousOriginal.setText("‹");
            previousOriginal.setTextSize(20);
            previousOriginal.setAllCaps(false);
            TextView originalLabel = comparisonDrawingLabel("");
            Button nextOriginal = new Button(this);
            nextOriginal.setText("›");
            nextOriginal.setTextSize(20);
            nextOriginal.setAllCaps(false);
            previousOriginal.setVisibility(View.GONE);
            nextOriginal.setVisibility(View.GONE);
            originalHeading.addView(previousOriginal, new LinearLayout.LayoutParams(dp(54), dp(44)));
            originalHeading.addView(originalLabel, new LinearLayout.LayoutParams(0, dp(44), 1f));
            originalHeading.addView(nextOriginal, new LinearLayout.LayoutParams(dp(54), dp(44)));
            root.addView(originalHeading, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            ZoomPanFrame originalZoomFrame = new ZoomPanFrame(this);
            ImageView enlargedOriginal = new ImageView(this);
            decodedOriginalViews.add(enlargedOriginal);
            enlargedOriginal.setScaleType(ImageView.ScaleType.FIT_CENTER);
            enlargedOriginal.setBackgroundColor(Color.WHITE);
            Runnable updateOriginal = () -> {
                int index = currentOriginalIndex[0];
                loadDrawingIntoImageView(enlargedOriginal, originalUris.get(index), 1800);
                originalLabel.setText("Original drawing " + (comparisonDrawingIndex + 1)
                        + " of " + totalOriginalDrawings);
                enlargedOriginal.setContentDescription(
                        "Enlarged original countertop drawing " + (comparisonDrawingIndex + 1)
                                + " of " + totalOriginalDrawings);
                previousOriginal.setEnabled(index > 0);
                nextOriginal.setEnabled(index < originalUris.size() - 1);
                originalZoomFrame.resetZoom();
            };
            previousOriginal.setOnClickListener(v -> {
                if (currentOriginalIndex[0] <= 0) return;
                currentOriginalIndex[0]--;
                updateOriginal.run();
            });
            nextOriginal.setOnClickListener(v -> {
                if (currentOriginalIndex[0] >= originalUris.size() - 1) return;
                currentOriginalIndex[0]++;
                updateOriginal.run();
            });
            originalZoomFrame.setZoomContent(enlargedOriginal);
            updateOriginal.run();
            zoomFrames.add(originalZoomFrame);
            root.addView(originalZoomFrame, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f));
        }

        if (aiVerificationDrawing != null) {
            root.addView(comparisonDrawingLabel("AI verification redraw — verify every dimension"));
            ZoomPanFrame redrawZoomFrame = new ZoomPanFrame(this);
            VerificationDrawingView enlargedRedraw = new VerificationDrawingView(this);
            enlargedRedraw.setVerificationDrawing(aiVerificationDrawing);
            redrawZoomFrame.setZoomContent(enlargedRedraw);
            zoomFrames.add(redrawZoomFrame);
            root.addView(redrawZoomFrame, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f));
        }

        reset.setOnClickListener(v -> {
            for (ZoomPanFrame zoomFrame : zoomFrames) zoomFrame.resetZoom();
        });
        editSizes.setOnClickListener(v -> {
            dialog.dismiss();
            showVerificationEditor();
        });
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(ignored -> {
            for (ZoomPanFrame zoomFrame : zoomFrames) zoomFrame.removeAllViews();
            for (ImageView imageView : decodedOriginalViews) clearDecodedDrawing(imageView);
            if (drawingZoomDialog == dialog) drawingZoomDialog = null;
        });
        dialog.setContentView(root);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
        drawingZoomDialog = dialog;
    }

    private TextView comparisonDrawingLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(Color.WHITE);
        label.setTextSize(14);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setGravity(Gravity.CENTER);
        label.setBackgroundColor(Color.rgb(91, 58, 41));
        label.setPadding(dp(8), dp(5), dp(8), dp(5));
        label.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return label;
    }

    private void updateDrawingStatusText() {
        if (drawingStatus == null) return;
        StringBuilder status = new StringBuilder();
        status.append("Drawing ")
                .append(Math.min(activeDrawingIndex + 1, drawingPhotoUris.size()))
                .append(" of ")
                .append(drawingPhotoUris.size())
                .append(": ");
        if (!hasActiveDrawingResult()) {
            if (aiDrawingLastError.isEmpty()) {
                status.append("not analyzed yet");
            } else {
                status.append("needs another try — ").append(aiDrawingLastError);
            }
        } else if (aiDrawingCanCalculate) {
            status.append(aiDrawingEditedByUser ? "edited estimate " : "AI estimate ")
                    .append(number.format(aiDrawingSquareFeet))
                    .append(" sq ft");
        } else {
            status.append("AI could not calculate the square footage");
        }
        if (aiDrawingEditedByUser) {
            status.append("\nMeasurements edited by you");
        } else if (!aiDrawingConfidence.isEmpty()) {
            status.append("\nConfidence: ").append(aiDrawingConfidence);
        }
        if (!aiDrawingExplanation.isEmpty()) {
            status.append("\n").append(aiDrawingExplanation);
        }
        if (!aiDrawingMissingInformation.isEmpty()) {
            status.append("\nMissing information: ").append(aiDrawingMissingInformation);
        }
        if (hasActiveDrawingResult() && !aiDrawingLastError.isEmpty()) {
            status.append("\nLatest refresh problem: ").append(aiDrawingLastError)
                    .append(" The saved result was kept.");
        }

        int selectedCount = drawingRecords.size();
        int usableCount = usableDrawingCount();
        if (selectedCount > 1 && hasCompleteAiDrawingEstimate()) {
            status.append("\nCombined total: ")
                    .append(number.format(combinedAiDrawingSquareFeet()))
                    .append(" sq ft from ")
                    .append(selectedCount)
                    .append(" drawings");
        } else if (selectedCount > 1 && usableCount > 0) {
            status.append("\nCalculated subtotal: ")
                    .append(number.format(calculatedDrawingSubtotal()))
                    .append(" sq ft from ")
                    .append(usableCount)
                    .append(" of ")
                    .append(selectedCount)
                    .append(" drawings — incomplete and not used for pricing");
        } else if (selectedCount > 1) {
            status.append("\n")
                    .append(analyzedDrawingCount())
                    .append(" of ")
                    .append(selectedCount)
                    .append(" drawings analyzed — no combined price total yet");
        }
        drawingStatus.setText(status.toString());
    }

    private void showVerificationEditor() {
        if (drawingAnalysisInProgress) {
            Toast.makeText(
                    this,
                    "Wait for the current drawing analysis to finish before editing.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (aiVerificationDrawing == null) {
            Toast.makeText(this, "There is no AI redraw to edit yet.", Toast.LENGTH_LONG).show();
            return;
        }
        if (drawingEditDialog != null) drawingEditDialog.dismiss();

        final JSONObject workingDrawing;
        try {
            workingDrawing = new JSONObject(aiVerificationDrawing.toString());
        } catch (Exception e) {
            Toast.makeText(this, "The redraw could not be opened for editing.", Toast.LENGTH_LONG).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(248, 246, 243));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(10), dp(7), dp(8), dp(7));
        toolbar.setBackgroundColor(Color.rgb(91, 58, 41));

        TextView title = new TextView(this);
        title.setText("Edit AI redraw");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        toolbar.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button cancel = compactDialogButton("Cancel");
        Button save = compactDialogButton("Save");
        toolbar.addView(cancel);
        toolbar.addView(save);
        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView editorScroll = new ScrollView(this);
        editorScroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(10), dp(14), dp(24));
        editorScroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(editorScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView instructions = label(
                "Change a number below to correct the displayed dimension and square-foot total. "
                        + "The outline is a verification guide. Model-written descriptions are hidden. "
                        + "Only labels that you add yourself will appear on the redraw.");
        instructions.setTextSize(15);
        content.addView(instructions);

        VerificationDrawingView preview = new VerificationDrawingView(this);
        preview.setVerificationDrawing(workingDrawing);
        content.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(300)));

        content.addView(editorSectionTitle("Dimensions in inches"));
        JSONArray dimensions = workingDrawing.optJSONArray("dimensions");
        ArrayList<EditText> dimensionFields = new ArrayList<>();
        ArrayList<Double> originalDimensionValues = new ArrayList<>();
        ArrayList<Integer> dimensionIndexes = new ArrayList<>();
        if (dimensions == null || dimensions.length() == 0) {
            content.addView(label("No editable dimensions were found."));
        } else {
            for (int i = 0; i < Math.min(dimensions.length(), 50); i++) {
                final JSONObject dimension = dimensions.optJSONObject(i);
                if (dimension == null) continue;
                final double originalValue = dimension.optDouble("value_inches", 0);
                content.addView(label(dimensionEditorDescription(
                        dimension,
                        workingDrawing,
                        dimensionFields.size() + 1)));
                EditText field = input("Dimension in inches", decimalInput());
                field.setText(measurementValue(originalValue));
                field.setSelectAllOnFocus(true);
                field.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                field.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        double entered = positiveDrawingValue(s.toString());
                        if (entered <= 0) return;
                        try {
                            dimension.put("value_inches", entered);
                            dimension.put("label", formatDrawingDimension(entered));
                            preview.invalidate();
                        } catch (Exception ignored) {
                        }
                    }
                    @Override public void afterTextChanged(Editable s) {}
                });
                content.addView(field);
                dimensionFields.add(field);
                originalDimensionValues.add(originalValue);
                dimensionIndexes.add(i);
            }
        }

        content.addView(editorSectionTitle("Add a label"));
        TextView labelHelp = label(
                "Optional: choose one drawing piece and type a short label such as Island.");
        labelHelp.setTextSize(15);
        content.addView(labelHelp);

        JSONArray shapes = workingDrawing.optJSONArray("shapes");
        ArrayList<EditText> shapeLabelFields = new ArrayList<>();
        ArrayList<Integer> editableShapeIndexes = new ArrayList<>();
        ArrayList<Spinner> shapeTypeFields = new ArrayList<>();
        ArrayList<Integer> originalShapeSelections = new ArrayList<>();
        ArrayList<String> labelTargetChoices = new ArrayList<>();
        if (shapes != null) {
            for (int i = 0; i < Math.min(shapes.length(), 24); i++) {
                final JSONObject shape = shapes.optJSONObject(i);
                if (shape == null || DrawingRules.isBacksplash(shape)) continue;
                labelTargetChoices.add(shapeEditorDescription(
                        shape,
                        workingDrawing,
                        editableShapeIndexes.size() + 1)
                        .replace("Optional label ", "Drawing piece "));
                int originalShapeSelection = DrawingRules.shapeEditorSelection(shape);
                Spinner typeField = drawingChoiceSpinner(
                        new String[]{
                                "Countertop piece",
                                "Sink opening — not deducted",
                                "Cooktop opening — not deducted",
                                "Slide-in stove opening — deducted",
                                "Other opening"
                        },
                        originalShapeSelection);
                EditText field = input(
                        "Optional label, for example Island",
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                field.setText(shape.optString("user_label", ""));
                field.setSingleLine(true);
                field.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                field.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        try {
                            String clean = cleanDrawingLabel(s.toString());
                            if (clean.isEmpty()) shape.remove("user_label");
                            else shape.put("user_label", clean);
                            preview.invalidate();
                        } catch (Exception ignored) {
                        }
                    }
                    @Override public void afterTextChanged(Editable s) {}
                });
                editableShapeIndexes.add(i);
                shapeTypeFields.add(typeField);
                originalShapeSelections.add(originalShapeSelection);
                shapeLabelFields.add(field);
            }
        }
        if (editableShapeIndexes.isEmpty()) {
            content.addView(label("No countertop pieces or openings were available for a custom label."));
        } else {
            Spinner labelTarget = drawingChoiceSpinner(
                    labelTargetChoices.toArray(new String[0]),
                    0);
            EditText simpleLabelField = input(
                    "Optional label, for example Island",
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            simpleLabelField.setSingleLine(true);
            simpleLabelField.setText(shapeLabelFields.get(0).getText());
            final boolean[] changingLabelTarget = {false};
            simpleLabelField.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (changingLabelTarget[0]) return;
                    int selected = labelTarget.getSelectedItemPosition();
                    if (selected < 0 || selected >= shapeLabelFields.size()) return;
                    shapeLabelFields.get(selected).setText(s);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
            labelTarget.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(
                        android.widget.AdapterView<?> parent,
                        View view,
                        int position,
                        long id) {
                    if (position < 0 || position >= shapeLabelFields.size()) return;
                    changingLabelTarget[0] = true;
                    simpleLabelField.setText(shapeLabelFields.get(position).getText());
                    simpleLabelField.setSelection(simpleLabelField.getText().length());
                    changingLabelTarget[0] = false;
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
            content.addView(labelTarget);
            content.addView(simpleLabelField);
        }
        CheckBox allowUnlinkedPictureOnly = new CheckBox(this);

        ArrayList<Spinner> areaRuleFields = new ArrayList<>();
        ArrayList<Integer> areaRulePartIndexes = new ArrayList<>();
        ArrayList<Integer> originalAreaSelections = new ArrayList<>();
        if (aiDrawingCalculationParts != null) {
            for (int i = 0; i < Math.min(aiDrawingCalculationParts.length(), 40); i++) {
                JSONObject part = aiDrawingCalculationParts.optJSONObject(i);
                if (part == null) continue;
                int selected = DrawingRules.areaEditorSelection(part);
                Spinner areaRule = drawingChoiceSpinner(
                        new String[]{
                                "Include in square footage",
                                "Sink/cooktop or ignore — no square-foot change",
                                "Slide-in stove — deduct",
                                "Other opening/notch — deduct"
                        },
                        selected);
                areaRuleFields.add(areaRule);
                areaRulePartIndexes.add(i);
                originalAreaSelections.add(selected);
            }
        }
        Spinner addedAreaType = drawingChoiceSpinner(
                new String[]{
                        "No added correction",
                        "Countertop piece — include",
                        "Sink/cooktop or ignored item — no square-foot change",
                        "Slide-in stove — deduct",
                        "Other opening/notch — deduct"
                },
                0);
        EditText addedAreaLength = input("Missing area length in inches", decimalInput());
        EditText addedAreaWidth = input("Missing area width in inches", decimalInput());
        addedAreaLength.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        addedAreaWidth.setImeOptions(EditorInfo.IME_ACTION_DONE);

        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> saveVerificationEdits(
                dialog,
                dimensionFields,
                originalDimensionValues,
                dimensionIndexes,
                editableShapeIndexes,
                shapeTypeFields,
                originalShapeSelections,
                shapeLabelFields,
                allowUnlinkedPictureOnly,
                areaRuleFields,
                areaRulePartIndexes,
                originalAreaSelections,
                addedAreaType,
                addedAreaLength,
                addedAreaWidth));
        dialog.setOnDismissListener(ignored -> {
            hideKeyboard();
            if (drawingEditDialog == dialog) drawingEditDialog = null;
        });
        dialog.setContentView(root);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.rgb(248, 246, 243)));
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
        drawingEditDialog = dialog;
    }

    private Button compactDialogButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setTextColor(Color.rgb(91, 58, 41));
        button.setBackgroundColor(Color.rgb(239, 230, 220));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(76), dp(44));
        params.setMargins(dp(5), 0, 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private Spinner drawingChoiceSpinner(String[] choices, int selectedIndex) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Math.max(0, Math.min(selectedIndex, choices.length - 1)));
        spinner.setPadding(dp(8), dp(4), dp(8), dp(4));
        return spinner;
    }

    private TextView editorSectionTitle(String text) {
        TextView title = sectionHeader(text);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(14), 0, dp(4));
        title.setLayoutParams(params);
        return title;
    }

    private void saveVerificationEdits(
            Dialog dialog,
            ArrayList<EditText> dimensionFields,
            ArrayList<Double> originalDimensionValues,
            ArrayList<Integer> dimensionIndexes,
            ArrayList<Integer> editableShapeIndexes,
            ArrayList<Spinner> shapeTypeFields,
            ArrayList<Integer> originalShapeSelections,
            ArrayList<EditText> shapeLabelFields,
            CheckBox allowUnlinkedPictureOnly,
            ArrayList<Spinner> areaRuleFields,
            ArrayList<Integer> areaRulePartIndexes,
            ArrayList<Integer> originalAreaSelections,
            Spinner addedAreaType,
            EditText addedAreaLength,
            EditText addedAreaWidth) {
        if (aiVerificationDrawing == null) {
            dialog.dismiss();
            Toast.makeText(this, "The AI redraw is no longer available.", Toast.LENGTH_LONG).show();
            return;
        }

        final JSONObject candidateDrawing;
        final JSONArray candidateParts;
        try {
            candidateDrawing = new JSONObject(aiVerificationDrawing.toString());
            candidateParts = aiDrawingCalculationParts == null
                    ? new JSONArray()
                    : new JSONArray(aiDrawingCalculationParts.toString());
        } catch (Exception e) {
            Toast.makeText(this, "The redraw changes could not be prepared.", Toast.LENGTH_LONG).show();
            return;
        }

        JSONArray dimensions = candidateDrawing.optJSONArray("dimensions");
        if (dimensions == null) dimensions = new JSONArray();
        if (dimensionFields.size() != originalDimensionValues.size()
                || dimensionFields.size() != dimensionIndexes.size()) {
            Toast.makeText(this, "The redraw fields could not be matched.", Toast.LENGTH_LONG).show();
            return;
        }
        if (areaRuleFields.size() != areaRulePartIndexes.size()
                || areaRuleFields.size() != originalAreaSelections.size()) {
            Toast.makeText(this, "The square-foot controls could not be matched.", Toast.LENGTH_LONG).show();
            return;
        }

        // Validate every field before changing either the drawing or its calculation candidate.
        ArrayList<Double> enteredDimensionValues = new ArrayList<>();
        for (int fieldIndex = 0; fieldIndex < dimensionFields.size(); fieldIndex++) {
            EditText field = dimensionFields.get(fieldIndex);
            double entered = positiveDrawingValue(field.getText().toString());
            if (entered <= 0 || entered > 10000) {
                field.requestFocus();
                Toast.makeText(
                        this,
                        "Enter every dimension as a number greater than 0 and no more than 10,000 inches.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            enteredDimensionValues.add(entered);
        }

        ArrayList<String> enteredLabels = new ArrayList<>();
        for (int i = 0; i < shapeLabelFields.size(); i++) {
            String labelText = cleanDrawingLabel(shapeLabelFields.get(i).getText().toString());
            if (labelText.length() > 30) {
                shapeLabelFields.get(i).requestFocus();
                Toast.makeText(this, "Keep each drawing label to 30 characters or fewer.", Toast.LENGTH_LONG).show();
                return;
            }
            enteredLabels.add(labelText);
        }

        JSONArray candidateShapes = candidateDrawing.optJSONArray("shapes");
        if (shapeLabelFields.size() != editableShapeIndexes.size()
                || shapeTypeFields.size() != editableShapeIndexes.size()
                || originalShapeSelections.size() != editableShapeIndexes.size()
                || candidateShapes == null && !shapeLabelFields.isEmpty()) {
            Toast.makeText(this, "The redraw label fields could not be matched.", Toast.LENGTH_LONG).show();
            return;
        }

        boolean shapeHandlingChanged = false;
        JSONObject unlinkedChangedShape = null;
        int unlinkedChangedShapeSelection = -1;
        for (int i = 0; i < enteredLabels.size(); i++) {
            JSONObject shape = candidateShapes.optJSONObject(editableShapeIndexes.get(i));
            if (shape == null) {
                Toast.makeText(this, "One redraw label location is no longer available.", Toast.LENGTH_LONG).show();
                return;
            }
            int selectedShapeType = shapeTypeFields.get(i).getSelectedItemPosition();
            boolean shapeTypeChanged = selectedShapeType != originalShapeSelections.get(i);
            if (shapeTypeChanged) {
                DrawingRules.applyShapeEditorSelection(shape, selectedShapeType);
                int linkedPartIndex = applyLinkedShapeHandling(
                        shape,
                        candidateParts,
                        selectedShapeType);
                if (linkedPartIndex == DrawingRules.LINK_AMBIGUOUS) {
                    Toast.makeText(
                            this,
                            "That shape matches more than one calculation piece. Re-run AI before changing its type so no quote math is changed incorrectly.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (linkedPartIndex == DrawingRules.LINK_NOT_FOUND) {
                    if (unlinkedChangedShape != null) {
                        Toast.makeText(
                                this,
                                "Only one unlinked shape can be paired with one added correction at a time. Save one correction, then edit again.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    unlinkedChangedShape = shape;
                    unlinkedChangedShapeSelection = selectedShapeType;
                } else {
                    shapeHandlingChanged = true;
                }
            }
            try {
                String labelText = enteredLabels.get(i);
                if (labelText.isEmpty()) shape.remove("user_label");
                else shape.put("user_label", labelText);
            } catch (Exception ignored) {
            }
        }

        boolean handlingChanged = shapeHandlingChanged;
        ArrayList<Integer> changedAreaPartIndexes = new ArrayList<>();
        ArrayList<Integer> changedPartsWithoutShape = new ArrayList<>();
        for (int i = 0; i < areaRuleFields.size(); i++) {
            int partIndex = areaRulePartIndexes.get(i);
            JSONObject part = candidateParts.optJSONObject(partIndex);
            if (part == null) {
                Toast.makeText(this, "One calculation piece is no longer available.", Toast.LENGTH_LONG).show();
                return;
            }
            int selection = areaRuleFields.get(i).getSelectedItemPosition();
            if (selection != originalAreaSelections.get(i)) {
                handlingChanged = true;
                DrawingRules.applyAreaEditorSelection(part, selection);
                changedAreaPartIndexes.add(partIndex);
                int linkedShapeIndex = synchronizeLinkedShapeHandling(
                        part,
                        candidateShapes,
                        selection);
                if (linkedShapeIndex == DrawingRules.LINK_AMBIGUOUS) {
                    Toast.makeText(
                            this,
                            "That calculation piece matches more than one redraw shape. Re-run AI before changing its square-foot handling.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (linkedShapeIndex == DrawingRules.LINK_NOT_FOUND) {
                    changedPartsWithoutShape.add(partIndex);
                }
            }
        }

        int addedAreaSelection = addedAreaType.getSelectedItemPosition();
        if (unlinkedChangedShape != null) {
            int targetAreaSelection = areaSelectionForShape(unlinkedChangedShapeSelection);
            int compatiblePartIndex = findCompatibleUnlinkedPart(
                    candidateParts,
                    candidateShapes,
                    targetAreaSelection,
                    changedAreaPartIndexes);
            if (compatiblePartIndex >= 0) {
                if (addedAreaSelection == targetAreaSelection + 1) {
                    Toast.makeText(
                            this,
                            "A matching calculation piece already exists. Clear the added correction so this opening is not counted twice.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    String linkId = nextUserLinkId(candidateParts, candidateShapes);
                    unlinkedChangedShape.put("link_id", linkId);
                    candidateParts.optJSONObject(compatiblePartIndex).put("link_id", linkId);
                    changedPartsWithoutShape.remove(Integer.valueOf(compatiblePartIndex));
                    unlinkedChangedShape = null;
                } catch (Exception ignored) {
                    Toast.makeText(this, "The shape and calculation piece could not be linked.", Toast.LENGTH_LONG).show();
                    return;
                }
            } else if (compatiblePartIndex == DrawingRules.LINK_AMBIGUOUS) {
                if (!allowUnlinkedPictureOnly.isChecked() || addedAreaSelection > 0) {
                    Toast.makeText(
                            this,
                            "More than one calculation piece could match that shape. Check picture-only and clear the added correction, or re-run AI.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                unlinkedChangedShape = null;
            } else if (addedAreaSelection
                    != addedAreaSelectionForShape(unlinkedChangedShapeSelection)) {
                if (allowUnlinkedPictureOnly.isChecked()) {
                    unlinkedChangedShape = null;
                } else {
                    Toast.makeText(
                            this,
                            "That shape is not linked to one calculation piece. Choose a matching existing formula type, add the matching missing correction, or check picture-only.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
        boolean addedCorrectionWithoutShape = false;
        if (addedAreaSelection > 0) {
            if (candidateParts.length() >= 40) {
                Toast.makeText(
                        this,
                        "This drawing already has the maximum number of calculation pieces.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            double length = positiveDrawingValue(addedAreaLength.getText().toString());
            double width = positiveDrawingValue(addedAreaWidth.getText().toString());
            if (length <= 0 || length > 10000) {
                addedAreaLength.requestFocus();
                Toast.makeText(
                        this,
                        "Enter the missing area's length as a number from 0 to 10,000 inches.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (width <= 0 || width > 10000) {
                addedAreaWidth.requestFocus();
                Toast.makeText(
                        this,
                        "Enter the missing area's width as a number from 0 to 10,000 inches.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            JSONObject addedPart = new JSONObject();
            try {
                String addedPartId = nextUserAreaId(candidateParts);
                addedPart.put("id", addedPartId);
                addedPart.put("length_inches", length);
                addedPart.put("width_inches", width);
                addedPart.put("quantity", 1);
                addedPart.put("user_added", true);
                if (unlinkedChangedShape != null) {
                    unlinkedChangedShape.put("link_id", addedPartId);
                    addedPart.put("link_id", addedPartId);
                } else {
                    addedCorrectionWithoutShape = true;
                }
                DrawingRules.applyAreaEditorSelection(addedPart, addedAreaSelection - 1);
                candidateParts.put(addedPart);
            } catch (Exception ignored) {
                Toast.makeText(this, "The added area could not be saved.", Toast.LENGTH_LONG).show();
                return;
            }
            handlingChanged = true;
        }

        // Duplicate arrows that control the same formula value must receive the same correction.
        HashMap<String, Double> changedTargets = new HashMap<>();
        boolean dimensionCalculationChanged = false;
        for (int fieldIndex = 0; fieldIndex < dimensionFields.size(); fieldIndex++) {
            JSONObject dimension = dimensions.optJSONObject(dimensionIndexes.get(fieldIndex));
            if (dimension == null) {
                Toast.makeText(this, "One redraw dimension is no longer available.", Toast.LENGTH_LONG).show();
                return;
            }
            double entered = enteredDimensionValues.get(fieldIndex);
            double original = originalDimensionValues.get(fieldIndex);
            if (Math.abs(entered - original) <= 0.0001) continue;
            String role = dimension.optString("role", "other");
            JSONArray partIds = dimension.optJSONArray("part_ids");
            boolean affectsCalculation = !"other".equals(role)
                    && partIds != null
                    && partIds.length() > 0;
            if (affectsCalculation && candidateParts.length() == 0) {
                dimensionFields.get(fieldIndex).requestFocus();
                Toast.makeText(
                        this,
                        "Re-run the AI analysis before changing a calculated dimension.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (affectsCalculation
                    && !registerChangedDimensionTargets(changedTargets, dimension, entered)) {
                dimensionFields.get(fieldIndex).requestFocus();
                Toast.makeText(
                        this,
                        "Two fields control the same measurement. Enter the same number in both fields.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            dimensionCalculationChanged = dimensionCalculationChanged || affectsCalculation;
        }

        for (int fieldIndex = 0; fieldIndex < dimensionFields.size(); fieldIndex++) {
            JSONObject dimension = dimensions.optJSONObject(dimensionIndexes.get(fieldIndex));
            if (dimension == null) continue;
            double entered = enteredDimensionValues.get(fieldIndex);
            double original = originalDimensionValues.get(fieldIndex);
            if (Math.abs(entered - original) > 0.0001) {
                String role = dimension.optString("role", "other");
                JSONArray partIds = dimension.optJSONArray("part_ids");
                boolean affectsCalculation = !"other".equals(role)
                        && partIds != null
                        && partIds.length() > 0;
                int updatedParts = updatePartsFromDimension(candidateParts, dimension, entered);
                if (affectsCalculation && updatedParts == 0) {
                    dimensionFields.get(fieldIndex).requestFocus();
                    Toast.makeText(
                            this,
                            "That dimension is not linked to a calculation piece. Re-run the AI analysis.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
            try {
                dimension.put("value_inches", entered);
                dimension.put("label", formatDrawingDimension(entered));
            } catch (Exception ignored) {
            }
        }

        double updatedSquareFeet = aiDrawingSquareFeet;
        String updatedExplanation = aiDrawingExplanation;
        boolean calculationChanged = handlingChanged || dimensionCalculationChanged;
        if (calculationChanged) {
            synchronizeLinkedDrawingDimensions(candidateDrawing, candidateParts);
            if (dimensionCalculationChanged
                    && !changedDimensionTargetsMatchDrawing(
                    candidateDrawing,
                    candidateParts,
                    changedTargets)) {
                Toast.makeText(
                        this,
                        "Those edits made two linked measurements disagree. Check the dimension fields.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            updatedSquareFeet = recalculateDrawingSquareFeet(candidateParts);
            if (updatedSquareFeet <= 0) {
                Toast.makeText(
                        this,
                        "Those edits do not produce a positive countertop area. Check the dimensions.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            updatedExplanation = buildEditedDrawingExplanation(candidateParts, updatedSquareFeet);
        }

        if (calculationChanged) {
            aiDrawingSquareFeet = updatedSquareFeet;
            aiDrawingCanCalculate = true;
            aiDrawingExplanation = updatedExplanation;
        }
        aiDrawingEditedByUser = true;
        aiDrawingCalculationParts = candidateParts;
        aiVerificationDrawing = candidateDrawing;
        verificationDrawingView.setVerificationDrawing(candidateDrawing);
        persistDrawingState();
        updateDrawingStatusText();
        calculateAndDisplay(false);
        dialog.dismiss();
        showStep();
        boolean hasUnmatchedFormulaVisual = !changedPartsWithoutShape.isEmpty()
                || addedCorrectionWithoutShape;
        Toast.makeText(
                this,
                hasUnmatchedFormulaVisual
                        ? "Redraw saved. One square-foot correction has no matching shape, so verify the picture and formula separately."
                        : "Redraw saved.",
                hasUnmatchedFormulaVisual ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }

    private double positiveDrawingValue(String text) {
        try {
            double parsed = Double.parseDouble(text.trim());
            return isFiniteDrawingNumber(parsed) && parsed > 0 ? parsed : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private boolean isFiniteDrawingNumber(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private String cleanDrawingLabel(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }

    private String formatDrawingDimension(double inches) {
        return measurementValue(inches) + "\"";
    }

    private String dimensionEditorDescription(
            JSONObject dimension,
            JSONObject drawing,
            int numberIndex) {
        double canvasWidth = drawing.optDouble("canvas_width", 1000);
        double canvasHeight = drawing.optDouble("canvas_height", 700);
        double x1 = dimension.optDouble("x1", 0);
        double y1 = dimension.optDouble("y1", 0);
        double x2 = dimension.optDouble("x2", 0);
        double y2 = dimension.optDouble("y2", 0);
        String position = drawingPosition((x1 + x2) / 2, (y1 + y2) / 2, canvasWidth, canvasHeight);
        String direction = Math.abs(x2 - x1) >= Math.abs(y2 - y1)
                ? "horizontal"
                : "vertical";
        String role = dimension.optString("role", "other");
        String purpose;
        if ("length".equals(role)) purpose = "length";
        else if ("width".equals(role)) purpose = "width";
        else if ("both".equals(role)) purpose = "length and width";
        else purpose = "reference only";
        return "Dimension " + numberIndex + " — " + position + " " + direction + " " + purpose;
    }

    private String shapeEditorDescription(JSONObject shape, JSONObject drawing, int numberIndex) {
        JSONArray points = shape.optJSONArray("points");
        double centerX = 0;
        double centerY = 0;
        int count = 0;
        if (points != null) {
            for (int i = 0; i < Math.min(points.length(), 16); i++) {
                JSONObject point = points.optJSONObject(i);
                if (point == null) continue;
                centerX += point.optDouble("x", 0);
                centerY += point.optDouble("y", 0);
                count++;
            }
        }
        if (count > 0) {
            centerX /= count;
            centerY /= count;
        }
        double canvasWidth = drawing.optDouble("canvas_width", 1000);
        double canvasHeight = drawing.optDouble("canvas_height", 700);
        String kind = "opening".equals(shape.optString("kind")) ? "opening" : "countertop piece";
        return "Optional label " + numberIndex + " — "
                + drawingPosition(centerX, centerY, canvasWidth, canvasHeight)
                + " " + kind;
    }

    private String calculationPartDescription(JSONObject part, int numberIndex) {
        double length = part.optDouble("length_inches", 0);
        double width = part.optDouble("width_inches", 0);
        double quantityValue = part.optDouble("quantity", 1);
        StringBuilder description = new StringBuilder("Calculation piece ")
                .append(numberIndex)
                .append(" — ")
                .append(measurementValue(length))
                .append(" × ")
                .append(measurementValue(width))
                .append(" in");
        if (Math.abs(quantityValue - 1) > 0.0001) {
            description.append(" × ").append(measurementValue(quantityValue));
        }
        return description.toString();
    }

    private int applyLinkedShapeHandling(
            JSONObject shape,
            JSONArray calculationParts,
            int shapeSelection) {
        int partIndex = DrawingRules.uniqueLinkedIndex(shape, calculationParts);
        if (partIndex < 0) return partIndex;
        DrawingRules.applyAreaEditorSelection(
                calculationParts.optJSONObject(partIndex),
                areaSelectionForShape(shapeSelection));
        return partIndex;
    }

    private int synchronizeLinkedShapeHandling(
            JSONObject part,
            JSONArray shapes,
            int areaSelection) {
        int shapeIndex = DrawingRules.uniqueLinkedIndex(part, shapes);
        if (shapeIndex < 0) return shapeIndex;
        JSONObject shape = shapes.optJSONObject(shapeIndex);
        if (shape == null) return DrawingRules.LINK_NOT_FOUND;
        int shapeSelection;
        if (areaSelection == DrawingRules.AREA_IGNORE_SINK_OR_COOKTOP) {
            shapeSelection = DrawingRules.shapeEditorSelection(shape)
                    == DrawingRules.SHAPE_COOKTOP
                    ? DrawingRules.SHAPE_COOKTOP
                    : DrawingRules.SHAPE_SINK;
        } else if (areaSelection == DrawingRules.AREA_DEDUCT_STOVE) {
            shapeSelection = DrawingRules.SHAPE_STOVE;
        } else if (areaSelection == DrawingRules.AREA_DEDUCT_OTHER) {
            shapeSelection = DrawingRules.SHAPE_OTHER_OPENING;
        } else {
            shapeSelection = DrawingRules.SHAPE_COUNTERTOP;
        }
        DrawingRules.applyShapeEditorSelection(shape, shapeSelection);
        return shapeIndex;
    }

    private int areaSelectionForShape(int shapeSelection) {
        if (shapeSelection == DrawingRules.SHAPE_SINK
                || shapeSelection == DrawingRules.SHAPE_COOKTOP) {
            return DrawingRules.AREA_IGNORE_SINK_OR_COOKTOP;
        }
        if (shapeSelection == DrawingRules.SHAPE_STOVE) {
            return DrawingRules.AREA_DEDUCT_STOVE;
        }
        if (shapeSelection == DrawingRules.SHAPE_OTHER_OPENING) {
            return DrawingRules.AREA_DEDUCT_OTHER;
        }
        return DrawingRules.AREA_INCLUDE;
    }

    private int addedAreaSelectionForShape(int shapeSelection) {
        return areaSelectionForShape(shapeSelection) + 1;
    }

    private int findCompatibleUnlinkedPart(
            JSONArray parts,
            JSONArray shapes,
            int targetAreaSelection,
            ArrayList<Integer> changedPartIndexes) {
        int preferred = uniqueCompatiblePart(
                parts,
                shapes,
                targetAreaSelection,
                changedPartIndexes);
        if (preferred != DrawingRules.LINK_NOT_FOUND) return preferred;
        return uniqueCompatiblePart(parts, shapes, targetAreaSelection, null);
    }

    private int uniqueCompatiblePart(
            JSONArray parts,
            JSONArray shapes,
            int targetAreaSelection,
            ArrayList<Integer> restrictedIndexes) {
        if (parts == null) return DrawingRules.LINK_NOT_FOUND;
        int match = DrawingRules.LINK_NOT_FOUND;
        int limit = restrictedIndexes == null
                ? Math.min(parts.length(), 40)
                : restrictedIndexes.size();
        for (int position = 0; position < limit; position++) {
            int partIndex = restrictedIndexes == null
                    ? position
                    : restrictedIndexes.get(position);
            JSONObject part = parts.optJSONObject(partIndex);
            if (part == null
                    || DrawingRules.areaEditorSelection(part) != targetAreaSelection
                    || DrawingRules.uniqueLinkedIndex(part, shapes)
                    != DrawingRules.LINK_NOT_FOUND) continue;
            if (match != DrawingRules.LINK_NOT_FOUND) return DrawingRules.LINK_AMBIGUOUS;
            match = partIndex;
        }
        return match;
    }

    private String nextUserLinkId(JSONArray parts, JSONArray shapes) {
        int number = 1;
        while (true) {
            String candidate = "user_link_" + number;
            boolean exists = false;
            if (parts != null) {
                for (int i = 0; i < Math.min(parts.length(), 40); i++) {
                    JSONObject part = parts.optJSONObject(i);
                    if (part != null && candidate.equals(part.optString("link_id", ""))) {
                        exists = true;
                        break;
                    }
                }
            }
            if (!exists && shapes != null) {
                for (int i = 0; i < Math.min(shapes.length(), 24); i++) {
                    JSONObject shape = shapes.optJSONObject(i);
                    if (shape != null && candidate.equals(shape.optString("link_id", ""))) {
                        exists = true;
                        break;
                    }
                }
            }
            if (!exists) return candidate;
            number++;
        }
    }

    private String nextUserAreaId(JSONArray parts) {
        int number = Math.max(1, parts == null ? 1 : parts.length() + 1);
        while (true) {
            String candidate = "user_area_" + number;
            boolean exists = false;
            if (parts != null) {
                for (int i = 0; i < Math.min(parts.length(), 40); i++) {
                    JSONObject part = parts.optJSONObject(i);
                    if (part != null && (candidate.equals(part.optString("id", ""))
                            || candidate.equals(part.optString("link_id", "")))) {
                        exists = true;
                        break;
                    }
                }
            }
            if (!exists) return candidate;
            number++;
        }
    }

    private String drawingPosition(
            double centerX,
            double centerY,
            double canvasWidth,
            double canvasHeight) {
        String vertical = centerY < canvasHeight * 0.36
                ? "top"
                : centerY > canvasHeight * 0.64 ? "bottom" : "middle";
        String horizontal = centerX < canvasWidth * 0.36
                ? "left"
                : centerX > canvasWidth * 0.64 ? "right" : "center";
        return vertical + "-" + horizontal;
    }

    private boolean registerChangedDimensionTargets(
            HashMap<String, Double> changedTargets,
            JSONObject dimension,
            double entered) {
        String role = dimension.optString("role", "other");
        JSONArray partIds = dimension.optJSONArray("part_ids");
        if (partIds == null) return true;
        for (int i = 0; i < Math.min(partIds.length(), 12); i++) {
            String partId = partIds.optString(i, "").trim();
            if (partId.isEmpty()) continue;
            if (("length".equals(role) || "both".equals(role))
                    && !registerChangedDimensionTarget(
                    changedTargets,
                    drawingTargetKey(partId, "length"),
                    entered)) return false;
            if (("width".equals(role) || "both".equals(role))
                    && !registerChangedDimensionTarget(
                    changedTargets,
                    drawingTargetKey(partId, "width"),
                    entered)) return false;
        }
        return true;
    }

    private boolean registerChangedDimensionTarget(
            HashMap<String, Double> changedTargets,
            String key,
            double entered) {
        Double existing = changedTargets.get(key);
        if (existing != null && Math.abs(existing - entered) > 0.05) return false;
        changedTargets.put(key, entered);
        return true;
    }

    private String drawingTargetKey(String partId, String property) {
        return partId + "\u0000" + property;
    }

    private boolean changedDimensionTargetsMatchDrawing(
            JSONObject drawing,
            JSONArray parts,
            HashMap<String, Double> changedTargets) {
        JSONArray dimensions = drawing.optJSONArray("dimensions");
        if (dimensions == null) return changedTargets.isEmpty();
        for (int i = 0; i < Math.min(dimensions.length(), 50); i++) {
            JSONObject dimension = dimensions.optJSONObject(i);
            if (dimension == null) continue;
            String role = dimension.optString("role", "other");
            if ("other".equals(role)) continue;
            double displayed = dimension.optDouble("value_inches", Double.NaN);
            if (!isFiniteDrawingNumber(displayed) || displayed <= 0) return false;
            JSONArray partIds = dimension.optJSONArray("part_ids");
            if (partIds == null) continue;
            for (int idIndex = 0; idIndex < Math.min(partIds.length(), 12); idIndex++) {
                String partId = partIds.optString(idIndex, "");
                JSONObject part = findDrawingCalculationPart(parts, partId);
                if (part == null) continue;
                if (("length".equals(role) || "both".equals(role))
                        && changedTargets.containsKey(drawingTargetKey(partId, "length"))
                        && Math.abs(displayed - part.optDouble("length_inches", Double.NaN)) > 0.05) {
                    return false;
                }
                if (("width".equals(role) || "both".equals(role))
                        && changedTargets.containsKey(drawingTargetKey(partId, "width"))
                        && Math.abs(displayed - part.optDouble("width_inches", Double.NaN)) > 0.05) {
                    return false;
                }
            }
        }
        return true;
    }

    private int updatePartsFromDimension(
            JSONArray parts,
            JSONObject dimension,
            double entered) {
        String role = dimension.optString("role", "other");
        if ("other".equals(role)) return 0;
        JSONArray partIds = dimension.optJSONArray("part_ids");
        if (partIds == null) return 0;
        int updatedParts = 0;
        for (int idIndex = 0; idIndex < Math.min(partIds.length(), 12); idIndex++) {
            String id = partIds.optString(idIndex, "");
            JSONObject part = findDrawingCalculationPart(parts, id);
            if (part == null) continue;
            try {
                if ("length".equals(role) || "both".equals(role)) {
                    part.put("length_inches", entered);
                }
                if ("width".equals(role) || "both".equals(role)) {
                    part.put("width_inches", entered);
                }
                updatedParts++;
            } catch (Exception ignored) {
            }
        }
        return updatedParts;
    }

    private JSONObject findDrawingCalculationPart(JSONArray parts, String id) {
        if (id == null || id.trim().isEmpty()) return null;
        for (int i = 0; i < Math.min(parts.length(), 40); i++) {
            JSONObject part = parts.optJSONObject(i);
            if (part != null && id.equals(part.optString("id"))) return part;
        }
        return null;
    }

    private void synchronizeLinkedDrawingDimensions(JSONObject drawing, JSONArray parts) {
        JSONArray dimensions = drawing.optJSONArray("dimensions");
        if (dimensions == null) return;
        for (int i = 0; i < Math.min(dimensions.length(), 50); i++) {
            JSONObject dimension = dimensions.optJSONObject(i);
            if (dimension == null) continue;
            double linkedValue = linkedDrawingDimensionValue(parts, dimension);
            if (!isFiniteDrawingNumber(linkedValue) || linkedValue <= 0) continue;
            try {
                dimension.put("value_inches", linkedValue);
                dimension.put("label", formatDrawingDimension(linkedValue));
            } catch (Exception ignored) {
            }
        }
    }

    private double linkedDrawingDimensionValue(JSONArray parts, JSONObject dimension) {
        String role = dimension.optString("role", "other");
        if ("other".equals(role)) return Double.NaN;
        JSONArray partIds = dimension.optJSONArray("part_ids");
        if (partIds == null || partIds.length() == 0) return Double.NaN;
        double result = Double.NaN;
        for (int i = 0; i < Math.min(partIds.length(), 12); i++) {
            JSONObject part = findDrawingCalculationPart(parts, partIds.optString(i, ""));
            if (part == null) return Double.NaN;
            double candidate;
            if ("length".equals(role)) {
                candidate = part.optDouble("length_inches", Double.NaN);
            } else if ("width".equals(role)) {
                candidate = part.optDouble("width_inches", Double.NaN);
            } else {
                double length = part.optDouble("length_inches", Double.NaN);
                double width = part.optDouble("width_inches", Double.NaN);
                if (!isFiniteDrawingNumber(length)
                        || !isFiniteDrawingNumber(width)
                        || Math.abs(length - width) > 0.05) return Double.NaN;
                candidate = length;
            }
            if (!isFiniteDrawingNumber(candidate) || candidate <= 0) return Double.NaN;
            if (isFiniteDrawingNumber(result) && Math.abs(result - candidate) > 0.05) {
                return Double.NaN;
            }
            result = candidate;
        }
        return result;
    }

    private double recalculateDrawingSquareFeet(JSONArray parts) {
        return DrawingMath.squareFeet(parts);
    }

    private String buildEditedDrawingExplanation(JSONArray parts, double squareFeet) {
        StringBuilder explanation = new StringBuilder("Edited measurements: ");
        int added = 0;
        int subtracted = 0;
        int described = 0;
        for (int i = 0; i < Math.min(parts.length(), 40); i++) {
            JSONObject part = parts.optJSONObject(i);
            if (part == null) continue;
            String feature = DrawingRules.featureType(part);
            boolean ignoredCutout = DrawingRules.FEATURE_SINK.equals(feature)
                    || DrawingRules.FEATURE_COOKTOP.equals(feature);
            boolean subtract = DrawingRules.FEATURE_STOVE.equals(feature)
                    || "subtract".equals(part.optString("operation"));
            String genericName;
            if (ignoredCutout) genericName = "Sink/cooktop cutout";
            else if (DrawingRules.FEATURE_STOVE.equals(feature)) {
                genericName = "Slide-in stove opening " + (++subtracted);
            } else if (subtract) genericName = "Opening " + (++subtracted);
            else genericName = "Piece " + (++added);
            double length = part.optDouble("length_inches", 0);
            double width = part.optDouble("width_inches", 0);
            double quantityValue = part.optDouble("quantity", 1);
            double area = length * width * quantityValue;
            if (described++ > 0) explanation.append("; ");
            explanation.append(ignoredCutout ? "do not deduct " : subtract ? "subtract " : "")
                    .append(genericName)
                    .append(" ")
                    .append(measurementValue(length))
                    .append(" × ")
                    .append(measurementValue(width));
            if (Math.abs(quantityValue - 1) > 0.0001) {
                explanation.append(" × ").append(measurementValue(quantityValue));
            }
            explanation.append(" = ").append(measurementValue(area)).append(" sq in");
        }
        explanation.append(". Total = ")
                .append(number.format(squareFeet))
                .append(" sq ft. Verify every edited dimension before pricing.");
        return explanation.toString();
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
                getContentResolver().takePersistableUriPermission(
                        selectedPhotoUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
            }
            addCountertopPhoto(selectedPhotoUri);
        }
        if (requestCode == TAKE_COUNTERTOP_PHOTO) {
            pendingCameraCapture = 0;
            Uri completedCountertopCapture = selectedPhotoUri;
            if (resultCode == RESULT_OK
                    && completedCountertopCapture != null
                    && isReadableDrawingUri(completedCountertopCapture)) {
                addCountertopPhoto(completedCountertopCapture);
                completedCountertopCapture = null;
            } else if (resultCode == RESULT_OK && data != null && data.getExtras() != null
                    && data.getExtras().get("data") instanceof Bitmap) {
                Uri photoUri = saveCameraBitmap(
                        (Bitmap) data.getExtras().get("data"),
                        "countertop_photos",
                        "countertop-photo-");
                if (photoUri != null) {
                    addCountertopPhoto(photoUri);
                } else {
                    Toast.makeText(this, "The countertop photo could not be saved.", Toast.LENGTH_LONG).show();
                }
            }
            if (completedCountertopCapture != null
                    && !countertopPhotoUris.contains(completedCountertopCapture)) {
                if (completedCountertopCapture.equals(selectedPhotoUri)) {
                    selectedPhotoUri = countertopPhotoUris.isEmpty()
                            ? null
                            : countertopPhotoUris.get(countertopPhotoUris.size() - 1);
                }
                releaseUnusedPhotoUri(completedCountertopCapture);
            }
        }
        if (requestCode == PICK_DRAWING_IMAGE && resultCode == RESULT_OK && data != null) {
            ArrayList<Uri> selectedDrawingUris = new ArrayList<>();
            ClipData selectedDrawings = data.getClipData();
            if (selectedDrawings != null) {
                for (int i = 0; i < selectedDrawings.getItemCount(); i++) {
                    Uri uri = selectedDrawings.getItemAt(i).getUri();
                    if (uri != null && !selectedDrawingUris.contains(uri)) {
                        selectedDrawingUris.add(uri);
                    }
                }
            } else if (data.getData() != null) {
                selectedDrawingUris.add(data.getData());
            }
            for (Uri uri : selectedDrawingUris) {
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {
                }
            }
            if (!selectedDrawingUris.isEmpty()) addDrawingPhotos(selectedDrawingUris);
        }
        if (requestCode == TAKE_DRAWING_PHOTO) {
            pendingCameraCapture = 0;
            Uri completedDrawingCapture = pendingDrawingCaptureUri;
            pendingDrawingCaptureUri = null;
            if (resultCode == RESULT_OK
                    && completedDrawingCapture != null
                    && isReadableDrawingUri(completedDrawingCapture)) {
                ArrayList<Uri> capturedDrawing = new ArrayList<>();
                capturedDrawing.add(completedDrawingCapture);
                addDrawingPhotos(capturedDrawing);
                completedDrawingCapture = null;
            } else if (resultCode == RESULT_OK && data != null && data.getExtras() != null
                    && data.getExtras().get("data") instanceof Bitmap) {
                Uri savedDrawingUri = saveCameraBitmap(
                        (Bitmap) data.getExtras().get("data"),
                        "drawing_photos",
                        "countertop-drawing-");
                if (savedDrawingUri != null) {
                    ArrayList<Uri> capturedDrawing = new ArrayList<>();
                    capturedDrawing.add(savedDrawingUri);
                    addDrawingPhotos(capturedDrawing);
                } else {
                    drawingStatus.setText(drawingPhotoUris.isEmpty()
                            ? "The drawing photo could not be saved."
                            : "The new photo could not be saved. Your existing drawings are still ready.");
                }
            }
            if (completedDrawingCapture != null) {
                releaseUnusedPhotoUri(completedDrawingCapture);
            }
        }
    }

    private void addCountertopPhoto(Uri photoUri) {
        selectedPhotoUri = photoUri;
        if (!countertopPhotoUris.contains(photoUri)) countertopPhotoUris.add(photoUri);
        roomPhoto.setImageURI(photoUri);
        photoStatus.setText(countertopPhotoUris.size() + " countertop photo"
                + (countertopPhotoUris.size() == 1 ? " is" : "s are")
                + " ready to attach to the email.");
        photoAccordionOpen = 1;
        showStep();
    }

    private Uri saveCameraBitmap(Bitmap bitmap, String folder, String prefix) {
        try {
            File photoDirectory = new File(getFilesDir(), folder);
            if (!photoDirectory.exists() && !photoDirectory.mkdirs()) return null;
            File photoFile = new File(photoDirectory, prefix + System.currentTimeMillis() + ".jpg");
            try (java.io.FileOutputStream output = new java.io.FileOutputStream(photoFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output);
            }
            return FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void releaseUnusedPhotoUri(Uri uri) {
        if (uri == null
                || drawingPhotoUris.contains(uri)
                || countertopPhotoUris.contains(uri)
                || uri.equals(selectedPhotoUri)
                || uri.equals(pendingDrawingCaptureUri)) return;
        try {
            if ((getPackageName() + ".fileprovider").equals(uri.getAuthority())) {
                // Delete only an app-owned camera copy. FileProvider restricts this call to
                // the narrow cache/files paths declared in file_paths.xml.
                getContentResolver().delete(uri, null, null);
            } else {
                // Never delete a gallery original. Release only this app's saved read grant.
                getContentResolver().releasePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        } catch (Exception ignored) {
            // The provider may already have removed the file or may not expose a saved grant.
        }
    }

    private void sendQuoteEmail() {
        if (!drawingRecords.isEmpty()
                && !hasCompleteAiDrawingEstimate()
                && manualCountertopSquareFeet() <= 0) {
            Toast.makeText(
                    this,
                    "The drawing estimate is incomplete and cannot be used for a quote yet. Re-run AI, correct it in the redraw editor, or enter the countertop measurements manually.",
                    Toast.LENGTH_LONG).show();
            return;
        }
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
        if (estimate.hasAiDrawings) {
            body.append("Verified drawing square feet (opening rules already applied): ")
                    .append(number.format(estimate.aiSquareFeet)).append("\n");
        }
        if (estimate.manualSquareFeet > 0) {
            body.append("Additional manual square feet: ")
                    .append(number.format(estimate.manualSquareFeet)).append("\n");
        }
        body.append("Gross square feet: ").append(number.format(estimate.gross)).append("\n");
        if (!estimate.hasAiDrawings) {
            body.append("Stove opening subtracted: ")
                    .append(number.format(estimate.stove)).append(" sq ft\n");
        }
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
        email.setType(countertopPhotoUris.isEmpty() ? "text/plain" : "image/*");
        if (!to.isEmpty() && to.contains("@")) {
            email.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
        }
        email.putExtra(Intent.EXTRA_SUBJECT, "New countertop quote request - " + text(customerName));
        email.putExtra(Intent.EXTRA_TEXT, body.toString());
        if (!countertopPhotoUris.isEmpty()) {
            if (countertopPhotoUris.size() == 1) {
                email.putExtra(Intent.EXTRA_STREAM, countertopPhotoUris.get(0));
                email.setClipData(ClipData.newUri(
                        getContentResolver(), "Countertop photo", countertopPhotoUris.get(0)));
            } else {
                email.setAction(Intent.ACTION_SEND_MULTIPLE);
                email.putParcelableArrayListExtra(Intent.EXTRA_STREAM, new ArrayList<>(countertopPhotoUris));
                ClipData photoData = ClipData.newRawUri("Countertop photos", countertopPhotoUris.get(0));
                for (int i = 1; i < countertopPhotoUris.size(); i++) {
                    photoData.addItem(new ClipData.Item(countertopPhotoUris.get(i)));
                }
                email.setClipData(photoData);
            }
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
                .setMessage("This clears the current customer, scanned slabs, measurements, saved photos, and drawings. Gallery originals stay on your phone. The office email stays saved.")
                .setPositiveButton("Clear", (dialog, which) -> resetQuote())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetQuote() {
        cancelDrawingAnalysis();
        clearAiDrawingResult();
        dismissDrawingDialogs();
        ArrayList<Uri> removedPhotoUris = new ArrayList<>(countertopPhotoUris);
        for (Uri uri : drawingPhotoUris) {
            if (!removedPhotoUris.contains(uri)) removedPhotoUris.add(uri);
        }
        slabs.clear();
        sections.clear();
        selectedPhotoUri = null;
        countertopPhotoUris.clear();
        drawingPhotoUris.clear();
        drawingRecords.clear();
        pendingDrawingCaptureUri = null;
        displayedDrawingPreviewUri = null;
        activeDrawingIndex = 0;
        drawingInputRevision++;
        pendingCameraCapture = 0;
        for (Uri uri : removedPhotoUris) releaseUnusedPhotoUri(uri);
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
        cabinetInQuantity.setText("0");
        cabinetsApproximateDate.setText("");
        cabinetInterestComments.setText("");
        cooktopCutoutYes = false;
        basketsYes = false;
        gridsYes = false;
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
        roomPhoto.setImageDrawable(null);
        drawingPhoto.setImageDrawable(null);
        photoStatus.setText("No photo selected.");
        drawingStatus.setText("No countertop drawing selected.");
        photoAccordionOpen = 0;
        squareFootResult.setText("Net square footage: 0.00");
        totalResult.setText("Estimated total: $0.00");
        stepIndex = 0;
        saveLists();
        persistDrawingState();
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
        return value == null
                ? ""
                : value.replace("MSI:", "").replace("...", "").trim();
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
            case PAGE_PHOTO: return "Drawing and square footage";
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
            case PAGE_WATERFALL: return questionText(pageId, "Do you want a waterfall?");
            case PAGE_CABINETS: return questionText(pageId, "Are the cabinets in?");
            case PAGE_BUY_CABINETS: return questionText(pageId, "Any cabinet comments?");
            case PAGE_SECTION_NAME: return questionText(pageId, "What should this countertop section be called?");
            case PAGE_SECTION_LENGTH: return questionText(pageId, "What is the section length in inches?");
            case PAGE_SECTION_WIDTH: return questionText(pageId, "What is the section width in inches?");
            case PAGE_SECTION_QUANTITY: return questionText(pageId, "How many identical sections are there?");
            case PAGE_STOVE_LENGTH: return questionText(pageId, "What is the slide-in stove opening length?");
            case PAGE_STOVE_WIDTH: return questionText(pageId, "What is the slide-in stove opening width?");
            case PAGE_SLABS: return "Which slabs does the customer like?";
            case PAGE_PHOTO: return questionText(pageId, "Upload a countertop drawing for AI square footage");
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
        applyV130PageChangesOnce();
        applyV138PageChangesOnce();
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

    private void applyV130PageChangesOnce() {
        if (prefs.getBoolean("v1_30_page_changes_applied", false)) return;
        movePageBefore(PAGE_EDGE_DETAIL, PAGE_CABINETS);
        savePageOrder();
        prefs.edit().putBoolean("v1_30_page_changes_applied", true).apply();
    }

    private void applyV138PageChangesOnce() {
        if (prefs.getBoolean("v1_38_page_changes_applied", false)) return;
        pageOrder.remove(Integer.valueOf(PAGE_PHOTO));
        pageOrder.add(Math.min(4, pageOrder.size()), PAGE_PHOTO);
        savePageOrder();
        prefs.edit().putBoolean("v1_38_page_changes_applied", true).apply();
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

    private Button accordionButton(String text, boolean open) {
        Button button = new Button(this);
        button.setText((open ? "▲  " : "▼  ") + text);
        button.setAllCaps(false);
        button.setTextSize(18);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        button.setBackgroundColor(Color.rgb(91, 58, 41));
        button.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        params.setMargins(0, dp(12), 0, 0);
        button.setLayoutParams(params);
        return button;
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

    private AutoCompleteTextView addressInput(String hint) {
        AutoCompleteTextView field = new AutoCompleteTextView(this);
        field.setHint(hint);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        field.setTextSize(19);
        field.setSingleLine(true);
        field.setThreshold(4);
        field.setPadding(dp(14), dp(14), dp(14), dp(14));
        field.setBackgroundColor(Color.WHITE);
        field.setSelectAllOnFocus(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(8), 0, dp(12));
        field.setLayoutParams(params);
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleAddressLookup(field, s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        return field;
    }

    private void scheduleAddressLookup(AutoCompleteTextView field, String query) {
        if (addressLookupRunnable != null) {
            addressHandler.removeCallbacks(addressLookupRunnable);
        }
        String trimmed = query.trim();
        if (trimmed.length() < 4) return;
        addressLookupRunnable = () -> lookupAddressSuggestions(field, trimmed);
        addressHandler.postDelayed(addressLookupRunnable, 550);
    }

    private void lookupAddressSuggestions(AutoCompleteTextView field, String query) {
        new Thread(() -> {
            ArrayList<String> suggestions = new ArrayList<>();
            try {
                Geocoder geocoder = new Geocoder(this, Locale.US);
                List<Address> addresses = geocoder.getFromLocationName(query, 5);
                if (addresses != null) {
                    for (Address address : addresses) {
                        String line = fullStreetAddress(address);
                        if (line != null && !line.trim().isEmpty() && !suggestions.contains(line)) {
                            suggestions.add(line);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            runOnUiThread(() -> {
                if (!field.getText().toString().trim().equals(query) || suggestions.isEmpty()) return;
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        suggestions);
                field.setAdapter(adapter);
                field.showDropDown();
            });
        }).start();
    }

    private String fullStreetAddress(Address address) {
        String number = address.getSubThoroughfare();
        String street = address.getThoroughfare();
        if (number == null || number.trim().isEmpty() || street == null || street.trim().isEmpty()) {
            return null;
        }
        StringBuilder line = new StringBuilder(number.trim()).append(" ").append(street.trim());
        String city = address.getLocality();
        if (city != null && !city.trim().isEmpty()) line.append(", ").append(city.trim());
        String state = address.getAdminArea();
        if (state != null && !state.trim().isEmpty()) line.append(", ").append(state.trim());
        String zip = address.getPostalCode();
        if (zip != null && !zip.trim().isEmpty()) line.append(" ").append(zip.trim());
        return line.toString();
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

    private static class ZoomPanFrame extends FrameLayout {
        private static final float MINIMUM_SCALE = 1f;
        private static final float MAXIMUM_SCALE = 8f;
        private final ScaleGestureDetector scaleDetector;
        private final GestureDetector gestureDetector;
        private View zoomContent;
        private float scale = MINIMUM_SCALE;
        private float translationX;
        private float translationY;
        private float lastX;
        private float lastY;
        private boolean moved;

        ZoomPanFrame(Context context) {
            super(context);
            setBackgroundColor(Color.BLACK);
            setClickable(true);
            scaleDetector = new ScaleGestureDetector(
                    context,
                    new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        @Override
                        public boolean onScale(ScaleGestureDetector detector) {
                            float previousScale = scale;
                            float nextScale = clampZoom(
                                    previousScale * detector.getScaleFactor(),
                                    MINIMUM_SCALE,
                                    MAXIMUM_SCALE);
                            if (Math.abs(nextScale - previousScale) < 0.001f) return true;
                            float ratio = nextScale / previousScale;
                            float focusFromCenterX = detector.getFocusX() - getWidth() / 2f;
                            float focusFromCenterY = detector.getFocusY() - getHeight() / 2f;
                            translationX = focusFromCenterX
                                    - (focusFromCenterX - translationX) * ratio;
                            translationY = focusFromCenterY
                                    - (focusFromCenterY - translationY) * ratio;
                            scale = nextScale;
                            moved = true;
                            applyTransform();
                            return true;
                        }
                    });
            gestureDetector = new GestureDetector(
                    context,
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDown(MotionEvent event) {
                            return true;
                        }

                        @Override
                        public boolean onDoubleTap(MotionEvent event) {
                            resetZoom();
                            return true;
                        }
                    });
        }

        void setZoomContent(View view) {
            removeAllViews();
            zoomContent = view;
            addView(view, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            post(this::resetZoom);
        }

        void resetZoom() {
            scale = MINIMUM_SCALE;
            translationX = 0f;
            translationY = 0f;
            applyTransform();
        }

        private void applyTransform() {
            if (zoomContent == null) return;
            clampTranslation();
            zoomContent.setPivotX(zoomContent.getWidth() / 2f);
            zoomContent.setPivotY(zoomContent.getHeight() / 2f);
            zoomContent.setScaleX(scale);
            zoomContent.setScaleY(scale);
            zoomContent.setTranslationX(translationX);
            zoomContent.setTranslationY(translationY);
        }

        private void clampTranslation() {
            if (zoomContent == null) return;
            float maximumX = Math.max(
                    0f,
                    (zoomContent.getWidth() * scale - getWidth()) / 2f);
            float maximumY = Math.max(
                    0f,
                    (zoomContent.getHeight() * scale - getHeight()) / 2f);
            translationX = clampZoom(translationX, -maximumX, maximumX);
            translationY = clampZoom(translationY, -maximumY, maximumY);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            getParent().requestDisallowInterceptTouchEvent(true);
            gestureDetector.onTouchEvent(event);
            scaleDetector.onTouchEvent(event);

            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                lastX = event.getX();
                lastY = event.getY();
                moved = false;
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (scaleDetector.isInProgress() || event.getPointerCount() > 1) {
                    lastX = event.getX(0);
                    lastY = event.getY(0);
                } else if (scale > MINIMUM_SCALE) {
                    float nextX = event.getX();
                    float nextY = event.getY();
                    float dx = nextX - lastX;
                    float dy = nextY - lastY;
                    if (Math.abs(dx) > 1f || Math.abs(dy) > 1f) moved = true;
                    translationX += dx;
                    translationY += dy;
                    lastX = nextX;
                    lastY = nextY;
                    applyTransform();
                }
            } else if (action == MotionEvent.ACTION_POINTER_UP) {
                int survivingPointer = event.getActionIndex() == 0 ? 1 : 0;
                lastX = event.getX(survivingPointer);
                lastY = event.getY(survivingPointer);
            } else if (action == MotionEvent.ACTION_UP) {
                if (!moved) performClick();
            }
            return true;
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        private static float clampZoom(float value, float minimum, float maximum) {
            return Math.max(minimum, Math.min(maximum, value));
        }
    }

    private static class VerificationDrawingView extends View {
        private static final float DEFAULT_CANVAS_WIDTH = 1000f;
        private static final float DEFAULT_CANVAS_HEIGHT = 700f;
        private JSONObject verificationDrawing;
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        VerificationDrawingView(Context context) {
            super(context);
            setBackgroundColor(Color.WHITE);
            setContentDescription("AI verification redraw of the countertop plan. Tap to zoom.");
            fillPaint.setStyle(Paint.Style.FILL);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeJoin(Paint.Join.ROUND);
            linePaint.setStrokeCap(Paint.Cap.ROUND);
            textPaint.setColor(Color.rgb(45, 45, 45));
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textBackgroundPaint.setColor(Color.argb(225, 255, 255, 255));
            textBackgroundPaint.setStyle(Paint.Style.FILL);
        }

        void setVerificationDrawing(JSONObject drawing) {
            verificationDrawing = drawing;
            invalidate();
        }

        void clearDrawing() {
            verificationDrawing = null;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.WHITE);
            if (verificationDrawing == null) return;

            float canvasWidth = (float) verificationDrawing.optDouble(
                    "canvas_width",
                    DEFAULT_CANVAS_WIDTH);
            float canvasHeight = (float) verificationDrawing.optDouble(
                    "canvas_height",
                    DEFAULT_CANVAS_HEIGHT);
            if (canvasWidth <= 0 || canvasHeight <= 0) return;

            float outerPadding = 20f;
            float availableWidth = Math.max(1f, getWidth() - outerPadding * 2f);
            float availableHeight = Math.max(1f, getHeight() - outerPadding * 2f);
            float scale = Math.min(availableWidth / canvasWidth, availableHeight / canvasHeight);
            float left = (getWidth() - canvasWidth * scale) / 2f;
            float top = (getHeight() - canvasHeight * scale) / 2f;

            canvas.save();
            canvas.translate(left, top);
            canvas.scale(scale, scale);
            JSONArray shapes = verificationDrawing.optJSONArray("shapes");
            JSONArray dimensions = verificationDrawing.optJSONArray("dimensions");
            List<BacksplashStrip> backsplashStrips = buildBacksplashStrips(
                    dimensions,
                    shapes,
                    canvasWidth,
                    canvasHeight);
            drawShapes(canvas, shapes, backsplashStrips);
            drawDimensions(canvas, dimensions, shapes, backsplashStrips);
            drawBacksplashDimensions(canvas, backsplashStrips);
            canvas.restore();
        }

        private void drawShapes(
                Canvas canvas,
                JSONArray shapes,
                List<BacksplashStrip> backsplashStrips) {
            if (shapes == null) return;
            // Countertops first, normalized backsplash strips second, and openings last.
            for (int pass = 0; pass < 3; pass++) {
                for (int i = 0; i < Math.min(shapes.length(), 24); i++) {
                    JSONObject shape = shapes.optJSONObject(i);
                    if (shape == null) continue;
                    String kind = shape.optString("kind", "countertop");
                    boolean drawInThisPass = pass == 0
                            ? !"opening".equals(kind) && !"backsplash".equals(kind)
                            : pass == 1
                            ? backsplashStrips.isEmpty() && "backsplash".equals(kind)
                            : "opening".equals(kind);
                    if (!drawInThisPass) continue;
                    drawShape(canvas, shape);
                }
                if (pass == 1 && !backsplashStrips.isEmpty()) {
                    drawBacksplashStrips(canvas, backsplashStrips);
                }
            }
        }

        private void drawShape(Canvas canvas, JSONObject shape) {
            JSONArray points = shape.optJSONArray("points");
            if (points == null || points.length() < 3) return;

            Path path = new Path();
            float centerX = 0;
            float centerY = 0;
            float minimumX = Float.MAX_VALUE;
            float minimumY = Float.MAX_VALUE;
            float maximumX = -Float.MAX_VALUE;
            float maximumY = -Float.MAX_VALUE;
            int validPoints = 0;
            for (int pointIndex = 0; pointIndex < Math.min(points.length(), 16); pointIndex++) {
                JSONObject point = points.optJSONObject(pointIndex);
                if (point == null) continue;
                float x = clamp((float) point.optDouble("x", 0), 0, DEFAULT_CANVAS_WIDTH);
                float y = clamp((float) point.optDouble("y", 0), 0, DEFAULT_CANVAS_HEIGHT);
                if (validPoints == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
                centerX += x;
                centerY += y;
                minimumX = Math.min(minimumX, x);
                minimumY = Math.min(minimumY, y);
                maximumX = Math.max(maximumX, x);
                maximumY = Math.max(maximumY, y);
                validPoints++;
            }
            if (validPoints < 3) return;
            path.close();
            centerX /= validPoints;
            centerY /= validPoints;

            String kind = shape.optString("kind", "countertop");
            boolean isStove = DrawingRules.isStove(shape);
            if (isStove) {
                fillPaint.setColor(Color.WHITE);
                canvas.drawPath(path, fillPaint);
                drawStoveSymbol(
                        canvas,
                        new RectF(minimumX, minimumY, maximumX, maximumY));
            } else {
                if ("backsplash".equals(kind)) {
                    fillPaint.setColor(Color.rgb(211, 157, 73));
                } else if ("opening".equals(kind)) {
                    fillPaint.setColor(Color.WHITE);
                } else {
                    fillPaint.setColor(Color.rgb(239, 230, 220));
                }
                linePaint.setColor(Color.rgb(91, 58, 41));
                linePaint.setStrokeWidth(
                        "backsplash".equals(kind) ? 6f : "opening".equals(kind) ? 5f : 4f);
                canvas.drawPath(path, fillPaint);
                canvas.drawPath(path, linePaint);
            }

            // Model-written shape labels can contain guesses or warning sentences. Only draw a
            // short label that the user deliberately added in the redraw editor.
            String label = DrawingRules.visibleUserLabel(shape);
            if (!label.isEmpty()) {
                textPaint.setTextSize(32f);
                drawTextWithBackground(canvas, label, centerX, centerY + 13f);
            }
        }

        private void drawStoveSymbol(Canvas canvas, RectF bounds) {
            if (bounds.isEmpty()) return;
            float centerX = bounds.centerX();
            float centerY = bounds.centerY();
            float width = bounds.width();
            float height = bounds.height();
            float smallestSide = Math.min(width, height);
            if (smallestSide < 20f) return;

            float burnerRadius = Math.max(3f, Math.min(20f, smallestSide * 0.09f));
            float horizontalOffset = Math.min(
                    width * 0.28f,
                    Math.max(0f, width / 2f - burnerRadius - 4f));
            float verticalOffset = Math.min(
                    height * 0.28f,
                    Math.max(0f, height / 2f - burnerRadius - 4f));
            if (horizontalOffset < burnerRadius
                    || verticalOffset < burnerRadius) return;
            linePaint.setColor(Color.rgb(91, 58, 41));
            linePaint.setStrokeWidth(4f);
            for (int horizontal = -1; horizontal <= 1; horizontal += 2) {
                for (int vertical = -1; vertical <= 1; vertical += 2) {
                    canvas.drawCircle(
                            centerX + horizontal * horizontalOffset,
                            centerY + vertical * verticalOffset,
                            burnerRadius,
                            linePaint);
                }
            }
        }

        private List<BacksplashStrip> buildBacksplashStrips(
                JSONArray dimensions,
                JSONArray shapes,
                float canvasWidth,
                float canvasHeight) {
            List<BacksplashStrip> strips = new ArrayList<>();
            if (dimensions == null) return strips;

            HashMap<String, BacksplashDimensionGroup> groups = new HashMap<>();
            for (int i = 0; i < Math.min(dimensions.length(), 50); i++) {
                JSONObject dimension = dimensions.optJSONObject(i);
                if (dimension == null) continue;
                JSONArray partIds = dimension.optJSONArray("part_ids");
                if (partIds == null) continue;
                for (int idIndex = 0; idIndex < Math.min(partIds.length(), 12); idIndex++) {
                    String partId = partIds.optString(idIndex, "").trim();
                    if (partId.isEmpty()) continue;
                    BacksplashDimensionGroup group = groups.get(partId);
                    if (group == null) {
                        group = new BacksplashDimensionGroup(partId);
                        groups.put(partId, group);
                    }
                    group.dimensions.add(dimension);
                }
            }

            for (BacksplashDimensionGroup group : groups.values()) {
                group.chooseDimensions();
                if (!group.isBacksplash()) continue;
                BacksplashStrip strip = createBacksplashStrip(
                        group,
                        shapes,
                        canvasWidth,
                        canvasHeight);
                if (strip != null) strips.add(strip);
            }
            return strips;
        }

        private BacksplashStrip createBacksplashStrip(
                BacksplashDimensionGroup group,
                JSONArray shapes,
                float canvasWidth,
                float canvasHeight) {
            JSONObject lengthDimension = group.lengthDimension;
            if (lengthDimension == null) return null;

            float x1 = (float) lengthDimension.optDouble("x1", 0);
            float y1 = (float) lengthDimension.optDouble("y1", 0);
            float x2 = (float) lengthDimension.optDouble("x2", 0);
            float y2 = (float) lengthDimension.optDouble("y2", 0);
            float dx = x2 - x1;
            float dy = y2 - y1;
            float lineLength = (float) Math.sqrt(dx * dx + dy * dy);
            if (Float.isNaN(lineLength) || Float.isInfinite(lineLength) || lineLength < 2f) {
                return null;
            }

            float unitX = dx / lineLength;
            float unitY = dy / lineLength;
            float normalX = -unitY;
            float normalY = unitX;
            float midpointX = (x1 + x2) / 2f;
            float midpointY = (y1 + y2) / 2f;

            JSONObject nearestCountertop = nearestCountertopShape(
                    midpointX,
                    midpointY,
                    shapes);
            float outwardX;
            float outwardY;
            float nearProjection;
            if (nearestCountertop != null) {
                RectF counterBounds = shapeBounds(nearestCountertop);
                float counterSide = (counterBounds.centerX() - midpointX) * normalX
                        + (counterBounds.centerY() - midpointY) * normalY;
                float outwardSign = counterSide >= 0f ? -1f : 1f;
                outwardX = normalX * outwardSign;
                outwardY = normalY * outwardSign;
                nearProjection = maximumShapeProjection(
                        nearestCountertop,
                        outwardX,
                        outwardY) + 28f;
            } else {
                float canvasSide = (canvasWidth / 2f - midpointX) * normalX
                        + (canvasHeight / 2f - midpointY) * normalY;
                float outwardSign = canvasSide >= 0f ? -1f : 1f;
                outwardX = normalX * outwardSign;
                outwardY = normalY * outwardSign;
                nearProjection = midpointX * outwardX + midpointY * outwardY + 18f;
            }

            double lengthInches = positiveValue(lengthDimension.optDouble("value_inches", 0));
            double widthInches = group.widthDimension == null
                    ? 0
                    : positiveValue(group.widthDimension.optDouble("value_inches", 0));
            float proportionalThickness = lengthInches > 0 && widthInches > 0
                    ? (float) (lineLength * widthInches / lengthInches)
                    : 18f;
            float measuredThickness = dimensionLineLength(group.widthDimension);
            float thickness = measuredThickness > 1f
                    ? (proportionalThickness + measuredThickness) / 2f
                    : proportionalThickness;
            thickness = clamp(thickness, 14f, 30f);

            float startProjection = Math.min(
                    x1 * unitX + y1 * unitY,
                    x2 * unitX + y2 * unitY);
            float endProjection = Math.max(
                    x1 * unitX + y1 * unitY,
                    x2 * unitX + y2 * unitY);
            float farProjection = nearProjection + thickness;
            BacksplashStrip strip = new BacksplashStrip(
                    group.partId,
                    lengthDimension,
                    group.widthDimension,
                    unitX,
                    unitY,
                    outwardX,
                    outwardY);
            strip.setPoint(0, unitX, unitY, startProjection, outwardX, outwardY, nearProjection);
            strip.setPoint(1, unitX, unitY, endProjection, outwardX, outwardY, nearProjection);
            strip.setPoint(2, unitX, unitY, endProjection, outwardX, outwardY, farProjection);
            strip.setPoint(3, unitX, unitY, startProjection, outwardX, outwardY, farProjection);
            strip.shiftInsideCanvas(canvasWidth, canvasHeight, 10f);
            return strip;
        }

        private void drawBacksplashStrips(Canvas canvas, List<BacksplashStrip> strips) {
            fillPaint.setColor(Color.rgb(211, 157, 73));
            linePaint.setColor(Color.rgb(91, 58, 41));
            linePaint.setStrokeWidth(6f);
            for (BacksplashStrip strip : strips) {
                Path path = new Path();
                path.moveTo(strip.x[0], strip.y[0]);
                for (int i = 1; i < 4; i++) path.lineTo(strip.x[i], strip.y[i]);
                path.close();
                canvas.drawPath(path, fillPaint);
                canvas.drawPath(path, linePaint);
            }
        }

        private void drawBacksplashDimensions(
                Canvas canvas,
                List<BacksplashStrip> strips) {
            if (strips.isEmpty()) return;
            linePaint.setColor(Color.rgb(25, 25, 25));
            linePaint.setStrokeWidth(3f);
            textPaint.setTextSize(34f);
            for (BacksplashStrip strip : strips) {
                float lengthX1 = strip.x[3] + strip.outwardX * 18f;
                float lengthY1 = strip.y[3] + strip.outwardY * 18f;
                float lengthX2 = strip.x[2] + strip.outwardX * 18f;
                float lengthY2 = strip.y[2] + strip.outwardY * 18f;
                drawMeasurementLine(canvas, lengthX1, lengthY1, lengthX2, lengthY2);
                String lengthLabel = dimensionDisplayLabel(strip.lengthDimension);
                if (!lengthLabel.isEmpty()) {
                    drawTextWithBackground(
                            canvas,
                            lengthLabel,
                            (lengthX1 + lengthX2) / 2f + strip.outwardX * 25f,
                            (lengthY1 + lengthY2) / 2f + strip.outwardY * 25f + 12f);
                }

                if (strip.widthDimension != null) {
                    float widthX1 = strip.x[1] + strip.unitX * 18f;
                    float widthY1 = strip.y[1] + strip.unitY * 18f;
                    float widthX2 = strip.x[2] + strip.unitX * 18f;
                    float widthY2 = strip.y[2] + strip.unitY * 18f;
                    drawMeasurementLine(canvas, widthX1, widthY1, widthX2, widthY2);
                    String widthLabel = dimensionDisplayLabel(strip.widthDimension);
                    if (!widthLabel.isEmpty()) {
                        drawTextWithBackground(
                                canvas,
                                widthLabel,
                                (widthX1 + widthX2) / 2f + strip.unitX * 25f,
                                (widthY1 + widthY2) / 2f + strip.unitY * 25f + 12f);
                    }
                }
            }
        }

        private void drawMeasurementLine(
                Canvas canvas,
                float x1,
                float y1,
                float x2,
                float y2) {
            float dx = x2 - x1;
            float dy = y2 - y1;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length < 1f) return;
            canvas.drawLine(x1, y1, x2, y2, linePaint);
            drawArrow(canvas, x1, y1, dx / length, dy / length, false);
            drawArrow(canvas, x2, y2, dx / length, dy / length, true);
        }

        private boolean belongsToBacksplashStrip(
                JSONObject dimension,
                List<BacksplashStrip> strips) {
            JSONArray partIds = dimension.optJSONArray("part_ids");
            if (partIds == null) return false;
            for (BacksplashStrip strip : strips) {
                for (int i = 0; i < Math.min(partIds.length(), 12); i++) {
                    if (strip.partId.equals(partIds.optString(i, ""))) return true;
                }
            }
            return false;
        }

        private JSONObject nearestCountertopShape(float x, float y, JSONArray shapes) {
            if (shapes == null) return null;
            JSONObject nearest = null;
            float nearestDistance = Float.MAX_VALUE;
            for (int i = 0; i < Math.min(shapes.length(), 24); i++) {
                JSONObject shape = shapes.optJSONObject(i);
                if (shape == null) continue;
                String kind = shape.optString("kind", "countertop");
                if ("opening".equals(kind) || "backsplash".equals(kind)) continue;
                RectF bounds = shapeBounds(shape);
                if (bounds.isEmpty()) continue;
                float nearestX = clamp(x, bounds.left, bounds.right);
                float nearestY = clamp(y, bounds.top, bounds.bottom);
                float distance = distanceSquared(x, y, nearestX, nearestY);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = shape;
                }
            }
            return nearest;
        }

        private RectF shapeBounds(JSONObject shape) {
            JSONArray points = shape.optJSONArray("points");
            if (points == null) return new RectF();
            float left = Float.MAX_VALUE;
            float top = Float.MAX_VALUE;
            float right = -Float.MAX_VALUE;
            float bottom = -Float.MAX_VALUE;
            for (int i = 0; i < Math.min(points.length(), 16); i++) {
                JSONObject point = points.optJSONObject(i);
                if (point == null) continue;
                float x = (float) point.optDouble("x", 0);
                float y = (float) point.optDouble("y", 0);
                left = Math.min(left, x);
                top = Math.min(top, y);
                right = Math.max(right, x);
                bottom = Math.max(bottom, y);
            }
            return left == Float.MAX_VALUE ? new RectF() : new RectF(left, top, right, bottom);
        }

        private float maximumShapeProjection(JSONObject shape, float axisX, float axisY) {
            JSONArray points = shape.optJSONArray("points");
            float maximum = -Float.MAX_VALUE;
            if (points == null) return maximum;
            for (int i = 0; i < Math.min(points.length(), 16); i++) {
                JSONObject point = points.optJSONObject(i);
                if (point == null) continue;
                float projection = (float) point.optDouble("x", 0) * axisX
                        + (float) point.optDouble("y", 0) * axisY;
                maximum = Math.max(maximum, projection);
            }
            return maximum;
        }

        private static double positiveValue(double value) {
            return !Double.isNaN(value) && !Double.isInfinite(value) && value > 0
                    ? value
                    : 0;
        }

        private static String dimensionDisplayLabel(JSONObject dimension) {
            if (dimension == null) return "";
            double value = positiveValue(dimension.optDouble("value_inches", 0));
            if (value <= 0) return "";
            String measurement;
            if (Math.abs(value - Math.rint(value)) < 0.005) {
                measurement = String.valueOf((long) Math.rint(value));
            } else {
                measurement = String.format(Locale.US, "%.2f", value)
                        .replaceAll("0+$", "")
                        .replaceAll("\\.$", "");
            }
            return measurement + "\"";
        }

        private static float dimensionLineLength(JSONObject dimension) {
            if (dimension == null) return 0f;
            float dx = (float) (dimension.optDouble("x2", 0)
                    - dimension.optDouble("x1", 0));
            float dy = (float) (dimension.optDouble("y2", 0)
                    - dimension.optDouble("y1", 0));
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        private void drawDimensions(
                Canvas canvas,
                JSONArray dimensions,
                JSONArray shapes,
                List<BacksplashStrip> backsplashStrips) {
            if (dimensions == null) return;
            linePaint.setColor(Color.rgb(25, 25, 25));
            linePaint.setStrokeWidth(3f);
            for (int i = 0; i < Math.min(dimensions.length(), 50); i++) {
                JSONObject dimension = dimensions.optJSONObject(i);
                if (dimension == null) continue;
                if (belongsToBacksplashStrip(dimension, backsplashStrips)) continue;
                float x1 = clamp((float) dimension.optDouble("x1", 0), 0, DEFAULT_CANVAS_WIDTH);
                float y1 = clamp((float) dimension.optDouble("y1", 0), 0, DEFAULT_CANVAS_HEIGHT);
                float x2 = clamp((float) dimension.optDouble("x2", 0), 0, DEFAULT_CANVAS_WIDTH);
                float y2 = clamp((float) dimension.optDouble("y2", 0), 0, DEFAULT_CANVAS_HEIGHT);
                float dx = x2 - x1;
                float dy = y2 - y1;
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                if (length < 1f) continue;
                canvas.drawLine(x1, y1, x2, y2, linePaint);
                drawArrow(canvas, x1, y1, dx / length, dy / length, false);
                drawArrow(canvas, x2, y2, dx / length, dy / length, true);

                String label = dimensionDisplayLabel(dimension);
                if (!label.isEmpty()) {
                    textPaint.setTextSize(34f);
                    float normalX = -dy / length;
                    float normalY = dx / length;
                    float midpointX = (x1 + x2) / 2f;
                    float midpointY = (y1 + y2) / 2f;
                    float positiveX = midpointX + normalX * 25f;
                    float positiveY = midpointY + normalY * 25f;
                    float negativeX = midpointX - normalX * 25f;
                    float negativeY = midpointY - normalY * 25f;
                    float[] nearestShapeCenter = nearestShapeCenter(midpointX, midpointY, shapes);
                    float positiveDistance = distanceSquared(
                            positiveX,
                            positiveY,
                            nearestShapeCenter[0],
                            nearestShapeCenter[1]);
                    float negativeDistance = distanceSquared(
                            negativeX,
                            negativeY,
                            nearestShapeCenter[0],
                            nearestShapeCenter[1]);
                    float labelX = positiveDistance >= negativeDistance ? positiveX : negativeX;
                    float labelY = positiveDistance >= negativeDistance ? positiveY : negativeY;
                    drawTextWithBackground(
                            canvas,
                            label,
                            labelX,
                            labelY + 12f);
                }
            }
        }

        private float[] nearestShapeCenter(float x, float y, JSONArray shapes) {
            float nearestCenterX = DEFAULT_CANVAS_WIDTH / 2f;
            float nearestCenterY = DEFAULT_CANVAS_HEIGHT / 2f;
            float nearestDistance = Float.MAX_VALUE;
            if (shapes == null) return new float[]{nearestCenterX, nearestCenterY};

            for (int shapeIndex = 0; shapeIndex < Math.min(shapes.length(), 24); shapeIndex++) {
                JSONObject shape = shapes.optJSONObject(shapeIndex);
                if (shape == null) continue;
                JSONArray points = shape.optJSONArray("points");
                if (points == null || points.length() < 3) continue;

                float minimumX = DEFAULT_CANVAS_WIDTH;
                float minimumY = DEFAULT_CANVAS_HEIGHT;
                float maximumX = 0f;
                float maximumY = 0f;
                boolean hasPoint = false;
                for (int pointIndex = 0; pointIndex < Math.min(points.length(), 16); pointIndex++) {
                    JSONObject point = points.optJSONObject(pointIndex);
                    if (point == null) continue;
                    float pointX = clamp(
                            (float) point.optDouble("x", 0),
                            0,
                            DEFAULT_CANVAS_WIDTH);
                    float pointY = clamp(
                            (float) point.optDouble("y", 0),
                            0,
                            DEFAULT_CANVAS_HEIGHT);
                    minimumX = Math.min(minimumX, pointX);
                    minimumY = Math.min(minimumY, pointY);
                    maximumX = Math.max(maximumX, pointX);
                    maximumY = Math.max(maximumY, pointY);
                    hasPoint = true;
                }
                if (!hasPoint) continue;

                float nearestX = clamp(x, minimumX, maximumX);
                float nearestY = clamp(y, minimumY, maximumY);
                float boundsDistance = distanceSquared(x, y, nearestX, nearestY);
                if (boundsDistance < nearestDistance) {
                    nearestDistance = boundsDistance;
                    nearestCenterX = (minimumX + maximumX) / 2f;
                    nearestCenterY = (minimumY + maximumY) / 2f;
                }
            }
            return new float[]{nearestCenterX, nearestCenterY};
        }

        private static float distanceSquared(
                float x1,
                float y1,
                float x2,
                float y2) {
            float dx = x1 - x2;
            float dy = y1 - y2;
            return dx * dx + dy * dy;
        }

        private void drawArrow(
                Canvas canvas,
                float x,
                float y,
                float directionX,
                float directionY,
                boolean pointsBackward) {
            float direction = pointsBackward ? -1f : 1f;
            float alongX = directionX * 18f * direction;
            float alongY = directionY * 18f * direction;
            float normalX = -directionY * 8f;
            float normalY = directionX * 8f;
            canvas.drawLine(x, y, x + alongX + normalX, y + alongY + normalY, linePaint);
            canvas.drawLine(x, y, x + alongX - normalX, y + alongY - normalY, linePaint);
        }

        private void drawTextWithBackground(
                Canvas canvas,
                String value,
                float centerX,
                float baselineY) {
            String text = value.length() > 60 ? value.substring(0, 60) : value;
            float width = textPaint.measureText(text);
            while (width > DEFAULT_CANVAS_WIDTH - 20f && text.length() > 4) {
                text = text.substring(0, text.length() - 2).trim() + "…";
                width = textPaint.measureText(text);
            }
            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            centerX = clamp(
                    centerX,
                    width / 2f + 10f,
                    DEFAULT_CANVAS_WIDTH - width / 2f - 10f);
            baselineY = clamp(
                    baselineY,
                    10f - metrics.top,
                    DEFAULT_CANVAS_HEIGHT - 10f - metrics.bottom);
            RectF background = new RectF(
                    centerX - width / 2f - 7f,
                    baselineY + metrics.top - 4f,
                    centerX + width / 2f + 7f,
                    baselineY + metrics.bottom + 4f);
            canvas.drawRoundRect(background, 5f, 5f, textBackgroundPaint);
            canvas.drawText(text, centerX, baselineY, textPaint);
        }

        private static class BacksplashDimensionGroup {
            final String partId;
            final List<JSONObject> dimensions = new ArrayList<>();
            JSONObject lengthDimension;
            JSONObject widthDimension;

            BacksplashDimensionGroup(String partId) {
                this.partId = partId;
            }

            void chooseDimensions() {
                for (JSONObject dimension : dimensions) {
                    String role = dimension.optString("role", "");
                    if ("length".equals(role) && lengthDimension == null) {
                        lengthDimension = dimension;
                    } else if ("width".equals(role) && widthDimension == null) {
                        widthDimension = dimension;
                    }
                }
                if (lengthDimension == null) {
                    double largest = -1;
                    for (JSONObject dimension : dimensions) {
                        double value = positiveValue(dimension.optDouble("value_inches", 0));
                        if (value > largest) {
                            largest = value;
                            lengthDimension = dimension;
                        }
                    }
                }
                if (widthDimension == null) {
                    double smallest = Double.MAX_VALUE;
                    for (JSONObject dimension : dimensions) {
                        if (dimension == lengthDimension && dimensions.size() > 1) continue;
                        double value = positiveValue(dimension.optDouble("value_inches", 0));
                        if (value > 0 && value < smallest) {
                            smallest = value;
                            widthDimension = dimension;
                        }
                    }
                }
            }

            boolean isBacksplash() {
                if (lengthDimension == null || widthDimension == null
                        || lengthDimension == widthDimension) return false;
                String lowerId = partId.toLowerCase(Locale.US);
                boolean namedBacksplash = lowerId.contains("backsplash")
                        || lowerId.contains("splash")
                        || lowerId.startsWith("bs_")
                        || lowerId.endsWith("_bs")
                        || lowerId.equals("bs");
                boolean namedOther = lowerId.contains("counter")
                        || lowerId.startsWith("ct_")
                        || lowerId.contains("island")
                        || lowerId.contains("opening")
                        || lowerId.contains("sink")
                        || lowerId.contains("stove")
                        || lowerId.contains("cooktop")
                        || lowerId.contains("cutout");
                double length = positiveValue(
                        lengthDimension.optDouble("value_inches", 0));
                double width = positiveValue(
                        widthDimension.optDouble("value_inches", 0));
                boolean backsplashProportions = !namedOther
                        && width > 0
                        && width <= 6.5
                        && length >= width * 3;
                return namedBacksplash || backsplashProportions;
            }
        }

        private static class BacksplashStrip {
            final String partId;
            final JSONObject lengthDimension;
            final JSONObject widthDimension;
            final float unitX;
            final float unitY;
            final float outwardX;
            final float outwardY;
            final float[] x = new float[4];
            final float[] y = new float[4];

            BacksplashStrip(
                    String partId,
                    JSONObject lengthDimension,
                    JSONObject widthDimension,
                    float unitX,
                    float unitY,
                    float outwardX,
                    float outwardY) {
                this.partId = partId;
                this.lengthDimension = lengthDimension;
                this.widthDimension = widthDimension;
                this.unitX = unitX;
                this.unitY = unitY;
                this.outwardX = outwardX;
                this.outwardY = outwardY;
            }

            void setPoint(
                    int index,
                    float axisX,
                    float axisY,
                    float axisProjection,
                    float normalX,
                    float normalY,
                    float normalProjection) {
                x[index] = axisX * axisProjection + normalX * normalProjection;
                y[index] = axisY * axisProjection + normalY * normalProjection;
            }

            void shiftInsideCanvas(float canvasWidth, float canvasHeight, float margin) {
                float minimumX = Math.min(Math.min(x[0], x[1]), Math.min(x[2], x[3]));
                float maximumX = Math.max(Math.max(x[0], x[1]), Math.max(x[2], x[3]));
                float minimumY = Math.min(Math.min(y[0], y[1]), Math.min(y[2], y[3]));
                float maximumY = Math.max(Math.max(y[0], y[1]), Math.max(y[2], y[3]));
                float shiftX = minimumX < margin ? margin - minimumX : 0f;
                if (maximumX + shiftX > canvasWidth - margin) {
                    shiftX += canvasWidth - margin - (maximumX + shiftX);
                }
                float shiftY = minimumY < margin ? margin - minimumY : 0f;
                if (maximumY + shiftY > canvasHeight - margin) {
                    shiftY += canvasHeight - margin - (maximumY + shiftY);
                }
                for (int i = 0; i < 4; i++) {
                    x[i] += shiftX;
                    y[i] += shiftY;
                }
            }
        }

        private static float clamp(float value, float minimum, float maximum) {
            return Math.max(minimum, Math.min(maximum, value));
        }
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
        final double aiSquareFeet;
        final double manualSquareFeet;
        final double gross;
        final double stove;
        final double net;
        final double total;
        final boolean hasAiDrawings;

        Estimate(
                double aiSquareFeet,
                double manualSquareFeet,
                double gross,
                double stove,
                double net,
                double total,
                boolean hasAiDrawings) {
            this.aiSquareFeet = aiSquareFeet;
            this.manualSquareFeet = manualSquareFeet;
            this.gross = gross;
            this.stove = stove;
            this.net = net;
            this.total = total;
            this.hasAiDrawings = hasAiDrawings;
        }
    }
}
