# Changelog

All notable changes to this project are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [1.0.0] — 2026-09-05

First public release.

### Added

- **The pad.** The whole screen is one button. Every tap fires the next of two
  MIDI notes, alternating, so one finger produces the alternating key presses a
  stream needs. Over USB MIDI, with nothing to install on the PC.
- **Latency as the design constraint.** The MIDI byte goes out before any
  animation state is touched, the path from touch to byte allocates nothing,
  touch dispatch is unbuffered on Android 13 and up, and the fastest display
  mode available is requested at startup.
- **Tempo gauge**, reading 1/4 BPM from a trimmed mean of recent tap intervals.
  The scale is piecewise with anchors on 140 / 200 / 220 and headroom to 260,
  so the range that decides a map gets the most bar. It falls the moment
  tapping stops — no decay constant, just a ceiling of one tap per silence.
- **Chain and total counters**, with a milestone flash every 50 hits.
- **Automatic Do Not Disturb** for the length of a session, behind Android's
  one-time notification policy grant. A heads-up notification steals the touch
  focus and ends a run; this is the only real fix for it.
- **First-run walkthrough**: five steps in the order the problems actually
  arrive in — cable, the game's MIDI setting, key binding, notifications, the
  pad — carrying the permission grant on the step that explains it. Reachable
  again from the connection screen.
- **Demo mode.** With no device connected the button becomes DEMO and opens the
  same pad with the notes going nowhere, so the app can be judged before a
  cable is found.
- **Settings**: debounce, chain window, the two MIDI notes, screen behaviour,
  lite graphics and tap diagnostics, persisted across launches, with a reset
  that asks first.
- **Lite graphics mode** for phones that drop frames: waves, sparks, orbital
  brackets, text glow and the counter spring all go.
- **Idle fade**, so a pad left alone stops animating against an OLED panel.
- **Tap diagnostics** overlay: last interval, dropped taps, held notes.
- 22 unit tests over the engine, the tempo maths and the gauge scale, plus a
  Compose UI test covering the touch-to-note wiring.

### Notes

- No network access, no analytics, no accounts. MIT licensed; the bundled
  Tektur typeface is under the SIL Open Font License.
- MIDI channel and velocity are deliberately not settings: osu! binds on pitch
  and ignores velocity, so both could only break a working setup.

[1.0.0]: https://github.com/shamil-aminov/alternate/releases/tag/v1.0.0
