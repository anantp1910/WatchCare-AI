import cv2
from firebase_admin import credentials, firestore, initialize_app

# Firebase setup
cred = credentials.Certificate("serviceAccountKey.json")
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

# This is the “camera” (your video)
cap = cv2.VideoCapture("demo.mp4")

while True:
    ret, frame = cap.read()
    if not ret:
        break

    cv2.imshow("WatchCare AI – Demo Feed", frame)
    key = cv2.waitKey(30) & 0xFF

    # Press A to simulate a fall/slump
    if key == ord("a"):
        push_alert()

    # Press Q to quit
    if key == ord("q"):
        break

cap.release()
cv2.destroyAllWindows()
