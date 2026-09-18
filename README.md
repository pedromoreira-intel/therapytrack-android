# TherapyTrack — Android

Native Android client for TherapyTrack, written in Kotlin with Jetpack Compose.
Both roles: the **client (patient) side** — check-ins, journal, assessments,
messages, data rights — and the **therapist side** — today's alerts and
sessions, caseload with invitations, the pre-session brief, session notes,
scheduling, and messaging.

It talks to the same backend as the iOS app and follows the same rules:

- **Offline first for clinical writes.** Check-ins, assessments, journal entries
  and messages are written to an encrypted on-device queue before the network
  is tried, carry a `client_id` reused on every retry (so the server can
  recognise a replay), and record the time they were *done*, not sent.
- **An unreadable queue is never overwritten.** `offline/DurableQueue.kt`
  distinguishes "nothing queued" from "cannot read", and a file that will not
  decode is moved aside rather than dropped.
- **Work is bound to the account that wrote it.** Every queued item carries its
  owner; a different account signed into the same phone never sends or sees it,
  and every request is refused if the signed-in account changed after it began
  (`core/ApiClient.kt`).
- **Session refresh is guarded.** A refresh applies only if the refresh token
  it sent is still the one on the device.
- Portuguese is the default language (`res/values`), English in `res/values-en`.

## Build

Requires JDK 17+ and the Android SDK (`sdk.dir` in `local.properties`).

```
./gradlew assembleDebug testDebugUnitTest
```

Debug builds point at `http://10.0.2.2:3001/api` (the host machine from an
emulator); release builds at the production API. Debug builds also accept a
session by intent extras (`debug_token`, `debug_refresh`, `debug_user_id`,
`debug_role`) so a smoke run never needs typed credentials.

## Layout

```
core/       ApiClient, Api (typed endpoints), models, session store, timestamps
offline/    DurableQueue, FlushCoalescer, StuckWork, the four outboxes
clinical/   Scoring, CrisisResources, instrument definitions
ui/         Compose theme, auth screens, client and therapist shells and screens
```

## Known gaps

- Therapist extras that live in the iOS app only: professional records,
  supervision, intervision, referrals, billing, community, AI drafting.
- `GET /messages/threads` uses Postgres `DISTINCT ON`, so on the SQLite dev
  backend the therapist's Messages tab reports "could not load"; open a
  conversation from the client's page instead. Production is unaffected.
- Crisis numbers are chosen by device region, as on iOS. A Portuguese pilot
  on a phone set to another region shows that region's numbers.
- Instrument wording in Portuguese is a working translation pending clinical
  review (PHQ-9/GAD-7 official PT versions; WAI-SR licence).
- A queue whose encryption key is lost (device reset of the keystore) is
  reported as unreadable and left alone; nothing yet tells the person.
