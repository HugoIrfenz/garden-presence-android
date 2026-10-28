# Screen-off visits 0.2 — 12 October 2026

Target: Xiaomi 14T Pro, Android 16 / API 36, HyperOS OS3.0.

## Completed

- `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`: successful.
- Seven pure policy tests pass: quiet-hour boundaries, explicit opt-in, permission gating, active-screen rejection, strict OFF vs optional AOD, call/Battery Saver rejection, cap/cooldown and one-off test overrides.
- Android lint: 0 errors, 11 warnings, 1 hint. Warnings concern pinned dependency versions, intentional synchronous preference commits around scheduling/one-use tickets, and one English-only time label. No lint errors are suppressed.
- Version 0.2 installed over 0.1 on the connected phone. The three sprite/lifecycle regression tests pass, 18.532 seconds. Test helper removed after testing.
- The regression tests originally assumed the saved girl was Vivianne, but the user had selected Virelia. Tests now explicitly choose their starting girl and restore the saved selection. No animation defect was found.
- MainActivity starts successfully with the user's saved Virelia selection. The new Screen-off visits control is present in the native UI hierarchy.

## Physical screen-off attempt

The user granted display-over-other-apps permission; read-only ADB confirmed `SYSTEM_ALERT_WINDOW: allow`. The user scheduled a one-off test and turned the display off; the phone reported `mWakefulness=Dozing`, with the user's AOD opt-in enabled. The test remained pending: HyperOS's alarm dump showed `power_pending` moving the due event approximately three days into the future. This is an OS scheduling deferral before the receiver runs, not evidence that the visit Activity failed to render.

The user adjusted battery restrictions/autostart and retried. The new test alarm no longer had the power-policy deferral and delivered at 22:29:40. Android logged `BAL_ALLOW_SAW_PERMISSION`, confirming that the real receiver could launch the Activity using the user-granted permission. An implementation bug then closed the Activity during an initial pause, before it could draw. Version 0.2.1 now tolerates an unconfirmed startup pause within its original 15-second hard timeout, removes `noHistory` dismissal during this transition, arms turn-screen-on before show-when-locked, and starts the animation sequence only after visible confirmation. Build, seven policy tests and lint passed; 0.2.1 installed. A new physical test is pending.

## Successful physical visit and final schedule fix

On version 0.2.1 the user started a new test and left the phone asleep. At 22:32:29 Jakarta time, the real alarm receiver launched Angela and the app logged `test_visible`. A screenshot captured the focused VisitActivity with the complete animated illustration and native quote/buttons. At the same time Android reported `mWakefulness=Awake`, with keyguard `showing=true`, `inputRestricted=true`, and `occluded=true`: the visit woke the screen without unlocking it. The app logged `visit_finished` 12.005 seconds after visible confirmation. After exit, no wake lock remained (`mWakeLockSummary=0x0`). The overall test took about 35.7 seconds from scheduling to visible artwork; scheduling is inexact.

The older automatic alarm still retained its previous HyperOS power-policy deferral. Version 0.2.2 re-arms an enabled schedule at its existing future due time when MainActivity or VisitSettingsActivity resumes. This preserves opt-in and cadence while refreshing the OEM policy. Seven policy tests, APK build and lint passed again (0 errors). Version 0.2.2 installed successfully; reopening MainActivity re-armed the automatic alarm for 13 October 06:13, and the alarm dump confirmed `power_pending=--`. Quiet hours remain enabled. No additional visit was triggered during this check.

Evidence: `outputs/Garden-Presence-0.2.1-Locked-Visit.png` and `outputs/Garden-Presence-0.2.2-Verification.json`. The visit renderer/lifecycle in 0.2.2 is unchanged from the successful 0.2.1 physical test. Long-term scheduling, reboot recovery, all five girls in lock-screen mode and battery endurance have not yet been measured.

No permissions or power policies were changed through ADB. No instrumentation-based launch is accepted as evidence of background-launch support because instrumentation can alter launch privileges.

The feature records `launch_requested` separately from `test_visible`/`visit_visible`; only an art-ready, resumed, focused, interactive Activity confirms appearance. HyperOS may require additional app permissions. Approximate alarms may be delayed or dropped by the OS. There is no claim yet of successful locked-screen presentation, long-term timing, reboot recovery or battery endurance.

Keep the original `DEVICE_TEST_RESULTS.md` as the historical 0.1 sprite-only record.

Official setting references: [Xiaomi 14T Pro background autostart](https://www.mi.com/global/support/faq/details/KA-507611/) and [Xiaomi Powerkeeper restrictions](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1628).
