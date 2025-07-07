# 📱 CallLogger App

An efficient and lightweight Android app that logs all your phone calls – both **incoming** and **outgoing** – and presents them in a well-organized tabular format.  

It also determines whether a call was **received**, **missed**, or **rejected**, and shows you the contact name, duration, and time of the call.

---

## ✨ Features

✅ **Real-time call log display**  
✅ **Detects call type**:
- Incoming / Outgoing  
- Status: Received / Missed / Rejected  

✅ **Auto fetches contact names**  
✅ **Version-based auto-update display** (build-time versioning logic added)  
✅ **Runtime permissions handled**  
✅ **Built with RecyclerView for smooth scrolling**  
✅ **Clean UI and material design**  
✅ **Written in Java** (easy for beginner Android devs to understand)

---

## 🧠 How It Works

1. **Permissions**:  
   App requests `READ_CALL_LOG`, `READ_CONTACTS`, etc.

2. **Call Log Fetching**:  
   Uses `ContentResolver` to query Android's call log content provider.

3. **Contact Matching**:  
   Matches `phone numbers` from call logs with `ContactsContract` to display saved contact names.

4. **Call Type and Status Logic**:
   - **Incoming + duration > 0** → Received  
   - **Incoming + duration = 0** → Missed or Rejected (based on end state)  
   - **Outgoing + duration > 0** → Received  
   - **Outgoing + duration = 0** → Rejected  

5. **Backend API (Optional)**:  
   The app is integrated with an API endpoint (`InsertCallLog`) to sync logs online.

6. **Version Tracking Logic**:  
   Every time the app is rebuilt, a `BUILD_DATETIME_VERSION` is shown in the UI for traceability.

---

## 🖼️ Screenshots

_(Insert your screenshots here later for better visibility)_

---

## 🧰 Tech Stack

- **Java**
- **Android Studio**
- **Material Design UI**
- **RecyclerView**
- **Volley (for API calls)**
- **CallLog & ContactsContract API**

---

## 🔄 Future Improvements

- Sync with backend database in real-time  
- Search or filter call logs  
- Export logs as PDF or Excel  
- Night mode UI  
- Call tagging for CRM or support-based use cases

---

## 💡 Getting Started

1. Clone the repo:
   ```bash
   git clone https://github.com/Akash-collaab/CallLogger-App.git
