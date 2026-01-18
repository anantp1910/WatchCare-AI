# camera_demo.py
# Plays a video (or webcam), computes UP/DOWN + time + torso angle,
# shows DOWN a bit earlier (DOWN_STATUS_SEC),
# sends ALERT a bit later (DOWN_ALERT_SEC),
# pushes a Firestore alert after confirmed down duration,
# and shows a TOP banner "ALERT SENT".

import os
import time
import math
import cv2

# --- Firebase Admin ---
from firebase_admin import credentials, firestore, initialize_app

# --- MediaPipe ---
import mediapipe as mp


# =========================
# CONFIG
# =========================

KEY_PATH = "serviceAccountKey.json"

SOURCE_MODE = "VIDEO"  # "VIDEO" or "WEBCAM"

# If VIDEO (playlist):
VIDEO_FILES = ["WhatsApp Video 2026-01-18 at 5.57.36 AM.mp4"
]

LOOP_VIDEO = False       # False = stop when all videos finish; True = loop the last/only video

# If WEBCAM:
WEBCAM_INDEX = 0

ALERTS_COLLECTION = "alerts"
ALERT_COOLDOWN_SEC = 10

# Angle threshold (0 degrees = perfectly upright; higher = leaning/lying)
DOWN_ANGLE_THRESHOLD = 45.0

# Show DOWN earlier, send alert later
DOWN_STATUS_SEC = 0.5   # show "DOWN" after being down for 0.5s
DOWN_ALERT_SEC  = 2.0   # send alert after being down for 2.0s

# Top banner duration (seconds) after alert push
ALERT_BANNER_SECONDS = 5.0


# =========================
# INIT FIREBASE
# =========================

if not os.path.exists(KEY_PATH):
    raise FileNotFoundError(f"Missing {KEY_PATH}. Put it in the same folder as camera_demo.py")

cred = credentials.Certificate(KEY_PATH)
initialize_app(cred)
db = firestore.client()


def push_alert(location="Ward A", severity=0.95):
    db.collection(ALERTS_COLLECTION).add({
        "location": location,
        "severity": float(severity),
        "status": "active",
        "created_at": firestore.SERVER_TIMESTAMP
    })


# =========================
# INIT MEDIAPIPE
# =========================

mp_pose = mp.solutions.pose
mp_drawing = mp.solutions.drawing_utils

pose = mp_pose.Pose(
    static_image_mode=False,
    model_complexity=1,
    enable_segmentation=False,
    min_detection_confidence=0.5,
    min_tracking_confidence=0.5
)


# =========================
# VIDEO SOURCE HANDLING
# =========================

def open_capture():
    if SOURCE_MODE.upper() == "WEBCAM":
        print(f"Source: WEBCAM (index={WEBCAM_INDEX})")
        cap = cv2.VideoCapture(WEBCAM_INDEX)
        return cap, 0

    video_path = VIDEO_FILES[0]
    print(f"Source: VIDEO ({video_path})")
    cap = cv2.VideoCapture(video_path)
    return cap, 0


def switch_to_next_video(current_index):
    next_index = (current_index + 1) % len(VIDEO_FILES)
    video_path = VIDEO_FILES[next_index]
    print(f"Switching to next video: {video_path}")
    cap = cv2.VideoCapture(video_path)
    return cap, next_index


# =========================
# MAIN
# =========================

def main():
    cap, video_index = open_capture()

    if not cap.isOpened():
        if SOURCE_MODE.upper() == "VIDEO":
            raise RuntimeError(f"ERROR: Could not open video: {VIDEO_FILES[0]}")
        raise RuntimeError("ERROR: Could not open webcam. Try index 0/1/2 and close Zoom/Teams.")

    last_alert_time = 0.0

    # Used to track how long we've been continuously "down"
    down_start_time = None

    # Prevent sending multiple alerts for one down event
    alert_sent_for_current_down = False

    # top banner timer (show "ALERT SENT" until this timestamp)
    alert_banner_until = 0.0

    while True:
        ret, frame = cap.read()

        # End-of-video handling (VIDEO mode)
        if not ret:
            if SOURCE_MODE.upper() == "VIDEO":
                if len(VIDEO_FILES) > 1:
                    cap.release()
                    cap, video_index = switch_to_next_video(video_index)
                    if not cap.isOpened():
                        raise RuntimeError(f"ERROR: Could not open video: {VIDEO_FILES[video_index]}")
                    continue

                # single video
                if LOOP_VIDEO:
                    cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
                    continue
                else:
                    print("Video ended.")
                    break
            else:
                print("Webcam read failed.")
                break

        h, w = frame.shape[:2]

        # --- Time (mm:ss) ---
        t_ms = cap.get(cv2.CAP_PROP_POS_MSEC)
        t_sec = int(t_ms // 1000)
        mm = t_sec // 60
        ss = t_sec % 60
        time_str = f"{mm:02d}:{ss:02d}"

        # --- Pose processing ---
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = pose.process(rgb)

        torso_angle = None
        status = "UP"

        if results.pose_landmarks:
            mp_drawing.draw_landmarks(frame, results.pose_landmarks, mp_pose.POSE_CONNECTIONS)

            lm = results.pose_landmarks.landmark

            left_sh = lm[mp_pose.PoseLandmark.LEFT_SHOULDER]
            right_sh = lm[mp_pose.PoseLandmark.RIGHT_SHOULDER]
            left_hip = lm[mp_pose.PoseLandmark.LEFT_HIP]
            right_hip = lm[mp_pose.PoseLandmark.RIGHT_HIP]

            # Midpoints in normalized coords (0..1)
            shx = (left_sh.x + right_sh.x) / 2.0
            shy = (left_sh.y + right_sh.y) / 2.0
            hipx = (left_hip.x + right_hip.x) / 2.0
            hipy = (left_hip.y + right_hip.y) / 2.0

            # Angle vs vertical:
            # 0 deg = upright, larger = leaning/lying
            dx = hipx - shx
            dy = hipy - shy
            if dy != 0:
                torso_angle = abs(math.degrees(math.atan2(dx, dy)))

            # --- DOWN tracking ---
            is_down_now = (torso_angle is not None) and (torso_angle > DOWN_ANGLE_THRESHOLD)

            if is_down_now:
                if down_start_time is None:
                    down_start_time = time.time()
                    alert_sent_for_current_down = False

                down_duration = time.time() - down_start_time

                # Show DOWN earlier
                status = "DOWN" if down_duration >= DOWN_STATUS_SEC else "UP"
            else:
                down_start_time = None
                alert_sent_for_current_down = False
                status = "UP"

        else:
            # If no pose found, treat as UP and reset
            down_start_time = None
            alert_sent_for_current_down = False
            status = "UP"

        # --- Overlay: Status, Time, Angle ---
        angle_str = "NA" if torso_angle is None else f"{torso_angle:.1f}°"

        # green for UP, red for DOWN
        status_color = (0, 255, 0) if status == "UP" else (0, 0, 255)

        cv2.putText(frame, f"Status: {status}", (15, 35),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.9, status_color, 2)
        cv2.putText(frame, f"Time: {time_str}", (15, 70),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.9, (255, 255, 255), 2)
        cv2.putText(frame, f"Angle: {angle_str}", (15, 105),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.9, (255, 255, 255), 2)

        # --- Send alert only after DOWN has lasted long enough ---
        if down_start_time is not None:
            down_duration = time.time() - down_start_time

            if (down_duration >= DOWN_ALERT_SEC) and (not alert_sent_for_current_down):
                now = time.time()
                if now - last_alert_time >= ALERT_COOLDOWN_SEC:
                    print(f"🚨 ALERT after {DOWN_ALERT_SEC:.1f}s DOWN at {time_str} | angle={angle_str}")
                    push_alert(location="Ward A", severity=0.95)
                    last_alert_time = now

                    # show top banner
                    alert_banner_until = time.time() + ALERT_BANNER_SECONDS
                    alert_sent_for_current_down = True

        # --- TOP BANNER: ALERT SENT ---
        if time.time() < alert_banner_until:
            overlay = frame.copy()
            cv2.rectangle(overlay, (0, 0), (w, 60), (0, 0, 255), -1)
            alpha = 0.6
            cv2.addWeighted(overlay, alpha, frame, 1 - alpha, 0, frame)

            text = "ALERT SENT"
            scale = 1.2
            thickness = 3
            (tw, th), _ = cv2.getTextSize(text, cv2.FONT_HERSHEY_SIMPLEX, scale, thickness)
            x = max(10, (w - tw) // 2)
            y = 42

            cv2.putText(frame, text, (x, y),
                        cv2.FONT_HERSHEY_SIMPLEX, scale, (255, 255, 255), thickness)

        cv2.imshow("WatchCareAI Demo", frame)

        key = cv2.waitKey(1) & 0xFF
        if key == ord('q'):
            break
        if key == ord('n') and SOURCE_MODE.upper() == "VIDEO" and len(VIDEO_FILES) > 1:
            # Press 'n' to jump to the next video manually
            cap.release()
            cap, video_index = switch_to_next_video(video_index)
            if not cap.isOpened():
                raise RuntimeError(f"ERROR: Could not open video: {VIDEO_FILES[video_index]}")
            down_start_time = None
            alert_sent_for_current_down = False
            continue

    cap.release()
    cv2.destroyAllWindows()
    pose.close()


if __name__ == "__main__":
    main()
