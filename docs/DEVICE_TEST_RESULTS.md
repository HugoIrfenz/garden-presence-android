# Xiaomi device verification — 12 October 2026

Device detected through authorized USB debugging: Xiaomi 14T Pro (2407FPN8EG), Android 16 / API 36, HyperOS OS3.0. Physical display 1220 × 2712 at 520 dpi.

## Build and installation

- Debug APK and instrumentation APK compiled successfully.
- Android lint: passed with **0 errors, 5 warnings**. Four warnings report newer dependency/tool versions; one requests an additional legacy backup declaration for pre-Android-12 devices. The tested phone uses the supplied Android-12+ data extraction rules, with cloud/device-transfer exclusions.
- Initial installs were blocked by HyperOS. Hugo enabled Install via USB and approved installation. No device security permissions were bypassed or modified through ADB.
- Foreground application launch succeeded. A physical-device screenshot shows Vivianne, native character selection, expression controls and visit controls.
- The first screenshot revealed a soft canvas at the desktop 1.5× cap. The Android renderer was adjusted to up to 3×, bounded by the source art size, and rebuilt/reinstalled.

## Instrumented checks

The initial three physical-device tests passed. The same suite passed again on the final higher-resolution APK: **OK (3 tests), 18.359 seconds**. Authoritative final output is in `outputs/Garden-Presence-Android-Device-Tests.txt` in the workspace:

1. Load every character locally, verify all expected textures, no static fallback, and latest-character-wins during rapid switching.
2. Complete a twelve-second manual visit, remain hidden after goodbye, stop animation, then restore a selected character.
3. Move activity into the background, verify paused state and unchanged animation clock, return to foreground and verify the clock advances.

## Limits

This verifies the sprite integration and foreground preview lifecycle only. It does not test real alarms, notifications, background activity launch, overlays, camera, speech, exact scheduling, Doze, long-running battery use or a production signing/update flow. Those features are not included in this APK. The automated tests do not substitute for a full accessibility audit or all-expression visual comparison on the phone.
