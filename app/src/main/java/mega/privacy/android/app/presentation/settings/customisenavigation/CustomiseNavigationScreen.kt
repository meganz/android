package mega.privacy.android.app.presentation.settings.customisenavigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.consumed
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.components.card.RoundCard
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.list.MegaReorderableLazyColumn
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.settings.customisenavigation.model.CustomiseNavigationUiState
import mega.privacy.android.app.presentation.settings.customisenavigation.model.MaxSelectableNavigationItems
import mega.privacy.android.app.presentation.settings.customisenavigation.model.MinSelectableNavigationItems
import mega.privacy.android.app.presentation.settings.customisenavigation.model.NavigationItemUiModel
import mega.privacy.android.app.presentation.settings.customisenavigation.model.NavigationSelectionError
import mega.privacy.android.app.presentation.settings.customisenavigation.model.addNavigationItem
import mega.privacy.android.app.presentation.settings.customisenavigation.model.moveNavigationItem
import mega.privacy.android.app.presentation.settings.customisenavigation.model.navigationSelectionError
import mega.privacy.android.app.presentation.settings.customisenavigation.model.removeNavigationItem
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.CustomiseNavigationResetButtonPressedEvent
import mega.privacy.mobile.analytics.event.CustomiseNavigationSaveButtonPressedEvent

/**
 * Test tags for the Customise navigation settings screen.
 */
internal object CustomiseNavigationScreenTestTags {
    private const val PREFIX = "customise_navigation_screen"
    const val PREVIEW_BAR = "$PREFIX:row_preview_bar"
    const val YOUR_NAVIGATION_CARD = "$PREFIX:card_your_navigation"
    const val ITEMS_COUNTER = "$PREFIX:text_items_counter"
    const val AVAILABLE_TO_ADD_CARD = "$PREFIX:card_available_to_add"
    const val SAVE_BUTTON = "$PREFIX:button_save"
    const val RESET_BUTTON = "$PREFIX:button_reset"

    fun navigationItemRow(id: String) = "$PREFIX:item_$id"
}

/**
 * Screen to customise the order and visibility of the main navigation bar items.
 */
@Composable
internal fun CustomiseNavigationScreen(
    state: CustomiseNavigationUiState,
    onBackPressed: () -> Unit,
    onSave: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.settings_customise_navigation_title),
                navigationType = AppBarNavigationType.Back(onBackPressed),
                subtitle = stringResource(sharedR.string.settings_customise_navigation_subtitle),
            )
        },
    ) { paddingValues ->
        when (state) {
            CustomiseNavigationUiState.Loading -> Unit
            is CustomiseNavigationUiState.Data -> CustomiseNavigationContent(
                data = state,
                onSave = onSave,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Composable
private fun CustomiseNavigationContent(
    data: CustomiseNavigationUiState.Data,
    onSave: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingIds by rememberSaveable(saver = PendingIdsSaver) {
        mutableStateOf(data.baseArrangement.map { it.id })
    }
    val allItems = remember(data.baseArrangement, data.availableItems) {
        data.baseArrangement + data.availableItems
    }
    val pendingItems = remember(pendingIds, allItems) {
        pendingIds.mapNotNull { id -> allItems.firstOrNull { it.id == id } }
    }
    val availableItems = remember(pendingIds, allItems) {
        allItems.filterNot { it.id in pendingIds }
    }
    val baseIds = remember(data.baseArrangement) { data.baseArrangement.map { it.id } }

    val snackbarHostState = LocalSnackBarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val maxItemsMessage = stringResource(
        sharedR.string.settings_customise_navigation_max_items_snackbar,
        MaxSelectableNavigationItems,
    )
    val minItemsMessage = stringResource(
        sharedR.string.settings_customise_navigation_min_items_snackbar,
        MinSelectableNavigationItems,
    )

    fun onSaveClicked() {
        Analytics.tracker.trackEvent(CustomiseNavigationSaveButtonPressedEvent)
        when (pendingIds.navigationSelectionError()) {
            NavigationSelectionError.TooFewItems -> coroutineScope.launch {
                snackbarHostState?.showAutoDurationSnackbar(minItemsMessage)
            }

            NavigationSelectionError.TooManyItems -> coroutineScope.launch {
                snackbarHostState?.showAutoDurationSnackbar(maxItemsMessage)
            }

            null -> onSave(pendingIds)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        NavigationPreviewBar(
            items = pendingItems,
            menuItem = data.menuItem,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            YourNavigationCard(
                items = pendingItems,
                menuItem = data.menuItem,
                onMove = { from, to -> pendingIds = pendingIds.moveNavigationItem(from, to) },
                onItemDisabled = { id -> pendingIds = pendingIds.removeNavigationItem(id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (availableItems.isNotEmpty()) {
                AvailableToAddCard(
                    items = availableItems,
                    onItemEnabled = { id -> pendingIds = pendingIds.addNavigationItem(id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        PrimaryFilledButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag(CustomiseNavigationScreenTestTags.SAVE_BUTTON),
            text = stringResource(sharedR.string.general_action_save),
            enabled = pendingIds != baseIds,
            onClick = ::onSaveClicked,
        )
        TextOnlyButton(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
                .testTag(CustomiseNavigationScreenTestTags.RESET_BUTTON),
            text = stringResource(sharedR.string.home_configuration_screen_menu_reset_to_default),
            onClick = {
                Analytics.tracker.trackEvent(CustomiseNavigationResetButtonPressedEvent)
                pendingIds = data.defaultArrangementIds
            },
        )
    }
}

@Composable
private fun NavigationPreviewBar(
    items: List<NavigationItemUiModel>,
    menuItem: NavigationItemUiModel,
    modifier: Modifier = Modifier,
) {
    RoundCard(modifier = modifier.testTag(CustomiseNavigationScreenTestTags.PREVIEW_BAR)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        ) {
            items.forEach { item ->
                NavigationPreviewBarSegment(
                    item = item,
                    isEnabled = true,
                    modifier = Modifier.weight(1f),
                )
            }
            NavigationPreviewBarSegment(
                item = menuItem,
                isEnabled = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NavigationPreviewBarSegment(
    item: NavigationItemUiModel,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MegaIcon(
            imageVector = item.icon,
            tint = if (isEnabled) IconColor.Secondary else IconColor.Disabled,
            modifier = Modifier.size(24.dp),
        )
        MegaText(
            text = stringResource(item.label),
            textColor = if (isEnabled) TextColor.Secondary else TextColor.Disabled,
            style = AppTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun YourNavigationCard(
    items: List<NavigationItemUiModel>,
    menuItem: NavigationItemUiModel,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onItemDisabled: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    RoundCard(modifier = modifier.testTag(CustomiseNavigationScreenTestTags.YOUR_NAVIGATION_CARD)) {
        Column {
            val selectedCount = items.size + 1
            val maxCount = MaxSelectableNavigationItems + 1
            val isSelectionInvalid = items.size < MinSelectableNavigationItems ||
                    items.size > MaxSelectableNavigationItems
            CardHeader(
                title = stringResource(sharedR.string.settings_customise_navigation_your_navigation),
            ) {
                MegaText(
                    modifier = Modifier.testTag(CustomiseNavigationScreenTestTags.ITEMS_COUNTER),
                    text = stringResource(
                        sharedR.string.settings_customise_navigation_items_counter,
                        selectedCount,
                        maxCount,
                    ),
                    textColor = if (isSelectionInvalid) {
                        TextColor.Error
                    } else {
                        TextColor.Secondary
                    },
                    style = AppTheme.typography.bodyMedium,
                )
            }
            SubtleDivider()
            MegaReorderableLazyColumn(
                items = items,
                key = { it.id },
                onMove = { from, to -> onMove(from.index, to.index) },
                modifier = Modifier.height(NavigationItemRowHeight * items.size),
            ) { item ->
                NavigationItemRow(
                    item = item,
                    subtitle = stringResource(sharedR.string.settings_customise_navigation_start_screen)
                        .takeIf { items.firstOrNull()?.id == item.id },
                    isChecked = true,
                    isEnabled = true,
                    showDragHandle = true,
                    onToggled = { onItemDisabled(item.id) },
                )
            }
            NavigationItemRow(
                item = menuItem,
                subtitle = null,
                isChecked = true,
                isEnabled = false,
                showDragHandle = true,
                onToggled = null,
            )
        }
    }
}

@Composable
private fun AvailableToAddCard(
    items: List<NavigationItemUiModel>,
    onItemEnabled: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    RoundCard(
        modifier = modifier.testTag(CustomiseNavigationScreenTestTags.AVAILABLE_TO_ADD_CARD),
    ) {
        Column {
            CardHeader(
                title = stringResource(sharedR.string.settings_customise_navigation_available_to_add),
            )
            SubtleDivider()
            items.forEach { item ->
                NavigationItemRow(
                    item = item,
                    subtitle = null,
                    isChecked = false,
                    isEnabled = true,
                    showDragHandle = false,
                    onToggled = { onItemEnabled(item.id) },
                )
            }
        }
    }
}

@Composable
private fun CardHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaText(
            modifier = Modifier.weight(1f),
            text = title,
            textColor = TextColor.Secondary,
            style = AppTheme.typography.bodyMedium,
        )
        trailingContent()
    }
}

@Composable
private fun NavigationItemRow(
    item: NavigationItemUiModel,
    subtitle: String?,
    isChecked: Boolean,
    isEnabled: Boolean,
    showDragHandle: Boolean,
    onToggled: (() -> Unit)?,
) {
    FlexibleLineListItem(
        modifier = Modifier
            .height(NavigationItemRowHeight)
            .testTag(CustomiseNavigationScreenTestTags.navigationItemRow(item.id)),
        title = stringResource(item.label),
        subtitle = subtitle,
        leadingElement = if (showDragHandle) {
            {
                MegaIcon(
                    imageVector = IconPack.Small.Thin.Outline.QueueLine,
                    tint = if (isEnabled) IconColor.Secondary else IconColor.Disabled,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp),
                )
            }
        } else {
            null
        },
        trailingElement = {
            Toggle(
                isChecked = isChecked,
                onCheckedChange = onToggled?.let { { _ -> it() } },
                isEnabled = isEnabled,
            )
        },
        enableClick = false,
        enable = isEnabled,
    )
}

private val NavigationItemRowHeight = 68.dp

private val PendingIdsSaver = listSaver<MutableState<List<String>>, String>(
    save = { state -> state.value },
    restore = { restored -> mutableStateOf(restored.toList()) },
)

@CombinedThemePreviews
@Composable
private fun CustomiseNavigationScreenDefaultPreview() {
    AndroidThemeForPreviews {
        CustomiseNavigationScreen(
            state = previewData(
                selected = listOf(previewHome, previewDrive, previewMedia),
                available = listOf(previewChat, previewShares),
            ),
            onBackPressed = {},
            onSave = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CustomiseNavigationScreenFullSelectionPreview() {
    AndroidThemeForPreviews {
        CustomiseNavigationScreen(
            state = previewData(
                selected = listOf(previewChat, previewDrive, previewMedia, previewHome),
                available = listOf(previewShares),
            ),
            onBackPressed = {},
            onSave = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CustomiseNavigationScreenLoadingPreview() {
    AndroidThemeForPreviews {
        CustomiseNavigationScreen(
            state = CustomiseNavigationUiState.Loading,
            onBackPressed = {},
            onSave = {},
        )
    }
}

private val previewHome = NavigationItemUiModel(
    id = "home",
    label = sharedR.string.general_section_home,
    icon = IconPack.Medium.Thin.Outline.Home,
)
private val previewDrive = NavigationItemUiModel(
    id = "drive",
    label = sharedR.string.general_drive,
    icon = IconPack.Medium.Thin.Outline.Folder,
)
private val previewMedia = NavigationItemUiModel(
    id = "media",
    label = sharedR.string.media_feature_title,
    icon = IconPack.Medium.Thin.Outline.Image01,
)
private val previewChat = NavigationItemUiModel(
    id = "chat",
    label = sharedR.string.general_chat,
    icon = IconPack.Medium.Thin.Outline.MessageChatCircle,
)
private val previewShares = NavigationItemUiModel(
    id = "shares",
    label = sharedR.string.video_section_videos_location_option_shared_items,
    icon = IconPack.Medium.Thin.Outline.FolderUsers,
)
private val previewMenu = NavigationItemUiModel(
    id = "menu",
    label = sharedR.string.general_menu,
    icon = IconPack.Medium.Thin.Outline.Menu01,
)

private fun previewData(
    selected: List<NavigationItemUiModel>,
    available: List<NavigationItemUiModel>,
) = CustomiseNavigationUiState.Data(
    baseArrangement = selected,
    availableItems = available,
    menuItem = previewMenu,
    defaultArrangementIds = listOf("home", "drive", "media"),
    savedEvent = consumed,
)
