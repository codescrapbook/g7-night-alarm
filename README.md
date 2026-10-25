G7 Night Alarm (Android, Kotlin/Compose)
=======================================

Purpose: A backup, tablet‑friendly glucose alarm for Dexcom G7 users whose official alarms may be too quiet or unreliable overnight. This app listens to xDrip+ broadcasts (not the Dexcom sensor directly), stores and charts glucose, and, on alarm, can play a piercing full‑volume alarm that continues until deliberately dismissed.

Safety note
-----------
- This app is NOT a medical device. It is intended only as a backup/secondary alarm. It does not replace your prescribed devices or medical advice.

What it does
------------
- Foreground monitoring service with a persistent notification that survives screen off and Doze. Auto‑start on boot/update.
- Receives glucose from xDrip+:
  - Primary: Local broadcast `com.eveningoutpost.dexdrip.BgEstimate`
  - Fallback: Local web server `http://127.0.0.1:17580/sgv.json` (if enabled in xDrip+)
- Stores readings locally (Room). Tablet‑friendly UI with: large current value, trend, minutes since last reading, and a 3h/6h/12h/24h chart with high/low bands.
- Configurable thresholds (defaults): urgent low 55 mg/dL, low 70, high 250, rapid fall rate, and no‑data alarm (default 20 minutes). Units mg/dL (mmol/L optional UI planned).
- Sleep Mode / Awake Mode:
  - Big buttons: “Good Night” (Sleep on) and “Good Morning” (Sleep off). Mode is shown prominently and persists across restarts.
  - Voice commands (optional; offline): “good night” / “good morning” using Vosk keyword spotting; spoken confirmation via Text‑to‑Speech.
  - Auto‑enable nightly: by default 23:00 local time (configurable). Exact alarm schedules daily and announces: “Turning on for sleep.” There is no automatic switch back; use button or voice “good morning”.
  - Alarm behavior by mode:
    - Sleep Mode: full screeching max‑volume alarm for all configured conditions.
    - Awake Mode (revised):
      - Adjustable Awake volume (alarm stream), with Preview in Settings.
      - Pleasant distinct sounds: low, high, and signal‑loss (30 min).
      - Low‑warning band (default 70–80 mg/dL): loop at Awake volume until cleared; after clearing, do not re‑sound for 30 min (configurable).
      - Low (<70): ramp from Awake volume to full max volume over 5 minutes (smooth), then switch to urgent screech and stay at max until cleared. Urgent low (<55) starts ramp higher. After clearing a sub‑70 low, re‑alarm sooner (default 15 min).
      - High: play at Awake volume, no escalation; re‑alarm after clearing (default 60 min).
      - Signal lost (≥30 min): its own sound at Awake volume, no escalation; re‑alarm after clearing (default 30 min).
- Full‑screen alarm activity over the lock screen with press‑and‑hold dismiss; looping harsh tone (2.6–3.4 kHz square‑like), continuous vibration, flashing background; volume forced to max for the alarm and restored on dismiss.
- Permissions onboarding in Settings with shortcuts for Notifications, Battery optimization exemption, and Do Not Disturb access; “Test alarm” button.
- Unit tests: alarm‑condition logic, xDrip+ intent constants, and schedule next‑run computation.
- Debug build includes a fake data generator (no sensor required).

Required xDrip+ settings
------------------------
On the same tablet, install and run xDrip+ (G7 Companion mode) alongside the official Dexcom app.

In xDrip+:
- Menu → Settings → Inter‑app settings:
  - Enable “Broadcast locally”
  - Enable “Compatible Broadcast”
  - (Recommended) Enable “Send the displayed glucose value”
- Optional fallback: Menu → Settings → Cloud Upload → Web Server → “Enable web server” (port defaults to 17580). The app polls `http://127.0.0.1:17580/sgv.json` every 30s when broadcasts are missing.

Permissions to grant (on first run or via Settings)
---------------------------------------------------
- Notifications
- Battery optimization exemption (Allow app to run in background)
- Do Not Disturb access (to let alarms break through if configured)
- Foreground service (granted automatically)
- Microphone (only if you enable Voice control)

Offline voice control (optional; auto‑download)
-----------------------------------------------
This app uses Vosk for offline keyword spotting. When you enable “Voice control” in Settings, the app will:
- Automatically download the small English model `vosk-model-small-en-us-0.15.zip` from the official Vosk site into app‑private storage.
- Verify the SHA‑256 checksum and unzip the model.
- Show progress in the notification.
- Start listening hands‑free for “good night” and “good morning” right after the model is installed.
Voice listening automatically pauses during a full alarm to avoid confusion.

Build and test locally
----------------------
Requirements:
- JDK 17+ and Android SDK (platform 35, build‑tools 35.0.0)

Commands:
```
./gradlew test
./gradlew assembleDebug
```
Artifacts:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (also saved at `/opt/cursor/artifacts/g7-night-alarm-debug.apk`)

CI
--
This repo includes a GitHub Actions workflow to build on CI:
- Runs `./gradlew assembleDebug test` and uploads `app-debug.apk` as an artifact.

Sideloading on your Android tablet
----------------------------------
1. Enable “Install unknown apps” for your file browser or USB installer app.
2. Transfer `app/build/outputs/apk/debug/app-debug.apk` to the tablet (USB, ADB, or cloud).
3. Install the APK. Launch “G7 Night Alarm”.
4. In Settings:
   - Grant Notifications
   - Grant Battery optimization exemption
   - (Recommended) Grant Do Not Disturb access
   - Verify data is arriving (current value shows after first broadcast)
   - Optionally enable Voice control after placing the Vosk model as described above
   - Set thresholds as desired

Known limitations and notes
---------------------------
- Not a medical device. Use as a backup alarm only.
- Voice control requires a downloaded offline model (see above). Without it, voice stays off.
- Exact alarm (auto sleep at 23:00) uses AlarmManager setExactAndAllowWhileIdle. Some OEMs may still delay or restrict alarms under aggressive battery management; ensure optimization is disabled for this app.
- The app listens to xDrip+ broadcasts and its local web server. It does not and will not pair directly with Dexcom sensors.
- Awake‑mode “normal alarm” uses a spoken cue (TTS) and high‑priority notification rather than a system ringtone; you can still raise media volume to make it more audible if desired.

License
-------
Apache 2.0 for this app code. Vosk model licenses are separate; follow the model author’s license terms.

# New repo

This repo was created by a Cursor cloud agent.
