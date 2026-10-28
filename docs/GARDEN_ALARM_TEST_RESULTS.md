# Garden alarms — 0.6.0

Verified 2026-10-13 on the connected Xiaomi 14T Pro (Android 16 / API 36, HyperOS 3), with user-granted permissions and the temporary instrumentation helper approved by the user.

## Build and automated checks

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`: successful, including a final rebuild after visual polish.
- 15 JVM tests passed. Four alarm timing tests cover five-minute snooze, next-day scheduling, a daylight-saving transition and stale/early delivery; eleven existing visit tests also passed.
- Lint: zero errors, 38 warnings, one hint. Warnings include localization, dependency versions, intentional synchronous preference commits, legacy resources and custom-view tooling.
- Both physical alarm tests passed without skips in 33.695 seconds. Output: `../../outputs/Garden-Presence-0.6-Alarm-Device-Tests.txt`.
- The five-character preview/head-pat test passed again after final clock contrast, fade and Virelia framing changes: one test, 18.333 seconds. Output: `../../outputs/Garden-Presence-0.6-Polish-Device-Test.txt`.

## What was verified on the phone

- All five alarm previews load their dedicated art, display correct names and greetings, keep native controls within screen bounds, and show their own head-pat dialogue. Previewing/patting does not schedule an alarm.
- Screenshots of each open-eye scene and head-pat reaction were saved under `../../outputs/Garden-Alarm-Art/device/`. Final Vivianne and Virelia screenshots were visually inspected for clock/name contrast and composition; all four new scenes were inspected before polish.
- A real short `AlarmManager` event is delivered. A queued Nixia alarm remains Nixia after browsing Zaria.
- Pressing the native snooze button registers a system alarm five minutes from the press, preserves Nixia, and clears the current ringing state. A stale dismissal token cannot dismiss that new snooze.
- A second short scheduled event is delivered and the native awake button clears its ringing state.
- Tests cancel their pending alarms and restore the prior selected girl. The temporary helper was removed afterward; the main 0.6 app remains installed.

## Limits

The phone was unlocked. The tests explicitly launch the alarm Activity after delivery; this does not prove that HyperOS will automatically present it over the locked screen. Earlier decorative-visit locked-screen success is not evidence for this separate alarm path.

The five-minute snooze was verified through the registered system alarm timestamp and persisted state, rather than waiting five minutes for that event. Actual delivery was exercised using short test events. Reboot restoration, prolonged idle/Doze and every HyperOS battery configuration were not exercised in this run.

There is one upcoming alarm, no repeating daily schedule, and no character voice. The artwork uses registered open/blink frames with bounded native mesh motion and head-pat accents; it is not a fully articulated Live2D model.
