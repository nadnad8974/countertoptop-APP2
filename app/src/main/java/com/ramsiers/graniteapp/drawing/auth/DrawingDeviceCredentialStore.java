package com.ramsiers.graniteapp.drawing.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Stores the Drawing AI credential encrypted by a non-exportable Android Keystore key. */
public final class DrawingDeviceCredentialStore {
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "ramsiers_drawing_device_credential_v1";
    private static final String PREFERENCES = "ramsiers_drawing_device_credential_store";
    private static final String PREF_IV = "encrypted_credential_iv_v1";
    private static final String PREF_CIPHERTEXT = "encrypted_credential_ciphertext_v1";
    private static final String PAYLOAD_VERSION = "1";
    private static final byte[] ASSOCIATED_DATA =
            KEY_ALIAS.getBytes(StandardCharsets.UTF_8);

    private final SharedPreferences preferences;

    public DrawingDeviceCredentialStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public synchronized void save(DrawingDeviceCredential credential) throws Exception {
        if (credential == null) throw new IllegalArgumentException("Credential is required.");
        SecretKey key = getOrCreateKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        cipher.updateAAD(ASSOCIATED_DATA);
        byte[] plaintext = (PAYLOAD_VERSION + "\n"
                + credential.deviceId() + "\n"
                + credential.token()).getBytes(StandardCharsets.UTF_8);
        try {
            byte[] ciphertext = cipher.doFinal(plaintext);
            boolean saved = preferences.edit()
                    .putString(PREF_IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .putString(PREF_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                    .commit();
            if (!saved) throw new IllegalStateException("Credential storage failed.");
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    public synchronized DrawingDeviceCredential load() {
        String encodedIv = preferences.getString(PREF_IV, "");
        String encodedCiphertext = preferences.getString(PREF_CIPHERTEXT, "");
        if (encodedIv == null || encodedIv.isEmpty()
                || encodedCiphertext == null || encodedCiphertext.isEmpty()) {
            return null;
        }

        byte[] plaintext = null;
        try {
            byte[] iv = Base64.decode(encodedIv, Base64.NO_WRAP);
            byte[] ciphertext = Base64.decode(encodedCiphertext, Base64.NO_WRAP);
            if (iv.length != 12 || ciphertext.length < 16) throw new IllegalStateException();

            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            java.security.Key key = keyStore.getKey(KEY_ALIAS, null);
            if (!(key instanceof SecretKey)) throw new IllegalStateException();

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            cipher.updateAAD(ASSOCIATED_DATA);
            plaintext = cipher.doFinal(ciphertext);
            String payload = new String(plaintext, StandardCharsets.UTF_8);
            String[] fields = payload.split("\n", -1);
            if (fields.length != 3 || !PAYLOAD_VERSION.equals(fields[0])) {
                throw new IllegalStateException();
            }
            return new DrawingDeviceCredential(fields[1], fields[2]);
        } catch (Exception exception) {
            clearEncryptedValues();
            return null;
        } finally {
            if (plaintext != null) Arrays.fill(plaintext, (byte) 0);
        }
    }

    public synchronized boolean disconnect() {
        boolean encryptedRecordRemoved = clearEncryptedValues();
        boolean keyRemoved = false;
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS);
            keyRemoved = !keyStore.containsAlias(KEY_ALIAS);
        } catch (Exception ignored) {
            // Without the encrypted record, an orphaned non-exportable key cannot authenticate.
        }
        return encryptedRecordRemoved || keyRemoved;
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        java.security.Key existing = keyStore.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;

        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEY_STORE);
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }

    private boolean clearEncryptedValues() {
        return preferences.edit().remove(PREF_IV).remove(PREF_CIPHERTEXT).commit();
    }
}
