# Dialogue scenes — 0.4.0

Verified 2026-10-12 on the connected Xiaomi 14T Pro, Android 16 / HyperOS OS3.0, 1220 × 2712 display.

- Production debug APK assembled and installed in place; existing preferences preserved.
- Eleven JVM policy/content tests passed.
- Five physical-device instrumentation tests passed in 55.959 seconds.
- New scene test opened all five full-screen foreground previews, checked each native quote against that girl's authored repertoire, verified its full bounds were on screen and its type size was at least 21sp, and confirmed previews did not change background visit counts or confirmation timestamps.
- Existing regression tests passed for varied dialogue/gestures, all five asset loads and rapid switching, twelve-second manual completion, and background pause/resume.
- Screenshots of all five scenes inspected: large artwork, complete readable dialogue, nameplate and controls visible without clipping on this phone.
- Temporary test helper removed after testing.
- Lint: zero errors, 21 warnings and one hint. Warnings concern dependency versions, intentional synchronous preference persistence, translation resources, unused strings and the programmatically constructed custom view. No lint errors suppressed.

This run verifies foreground scene presentation. It does not constitute a new physical lock-screen wake test. The background wake path was previously verified on this device (see SCREEN_OFF_TEST_RESULTS.md), and a subsequent Zaria visit was recorded as visible and completed before this change. Preview activity launches explicitly avoid claiming screen-off capability.

The final About-label correction uses BuildConfig.VERSION_NAME; scene behavior is identical to the APK exercised by instrumentation. The final package was rebuilt and reinstalled afterward.

Dialogue is offline authored text. No voice playback, microphone, network or generated responses were added.
