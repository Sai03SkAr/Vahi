# 20 — Release Plan and Operations

> **Status: proposal.** Everything between "the app works on my tablet" and "the app is on the Play Store and I can safely ship an update".
>
> Depends on [D5](22-decisions.md#d5--a-real-product-not-a-portfolio-prototype) — Vahi is a real product, so this is mandatory work, not optional polish.

---

## Part 1 — The current state, honestly

| Item | State |
|---|---|
| Release build | `isMinifyEnabled = false`, no resource shrinking, **no ProGuard rules** |
| Signing | **None.** No keystore, no signing config |
| Versioning | `versionCode = 1`, `versionName = "1.0"` — hardcoded, never incremented |
| Permissions | **Zero declared.** Correct today; changes with sync and PDF import |
| Crash reporting | None |
| Analytics | None |
| Privacy policy | None |
| Play listing | Nothing |
| Backup config | **Templates with every rule commented out** — a live hazard ([D13](22-decisions.md#d13--android-auto-backup-is-disabled-for-notebook-data)) |
| Git | **One commit**, with hundreds of uncommitted lines on top |

None of this blocks development. **All of it blocks release**, and several items take real calendar time (Play Console review, policy declarations) that is easy to underestimate.

---

## Part 2 — Version control hygiene

**Fix this first — before foundation work, not after.** A single mistaken `git checkout` or `git clean` right now destroys the entire working state of the project, because almost none of it is committed.

1. Commit the working tree in **logical chunks**: the drawing engine · the launcher-icon change · the screen rewrites · the toolbar and Session 4 bug fixes · the documentation set.
2. Adopt a branch convention: `main` stays releasable; work happens on `feat/*` and `fix/*`.
3. Tag every release: `v1.0.0`.
4. Add to `.gitignore` if absent: `local.properties`, `*.jks`, `keystore.properties`, `.idea/deploymentTargetSelector.xml`.
5. **Never commit the keystore or its passwords.**

---

## Part 3 — Build configuration

### 3.1 Build types

```kotlin
buildTypes {
    debug {
        applicationIdSuffix = ".debug"      // debug and release installable side by side
        versionNameSuffix = "-debug"
        isMinifyEnabled = false
    }
    release {
        isMinifyEnabled = true              // currently false
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        signingConfig = signingConfigs.getByName("release")
    }
}
```

`applicationIdSuffix` matters more than it looks: without it you cannot keep a working release build installed while developing, and you will end up testing release builds far less than you should.

### 3.2 ProGuard / R8 rules

`proguard-rules.pro` is currently the untouched template with every rule commented out. Enabling minification against that file **will** break things at runtime, in release only, in ways that do not reproduce in debug. Rules needed:

| For | Why |
|---|---|
| Room entities and DAOs | Reflection over generated code |
| `kotlinx.serialization` `@Serializable` classes | Serializers are looked up reflectively |
| `androidx.ink` native bindings | Native ↔ JVM boundary |
| Hilt generated components | Usually covered by the plugin — verify |
| Enums persisted by name | Do not let R8 rename them ([`17-data-spec.md`](17-data-spec.md) §2.5) |
| ML Kit (v1.2) | Vendor-supplied consumer rules — verify they apply |

**Test the minified build properly.** Run the full manual checklist in [`19-quality-bar.md`](19-quality-bar.md) §4.1 against a **release** build before every launch. The classic Android failure is shipping a release that crashes on a code path that works perfectly in debug.

### 3.3 Versioning

Semantic versioning for `versionName`; monotonically increasing `versionCode`.

```
versionName = "MAJOR.MINOR.PATCH"    1.0.0 → 1.1.0 → 1.1.1
versionCode = MAJOR*10000 + MINOR*100 + PATCH     // 1.1.2 → 10102
```

Derive both from a single source in `build.gradle.kts` so they cannot drift. **`versionCode` can never be reused or decreased** — Play rejects the upload, and reconstructing what you shipped afterwards is miserable.

### 3.4 Signing

```kotlin
// keystore.properties — gitignored, never committed
storeFile=/absolute/path/vahi-release.jks
storePassword=…
keyAlias=vahi
keyPassword=…
```

Read it in `build.gradle.kts` and fail the release build loudly if it is missing.

> **Back up the keystore in at least two places.** Losing it means you can never update the app under the same listing — you would have to publish a new one and abandon every existing user. Play App Signing (Google holds the app signing key, you hold the upload key) substantially reduces this risk and should be enabled at first upload.

### 3.5 Output format

Ship an **Android App Bundle (`.aab`)**, not an APK. Per-ABI splitting matters here specifically because `androidx.ink` ships native libraries — a universal APK carries every ABI and inflates the download well past the 25 MB budget ([`19-quality-bar.md`](19-quality-bar.md) §2.4).

---

## Part 4 — Permissions and privacy

### 4.1 Permission timeline

The manifest declares **zero permissions** today, which is correct and worth protecting. Every addition needs a reason.

| Permission | When | Why | Alternative first |
|---|---|---|---|
| *(none)* | v1 | Everything is app-internal storage | — |
| `INTERNET` | v1.2 | ML Kit language-pack downloads | None |
| `INTERNET`, `ACCESS_NETWORK_STATE` | v2 | Sync | None |
| `POST_NOTIFICATIONS` | v2 | Sync status | Only if genuinely useful |
| ~~`READ_EXTERNAL_STORAGE`~~ | **never** | PDF import | **Storage Access Framework** — no permission needed |
| ~~`CAMERA`~~ | never in v1 | Photo insert | `ACTION_IMAGE_CAPTURE` — no permission needed |

**The rule: prefer the system picker over a permission, always.** SAF and `ACTION_IMAGE_CAPTURE` give you the file without asking for anything, and "this app asks for no permissions" is a real trust signal in this market.

### 4.2 Privacy policy — required before publishing

Play requires a privacy policy URL for every app. It must be truthful and specific. Vahi's is unusually easy to write, and that is an asset:

> **What Vahi collects:** nothing, in v1. Your notebooks, handwriting and imported documents stay on your device. Vahi has no account system in v1 and sends nothing to any server.
>
> **Handwriting recognition** (v1.2) runs entirely on your device. Your handwriting is never uploaded.
>
> **Crash reports** (if enabled) contain technical diagnostics — device model, Android version, stack trace. They never contain your notes, notebook titles or handwriting.
>
> **When you sign in** (v2): email address and the notebooks you choose to sync.

Host it at a stable URL. Link it from Settings → About **and** from the Play listing.

### 4.3 Play Data Safety declaration

A separate form from the privacy policy, and it must match it exactly — mismatches get apps removed.

| Question | v1 answer |
|---|---|
| Does the app collect or share user data? | **No** |
| Is data encrypted in transit? | N/A (no transmission) |
| Can users request deletion? | Yes — uninstall, or delete in-app |
| Ads / analytics SDKs? | **None** |

**When crash reporting is added, the answer changes to "yes — Crash logs, Diagnostics".** Update the form in the same release, not later.

### 4.4 Children and content ratings

Students include minors. Complete the IARC questionnaire honestly and consider **Play Families policy** implications:
- No ads ([D16](22-decisions.md#d16--freemium-with-a-generous-free-tier-no-page-caps)) — already compliant, keep it that way.
- No third-party analytics SDK in v1.
- If Vahi ever targets under-13s explicitly, Families policy adds real constraints. Decide deliberately rather than drifting into it.

---

## Part 5 — Crash reporting and analytics

**Crash reporting: yes, from the first public build.** Without it you learn about crashes from one-star reviews.

| Option | Trade-off |
|---|---|
| **Firebase Crashlytics** | Free, excellent, standard. Pulls in Firebase and changes the Data Safety answer. |
| **Sentry** | Good, self-hostable, less Google coupling. |
| **Play Console vitals only** | Free, zero dependency, **but** no custom logging and sparse detail. Insufficient alone. |

**Recommendation: Crashlytics**, with two hard rules:
1. **Never log note content, notebook titles, or handwriting.** Log the *operation* ("save page", "import PDF"), never the data.
2. Add a **crash-reporting opt-out** in Settings → About. In a privacy-forward product, that is consistent rather than merely nice.

**Analytics: minimal and opt-in.** Track only the handful of things in [`14-product-spec.md`](14-product-spec.md) §7 — time-to-ink, notebooks created, crash-free rate. Not screen views, not session length, not funnels. There is nothing to optimise a funnel for yet, and a note-taking app that watches you is a contradiction.

---

## Part 6 — Backup and data safety for the user

Per [D13](22-decisions.md#d13--android-auto-backup-is-disabled-for-notebook-data), Android Auto Backup is **excluded** for notebook data. That leaves a gap the user must be told about honestly, and given a tool for.

| Release | The promise |
|---|---|
| v1 | "Your notes live on this tablet." Export to PDF or `.vahi` is the backup path. Say so in onboarding and in Settings → Storage. **Do not imply cloud backup that does not exist.** |
| v2 | Real sync, with conflict resolution |

Ship in v1: export a single notebook, export all notebooks, and a Settings → Storage screen showing where notes live and how to back them up. A student who loses a semester of notes because the app implied a backup it never had is the worst outcome this project can produce.

---

## Part 7 — Play Store listing

| Asset | Requirement | Notes |
|---|---|---|
| App name | 30 chars | "Vahi — Digital Notebook" |
| Short description | 80 chars | Lead with the differentiator |
| Full description | 4000 chars | English **and** Hindi |
| Icon | 512×512 PNG | Exists; verify it reads at small sizes |
| Feature graphic | 1024×500 | Needed |
| **Tablet screenshots** | ≥ 4, 7" and 10" | **Required** to appear well in tablet search |
| Phone screenshots | — | Optional, but see below |
| Category | Productivity | |
| Content rating | IARC questionnaire | |
| Target audience | 13+ | |
| Privacy policy URL | Required | |

**The tablet-only problem** ([D2](22-decisions.md#d2--tablet-only-for-v1)). Play will still show Vahi to phone users, who will install it, hit the "designed for tablets" screen and leave a one-star review. Mitigations:
1. `<uses-feature android:name="android.hardware.type.pen" android:required="false" />` and screen-size hints so Play prefers tablets.
2. **Say "Designed for tablets" in the first line of the short description.**
3. Use only tablet screenshots.
4. Consider `<supports-screens>` / `<uses-feature android:name="android.software.leanback" ... >` filtering only if reviews show it is a real problem — filtering too aggressively is worse than a few bad reviews.

---

## Part 8 — Release process

### 8.1 Pre-release checklist

**Build**
- [ ] `versionCode` incremented, `versionName` updated
- [ ] Release build minified, shrunk, signed
- [ ] AAB, not APK; per-ABI size within budget
- [ ] `applicationId` has no `.debug` suffix
- [ ] `isDebuggable = false`; no logging of user content

**Verify — on a release build, on real hardware**
- [ ] Full manual checklist ([`19-quality-bar.md`](19-quality-bar.md) §4.1) on T1 and T2
- [ ] Performance budgets re-measured ([`19-quality-bar.md`](19-quality-bar.md) §2)
- [ ] Fresh install → onboarding → create → write → close → reopen → **ink is there**
- [ ] Upgrade install over the previous version → **no data loss**
- [ ] TalkBack, 200 % font scale, dark mode, Hindi

**Store**
- [ ] Data Safety form matches the privacy policy
- [ ] Privacy policy URL live
- [ ] Screenshots current
- [ ] Release notes written, English + Hindi

**Docs** — [`00-START-HERE.md`](00-START-HERE.md) §7
- [ ] `PROJECT-LOG.md` release entry
- [ ] `21-backlog.md` items closed
- [ ] Version tagged in git

### 8.2 Rollout

Never ship 100 % on day one of a data-holding app.

```
Internal testing  →  Closed (10–20 real students, real tablets)  →
Open beta         →  Production 5 %  →  20 %  →  50 %  →  100 %
                     ↑ hold ≥ 48 h at each stage, watching vitals
```

**Halt and roll back immediately if:** crash-free sessions drop below 99 %, ANR rate rises, or **any** report of lost notes appears. The last one is a P0 regardless of volume — one confirmed data-loss report is worse than a hundred cosmetic bugs.

**The closed test with real students on real tablets is the highest-value step here.** It is the only way to find out how palm rejection behaves on hardware you do not own, and it is exactly the risk (R5) the plan flags as high.

### 8.3 Post-release

- Watch Play vitals daily for the first week.
- Respond to reviews — especially one-star reviews from phone users, where a polite "Vahi is built for tablets" reply helps the next reader.
- Keep a `CHANGELOG.md` from v1.0.0.
- Prepare a **hotfix path**: a branch strategy that lets a P0 fix ship without dragging in unreleased work.

---

## Part 9 — Pre-launch blockers

Ordered. Everything here must be true before the first production upload.

| # | Blocker | Size |
|---|---|---|
| 1 | Working tree committed; git hygiene in place | 0.5 ed |
| 2 | Backup rules explicitly configured ([D13](22-decisions.md#d13--android-auto-backup-is-disabled-for-notebook-data)) | 0.25 ed |
| 3 | Release signing config + keystore, backed up twice | 0.5 ed |
| 4 | ProGuard rules; minified release verified end-to-end | 1.5 ed |
| 5 | Crash reporting live, with content-logging rules enforced | 0.5 ed |
| 6 | Privacy policy written and hosted | 0.5 ed |
| 7 | Data Safety form submitted, matching the policy | 0.25 ed |
| 8 | Play listing: screenshots, descriptions, EN + HI | 1 ed |
| 9 | v1 exit criteria all met ([`14-product-spec.md`](14-product-spec.md) §4) | — |
| 10 | Closed test with ≥ 10 real students on real tablets | 3–5 ed elapsed |

**≈ 6 engineering-days plus elapsed testing time.** Budget it. It is the part that always gets underestimated, and skipping it is how a good app ships broken.
