# :data-test — Fake SDK Gateways

WireMock-style, in-process fakes of the data layer's SDK gateway interfaces for automated
testing:

- `FakeMegaApiGateway` implements `mega.privacy.android.data.gateway.api.MegaApiGateway`
- `FakeMegaChatApiGateway` implements `mega.privacy.android.data.gateway.api.MegaChatApiGateway`

Both come with common-sense defaults, so a test only configures the behaviour it cares about,
plus a secondary test-facing surface to stub responses, verify calls, emit SDK events and reset.

```kotlin
val gateway = FakeMegaApiGateway()

// Stub results per method, optionally per arguments
gateway.stubResult(MegaApiGateway::getNumUnreadUserAlerts, 3)
gateway.stub(MegaApiGateway::getMegaNodeByHandle, matcher = { it[0] == 7L }) { stubbedNode }

// Stub the outcome of listener-based commands
gateway.stubRequest(MegaApiGateway::login, error = StubMegaError(MegaError.API_EMFAREQUIRED))

// Or script a transfer progressively, as if a large file were uploading
gateway.stubTransferScript(startUploadRef, steps = progressSteps, finalTransfer = done)

// Configure state-backed defaults directly
gateway.account.isLoggedIn = false
gateway.nodeTree.addNode(StubMegaNode(handle = 10L, name = "photo.jpg", parentHandle = 1L), 1L)

// Emit SDK events into the gateway flows
gateway.emitGlobalUpdate(GlobalUpdate.OnReloadNeeded)

// Verify recorded calls
assertThat(gateway.invocationsOf(MegaApiGateway::login)).hasSize(1)

// Back to defaults
gateway.resetToDefaults()
```

## Default behaviour

Out of the box each fake represents a logged-in account (`test@mega.nz`, handle `111`) with a
node tree seeded with Cloud Drive / Rubbish Bin / Vault roots (handles 1/2/3):

- Node and account reads resolve through the mutable state objects (`account`, `nodeTree`,
  `chatState`).
- Other queries return "empty success": `null` for nullable reads, `false`/`0`/empty stub lists,
  or the SDK's invalid-handle/sentinel constants.
- Listener-based commands synchronously complete their listener with `onRequestStart` +
  `onRequestFinish` and `API_OK` / `ERROR_OK`, using a stub request of the matching
  `TYPE_*` constant. Transfer methods complete `onTransferStart` + `onTransferFinish`.
- `Flow` properties are backed by shared flows with public `emit*` helpers.

No deep SDK behaviour is simulated: listener-based commands such as `copyNode`/`moveNode`/
`deleteNode` never themselves mutate the node tree. To apply the SDK-side effect, use the
`nodeTree` mutating helpers, which change the state **and** emit the matching
`GlobalUpdate.OnNodesUpdate` (with the real `MegaNode.CHANGE_TYPE_*` flag) in one call:

```kotlin
gateway.nodeTree.rename(handle = 10L, newName = "renamed.jpg")   // CHANGE_TYPE_NAME
gateway.nodeTree.move(handle = 10L, newParentHandle = 20L)       // CHANGE_TYPE_PARENT
gateway.nodeTree.copy(handle = 10L, newParentHandle = 20L)       // CHANGE_TYPE_NEW, returns the copy
gateway.nodeTree.moveToRubbish(handle = 10L)                     // CHANGE_TYPE_PARENT to Rubbish
gateway.nodeTree.remove(handle = 10L)                            // CHANGE_TYPE_REMOVED
```

Unknown handles (and moves under a node's own descendant) are no-ops returning null, matching how
the read helpers treat invalid input. Tests can still mutate `nodeTree` directly (`addNode` /
`removeNode`) and emit updates by hand when they need finer control. The
`MegaApiJava`/`MegaChatApiJava` parameter passed to listener callbacks is an inert instance that
must never be invoked.

## Chat message history

The chat fake feeds the app's real message paging pipeline. Seed a room's history (oldest first,
unique `msgId`s, distinct timestamps) and the fake answers `loadMessages` the way the SDK does:
the next batch is emitted newest to oldest as `ChatRoomUpdate.OnMessageLoaded` into the
`openChatRoom(chatId)` flow, followed by an `OnMessageLoaded(null)` terminator, returning
`SOURCE_LOCAL`. Once the history is exhausted (or when nothing is seeded) only the null
terminator is emitted and `SOURCE_NONE` is returned, so paging terminates cleanly; the delivery
cursor then rewinds so a later refresh replays the full history.

```kotlin
gateway.chatState.chatRooms[chatId] = StubMegaChatRoom(chatId = chatId, title = "Chat")
gateway.chatState.addChatMessage(
    chatId,
    StubMegaChatMessage(
        msgId = 1L, userHandle = peerHandle, type = MegaChatMessage.TYPE_NORMAL,
        status = MegaChatMessage.STATUS_SEEN, timestamp = 1_700_000_000L, content = "hi",
    ),
)

// Deliver a live incoming message (appends to the history and emits OnMessageReceived)
gateway.emitMessageReceived(chatId, StubMegaChatMessage(msgId = 2L, /* … */))
```

`sendMessage` echoes an own in-flight message as the SDK does — a `STATUS_SENDING` message with a
fresh generated id (used as both `msgId` and `tempId`), authored by the logged-in user — and
appends it to the room's history, so a sent message renders through the app's real send-and-store
path. `getMessage` resolves through the seeded history.

## Stubbing semantics

Stubs are keyed by method name (`KFunction.name`), so overloads share stubs — use the
`matcher` parameter to disambiguate by arguments. Later stubs win over earlier ones and stubs
are persistent (not one-shot). `clearStubs()`, `clearInvocations()` and `reset()` /
`resetToDefaults()` restore behaviour.

## Structure

| Package | Contents |
|---|---|
| `engine` | `FakeGatewayEngine` — stub registry, invocation recording, `FakeGatewayStubbing` surface |
| `stub` | `Stub*` subclasses of the SWIG SDK wrapper classes, constructible without native code |
| `state` | `FakeAccountState`, `FakeNodeTree`, `FakeChatState` — mutable default state |
| `gateway` | `FakeMegaApiGateway`, `FakeMegaChatApiGateway` and outcome types |

The `Stub*` classes subclass the SDK wrappers via the protected `(cPtr, cMemoryOwn)` constructor
with `(0, false)` and override every public instance method, so they are safe to use in JVM unit
tests where the native SDK library is not loaded. They can also be used directly in tests to
build return values.

The module's unit tests double as behavioural documentation — see
`src/test/kotlin/mega/privacy/android/data/test/` for the documented defaults and stubbing
semantics of every area.

## Full-app instrumented tests

The gateway bindings live in the public `SdkGatewayModule` (`:data`), so an instrumented test
suite can replace only the SDK while the rest of the app stays real:

- `app/src/androidTest/.../di/FakeSdkGatewayModule.kt` installs the fakes app-wide via
  `@TestInstallIn(replaces = [SdkGatewayModule::class])` and exposes them for test injection.
- `app/src/androidTest/.../CloudDriveUploadTest.kt` is the reference example: it boots the full
  app logged in (credentials seeded through the app's real save-credentials path, account
  details fetched from the fake), drives the UI with UiAutomator, runs a real upload through
  the transfer chain, and simulates the SDK's post-upload node update.

Notes for writing more of these: `MegaApplication.onCreate`/`onStart` do not run under the Hilt
test application, so the production boot initialisers are not triggered automatically. Boot the
test process through them with `TestAppBoot.runCoreInitializers()`
(`app/src/androidTest/.../boot/TestAppBoot.kt`) in `@Before`: it initialises test WorkManager and
then runs the same app-create and app-start initialiser units as production via `GlobalInitialiser`
(Analytics, notification channels, the transfer-events monitor, and the rest), keeping test and
production boot converged. Pass an `exclude` set of unit names (`AppCreateInitialiser.name`) only
for a unit a test demonstrably cannot tolerate, or `includeAppStart = false` to skip the app-start
tier. Only genuinely device-level or harness concerns stay in `@Before` (planting Timber, granting
POST_NOTIFICATIONS) alongside the scenario's own state seeding. Drive the UI with UiAutomator, not a
Compose test rule — the compose rule's idle synchronization deadlocks against the production
splash-gated composition. Cross-process system UI (the file picker) is stubbed with Espresso-Intents
returning a MediaStore file; a file in the app's own private dir would be rejected by the upload
pipeline's file preparation.

## Status

The gateways are faked at the `:data` gateway-interface level, which is the abstraction the
refactored app talks to. Parts of the app that still access the SDK directly cannot use these
fakes until they are refactored to go through the gateways.
