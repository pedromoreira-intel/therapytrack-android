# Android ↔ iOS parity plan (therapist side)

What the Android app still lacks versus the iOS therapist app, and the order to
build it in. Each phase is independently shippable; none touches the backend.

Ground rules that already hold and every phase keeps:

- **Reads** go straight to `Api` and show *could not load* (not *nothing here*)
  on failure.
- **Writes a clinician would grieve losing** go through an `Outbox` (on disk
  first, `client_id`, owner-bound, stuck row). Everything else is a plain
  request with an inline error.
- **Plan gating**: the server answers `402` (`ApiError.NotInPlan`) for a
  feature outside the plan. Screens show the message and a *see plan* link;
  they never hide a feature by guessing the plan locally.
- **AI consent**: the server answers `403` with a reason when the client has
  not consented to `ai_drafting`. Surface the reason; no local override.
- Booleans decode leniently (`LenientBoolean`) — the SQLite dev server sends 0/1.
- Portuguese in `values/`, English in `values-en/`, one commit per phase,
  emulator smoke against the throwaway SQLite backend, JVM tests for any logic
  that is not a screen.

Effort is a working-session estimate for one person with the codebase warm.

---

## Phase 1 — Plan & professional records  (~½ day)

Simple lists and forms; unlocks the gating UI every later phase needs.

**Endpoints**
- `GET /billing/me` → `{plan, status, current_period_end, source?, effective_reason?, features[], free_client_limit?}`
- `GET|POST /professional/credentials`, `PUT|DELETE /professional/credentials/:id`
  — body `{license_type, license_number, issuing_body, region, expires_on, notes}`
- `GET|POST /professional/training`, `DELETE /professional/training/:id`
  — body `{title, institution, training_type, completed_on, hours, certificate_url, notes}`
- `GET /professional/summary`

**Code**
- `core/Models.kt`: `ApiPlan`, `ApiCredential`, `ApiTraining`, `ApiProfessionalSummary`.
- `core/Api.kt`: the calls above.
- `core/Plan.kt`: `PlanState` (`StateFlow<ApiPlan?>` in `AppContainer`, loaded
  at sign-in and on `NotInPlan`), `fun ApiPlan.has(feature)`.
- `ui/common/PlanGate.kt`: `@Composable NotInPlanCard(error, onSeePlan)` used
  by every gated screen.
- `ui/therapist/PracticeScreen.kt`: a new **Prática** tab (iOS "Practice"):
  sections *Plano*, *Credenciais*, *Formação (CPD)*, and entry points for
  phases 2–4 (rendered disabled until those land).
- `ui/therapist/ProfessionalScreens.kt`: credentials list + add/edit sheet;
  training list + add sheet; expiry pill (red when `expires_on` < 60 days).
- Profile: replace the "iOS only" line with the plan card.

**Strings**: ~40 (plan names, statuses, credential/training fields).

**Smoke**: add a credential and a training entry; expired credential shows red;
`/billing/me` on the dev server returns `network` (DEFAULT_PLAN) — check the
card says so.

---

## Phase 2 — Referrals & colleague directory  (~1 day)

Network-plan features; first place the 402 path is exercised for real.

**Endpoints**
- `GET /profiles/directory?…` (filters: specialty, city, language, accepting),
  `GET /profiles/:therapistId`, `GET|PUT /profiles/me`
- `GET /referrals?direction=sent|received`
- `POST /referrals` — `{to_therapist_id, delivery: 'in_person'|'online'|'either', urgency: 'routine'|'soon'|'urgent', presenting_issue, population, language, city, note}`
  (no client identity travels: the referral is a description, not a record)
- `PUT /referrals/:id/respond` — `{accept: Boolean}`; `PUT /referrals/:id/withdraw`

**Code**
- Models: `ApiTherapistProfile`, `ApiReferral` (with `direction`, `status`,
  `counterpart` name), `ApiDirectoryEntry`.
- `ui/therapist/DirectoryScreen.kt`: filter chips + list → profile page →
  *Referenciar* button.
- `ui/therapist/ReferralScreens.kt`: new-referral form; inbox/outbox tabs;
  accept/decline/withdraw with confirmation.
- `ui/therapist/MyProfileScreen.kt`: edit own directory profile
  (specialties, languages, city, accepting new clients, bio).
- Inline `NotInPlanCard` on Directory and Referrals for free/professional plans.

**Smoke**: needs two therapists on the dev server (register a second via curl);
send a referral A→B, decline as B, withdraw one as A. Force `DEFAULT_PLAN=free`
once to see the 402 card.

---

## Phase 3 — Supervision log  (~½ day)

**Endpoints**
- `GET|POST /intervision/supervision/sessions`, `DELETE …/:id`
  — body `{supervisor_name, supervisor_credentials, supervision_type, session_date, hours, topics, notes, rating}`
- `GET|POST /intervision/supervision/network`
  — body `{professional_name, credentials, specialization, is_online, hourly_rate, contact_email}`

**Code**
- Models: `ApiSupervisionSession`, `ApiSupervisor`.
- `ui/therapist/SupervisionScreens.kt`: log list with hours total per year
  (CPD evidence), add-session sheet, supervisor network list + add.
- Hook into Prática tab.

**Decision to make**: whether a supervision session is outbox-worthy. It is
reflective text the therapist wrote and cannot recreate → **yes**, a small
`SupervisionOutbox` (same `Outbox<T>` base, `StuckItem.Kind.SESSION_NOTE`
reused or a new `SUPERVISION` kind with `isIrreplaceable = true`).

---

## Phase 4 — Intervision groups  (~1½ days, the largest)

**Endpoints**
- `GET /intervision/groups` (public + mine), `GET /intervision/groups/:id`,
  `GET /intervision/my-groups`
- `POST /intervision/groups` — `{name, description, focus_area, meeting_schedule, is_online, max_members, meeting_link}`
- `POST /intervision/join` — `{group_id, message}`; `PUT /intervision/requests/:id` — `{accept}` (group owner)
- `GET /intervision/groups/:id/discussions`, `POST /intervision/discussions` — `{group_id, title, …}`
- `GET|POST /intervision/discussions/:id/messages`
- `GET /intervision/groups/:id/meetings`, `POST /intervision/meetings` — `{group_id, title, agenda, meeting_link, scheduled_at, duration_minutes}`

**Code**
- Models: `ApiIntervisionGroup`, `ApiJoinRequest`, `ApiDiscussion`, `ApiDiscussionMessage`, `ApiMeeting`.
- `ui/therapist/intervision/`: `GroupsScreen` (mine / discover), `GroupScreen`
  (members, pending requests if owner, discussions, meetings), `DiscussionScreen`
  (reuses the bubble layout from `ConversationScreen` — extract `MessageList`
  first), `NewGroupSheet`, `NewMeetingSheet`.
- Discussion messages: plain request (they are conversational, retyped
  cheaply) — **no outbox**, mirrors the iOS decision.
- Free plan: `intervision.unlimited` missing → server may cap groups; show the
  402 card when it does.

**Smoke**: two therapists; A creates a group, B requests to join, A accepts,
both post in a discussion, A schedules a meeting.

---

## Phase 5 — Client page parity  (~1 day)

The Android client page has the brief and notes; iOS also has:

- **Goals & homework** — `GET /goals?patient_id`, `POST /goals` `{patient_id, title, description, category, due_date}`, `PUT /goals/:id` (progress/completed), `DELETE`.
  Add a *Objetivos e tarefas* section: list with progress, add sheet, mark complete.
- **Assessment history** — `GET /assessments/{phq9|gad7|wai-sr}?patient_id`
  → a simple line chart per instrument (Compose `Canvas`; no charting
  library needed for three series), severity bands shaded.
- **Check-in chart** — `GET /ema?patient_id` → mood/anxiety/sleep over 30 days.
- **Shared journal** — `GET /journal` (therapist role returns shared entries
  for their caseload) filtered by `patient_id`; read-only list.
- **Feature toggles** — `GET|PUT /client-features/:patientId` `{feature_name, enabled}`,
  `PUT …/bulk`. Switches for what the client's app shows (check-ins,
  assessments, journal sharing, messaging).
- **Edit client** — `PUT /patients/:id` for diagnosis, status, risk level.

All reads; goals get a plain request with inline error (the iOS app does the
same).

---

## Phase 6 — AI drafting  (~½ day)

Gated twice: plan (`402`) and client consent (`403` with `reason`).

**Endpoints**
- `POST /session-notes/generate` — `{transcript, patient_id}` → draft fields
- `POST /ai-features/summarize-session`, `…/suggest-interventions`,
  `…/generate-progress-report`, `…/treatment-plan` — all take `patient_id`
- `GET /ai-features/features`

**Code**
- `ui/therapist/AiDraftSheet.kt` opened from the session-note editor:
  paste/dictate transcript → *Gerar rascunho* → fills focus/interventions/
  progress/homework/plan fields **as editable text, never auto-saved**.
- Consent state visible on the client page (from `GET /privacy/consent` is
  the client's own; for the therapist the 403 reason is the signal) — show a
  one-line *Cliente não deu consentimento para rascunhos com IA* when the
  server says so, with no way to bypass.
- Progress report: render the returned text in a scrollable sheet with
  *Copiar*.

**Smoke**: `tests/lib/anthropic-stub.js` on :4010 already fakes the model;
run the dev server with `ANTHROPIC_BASE_URL=http://localhost:4010`; walk
invite-accept + consent for the seeded client (as the backend tests do).

---

## Phase 7 — Community  (~1 day, lowest value; do last or skip)

`GET|POST /posts`, `GET /posts/:id`, `POST /posts/:id/{vote,reply,accept}`.
Feed, post detail with replies, compose. Plain requests. Only worth doing if
the community is actually used in the pilot.

---

## Cross-cutting, do alongside phase 1

- **`Prática` tab** replaces the four-tab bar with five: Hoje · Clientes ·
  Prática · Mensagens · Perfil (iOS has four because Messages lives inside
  Today there; keep Android's separate tab — it tested well).
- **`MessageList` extraction** from `ConversationScreen` so intervision
  discussions reuse it (phase 4).
- **Backend, optional**: `GET /messages/threads` uses Postgres `DISTINCT ON`;
  port it to a window function or `GROUP BY` so the SQLite dev path works.
  Not needed for production; needed for a complete dev-server smoke.
- **Tests to add**: `PlanTest` (feature lookup, expired → free), `ReferralShapeTest`
  (direction/status mapping), `SupervisionOutboxTest` if phase 3 adds an
  outbox — all JVM, no emulator.

## Order and total

1 → 2 → 3 → 4 → 5 → 6 (→ 7). Roughly five working days for full parity;
phases 1, 3 and 5 alone (~2 days) cover everything a solo-practice pilot
therapist touches. Phases 2 and 4 only matter once more than one clinician is
on the platform.
