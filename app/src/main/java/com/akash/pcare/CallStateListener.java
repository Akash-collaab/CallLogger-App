package com.akash.pcare;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class CallStateListener extends PhoneStateListener {

    private boolean isIncoming = false;
    private boolean wasRinging = false;
    private boolean callAnswered = false;
    private String incomingNumber = "";
    private final Context context;

    public CallStateListener(Context context) {
        this.context = context;
    }

    @Override
    public void onCallStateChanged(int state, String number) {
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                wasRinging = true;
                callAnswered = false;
                incomingNumber = number;
                break;

            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (wasRinging && isIncoming) {
                    callAnswered = true;
                } else {
                    // Outgoing call started
                    String contactName = CallLogUtils.getContactName(context, number);
                    String nameOrNumber = (contactName.equals("Unknown"))
                            ? number
                            : contactName + " (" + number + ")";
                    Toast.makeText(context, "📤 pCare: Outgoing Call to " + nameOrNumber, Toast.LENGTH_LONG).show();
                }
                break;

            case TelephonyManager.CALL_STATE_IDLE:
                if (wasRinging && isIncoming) {
                    String status = callAnswered ? "Received" : "Missed";
                    saveCallLog(context, incomingNumber, "Incoming", status);
                }

                // Reset flags
                isIncoming = false;
                wasRinging = false;
                callAnswered = false;
                incomingNumber = "";
                break;
        }
    }

    private void saveCallLog(Context context, String number, String type, String status) {
        String contactName = CallLogUtils.getContactName(context, number);
        String date = CallLogUtils.getCurrentFormattedDateTime();
        String duration = CallLogUtils.getCallDuration(context, number);

        CallLogModel model = new CallLogModel(number, contactName, type, date, duration, status);
       // ApiHelper.sendIncomingCall(context, model);  // You need to define this method or class
    }
}
