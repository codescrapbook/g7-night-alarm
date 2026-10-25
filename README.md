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
    - Awake Mode: normal (not max‑volume) notification/voice alert for low/high/rapid‑fall; BUT “urgent low (<55)” and “no data ≥ 30 minutes” always use the full screeching alarm (safety net; can be disabled in settings).
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

Offline voice control (optional)
--------------------------------
This app uses Vosk for offline keyword spotting. To keep the APK small, the English model is not bundled.
- Download “vosk-model-small-en-us” from `https://alphacephei.com/vosk/models` on a computer.
- Extract the folder and copy it to the tablet into: Android/data/com.g7nightalarm.app/files/ so it becomes:
  - `Android/data/com.g7nightalarm.app/files/vosk-model-small-en-us/...`
- In app Settings, enable “Voice control”. The service will load the model and start listening for “good night” / “good morning” hands‑free.
- Voice pauses automatically while a full alarm is sounding to avoid confusion.

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
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

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
