# Contributing

Thanks for looking. Issues and pull requests are both welcome.

## Getting set up

Clone, open in Android Studio, and build:

```bash
./gradlew assembleDebug
```

Run the unit tests — they need no device and cover the engine, the tempo
maths and the gauge scale:

```bash
./gradlew test
```

The instrumented tests need a phone or emulator attached:

```bash
./gradlew connectedAndroidTest
```

## What the code is trying to be

This is a latency app. Two rules follow from that, and a change that breaks
either needs a good reason in the commit message:

1. **Nothing allocates on the path from a touch to the MIDI byte.** That
   means `AlternateEngine.press`, `TapStats.onTap` and
   `MidiRepository.sendNote`. No collections, no boxing, no `runCatching`,
   no logging. Derived values are computed in the frame loop instead — see
   the split between `TapStats.onTap` and `TapStats.refresh`.
2. **The MIDI note goes out before anything else happens.** Animation state
   is updated after the send, never before.

Beyond that: `keypad/` must not import anything from `android.*` or Compose
UI. Keeping it free of them is what makes it testable on the JVM.

## Style

- Comments explain *why*, not *what*. If a constant was chosen by feel, say
  what it felt like and what would break if it moved.
- Comments and identifiers in English.
- User-facing text goes in `res/values/strings.xml`, never inline.
- Magic numbers live in a named constant near the bottom of their file.

## Release signing

Release builds fall back to the debug key, so `./gradlew assembleRelease`
works on a fresh clone. To sign with your own key, generate a keystore:

```bash
keytool -genkeypair -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias alternate
```

Then create `keystore.properties` in the project root — it is git-ignored,
and it must stay that way:

```properties
storeFile=release.jks
storePassword=<your password>
keyAlias=alternate
keyPassword=<your password>
```

CI reads the same values from the environment instead:
`ALTERNATE_STORE_FILE`, `ALTERNATE_STORE_PASSWORD`, `ALTERNATE_KEY_ALIAS`,
`ALTERNATE_KEY_PASSWORD`.

Never commit a keystore or its passwords.

## Pull requests

- One change per pull request.
- Add a test if the change touches `keypad/`.
- Run `./gradlew test` before opening it.
- Say what you changed and why. Screenshots help for anything visual.
