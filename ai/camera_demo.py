import cv2
from firebase_admin import credentials, firestore, initialize_app

# Firebase setup
cred = credentials.Certificate("ai/serviceAccountKey.json")
initialize_app(cred)
db = firestore.client()

def push_alert():
    doc = {
        "location": "Waiting Room Cam 3",
        "severity": 0.93,
        "status": "active",
        "created_at": firestore.SERVER_TIMESTAMP,
        "claimed_by": None,
        "claimed_at": None,
        "resolved_by": None,
        "resolved_at": None
    }
    db.collection("alerts").add(doc)
    print("Alert pushed to Firestore")

# Use your demo video (make sure demo.mp4 is in the WatchCare-AI-master folder)
cap = cv2.VideoCapture("demo.mp4")

triggered = False
TRIGGER_AT = 5.0  # change this to the second you want (you said 5 seconds)

while True:
    ret, frame = cap.read()
    if not ret:
        break

    # current time in seconds
    t = cap.get(cv2.CAP_PROP_POS_MSEC) / 1000.0

    # show timer on screen
    cv2.putText(
        frame,
        f"time: {t:.2f}s",
        (20, 40),
        cv2.FONT_HERSHEY_SIMPLEX,
        1,
        (255, 255, 255),
        2
    )

    # auto trigger alert once at TRIGGER_AT
    if not triggered and t >= TRIGGER_AT:
        push_alert()
        triggered = True

        # optional: show a big FALL DETECTED label
        cv2.putText(
            frame,
            "FALL DETECTED - ALERT SENT",
            (20, 100),
            cv2.FONT_HERSHEY_SIMPLEX,
            1,
            (0, 0, 255),
            3
        )

    cv2.imshow("WatchCare AI - Demo Feed", frame)

    # press q to quit
    if cv2.waitKey(30) & 0xFF == ord("q"):
        break

cap.release()
cv2.destroyAllWindows()
