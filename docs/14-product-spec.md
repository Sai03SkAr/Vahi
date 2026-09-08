# 14 — Product Specification

> **Status: proposal.** This is the missing product layer. [`11-forward-plan.md`](11-forward-plan.md) says *how to build*; this says **what to build and, more importantly, what not to**.
>
> Context assumed: [`22-decisions.md`](22-decisions.md) D1, D3, D5, D10, D16.

---

## Part 1 — The product in one sentence

> **Vahi is the notebook app that feels best to write on with an Android tablet — and the only one that can read handwriting in Indian scripts.**

Everything in this document is downstream of that sentence. If a proposed feature does not make writing feel better, make study material more useful, or make handwriting searchable, it is table stakes at best and scope creep at worst.

---

## Part 2 — Who it is for

Not a marketing persona exercise. Three concrete people whose needs conflict, so the conflicts have to be resolved deliberately.

### P1 — Aditi, 2nd-year engineering student *(the primary user; optimise for her)*

- Owns a ₹15–25k Android tablet. **Has a cheap capacitive stylus with no pressure sensitivity, and often just uses her finger.**
- Takes notes in lectures — fast, messy, mixed English and Hindi, with diagrams.
- Downloads question papers and lecture slides as PDFs and annotates them.
- Studies on a train with no signal.
- Storage on her tablet is nearly full.

**What she needs:** open and write in under two seconds · ink that does not lag · her palm not drawing on the page · imported PDFs · nothing lost, ever · small install size.

**What she does not need:** twelve brush types, a laser pointer, an infinite canvas.

### P2 — Rahul, class-11 student *(the volume user)*

- Shares a family tablet. Uses it for one subject at a time.
- Organises by subject and revises from his own notes before exams.
- Wants to find "that thing I wrote about oscillations" three weeks later.

**What he needs:** folders by subject · search that actually finds handwritten content · flashcards/practice from his own notes.

### P3 — Ms. Iyer, teacher *(the secondary user; do not design for her yet)*

- Annotates worksheets, exports as PDF, shares to a class WhatsApp group.

**What she needs:** clean PDF export · share sheet.
**Explicitly out of scope for v1:** classroom features, multi-user, grading.

### The conflict, resolved

Aditi wants speed and simplicity; Rahul wants organisation and retrieval. **Resolution: default to Aditi.** The app opens fast and writes fast with zero configuration; organisation and search are there when you look for them but never in the way. A first-run user should never see a folder picker.

---

## Part 3 — Jobs to be done

Ordered by frequency. Frequency is the ranking that matters — optimising a rare job at the cost of a frequent one is the classic feature-list mistake.

| # | Job | Frequency | Current cost in Vahi | Target cost |
|---|---|---|---|---|
| J1 | "Write something down right now" | Many times daily | Splash video + 3 taps, lands on a *new* blank page every time | **≤ 1 tap, ≤ 2 s, lands where I left off** |
| J2 | "Get back to the notebook I was in" | Many times daily | **Impossible** — nothing persists | 1 tap from the library's "Jump back in" row |
| J3 | "Fix what I just wrote" | Constant, mid-writing | Undo works; erase is bounding-box and unreliable | Undo ≤ 100 ms; erase hits only what it visibly touches |
| J4 | "Switch pen colour / thickness" | Many times per session | 1 tap (toolbar shipped in Session 4) | Keep at 1 tap. Do **not** regress to picker dialogs. |
| J5 | "Annotate this PDF I downloaded" | Weekly | Not implemented | Import → annotate → export, no format juggling |
| J6 | "Find where I wrote about X" | Weekly, spikes before exams | Not implemented | Search titles **and handwritten content**, incl. Hindi |
| J7 | "Organise by subject" | Occasionally | Not implemented | Folders, drag or menu-move |
| J8 | "Get my notes onto another device / not lose them" | Rare, but catastrophic when needed | Not implemented | Export now; sync in Track F |
| J9 | "Turn my notes into something I can revise from" | Before exams | Not implemented | Flashcards / practice questions from recognised text |

**J1 and J2 are the whole product.** If those two are not excellent, nothing below them matters.

---

## Part 4 — Scope: v1

**v1 is defined by one claim: *Vahi is a notebook you can trust and enjoy writing in.*** It does not need to be differentiated yet — it needs to be *good*. Differentiation is v1.1.

### In scope for v1

| Area | Included |
|---|---|
| **Writing** | Pen (colour, width, pressure where available), highlighter, eraser, undo/redo, pan/zoom, finger and stylus input, palm rejection |
| **Documents** | Paged A4 notebooks · blank / ruled / grid paper · add, insert, delete, reorder pages · vertical continuous scroll |
| **Library** | Create, open, rename, delete (with undo), pin, duplicate · page-1 thumbnails · "Jump back in" row · grid & list views · sort · title search |
| **Persistence** | Everything auto-saved. Nothing lost on rotation, backgrounding, or process death. |
| **Export** | Whole notebook or page range → PDF · single page → PNG · system share sheet |
| **Settings** | App theme · paper theme · default paper · **handedness** · stylus & gestures · language · storage · about |
| **Platform** | Tablet (Expanded) · light + dark · English + Hindi UI · TalkBack-navigable · 200 % font scale survivable |

### Explicitly out of scope for v1 — and why

Stating these as decisions, not omissions, is what stops them creeping back in.

| Not in v1 | Why |
|---|---|
| Accounts, sign-in, cloud sync | Needs a stable local model first; nothing in v1 requires an account ([D10](22-decisions.md#d10--no-auth-wall-account-is-optional-and-requested-in-context), [D15](22-decisions.md#d15--offline-first-the-local-database-is-the-source-of-truth)) |
| PDF import & annotation | The single biggest v1.1 feature. Deferred because `androidx.pdf` is beta and it forces `minSdk 28` ([D8](22-decisions.md#d8--pdf-annotation-builds-on-androidxpdf--androidxink), [D12](22-decisions.md#d12--minsdk-stays-at-26-until-pdf-forces-28)). Ship a trustworthy notebook first. |
| Handwriting recognition & content search | The differentiator — and it deserves a stable ink model beneath it. v1.1. |
| AI study features | Depends on recognition. v1.2. |
| Folders | Needed once a user has ~15 notebooks. That is week 3, not day 1. v1.1. |
| Shapes, text boxes, images, lasso select | Real, but every one rests on the ink foundation. Add after it is proven. |
| Paid tier / paywall | Nothing to sell until sync exists ([D16](22-decisions.md#d16--freemium-with-a-generous-free-tier-no-page-caps)) |
| Phone layouts | [D2](22-decisions.md#d2--tablet-only-for-v1) |
| Notebook locking | Implies real encryption and a credential story. Do it properly later or not at all — a fake lock is worse than none. |
| Sharing a notebook with another user | Needs a backend. |
| Infinite canvas | [D1](22-decisions.md#d1--paged-documents-not-infinite-canvas) |
| Laser pointer, presentation mode | Not a student job. |

### The v1 exit criteria

v1 ships when **all** of these are true. Not "mostly".

1. Draw → background the app → force-stop it → reopen → **the ink is exactly where it was**, on the right page, at the right zoom.
2. Rotate the device mid-stroke. Nothing is lost, nothing is corrupted.
3. Rest a palm on the screen while writing with a stylus. **The palm leaves no mark.**
4. Write a full page of dense notes on the mid-range reference device. Frame rate stays within budget ([`19-quality-bar.md`](19-quality-bar.md) §2.2).
5. Ink latency on the reference device is within budget and has been **measured and written down**.
6. Export a 20-page notebook to PDF. It opens correctly in Drive/Adobe and the geometry matches the screen.
7. Every screen has a loading, empty and error state. No control is a no-op.
8. TalkBack can complete: create notebook → write → go back → delete → undo the delete.
9. Full UI in Hindi. No clipped text at 200 % font scale.
10. Release build: minified, shrunk, signed, crash-reporting live, privacy policy published, Data Safety form submitted.

---

## Part 5 — Roadmap by release

Sizes are **engineering-days** ([D7](22-decisions.md#d7--schedule-expressed-in-engineering-days-not-calendar-dates)). Item-level breakdown is in [`21-backlog.md`](21-backlog.md).

| Release | Theme | Headline content | Size |
|---|---|---|---|
| **v0.9 — Foundation** *(internal)* | "It cannot lose your work" | Ink engine · coordinate model · navigation + ViewModels · Room · command history | ~40 ed |
| **v1.0 — The Notebook** | "A notebook you trust and enjoy" | Pages, pan/zoom, tools, library CRUD, export, settings, a11y, Hindi, release hardening | ~35 ed |
| **v1.1 — Study Material** | "Bring your textbooks in" | PDF import & annotation · folders · multi-select | ~20 ed |
| **v1.2 — The Differentiator** | "Search your handwriting" | ML Kit Indic recognition · content search · language packs | ~15 ed |
| **v1.3 — Study Tools** | "Revise from your own notes" | Flashcards · practice questions · explain-this-passage | ~15 ed |
| **v1.5 — All Android devices** | "On the tablet *and* the phone" | Responsive layouts for phone/foldable · bottom-bar editor on Compact · sheets instead of popovers | ~14 ed |
| **v2.0 — Multi-device** | "Everywhere" | Accounts · Google Sign-In · sync with conflict resolution · Plus tier | ~35 ed |
| **v2.x — iOS** | "iPad and iPhone too" | KMP restructuring · Koin · **a second ink engine on PencilKit** · ML Kit iOS binding · App Store pipeline | ~45 ed |

> **v1.5 and v2.x are the cross-platform expansion, planned in full in [`23-cross-platform-plan.md`](23-cross-platform-plan.md) and gated on v1 shipping ([D17](22-decisions.md#d17--android-tablets-for-v1-all-devices-as-a-planned-post-v1-phase)).** Note the ordering: **all-Android-devices comes before iOS**, because it's ~25% of the cost, likely reaches more of the target student (most tablet owners also own a phone), and produces the responsive layouts iPhone would otherwise need built twice. iOS is placed after sync deliberately — "works on my iPad" that shows an empty, unrelated library isn't what a user means by multi-device.

**Note the shape.** The differentiator does not ship until v1.2. That is deliberate: a differentiator built on a shaky notebook is a demo. But it is only ~4 releases out, and everything before it is load-bearing for it.

---

## Part 6 — Monetisation

Per [D16](22-decisions.md#d16--freemium-with-a-generous-free-tier-no-page-caps). **No paywall ships before v2.0.**

| | **Vahi Free** | **Vahi Plus** |
|---|---|---|
| Notebooks | **Unlimited** | Unlimited |
| Pages per notebook | **Unlimited** | Unlimited |
| All drawing tools | ✅ | ✅ |
| PDF import & annotation | ✅ | ✅ |
| Handwriting recognition | **1 language** | Unlimited languages |
| Export to PDF | ✅ (standard resolution) | High resolution, batch export |
| Cloud sync & backup | — | ✅ |
| Multi-device | — | ✅ |
| AI study tools | Limited daily quota | Unlimited |
| Ads | **Never** | Never |

**The rules behind this table**

1. **Never cap the user's own content.** No notebook limit, no page limit, no stroke limit. The moment a student cannot add a page to their Physics notes, you have taken their work hostage and they will not forgive it.
2. **Charge for what costs you money.** Server storage, sync bandwidth, and any cloud AI inference are real recurring costs. On-device recognition is not — so gate it by *breadth* (languages) rather than by *use*.
3. **No ads, ever.** An app you write your exam notes in cannot be an ad surface. This is a positioning decision as much as an ethical one.
4. **Price for the market.** Anchor to local subscription norms, not USD conversions. Offer an annual price with a real discount and a student verification path if it is cheap to run.

**UI consequences (do these in v1, before there is anything to sell):**
- Remove the permanent "Upgrade" pill from the top bar.
- Keep exactly one upgrade surface: Settings → Vahi Plus.
- Contextual prompts only where the value is obvious ("Back up your notes to the cloud →").

---

## Part 7 — What success looks like

Measurable, and honest about what is not measurable yet.

| Metric | Target | How measured |
|---|---|---|
| **Time-to-ink** (launch → first mark, warm start) | **< 2 s, ≤ 1 tap** | Instrumented trace on the reference device |
| **Ink latency** (touch → pixel) | Within budget, [`19-quality-bar.md`](19-quality-bar.md) §2.1 | High-speed camera or the platform latency tool |
| **Crash-free sessions** | **> 99.5 %** | Crash reporting ([`20-release-plan.md`](20-release-plan.md) §5) |
| **Data-loss reports** | **Zero.** Any single confirmed report is a P0. | Support channel + crash reports |
| **Day-7 retention** | > 25 % | Play Console |
| **Notebooks per retained user at day 30** | > 3 | Local analytics (opt-in) |
| **Recognition accuracy, Devanagari** | Usable enough that search finds the note | Manual corpus test — *not* automatable pre-v1.2 |
| **Install size** | Within budget, [`19-quality-bar.md`](19-quality-bar.md) §2.4 | Play Console app size report |

**Deliberately not measured in v1:** revenue (there is none), DAU (vanity at this stage), session length (longer is not better for a tool).

---

## Part 8 — Competitive position

| App | Its strength | The gap Vahi aims at |
|---|---|---|
| **GoodNotes** | The category leader; excellent feel | iPad-first. Android build is a port, and it shows on mid-range hardware. |
| **Notability** | Audio sync, clean UX | Very iPad-centric. |
| **Samsung Notes** | Superb on Galaxy Tab; free | **Samsung devices only.** Every other Android tablet is unserved. |
| **Nebo** | Best-in-class handwriting → text | Latin/European scripts. Paid. Indic support is not the focus. |
| **Xodo / Adobe** | PDF-first, strong annotation | PDF readers with ink bolted on, not notebooks. |
| **OneNote** | Free, syncs everywhere | Not handwriting-first; the page model is unlike paper; heavy. |

**The honest read.** Vahi cannot out-feature GoodNotes and should not try. It can be *the best notebook on a non-Samsung Android tablet*, and it can be *the only one that reads Hindi handwriting*. Those two claims are narrow, true and defensible. Everything in this document is arranged to make them true.

**The failure mode to avoid, stated plainly:** shipping twenty features at 60 % quality and being a worse GoodNotes. Eight features at 95 % plus one nobody else has is a product.

---

## Part 9 — Open product questions

| # | Question | Blocks | Owner |
|---|---|---|---|
| Q1 | Keep the splash video as a brand asset, or drop Media3 entirely? ([D11](22-decisions.md#d11--drop-the-video-splash-and-the-media3-dependency)) | Splash rework — small | Project owner |
| Q2 | Is the freemium shape in Part 6 right, or is this a one-time purchase / education-licensing product? ([D16](22-decisions.md#d16--freemium-with-a-generous-free-tier-no-page-caps)) | Nothing until v2.0 | Project owner |
| Q3 | ~~Is there an existing Figma?~~ **Answered:** yes — [`vahi-v1`](https://www.figma.com/design/JJAGOm1by1gHE6Nxe4yUhx/vahi-v1?node-id=0-1). Still needs reading and reconciling ([V4](22-decisions.md#v4--figma-is-the-visual-source-of-truth-the-docs-describe-behaviour), `VAHI-004`) | Significant visual build-out | Project owner |
| Q4 | Which Indian language ships **second**, after Hindi? | Localisation work in v1.1 | Owner + usage data |
| Q5 | Is teacher/classroom use a real future direction, or a distraction? | Nothing in v1; would reshape v2+ | Project owner |
