<div align="center">

# FEMdefend

**A Next-Generation, Peer-to-Peer Women's Safety Ecosystem for Android**

[![Platform](https://img.shields.io/badge/Platform-Android_7.0%2B_(API_24--34)-brightgreen.svg?style=for-the-badge&logo=android)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Java_17-orange.svg?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
[![Backend](https://img.shields.io/badge/Backend-Firebase_Firestore_%26_Auth-ffca28.svg?style=for-the-badge&logo=firebase)](https://firebase.google.com/)
[![Voice Engine](https://img.shields.io/badge/Voice_Engine-Picovoice_Porcupine_3.0-blueviolet.svg?style=for-the-badge)](https://picovoice.ai/platform/porcupine/)
[![Local DB](https://img.shields.io/badge/Local_DB-Room_Persistence_Library-blue.svg?style=for-the-badge)](https://developer.android.com/training/data-storage/room)

FEMdefend is a production-ready Android application built to provide immediate personal safety assistance, covert background evidence recording, and on-demand legal and self-defense education. At its core is a **peer-to-peer emergency notification network** that intercepts designated SMS alerts on a guardian's device to trigger an instant alarm — bypassing conventional notification delivery constraints entirely.

[Features](#-features) · [Tech Stack](#️-tech-stack) · [Architecture](#-architecture) · [Database Design](#-database-design) · [Screenshots](#-screenshots) · [Getting Started](#-getting-started) · [Contributing](#-contributing)

</div>

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#️-tech-stack)
- [Architecture](#-architecture)
- [Database Design](#-database-design)
- [System Flow Diagrams](#-system-flow-diagrams)
- [Screenshots](#-screenshots)
- [Getting Started](#-getting-started)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

### 🎙️ Hands-Free Voice Trigger (Offline Wake-Word Detection)
- Powered by **Picovoice Porcupine v3.0.1**, running a compact deep-learning model entirely on-device — no audio data ever leaves the phone.
- Continuously listens in the background for the wake phrase **"Help Me"** with near-zero battery impact.
- A high-priority cancellation dialog with a countdown timer prevents accidental emergency dispatches before the full alert payload is fired.

### 📹 Intelligent Background Recording with Darkness Detection
- Spawns a background `LifecycleService` using **Android CameraX** to capture video evidence without interrupting the user's screen.
- A **real-time frame-brightness analyzer** computes the average YUV luminance of each camera frame. If the rear camera is obstructed or in complete darkness, the system automatically switches to the front camera to preserve evidence quality.

### 📡 P2P SMS Interception & Guardian Auto-Alerting
- A high-priority (`999`) `BroadcastReceiver` monitors incoming SMS messages for designated emergency tags and embedded Google Maps coordinates.
- When an alert is received on a guardian's device (provided they also have FEMdefend installed), the app intercepts the message, extracts the coordinates, and launches a full-screen panic alarm with map directions — overriding silent or do-not-disturb modes.

### 🗺️ Safe Route Navigation & Active Checkpoint Tracking
- Queries the **Google Directions API** with alternative routes enabled, decodes polyline coordinates asynchronously in `SafeRouteCalculator`, and renders the safest/shortest path in **green** with secondary alternatives in **red**.
- Dispatches the full route waypoint list to a foreground `LocationTrackingService`, which polls high-accuracy GPS every **10 seconds**.
- Checks proximity to each waypoint using `Location.distanceBetween()`. Upon entering a **20-metre radius**, the service advances to the next checkpoint automatically.
- On clearing the final waypoint, the service broadcasts `com.example.femfdefend.DESTINATION_REACHED`, updates the user's status in Firestore, and terminates the tracking thread.

### 🏥 Proximity-Based Emergency Services Querying
- Applies the **Haversine formula** against bounding-box Firestore queries to surface nearby hospitals, police stations, and women's NGOs within a **10 km radius** in real time, and automatically dispatches panic messages to relevant services.

---

## 🛠️ Tech Stack

| Layer | Technology |
| :--- | :--- |
| Language | Java 17 |
| Minimum SDK | Android 7.0 (API 24) |
| Target SDK | API 34 |
| Cloud Backend | Firebase Firestore, Firebase Auth |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| Wake-Word Engine | Picovoice Porcupine 3.0.1 |
| Camera | Android CameraX |
| Local Database | Room Persistence Library |
| Maps & Routing | Google Maps SDK, Google Directions API |

---

## 🏗️ Architecture

FEMdefend follows a **service-oriented architecture** on Android, with a clear separation between UI layers, background services, data models, and utility helpers.

```
app/src/main/
├── AndroidManifest.xml
└── java/com/example/femfdefend/
    ├── MainActivity.java
    ├── SplashActivity.java
    ├── LoginActivity.java
    ├── RegisterActivity.java
    ├── DashboardActivity.java
    ├── ProfileActivity.java
    │
    ├── adapters/               # RecyclerView adapters (Helpline, Laws, Videos)
    ├── dialogs/                # Custom dialog builders (Emergency countdown UI)
    ├── models/                 # Data models (User, EmergencyContact, Law, Video)
    ├── receivers/              # SMSReceiver — P2P background alert interceptor
    │
    ├── services/
    │   ├── FCMService.java                  # Handles remote push notification routing
    │   ├── LocationTrackingService.java     # Foreground GPS checkpoint monitoring
    │   ├── VoiceDetectionService.java       # Wake-lock, continuous wake-word listener
    │   ├── EmergencyRecordingService.java   # Background CameraX recording + brightness analysis
    │   └── EmergencyServiceHandler.java     # Coordinates emergency broadcast dispatch
    │
    └── utils/
        ├── DatabaseInitializer.java
        ├── DummyDataGenerator.java
        ├── FirebaseHelper.java              # Abstraction layer for Firestore operations
        └── SMSHelper.java
```

---

## 🗄️ Database Design

### Cloud Firestore (NoSQL)

#### `users` Collection

Stores authentication profile data and real-time coordinates during active tracking sessions.

| Field | Type | Description |
| :--- | :--- | :--- |
| `uid` | String | Unique Firebase Auth identifier |
| `fullName` | String | User's full name |
| `email` | String | Registered email address |
| `phone` | String | Registered phone number |
| `isEmergency` | Boolean | `true` when the user has an active panic alert |
| `lastLocation` | GeoPoint | Most recently recorded latitude/longitude |
| `lastEmergencyTime` | Long | Unix timestamp of the last panic trigger |

#### `users/{userId}/emergency_contacts` Sub-collection

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | String | Auto-generated contact identifier |
| `name` | String | Contact display name |
| `phone` | String | Contact phone number |
| `relationship` | String | Relationship label (e.g., Family, Friend, Spouse) |

---

### Local Room Database (SQLite)

#### Table: `emergency_recordings`

Caches metadata and file paths of all CameraX-recorded evidence on-device.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | Integer | Primary Key, Autogenerate | Recording sequence ID |
| `filePath` | String | NOT NULL | Absolute path to the MP4 file in internal storage |
| `timestamp` | Long | NOT NULL | Unix timestamp (ms) when recording began |
| `duration` | Long | NOT NULL | Total recording length in milliseconds |
| `cameraUsed` | String | NOT NULL | Source camera (`front` or `back`) |

---

## 🔄 System Flow Diagrams

### Emergency Activation Sequence

```mermaid
sequenceDiagram
    participant User
    participant Detector as Shake / Voice Detector
    participant Core as Dashboard / Service Handler
    participant GPS as Location Provider
    participant DB as Firestore & Room DB
    participant Alert as SMS & FCM Dispatch

    User->>Detector: Accelerometer shake OR vocal "Help Me"
    Detector->>Core: Trigger alarm event
    Core->>GPS: Request high-accuracy coordinates
    GPS-->>Core: Return latitude/longitude (GeoPoint)
    Core->>DB: Set isEmergency = true, log emergency record
    Core->>DB: Initiate CameraX foreground recording
    Core->>Alert: Dispatch SMS with Maps link to emergency contacts
    Core->>Alert: Send FCM push notification via backend
```

### P2P SMS Interception Flow

```mermaid
graph TD
    A[User Device: Alarm Triggered] -->|Send SMS with 'EMERGENCY ALERT' & map link| B(Carrier Network)
    B -->|Delivers SMS to guardian's phone| C[Guardian Device: SMSReceiver]
    C -->|Priority 999 — parse incoming message| D{Contains 'EMERGENCY ALERT'?}
    D -->|Yes| E[Extract embedded Google Maps coordinates]
    D -->|No| F[Pass SMS to default messaging app]
    E --> G[Trigger alarm sound & launch active map panel with directions]
```

---

## 📸 Screenshots

> **Adding screenshots:** Create a `screenshots/` folder at the project root and place your PNG/JPG files inside. GitHub will render the images automatically.

<p align="center">
  <img src="screenshots/dashboard.png" width="30%" alt="Dashboard Screen" />
  &nbsp;
  <img src="screenshots/live_tracking.png" width="30%" alt="Live Tracking Map" />
  &nbsp;
  <img src="screenshots/voice_detection.png" width="30%" alt="Voice Recognition Active" />
</p>
<p align="center">
  <img src="screenshots/self_defense.png" width="30%" alt="Self-Defense Videos" />
  &nbsp;
  <img src="screenshots/women_laws.png" width="30%" alt="Legal Catalog" />
  &nbsp;
  <img src="screenshots/emergency_recordings.png" width="30%" alt="Emergency Video Log" />
</p>

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- An active [Firebase project](https://console.firebase.google.com/) with Firestore and Authentication enabled
- A valid [Google Maps API key](https://developers.google.com/maps/documentation/android-sdk/get-api-key) with the Directions API enabled
- A [Picovoice access key](https://console.picovoice.ai/)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/femdefend.git
   cd femdefend
   ```

2. **Connect Firebase**
   - Download your `google-services.json` from the Firebase console and place it in the `app/` directory.

3. **Configure API keys**
   Add the following to your `local.properties` file (never commit this file):
   ```properties
   MAPS_API_KEY=your_google_maps_api_key
   PICOVOICE_ACCESS_KEY=your_picovoice_access_key
   ```

4. **Sync and build**
   Open the project in Android Studio, let Gradle sync complete, then build and run on a physical device (API 24+).

> **Note:** Several features — including wake-word detection, SMS interception, and background recording — require a physical device. They will not function correctly on an emulator.

---

## 🤝 Contributing

Contributions are welcome and appreciated. To get started:

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit your changes with a clear message: `git commit -m 'Add: description of change'`
4. Push to your fork: `git push origin feature/your-feature-name`
5. Open a Pull Request against the `main` branch.

Please ensure your code follows the existing style conventions and that any new features are accompanied by appropriate documentation.

---

<<<<<<< HEAD
=======


>>>>>>> fd9ae1f (resolved merge conflict)
<div align="center">
  Built with ❤️ for women's safety.
</div>
