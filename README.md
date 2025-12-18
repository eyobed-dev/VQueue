# VQueue

**VQueue** is a local queue management system built for Android that utilizes **Bluetooth Low Energy (BLE)** technology. It enables devices to operate without an internet connection, allowing users to host queues or join existing ones nearby. This is particularly useful for managing wait times in environments where internet connectivity is unreliable or unnecessary.

## 🚀 Key Features

*   **Offline Queue Management:** Operates entirely over BLE, requiring no internet connection.
*   **Role-Based Operation:**
    *   **Queue Server (Host):** Creates a queue and broadcasts its status (Wait Time, Current Ticket Number) to nearby devices.
    *   **Queue Client (Waiter):** Scans for nearby queues, joins them to receive a ticket, and monitors the queue status.
*   **Real-Time Monitoring:** Clients receive real-time updates from the host via BLE advertisements.
*   **Foreground Services:** Ensures reliable operation for scanning and hosting even when the app is in the background.
*   **Notifications:** Alerts users when it is their turn to be served.

## 🛠️ Technology Stack

*   **Language:** Java
*   **Platform:** Android (Min SDK 21, Target SDK 35)
*   **Build System:** Gradle
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **Database:** Room Database (for local storage of history and settings)
*   **Connectivity:** Android Bluetooth Low Energy (BLE) API
    *   `BluetoothLeAdvertiser` for hosting
    *   `BluetoothLeScanner` for discovering queues
    *   `BluetoothGatt` for connection and data exchange
*   **Background Tasks:** Foreground Services (`QueueConnectionService`, `QueueManagementService`, `QueueMonitoringService`)
*   **UI Components:**
    *   Navigation Component
    *   ConstraintLayout
    *   SwipeRefreshLayout
    *   Material Design Components

## 📱 How It Works

### Queue Server (Host)
1.  **Initialization:** The host sets up a queue with details like name and estimated wait time per person.
2.  **Broadcasting:** The app starts a **BLE Advertisement** containing the queue's current status (Name, Current Number, Wait Time).
3.  **Management:** The host can increment the "Current Number" to call the next person.

### Queue Client (Guest)
1.  **Discovery:** The app scans for advertising packets from nearby VQueue hosts.
2.  **Joining:** When a user joins a queue, the app connects to the host via **GATT**, sends a unique ID, and receives a **Ticket Number**.
3.  **Monitoring:** The app disconnects but continues to scan for the host's advertisements. A Foreground Service compares the advertised "Current Number" with the user's ticket.
4.  **Notification:** When the numbers match, the app triggers an alarm/notification to alert the user.

## 📦 Installation

1.  **Clone the Repository:**
    ```bash
    git clone https://gitlab.com/fit-vut1/tama/vqueue.git
    cd vqueue
    ```
    *(Note: Replace with the correct repository URL if different)*

2.  **Open in Android Studio:**
    *   Launch Android Studio.
    *   Select **Open an existing Android Studio project**.
    *   Navigate to the cloned directory and select it.

3.  **Build and Run:**
    *   Connect an Android device (Physical device recommended for BLE testing) or use an emulator with Bluetooth support.
    *   Click the **Run** button (green arrow) to build and install the app.

## ⚠️ Requirements & Permissions

*   **Android Device:** Android 5.0 (Lollipop) or higher.
*   **Bluetooth:** Device must support Bluetooth Low Energy (BLE).
*   **Permissions:** The app requires the following permissions to function:
    *   `BLUETOOTH`, `BLUETOOTH_ADMIN` (Legacy)
    *   `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT` (Android 12+)
    *   `ACCESS_FINE_LOCATION` (Required for BLE scanning on some Android versions)
    *   `FOREGROUND_SERVICE` (To keep scanning/hosting active)
    *   `POST_NOTIFICATIONS` (For alerts)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is currently unlicensed.
