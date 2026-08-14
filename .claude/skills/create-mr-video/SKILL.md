---
name: create-mr-video
description: >
  Produce demo recordings for the current branch's MR description: propose an instrumented
  test that demonstrates the branch's flow (feature) or defect (bug fix), create it once the
  developer approves, run it on a connected device, and capture screen recordings that start
  where the action starts. Bug fixes get two recordings — the issue on the pre-fix build and
  the fixed behaviour on the branch build.
triggers:
  - /create-mr-video
  - mr video
  - demo recording
  - record the mr
---

# Create MR Video

Turn the current branch's change into a watchable demo clip (or pair of clips) for the MR
description, driven by a real instrumented test on the `:data-test` fake-SDK harness.

## Usage

```
/create-mr-video                 # Analyse branch, propose the demo test, await approval
/create-mr-video --bug          # Force bug-fix mode (two recordings)
/create-mr-video --feature      # Force feature mode (one recording)
/create-mr-video --test <FQCN#method>   # Skip proposal; record an existing test
```

## Workflow

### 1. Analyse the branch

- Diff the branch against develop: `git diff --stat origin/develop...HEAD`, then read the
  key changed files. Identify the user-visible behaviour the branch adds or fixes.
- Classify **feature** vs **bug fix**: check the Jira ticket type for the key in the branch
  name (`bash tools/jira/jira status <KEY>`), the commit messages, and the nature of the
  diff. `--bug` / `--feature` override.
- Check `app/src/androidTest/java/test/mega/privacy/android/app/` for an existing test that
  already demonstrates the change — if one exists, propose recording it instead of writing a
  new one.

### 2. Propose the demo test (STOP — user approval required)

Present a concise proposal and wait for approval before writing any code:

- The journey as a numbered list of on-screen steps (what the viewer will see).
- What is asserted at each step.
- How state is seeded (fake gateways, preferences, feature flags).
- For bug fixes: the same journey shown twice — state which step visibly goes wrong on the
  pre-fix build.

Design the journey for the camera:

- **One continuous test method per recording**, one activity launch. Instrumented launches
  are expensive and a mid-test process restart records as dead launcher time.
- Show the *before* state, the *action*, and the *after* state in one flow (e.g. prove a row
  is absent before demonstrating what makes it appear).
- `Thread.sleep(1_000..2_000)` holds at each state a human should register. End on a 2s hold
  of the final state.

### 3. Create the test

Location: `app/src/androidTest/java/test/mega/privacy/android/app/` (package
`mega.privacy.android.app`). Model it on `MenuHiddenSectionsTest`, `RenameNodeTest`, and
`ChatRoomTest` — read one before writing.

Harness conventions:

- `@HiltAndroidTest` + `HiltAndroidRule`; boot with `TestAppBoot.runCoreInitializers()`.
- The whole app runs as in production; only the SDK gateways are faked
  (`FakeMegaApiGateway`, `FakeMegaChatApiGateway` from `:data-test`). Seed SDK state through
  the fakes (`stub`, node tree, chat state).
- Log in through the production path: inject and call `SaveAccountCredentialsUseCase()` then
  `GetSpecificAccountDetailUseCase(storage, transfer, pro)` in `@Before`.
- Force feature flags with the injected `FakeFeatureFlagValueProvider` (works for both
  `AppFeatures` and `ApiFeatures`); `clear()` it in `@After`.
- Seed preferences through injected domain use cases — the same ones production writes with.
- Drive the UI with **UiAutomator**, not a Compose rule (the compose rule's idle sync fights
  the production activity's splash-gated composition). Compose test tags are matched as
  resource ids (`By.res(tag)`) because `MegaActivity` sets `testTagsAsResourceId = true`.
- Copy the `awaitObject` (wait + UiDump-to-logcat on timeout) and `scrollUntilObject`
  helpers from an existing test rather than reinventing them.
- Grant `POST_NOTIFICATIONS` up front via `uiAutomation.grantRuntimePermission`.

Gotchas (each cost a debugging round when this skill was authored):

- **One test method per `am instrument` invocation.** Two tests in one process die on
  "multiple DataStores active for the same file" — the Gradle runner avoids this with the
  Test Orchestrator, manual runs must invoke each method separately.
- **Legacy `BaseActivity` screens cannot run under the Hilt test application** — they bounce
  into the SDK-reload path in a loop and crash the process. Never route the journey through a
  legacy Activity (e.g. `SettingsActivity`); enter Compose destinations through the production
  seam instead: `MegaActivity.getIntentWithExtraDestinations(context, listOf(<NavKey>))` with
  `FLAG_ACTIVITY_NEW_TASK`, or a `MegaNavigator` call.
- **Mark one-time onboarding UI as already seen in `@Before`** (e.g.
  `SetCustomiseNavigationTooltipShownUseCase`) so popups cannot intercept taps on clean-data
  runs.
- **`By.text` collides with navigation-bar labels.** Scope list-row matches with
  `By.res(rowTag).hasDescendant(By.text(label))`.
- **Business rules bite silently** (e.g. the customise-navigation minimum item count rejects
  a save with only a snackbar). Seed starting state that keeps every scripted action valid.
- Bottom navigation item tags are `main_navigation:navigation_item_<NavKeyClassSimpleName>`;
  the `MegaTopAppBar` back affordance is `By.desc("Navigation Icon")`.
- Resolve label strings from resources (`targetContext.getString(sharedR.string...)`), never
  hardcode English text.

### 4. Verify green before recording

```bash
./gradlew :app:installGmsDebug :app:installGmsDebugAndroidTest
adb shell am instrument -w \
  -e class "mega.privacy.android.app.<TestClass>#<method>" \
  mega.privacy.android.app.test/mega.privacy.android.app.HiltTestRunner
```

Pick the device with `adb devices -l` and export `ANDROID_SERIAL` (ask the user if several
are attached and the choice matters). Iterate until `OK (1 test)`. On UiAutomator timeouts,
read the window-hierarchy dump: `adb logcat -d -s UiDump`.

### 5. Record

The recording must start where the action starts — not at instrumentation setup. Run the
whole take as **one shell command** (splitting steps across separate tool calls adds tens of
seconds of latency and misses the window):

```bash
export ANDROID_SERIAL=<serial>
adb shell pidof screenrecord && adb shell killall -2 screenrecord
adb shell am instrument -w \
  -e class "mega.privacy.android.app.<TestClass>#<method>" \
  mega.privacy.android.app.test/mega.privacy.android.app.HiltTestRunner &
INSTPID=$!
for i in $(seq 1 300); do
  focus=$(adb shell dumpsys activity activities | grep -m1 topResumedActivity)
  case "$focus" in *MegaActivity*) break;; esac
  sleep 0.25
done
adb shell screenrecord --bit-rate 8000000 --time-limit 150 /sdcard/<clip-name>.mp4 &
RECPID=$!
wait $INSTPID
sleep 1
adb shell killall -2 screenrecord
wait $RECPID
```

- Poll `topResumedActivity` from `dumpsys activity activities`. Do **not** use
  `dumpsys window`'s `mCurrentFocus` — multi-display devices report a `null`-focus display
  first and `grep -m1` matches it forever.
- If the journey launches an activity other than `MegaActivity`, match that class instead.
- Name clips `AND-<ticket>-<slug>.mp4`.

Then pull and verify:

```bash
adb pull /sdcard/<clip-name>.mp4 ~/Desktop/<clip-name>.mp4
```

Check the duration is plausible (test wall time minus setup). Without ffmpeg, parse the mp4
`mvhd` box with inline python:

```python
import struct
data = open(PATH, "rb").read()
i = data.find(b"mvhd")
if data[i + 4] == 1:  # version
    timescale, duration = struct.unpack(">I", data[i+24:i+28])[0], struct.unpack(">Q", data[i+28:i+36])[0]
else:
    timescale, duration = struct.unpack(">I", data[i+16:i+20])[0], struct.unpack(">I", data[i+20:i+24])[0]
print(f"{duration/timescale:.1f}s")
```

Redo the take if the file is tiny (< ~100 KB — the recorder started after the test finished)
or wildly long.

### 6. Bug-fix mode: two recordings

Record the same test twice — first against the app **without** the fix, then with it:

1. Build the pre-fix app from the merge base in a temporary worktree (keeps the checkout
   clean; remove it afterwards):
   ```bash
   git worktree add <scratch>/prefix-build $(git merge-base origin/develop HEAD)
   (cd <scratch>/prefix-build && ./gradlew :app:installGmsDebug)
   git worktree remove <scratch>/prefix-build
   ```
   Respect the one-gradle-build-at-a-time rule per worktree.
2. Install the **branch** test APK (`:app:installGmsDebugAndroidTest` from the main
   checkout — APK signatures match, only the app APK differs).
3. Recorded take #1 (`AND-<ticket>-issue.mp4`): the assertions on the fixed behaviour are
   *expected to fail* — that is the point. Put the demonstrating holds **before** the failing
   assertion so the clip shows the broken state, and keep the failure late in the journey.
4. Reinstall the branch app (`./gradlew :app:installGmsDebug`) and recorded take #2
   (`AND-<ticket>-fixed.mp4`), which must pass.

### 7. Finish

- Commit the test to the branch (it ships with the MR) and push.
- Tell the user where the clips are (`~/Desktop/…`) and suggest the MR-description wording
  for attaching them (issue clip first, fix clip second, one line describing each).
- Mention that copies remain on the device at `/sdcard/` (deleting device files needs the
  user).
- If a Jira ticket is associated, note the clips can also be attached there.

## Notes

- Requires a connected device/emulator (`adb devices`), the `gms` debug flavor, and nothing
  else — no ffmpeg, no orchestrator.
- The recording holds (`Thread.sleep`) stay in the committed test: they cost ~7s per CI run
  and keep every future run recordable.
- If the branch's flow cannot be reproduced against the gateway-level fakes (out-of-process
  pickers, rendered chat message lists — see `ChatRoomTest`'s KDoc for known exclusions),
  say so in the proposal and offer a manually-driven recording instead.
