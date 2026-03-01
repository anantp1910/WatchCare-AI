# WatchCare-AI

Real-time fall detection and alerting system designed for hospital waiting rooms to prevent patient incidents from going unnoticed in understaffed emergency environments.

Built at Carnegie Mellon University's NexHacks (24-hour hackathon).

---

## 🚨 Problem

Hospital ER waiting rooms are often understaffed.

Patients may:
- Slump in chairs
- Faint while seated
- Slide off chairs
- Collapse without immediate supervision

Traditional camera monitoring requires constant human attention. In busy environments, subtle fall-related incidents can go unnoticed.

WatchCare-AI provides automated real-time monitoring and alerting to reduce response time and improve patient safety.

---

## ⚡ Solution Overview

WatchCare-AI is a computer vision pipeline that:

1. Monitors live video feed
2. Performs real-time pose estimation
3. Detects abnormal body angles and motion persistence
4. Triggers instant nurse alerts
5. Enables claim-and-resolve tracking for accountability

The system is designed to minimize false positives in safety-critical environments.

---

## 🧠 System Architecture

### 🔍 Computer Vision Layer
- Python
- OpenCV
- Pose estimation model
- Body angle threshold detection
- Motion persistence filtering logic

The detection logic evaluates:
- Torso angle deviation
- Sudden vertical displacement
- Sustained abnormal posture duration

This prevents alerts from being triggered by normal seated movements.

---

### 🔔 Alerting Layer
- Firebase Firestore
- Real-time event synchronization
- Audit logging
- Claim-and-resolve workflow for nurse accountability

Alerts include:
- Timestamp
- Camera ID
- Incident classification
- Status tracking (Open / Claimed / Resolved)

---

### 📱 Mobile Integration
Collaborated with Android developers to deliver:
- Instant push notifications
- Real-time alert dashboard
- Ownership tracking
- Resolution confirmation

---

## 🏗 Design Principles

- Real-time responsiveness
- False-positive minimization
- Clear ownership assignment
- Scalable system design
- Audit-ready logging

---

## 🛠 Tech Stack

- Python
- OpenCV
- Pose Estimation
- Firebase Firestore
- Android (integration layer)

---

## 🧪 Detection Logic (Simplified)

The system evaluates:

- Body angle > predefined threshold
- Vertical drop speed exceeding tolerance
- Motion persistence over defined time window

Only if multiple conditions are satisfied is an alert triggered.

This layered validation reduces alert noise in busy hospital environments.

---

## ⏱ Built In

Developed in a 24-hour hackathon environment at Carnegie Mellon University's NexHacks.

Focused on:
- Real-world healthcare safety gaps
- Scalable architecture
- Deployable MVP logic

---

## 📈 Impact Potential

- Reduced ER monitoring burden
- Faster incident response time
- Improved patient safety
- Real-time accountability tracking

---

## 🔮 Future Improvements

- Edge-device deployment for latency reduction
- HIPAA-compliant cloud architecture
- Multi-camera synchronization
- Fall severity scoring
- Machine learning refinement using real incident datasets
- Integration with hospital paging systems
- Privacy-preserving anonymized pose detection

---

## 👩‍💻 Contributors

Built by:
- Varnika Yadav
- Devaj Solanki
- Anant Patel
- Anusha Agarwal

---

## 📌 Why This Matters

WatchCare-AI demonstrates applied computer vision, real-time systems engineering, and safety-critical system design under time constraints.

It showcases practical AI deployment beyond theoretical modeling.
