# CLAUDE.md

This file provides guidance to Claude Code when working in the `:core:ui-components:node-components` module.

> Module path: `:core:ui-components:node-components` · Build file: `core/ui-components/node-components/node-components.gradle.kts` · Namespace: `mega.privacy.android.core.nodecomponents`

> ⚠️ **Deprecated module — do not add new code here.** New node UI, components, mappers, menus, and action handlers belong in `:shared:nodes`. When you touch or reuse anything in this module, take the opportunity to migrate it to `:shared:nodes` (and update its callers) rather than extending it in place. The contents below document the existing code so you can understand and move it — not so you can grow it.

## Overview
A shared Jetpack Compose UI component library for rendering and acting on MEGA nodes (files/folders). It provides the reusable building blocks — list/grid items, node action menus and bottom sheets, node-related dialogs, and the handlers that wire taps to business operations (download, move, share, rename, get-link, hide, offline, etc.).

It is consumed by many feature modules (cloud drive, links, shares, offline, rubbish bin, timeline, video player, chat, recents) that need a consistent way to display nodes and present node actions. The module is presentation-focused: it depends on `:domain` and use cases for behaviour but does not own feature navigation graphs.

## Architecture & Layout
Single source tree under `java/mega/privacy/android/core/nodecomponents`:
- `list/` — node list/grid item composables (`NodeActionListTile`, `MediaGridViewItem`, `UnverifiedContactShareBanner`).
- `components/` — misc UI: `AddContentFab`, `selectionmode/` (`NodeSelectionModeBottomBar`), `offline/` (offline-node action handling + ViewModel).
- `sheet/` — bottom sheets: `nodeactions/` (`NodeMoreOptionsBottomSheet`), `options/` (`NodeOptionsBottomSheet` + ViewModel + navigation), `home/` (`HomeFabOptionsBottomSheet`), `changelabel/`, `upload/` (`UploadOptionsBottomSheet`).
- `dialog/` — node dialogs grouped by action: `rename`, `delete`, `removelink`, `removeshare`, `leaveshare`, `sharefolder`, `openlink`, `storage`.
- `menu/` — the menu system: `menuaction/` (one `*MenuAction` per action), `menuitem/` (`*BottomSheetMenuItem`), `provider/` (per-source menu option providers, e.g. `CloudDriveMenuOptionsProvider`), `registry/` (`NodeMenuProviderRegistry`).
- `action/` — action handlers: `clickhandler/` (one `*ActionClickHandler` per action), `eventhandler/`, plus dispatchers (`SingleNodeActionHandler`, `MultiNodeActionHandler`, `NodeActionHandlerViewModel`, `NodeOptionsActionViewModel`).
- `mapper/` — mappers for destinations, icons, labels, content URIs, view types, offline/zip typed nodes.
- `model/` — UI state holders and menu-item models.
- `scanner/` + `upload/` — document scanner (ML Kit) and upload/capture handlers.
- `di/` — Hilt modules; `navigation/` — `NodeComponentsFeatureDestination`; `extension/` — `TypedNodeExtension`.

## Key Components
- **UI components**: `NodeActionListTile`, `MediaGridViewItem`, `NodeSelectionModeBottomBar`, `AddContentFab` (list/grid + selection mode); `NodeOptionsBottomSheet`, `NodeMoreOptionsBottomSheet`, `HomeFabOptionsBottomSheet`, `UploadOptionsBottomSheet` (sheets); `RenameNodeDialogM3`, `MoveToRubbishOrDeleteNodeDialogM3`, `RemoveNodeLinkDialogM3`, `ShareFolderDialogM3`, `LeaveShareDialogM3`, `OpenLinkDialog`, `StorageStatusDialogViewM3` (dialogs).
- **Menu / actions**: `*MenuAction` + `*BottomSheetMenuItem` + `*ActionClickHandler` triads per node action; per-source `*MenuOptionsProvider` classes registered in `NodeMenuProviderRegistry`.
- **State / models**: `NodeActionState`, `NodeBottomSheetState`, `ScanDocumentUiState`, `OfflineNodeActionsUiState`, `ChangeLabelState`; menu-item models (`NodeBottomSheetMenuItem`, `NodeActionModeMenuItem`, `NodeSelectionMenuItem`); typed-node models (`OfflineTypedNode`, `ZipFileTypedNode`, `RestoreData`).

## Module Dependencies
Project deps: `:domain`, `:shared:nodes`, `:resources:icon-pack`, `:resources:string-resources`, `:core:formatter`, `:navigation`, `:core:ui-components:shared-components`, `:core:feature-flags`, `:core:passcode:passcode`, `:core:navigation-contract`, `:core:coroutine`, `:core:analytics:analytics-tracker`.
External: MEGA core-ui + core-ui-tokens, Compose BOM / Material3 (+ adaptive/window), Navigation Compose & Navigation3, Coil3 (image/thumbnail loading), kotlinx-collections-immutable, kotlinx-serialization, compose-state-events, Timber, ML Kit document scanner. Uses Hilt convention plugin.

## Testing
JUnit 5 + Compose UI test (Truth, Mockito, Turbine via `core-test` / `core-ui-test` / `analytics-test`). Run:
`./gradlew core:ui-components:node-components:testDebugUnitTest`

## Notes & Gotchas
- **Deprecated — build new node UI in `:shared:nodes`, not here.** Do not add new components/actions to this module. If a change forces you to work in here, migrate the affected pieces to `:shared:nodes` and repoint their usages as part of the change.
- Components are reusable and presentation-only — keep feature-specific navigation/graph wiring in the consuming feature module, not here.
- For reference (existing system, not an invitation to extend it): a node action here is a triad — a `*MenuAction`, a `*BottomSheetMenuItem`, and a `*ActionClickHandler`, exposed via the relevant `*MenuOptionsProvider` (registered in `NodeMenuProviderRegistry`) and bound in the `di/` Hilt modules. New actions should be built in `:shared:nodes` instead.
- `M3` suffixes mark Material3 versions of dialogs; prefer them for new work.
- Follows the global rules in the root `.claude/CLAUDE.md` (4-space indent, Compose/ViewModel conventions).
