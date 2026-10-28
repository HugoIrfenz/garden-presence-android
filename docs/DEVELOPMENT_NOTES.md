> Historical implementation notes. References to the original workspace outputs and art masters are archival; those files are not included in this repository. Current setup instructions are in the root README.

# Garden Presence — Android 0.7 Garden editorial

## A calmer native interface — 0.7

The home, alarm setup, alarm scenes, visit settings and visit dialogue share a native design system in `GardenUi.kt`: warm paper, charcoal artwork stages, fine rules, smaller corner radii and a clear primary action. System sans-serif handles controls and names; serif is reserved for the Garden wordmark and spoken dialogue. No new fonts, network dependencies or generated artwork are required.

Home leads with the selected girl and **Spend a moment**. **Expressions & movement** expands on demand. The **Mornings** and **Little visits** rows open their settings with real saved-status summaries. Home appearance remains selectable in **Settings**; existing light/dark preferences are preserved, with paper as the default for an unset preference.

Mornings shows the actual next alarm, a five-portrait selector, silent scene preview and a clear **Set an alarm** action. Permission details collapse when the essential permissions are already granted and remain accessible at any time. Selecting a portrait does not change an alarm already scheduled. Alarm scenes use a small corner clock and a compact left-aligned dialogue panel, with distinct head-pat, snooze and dismissal controls. Visit setup remains opt-in; all scheduler, sound and rigging behavior is preserved.

Scene text and actions are still native accessible views. Portrait choices expose radio-button semantics, selected states and keyboard focus. Content scrolls vertically, and portrait strips scroll horizontally where needed. See `docs/EDITORIAL_UI_TEST_RESULTS.md` for this release's verification record.

## More expressive alarm rigging — 0.6.1

All five alarm poses now use a 40 × 60 connected deformation mesh with individually weighted hair, sleeve and accessory regions. The head tilts around the neck while the face moves together; hair follows with differing timing and amplitude. Angela has lively bob/coat/tie movement, Vivianne has slow long-hair drift, Nixia has bouncy buns/ribbons and occasional double blinks, Virelia has swaying braids with a stable breakfast tray, and Zaria has asymmetric bob/jacket/earring motion. Head pats add a 3.2-second lean and damped hair follow-through.

`AlarmMotionRig.kt` contains source-normalized weights and character tuning. This reuses the approved anime artwork and registered eyelid frames, without adding image payload. It is regional mesh rigging of one connected illustration, not newly separated or inpainted hair/arm layers. Larger articulated pose changes would require those additional assets. The existing visit sprites are unchanged.

Motion is capped at roughly 30 requested frames per second, uses reused vertex/weight buffers, and freezes its animation clock while paused. Reduced motion keeps the pose exactly still while preserving blink-frame feedback for explicit head pats. Scheduling and snooze code are unchanged. See `docs/GARDEN_MOTION_TEST_RESULTS.md` for this release's checks.

## Mornings in the Garden — 0.6

Open **Mornings in the Garden · alarm & snooze**, choose a girl, then use her silent preview or set an alarm. All five now have dedicated anime alarm artwork, registered blink frames, individual head-pat responses, themed scenes and different idle movement. Vivianne is quiet beside her sleeve; Nixia peeks over a cushion; Virelia carefully brings breakfast; Zaria looks back with a composed editorial pose. Angela retains her approved anime art.

There is **one upcoming alarm**, not five independent alarms. Browsing another girl does not change the already scheduled character. Snooze preserves the ringing character and schedules five minutes from the button press. Preview head pats never schedule an alarm; the preview's snooze button does schedule a real alarm. Sound remains Android's default alarm tone, with no character voice or generated dialogue.

Edit `AlarmCharacters.kt` for scene copy, palettes and motion/framing values. New runtime images live in `app/src/main/assets/alarm-characters/{vivianne,nixia,virelia,zaria}/`; Angela remains in `alarm-angela/`. Masters and exact built-in image-generation prompts are in `../outputs/Garden-Alarm-Art/`; registered eye patches are prepared by `../work/prepare-garden-alarm-art.py`. These are two image states with restrained native mesh animation, not a fully articulated Live2D rig.

The existing `AngelaAlarm*` component names, preference file and channel ID remain stable for upgrade compatibility. Physical Xiaomi checks passed for all five previews and head-pat reactions, actual short alarm delivery, five-minute snooze registration, character retention, stale actions and dismissal. See `docs/GARDEN_ALARM_TEST_RESULTS.md` for evidence and limits. The sections below retain earlier release history.

**0.5.1 art revision:** Angela's dedicated alarm pose follows the approved anime sprite and original character sheet: cleaner cel shading, familiar amber eyes, simplified anime facial features and matched blue hair shapes. Both blink/head-pat frames were replaced and registered at the new eye coordinates. Masters and built-in image-generation prompts: `../outputs/Angela-Alarm-Art-v2/`. Physical alarm/snooze verification was pending at that release and subsequently completed for 0.6 as documented above.

An offline Android host for the sprite pack, short screen-off visits, and a dedicated Angela alarm pilot. It is **not the complete alarm-clock release** described in the supplied full-build contract. Android and OEM restrictions require a real screen-off test on each phone; a successful build alone does not confirm background launch support.

## Mornings with Angela — 0.5

Open **Mornings with Angela · alarm & snooze** on the home screen. Her new waist-up, cheek-in-palm pose is separate from the standing visit sprites. Native rendering uses registered eyelid frames for blinks and a contented head-pat reaction, gentle mesh breathing, and short heart accents. Text and buttons remain accessible native views. Reduced motion removes deformation and moving accents.

- **Silent preview** opens the scene without ringing. Head pats do not schedule anything. Its snooze button explicitly schedules a real alarm five minutes later, once permissions are ready.
- **Set Angela's alarm** selects one upcoming time (tomorrow if that time has passed). It replaces a pending Angela alarm/snooze; there is no repeating daily schedule yet.
- **5 more minutes** schedules a new `AlarmManager.setAlarmClock` event before stopping the current alarm. It persists across closing the app and reboots when the due time remains in the future. A failed schedule keeps the current alarm active and shows an error.
- **I'm awake** ends the current alarm and stops its sound. The notification provides the same snooze/dismiss controls. Stale notification actions cannot dismiss a newer alarm.
- Alarm sound uses Android's default alarm tone and alarm volume. No character voice or TTS. A foreground media-playback service exists only while ringing, with a two-minute stop and a bounded CPU wake lock. Unanswered alarms stop instead of ringing indefinitely.
- **Cancel Angela's alarm & snooze** removes the queued alarm and ends any ringing session.

Android's **Alarms & reminders**, **Notifications**, and (on newer Android) **Full-screen alarms** controls must be granted by the user. Notifications must retain high importance. While the phone is in use, Android may show the alarm notification instead of opening a full-screen scene. No permission is granted by shell commands or bypassed. This alarm path is independent of the approximate decorative visits: it does not consume their daily caps and does not use their quiet hours.

Implementation: `AngelaAlarmScheduler.kt` (one-shot scheduling, token validation, notification actions and restoration), `AngelaAlarmService.kt` (bounded ringing), `AngelaAlarmActivity.kt` (scene/reactions), `AngelaAlarmArtView.kt` (native 2D rendering), `AngelaAlarmSettingsActivity.kt` (explicit settings).

Art masters and successful generation prompts: `../outputs/Angela-Alarm-Art/`. Runtime art: `app/src/main/assets/alarm-angela/`. Generated with the built-in image tool; eyelid patches registered and encoded by `../work/prepare-alarm-art.py`. These two image states and mesh deformation are not a complete articulated Live2D rig.

Verification: `AlarmTimingTest` covers snooze timing, next-day scheduling, a daylight-saving transition, and stale broadcasts. `AngelaAlarmDeviceTest` checks the scene and head-pat feedback, then (only with user-granted permissions and no existing user alarm) checks actual system delivery, snooze registration, stale-token protection and dismissal. Device results are recorded separately rather than inferred from compilation.

A physical test on Hugo's Xiaomi 14T Pro succeeded: Angela woke the locked screen, appeared for twelve seconds and closed. See `docs/SCREEN_OFF_TEST_RESULTS.md` for device evidence and remaining limits. Version 0.2.2 also refreshes existing alarms after returning from battery settings, because HyperOS can retain the previous restriction on an already scheduled event.

Included: all five characters, 62 transparent layers, blink/expression controls, character-specific idle movement, light/dark backgrounds, reduced motion, pause/resume, persistent character/theme preferences and a native twelve-second manual visit with accessible quote text. The original Garden Home site is unchanged.

## Full-screen dialogue scenes in 0.4

Tap **A little visit · full-screen preview** to open the same large character composition used for screen-off visits. A native RPG-style dialogue panel includes her full name, flower/symbol, readable serif dialogue, character-color accents and a short text reveal. Tap the line to reveal it immediately. Reduced motion and TalkBack touch exploration show the complete line immediately. A greeting remains visible while artwork loads. Dialogue is written text; voice playback and lip-sync are not implemented.

**Close** dismisses the scene. Foreground previews return to the girls without incrementing background visit counts or marking screen-off capability verified. Real screen-off scenes also offer **Pause future visits**. Both retain the twelve-second sequence and fifteen-second hard startup deadline. `VisitDialogueView.kt` owns native dialogue presentation; `VisitActivity.kt` hosts both private foreground previews and validated background visits.

All five scenes were checked on the Xiaomi 14T Pro; see `docs/DIALOGUE_SCENES_TEST_RESULTS.md` for results and verification limits.

## More variety in 0.3

Each girl has **12 authored lines and 4 gesture choices**, for 60 lines and 12 distinct gesture animations overall. Lines and gestures use independent shuffled decks stored per girl on the phone. All her lines are used before cycling again; the cycle boundary avoids an immediate repeat. A blocked screen-off launch does not consume a line. The same repertoire powers **A little visit** on the foreground preview and real screen-off visits. Reopening or updating the app preserves the remaining decks.

- Vivianne: sparse company, a soft tilt, a shy glance, a sleeve movement or a tiny nod.
- Angela: warm practical encouragement, an open-palm gesture and brighter nods.
- Virelia: gentle domestic copy, small bows, sleeve movements and a brief flustered expression.
- Zaria: concise artistic perspectives, a composed turn and restrained hair/sleeve movement.
- Nixia: playful short announcements, a peek, a wink and a compact cheer.

`VisitContent.kt` contains editable fictional UI copy and gesture/expression mappings. These are not historical memories, new canon facts, live observations or AI responses. `VisitRepertoire.kt` persists the decks. `visit-motion.js` defines bounded gesture curves; `renderer.js` applies them to existing registered head, hair, hand and sleeve layers. No extra character art or downloaded assets. Gestures are subtle transformations of the available layers, not new fully articulated poses or lip-sync. Reduced motion suppresses all gesture displacement while retaining readable text and expressions. Visits retain the existing 12-second sequence and 15-second absolute startup deadline.

Validation commands: `./gradlew testDebugUnitTest assembleDebug lintDebug` and `node --test tests/visit-motion.test.cjs`. The device suite also checks that each girl's preview uses her authored line and supported gesture.

No internet permission, account, external website, microphone, camera or permanent overlay. These image layers are not Cubism exports. Decorative visits use the user-granted display-over-other-apps permission for Android's background activity launch exception; their presentation is a short-lived Activity. The separate 0.5 alarm mode adds alarm notifications and a temporary foreground service only while ringing.

## Activate a screen-off visit

1. Open Garden Presence → **Screen-off visits**.
2. Use **Allow display over other apps**, approve Garden Presence in Android settings, then return.
3. Select a girl (or take turns). Press **Test once, then lock your phone**, read the explanation and start it. Turn the screen off with the power button.
4. The test requests a visit in about 20 seconds; Android can delay it. Tests expire after five minutes instead of surprising you much later. Test visits bypass quiet hours and the daily cap, but still obey active-screen, AOD, call and Battery Saver checks.
5. Check **Visit history** for the actual result. On HyperOS, App info → Other permissions may also require lock-screen and background-window access. Names vary by version. Battery restrictions/autostart can affect later delivery.
6. Once satisfied, enable **Automatic visits**. Default: roughly every two hours, maximum three per day, quiet hours 22:30–06:00. A fully off display is required unless AOD is explicitly included. No visits while the phone is interactive. Matching quiet-hour start/end mutes the whole day.

**Pause visits & cancel pending test** removes both alarms and closes an active visit. **Close** dismisses only the visible visit. The app never unlocks the phone. After the Activity closes, Android controls the lock-screen timeout; this app does not force the device back to sleep. Removing the app or force-stopping it stops scheduling; relaunch/toggle visits to reschedule after a force-stop.

## Screen-off architecture

- `VisitPolicy.kt`: pure eligibility policy and quiet-hour boundaries, covered by seven unit tests.
- `VisitScheduler.kt`: one inexact `RTC_WAKEUP` alarm via `setAndAllowWhileIdle`, persisted opt-in, limits, shuffled character rotation, and a local history capped at 40 events. No exact-alarm or full-screen notification privileges. Re-arm on boot, app update and clock/time-zone changes.
- `VisitReceiver.kt`: consumes a one-use token, rejects stale events, re-arms the next approximate check, and starts an explicit private Activity only if eligible. It does not poll or keep a service alive.
- `VisitActivity.kt`: validates a one-use launch ticket, rechecks eligibility, uses `setShowWhenLocked` / `setTurnScreenOn`, and clears keep-screen-on on every exit. Art-ready, focused and interactive presentation is logged separately from an unconfirmed launch request. Native hard timeout: 15 seconds including artwork loading. Normal art-ready visit: 12 seconds. Unlocking, screen-off, interruption or dismissal cancels it.
- `VisitSettingsActivity.kt`: opt-in controls, Android settings links, one-off test, history and honest setup status. No automatic permission granting. Settings alone never turn visits on.

Only bundled presentation events cross the WebView bridge. No background JavaScript scheduling. No private canon files are in the APK. The local history contains timestamps, character IDs and outcomes only.

## Build

Open this directory in Android Studio, or use JDK 17+ with SDK 36 installed:

```
./gradlew testDebugUnitTest assembleDebug lintDebug
./gradlew connectedDebugAndroidTest
```

On Windows use `gradlew.bat`. Set the SDK directory in an untracked `local.properties` or through `ANDROID_HOME`. Debug output: `app/build/outputs/apk/debug/app-debug.apk`.

Pinned tools: Android Gradle Plugin 8.13.2, Gradle 8.13, Kotlin 2.0.21; min SDK 29, compile/target SDK 36. The local renderer is hosted using AndroidX WebKit 1.8.0 `WebViewAssetLoader`. Native Android Views are used for this small verification host; this is not the final app navigation architecture.

Package ID: `home.garden.presence.preview`. This updates the original preview in place. Uninstalling removes this app's preferences, visit history and scheduled visits.

## Architecture

`MainActivity.kt` owns visible controls, native quote text, timed preview cancellation and lifecycle. Trusted bundled `stage.html` hosts only the renderer canvas. Character data is from the local manifest. All file/content access and arbitrary URL loading are denied; the app has no internet permission. The JS interface only accepts presentation events, not scheduling, device commands or permission changes.

Images and scripts under `app/src/main/assets/` are copied from `outputs/Garden-Presence-Sprites-v1`. Update them together to preserve layer registration. The Android renderer's `resize()` uses a higher density cap (up to 3×, bounded by the 1024 × 1536 source dimensions) than the desktop preview to avoid blurry art on Xiaomi's 520-dpi display. Preserve this adjustment when updating. Only the runtime WebP files are bundled, not the PNG masters, private canon or generation prompts.

Animation pauses on activity pause; any in-flight manual preview is cancelled. The renderer rejects stale character loads and releases decoded layers on character switch/disposal. Native text remains outside the canvas for accessibility. Automatic visits are controlled separately by native scheduling and explicit opt-in.

## Device verification

`SpriteDeviceTest.kt` verifies five-character switching, stale-load handling, background pause/resume and completion of a manual visit. `docs/DEVICE_TEST_RESULTS.md` records actual results when available. Successful compilation is not evidence of device behavior.

## Official references

- [Bundled local WebView content and WebViewAssetLoader](https://developer.android.com/develop/ui/views/layout/webapps/load-local-content)
- [AGP 8.13 compatibility with Gradle 8.13 and SDK 36](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [Android background activity launch restrictions and exceptions](https://developer.android.com/guide/components/activities/secure-bal)
- [Inexact alarms and idle restrictions](https://developer.android.com/develop/background-work/services/alarms)
