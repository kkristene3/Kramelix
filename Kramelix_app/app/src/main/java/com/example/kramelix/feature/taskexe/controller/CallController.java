package com.example.kramelix.feature.taskexe.controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.kramelix.MainActivity;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This controller handles all phone-call related functionality.
 *
 * <p>It serves as a feature-level abstraction for calling contacts or phone numbers,
 * including permission checks, contact lookups, and name normalization.</p>
 *
 * <br>
 * <strong>Responsibilities</strong>
 * <ul>
 *     <li>Executes direct number calls (e.g. "call 6131234567").</li>
 *     <li>Searches contacts by name, normalizes them, & calls if a match is found.</li>
 *     <li>Provides a clean list of device contact names for the LLM layer.</li>
 * </ul>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>All calls must be made from a valid {@link Context} that can request permissions.</li>
 *     <li>READ_CONTACTS and CALL_PHONE permissions are required.</li>
 *     <li>Unknown names must fail gracefully without throwing exceptions.</li>
 * </ul>
 *
 * @author Amy Huang
 * @noinspection DynamicRegexReplaceableByCompiledPattern, BooleanMethodNameMustStartWithQuestion
 * @since 1.0
 */
public final class CallController {

    // -------------------- STATE --------------------
    /**
     * Logcat tag.
     */
    private static final String TAG = "CallController";

    // -------------------- STATE --------------------
    /**
     * Current context.
     */
    private final Context context;

    // ------------------------- LIFECYCLE -------------------------

    /**
     * Constructor.
     *
     * @param context Current valid {@link Context}.
     */
    private CallController(@NonNull Context context) {
        // INITIALIZATION:
        this.context = context;
    }

    /**
     * Factory instance.
     *
     * @param context Current valid {@link Context}.
     * @return The controller instance.
     */
    public static CallController getInstance(@NonNull Context context) {
        // OUTPUT:
        return new CallController(context);
    }

    // ----------------------- PUBLIC ENTRY POINT -----------------------

    /**
     * This function determines whether to call a phone number or a contact by name,
     * depending on the user-provided string target.
     *
     * @param target String representing the number or contact name.
     * @return Boolean representing the success of the call attempt.
     */
    @SuppressLint("LogConditional")
    boolean handleCallRequest(@NonNull String target) {

        // LOG OUTPUT: debugging
        Log.d(TAG, "handleCallRequest(" + target + ")");

        // PROCESS: assuming the target is a number if it contains digits
        if (target.matches(".*\\d+.*")) { // number
            // OUTPUT:
            return callNumber(target);
        } else { // contact
            // OUTPUT:
            return callContactByName(target);
        }

    }

    // ----------------------- TASK: CALL BY NUMBER -----------------------

    /**
     * This function initiates a direct call to a given phone number.
     *
     * @param number The raw phone number provided by the user.
     * @return {@code true} if the call was successfully initiated; {@code false} otherwise.
     */
    @SuppressLint("LogConditional")
    private boolean callNumber(@NonNull String number) {

        // LOG OUTPUT: debugging
        Log.d(TAG, "This is the given phone number: " + number);

        try {

            // PROCESS: ensuring CALL_PHONE permission
            if (context instanceof MainActivity) {

                // VARIABLE DECLARATION: retrieving main context
                MainActivity activity = (MainActivity) context;

                if (!activity.ensureCallPermission()) {

                    // OUTPUT: missing permission
                    Log.w(TAG, "Missing CALL_PHONE permission.");
                    return false;

                }

            }

            // VARIABLE DECLARATION: cleanup & preparing call intent
            String clean = number.replaceAll("[^\\d+]", "");

            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + clean));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // PROCESS: starting call activity
            context.startActivity(intent);

            // OUTPUT:
            Log.i(TAG, "Calling " + clean);
            return true;

        } catch (RuntimeException e) { // error-handling

            // OUTPUT:
            Log.e(TAG, "callNumber failed: " + e.getMessage());
            return false;

        }
    }

    // ----------------------- TASK: CALL BY CONTACT NAME -----------------------

    /**
     * This function searches for a contact by name & initiates a call if found.
     *
     * <p>It retrieves all contacts via {@link #getAllContactNames()},
     * normalizes both the input and stored names, & performs a flexible match.</p>
     *
     * @param contactName The display name of the contact to call.
     * @return {@code true} if a matching contact was found & the call was initiated; {@code false} otherwise.
     */
    private boolean callContactByName(@NonNull String contactName) {

        try {

            // PROCESS: ensuring contact permission
            if (!hasContactPermission()) return false;

            // PROCESS: fetching all contacts (human-readable names)
            List<String> contacts = getAllContactNames();

            if (contacts.isEmpty()) { // no contacts found

                // OUTPUT:
                Log.w(TAG, "No contacts retrieved from device.");
                return false;

            }

            // PROCESS: normalizing target name for matching
            String normalizedTarget = normalizeName(contactName);
            String bestMatchName = null;

            // PROCESS: iterating over contacts to find the best match
            for (String displayName : contacts) {

                String normalizedDisplay = normalizeName(displayName);

                if (normalizedDisplay.contains(normalizedTarget)
                        || normalizedTarget.contains(normalizedDisplay)) { // found match

                    bestMatchName = displayName;
                    break;

                }

            }

            // PROCESS: retrieving number for the matched name
            if (null != bestMatchName) {

                String phoneNumber = getNumberForContact(bestMatchName);

                if (null != phoneNumber) { // number found

                    // OUTPUT:
                    Log.i(TAG, "Matched contact: " + bestMatchName + " (" + phoneNumber + ")");
                    return callNumber(phoneNumber);

                }

            }

            // OUTPUT: no match found
            Log.w(TAG, "No contact found matching: " + normalizedTarget);
            return false;

        } catch (SecurityException e) { // error-handling

            // OUTPUT:
            Log.e(TAG, "Permission error: " + e.getMessage());
            return false;

        } catch (RuntimeException e) { // error-handling

            // OUTPUT:
            Log.e(TAG, "Runtime error: " + e.getMessage());
            return false;

        }

    }

    // ----------------------- HELPER FUNCTIONS: RETRIEVE CONTACT LIST -----------------------

    /**
     * This helper function returns a clean list of all distinct contact display names on the device.
     *
     * <p>Used by both the LLM and {@link #callContactByName(String)} to enable name matching.</p>
     *
     * @return List of unique contact display names, or empty list if none/missing permissions.
     */
    @SuppressLint("Range")
    @NonNull
    public List<String> getAllContactNames() {

        // PROCESS: ensuring READ_CONTACTS permission
        if (!hasContactPermission()) return new ArrayList<>(0);

        // PROCESS: building query
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};

        Set<String> uniqueNames = new LinkedHashSet<>(0);

        // PROCESS: running query
        try (android.database.Cursor cursor = context.getContentResolver().query(
                uri, projection, null, null, null)) {

            if (null == cursor) { // error-handling

                // OUTPUT: missing cursor
                Log.w(TAG, "getAllContactNames: null cursor");
                return new ArrayList<>(0);

            }

            while (cursor.moveToNext()) {

                // VARIABLE DECLARATION: retrieving contact name
                String displayName = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

                if (null == displayName) continue; // skipping null name

                String cleaned = displayName.trim(); // cleaning name

                if (!cleaned.isEmpty()) {
                    uniqueNames.add(cleaned); // adding to set
                }

            }

        } catch (SecurityException se) { // error-handling

            // OUTPUT: missing permission
            Log.e(TAG, "getAllContactNames: missing READ_CONTACTS: " + se.getMessage());
            return new ArrayList<>(0);

        } catch (RuntimeException re) { // error-handling

            // OUTPUT: runtime error
            Log.e(TAG, "getAllContactNames: runtime error: " + re.getMessage());
            return new ArrayList<>(0);

        }

        // OUTPUT: the cleaned list of contacts
        return new ArrayList<>(uniqueNames);

    }

    // ----------------------- HELPER FUNCTIONS: RETRIEVE CONTACT NUMBER -----------------------

    /**
     * This helper function retrieves the primary phone number for a given contact display name.
     *
     * @param contactName The exact contact display name.
     * @return The phone number string, or null if none found.
     */
    @Nullable
    @SuppressLint("Range")
    private String getNumberForContact(@NonNull String contactName) {

        // VARIABLE DECLARATION: building query
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};

        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " = ?";
        String[] selectionArgs = {contactName};

        // PROCESS: running query
        try (android.database.Cursor cursor =
                     context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {

            if (null != cursor && cursor.moveToFirst()) { // found
                // OUTPUT: returning number
                return cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            }

        } catch (RuntimeException e) { // error-handling
            // OUTPUT:
            Log.e(TAG, "Failed to retrieve number for " + contactName + ": " + e.getMessage());
        }

        // OUTPUT: no number found
        return null;

    }

    // ----------------------- HELPER FUNCTIONS: NORMALIZATION -----------------------

    /**
     * This helper function normalizes a name string for comparison by:
     * <ul>
     *     <li>Removing accents (e.g. é → e)</li>
     *     <li>Removing punctuation & emojis</li>
     *     <li>Lowercasing</li>
     *     <li>Collapsing extra spaces</li>
     * </ul>
     *
     * @param name Input display name.
     * @return A normalized, lowercase version suitable for fuzzy comparison.
     */
    private static String normalizeName(@NonNull String name) {

        // PROCESS: removing emojis/non-alphanumeric characters (but keeping spaces)
        String clean = name.replaceAll("[^\\p{L}\\p{Nd} ]+", " ");

        // PROCESS: decomposing accents & removing diacritics
        clean = Normalizer.normalize(clean, Normalizer.Form.NFD).replaceAll("\\p{M}", "");

        // PROCESS: lowercasing & trimming
        clean = clean.toLowerCase().trim().replaceAll("\\s+", " ");

        // OUTPUT: the cleaned name
        return clean;

    }

    // -------------------- HELPER FUNCTIONS: PERMISSIONS --------------------

    /**
     * Getter function for whether the READ_CONTACTS permission is granted.
     *
     * @return {@code true} if the permission is granted; {@code false} otherwise.
     * @noinspection BooleanMethodIsAlwaysInverted
     */
    private boolean hasContactPermission() {

        if (context instanceof MainActivity) {

            // VARIABLE DECLARATION: retrieving main context
            MainActivity activity = (MainActivity) context;

            if (!activity.ensureCallPermission()) {

                // OUTPUT: missing permission
                Log.w(TAG, "Missing READ_CONTACTS permission.");
                return false;

            }

            // OUTPUT: otherwise, permissions enabled
            return true;

        }

        // OUTPUT: bad context
        Log.w(TAG, "Missing MainActivity context for READ_CONTACTS permission, but assuming previous permission was granted.");
        return true;

    }

}
