package com.akash.pcare;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

public class SyncService extends JobIntentService {
    private static final int JOB_ID = 1001;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, SyncService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d("SyncService", "Started background sync...");
        CallLogUtils.fetchAndSendLogs(this);
    }
}
