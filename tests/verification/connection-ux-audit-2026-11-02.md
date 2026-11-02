# Connection interaction audit — 0.6.14

Audited device selection, pairing, interrupted requests, page return, and failure
feedback. No physical devices or emulator were restarted.

## Reproduction and fixes

Four new interaction regressions failed on 0.6.13 before implementation:
- An unresolved old preview blocked scan polling despite the independent server
  pairing queue. Poll ownership now follows the UI epoch; stale completion cannot
  overwrite a newer poll schedule or initiate another preview.
- Cancelling a visible QR left the scan prompt until a subsequent preview.
  Cancellation now immediately renders a recoverable state, without old pixels.
- A rejected scan status request left an inert QR. Failure now exposes retry.
  A nonresponding status read has a five-second deadline; late answers are ignored.
- Confirming a phone had no progress text. It now shows localized confirmation
  progress, invalidates earlier status reads, and restores the button on failure.

Additional cases:
- Pause/disconnect are disabled and guarded during pairing.
- Empty selection no longer retains the forgotten active device name.
- Pair completion resets picker selection to follow the newly paired phone.
- Interrupted QR creation no longer leaves the page busy on return; page return
  immediately renders connecting and updates controls.

## Verification

- Before: /tmp/tapilot-ux-audit-before.log — four reproducible failures.
- Final: npm --prefix plugins/tapilot run check — 140 tests passed.
- Eight new interaction tests cover the cases above, including stalled/late
  responses. Existing runtime TLS identity and network-address tests remain green.
- git diff --check passed.
- Codex embedded UI and physical cross-network recovery were not exercised;
  these results establish deterministic UI/runtime regression coverage, not
  real-phone network or host presentation acceptance.
