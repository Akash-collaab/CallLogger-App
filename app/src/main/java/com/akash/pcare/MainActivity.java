package com.akash.pcare;


import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;



public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    List<CallLogModel> callLogs = new ArrayList<>();
    private static final int PERMISSION_REQUEST_CODE = 100;
    private EditText editOwnMobile;
    private Button btnSaveNumber;
    private LinearLayout ownNumberLayout;
    private SharedPreferences sharedPreferences;

    private static final String OWN_MOBILE_KEY = "OwnMobileNumber";
    private static final String LAST_SENT_TIME_KEY = "LastCallSentTimeMillis";
    private static final String SENT_LOG_IDS_KEY = "SentCallLogIds";

    private static final String API_URL = "https://star.softtact.com:7218/api/CallLog_/InsertCallLog";
                                        // https://star.softtact.com/api/CallLog_/InsertCallLog

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("pCare");
        setContentView(R.layout.activity_main);

        TextView versionText = findViewById(R.id.versionText);
        versionText.setText(BuildConfig.BUILD_DATETIME_VERSION);


        recyclerView = findViewById(R.id.callLogRecyclerView);
        editOwnMobile = findViewById(R.id.editOwnMobile);
        btnSaveNumber = findViewById(R.id.btnSaveNumber);
        ownNumberLayout = findViewById(R.id.ownNumberLayout);
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        String savedNumber = sharedPreferences.getString(OWN_MOBILE_KEY, null);

        if (savedNumber != null && !savedNumber.isEmpty()) {
            ownNumberLayout.setVisibility(LinearLayout.GONE);
            checkPermissionsAndFetchLogs();
        } else {
            ownNumberLayout.setVisibility(LinearLayout.VISIBLE);
        }

        btnSaveNumber.setOnClickListener(v -> {
            String number = editOwnMobile.getText().toString().trim();
            if (number.length() >= 10) {
                sharedPreferences.edit().putString(OWN_MOBILE_KEY, number).apply();
                ownNumberLayout.setVisibility(LinearLayout.GONE);
                checkPermissionsAndFetchLogs();
            } else {
                Toast.makeText(this, "Please enter a valid 10-digit number", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPermissionsAndFetchLogs() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS},
                    PERMISSION_REQUEST_CODE);
        } else {
            fetchCallLogs();
            CallLogUtils.fetchAndSendLogs(this); // 🔁 Immediately try to send any unsent logs
        }
    }

    private void fetchCallLogs() {
        callLogs.clear();

        Cursor cursor = getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null, null, null,
                CallLog.Calls.DATE + " DESC"
        );

        long todayStartMillis = getTodayStartMillis();
        Set<String> sentIds = new HashSet<>(sharedPreferences.getStringSet(SENT_LOG_IDS_KEY, new HashSet<>()));

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                if (dateMillis < todayStartMillis) break;

                String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                long duration = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));

                String formattedDate = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                        .format(new Date(dateMillis));

                String callType, callStatus;
                if (type == CallLog.Calls.OUTGOING_TYPE) {
                    callType = "Outgoing";
                    callStatus = duration > 0 ? "Received" : "Rejected/Missed";
                } else if (type == CallLog.Calls.INCOMING_TYPE) {
                    callType = "Incoming";
                    callStatus = duration > 0 ? "Received" : "Rejected/Missed";
                } else if (type == CallLog.Calls.MISSED_TYPE || duration == 0) {
                    callType = "Incoming";
                    callStatus = "Rejected/Missed";
                } else {
                    callType = "Unknown";
                    callStatus = "N/A";
                }

                String contactName = getContactName(this, number);
                CallLogModel log = new CallLogModel(number, contactName, callType, formattedDate, duration + " sec", callStatus);

                String uniqueLogId = number + "_" + dateMillis;
                if (sentIds.contains(uniqueLogId)) continue;

                callLogs.add(log);
                sendCallLogToApi(log, dateMillis, uniqueLogId, sentIds);

            } while (cursor.moveToNext());

            cursor.close();
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new CallLogAdapter(callLogs));
    }

    private void sendCallLogToApi(CallLogModel log, long logTimestampMillis, String uniqueLogId, Set<String> sentIds) {
        try {
            String ownMobile = sharedPreferences.getString(OWN_MOBILE_KEY, null);
            if (ownMobile == null || ownMobile.isEmpty()) return;

            RequestQueue queue = Volley.newRequestQueue(this);

            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
            isoFormat.setTimeZone(TimeZone.getDefault());
            String isoDate = isoFormat.format(new Date(logTimestampMillis));

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("OwnMobileNumber", ownMobile);
            jsonBody.put("CustomerMobileNumber", log.getNumber());
            jsonBody.put("ContactName", log.getContactName() != null ? log.getContactName() : "Unknown");
            jsonBody.put("InOut", log.getType().equals("Outgoing"));
            jsonBody.put("CallStatus", log.getCallStatus());
            jsonBody.put("CallDateTime", isoDate);
            jsonBody.put("DurationInSec", Integer.parseInt(log.getDuration().replace(" sec", "")));
            jsonBody.put("Comments", "pCare");

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    API_URL,
                    jsonBody,
                    response -> {
                        Log.d("API", "Success: " + response);
                        sentIds.add(uniqueLogId);
                        sharedPreferences.edit().putStringSet(SENT_LOG_IDS_KEY, sentIds).commit(); // ✅ save
                    },
                    error -> {
                        String errorDetails = error.toString();
                        if (error.networkResponse != null) {
                            errorDetails += "\nStatus Code: " + error.networkResponse.statusCode;
                            if (error.networkResponse.data != null) {
                                errorDetails += "\nResponse: " + new String(error.networkResponse.data);
                            }
                        }
                        Log.e("API", "Failed to send: " + errorDetails);
                    }
            );

// 🛠️ Set custom timeout
            request.setRetryPolicy(new DefaultRetryPolicy(
                    15000, // ⏱ 15 seconds
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            queue.add(request);


            queue.add(request);
        } catch (Exception e) {
            Log.e("API", "Unexpected Error: " + e.getMessage());
        }
    }


    private long getTodayStartMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean callLogGranted = false;
            boolean contactsGranted = false;

            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.READ_CALL_LOG)) {
                    callLogGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                }
                if (permissions[i].equals(Manifest.permission.READ_CONTACTS)) {
                    contactsGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                }
            }

            if (callLogGranted && contactsGranted) {
                fetchCallLogs();
            } else {
                Toast.makeText(this, "Permissions Denied!", Toast.LENGTH_SHORT).show();
            }
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

            cursor = context.getContentResolver().query(
                    uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.PhoneLookup.DISPLAY_NAME));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

        return name;
    }
}
