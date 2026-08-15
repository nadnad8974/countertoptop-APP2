package com.ramsiers.graniteapp;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class DriveJobStore {
    private static final int MAX_JOB_FILE_BYTES = 2 * 1024 * 1024;

    static final class JobEntry {
        final JSONObject data;
        final String folderName;
        final ArrayList<Uri> drawingUris;
        final ArrayList<Uri> countertopPhotoUris;
        final Uri signedQuoteUri;

        JobEntry(
                JSONObject data,
                String folderName,
                ArrayList<Uri> drawingUris,
                ArrayList<Uri> countertopPhotoUris,
                Uri signedQuoteUri) {
            this.data = data;
            this.folderName = folderName;
            this.drawingUris = drawingUris;
            this.countertopPhotoUris = countertopPhotoUris;
            this.signedQuoteUri = signedQuoteUri;
        }
    }

    private DriveJobStore() {
    }

    static String rootName(Context context, Uri treeUri) {
        DocumentFile root = DocumentFile.fromTreeUri(context, treeUri);
        return root == null || root.getName() == null ? "Selected Drive folder" : root.getName();
    }

    static JobEntry save(
            Context context,
            Uri treeUri,
            String folderName,
            JSONObject job,
            List<Uri> drawings,
            List<Uri> countertopPhotos,
            Uri signedQuoteUri) throws Exception {
        DocumentFile root = requireWritableRoot(context, treeUri);
        DocumentFile folder = findDirectory(root, folderName);
        if (folder == null) folder = root.createDirectory(folderName);
        if (folder == null || !folder.isDirectory()) {
            throw new IllegalStateException("The customer folder could not be created.");
        }

        DocumentFile drawingFolder = replaceDirectory(folder, "Drawings");
        DocumentFile photoFolder = replaceDirectory(folder, "Countertop Photos");
        ArrayList<Uri> storedDrawings = copyMedia(context, drawingFolder, drawings, "Drawing");
        ArrayList<Uri> storedPhotos = copyMedia(context, photoFolder, countertopPhotos, "Countertop Photo");

        Uri storedQuote = null;
        if (signedQuoteUri != null) {
            storedQuote = copyOne(
                    context,
                    folder,
                    signedQuoteUri,
                    "application/pdf",
                    "Signed Final Quote.pdf");
        }

        job.put("driveFolderName", folderName);
        job.put("savedAt", System.currentTimeMillis());
        writeJson(context, folder, job);
        return new JobEntry(job, folderName, storedDrawings, storedPhotos, storedQuote);
    }

    static ArrayList<JobEntry> list(Context context, Uri treeUri, String query) throws Exception {
        DocumentFile root = requireReadableRoot(context, treeUri);
        String needle = query == null ? "" : query.trim().toLowerCase(java.util.Locale.US);
        ArrayList<JobEntry> entries = new ArrayList<>();
        for (DocumentFile folder : root.listFiles()) {
            if (!folder.isDirectory()) continue;
            DocumentFile jobFile = folder.findFile("job.json");
            if (jobFile == null || !jobFile.isFile()) continue;
            JSONObject job = readJson(context, jobFile.getUri());
            if (!needle.isEmpty() && !job.toString().toLowerCase(java.util.Locale.US).contains(needle)) {
                continue;
            }
            entries.add(new JobEntry(
                    job,
                    folder.getName() == null ? job.optString("driveFolderName") : folder.getName(),
                    mediaUris(folder.findFile("Drawings")),
                    mediaUris(folder.findFile("Countertop Photos")),
                    fileUri(folder.findFile("Signed Final Quote.pdf"))));
        }
        Collections.sort(entries, (left, right) -> Long.compare(
                right.data.optLong("savedAt", 0), left.data.optLong("savedAt", 0)));
        return entries;
    }

    private static DocumentFile requireWritableRoot(Context context, Uri treeUri) {
        DocumentFile root = requireReadableRoot(context, treeUri);
        if (!root.canWrite()) throw new IllegalStateException("The selected folder is read-only.");
        return root;
    }

    private static DocumentFile requireReadableRoot(Context context, Uri treeUri) {
        DocumentFile root = DocumentFile.fromTreeUri(context, treeUri);
        if (root == null || !root.exists() || !root.isDirectory() || !root.canRead()) {
            throw new IllegalStateException("The shared customer folder must be connected again.");
        }
        return root;
    }

    private static DocumentFile findDirectory(DocumentFile parent, String name) {
        DocumentFile found = parent.findFile(name);
        return found != null && found.isDirectory() ? found : null;
    }

    private static DocumentFile replaceDirectory(DocumentFile parent, String name) {
        DocumentFile existing = parent.findFile(name);
        if (existing != null && existing.isDirectory()) return existing;
        if (existing != null) existing.delete();
        DocumentFile created = parent.createDirectory(name);
        if (created == null) throw new IllegalStateException(name + " could not be created.");
        return created;
    }

    private static ArrayList<Uri> copyMedia(
            Context context,
            DocumentFile destination,
            List<Uri> sources,
            String prefix) throws Exception {
        ArrayList<Uri> result = new ArrayList<>();
        Set<String> retainedNames = new HashSet<>();
        for (int i = 0; i < sources.size(); i++) {
            Uri source = sources.get(i);
            String mime = context.getContentResolver().getType(source);
            if (mime == null || !mime.startsWith("image/")) mime = "image/jpeg";
            String extension = mime.contains("png") ? ".png" : ".jpg";
            String name = prefix + " " + (i + 1) + extension;
            retainedNames.add(name);
            result.add(copyOne(context, destination, source, mime, name));
        }
        for (DocumentFile old : destination.listFiles()) {
            if (old.getName() != null && !retainedNames.contains(old.getName())) old.delete();
        }
        return result;
    }

    private static Uri copyOne(
            Context context,
            DocumentFile destination,
            Uri source,
            String mime,
            String name) throws Exception {
        DocumentFile old = destination.findFile(name);
        if (old != null && old.getUri().equals(source)) return source;
        if (old != null) old.delete();
        DocumentFile target = destination.createFile(mime, name);
        if (target == null) throw new IllegalStateException(name + " could not be saved.");
        ContentResolver resolver = context.getContentResolver();
        try (InputStream input = resolver.openInputStream(source);
             OutputStream output = resolver.openOutputStream(target.getUri(), "wt")) {
            if (input == null || output == null) throw new IllegalStateException(name + " could not be opened.");
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
        }
        return target.getUri();
    }

    private static void writeJson(Context context, DocumentFile folder, JSONObject job) throws Exception {
        DocumentFile file = folder.findFile("job.json");
        if (file == null) file = folder.createFile("application/json", "job.json");
        if (file == null) throw new IllegalStateException("job.json could not be created.");
        try (OutputStream output = context.getContentResolver().openOutputStream(file.getUri(), "wt")) {
            if (output == null) throw new IllegalStateException("job.json could not be opened.");
            output.write(job.toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static JSONObject readJson(Context context, Uri uri) throws Exception {
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) throw new IllegalStateException("A saved job could not be opened.");
            byte[] buffer = new byte[16 * 1024];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                total += read;
                if (total > MAX_JOB_FILE_BYTES) throw new IllegalStateException("A saved job file is too large.");
                output.write(buffer, 0, read);
            }
            return new JSONObject(output.toString(StandardCharsets.UTF_8.name()));
        }
    }

    private static ArrayList<Uri> mediaUris(DocumentFile folder) {
        ArrayList<Uri> uris = new ArrayList<>();
        if (folder == null || !folder.isDirectory()) return uris;
        ArrayList<DocumentFile> files = new ArrayList<>();
        for (DocumentFile file : folder.listFiles()) if (file.isFile()) files.add(file);
        Collections.sort(files, (left, right) -> String.valueOf(left.getName()).compareToIgnoreCase(String.valueOf(right.getName())));
        for (DocumentFile file : files) uris.add(file.getUri());
        return uris;
    }

    private static Uri fileUri(DocumentFile file) {
        return file != null && file.isFile() ? file.getUri() : null;
    }
}
