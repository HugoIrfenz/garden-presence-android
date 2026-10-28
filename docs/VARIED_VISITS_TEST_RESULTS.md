# Varied visits 0.3 — 12 October 2026

Installed on Hugo's Xiaomi 14T Pro, Android 16 / HyperOS OS3.0, preserving existing preferences and permissions.

## Content and behavior

- 12 authored lines per girl (60 total), four gesture choices each, 12 distinct layer-gesture curves overall.
- Independent per-character shuffled line and gesture decks persist across app restarts. Every line is used before the deck refills; the previous line and gesture cannot immediately repeat at a normal cycle boundary.
- The foreground twelve-second preview and screen-off visit share the content/gesture selection. Blocked or never-visible screen-off launches do not consume a line.
- Gestures move registered head, hair, hand, sleeve and accessory layers conservatively. Reduced motion disables displacement. No new bitmap assets, network requests, audio or lip-sync were introduced.

## Verification

- Gradle build, unit tests and Android lint passed: 11 JVM tests; lint 0 errors, 11 warnings, 1 hint.
- Four Node tests passed: native/renderer motion-name compatibility, all gesture bounds and return-to-rest curves, distinct motion profiles, and reduced-motion neutrality.
- Four instrumentation tests passed on the physical phone in 34.783 seconds. Coverage includes all five new visits using their own authored native quote and a supported active gesture; five-character asset loading without fallback; rapid switching; twelve-second completion; background pause/resume.
- All five physical-device preview screenshots inspected: art/layer registration and native quote text remain readable. Files are `outputs/Garden-Presence-0.3-{character}.png` in the workspace. No clipped character artwork or detached layer issue was observed in these samples.
- Xiaomi required manual approval for the temporary test helper. It was removed after successful testing. User's selected character was restored by the test suite.

These device tests exercised foreground visits. The screen-off wake/lock/close path was physically verified in 0.2.1 and its scheduling/lifecycle behavior is retained; no new background-launch claim is inferred from instrumentation. Long-term battery and scheduling behavior remain outside this short verification run.
