import cv2
import numpy as np
from datetime import datetime
import time
import argparse

from firebase_admin import credentials, firestore, initialize_app

# ================== ARGPARSE ==================
parser = argparse.ArgumentParser()
parser.add_argument("--video", type=str, default="demo.mp4", help="Path to demo video")
parser.add_argument("--webcam", action="store_true", help="Use live webcam instead of video")
args = parser.parse_args()

VIDEO_PATH = args.video
USE_WEBCAM = args.webcam

# -------- MediaPipe import --------
try:
    import mediapipe as mp
except Exception as e:
    print(f"MediaPipe import failed: {e}")
    raise

# ================== CONFIG ==================
KEY_PATH = "serviceAccountKey.json"

# Fall detection tuning
FALL_ANGLE_THRESHOLD = 45      # degrees from vertical (0=standing, 90=horizontal)
STAY_DOWN_TIME = 4.0           # seconds must stay down before alert
MOVEMENT_THRESHOLD = 0.05      # avg normalized movement threshold
MIN_VISIBILITY = 0.5           # ignore landmarks if visibility below this

LOCATION_NAME = "Waiting Room Cam 3"
SEVERITY_SCORE = 0.93

print("Source:", "WEBCAM" if USE_WEBCAM else f"VIDEO ({VIDEO_PATH})")

# ================== FIREBASE SETUP ==================
cred = credentials.Certificate(KEY_PATH)
initialize_app(cred)
db = firestore.client()

def push_alert(trigger_time_s: float):
    doc = {
        "location": LOCATION_NAME,
        "severity": SEVERITY_SCORE,
        "status": "active",
        "created_at": firestore.SERVER_TIMESTAMP,
        "claimed_by": None,
        "claimed_at": None,
        "resolved_by": None,
        "resolved_at": None,
        "trigger_time_s": round(trigger_time_s, 2),
        "triggered_local_time": datetime.now().isoformat(timespec="seconds")
    }
    db.collection("alerts").add(doc)
    print(f"ALERT pushed to Firestore at t={trigger_time_s:.2f}s")

# ================== MEDIAPIPE SETUP ==================
mp_pose = mp.solutions.pose
mp_draw = mp.solutions.drawing_utils

pose = mp_pose.Pose(
    static_image_mode=False,
    model_complexity=1,
    min_detection_confidence=0.5,
    min_tracking_confidence=0.5
)

# ================== HELPER FUNCTIONS ==================
def get_landmark_xy(landmarks, idx):
    lm = landmarks[idx]
    return np.array([lm.x, lm.y], dtype=np.float32), lm.visibility

def body_angle_from_vertical(landmarks):
    """
    Compute angle (degrees) between body axis (shoulder-mid -> hip-mid)
    and vertical direction. 0 = upright, 90 = horizontal.
    """
    ls, v1 = get_landmark_xy(landmarks, mp_pose.PoseLandmark.LEFT_SHOULDER.value)
    rs, v2 = get_landmark_xy(landmarks, mp_pose.PoseLandmark.RIGHT_SHOULDER.value)
    lh, v3 = get_landmark_xy(landmarks, mp_pose.PoseLandmark.LEFT_HIP.value)
    rh, v4 = get_landmark_xy(landmarks, mp_pose.PoseLandmark.RIGHT_HIP.value)

    if min(v1, v2, v3, v4) < MIN_VISIBILITY:
        return None

    shoulder_mid = (ls + rs) / 2.0
    hip_mid = (lh + rh) / 2.0

    vec = hip_mid - shoulder_mid  # body axis downward
    vertical = np.array([0.0, 1.0], dtype=np.float32)

    vec_norm = np.linalg.norm(vec) + 1e-6
    cosang = np.clip(np.dot(vec, vertical) / vec_norm, -1.0, 1.0)
    angle = np.degrees(np.arccos(cosang))
    return float(angle)

def avg_movement(prev_pts, curr_pts):
    if prev_pts is None or curr_pts is None:
        return None
    diffs = np.linalg.norm(curr_pts - prev_pts, axis=1)
    return float(np.mean(diffs))

def extract_keypoints(landmarks):
    ids = [
        mp_pose.PoseLandmark.NOSE.value,
        mp_pose.PoseLandmark.LEFT_SHOULDER.value,
        mp_pose.PoseLandmark.RIGHT_SHOULDER.value,
        mp_pose.PoseLandmark.LEFT_HIP.value,
        mp_pose.PoseLandmark.RIGHT_HIP.value,
    ]
    pts = []
    for i in ids:
        p, vis = get_landmark_xy(landmarks, i)
        if vis < MIN_VISIBILITY:
            return None
        pts.append(p)
    return np.stack(pts, axis=0)

# ================== MAIN LOOP ==================
cap = cv2.VideoCapture(0 if USE_WEBCAM else VIDEO_PATH)
if not cap.isOpened():
    print(f"ERROR: Could not open {'webcam' if USE_WEBCAM else 'video'}: {VIDEO_PATH if not USE_WEBCAM else 'index 0'}")
    raise SystemExit(1)

triggered = False
down_start_time = None
prev_pts = None
start_wall = time.time()

while True:
    ret, frame = cap.read()
    if not ret:
        break

    # time in seconds:
    # - video: use video timestamp
    # - webcam: use wall clock time since start
    if USE_WEBCAM:
        t = time.time() - start_wall
    else:
        t = cap.get(cv2.CAP_PROP_POS_MSEC) / 1000.0

    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    res = pose.process(rgb)

    status_text = "No person"
    angle_text = ""
    move_text = ""

    is_down = False
    is_still = False

    if res.pose_landmarks:
        landmarks = res.pose_landmarks.landmark

        mp_draw.draw_landmarks(frame, res.pose_landmarks, mp_pose.POSE_CONNECTIONS)

        angle = body_angle_from_vertical(landmarks)
        pts = extract_keypoints(landmarks)

        mv = avg_movement(prev_pts, pts) if pts is not None else None
        prev_pts = pts

        if angle is not None:
            angle_text = f"angle: {angle:.1f}"
            is_down = angle >= FALL_ANGLE_THRESHOLD

        if mv is not None:
            move_text = f"move: {mv:.3f}"
            is_still = mv <= MOVEMENT_THRESHOLD

        status_text = "DOWN" if is_down else "UP"

        # Fall logic: must be DOWN + STILL continuously for STAY_DOWN_TIME
        if is_down and is_still and not triggered:
            if down_start_time is None:
                down_start_time = t
            elif (t - down_start_time) >= STAY_DOWN_TIME:
                push_alert(t)
                triggered = True
        else:
            down_start_time = None

    # Overlay info
    cv2.putText(frame, f"time: {t:.2f}s", (20, 40),
                cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)

    cv2.putText(frame, f"status: {status_text}", (20, 80),
                cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)

    if angle_text:
        cv2.putText(frame, angle_text, (20, 120),
                    cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)

    if move_text:
        cv2.putText(frame, move_text, (20, 160),
                    cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)

    if triggered:
        cv2.putText(frame, "ALERT SENT", (20, 200),
                    cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)

    cv2.imshow("WatchCare AI - Fall Detection Demo", frame)

    if cv2.waitKey(30) & 0xFF == ord("q"):
        break

cap.release()
cv2.destroyAllWindows()
