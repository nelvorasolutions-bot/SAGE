# Water Buddy (Android)

A little cartoon character who walks onto the screen, over whatever app is open, to remind your friend to drink water. Tap the buddy, hit "I drank," and she walks off happy. The home screen tracks daily intake, streaks, and stats.

This is a native Android app. It ships with a placeholder animated character (a cute cartoon person) so it works the moment you build it. You swap in your real Gemini character later by dropping in four files (see "Swap in your own character" below). The placeholder proves the whole thing works: walk in, wave, drink, walk out.

## What you need to do (one build)

I could not produce the finished APK for you inside this tool, because building an Android app needs Google's Android build servers, which this sandbox cannot reach. So there is exactly one build step, and it is on your machine. It takes about 10 minutes the first time.

1. Install Android Studio (free): https://developer.android.com/studio
2. Open Android Studio, choose "Open," and select this `water-buddy-android` folder.
3. Wait for it to sync (bottom status bar). The first sync downloads the build tools automatically; let it finish.
4. Plug in an Android phone with USB debugging on, OR create an emulator (Device Manager, pick any phone, download a system image). Note: the emulator can show the overlay too.
5. Press the green Run arrow (or Shift + F10). The app installs and opens.

To hand it to your friend as a shareable file:

1. Menu: Build, then "Build Bundle(s) / APK(s)," then "Build APK(s)."
2. When it finishes, click "locate" in the popup. You get `app-debug.apk`.
3. Send her that APK (WhatsApp, Drive, email). On her phone she taps it, allows "install from this source," and installs.

## What she does on her phone (first run)

1. Open Water Buddy.
2. In the "Floating buddy" card, tap "Allow drawing over other apps." Turn the toggle on for Water Buddy, then come back.
3. Tap "Allow notifications" and accept.
4. Tap "Preview: make buddy appear now" to see the buddy walk in over the home screen.
5. Flip the "Reminders" switch on. From then on the buddy visits on the interval set in Settings (default every 60 minutes), only during active hours (default 8am to 10pm), and only until the daily goal is met.

One reliability note worth telling her: some phones (Xiaomi, Samsung, Oppo, and others) aggressively kill background apps. If reminders ever stop appearing, go to phone Settings, Apps, Water Buddy, Battery, and set it to "Unrestricted" (wording varies by phone). That keeps the timer alive.

## Swap in your own character

The buddy is just four transparent animated files in `app/src/main/assets`:

- `buddy_walk` (walking, used for walking in and out)
- `buddy_idle` (standing, gentle breathing)
- `buddy_wave` (waving, the "hey, drink" moment)
- `buddy_drink` (raising a glass and drinking)

To replace the placeholder with your Gemini character:

1. Generate the four animations (see `GEMINI_PROMPTS.md`). Export each as an animated GIF or animated WebP with a transparent background.
2. Name them exactly `buddy_walk`, `buddy_idle`, `buddy_wave`, `buddy_drink` with a `.gif` or `.webp` extension.
3. Delete the old placeholder files in `app/src/main/assets` and drop yours in, keeping those names.
4. Rebuild (Run again). Nothing else to change; the app auto-detects `.webp`, `.gif`, or `.png`.

Keep them roughly portrait (taller than wide) and centered, with empty space trimmed, so they sit nicely on screen. The app draws them at about 150 by 220 on the overlay.

## Settings in the app

- Daily goal (default 2000 ml)
- Glass size (default 250 ml; this is how much one tap of "I drank" logs)
- Remind every (default 60 minutes)

Active hours are 8am to 10pm by default. If you want to change those, they live at the top of `Store.kt` (`startHour`, `endHour`).

## How it is put together (for you, quickly)

- `MainActivity.kt`: the home screen, tracking, settings, permission buttons, reminder switch.
- `OverlayService.kt`: the floating window. Walks the buddy in, waves, handles tap to open the "I drank / Snooze" panel, drag to move, then walks out.
- `ReminderReceiver.kt` plus `Scheduler.kt`: the timer. Uses the system alarm so it fires even when the app is closed, then reschedules the next visit.
- `BootReceiver.kt`: re-arms reminders after a reboot.
- `Store.kt`: all saved data (intake, streaks, settings) in simple phone storage. No account, no server.
- `Buddy.kt`: the only file that loads the character art. This is the swap point.

No third party libraries, no logins, no internet. Everything stays on the phone.
