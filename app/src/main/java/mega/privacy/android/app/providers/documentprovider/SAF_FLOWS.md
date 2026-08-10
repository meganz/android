# SAF DocumentsProvider Write Flows

How the MEGA Cloud Drive `DocumentsProvider` exposes **upload file**, **create folder**, **upload folder (with nested files)**, and **rename** to the Storage Access Framework (SAF). Read this once and the code (`CloudDriveDocumentProvider` + `CloudDriveDocumentDataProvider`) becomes much easier to follow.

---

## 1. The mental model

### Two collaborating classes

| Class | Lives where | Owns |
|---|---|---|
| `CloudDriveDocumentProvider` | Binder thread (SAF) | URI/Cursor protocol, `notifyChange`, `setNotificationUri`, exception mapping. Has `Context`. |
| `CloudDriveDocumentDataProvider` | `@Singleton`, `applicationScope` | `state: StateFlow`, request flow, SDK use cases, pending entries. **No `Context`**. |

The provider is the SAF surface. The data provider is the engine. The provider holds **no business state**; it consults `dataProvider.state.value` and returns cursors.

### State produced by the data provider

`state` is a `StateFlow<CloudDriveDocumentProviderUiState>` derived from three things, combined via `flatMapLatest`:

1. **Session state** — `monitorPasscodeLock` × `connectivity` × `monitorUserCredentials` → produces `NotLoggedIn` / `PasscodeLockEnabled` / `Offline` / `Ready(rootNodeId)`.
2. **`requestFlow`** — `MutableStateFlow<DocumentDataRequest>` toggled by the provider via `loadDocumentInBackground(id)` / `loadChildrenInBackground(id)`. Decides whether the inner flow is fetching a single document or the children of a parent.
3. **`monitorNodeUpdatesUseCase`** — every emission cancels and restarts the inner flow so it re-fetches against the latest SDK state.

```
monitorSessionState
  └─ flatMapLatest sessionStateToUiState
        └─ monitorNodeUpdates.map{}.onStart{Unit}
              └─ flatMapLatest requestFlow
                    └─ flatMapLatest → getDocumentFlow | getChildrenFlow
```

UI states the provider reads:

- `Initialising` — initial value of `asUiStateFlow`.
- `RootNodeNotLoaded` — `Ready` but root not yet available.
- `LoadingDocument` / `LoadingChildren` — flow is in the middle of a fetch.
- `DocumentData` / `ChildData` — the data the cursor will surface.
- `FileNotFound` — terminal "node not found" for that doc id.
- `PasscodeLockEnabled` / `Offline` / `NotLoggedIn` — session error states.

### Document IDs

Three shapes:

- Root: literal string `mega_cloud_drive_root` (`CLOUD_DRIVE_ROOT_ID`).
- Real nodes: `mega_cloud_drive_root:1234567890`.
- Pending placeholders: `mega_cloud_drive_pending:<uuid>` (`PENDING_PREFIX`).

`DocumentIdToNodeIdMapper` / `NodeIdToDocumentIdMapper` convert between the first two. The placeholder shape is recognised by `isPendingDocumentId` and resolved to a real `NodeId` by `resolvePendingFolderParent` (folders only; files never need this since SAF doesn't put files under placeholder file ids).

### Two cursor lifecycles you must keep straight

| Caller | Lifecycle | Notify works? |
|---|---|---|
| `queryChildDocuments` (folder listing) | SAF keeps cursor open while the folder is displayed; `setNotificationUri` registers an observer | **Yes** — `notifyChildDocumentsChanged(parent)` triggers a re-query. |
| `queryDocument` via `DocumentInfo.fromUri` | One-shot synchronous read; cursor closed before `notifyChange` reaches it | **No** — the data has to be fresh in `state.value` *at the moment* the cursor is built. |

Every write flow is shaped by this distinction.

---

## 2. Pending entries (foundation for upload, create folder, upload folder)

The data provider keeps three concurrent maps for in-flight write work:

```kotlin
// Placeholder rows + per-write metadata. Lifetime:
//  - file: until upload finishes (success / failure / timeout) → finalizePendingFile
//  - folder: kept alive after completeFolderCreation so SAF can keep using the
//    placeholder id as a destination for nested children
private val pendingCreates = ConcurrentHashMap<String, PendingCreate>()

// One-shot signal completed when a pending file is finalized.
private val pendingFinalizeSignals =
    ConcurrentHashMap<String, CompletableDeferred<Unit>>()

// Real NodeId published by completeFolderCreation; lets nested SAF createDocument
// calls resolve their parent without waiting once the folder is materialised.
private val pendingFolderRealNodeId =
    ConcurrentHashMap<String, NodeId>()

// One-shot signal completed by completeFolderCreation. Carries the real NodeId on
// success, or the SDK error on failure so awaiters fail fast instead of timing out.
private val pendingFolderSignals =
    ConcurrentHashMap<String, CompletableDeferred<NodeId>>()
```

`PendingCreate` carries `parentNodeId`, `parentDocumentId`, `displayName`, `mimeType`.

Helpers:

- `registerPendingFolder(parent, name) → pendingId` — synchronous; checks duplicate name; resolves placeholder parents (so nested folder creates work).
- `registerPendingFile(parent, name, mime) → pendingId` — same, plus the parent can be a placeholder folder.
- `isPendingDocumentId(id)`, `getPendingDocumentRow(id)` — for `queryDocument(pendingId)`. Folder placeholders advertise `FLAG_DIR_SUPPORTS_CREATE`; file placeholders advertise `FLAG_SUPPORTS_WRITE`.
- `getPendingChildrenForParent(parentDocumentId)` — synthesised rows merged into `ChildData` so the placeholder appears in the listing immediately. Filters out folder placeholders whose real NodeId is already known, so they don't double up with the real folder that `monitorNodeUpdates` has surfaced.

`childDocumentsCursor` merges these rows in:

```kotlin
documentCursor(
    rows = state.children + dataProvider.getPendingChildrenForParent(parentDocumentId),
    ...
)
```

---

## 3. Create folder

```
SAF caller                       Provider                                  DataProvider                              SDK
    │ createDocument(parent, DIR, name)
    ├──────────────────────────►│ runBlocking(openDocumentDispatcher) {
    │                           │   registerPendingFolder(parent, name)
    │                           │ }                                        ├─ duplicate check
    │                           │                                          │ pendingCreates[pid] = ...
    │                           │◄──────── pendingId ──────────────────────┤
    │                           │
    │                           │ applicationScope.launch {
    │                           │   completeFolderCreation(pid)            ├─► createFolderNodeUseCase ──────────────►│
    │                           │     newNodeId ◄──────────────────────────┤◄────────── real NodeId ──────────────────┤
    │                           │     pendingFolderRealNodeId[pid]=newNodeId
    │                           │     pendingFolderSignals[pid]?.complete(newNodeId)
    │                           │     state.first {                        │
    │                           │       ChildData(parent) contains name }  │◄────────── monitorNodeUpdates ───────────┤
    │                           │   notifyDocumentChanged(pid)             │
    │                           │   notifyChildDocumentsChanged(parent)    │
    │                           │ }                                        │
    │◄──── pendingId ───────────┤                                          │
```

### Why publish the real NodeId *before* the `state.first` wait?

Nested SAF child create calls (the next section) suspend on `pendingFolderSignals[pid]` waiting for the parent's real `NodeId`. If we waited for state propagation first, every nested file create would block for up to 5 s (`STATE_REFRESH_TIMEOUT_MS`) per level — bad for a deep folder tree. Publishing the NodeId immediately wakes child awaiters; the state wait then only matters for the folder's *first* appearance in a listing the user is viewing.

### Why keep the placeholder alive after success?

For a nested folder copy, SAF calls our `createDocument(parent, DIR, name)` and then keeps using the returned id (the placeholder) as the destination for `createDocument(placeholder, child)` calls. If we removed the entry, `queryDocument(placeholder)` would error with `FileNotFoundException`. So:

- **Success path**: keep `pendingCreates[pid]` alive *and* populate `pendingFolderRealNodeId[pid]`. Children resolve via the real-NodeId map.
- **Failure path**: `pendingCreates.remove(pid)` + `pendingFolderSignals[pid].completeExceptionally(e)`. Awaiters fail fast (`FileNotFoundException("Invalid parent")`) and SAF aborts the subtree.

Lingering folder entries are bounded by the SAF copy operation's lifetime (process-scoped). Each entry is a few hundred bytes; we trade a tiny memory cost for correctness.

---

## 4. Upload file

The most involved single-file flow because the upload is **long-running**. Three logical phases:

1. **Register placeholder** (synchronous, off binder).
2. **Write to scratch file** (SAF writes through our PFD).
3. **Background upload** (`startUploadUseCase`) then **state.first** wait so the listing swaps placeholder → real node in one notify.

```
SAF caller         Provider                                DataProvider                           SDK / Transfers
   │ createDocument(p, mime, name)
   ├────────────────►│ runBlocking { registerPendingFile(...) }
   │                 │                                      ├─ resolveParentNodeId (placeholder OK)
   │                 │                                      ├─ pendingCreates[pid] = ...
   │                 │◄──────── pendingId ──────────────────┤
   │                 │
   │                 │ notifyChildDocumentsChanged(parent) ◄── placeholder visible NOW
   │                 │ applicationScope.launch {
   │                 │   awaitFileFinalized(pid) ──────────► (suspends on CompletableDeferred)
   │                 │   notifyChildDocumentsChanged(parent)
   │                 │ }
   │◄── pendingId ───┤
   │
   │ openDocument(pid, "w", signal)
   ├────────────────►│ resolveScratchFile(pid)              ├─ prepareWriteScratchFile(pid)
   │                 │ ParcelFileDescriptor.open(scratch,w, │   creates cache file
   │                 │   writeCloseHandler) { err ->        │
   │                 │     onWriteScratchClosed(pid, file, err)
   │                 │ }                                    │
   │◄── PFD ─────────┤
   │
   │ writes... close()
   │                 │ onWriteScratchClosed(pid, file, err) ►
   │                 │                                      ├─ if err: delete scratch + finalizePendingFile(pid)
   │                 │                                      │
   │                 │                                      ├─ else applicationScope.launch {
   │                 │                                      │   try {
   │                 │                                      │     withTimeoutOrNull(30 min) {
   │                 │                                      │       startUploadUseCase(localPath=scratch, ────►│
   │                 │                                      │         parentNodeId, fileName,                 │ uploads
   │                 │                                      │         isSourceTemporary=true,                 │
   │                 │                                      │         pitagTarget=CloudDrive)                 │
   │                 │                                      │       .collect { event ->                       │
   │                 │                                      │         /* log TransferFinishEvent errors */    │
   │                 │                                      │       }                                         │
   │                 │                                      │     }
   │                 │                                      │     withTimeoutOrNull(STATE_REFRESH_TIMEOUT_MS) {
   │                 │                                      │       state.first {                             │
   │                 │                                      │         ChildData(parent) contains pending.name │◄── monitorNodeUpdates
   │                 │                                      │       }                                         │   sees the new node
   │                 │                                      │     }
   │                 │                                      │   } finally {
   │                 │                                      │     finalizePendingFile(pid)
   │                 │                                      │   }
   │                 │                                      │ }
   │                 │
   │                 │ (awaitFileFinalized resumes)
   │                 │ notifyChildDocumentsChanged(parent) ◄── placeholder replaced by real node
```

### Key data structures

```kotlin
suspend fun awaitFileFinalized(pendingId: String) {
    if (!pendingCreates.containsKey(pendingId)) return        // already finalized
    val signal = pendingFinalizeSignals.computeIfAbsent(pendingId) { CompletableDeferred() }
    if (!pendingCreates.containsKey(pendingId)) {             // race after computeIfAbsent
        pendingFinalizeSignals.remove(pendingId)?.complete(Unit)
    }
    signal.await()
}

private fun finalizePendingFile(pendingId: String) {
    pendingCreates.remove(pendingId)
    pendingFinalizeSignals.remove(pendingId)?.complete(Unit)
}
```

`finalizePendingFile` is called from:

1. `onWriteScratchClosed` early-return when `err != null`.
2. The upload coroutine's `finally`, after success / failure / timeout of `startUploadUseCase` and the state wait.

### Two important properties

- **Placeholder visible immediately**: `childDocumentsCursor` merges pending children, and we `notifyChildDocumentsChanged(parent)` right after register.
- **Smooth handoff**: pending stays alive across the entire upload. The listing keeps showing the placeholder until the SDK actually has the real node *and* `pendingCreates` is cleared, then a single notify swaps them in one re-query.

### Why `state.first { ChildData contains name }` after the upload?

Same reason as the folder case (§3): `startUploadUseCase` returns when the SDK has the file, but `state.value` may still hold the old `ChildData(parent)`. Waiting until state reflects the new node ensures the post-finalize `notifyChildDocumentsChanged(parent)` triggers a re-query that *finds the real file* in `ChildData.children`, with the placeholder already removed.

---

## 5. Upload folder (with nested files)

Android SAF drives the recursion; our provider only reacts to per-call hooks. For source `MyFolder/file.txt` into a MEGA destination, SAF roughly does:

```
copyDocument(srcFolder, destParent):
    destFolder = createDocument(destParent, MIME_TYPE_DIR, "MyFolder")  ← us
    for child in queryChildDocuments(srcFolder):                        ← source provider
        if child.isFolder: copyDocument(child, destFolder)              // recurse
        else:
            destFile = createDocument(destFolder, child.mime, child.name) ← us
            srcPfd  = openDocument(child, "r")                          ← source provider
            destPfd = openDocument(destFile, "w")                       ← us
            stream srcPfd → destPfd
```

The destination passed back to SAF for nested calls is the **placeholder folder id** we returned from the first `createDocument`. So our provider must:

1. Treat that placeholder as a valid parent for further `createDocument` calls.
2. Block the nested call until the real folder NodeId is known (otherwise the upload destination is unresolvable).

That's what `resolvePendingFolderParent` does:

```kotlin
private suspend fun resolvePendingFolderParent(parentDocumentId: String): NodeId? {
    pendingFolderRealNodeId[parentDocumentId]?.let { return it }      // fast path
    val pending = pendingCreates[parentDocumentId] ?: return null      // unknown
    if (pending.mimeType != Document.MIME_TYPE_DIR) return null        // file placeholder
    val signal = pendingFolderSignals.computeIfAbsent(parentDocumentId) { CompletableDeferred() }
    pendingFolderRealNodeId[parentDocumentId]?.let { return it }      // race-after-register
    return runCatching {
        withTimeoutOrNull(FOLDER_CREATE_TIMEOUT_MS) { signal.await() }  // 30 s ceiling
    }.getOrNull()
}
```

Returning `null` here surfaces as `FileNotFoundException("Invalid parent")` in `registerPendingCreate`, which causes SAF to abort that subtree without affecting siblings.

### End-to-end timeline for `MyFolder/file.txt`

```
T0  createDocument(realParent, DIR, "MyFolder")
       → registerPendingFolder → pid_M (pendingCreates[pid_M] = ...)
       provider returns pid_M synchronously
       provider applicationScope.launch { completeFolderCreation(pid_M) }
       provider notifyChildDocumentsChanged(realParent)

T0+ε createDocument(pid_M, "text/plain", "file.txt")    ← SAF immediately
       → registerPendingFile
         → resolveParentNodeId("pid_M")
           → resolvePendingFolderParent("pid_M")
             → pendingFolderRealNodeId[pid_M] is null
             → pending exists, mimeType DIR
             → withTimeoutOrNull(30 s) signal.await()  *** suspends ***

T1  applicationScope finally runs completeFolderCreation(pid_M):
       → createFolderNodeUseCase("MyFolder", realParent) → realM
       → pendingFolderRealNodeId[pid_M] = realM
       → pendingFolderSignals.remove(pid_M).complete(realM)   *** awaiter resumes ***
       → state.first { ChildData(realParent) contains "MyFolder" }
       → notifyDocumentChanged(pid_M); notifyChildDocumentsChanged(realParent)

T1+ε resolvePendingFolderParent returns realM
       → registerPendingFile completes: pendingCreates[pid_F] = (parentNodeId=realM, …)
       provider returns pid_F synchronously
       provider notifyChildDocumentsChanged(pid_M)

T2  openDocument(pid_F, "w")  → scratch PFD
T3  SAF writes; PFD closes → onWriteScratchClosed(pid_F)
       → applicationScope.launch { startUploadUseCase(localPath, parentNodeId=realM, ...) }
       → on success/failure/timeout: finalizePendingFile(pid_F)
```

Deeper folders (`a/b/c/file.txt`) chain naturally: every level publishes its real NodeId to its placeholder, the next level resolves it via the same mechanism.

### What we deliberately don't do

- **No directory traversal in the provider.** SAF enumerates the source side. We only see per-child callbacks.
- **No `StartUploadUseCase` on the source folder.** We don't have the folder on disk; the source is another `DocumentsProvider`. SAF copies file-by-file via our `openDocument(write)` PFD; each closed PFD triggers exactly one `startUploadUseCase` call.
- **No re-query of the placeholder folder's children from SAF.** SAF doesn't ask for `queryChildDocuments(pid_M)` during a copy — it tracks source children itself. Our provider would error on that path; that's acceptable since it's not exercised.

---

## 6. Rename

Rename is the trickiest because of how `DocumentsUI` consumes the result:

> `DocumentsUI`'s `RenameDocumentsTask` calls `DocumentInfo.fromUri(...)` *immediately* after our `renameDocument` returns. That's a **synchronous one-shot** read of `queryDocument`. If the cursor is empty or stale, `DocumentInfo` throws an NPE inside `updateFromUri()`, the task records `null`, and the UI shows **"Failed to rename document"** even though the cloud rename succeeded.

```
SAF caller             Provider.renameDocument                  DataProvider.renameDocument            SDK
   │ renameDocument(documentId, displayName)
   ├────────────────────►│ runBlocking(openDocumentDispatcher) {
   │                     │   dataProvider.renameDocument(documentId, displayName)
   │                     │     │
   │                     │     ├─ resolveParentDocumentId(nodeId)
   │                     │     ├─ renameNodeUseCase(handle, newName) ────────────────────────────────►│
   │                     │     │
   │                     │     ├─ requestFlow.emit(Document(documentId))   // force refetch
   │                     │     ├─ withTimeoutOrNull(STATE_REFRESH_TIMEOUT_MS) {
   │                     │     │     state.first {
   │                     │     │       DocumentData && it.documentId == documentId
   │                     │     │         && it.document.displayName == newName
   │                     │     │     }
   │                     │     │   }                                                                    │
   │                     │     │◄──── state transitions to DocumentData(newName) ◄─────────────────────┤ monitor fires,
   │                     │   } ── parentDocumentId                                                      │ flow refetches
   │                     │
   │                     │ notifyChildDocumentsChanged(parentDocumentId)   ◄── refreshes listing
   │                     │ return documentId          (NOT null — see below)
   │
   │                     ◄── DocumentInfo.fromUri(uri) → queryDocument(documentId)
   │                          state.value is DocumentData(newName)  ✓
```

### Two non-obvious returns

1. **Return `documentId`, not `null`.** Android's docs say "return null if the docId didn't change", but AOSP `DocumentsUI` calls `DocumentInfo.fromUri(resolver, newUri, …)` directly — a `null` URI throws NPE inside `updateFromUri()`, which gets caught broadly and surfaces as "Failed to rename". We always return the (unchanged) `documentId` so the URI stays valid.

2. **Wait for state, then `notifyChildDocumentsChanged(parent)`.** The *document* re-query uses the data we already loaded (no observer to wait for). The *folder listing* observer is alive and will pick up the notify and re-query, getting the renamed row.

### The "error then loading" race we fixed

Even after waiting for `DocumentData(newName)`, `monitorNodeUpdates` could fire *again* between our return and SAF's re-query. The original `getDocumentFlow` started with `emit(LoadingDocument)` on every restart — so state would oscillate `DocumentData(new) → LoadingDocument → DocumentData(new)`. If SAF landed on the `LoadingDocument` window: empty cursor → "Failed to rename" toast → loading spinner until state stabilized.

Fix: **drop the upfront `emit(LoadingDocument)` / `emit(LoadingChildren)` in `getDocumentFlow` / `getChildrenFlow`**. State stays at the last `DocumentData` / `ChildData` during a refetch; only the new value is emitted. Same value → conflated, no consumer churn. Initial-load loading cursors still come from `loadDocumentAsync` / `loadChildrenAsync` in the provider when state hasn't reached the right `DocumentData` yet.

---

## 7. Notification cheat sheet

| Operation | Notify document URI | Notify children URI of parent |
|---|---|---|
| `createDocument` (folder) | ✅ on placeholder id (after `completeFolderCreation` → state.first wait) | ✅ once SDK has folder + state catches up |
| `createDocument` (file) | — (placeholder transitions naturally on listing re-query) | ✅ immediately (placeholder visible) and ✅ after `awaitFileFinalized` (real file replaces placeholder) |
| `createDocument` (nested under placeholder folder) | — | ✅ on the placeholder folder URI (only relevant if the user is viewing into the placeholder, which SAF doesn't do during a copy) |
| `renameDocument` | — (handled by waiting for state and returning the unchanged id) | ✅ |

Rules of thumb:

- **Notify the children URI for any change to the listing.** That cursor is observed.
- **Don't rely on `notifyDocumentChanged` for synchronous re-queries.** `DocumentInfo.fromUri` is one-shot — the data must be fresh in `state.value` *now*, not after a notify.
- **Always set `EXTRA_LOADING` on a cursor that is waiting for data**, so SAF holds the cursor open and re-queries on notify.

---

## 8. Common pitfalls (the ones that bit us)

1. **`runBlocking` on the binder thread**: fine — SAF binder methods are allowed to block. But the work itself runs on `openDocumentDispatcher` (a daemon thread pool). Don't run SDK calls on the binder thread directly.
2. **Returning `null` from `renameDocument`**: causes `DocumentInfo.fromUri(null)` NPE in DocumentsUI → "Failed to rename".
3. **Synthesising rows for pending entries**: necessary so the placeholder appears in the listing immediately. File placeholders need `FLAG_SUPPORTS_WRITE`; folder placeholders need `FLAG_DIR_SUPPORTS_CREATE` (so SAF treats them as upload destinations during recursive folder copies).
4. **`monitorNodeUpdates` is noisy**: it fires for any node update, not just yours. Don't restart loading state on every emission.
5. **Uploads can take a long time**: never `withTimeout` on the binder thread; the upload watcher runs in `applicationScope.launch` with a 30-minute ceiling, fully off-thread.
6. **`pendingCreates` is in-memory**: process death loses placeholders. Uploads in flight on the SDK continue and the file appears via the normal SDK update path on the next session. Acceptable.
7. **Using `ContentProvider.getContext().applicationContext` for system services**: don't. Use the provider's own `context`.
8. **Mockito + value-class suspend returns**: `wheneverBlocking { fn }.thenThrow(X)` for a suspend fn returning `NodeId` is broken — Mockito returns null, Kotlin unboxes, caller sees NPE. Use `.thenAnswer { throw X }` and wrap the production call in `runCatching`/`getOrElse`.
9. **Filtering placeholder folders out of listings**: once `completeFolderCreation` populates `pendingFolderRealNodeId[pid]`, the real folder is in `ChildData.children`. `getPendingChildrenForParent` must skip those entries to avoid showing both rows.

---

## 9. Files & key symbols

| File | Symbol | Purpose |
|---|---|---|
| `CloudDriveDocumentProvider.kt` | `createDocument` | Synchronously registers pending; for folders, launches `completeFolderCreation`+notify; for files, immediate notify + `awaitFileFinalized` + notify. |
| `CloudDriveDocumentProvider.kt` | `renameDocument` | `runBlocking { dataProvider.renameDocument(...) }` → notify children → return `documentId`. |
| `CloudDriveDocumentProvider.kt` | `openDocument` | Read mode → `openDocumentFile`; write mode (pending id) → scratch PFD with `writeCloseHandler` → `onWriteScratchClosed`. |
| `CloudDriveDocumentDataProvider.kt` | `registerPendingFolder` / `registerPendingFile` | Insert into `pendingCreates`; both go through `registerPendingCreate` → `resolveParentNodeId` (handles placeholder parents). |
| `CloudDriveDocumentDataProvider.kt` | `completeFolderCreation` | SDK call; on success populates `pendingFolderRealNodeId` + completes signal + waits on state; on failure removes placeholder + completes signal exceptionally. |
| `CloudDriveDocumentDataProvider.kt` | `resolvePendingFolderParent` | Resolves a placeholder folder document id to its real `NodeId` (suspending until known). Used for nested folder uploads. |
| `CloudDriveDocumentDataProvider.kt` | `onWriteScratchClosed` | `startUploadUseCase` + state wait, then `finalizePendingFile` in `finally`. |
| `CloudDriveDocumentDataProvider.kt` | `awaitFileFinalized` / `finalizePendingFile` | `CompletableDeferred` per `pendingId`. |
| `CloudDriveDocumentDataProvider.kt` | `renameDocument` | SDK rename → `requestFlow.emit(Document)` → `state.first { DocumentData(newName) }` → return parent docId. |
| `CloudDriveDocumentDataProvider.kt` | `getPendingChildrenForParent` | Synthesised placeholder rows for the parent listing; skips folder placeholders whose real NodeId is known. |
| `PendingCreate.kt` | `parentNodeId, parentDocumentId, displayName, mimeType` | Carried by `pendingCreates`. |

---

## 10. Quick test recipes

- **Rename UI race**: rename a file in a folder you have open. The toast should be a success snack, no "Failed to rename". The listing should reflect the new name within ~100 ms.
- **Create folder reload**: tap "New folder" in DocumentsUI. The new folder should appear in the listing without navigating away.
- **Upload single file placeholder lifetime**: copy a large file (~50 MB) to a MEGA SAF location from another app. The placeholder should appear immediately, persist through the entire upload, and be replaced (not duplicated) by the real file when the SDK signals completion.
- **Upload folder with nested files**: copy `parent/sub/file.txt` from another SAF source. Both `parent` and `sub` should appear as placeholders, then resolve to real folders; `file.txt` should upload under `sub` (verify in MEGA web UI that the parent path is correct).
- **Process death mid-upload**: same as single-file upload but force-kill MEGA after the placeholder appears. Reopen Files; the placeholder is gone, the SDK transfer continues, and the real file appears via the normal listing refresh.
