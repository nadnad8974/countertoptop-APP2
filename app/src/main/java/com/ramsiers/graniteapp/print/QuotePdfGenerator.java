package com.ramsiers.graniteapp.print;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/** Creates the private, phone-local printable quote summary. */
public final class QuotePdfGenerator {
    private static final int PAGE_WIDTH = 612;
    private static final int PAGE_HEIGHT = 792;
    private static final int MARGIN = 42;
    private static final int CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2);
    private static final int BROWN = Color.rgb(91, 58, 41);
    private static final int TAN = Color.rgb(239, 231, 221);
    private static final int LIGHT = Color.rgb(248, 246, 243);
    private static final int GRID = Color.rgb(220, 213, 205);
    private static final int GRAY = Color.rgb(100, 96, 92);

    private QuotePdfGenerator() {
    }

    public static final class PriceRow {
        public final String item;
        public final String details;
        public final String amount;

        public PriceRow(String item, String details, String amount) {
            this.item = clean(item);
            this.details = clean(details);
            this.amount = clean(amount);
        }
    }

    public static final class Data {
        public String customer = "Not provided";
        public String phone = "Not provided";
        public String email = "Not provided";
        public String address = "Not provided";
        public final List<PriceRow> pricedOptions = new ArrayList<>();
        public String estimatedTotal = "$0.00";
        public String sinkSelections = "No sink selected";
        public String sinkLocationNote = "Not provided";
        public String waterfall = "0";
        public String cabinets = "Not provided";
        public String drawingTotal = "0.00 sq. ft.";
        public String drawingDetails = "No verified drawing estimate.";
        public String countertopSections = "No countertop sections added yet.";
        public String selectedSlabs = "No slabs selected.";
        public String projectNotes = "Not provided";
    }

    public static File create(File outputFile, Data data) throws Exception {
        PdfDocument document = new PdfDocument();
        PdfDocument.Page page = document.startPage(
                new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create());
        Canvas canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int y = drawHeader(canvas, paint);
        y = drawLocations(canvas, paint, y);
        y = drawCustomer(canvas, paint, y + 6, data);
        y = drawPricedOptions(canvas, paint, y + 7, data);
        y = drawProjectOptions(canvas, paint, y + 7, data);
        y = drawDrawingSummary(canvas, paint, y + 7, data);
        y = drawSectionsAndSlabs(canvas, paint, y + 7, data);
        drawFooter(canvas, paint, Math.min(y + 10, PAGE_HEIGHT - 21));

        document.finishPage(page);
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            document.close();
            throw new IllegalStateException("The quote PDF folder could not be created.");
        }
        try (FileOutputStream output = new FileOutputStream(outputFile)) {
            document.writeTo(output);
        } finally {
            document.close();
        }
        return outputFile;
    }

    private static int drawHeader(Canvas canvas, Paint paint) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.5f);
        paint.setColor(Color.BLACK);
        canvas.drawOval(187, 18, 425, 87, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC));
        paint.setTextSize(35);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Ramsier's", PAGE_WIDTH / 2f, 61, paint);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(7.5f);
        canvas.drawText("EST. 2007", PAGE_WIDTH / 2f, 78, paint);

        paint.setColor(BROWN);
        paint.setTextSize(20);
        canvas.drawText("QUOTE REQUEST SUMMARY", PAGE_WIDTH / 2f, 113, paint);
        paint.setColor(GRAY);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(7.5f);
        canvas.drawText("CUSTOMER SELECTIONS AND PROJECT DETAILS", PAGE_WIDTH / 2f, 126, paint);
        return 135;
    }

    private static int drawLocations(Canvas canvas, Paint paint, int y) {
        int h = 53;
        fillRect(canvas, paint, MARGIN, y, MARGIN + CONTENT_WIDTH, y + h, LIGHT);
        strokeRect(canvas, paint, MARGIN, y, MARGIN + CONTENT_WIDTH, y + h, GRID);
        line(canvas, paint, PAGE_WIDTH / 2f, y, PAGE_WIDTH / 2f, y + 37, GRID);
        line(canvas, paint, MARGIN, y + 37, MARGIN + CONTENT_WIDTH, y + 37, GRID);

        drawCentered(canvas, paint, "AMHERST LOCATION", 174, y + 11, 7.5f, BROWN, true);
        drawCentered(canvas, paint, "46485 Telegraph Rd., Amherst, Ohio 44001", 174, y + 25, 7.2f, Color.DKGRAY, false);
        drawCentered(canvas, paint, "440-984-8915", 174, y + 34, 7.2f, Color.DKGRAY, true);
        drawCentered(canvas, paint, "NORTH RIDGEVILLE LOCATION", 438, y + 11, 7.5f, BROWN, true);
        drawCentered(canvas, paint, "34555 Mills Rd., N. Ridgeville, Ohio 44039", 438, y + 25, 7.2f, Color.DKGRAY, false);
        drawCentered(canvas, paint, "440-327-3077", 438, y + 34, 7.2f, Color.DKGRAY, true);
        drawCentered(canvas, paint, "EMAIL: RamsiersStone@gmail.com", PAGE_WIDTH / 2f, y + 49, 7.2f, Color.DKGRAY, true);
        return y + h;
    }

    private static int drawCustomer(Canvas canvas, Paint paint, int y, Data data) {
        int rowH = 21;
        int labelW = 59;
        int half = CONTENT_WIDTH / 2;
        fillRect(canvas, paint, MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowH * 2, LIGHT);
        strokeRect(canvas, paint, MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowH * 2, GRID);
        line(canvas, paint, MARGIN, y + rowH, MARGIN + CONTENT_WIDTH, y + rowH, GRID);
        line(canvas, paint, MARGIN + half, y, MARGIN + half, y + rowH * 2, GRID);
        line(canvas, paint, MARGIN + labelW, y, MARGIN + labelW, y + rowH * 2, GRID);
        line(canvas, paint, MARGIN + half + labelW, y, MARGIN + half + labelW, y + rowH * 2, GRID);

        drawCell(canvas, paint, "CUSTOMER", MARGIN + 7, y + 14, 7.2f, true);
        drawCell(canvas, paint, oneLine(data.customer, 30), MARGIN + labelW + 7, y + 14, 8f, false);
        drawCell(canvas, paint, "PHONE", MARGIN + half + 7, y + 14, 7.2f, true);
        drawCell(canvas, paint, oneLine(data.phone, 22), MARGIN + half + labelW + 7, y + 14, 8f, false);
        drawCell(canvas, paint, "EMAIL", MARGIN + 7, y + rowH + 14, 7.2f, true);
        drawCell(canvas, paint, oneLine(data.email, 31), MARGIN + labelW + 7, y + rowH + 14, 7.6f, false);
        drawCell(canvas, paint, "ADDRESS", MARGIN + half + 7, y + rowH + 14, 7.2f, true);
        drawCell(canvas, paint, oneLine(data.address, 28), MARGIN + half + labelW + 7, y + rowH + 14, 7.6f, false);
        return y + rowH * 2;
    }

    private static int drawPricedOptions(Canvas canvas, Paint paint, int y, Data data) {
        y = sectionTitle(canvas, paint, y, "PRICED OPTIONS");
        int headerH = 18;
        fillRect(canvas, paint, MARGIN, y, MARGIN + CONTENT_WIDTH, y + headerH, TAN);
        drawCell(canvas, paint, "Item", MARGIN + 8, y + 12, 7.5f, true);
        drawCell(canvas, paint, "Quantity / Details", MARGIN + 170, y + 12, 7.5f, true);
        drawCell(canvas, paint, "Amount", MARGIN + 442, y + 12, 7.5f, true);
        y += headerH;

        List<PriceRow> rows = data.pricedOptions.isEmpty()
                ? java.util.Collections.singletonList(new PriceRow("No priced options selected", "", "$0.00"))
                : data.pricedOptions;
        int shown = Math.min(rows.size(), 6);
        int rowH = 18;
        for (int i = 0; i < shown; i++) {
            PriceRow row = rows.get(i);
            fillRect(canvas, paint, MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowH, Color.WHITE);
            strokeRect(canvas, paint, MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowH, GRID);
            line(canvas, paint, MARGIN + 164, y, MARGIN + 164, y + rowH, GRID);
            line(canvas, paint, MARGIN + 436, y, MARGIN + 436, y + rowH, GRID);
            drawCell(canvas, paint, oneLine(row.item, 31), MARGIN + 7, y + 12, 7.2f, false);
            drawCell(canvas, paint, oneLine(row.details, 47), MARGIN + 171, y + 12, 7.2f, false);
            drawRight(canvas, paint, row.amount, MARGIN + CONTENT_WIDTH - 7, y + 12, 7.2f, true);
            y += rowH;
        }
        fillRect(canvas, paint, MARGIN, y, MARGIN + CONTENT_WIDTH, y + 19, LIGHT);
        strokeRect(canvas, paint, MARGIN, y, MARGIN + CONTENT_WIDTH, y + 19, GRID);
        drawRight(canvas, paint, "ESTIMATED TOTAL  " + data.estimatedTotal,
                MARGIN + CONTENT_WIDTH - 7, y + 13, 8f, true);
        return y + 19;
    }

    private static int drawProjectOptions(Canvas canvas, Paint paint, int y, Data data) {
        y = sectionTitle(canvas, paint, y, "SINKS AND PROJECT OPTIONS");
        int labelW = 114;
        y = summaryRow(canvas, paint, y, 31, labelW, "SINK SELECTIONS", data.sinkSelections);
        y = summaryRow(canvas, paint, y, 24, labelW, "SINK LOCATION NOTE", data.sinkLocationNote);
        y = summaryRow(canvas, paint, y, 21, labelW, "WATERFALL SIDES", data.waterfall);
        y = summaryRow(canvas, paint, y, 24, labelW, "CABINETS", data.cabinets);
        return y;
    }

    private static int drawDrawingSummary(Canvas canvas, Paint paint, int y, Data data) {
        y = sectionTitle(canvas, paint, y, "DRAWING AND MEASUREMENT SUMMARY");
        int h = 49;
        fillRect(canvas, paint, MARGIN, y, MARGIN + CONTENT_WIDTH, y + h, LIGHT);
        strokeRect(canvas, paint, MARGIN, y, MARGIN + CONTENT_WIDTH, y + h, GRID);
        line(canvas, paint, MARGIN + 123, y, MARGIN + 123, y + h, GRID);
        line(canvas, paint, MARGIN + 232, y, MARGIN + 232, y + h, GRID);
        drawWrapped(canvas, paint, "COMBINED VERIFIED\nDRAWING ESTIMATE",
                MARGIN + 8, y + 14, 7.3f, 105, 2, true, Color.DKGRAY);
        drawCentered(canvas, paint, oneLine(data.drawingTotal, 20),
                MARGIN + 177, y + 30, 13f, BROWN, true);
        drawWrapped(canvas, paint, data.drawingDetails,
                MARGIN + 240, y + 14, 7.2f, CONTENT_WIDTH - 248, 3, false, Color.DKGRAY);
        return y + h;
    }

    private static int drawSectionsAndSlabs(Canvas canvas, Paint paint, int y, Data data) {
        y = sectionTitle(canvas, paint, y, "COUNTERTOP SECTIONS AND SELECTED SLABS");
        y = summaryRow(canvas, paint, y, 23, 114, "Countertop sections", data.countertopSections);
        y = summaryRow(canvas, paint, y, 23, 114, "Selected slabs", data.selectedSlabs);
        if (!"Not provided".equals(data.projectNotes)) {
            y = summaryRow(canvas, paint, y, 23, 114, "Project notes", data.projectNotes);
        }
        return y;
    }

    private static int summaryRow(
            Canvas canvas,
            Paint paint,
            int y,
            int h,
            int labelW,
            String label,
            String value) {
        fillRect(canvas, paint, MARGIN, y, MARGIN + CONTENT_WIDTH, y + h, Color.WHITE);
        strokeRect(canvas, paint, MARGIN, y, MARGIN + CONTENT_WIDTH, y + h, GRID);
        line(canvas, paint, MARGIN + labelW, y, MARGIN + labelW, y + h, GRID);
        drawWrapped(canvas, paint, label, MARGIN + 7, y + 13, 7.2f,
                labelW - 13, 2, true, Color.DKGRAY);
        drawWrapped(canvas, paint, value, MARGIN + labelW + 7, y + 13, 7.2f,
                CONTENT_WIDTH - labelW - 14, h > 25 ? 2 : 1, false, Color.DKGRAY);
        return y + h;
    }

    private static int sectionTitle(Canvas canvas, Paint paint, int y, String title) {
        fillRect(canvas, paint, MARGIN, y, MARGIN + CONTENT_WIDTH, y + 22, BROWN);
        drawCell(canvas, paint, title, MARGIN + 11, y + 15, 9f, true, Color.WHITE);
        return y + 22;
    }

    private static void drawFooter(Canvas canvas, Paint paint, int y) {
        line(canvas, paint, MARGIN, y, MARGIN + CONTENT_WIDTH, y, GRID);
        drawCell(canvas, paint, "Ramsier's - Quote Request Summary", MARGIN, y + 12, 6.5f, false, GRAY);
        drawRight(canvas, paint, "Page 1 of 1", MARGIN + CONTENT_WIDTH, y + 12, 6.5f, false);
    }

    private static void fillRect(Canvas canvas, Paint paint, float left, float top, float right, float bottom, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRect(left, top, right, bottom, paint);
    }

    private static void strokeRect(Canvas canvas, Paint paint, float left, float top, float right, float bottom, int color) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.65f);
        paint.setColor(color);
        canvas.drawRect(left, top, right, bottom, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private static void line(Canvas canvas, Paint paint, float x1, float y1, float x2, float y2, int color) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.65f);
        paint.setColor(color);
        canvas.drawLine(x1, y1, x2, y2, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private static void drawCell(Canvas canvas, Paint paint, String text, float x, float baseline, float size, boolean bold) {
        drawCell(canvas, paint, text, x, baseline, size, bold, Color.DKGRAY);
    }

    private static void drawCell(
            Canvas canvas,
            Paint paint,
            String text,
            float x,
            float baseline,
            float size,
            boolean bold,
            int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        paint.setTextSize(size);
        canvas.drawText(clean(text), x, baseline, paint);
    }

    private static void drawRight(
            Canvas canvas,
            Paint paint,
            String text,
            float x,
            float baseline,
            float size,
            boolean bold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.DKGRAY);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        paint.setTextSize(size);
        canvas.drawText(clean(text), x, baseline, paint);
    }

    private static void drawCentered(
            Canvas canvas,
            Paint paint,
            String text,
            float x,
            float baseline,
            float size,
            int color,
            boolean bold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        paint.setTextSize(size);
        canvas.drawText(clean(text), x, baseline, paint);
    }

    private static void drawWrapped(
            Canvas canvas,
            Paint paint,
            String text,
            float x,
            float firstBaseline,
            float size,
            float maxWidth,
            int maxLines,
            boolean bold,
            int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        paint.setTextSize(size);
        List<String> lines = wrap(text, paint, maxWidth, maxLines);
        for (int i = 0; i < lines.size(); i++) {
            canvas.drawText(lines.get(i), x, firstBaseline + i * (size + 2f), paint);
        }
    }

    private static List<String> wrap(String text, Paint paint, float maxWidth, int maxLines) {
        ArrayList<String> lines = new ArrayList<>();
        String clean = clean(text).replace('\n', ' ');
        String[] words = clean.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (paint.measureText(candidate) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            if (line.length() > 0) lines.add(line.toString());
            line.setLength(0);
            line.append(word);
            if (lines.size() == maxLines - 1) break;
        }
        if (lines.size() < maxLines && line.length() > 0) lines.add(line.toString());
        if (lines.isEmpty()) lines.add("");
        if (lines.size() == maxLines) {
            int last = lines.size() - 1;
            String remainder = lines.get(last);
            while (paint.measureText(remainder + "...") > maxWidth && remainder.length() > 2) {
                remainder = remainder.substring(0, remainder.length() - 1);
            }
            if (!remainder.equals(clean) && !remainder.endsWith("...")) remainder += "...";
            lines.set(last, remainder);
        }
        return lines;
    }

    private static String oneLine(String value, int maxCharacters) {
        String clean = clean(value).replace('\n', ' ');
        if (clean.length() <= maxCharacters) return clean;
        return clean.substring(0, Math.max(1, maxCharacters - 3)).trim() + "...";
    }

    private static String clean(String value) {
        if (value == null || value.trim().isEmpty()) return "Not provided";
        return value.trim();
    }
}
