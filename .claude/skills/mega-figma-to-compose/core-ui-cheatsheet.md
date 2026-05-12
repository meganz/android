# MEGA Core-UI Cheatsheet for Figma → Compose

Lookup table used by the `figma-to-mega-compose` skill at workflow step 5. Add to this file whenever you discover a new mapping during a translation. Do not duplicate entries — update the existing row.

## Component mapping

| Figma node / concept | Compose component | Module / package | Notes |
|---|---|---|---|
| Top app bar (back / title / subtitle / overflow) | `MegaTopAppBar(title, subtitle, navigationType, actions)` | `mega.android.core.ui.components.toolbar` | Use `AppBarNavigationType.Back(onBack)` for the back arrow; pass `actions = listOf(MenuActionWithClick(CommonMenuAction.More, onMore))` for `more-vertical`. |
| Page Scaffold | `MegaScaffold(modifier, topBar, bottomBar, content)` | `mega.android.core.ui.components` | Always prefer over Material `Scaffold`. |
| Switch / Toggle | `Toggle(isChecked, onCheckedChange)` | `mega.android.core.ui.components.toggle` | ✅ Code Connect mapped. |
| Bottom navigation bar | `NavigationBottomBar { NavigationBottomBarItem(defaultIcon, selectedIcon, label, isSelected, onClick) }` | `mega.android.core.ui.components.bottombar` | ✅ Code Connect mapped. Usually owned by the global Scaffold, not per-screen. |
| Reorderable / drag-to-sort list | `MegaReorderableLazyColumn(items, lazyListState, key, onMove, onDragStarted, onDragStopped, dragEnabled)` | `mega.android.core.ui.components.list` | `DragDropListView` is **deprecated** — do not use. |
| Generic body text | `MegaText(text, textColor = TextColor.Primary)` | `mega.android.core.ui.components` | Honor design system typography by relying on `TextColor`, not raw `Color`. |
| Generic icon | `MegaIcon(painter, contentDescription, tint = IconColor.Primary)` | `mega.android.core.ui.components.image` | `tint` accepts `IconColor.Primary / Secondary / Accent / …`. |
| Two-line list item (title + subtitle, optional leading/trailing) | `GenericTwoLineListItem(title, subtitle, icon, trailingIcons, onItemClicked)` | `mega.privacy.android.shared.original.core.ui.controls.lists` | Useful when row needs trailing toggle / icon-button. |
| One-line list item (menu item, simple row) | `OneLineListItem(text, onClickListener, modifier)` | `mega.android.core.ui.components.list` | Default for bottom-sheet menu rows. |
| Modal bottom sheet | `MegaModalBottomSheet(sheetState, bottomSheetBackground, onDismissRequest, content)` | `mega.android.core.ui.components.sheets` | Background: `MegaModalBottomSheetBackground.Surface1`. |
| Drag handle (`queue-line`, 16dp) | `IconPack.Small.Thin.Outline.QueueLine` | `mega.privacy.android.icon.pack` | tint = `IconColor.Secondary`, size = 16.dp. |
| Back arrow | `IconPack.Medium.Thin.Outline.ArrowLeft` | same | Usually rendered automatically by `AppBarNavigationType.Back`. |
| Overflow / more-vertical | `IconPack.Medium.Thin.Outline.MoreVertical` | same | Usually rendered automatically by `CommonMenuAction.More`. |
| Page background color | `DSTokens.colors.background.pageBackground` | `mega-core-ui-tokens` | — |
| Text primary color | `DSTokens.colors.text.primary` (or `TextColor.Primary`) | same | Prefer `TextColor` enum when used inside `MegaText`. |
| Icon secondary color | `DSTokens.colors.icon.secondary` (or `IconColor.Secondary`) | same | Prefer `IconColor` enum when used inside `MegaIcon`. |

## Icon naming convention

Figma icon node names map to `IconPack.<Size>.<Weight>.<Style>.<PascalName>`.

| Figma icon name | IconPack path |
|---|---|
| `arrow-left` (24dp medium) | `IconPack.Medium.Thin.Outline.ArrowLeft` |
| `more-vertical` (24dp medium) | `IconPack.Medium.Thin.Outline.MoreVertical` |
| `queue-line` (16dp small) | `IconPack.Small.Thin.Outline.QueueLine` |

If a Figma icon has no `IconPack.*` equivalent, **stop and ask** before importing a new vector — adding to the icon pack is owned by the design-system team.

## Worked example — Customise Home screen

The Figma file [`AND Home Phase 2 → 6416:47936` "Customise Home"] translates 1:1 to existing code at:

`feature/home/home/src/main/java/mega/privacy/mobile/home/presentation/configuration/HomeConfigurationScreen.kt`

A standalone, ViewModel-free preview that demonstrates the translation pattern:

```kotlin
@Composable
fun CustomiseHomePreview(
    sections: List<HomeSection>,
    onBack: () -> Unit,
    onMore: () -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onToggle: (HomeSection, Boolean) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    MegaScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MegaTopAppBar(
                title = "Customise Home",
                subtitle = "Drag sections to reorder",
                navigationType = AppBarNavigationType.Back(onBack),
                actions = listOf(MenuActionWithClick(CommonMenuAction.More, onMore)),
            )
        },
    ) { padding ->
        MegaReorderableLazyColumn(
            items = sections,
            lazyListState = lazyListState,
            key = { it.id },
            modifier = Modifier.fillMaxSize().padding(padding),
            onMove = { from, to -> onMove(from.index, to.index) },
            onDragStarted = { _, _ -> },
            onDragStopped = { },
            dragEnabled = { it.id != "shortcuts" }, // Figma: first item handle is opacity-0
        ) { item ->
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MegaIcon(
                    painter = rememberVectorPainter(IconPack.Small.Thin.Outline.QueueLine),
                    contentDescription = "Reorder",
                    tint = IconColor.Secondary,
                    modifier = Modifier.size(16.dp),
                )
                MegaText(text = item.name, modifier = Modifier.weight(1f))
                Toggle(
                    isChecked = item.enabled,
                    onCheckedChange = { onToggle(item, it) },
                )
            }
        }
    }
}
```

**Mapping notes from this example:**

- The Figma top app bar (back + title + subtitle + 3-dots) collapses to a single `MegaTopAppBar` call — no need to assemble status bar / leading-icon / title manually.
- The Figma drag handle is **16dp**, tinted secondary — use `IconPack.Small.Thin.Outline.QueueLine` + `IconColor.Secondary`, never a custom drawable.
- The Figma "Shortcuts" row has `opacity-0` on its leading drag handle. That maps to `dragEnabled = { it.id != "shortcuts" }`, **not** to a hidden composable — `MegaReorderableLazyColumn` natively supports per-item disable.
- The Figma bottom navigation bar (3 tabs) is the global `NavigationBottomBar`. It is supplied by the parent layout; **do not** add it inside this screen's Scaffold.
- Status bar (time + signal + battery) in the Figma frame is a device chrome mock — never translate it to code.

## How to extend this cheatsheet

When you encounter a Figma component without an entry above:

1. Grep `shared/original-core-ui` and `mega-core-ui` (whichever module is on your classpath) for the role (e.g. "Banner", "Chip", "Avatar").
2. Confirm with `Read` that the component takes the right parameters for the Figma design.
3. Add a row to the table above with module, package, and notes.
4. If the component truly does not exist, link the Figma frame in your PR and ask the design-system owners — do not author a one-off.
