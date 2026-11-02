# QR recovery (0.6.12)

The 0.6.10 screenshot shows QR creation still pending (cancel disabled), not an
expired QR. Runtime inspection found pairing creation/status/cancel shared the
phone operation queue. A stalled connection or preview could therefore delay QR
creation before the existing child-process startup deadline even began.

Pairing creation/status/cancel now use their own serialized queue. Confirmation
remains on the phone queue because it changes credentials. The UI shows immediate
progress, bounds QR creation to 12 seconds, offers retry on failure, and permits
cancellation while creation is pending. Late responses cannot restore cancelled
pairing UI.

Connection recovery now offers Scan to reconnect directly. Scanning the same
phone updates the existing certificate-keyed entry; entering an IP is an advanced
fallback, not the primary recovery flow.

Validation: 129 plugin tests passed. A real runtime/TLS regression test holds an
old preview request unresolved while generating an actual QR and cancelling its
pairing window; neither request waits for the old phone. UI tests cover progress,
deadline, cancellation, late results, and the prominent scan action. The actual
UI module with mocked transport was inspected in Ego browser (space 15, replacing
expired space 14); its scan button renders the QR and cancellation UI correctly.
No embedded Codex visual acceptance or company-network phone scan is claimed.
