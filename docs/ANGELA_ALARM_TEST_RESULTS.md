# Angela alarm pilot — 0.5.0

Historical results for 0.5. The device installation blocker was resolved and alarm/snooze checks passed in 0.6; see [Garden alarm results](GARDEN_ALARM_TEST_RESULTS.md). The pending status below describes the original 0.5 run.

## Completed

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` succeeds.
- 15 JVM tests pass: existing 11 visit tests and 4 new alarm timing tests.
- Alarm tests cover five minutes from the snooze press, next-day scheduling, a daylight-saving transition and rejection of early/stale events.
- Lint: zero errors, 36 warnings and one hint. Remaining warnings concern translation strings, dependency versions, deliberate synchronous preference writes, unused legacy resources and custom-view tooling.
- New open-eye and closed-eye artwork generated with the built-in image tool, inspected, copied to the workspace and converted to registered runtime WebP images (approximately 570 KB together).
- Main app update installed on the connected Xiaomi 14T Pro.

## Pending physical verification

Xiaomi cancelled installation of the separate instrumentation helper with `INSTALL_FAILED_USER_RESTRICTED`. No permission bypass was attempted. The phone screenshot was black, so scene layout and actual alarm/snooze delivery are **not yet verified on the device** for this version. Earlier versions' visit tests do not establish that this new alarm flow works.

`AngelaAlarmDeviceTest` is ready to check the preview, readable controls and head-pat reaction, then actual system alarm delivery, native snooze registration, stale-token protection and dismissal. Scheduling tests require user-granted exact-alarm/notification permissions and skip if a user alarm already exists, preserving it. The test schedules short audible alarms and cancels its pending snooze in cleanup.

The user has been asked to enable the three permission controls under **Mornings with Angela**, leave the phone unlocked and approve the temporary helper. After automated tests, a manual **Test alarm in 20 seconds** with the screen locked is still needed to verify HyperOS full-screen presentation. The helper will be removed after testing.

## Platform references

- https://developer.android.com/develop/background-work/services/alarms
- https://developer.android.com/develop/ui/views/notifications/time-sensitive
- https://developer.android.com/develop/background-work/services/fgs/service-types

This is a single-upcoming-alarm pilot, not a repeating alarm clock or an independently validated critical wake-up system. It uses the system alarm tone, with no recorded character voice.
