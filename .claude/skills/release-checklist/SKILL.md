---
name: release-checklist
description: >
  Drive the MEGA Android Secure Release Checklist as Release Captain (RC).
  Orchestrates the full release train: create the RC tracking ticket, prepare
  release notes, request SDK/MEGAChat builds, cut the code-freeze/Alpha build,
  run QA rounds, promote through Beta → 25/50/100% Production, and run hotfixes.
  Encodes the flow, gradle/MR commands, and date rules so each step is
  executable, not just described. Internal identifiers (Slack channel /
  user-group IDs, Jira epic + fields, Confluence/TestRail ids) are referenced by
  NAME and resolved from a gitignored `local-constants.md` — never hardcoded
  here. Always defer to the latest Confluence page for the version.
triggers:
  - /release-checklist
  - release captain
  - release checklist
  - start release
  - code freeze
  - promote release
---

# Android Secure Release Checklist (Release Captain)

Orchestration guide for running an Android release end-to-end. The **source of
truth is the Confluence page** for the version:
`Android Secure Release Checklist - v<X.Y>` (space `MOB`). This skill mirrors
that flow and pre-fills the constants RC work needs, but the checklist wins on
any conflict — **always re-read the latest Confluence page; do not rely on memory.**

> 🆕 **At the start of every release (NOT a hotfix): ensure the per-version page exists first.**
> If `Android Secure Release Checklist - v<X.Y>` doesn't exist yet, **clone it from the master
> template** *Android Secure Release Checklist* (`https://confluence.developers.mega.co.nz/display/MOB/Android+Secure+Release+Checklist`)
> and rename the copy with the exact version in the title — use `confluence_copy_page` (copy the
> template page → set the new title to `Android Secure Release Checklist - v<X.Y>`). Then work from
> that copy (e.g. update the Prerequisite release-ticket link on it). Hotfixes reuse the base
> version's page — don't create a new checklist page for a hotfix.

> ⚠️ The checklist order matters. Do not jump between steps. Any ticket
> add/remove after code freeze needs a mobile leader's approval.

## How to use

1. Determine the current state (which section are we in) before doing anything.
2. Work **only the steps due for today** unless told otherwise. The schedule is
   day-anchored (see *Schedule* below).
3. Do the read/analysis yourself; **draft** every outward message (Slack, Jira)
   and **confirm before sending** — these are outward-facing and broad-mention.
4. Track multi-step work with the task tools.

## 🔐 Local constants (resolve IDs by name — never hardcode here)

This repo mirrors **publicly**, so MEGA-internal identifiers are **not** stored
in this file. They live in **`local-constants.md`** (same folder, **gitignored**).
Everywhere below refers to things by name — `#devops-cicd`, `@eu-mobile-release`,
"RC ticket Epic", "Release Plan page id", etc. **At runtime, read
`local-constants.md` and resolve the name to its real value.**

- First use / file missing → copy **`local-constants.example.md`** →
  `local-constants.md` and fill it in (each value's source is noted in the template).
- **Slack pings:** message templates below use the readable `@group` form. When
  sending via the Slack API, expand each `@group` to its `<!subteam^ID>` (ID from
  `local-constants.md`) — plain text `@group` does **not** notify. Post to channels
  by the `C…` id from `local-constants.md`.
- ⚠️ **Channel-search is unreliable** — `slack_search_channels "mobile-dev-team"`
  returned *no results* even though the channel exists. Prefer the id in
  `local-constants.md`; don't conclude a channel is gone just because search misses it.

## Constants (non-sensitive; re-confirm if stale)

| Thing | Value |
|---|---|
| RC ticket title | `Release management - Android <X.Y>` (type **Story**) |
| RC ticket required fields | Expense Product = `Cloud/Default`; Expense Type = `OPEX`; Epic Link = RC ticket Epic (field ids + Epic in `local-constants.md`) |
| Cadence | Every 2 weeks; **code freeze = Wednesday** |
| Jira projects in a release | AND, AP, BAC, CC, CU, MEET, TRAN, SHR, FM, SAT, SAO |

### Slack channels / user groups — role reference (IDs in `local-constants.md`)
| Handle | Used for |
|---|---|
| `#mobile-platform` | release-notes request (1.1); SDK Release Train is announced here |
| `#mobile-dev-team` | **code-freeze 30-min warning + "freeze started" (1.4)**; crash/ANR monitoring reminders |
| `#android-dev-team` | feature-flag request (1.3); next-RC reminder; QA Failed/Feedback reports (1.5) |
| `#sdk` | post target version in Release Train thread (1.3); SDK RC announcements |
| `#sdk-android-pipeline` | **prebuilt-SDK build results** (`build sdk-aar`) post here — watch this to get the `…-rel` version without a GitLab token; each post names its source MR + SDK/chat branch |
| `#megachat_native` | MEGAChat version announcements (1.3) |
| `#devops-cicd` | ask DevOps to promote Beta/Production (2.x) |
| `#android`, `#release`, `#app_release_updates` | build reports / release thread / release announcements |
| `@productmanagers` (user group) | release-notes highlights |
| `@androiddevs-urgent` (user group) | dev-facing freeze / feature-flag pings |
| `@eu-mobile-release` (user group) | DevOps promotion approvers (Beta/Prod) — always ping |
| `@nz-mobile-release` (user group) | DevOps promotion approvers — ping **only** 7am–7pm NZ time |

### Date rules
- **"Previous Friday"** = the Friday of the week *before* code-freeze week
  (code freeze is the following Wednesday). This is release-notes prep day (1.1).
- **Next code freeze** = current code freeze **+ 14 days**.
- **Next-RC reminder** fires the Friday before the *next* code-freeze week
  (i.e. next-freeze-Wednesday − 5 days).
- Get the version's code-freeze date from Jira: it's the version **`startDate`**
  for `Android <X.Y>` (`jira_get_project_versions` on `AND`).

## Version type
- `X.Y` where `Y != 0` → **minor**. Skip the major-only steps (Jira version
  bumps across all projects + the major-version `#android` announcement).
- `X.0` → **major**. Do the major-only steps and prepare real release notes.
- If unsure, check the latest Release Train announcement in `#mobile-platform`.

---

## Prerequisite — create the RC tracking ticket

Create once at the start of being RC. Log **all** RC time against it.

- Search the **RC ticket Epic** (see `local-constants.md`) children for an existing
  `Release management - Android <X.Y>`.
  (Server uses *Epic Link*, not `parent`: JQL `"Epic Link" = <RC-Epic>`.)
- If none, create it (mirror the most recent one):
  - project `AND`, type `Story`, assignee = you,
  - `additional_fields`: set `epic_link` = RC ticket Epic, the Expense Product field
    = `{"value":"Cloud/Default"}`, and the Expense Type field = `{"value":"OPEX"}`
    (Epic + both `customfield_*` ids in `local-constants.md`).
  - then transition with the **Start Progress** id (in `local-constants.md`) → **In Progress**.
- **Confluence:** the per-version page is a copy of the template; update the
  placeholder release-ticket link in the *Prerequisite* step to the new ticket.
  Note: the Confluence integration only does **whole-page replace**
  (`confluence_update_page`) — there is no line/section edit, and these pages are
  ~130k chars, so this single link swap is best done **manually** in the UI
  (or recorded via `confluence_add_comment`).

---

## Schedule (Code Freeze & Alpha)

### 1.1 Release-notes prep — **previous Friday**
- *(major only)* Bump the Jira version number across all 11 projects; announce
  the major release plan in `#android`.
- Draft + post the release-notes request in `#mobile-platform`, tagging both
  user groups, deadline **Monday**:
  > Hi team (`@productmanagers` & `@androiddevs-urgent`), we are preparing release notes for the upcoming Android release v<X.Y> on <code-freeze date>. If you have highlight features to announce to our end users in your responsible area, please reply in this thread at the latest on Monday. Thank you.
- Review tickets in `fixVersion = "Android <X.Y>"` for highlight features;
  confirm with devs whether to mention them. No highlights → default notes
  (`- Bug fixes and performance improvements`).
- Set the next-RC reminder. **This MUST be typed by the RC in their own Slack
  client** — there is no automation for it:
  > `/remind #android-dev-team that @<next-RC> please start requesting release notes for next release at 9am on <Friday before next code-freeze week>.`
  - Fill in the **actual next RC** (e.g. `@kg`) and the computed date, not a placeholder.
  - ⛔ **Do NOT** send the `/remind` text via the Slack API and **do NOT** substitute
    `slack_schedule_message`. A slash command sent through the API posts as literal
    text in the channel and creates **no** reminder; only typing `/remind` in the
    Slack message box registers it. Just hand the RC the ready-to-paste command above.

### 1.2 Prepare code freeze — **Monday**
- Add the new version row to `Android Release plan - <year>` (Confluence,
  Release Plan page id in `local-constants.md`): fill **Version** + **Package** link only.
  Leave Code Freeze Date (filled Wed, step 2321), Release Date, and — unless
  confirmed — Release Captain blank. Newest row goes at the **top** of the data
  table (above the previous version).
  - Package link format: `…/issues/?jql=fixVersion %3D "Android <X.Y>" ORDER BY priority DESC, updated DESC` (URL-encoded).
  - **This page is small (~12 KB)** so it CAN be updated via `confluence_update_page`
    with `content_format='storage'`: fetch raw storage, insert one `<tr>…</tr>`
    above the latest row, push it back, then re-fetch and confirm no historical row
    changed. Copy RC `ri:user` keys verbatim from the existing rows / the schedule
    table — never hand-type them. (Contrast: the checklist page is ~130 KB → manual.)

### 1.3 Request SDK & MEGAChat — **Tuesday**
- In `#sdk`, reply in the Release Train thread with the target Android version + Jira package link.
  > Hi team, <jira-package-link|Android <X.Y>> will be our next release. Thanks!
  - ⚠️ **The Release Train thread rotates every cycle.** A fresh Slackbot reminder
    ("A new Release Train is coming :steam_locomotive:… post the target version in
    the thread") is posted (~Mon/Tue) as a **new** thread parent. Reply to the
    **newest** reminder, NOT the long-running prior thread — the previous cycle's
    thread keeps getting SDK-bot build/hotfix posts (e.g. last cycle's `v10.15.0a`
    targeting only the *previous* app versions), so it looks active but is stale.
  - Find it: search `#sdk` for the Slackbot reminder, sort by
    timestamp, take the latest; confirm its parent has no/our-cycle replies before posting.
- Watch `#sdk` and `#megachat_native` for the new SDK / MEGAChat releases **targeted
  at our version**. A published SDK that lists only other apps under "Target apps"
  (e.g. iOS only) is NOT ours — the Android-targeted build follows after we post.
  You need both the SDK and MEGAChat tags for `preRelease --sdk … --chat …` (1.4).
- Verify all tickets are `QA`/`Resolved`, and none are missing `fixVersion`
  (JQL: `project in (AND,CC,TRAN,MEET,AP,BAC,CU,SHR,FM,SAO,SAT) AND status in (Resolved,QA) AND fixVersion = EMPTY AND assignee in (membersOf(android))`).
  - Also confirm none of `fixVersion = "Android <X.Y>"` are still un-QA'd:
    `… AND status not in (Resolved, QA, Closed)` should return 0.
- Collect remote feature flags to enable — post in `#android-dev-team`:
  > Hi @androiddevs-urgent, if your feature is ready and you are going to enable your remote feature flag in this release *v<X.Y>*, please reply to this message. Thank you.

### 1.4 Create the code-freeze build — **Wednesday**

**Get the SDK + MEGAChat tags first.** From the `#sdk` and `#megachat_native`
bot posts ("New SDK version → `vX`", "New MEGAchat version → `vY`"), take the rc
tags whose **"Target apps"** lists **Android `<X.Y>`** (ignore posts targeting only
iOS/MEGAsync). e.g. SDK `v10.16.0-rc.1`, MEGAChat `v9.3.7-rc.1`.

**Pre-flight — get a clean `develop` with the rc tags reachable:**
1. `git checkout develop && git pull --ff-only`.
2. **Apply step 2669 (GitLab redirect) — mandatory, not optional.** The submodules'
   `origin` is **GitHub**, but brand-new develop commits + the rc tags live only on
   **GitLab** (GitHub mirror lags), so a plain `submodule update` fails with
   `fatal: ... not our ref <sha>`. Run the `git config --file=.gitmodules …` block
   (both submodules → GitLab `git@code.developers.mega.co.nz:…`, branch `develop`),
   then `git submodule sync && git submodule update --init --recursive --remote`.
3. Verify the rc tags resolve: `git -C sdk/src/main/jni/mega/sdk tag -l <SDK_TAG>`
   and the megachat one; both submodule `origin` should now be GitLab.

⚠️ **Beware `git add -A` + branch switches eating untracked files.** `preRelease`
runs `git add -A`; later `git checkout -B develop …` can drop untracked files (e.g.
this skill folder) — they may end up in a `stash`/commit. If something vanishes,
recover with `git log --all -- <path>` / `git show <sha>:<path>`, don't rewrite from memory.

- Upload approved release notes to WebLate (skip if default notes). Use the
  `weblate` skill. Get content team approval in `#android`.
- **preRelease:** `./gradlew preRelease --rv "<X.Y>" --sdk "<SDK_TAG>" --chat "<CHAT_TAG>"`
  (~3 min). Creates branch `task/pre-release/v<X.Y>` + MR, **already assigned to you**,
  **squash ON** (correct for the pre-release MR), 3 commits: Update SDKs / Update App
  version / Update strings. The bot's **"Code Review Failed — Diff too large"** note is
  expected (translations bloat the diff) and **non-blocking**.
- **Pre-built SDK (MR comment):** `build sdk-aar --lib-type=rel --sdk-branch=<SDK_TAG> --chat-branch=<CHAT_TAG>`.
  The result does **NOT** reply on the MR — it posts (~15–60 min; ~13 min observed) to
  **#sdk-android-pipeline** as `:rocket: Prebuilt SDK is published to Artifactory
  Successfully!` with `SDK Branch`, `Chat SDK Branch`, `Version: <YYYYMMDD.HHMMSS-rel>`, an AAR
  download link, and `Triggered from: <this MR>` (match the MR to be sure it's your build). Set
  `extra["megaSdkVersion"]` in root `build.gradle.kts` to that `-rel` value (replacing
  the prior `-dev`/`-rel`), commit **only `build.gradle.kts`** as "Update prebuilt SDK version", push.
- **Next Jira version (MR comment):** `create_jira_version -rv "<NEXT_X.Y>" -rd "<NEXT_FREEZE_DATE>"`
  — the version **2 weeks out** (e.g. 16.9 → 16.10, freeze +14d). Bot replies
  "Create Jira Version succeeded"; verify in AND (`jira_get_project_versions`).
- Add reviewers; **needs 2 approvals**, then **merge the pre-release MR → `develop`** (squash).
- **Re-sync local develop after the squash-merge (gotcha):** preRelease leaves a
  local-only "Update SDKs" commit on `develop`, but the squash-merge created ONE new
  commit on `origin/develop` → they diverge and `git pull --ff-only` aborts. The
  destructive-git guard blocks `git reset --hard`, so realign with
  **`git checkout -B develop origin/develop`** (tree must be clean) then
  `git submodule update --init --recursive`. **Verify `appVersion`=`<X.Y>` and
  `megaSdkVersion`=the `-rel` build before continuing** — otherwise `release` cuts from the old version.
- **2321 Code Freeze Date** in the Release Plan: set the **actual** cut date (today),
  not the scheduled one (precedent: 16.8 recorded 06-11 vs its 06-10 schedule). The new
  row's two empty cells `<td><br/></td><td><br/></td>` are a unique anchor for the edit.
- **30-min warning** → post to **`#mobile-dev-team`**, ping `@androiddevs-urgent`:
  > Hi @androiddevs-urgent, I'm about to code freeze *v<X.Y>* in 30 minutes. Let me know if I need to wait for some of your MRs to be included in this release. Thanks.
  Wait ~30 min (watch the thread for "wait for my MR" replies; post-freeze adds need a TL OK).
  (NOT #android-dev-team — code-freeze messages live in #mobile-dev-team.)
- ⚠️ **ALWAYS `git pull --ff-only` develop immediately before cutting (step 2457).**
  Commits merge during the 30-min window, so the develop you synced earlier is stale —
  the release branch must be cut from the *latest* develop tip. Re-verify `appVersion`
  after pulling. (`release` cuts the branch from current `develop` HEAD.)
- **Cut release:** `./gradlew release --rv "<X.Y>"` — the gradle task does its own
  `git checkout develop && git fetch && git pull`, then `git checkout -b release/v<X.Y>`
  and pushes with the MR options set automatically: **target=`master`, squash=false,
  label=`WIP`** (so "untick squash / add WIP / MR→master" is handled for you — no
  manual step). **Do not pre-create `release/v<X.Y>` by hand** — a stray local branch
  collides; if one exists, `git checkout develop && git branch -D release/v<X.Y>` first
  (safe when it equals `origin/develop`). ⚠️ never rebase this MR with develop.
  Confirm afterward: `origin/release/v<X.Y>` exists and the MR (→ `master`) is open.
  - 🛡️ Protect the untracked skill folder from the task's `git add -A`: add it to
    **`.git/info/exclude`** (`printf '.claude/skills/release-checklist/\n' >> .git/info/exclude`).
    Local-only, never committed; prevents the sweep that otherwise lands it in a commit/stash.
- On the **release MR**, in order. ⚠️ **Space MR bot commands — never post them
  back-to-back.** After posting a command, **wait for its command result (the bot's reply
  on the MR) to appear**, then post the next; ≥1 min minimum if you're not watching for the
  result. Commands posted within ~1s get dropped by the bot (that's why a
  `send_code_freeze_reminder` posted ~1s before `update_analytics_dependency` silently did
  nothing). Note: a command **result showing up ≠ the command's work finished** (e.g. the
  build/commit may still be running) — the result is just the bot's acknowledgement that
  it picked the command up. Applies to all MR commands (`build sdk-aar`, `create_jira_version`, etc.).
  1. `send_code_freeze_reminder --current-version "<X.Y>" --next-version "<NEXT_X.Y>"` — posts the "freeze started, use <NEXT> now" notice (via the *Release announcement* bot) to #mobile-dev-team.
  2. `update_analytics_dependency` — bumps analytics to the latest tag and **commits "Update analytics dependency" to the release branch** (takes a few min).
  3. **Wait for that commit to land**, then `deliver_appStore` so the Alpha includes it.
     ⚠️ Use **plain `deliver_appStore`** (no `--rollout` flag) — it auto-rolls-out to the
     Alpha track so QA gets the build immediately. Do **not** pass `--rollout 0` here: that
     uploads without rolling out and forces a manual rollout step. The Alpha
     build (~20 min) posts a report to the MR + `#android` + `#qa`.

  **Confirming each command landed.** Simplest if you have MR access: all three post an
  `appdev`-bot ack note on the MR after a few-min delay (`Code freeze remind message sent
  successfully!` / `added 1 commit … Update analytics dependency` / `:runner: … pipeline has
  started`), so just watch the MR notes. Token-free fallbacks (no GitLab access), each reporting
  in a *different* place:
  - `send_code_freeze_reminder` → posts to **both** the MR and Slack, but is **not instant —
    allow ~4-5 min** (don't assume it failed or re-post if it shows nothing at first). Confirm
    via *either*: (a) the MR note from the `appdev` bot `:white_check_mark: Code freeze remind
    message sent successfully!`, or (b) the *Release announcement* bot (id in `local-constants.md`) message in
    **#mobile-dev-team**:
    > Hi @androiddevs-urgent, we have started the Code Freeze for version `Android <X.Y>` in project `MEGA`. Any tickets merged to develop should now use the next Fix Version `Android <NEXT_X.Y>`. Thanks!
  - `update_analytics_dependency` → confirm the **commit on the release branch** (not an MR
    reply). Background-poll `origin/release/v<X.Y>` over SSH (no token):
    ```bash
    for i in $(seq 1 30); do
      git fetch origin release/v<X.Y> -q 2>&1 | grep -viE 'submodule|not our ref|warning|unreachable|gc.log' || true
      case "$(git log --oneline -1 origin/release/v<X.Y>)" in
        *"Update analytics dependency"*) echo "LANDED"; exit 0;; esac
      sleep 60
    done
    ```
    (Avoid `/dev/null` redirects — sandbox-blocked. Run `run_in_background: true`.) This
    commit becomes the release HEAD → use its short sha for the Alpha poll below.
  - `deliver_appStore` → Alpha report posts to the MR + `#android` + `#qa`. Token-free,
    background-poll the **public** Artifactory listing for a `<versionCode>_<release-HEAD-short-sha>/`
    folder (sha = the analytics commit from step 2):
    ```bash
    url="https://artifactory.developers.mega.co.nz/artifactory/android-mega/release/v<X.Y>/"
    for i in $(seq 1 30); do
      curl -s "$url" | grep -oE '[0-9]+_<HEAD_SHORT_SHA>/' && exit 0
      sleep 60
    done
    ```
    Timeout with no folder → check the MR for a build failure.

### 1.5 QA tests
- Monitor the Android TestRail project (id in `local-constants.md`) run `Android ALPHA release v<X.Y>(NNNNNNNNN)` daily.
- Report Failed/Feedback TCs every morning to `#android-dev-team` — use the
  **`summary_release_testrail`** skill (e.g. `/summary_release_testrail v<X.Y> to #android-dev-team`).
- Monitor Play Console for Google approval; watch Crashlytics for new crashes/ANRs.

### 1.6 New Release Candidate builds
Each round, devs cherry-pick TC fixes to `release/v<X.Y>` (RC verifies). To roll a new RC:
- **If there's a new SDK/MEGAChat RC** (check `#sdk` / `#megachat_native` thread for the
  new tag targeting Android `<X.Y>` — often only one of them bumps; **pass BOTH tags anyway**):
  1. Re-apply the **GitLab redirect (step 2669)** — `checkout -B`/branch switches reset
     `.gitmodules` back to GitHub, so the new rc tag won't fetch until you redo it +
     `submodule sync && submodule update --remote`. Verify `git -C …/mega/sdk tag -l <NEW_TAG>` resolves.
  2. On `develop`: `./gradlew preRelease --rv "<X.Y>" --sdk "<NEW_SDK_TAG>" --chat "<CHAT_TAG>"`
     → new **pre-release MR** (strings + SDK submodule bump). Note: develop's `appVersion`
     stays `<X.Y>` through QA (only the Jira fixVersion moved to NEXT), so `--rv "<X.Y>"` is right.
  3. **`build sdk-aar … in the PRE-RELEASE MR`** (NOT the release MR) — the result posts to
     **#sdk-android-pipeline** with `Version: <…-rel>`. Read it there (no GitLab token needed).
  4. Set `megaSdkVersion` to that `-rel` value in the pre-release MR (commit only `build.gradle.kts`), push.
  5. Get the pre-release MR **2 approvals + green pipeline → merge to develop** (squash), then
     **cherry-pick the squashed merge commit** onto `release/v<X.Y>` (next bullet).
- **Monitoring the merge without a GitLab token** (the merge is reviewer-gated; you wait on it):
  background-poll the remote over SSH (no token needed) and auto-continue when it fires. Two
  definitive signals — the source branch is deleted on merge (`force_remove_source_branch`), and
  the squash lands on develop:
  ```bash
  for i in $(seq 1 120); do
    git fetch origin develop -q 2>&1 | grep -viE 'submodule|not our ref' || true
    [ -z "$(git ls-remote --heads origin task/pre-release/v<X.Y>)" ] && { echo "MERGED (branch deleted)"; exit 0; }
    case "$(git log --oneline -1 origin/develop)" in *"Pre-release - v<X.Y>"*) echo "MERGED"; exit 0;; esac
    sleep 60
  done
  ```
  Run it with `run_in_background: true`; you're notified on exit, then continue with the cherry-pick.
  (Avoid `/dev/null` redirects — the sandbox blocks them.)
- **Strings-only round** (no SDK bump): `./gradlew preRelease --rv "<X.Y>"` → merge to develop → cherry-pick strings commit to release.
- **Cherry-pick onto release (squash-merge aware):** the pre-release MR squash-merges into
  **one** commit on develop (e.g. `Pre-release - v<X.Y>`); that's what you cherry-pick — not
  the 3 original branch commits (they're orphaned by the squash). Steps: `git fetch`,
  `git checkout -B release/v<X.Y> origin/release/v<X.Y>`, `git cherry-pick <squashed-sha>`.
  - ⚠️ **Expect a conflict in `resources/string-resources/src/main/res/values/strings_shared.xml`**
    (English source) — release's tail differs from develop's. The HEAD (release) side is usually
    empty and the incoming side adds the new `<string>`s before `</resources>`: **keep the incoming
    block**, drop the markers. The SDK gitlink (→ new rc tag), `megaSdkVersion`, and the
    per-locale `strings_shared.xml` all apply cleanly. Then `git add` the file,
    `GIT_EDITOR=true git cherry-pick --continue`, verify no `<<<<<<<` remain, push.
- **`deliver_appStore` on the release MR** → new Alpha. Watch the build report on the release MR
  + `#android` + `#qa` (`:rocket: …uploaded to Google Play Alpha … Version: <X.Y>(<code>)`).
  - **Monitoring without a GitLab token:** background-poll the **public** Artifactory listing
    `https://artifactory.developers.mega.co.nz/artifactory/android-mega/release/v<X.Y>/` — a new
    build appears as a `<versionCode>_<release-HEAD-short-sha>/` folder (e.g. `261761238_1e964c7142/`);
    `slack_info.txt` / `release_info.txt` there carry the published version string. Poll for a folder
    matching the release HEAD sha; timeout → check the MR for a build failure.
- **Set `Fixed` TCs → `Retest`** so QA re-verifies on the new build. TestRail statuses:
  **Fixed = `status_id` 8, Retest = `4`** (Failed 5, Feedback 10). Find them
  `TR "get_tests/<run>&status_id=8"`, then for each POST `add_result/<test_id>` with
  `{"status_id":4,"comment":"new Alpha RC delivered (v<X.Y>, SDK <tag>) — please retest"}`.
- Repeat 1.5–1.6 until QA signs off (no failed TCs).

---

## 2. Release (Beta → Production)

Trigger: QA signs off in the `#android` release thread.

> 🛑 **VERIFY THE EXACT BUILD VERSION CODE IMMEDIATELY BEFORE EVERY PROMOTION REQUEST**
> (Beta, and each of Production 25/50/100). **Do NOT reuse the version code of the Alpha
> you personally built** — the `release/v<X.Y>` branch keeps moving (dev hotfixes, new SDK
> RCs, extra `deliver_appStore` runs), so the current build is often newer than your last Alpha.
> **Real mistake (v16.9):** posted a #devops-cicd Production-25% request for `16.9(261880606)`
> (my round-4 Alpha) when the actual latest build was `16.9(261881039)` (`3c7e30562a`, a hotfix +
> SDK `v10.16.0-rc.3` had landed after). Slack has **no edit/delete** → a correction reply is the
> only fix and it's messy/embarrassing.
> **How to get the right code every time:** take the **newest** entry from any of:
>   - **`#android`** — the deliver/build reports post here; grab the most recent
>     one **for the current release** (e.g. `16.9(...)`). ⚠️ **Filter by version** — `#android`
>     also carries builds/hotfixes for *other* live versions (e.g. 16.8.x); never grab a code
>     that isn't `16.9(...)`.
>   - `release/v<X.Y>/` on Artifactory — the highest `<code>_<sha>/` folder; or
>   - the release MR's latest `:rocket: … uploaded to Google Play` / deliver report.
> **Cross-check:** confirm the report's `<sha>` matches `git rev-parse origin/release/v<X.Y>`
> before posting. Promote **that** `<X.Y>(<code>)`.

### 2.1 Beta
- **Feature flags → PUBLISH:** post the reminder in **`#android-dev-team`**,
  tagging `@androiddevs-urgent` (even if the 1.3 request got no replies — send it anyway):
  > Hi @androiddevs-urgent, v<X.Y> has been signed off by QA and is moving to Beta. If you have a remote feature flag to enable in this release, please set its status to *PUBLISH* in https://featureflags.tools.mega.co.nz/ and set *Min Android (rules.av)* to *<X.Y>*. Also please update your flag status in the Android Remote Feature Flags Tracking doc. Thank you.
- **Ask `#devops-cicd` to promote to Beta, 100%** — exact template (ping
  `@eu-mobile-release` always; add `@nz-mobile-release` **only** 7am–7pm NZ time — you can't
  read the wall clock, so confirm the NZ hour with the user; app build timestamps are `+12` NZ):
  > Hello @eu-mobile-release team, the MEGA Android app *<X.Y>(<code>)* is now ready for promotion to *BETA* channel with a *100% rollout*.
  > Job: https://controller.cibuild.mega.co.nz/job/Android-Promote
  > • Application: MEGA
  > • Version: <X.Y>(<code>)
  > • Target channel: Beta
  > • Percentage: 100%
  > Thank you
  (Add `@nz-mobile-release` after `@eu-mobile-release` when within NZ hours.)
- DevOps runs the Android-Promote job and replies "done" in-thread; it auto-notifies the #android release thread.

### 2.2 Production (phased: 25% → 50% → 100%)
- Ensure the previous version is at 100% first.
- `#devops-cicd` promote to **Production 25%** (same template as 2.1, `Target channel: Production`,
  `Percentage: 25%`); wait 1–2 days at each stage.
- Upload Crashlytics symbols: `upload_symbol` on the release MR.
- `postRelease -rv <X.Y>` on the release MR → sets cross-project releases to
  *Released*, closes tickets, publishes GitLab/GitHub tags, merges release→master, pushes to GitHub.
- Publish GitLab + GitHub releases (auto via postRelease; manual steps in Confluence). Publish Core-UI to GitHub.
- Upload release-notes translations: `./gradlew readReleaseNotes --app-version "<X.Y>" --xml release.xml` → paste into Play Console (skip if default notes / already uploaded).
- Promote to **50%**, then **100%** (`#devops-cicd` templates).
- Notify `#sdk` (reply in thread), `#release`, `#app_release_updates` that the build is signed off. Update the Release plan doc.
- Set Crashlytics reminders (`/remind #mobile-dev-team … in 4 hours` / `… in 12 hours`).

### 2.3 After release
- Watch Play Console ANR/crash, Crashlytics, Play reviews, and App Content section (notify Android TLs in `#android`).
- **Close the RC ticket — do NOT add a fix version to it.**

---

## 3. Hotfix
For **TL-approved** crashes/bugs only; minimal code; QA usually skipped so test carefully.
Worked example below = **16.9.1** for AND-24225 (`IllegalStateException` — `LiveData.setValue` off
the main thread in `AudioPlayerServiceViewModel.onPlayerError`; fixed by wrapping in `withContext(mainDispatcher)`).

1. **Analyse** — read the crash ticket (`jira_get_issue`); it links a **Crashlytics** issue id, so
   you can pull the stack trace from Firebase too. Get root cause + impact (users/events).
2. **Is the fix already on develop?** Often a dev has already merged it. Check:
   `git merge-base --is-ancestor <fix-sha> v<X.Y>` → *not* on the tag means it needs cherry-picking
   (and *on develop* means no new code to write — just cherry-pick). If not fixed yet, follow the
   fix flow: develop+release → fix on develop → merge → cherry-pick to release; release-only → fix on release.
3. **Jira version** — create **only** the projects that change (AND-only for an app fix; + cross-project).
   ⚠️ **MCP `jira_create_version` / `jira_batch_create_versions` FAIL on this Server**
   (`Unexpected response from Jira API: None`) → **have the RC create `Android <X.Y.Z>` manually**
   (the MR bot `create_jira_version` works but over-creates across all 11 projects — avoid for a hotfix).
4. **Cut the branch:** `./gradlew releaseHotfix --rv="<X.Y.Z>" --tag="v<X.Y>"` → `release/v<X.Y.Z>`
   + `Release - v<X.Y.Z>` MR (auto: target `master`, squash=false, WIP).
5. **Cherry-pick** the fix commit onto `release/v<X.Y.Z>` (clean when the base tag lacks it); verify the
   change is present. Set the ticket's `fixVersion = Android <X.Y.Z>` (`jira_update_issue`,
   `{"fixVersions":[{"name":"Android <X.Y.Z>"}]}`) — **only after** the version exists (step 3).
6. **Bump** `appVersion` → `<X.Y.Z>` in root `build.gradle.kts` (commit ONLY that file), push.
7. **`deliver_appStore`** on the hotfix MR → build `<X.Y.Z>(<code>)`. **Re-verify the code** (see the
   🛑 rule in §2) from Artifactory `release/v<X.Y.Z>/` before any promotion request.
8. **`#devops-cicd` promote** — TL decides phase (we did **Production 25%**); `@eu` always, `@nz` only 7am–7pm NZ.
9. Wait for DevOps "done" → **`upload_symbol`** → **`postRelease -rv <X.Y.Z>`** (closes the ticket,
   publishes `v<X.Y.Z>` GitLab/GitHub tags, merges `release/v<X.Y.Z>` → master, deletes the branch).
   Detect completion via the same git-poll (branch deleted / `v<X.Y.Z>` tag).
10. **Hotfix report → #android release thread** (reply in the version's release thread).
    Format = Feng's 16.7.1 report — sections: *Hotfix* (app version + SDK/Karere commit links),
    *Descriptions* (what happened + Jira tickets), *Impacts* (events/users, when + rollout %),
    *Causes & Solutions*, *Actions taken* (dated timeline w/ @mentions), *How to avoid in future*.
11. **2529 — Update the Release Plan** (page id in `local-constants.md`): add the `<X.Y.Z>` row **directly above its base
    version** row; Code Freeze Date = Release Date = the hotfix day; RC = plain-text `@Name` (the page
    switched away from `ri:user` links). Then phase 25→50→100 like a normal release.

---

## Related skills / tools
- **`summary_release_testrail`** — daily Failed/Feedback TestRail → Slack notice.
- **`weblate`** — upload new strings / release notes to WebLate.
- **`resolve-testrail-issue`** — backport a fix to a release branch + resolve the TestRail test.
- **`crash-triage` / `analyze-native-crash` / `create-crash-ticket`** — crash work during QA/post-release.
- **`create-mr`** — open MRs for fixes.
- MCP: `jira_*` (tickets/versions/transitions), `confluence_*` (read checklist;
  whole-page write only), Slack reader (search/send), `crashlytics_*`, `firebase_*`.

## Gotchas
- Large Jira/Confluence reads overflow to a file — read that file (Confluence
  storage ≈ 2× the markdown size).
- `create_jira_version` takes the **next** version (2 weeks out), not the current.
- Do **not** rebase the `release` MR onto develop; do **not** squash it.
- `#nz-mobile-release` pings only between 7am–7pm NZ time.
- `/remind` is **manual only** — never send it via API or fake it with a scheduled
  message; the RC pastes it into Slack themselves (see 1.1).
