# WatchCareAI

WatchCareAI is a real-time AI-powered fall detection and alerting system designed for healthcare environments. Using pose estimation and motion analysis, the system detects subtle patient incidents such as fainting, slumping, or sliding from chairs and instantly alerts healthcare workers through a mobile app.

---

## Inspiration

WatchCareAI was inspired by a real emergency room experience where we saw firsthand how overwhelmed healthcare workers can become during peak stress situations.

In crowded and understaffed ER waiting rooms, subtle patient incidents can easily go unnoticed.

We wanted to build a system that acts as an extra set of eyes for healthcare staff, helping improve patient safety through real-time monitoring and intelligent alerts.

---

## What It Does

WatchCareAI continuously analyzes live video streams using pose estimation and motion tracking to identify possible fall-related incidents.

When suspicious activity is detected, the system:

- Detects posture instability or falls
- Monitors motionlessness over time
- Generates a structured alert with:
  - Timestamp
  - Location
  - Alert status
- Sends alerts instantly to a nurse-facing Android application

The nurse app allows healthcare workers to:

- View live alerts
- Claim incidents to avoid duplicate responses
- Resolve alerts after assistance is provided
- Maintain a clear incident audit trail

---

## System Architecture

```text
Camera Feed
      ↓
Pose Estimation
      ↓
Fall Detection Logic
      ↓
Firebase Firestore
      ↓
Nurse Mobile App
```

---

## How We Built It

### AI & Detection System

- Built using Python, OpenCV, and MediaPipe Pose
- Performs real-time pose estimation on video feeds
- Tracks:
  - Body angles
  - Posture changes
  - Motion consistency over time

The system flags incidents when:

- Body angle falls below a threshold
- The person remains motionless for more than 4 seconds

### Backend

- Powered by Firebase Firestore
- Stores alerts in real time
- Synchronizes updates instantly across connected devices

### Android Application

- Built in Kotlin using Android Studio
- Listens to Firestore updates in real time
- Supports:
  - Alert notifications
  - Claim functionality
  - Resolve functionality
  - Live alert dashboard

---

## Features

- Real-time fall detection
- Pose estimation using MediaPipe
- Firebase-powered live synchronization
- Nurse alert management system
- Claim & resolve workflow
- Timestamped incident tracking
- Mobile-first healthcare monitoring

---

## Challenges We Faced

- Designing reliable real-time communication between AI and mobile systems
- Coordinating Firebase schema development across multiple contributors
- Reducing false positives while still detecting subtle incidents
- Debugging cross-platform integration under hackathon time constraints

---

## What We Learned

Through WatchCareAI, we gained hands-on experience with:

- Real-time AI systems
- Computer vision and pose estimation
- Firebase backend architecture
- Android app development
- State synchronization across distributed systems
- Building safety-focused applications under pressure

Most importantly, we learned how to design technology around real-world human problems.

---

## Future Improvements

As WatchCareAI evolves, we plan to:

- Improve AI accuracy and context awareness
- Add multi-person tracking
- Reduce false positives using temporal analysis
- Prioritize alerts by severity and confidence
- Incorporate environmental and behavioral context
- Train models using real-world healthcare data
- Expand beyond fall detection into broader patient safety monitoring

---

## Built With

- Python
- OpenCV
- MediaPipe Pose
- Firebase Firestore
- Kotlin
- Android Studio
- GitHub

---

## Getting Started

### Prerequisites

- Python 3.x
- Android Studio
- Firebase Project Setup
- Webcam or video feed source

### Clone the Repository

```bash
git clone https://github.com/your-username/WatchCareAI.git
cd WatchCareAI
```

### Install Dependencies

```bash
pip install -r requirements.txt
```

### Run the AI Detection System

```bash
python main.py
```

### Run the Android App

1. Open the Android project in Android Studio
2. Connect your Firebase configuration
3. Build and run the app on an emulator or Android device

---

## Repository Structure

```text
WatchCareAI/
│
├── ai-detection/
├── android-app/
├── firebase/
├── assets/
├── README.md
└── requirements.txt
```

---

## License

This project was created for educational and hackathon purposes.
