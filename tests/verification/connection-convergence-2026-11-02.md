# Connection recovery convergence — 0.6.15

## New reproducible issues

1. Resume discarded its successful device.preview image and returned only live
   metadata. The UI then needed another capture. Resume now uses the shared
   capture path and returns the first image with its frame ID and viewer scope.
   A newer privacy pause still wins; failed resumption keeps the privacy pause.
2. A response without an embedded QR left "Creating QR code" visible despite an
   available pairing-window link. The fallback now has an accurate instruction.
3. An image decode failure left an empty QR area. It now exposes refresh and the
   pairing-window link. Old QR error handlers cannot affect a newer session.
   Status polling preserves fallback instructions until a phone actually scans.

## Evidence and stopping boundary

- Resume regression failed before implementation:
  /tmp/tapilot-convergence-before.log
- Both QR rendering regressions failed before implementation:
  /tmp/tapilot-qr-decode-before.log
- Added seven tests: first-frame resume, privacy on failed resume, runtime panel
  resume with viewer scope, confirmation/status race, immediate UI resume and
  control viewer identity, missing QR fallback, stale/decode failure recovery.
- Full plugin check: 147 passed, zero failed.
  /tmp/tapilot-convergence-final.log
- git diff --check passed.
- Audited cancellation, pending status, failure recovery, pause/resume, selection
  and stale-result paths. No additional reproducible failure remains in the
  exercised deterministic matrix.
- Not a claim that all product defects are eliminated: Codex embedded renderer,
  real-phone camera pairing and home/company network transitions remain separate
  live acceptance. Emulator was not stopped or altered.
