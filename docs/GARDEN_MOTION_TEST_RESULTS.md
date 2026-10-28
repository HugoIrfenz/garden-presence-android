# Garden motion — 0.6.1

Verified 2026-10-13. Installed in place on Hugo's Xiaomi 14T Pro, Android 16 / API 36. This update changes alarm illustration motion only; alarm scheduling, snooze and regular visit sprites retain their previous implementation.

## Checks

- Build, unit tests and lint succeeded. 19 JVM tests passed, including four new rig tests.
- The rig test samples all five poses at 241 animation times, including head-pat states. Both triangles in each mesh cell retain positive area, positions stay finite/bounded, and no cell folds over.
- Reduced-motion output remains the exact undeformed pose across time and reactions. Head-pat displacement returns to idle continuously. Vivianne's hair has independent motion relative to her torso; Virelia's tray remains anchored.
- Physical `AlarmRigDeviceTest`: **one test passed**, 34.823 seconds. It visits all five scenes, compares actual rendered artwork pixels at two times, verifies paused and reduced-motion images are identical over time, and checks that previews do not schedule an alarm. No skip or permission bypass.
- Two idle screenshots and one head-pat screenshot per girl were saved under `../../outputs/Garden-Alarm-Motion-v1/device/`. Rendered frames from all five were visually inspected for face shape, framing and continuity.
- Temporary test helper removed after checking; main app remains installed and was reopened.

Log: `../../outputs/Garden-Presence-0.6.1-Rig-Device-Test.txt`. Packaged APK, source ZIP and verification JSON are under `../../outputs/` with prefix `Garden-Presence-0.6.1`.

## Implementation and limits

The rig uses a continuous 40 × 60 mesh with precomputed regional weights and reused frame buffers. Motion differs for each hair section, sleeve/accessory and head. Existing registered open/blink images share identical deformation. This is not a new separated-layer Cubism rig; it cannot reveal previously hidden anatomy or make large articulated changes of pose.

Requested redraws are bounded to approximately 30 fps; this run verifies visible motion and static pause behavior, not a formal GPU frame-time or battery benchmark. Tests do not re-establish locked-screen alarm launch or wait for a full five-minute snooze; see the separate 0.6 alarm test record for the prior scheduling checks and limits.
