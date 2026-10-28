# Garden editorial — 0.7

Date: 2026-10-13. Native UI redesign for home, morning setup, alarm scenes, visit settings and dialogue.

## Build and installation

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` succeeded. Nineteen JVM tests passed; scheduling and motion implementations are unchanged.
- Lint reports zero errors, 39 warnings and one hint. Remaining warnings include localization, existing component/tooling issues and dependency versions.
- Installed on Hugo's Xiaomi 14T Pro as version code 11, `0.7.0-garden-editorial`, preserving app data and permissions. The home screen was launched and visually inspected on the phone.
- Xiaomi rejected the separate test-helper installation twice with `INSTALL_FAILED_USER_RESTRICTED`. No permission or installation restriction was bypassed. Automated UI verification was moved to the local Android API 36.1 emulator.

## Test coverage

Final result: **five distinct emulator UI tests passed** across the main run and focused recheck. The four unaffected tests passed in the main run. The corrected dialogue test and repeated alarm-preview test passed together in 50.972 seconds (`OK (2 tests)`). Clear screenshots of the paper home, mornings, visits, alarm composition and visit dialogue were visually inspected. The emulator test helper was removed and the task-started emulator was shut down afterward.

`EditorialUiDeviceTest` checks light and dark home screens, expression disclosure, all five portrait selections, selected preview labels and preservation of the pending alarm timestamp while browsing. It restores test-modified preferences. It saves screenshots of home, controls, mornings, permissions and visit settings.

Existing tests also exercise all five local sprites and rapid switching, foreground/background renderer lifecycle, all five alarm previews and head-pat dialogue, and all five visit dialogue scenes. Preview tests check native control/dialogue bounds and ensure no alarm/visit counter side effects.

The first emulator run passed three tests but was interrupted because the emulator's System UI became unresponsive and covered screenshots with a system dialog. Its log is retained as `../../outputs/Garden-Presence-0.7-UI-Tests-Interrupted-Emulator.txt`. The emulator was restarted with more memory and host graphics. Four tests passed in the next run; the dialogue test failed its old fixed 2.8-second loading delay. That test now waits up to twelve seconds for the authored dialogue and finished text reveal, within the scene's existing fifteen-second startup deadline. Assertions for authored text, readability, visibility and unchanged counters remain in place. See `../../outputs/Garden-Presence-0.7-UI-Tests.txt` and `../../outputs/Garden-Presence-0.7-UI-Followup-Tests.txt` for the run and focused recheck.

Screenshots are stored in `../../outputs/Garden-Editorial-UI/`. APK/source packaging verifies all ten alarm artwork payloads against the source assets. The redesign adds no generated artwork, fonts, external runtime libraries, voice features or network calls.

The longer dialogue readiness wait exposed the actual remaining emulator blocker: Android's first-use “Viewing full screen” tutorial retained window focus, so the visit correctly did not start its sequence. Test setup now locates that informational tutorial and clicks its “Got it” action through the test automation accessibility API. It does not grant permissions or dismiss arbitrary dialogs. The blocked follow-up is retained in `../../outputs/Garden-Presence-0.7-UI-Tutorial-Blocked.txt`.

## Limits

UI checks on an emulator do not establish Xiaomi locked-screen behavior. Alarm sound and real five-minute snooze behavior were not re-tested in this visual release; those implementations retain the prior version's checks and limitations. No additional Android permissions were granted by automation.
