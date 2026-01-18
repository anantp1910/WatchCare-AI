import os
from firebase_admin import credentials, firestore, initialize_app

KEY_PATH = "serviceAccountKey.json"
ALERTS_COLLECTION = "alerts"

if not os.path.exists(KEY_PATH):
    raise FileNotFoundError(f"Missing {KEY_PATH} in current folder.")

cred = credentials.Certificate(KEY_PATH)
initialize_app(cred)
db = firestore.client()

def push_alert(location="Ward A", severity=0.85):
    db.collection(ALERTS_COLLECTION).add({
        "location": location,
        "severity": float(severity),
        "status": "active",
        "created_at": firestore.SERVER_TIMESTAMP
    })
    print("✅ Alert pushed.")

if __name__ == "__main__":
    push_alert()
