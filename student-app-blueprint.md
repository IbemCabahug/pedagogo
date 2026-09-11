# Student Academic Assistant — Product Blueprint (v1)

**Context locked in from interview:** Solo Android developer (Kotlin), targeting general university/college students in the Philippines, open-ended timeline, free/open-source (no monetization pressure), privacy stance = on-device first.

---

## 1. Executive Summary

The original concept — an all-in-one academic assistant with scheduling, alarms, note scanning, and AI — is too broad for a solo developer to ship. Research on why planner apps get abandoned points to one dominant failure mode: manual data entry friction, followed by "guilt retention" (accumulating overdue items making the app unpleasant to open). The technically strongest, most defensible starting point is **not** a general planner — it's a fast, accurate way to get a printed/typed class schedule onto a phone and reliably get alerted before each class, done better than Google Calendar + a stock alarm app does it today.

Recommendation: build **one thing extremely well** (schedule capture → structured calendar → class-aware alarms) before touching notes, AI, or task management.

---

## 2. Problem Statement

Filipino university students currently scatter academic information across Messenger group chats, screenshots, paper schedules, and LMS portals (e.g., Canvas, Google Classroom, in-house portals). The actual highest-value problem, based on your input ("all of the above") and the research, decomposes into:

1. **Entry friction** — getting a class schedule into any digital system takes real manual effort, which is the top reason planner tools are abandoned before they help anyone (industry-source pattern, not peer-reviewed, but consistent across several independent builders).
2. **Missed-class risk** — generic alarms/calendar notifications aren't tuned to "get up and physically get to a specific room by a specific time."
3. **Procrastination**, which research shows is a self-regulation issue, not a notification issue — reminders help most when tied to a specific plan the student already made (implementation-intentions research), not as generic nags.

**Chosen highest-value problem for v1:** #1 and #2. #3 is a Tier-3 problem that needs behavioral data you don't have yet.

---

## 3. Target Users & Persona

**Primary persona — "Jana," 2nd-year BS student, Cebu-based university.**
- Has 5-7 subjects/semester, each with recurring weekly time slots, some with lab sections.
- Gets her schedule as a screenshot from the registrar's portal or a printed COR (Certificate of Registration).
- Uses Messenger group chats for class updates, Google Calendar sporadically, phone alarm clock separately.
- Has missed or been late to class from sleeping through a generic alarm or forgetting a room change.
- Android device, mid-range, cares about storage/data usage.

Education-student-specific needs (practicum schedules, lesson plans, classroom observations) are real but are a **Tier 3/4 vertical**, not a v1 requirement — validate with general students first before building a niche you assume matters.

---

## 4. Product Definition

- **Name placeholder:** Iskedyul (working title — Cebuano/Filipino-inflected, "schedule")
- **One sentence:** An Android app that turns a photo of your printed class schedule into a real calendar with alarms tuned to actually get you to class.
- **Core problem:** Manual schedule entry is tedious enough that students don't do it, so they miss classes despite "having a calendar app."
- **Primary use case:** Onboarding scan → structured weekly schedule → class-specific alarms.
- **Secondary use case:** Manual add/edit of one-off events (exams, deadlines) once the base schedule exists.
- **What it should NOT attempt in v1:** notes, OCR of handwriting, AI chat, task/project management, study-plan generation, gamification.
- **Product philosophy:** Reduce the friction of getting real academic information into a reliable alert system. Never guilt the student for what they didn't do — the app should feel calmer to open after a bad week, not worse.

---

## 5. Feature Tiers

### Tier 1 — MVP (build first)
| Feature | Purpose | AI needed? | Notes |
|---|---|---|---|
| Schedule photo capture + OCR (printed/typed only) | Kill manual entry friction | No — ML Kit on-device Text Recognition + rule-based table parser | Must have human-in-the-loop confirmation screen; never auto-commit |
| Manual schedule editor | Fallback / correction path | No | Required because OCR will never be 100% |
| Weekly recurring calendar view | See the semester at a glance | No | Local Room database |
| Class-aware alarm system | The actual differentiator | No | AlarmManager + exact-alarm permission handling, prep-time offset |
| Basic one-off events (exam, deadline) | Cover non-recurring items | No | Simple CRUD |
| Today/Next-up dashboard | "What do I need to know right now" | No | See Section 9 |
| On-device only storage, no account required | Matches your privacy stance, zero backend cost | No | SQLite/Room, no login wall |

### Tier 2 — High-value expansion (post-MVP, after real usage data)
- Cloud backup/sync (opt-in, e.g. simple account so a phone reset doesn't wipe the semester)
- "Leave now" / location-aware reminder (requires GPS/geofencing — real battery and permission cost, only add once Tier 1 alarms are proven reliable)
- Multi-stage reminders (e.g., night-before + morning-of)
- Exam/deadline countdown widgets
- Simple task list tied to subjects (not a full project manager)

### Tier 3 — Advanced / AI (only after Tier 1–2 are stable and you have a real user base to justify recurring API cost)
- Cloud-vision-assisted OCR for handwritten schedules (paid API, opt-in, explicit "this leaves your device" warning)
- Notes photo capture + OCR + tagging
- AI flashcard/quiz generation from notes
- Rule-based (not AI) study-session suggestion engine — a constraint scheduler is enough; don't market it as AI
- AI academic chatbot

### Tier 4 — Explicitly future / do not build now
- Practicum/lesson-plan/portfolio tooling for education majors (validate demand first)
- Full LMS integration
- Social/collaborative features
- Gamification/streaks (research shows these often *cause* the guilt-retention abandonment pattern rather than prevent it)

---

## 6. Alarm Architecture — the core differentiator

**Why not just use Android's built-in alarm/calendar notifications:** stock alarms are time-only, with no concept of "which class," "which room," or "how long it takes to get ready." Calendar notifications are typically a single fixed offset (e.g., 10 min before) with no retry/escalation logic and are subject to the same OS battery restrictions without special handling.

**Technical reality that shapes the design:** since Android 13+, `SCHEDULE_EXACT_ALARM` is no longer pre-granted by default to newly installed apps; scheduling precise alarms without checking `canScheduleExactAlarms()` first will throw a `SecurityException`. Apps that function as an alarm/calendar app can instead declare `USE_EXACT_ALARM`, which is granted automatically at install and behaves like `SCHEDULE_EXACT_ALARM` — this app qualifies as that category and should use it, while still handling the runtime check defensively and listening for `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.

**Design:**
- Each class instance generates an alarm at `class_start_time − prep_offset` (student-configurable per subject, default 30 min).
- Alarms are **not** silent notifications — they use `setAlarmClock()` (the same class Android's own clock app uses) so they survive Doze and show the reliable "next alarm" system indicator.
- Snooze is capped (e.g., 2x, 5 min each) to avoid the classic "snoozed through the whole class" failure.
- A missed-alarm (dismissed without confirmation, or ignored past class start time) logs quietly — no shaming UI — and can optionally nudge "did you make it to class?" only if the student has opted into that check-in.
- Recurring weekly pattern is stored once per subject/section and expanded into individual alarm instances by a `WorkManager` job that runs periodically (not by scheduling months of alarms at once, which is unreliable across reboots/updates).

**Explicitly excluded from v1:** location-based "leave now" alerts (Tier 2 — needs geofencing, background location permission, and battery-cost validation first).

---

## 7. Schedule Scanner Architecture

```
Photo capture (CameraX)
   → Image preprocessing (contrast/deskew)
   → ML Kit on-device Text Recognition (Latin script)
   → Heuristic table/grid reconstruction (row/column clustering by pixel position)
   → Field classification (subject / instructor / room / day / time via regex + position heuristics)
   → Confidence scoring per field
   → Human confirmation screen (every low-confidence field highlighted, nothing silently accepted)
   → Structured Schedule object
   → Calendar + alarm generation
```

**Hard limitation to design around, not hide:** ML Kit's on-device recognizer is built and tuned for printed/typed Latin-script text; recognition quality depends heavily on image resolution, lighting, and font clarity, and informal developer reports rate its handwriting recognition as effectively unusable for real handwritten input. **Do not market handwritten-schedule scanning as a v1 feature.** The confirmation screen is not optional — it is the feature that makes an imperfect OCR pipeline trustworthy instead of dangerous (a wrong room number silently entered is worse than no scanner at all).

**Note scanner (Tier 3):** same OCR engine, but far less forgiving — printed notes, math notation, diagrams, and mixed handwriting/print each need separate handling, and full note-intelligence (summarization, flashcards) needs a cloud LLM, which is out of scope until there's a reason to take on that cost and complexity.

---

## 8. Reminder Strategy (Tier 2 refinement)

Research on implementation intentions found that reminders boost compliance most when attached to a specific plan the person already made, and that over-reliance on reminders can undermine the habit forming on its own. Design implication: default to plain, predictable reminders tied to things the student explicitly scheduled ("Lesson Plan due tomorrow 8AM"), not inferred "smart" framing ("you have 45 minutes free tonight") — the latter requires accurately knowing free time, and being wrong erodes trust faster than a plain reminder would. Only build adaptive/contextual reminders once you have real behavioral data from Tier 1 users showing plain reminders aren't enough.

---

## 9. Dashboard UX Hierarchy

Answers "what do I need to know and do right now," in priority order:
1. **Next alarm/class** (biggest visual weight — subject, room, time until, prep-offset countdown)
2. **Rest of today's classes** (compact list)
3. **Upcoming deadlines within 72 hours** (exams/assignments only — not a full task list in v1)
4. **This week at a glance** (tap-through to full calendar)

Explicitly excluded from the dashboard: streaks, overdue counters, red badges, or anything framed as a failure count — this is a direct response to the "guilt retention" abandonment pattern found in the research.

---

## 10. Data Model (Tier 1)

```
User (local-only profile, no auth in v1)
  id, display_name, prep_offset_default

Subject
  id, name, instructor, section, color

ClassSlot   (recurring weekly pattern, generated from OCR or manual entry)
  id, subject_id, day_of_week, start_time, end_time, room, semester_id

Semester
  id, name, start_date, end_date

Event        (one-off: exam, deadline, activity)
  id, subject_id (nullable), title, datetime, type

Alarm
  id, class_slot_id or event_id, trigger_time, prep_offset, status (pending/fired/dismissed/missed)

ScanResult   (audit trail of OCR sessions, for debugging/improving the parser)
  id, raw_image_ref (local only), extracted_fields_json, confirmed_by_user (bool)
```

All tables live in a local Room (SQLite) database. Nothing leaves the device in v1 — no backend, no account, no cloud sync, matching your stated privacy preference and eliminating hosting cost entirely.

---

## 11. Technical Stack

- **Platform:** Native Android (Kotlin) — matches your existing comfort, avoids Flutter/RN learning curve, and gives direct access to `AlarmManager`/`WorkManager` without cross-platform plugin friction, which matters a lot for an alarm-centric app.
- **Local DB:** Room (SQLite wrapper)
- **OCR:** ML Kit Text Recognition (on-device, Latin script bundle) via CameraX capture
- **Background scheduling:** AlarmManager (`setAlarmClock()`/`USE_EXACT_ALARM`) for alarms, WorkManager for periodic recurring-schedule expansion
- **No backend in v1** — everything local. If Tier 2 cloud backup is added later, Firebase or Supabase free tier is sufficient for a small non-monetized user base.

---

## 12. Offline-First

Everything in Tier 1 works fully offline by design — there is no cloud dependency to design around yet. Tier 2 cloud backup and Tier 3 AI features are the only pieces that require connectivity; both should degrade gracefully (app remains fully usable with them switched off).

---

## 13. Privacy & Security

- No account, no login, no server in v1 → no centralized student data to breach.
- Scanned images: process on-device, then let the user choose to keep or discard the source photo (default: discard after confirmation, keep only the structured data).
- If/when Tier 2 sync or Tier 3 cloud OCR is added, this must be **explicitly opt-in**, with plain-language disclosure of what leaves the device and to whom (Google's ML Kit cloud tier or any third-party LLM API), not bundled silently into an update.
- No analytics/telemetry by default in v1; if added later, aggregate and anonymized only, disclosed in-app.

---

## 14. Feasibility Ratings (Tier 1 features)

| Feature | Dev effort | Infra cost | Privacy risk | User value |
|---|---|---|---|---|
| Printed-schedule OCR + confirmation | 🟡 Moderate | 🟢 None (on-device) | 🟢 Low | 🟠 High |
| Class-aware alarms | 🟡 Moderate (permission handling, reboot persistence) | 🟢 None | 🟢 Low | 🟠 High |
| Manual calendar/task CRUD | 🟢 Easy | 🟢 None | 🟢 Low | 🟡 Medium (table stakes) |
| Dashboard | 🟢 Easy | 🟢 None | 🟢 Low | 🟡 Medium |
| Handwritten OCR (excluded from v1) | 🔴 Highly difficult/risky if attempted on-device | 🟢–🟠 depends on approach | 🟠 Higher if cloud | 🟡 Medium (expectation-risk high) |

---

## 15. Roadmap

**Phase 0 — Research (done via this document):** problem validation, technical constraint research, competitive gap analysis.

**Phase 1 — Prototype (weeks):** manual schedule entry + class-aware alarms only, no OCR yet. Goal: prove the alarm architecture works reliably across reboots, Doze, and OEM battery-optimization quirks (a known cross-device pain point) before adding OCR complexity on top.

**Phase 2 — MVP (add OCR):** schedule photo capture, confirmation flow, dashboard. Ship to a small group of real students (classmates, org-mates) for a full week of real class schedules.

**Phase 3 — Beta:** fix OCR edge cases found in real COR/registrar-portal formats, refine alarm reliability across more devices, add basic one-off event support if not already in.

**Phase 4 — Tier 2:** opt-in backup/sync, multi-stage reminders, subject-linked simple tasks.

**Phase 5 — Tier 3 (only if Tier 1–2 retain real users):** cloud-assisted handwriting OCR, notes capture, AI features — each gated behind a clear user-demand signal, not built speculatively.

---

## 16. Testing Strategy

- **Alarm reliability testing:** across at least 3 device manufacturers (Samsung, Xiaomi/Redmi, generic AOSP-like) since OEM battery managers are the most common real-world cause of "my alarm didn't go off" bugs — this is the single highest-risk area for the app's core promise.
- **OCR accuracy testing:** build a small test set of real Philippine university CORs/schedule screenshots (with permission) across different formats/fonts; measure field-level accuracy, not just "did it extract something."
- **Usability testing:** have 5-8 real students go through the full onboarding-scan-to-first-alarm flow unassisted, observe where they get stuck.
- **Offline testing:** airplane mode through the entire Tier 1 flow.

---

## 17. Success Metrics

Avoid vanity metrics like raw downloads. Track instead:
- Schedule scan → confirmed calendar completion rate (did the OCR flow actually finish successfully)
- % of fields the student had to manually correct after a scan (OCR quality proxy)
- Alarm fire-and-dismiss rate vs. missed rate
- 7-day and 30-day retention (does the student still have the app installed and getting alarms a month into the semester)
- Self-reported: "did this help you make it to class on time" (short in-app survey after 2 weeks)

---

## 18. Competitive Gap

Existing options (Google Calendar, Apple Calendar, MyStudyLife, Notion, Todoist) all handle generic recurring events and reminders competently. None of them are built around the specific two-minute "photo of my COR → alarms tuned to actually get me to class" moment for a Filipino student's typical registration document format. That narrow wedge — not a broader feature set — is the actual opportunity. If this app tries to also be a notes app, task manager, and AI tutor, it stops being differentiated from tools with far more engineering resources behind them (Notion, Google) and starts competing on breadth it can't win.

---

## 19. Dream Version vs. Realistic Version

**Dream (unlimited resources):** full academic OS — schedule, notes, AI tutor, LMS integration, practicum/portfolio tooling for education majors, adaptive study planning, cross-platform, cloud-synced, multi-language.

**Realistic v1 (solo dev, open timeline, no budget):** Android-only, on-device-only, printed-schedule OCR with mandatory human confirmation, class-aware alarms, a calm no-guilt dashboard. That's it. Everything else is earned, not assumed.

---

## 20. Open Questions for Next Round (worth answering before Phase 1 starts)

- Exact format(s) of the schedule documents students will actually scan (screenshot vs. printed COR vs. handwritten) — worth collecting 10-15 real samples before finalizing the OCR parser.
- Whether "no account, fully local" is acceptable long-term given phones get lost/reset, or whether even a minimal opt-in backup should be pulled into Tier 1.
- Tagalog/Bisaya UI language support — not researched yet; worth deciding before building strings, since retrofitting localization is more expensive than designing for it from the start.
