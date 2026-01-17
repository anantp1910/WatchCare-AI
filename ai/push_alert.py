from firebase_admin import credentials, firestore, initialize_app

cred = credentials.Certificate("serviceAccountKey.json")
initialize_app(cred)

db = firestore.client()

doc = {
    "location": "Waiting Room Cam 3",
    "severity": 0.97,
    "status": "active",
    "created_at": firestore.SERVER_TIMESTAMP,
    "claimed_by": None,
    "claimed_at": None,
    "resolved_by": None,
    "resolved_at": None
}

db.collection("alerts").add(doc)
print("Alert pushed to Firestore")
