# Testing Callora

## Prerequisites

The repo's Gradle wrapper works, but the build needs an Android SDK. If `local.properties`
is missing, create it pointing at your SDK (forward slashes — backslashes are escape
characters in a `.properties` file and `\U` will fail to parse):

```
sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk
```

Required SDK packages: `platforms;android-36.1`, `build-tools;36.0.0`, `platform-tools`.

The debug build signs with `debug.keystore` at the repo root, which is **gitignored and not
committed**. Create it once, or the build fails at `validateSigningDebug`:

```bash
keytool -genkeypair -v -keystore debug.keystore -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10950 -dname "CN=Android Debug,O=Android,C=US"
```

---

## 1. Voice changer — audible test, no device required

Renders every preset to a WAV you can play on your desktop:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.example.audio.*' --rerun
```

Output lands in `app/build/demo-audio/`. Play `00-original.wav` first, then any preset file.
`01-none.wav` is byte-identical to the original by design — `VoiceEffect.NONE` short-circuits.

## 2. Unit tests

```bash
./gradlew :app:testDebugUnitTest --rerun
```

Covers the DSP (pitch accuracy per preset, streaming/block-size independence, clipping,
length preservation), WAV encode/decode, and the offline processor. Results in
`app/build/test-results/testDebugUnitTest/`.

**Pitch is verified with pure tones, not the synthetic voice.** A harmonic-rich signal makes
autocorrelation, HPS, and spectral-peak analysis disagree by an octave; a single tone cannot
be misread. If you add your own verification, use tones.

## 3. Screen rendering — no device required

Renders Compose screens to PNG on the JVM via Robolectric + Roborazzi:

```bash
./gradlew :app:testDebugUnitTest --rerun -Proborazzi.test.record=true
```

Output in `app/src/test/screenshots/`.

**`--rerun` is required.** Roborazzi writes PNGs as a side effect, not a declared task output,
so an up-to-date or cache-hit test task reports BUILD SUCCESSFUL and silently writes nothing.

## 4. On-device test

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then, for the voice changer:

1. **Plug in wired headphones.** On the loudspeaker the monitored output re-enters the mic and
   will howl; hardware AEC only partly suppresses it.
2. Settings → **Voice Changer**.
3. Tap a preset, then **Preview with headphones**. Grant the microphone permission.
4. Speak. You should hear yourself shifted, with the level meter tracking your voice.
5. Tap presets while previewing — they apply at the next audio block, without a dropout.

Checks worth running:

| Test | Expected |
|---|---|
| Leave the Settings screen while previewing | Preview stops; mic indicator clears |
| Receive a phone call while previewing | Preview stops (audio focus loss) |
| Deny the microphone permission | Inline message, no crash |
| Kill and reopen the app | Selected preset persists |
| Rotate the device while previewing | Audio continues without a glitch |

## 5. Release build

```bash
./gradlew :app:minifyReleaseWithR8
```

This validates R8 and the ProGuard rules **without needing a release keystore**. A full
`assembleRelease` additionally requires `KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD`,
plus the upload keystore — none of which are in the repo.

> Minification is enabled but has **not** been exercised at runtime. R8 succeeding proves the
> code links, not that every reflective path survives. Install a release build and exercise
> Gemini calls and Room reads before shipping.

---

## Known limitations

- **The voice changer does not alter WhatsApp or cellular calls.** WhatsApp opens the
  microphone in its own process and the cellular uplink runs mic → modem, bypassing the app
  layer. Android exposes no API to inject audio into either. The engine changes audio Callora
  itself owns; `VoiceChangerEngine.onFrame` is the hook a VoIP stack would consume, and no such
  stack exists yet.
- No emulator or connected device is configured in this checkout, so steps in §4 are the only
  ones that need hardware.
- `namespace` is still the scaffold default `com.example` while `applicationId` is
  `com.aistudio.callrecord.agency`.
- `google-services.json` is absent, so Firebase compiles (`WARN` strategy) but is inert.
