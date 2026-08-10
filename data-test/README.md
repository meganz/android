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

No deep SDK behaviour is simulated: commands such as copy/move/delete never mutate the node
tree. Tests needing tree changes mutate `nodeTree` directly or stub the relevant reads. The
`MegaApiJava`/`MegaChatApiJava` parameter passed to listener callbacks is an inert instance that
must never be invoked.

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

Notes for writing more of these: app startup initializers and `BootEventReceiver` no-op under
the Hilt test application (their entry points don't exist yet at process start), so tests
replicate what they need in `@Before` (Analytics, WorkManager, notification channels). Drive
the UI with UiAutomator, not a Compose test rule — the compose rule's idle synchronization
deadlocks against the production splash-gated composition. Cross-process system UI (the file
picker) is stubbed with Espresso-Intents returning a MediaStore file; a file in the app's own
private dir would be rejected by the upload pipeline's file preparation.

## Status

The gateways are faked at the `:data` gateway-interface level, which is the abstraction the
refactored app talks to. Parts of the app that still access the SDK directly cannot use these
fakes until they are refactored to go through the gateways.
