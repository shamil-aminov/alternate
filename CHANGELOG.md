# Changelog

All notable changes to this project are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [1.0.0] — unreleased

First public release.

### Added

- Settings screen: debounce, chain window, MIDI notes, channel, velocity,
  screen behaviour, lite graphics and diagnostics, persisted across launches.
- Automatic Do Not Disturb for the duration of a session, behind Android's
  one-time notification policy grant. A heads-up notification steals the
  touch focus and ends a run; this is the only real fix for it.
- How-it-works screen covering the cable, the game's MIDI setting, key
  binding and notifications.
- Demo mode. With no MIDI device connected the START button becomes DEMO and
  opens the pad anyway, with the notes going nowhere and a banner saying so,
  so the app can be tried before a cable is found.
- Tap diagnostics overlay: last interval, dropped taps, held notes.
- Demo mode, a carousel for the instructions, and a sticky back arrow on the
  settings and instructions screens.
- Chain milestone flash every 50 hits.
- Idle fade, so a pad left alone stops animating against an OLED panel.
- 22 unit tests over the engine, the tempo maths and the gauge scale, plus a
  Compose UI test covering touch-to-note wiring.

### Changed

- The tempo gauge now reads BPM. It previously read an internal tap-energy
  accumulator, which is why it looked empty at 60 BPM and lagged at 200.
- The gauge scale is piecewise, with anchors on 140 / 200 / 220 and headroom
  to 260, so the range that decides a map gets the most bar.
- The gauge is a single-colour outline divided into blocks that light up. No
  colour ramp, no tick marks, no
  peak mark: height already says everything the decoration was restating.
- The gauge falls the moment tapping stops. There is no decay rate: with no
  tap for a given stretch the rate through it cannot exceed one tap in that
  stretch, so the reading is capped there and drops away on its own, faster
  the faster you were going.
- The gauge is much less smoothed: shorter time constants both ways, a
  stiffer block spring and less averaging in the rate, so the top block
  moves with the jitter of the stream instead of an average of it.
- The settings list is centred; the back chevron keeps the corner.
- The walkthrough opens on a card saying what the app is before it starts
  giving instructions.
- A first-run walkthrough. The carousel opens once on a fresh install and
  carries the notification-policy grant on the step that explains it, so the
  permission is asked for where it makes sense rather than found later under
  a name nobody recognises. That row is reworded too.
- The "3 / 5" counter beside NEXT is gone; the dots already said it.
- The gauge block at the top of the stack is lit in proportion to how far
  into its own step the tempo has come, so the shading answers to changes
  far smaller than a block instead of only appearing mid-spring.
- Gauge blocks shade from the deep blue up to the accent as they spring on,
  so the travel can be watched rather than only its two ends.
- The settings back chevron sits in the same corner as the instructions one,
  over a list that now owns the full height of the window.
- Fullscreen on every screen, not just the pad, under one setting.
- The tempo needs three intervals before it reports anything, and trims the
  extremes from three samples up. Three quick taps used to read 500 BPM.
- The gauge is blocks alone; the outline that boxed them in is gone, so the
  column squares up with the BPM readout above it.
- Screen changes are a plain cross-fade. The per-screen assembly looked
  deliberate alone and arbitrary in use — the order pieces arrived in had no
  relation to the order anyone reads them.
- Reset to defaults is a list row that asks before it fires, not a button.
  Both answers wear the quiet outline; the destructive one is red.
- Every screen assembles from the edges when it appears, staggered, instead
  of arriving whole. Screen changes underneath are a short fade.
- Gauge blocks are outlined individually and spring on and off, spending the
  overshoot on a flare rather than on movement.
- Screen titles are gone. A screen reached by tapping SETTINGS does not need
  to open with the word SETTINGS, and on a landscape phone the heading was
  charging a fifth of the height to repeat the tap that got you there.
- Screens scale as they cross-fade, forward and back, instead of a flat
  dissolve.
- The back chevron is drawn rather than set in type, so it sits level with
  the title instead of hanging below it.
- BEST is gone from the pad, and TOTAL no longer punches — it is a running
  count, not an event.
- MIDI channel and velocity are no longer settings. osu! binds on pitch and
  ignores velocity, so both could only break a working setup.
- Lite graphics now drops waves, sparks, orbital brackets, text glow and the
  counter spring, not just sparks.
- One colour rule across the app: accent for what is live or interactive,
  bone for values and titles, steel for labels and captions.
- The former gauge colour ramp ran white to blue, with halo and vein towards the
  top to offset white being the brighter colour on black.
- Tempo smoothing is asymmetric — 80 ms rising, 400 ms falling — so the gauge
  snaps up to a tempo and lingers on the way down.
- The counter punch is a spring rather than a linear fade.
- Rebuilt the connection screen for landscape: two columns, with the wordmark
  and navigation on the left and the device list and button on the right. The
  old single centred column ran off the bottom of a landscape phone, leaving
  the start button unreachable.
- Settings are one list with section headings instead of five panels, with
  every row ending in a control area of the same width so the steppers and
  switches line up down the page.
- How it works is a five-step carousel instead of a page of panels, with the
  copy cut to a line or two per step.
- Back arrows on the settings and instructions screens.
- One modular type scale throughout; the HUD font scale is pinned so a system
  text-size setting cannot push readouts into each other.
- HUD geometry is derived from screen size rather than fixed in dp.
- All comments and user-facing strings are in English; strings moved to
  resources.

### Fixed

- The tempo maths ran on the touch thread, sorting and allocating three arrays
  per tap on the way to sending the MIDI note. It now records an interval in
  constant time and computes in the frame loop.
- Held notes were not released when the window lost focus. A heads-up
  notification does not stop the activity, so `onStop` never fired and a held
  note stayed down in the game.
- `allNotesOff` shared a message buffer with `sendNote` across two threads.
- The gauge allocated roughly 48 `Path` objects per frame.
- The BPM readout read a per-frame state during composition, recomposing the
  text on every frame instead of ten times a second.
- The MIDI port was not dropped when its device was unplugged.
- The core radius used for spark spawning went stale after a screen rotation.

[1.0.0]: https://github.com/shamik230/alternate/releases/tag/v1.0.0
