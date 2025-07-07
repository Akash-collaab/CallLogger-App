package com.akash.pcare;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

public class CallLogUtils {

    private static final String OWN_MOBILE_KEY = "OwnMobileNumber";
    private static final String SENT_LOG_IDS_KEY = "SentCallLogIds";
    private static final String API_URL = "https://star.softtact.com:7218/api/CallLog_/InsertCallLog";

    public static void fetchAndSendLogs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String ownMobile = prefs.getString(OWN_MOBILE_KEY, null);
        if (ownMobile == null || ownMobile.isEmpty()) return;

        long todayStartMillis = getTodayStartMillis();
        Set<String> sentIds = new HashSet<>(prefs.getStringSet(SENT_LOG_IDS_KEY, new HashSet<>()));

        Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI, null, null, null,
                CallLog.Calls.DATE + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                if (dateMillis < todayStartMillis) continue;

                String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                long duration = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));

                String callType = (type == CallLog.Calls.OUTGOING_TYPE) ? "Outgoing" : "Incoming";
                String callStatus = (duration > 0) ? "Received" : "Rejected/Missed";

                String contactName = getContactName(context, number);
                String formattedDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
                        .format(new Date(dateMillis));
                String uniqueLogId = number + "_" + dateMillis;

                if (sentIds.contains(uniqueLogId)) continue;

                CallLogModel model = new CallLogModel(
                        number, contactName, callType, formattedDate, duration + " sec", callStatus
                );
                sendCallLogToApi(context, model, dateMillis, uniqueLogId, sentIds, prefs);

            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    private static void sendCallLogToApi(
            Context context, CallLogModel log, long logTimestamp,
            String uniqueLogId, Set<String> sentIds, SharedPreferences prefs
    ) {
        try {
            RequestQueue queue = Volley.newRequestQueue(context);
            String ownMobile = prefs.getString(OWN_MOBILE_KEY, null);

            SimpleDateFormat isoFormat = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()
            );
            isoFormat.setTimeZone(TimeZone.getDefault());
            String isoDate = isoFormat.format(new Date(logTimestamp));

            JSONObject json = new JSONObject();
            json.put("OwnMobileNumber", ownMobile);
            json.put("CustomerMobileNumber", log.getNumber());
            json.put("ContactName", log.getContactName());
            json.put("InOut", log.getType().equals("Outgoing"));
            json.put("CallStatus", log.getCallStatus());
            json.put("CallDateTime", isoDate);
            json.put("DurationInSec", Integer.parseInt(log.getDuration().replace(" sec", "")));
            json.put("Comments", "pCare");

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, API_URL, json,
                    response -> {
                        Log.d("API", "CallLog sent successfully");
                        sentIds.add(uniqueLogId);
                        prefs.edit().putStringSet(SENT_LOG_IDS_KEY, sentIds).commit(); // commit() sync save
                    },
                    error -> {
                        String errorDetails = error.toString();
                        if (error.networkResponse != null) {
                            errorDetails += "\nStatus Code: " + error.networkResponse.statusCode;
                            if (error.networkResponse.data != null) {
                                errorDetails += "\nResponse Data: " + new String(error.networkResponse.data);
                            }
                        }
                        Log.e("API", "Failed to send: " + errorDetails);
                    }

            );
            queue.add(request);

        } catch (Exception e) {
            Log.e("API", "Exception: " + e.getMessage());
        }
    }

    public static String getContactName(Context context, String phoneNumber) {
        if (context == null || phoneNumber == null) return "Unknown";
        Cursor cursor = null;
        String name = "Unknown";
        try {
            Uri uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(phoneNumber)
            );
            cursor = context.getContentResolver().query(uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return name;
    }

    public static String getCurrentFormattedDateTime() {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());
    }

    public static String getCallDuration(Context context, String number) {
        Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null,
                CallLog.Calls.NUMBER + "=?",
                new String[]{number},
                CallLog.Calls.DATE + " DESC"
        );
        if (cursor != null && cursor.moveToFirst()) {
            long duration = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));
            cursor.close();
            return duration + " sec";
        }
        return "0 sec";
    }

    private static long getTodayStartMillis() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
