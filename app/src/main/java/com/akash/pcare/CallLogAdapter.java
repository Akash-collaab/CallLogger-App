package com.akash.pcare;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CallLogAdapter extends RecyclerView.Adapter<CallLogAdapter.ViewHolder> {

    private List<CallLogModel> callLogs;

    public CallLogAdapter(List<CallLogModel> callLogs) {
        this.callLogs = callLogs;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView number, contactName, type, date, duration, status;

        public ViewHolder(View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.number);
            contactName = itemView.findViewById(R.id.contact_name); // Optional: create TextView in layout
            type = itemView.findViewById(R.id.type);
            date = itemView.findViewById(R.id.date);
            duration = itemView.findViewById(R.id.duration);
            status = itemView.findViewById(R.id.status); // Optional: for Answered/Missed
        }
    }

    @NonNull
    @Override
    public CallLogAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.call_log_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CallLogModel log = callLogs.get(position);
        holder.number.setText("Number: " + log.getNumber());
        holder.contactName.setText("Name: " + log.getContactName());
        holder.type.setText("Type: " + log.getType());
        holder.date.setText("Date: " + log.getDate());
        holder.duration.setText("Duration: " + log.getDuration());
        holder.status.setText("Status: " + log.getCallStatus());
    }

    @Override
    public int getItemCount() {
        return callLogs.size();
    }

    // Utility to get contact name by phone number
    public static String getContactName(Context context, String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        String contactName = "Unknown";
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                contactName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
            }
            cursor.close();
        }
        return contactName;
    }
}
