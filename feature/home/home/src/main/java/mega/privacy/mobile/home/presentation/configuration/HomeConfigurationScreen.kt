package mega.privacy.mobile.home.presentation.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.MegaReorderableLazyColumn
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.WidgetCustomisationChooseDefaultStartScreenMenuItemEvent
import mega.privacy.mobile.analytics.event.WidgetCustomisationLastWidgetSnackbarDisplayedEvent
import mega.privacy.mobile.analytics.event.WidgetCustomisationMoreMenuToolbarEvent
import mega.privacy.mobile.analytics.event.WidgetCustomisationResetToDefaultMenuItemEvent
import mega.privacy.mobile.analytics.event.WidgetCustomisationScreenEvent
import mega.privacy.mobile.analytics.event.WidgetCustomisationWidgetDisabledEvent
import mega.privacy.mobile.analytics.event.WidgetCustomisationWidgetEnabledEvent
import mega.privacy.mobile.analytics.event.WidgetCustomisationWidgetsReorderedEvent
import mega.privacy.mobile.home.presentation.configuration.model.HomeConfigurationUiState
import mega.privacy.mobile.home.presentation.configuration.model.WidgetConfigurationItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeConfigurationScreen(
    state: HomeConfigurationUiState,
    onWidgetEnabledChange: (WidgetConfigurationItem, Boolean) -> Unit,
    onWidgetOrderChange: (orderedItems: List<WidgetConfigurationItem>) -> Unit,
    showSnackbarMessage: (String) -> Unit,
    onBack: () -> Unit,
    onResetToDefault: () -> Unit,
    onChooseDefaultStartScreen: () -> Unit,
) {
    var showMenuBottomSheet by rememberSaveable { mutableStateOf(false) }
    val menuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val minimumWidgetErrorMessage =
        stringResource(sharedR.string.home_configuration_widget_removal_not_allowed_message)

    LaunchedOnceEffect {
        Analytics.tracker.trackEvent(WidgetCustomisationScreenEvent)
    }

    MegaScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.home_configuration_screen_toolbar_title),
                subtitle = stringResource(sharedR.string.home_configuration_screen_toolbar_subtitle),
                navigationType = AppBarNavigationType.Back(onBack),
                actions = buildList {
                    if (state is HomeConfigurationUiState.Data) {
                        add(
                            MenuActionWithClick(CommonMenuAction.More) {
                                Analytics.tracker.trackEvent(WidgetCustomisationMoreMenuToolbarEvent)
                                showMenuBottomSheet = true
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (state) {
            HomeConfigurationUiState.Loading -> {}
            is HomeConfigurationUiState.Data -> {
                HomeConfigurationContentView(
                    modifier = Modifier.padding(paddingValues),
                    state = state,
                    onWidgetEnabledChange = onWidgetEnabledChange,
                    onWidgetOrderChange = onWidgetOrderChange,
                    onWidgetStateChangeFailed = {
                        Analytics.tracker.trackEvent(
                            WidgetCustomisationLastWidgetSnackbarDisplayedEvent
                        )
                        showSnackbarMessage(minimumWidgetErrorMessage)
                    }
                )

                if (showMenuBottomSheet) {
                    val dismissSheet: (() -> Unit) -> Unit = { callback ->
                        coroutineScope.launch {
                            menuSheetState.hide()
                        }.invokeOnCompletion {
                            showMenuBottomSheet = false
                            callback()
                        }
                    }
                    MegaModalBottomSheet(
                        sheetState = menuSheetState,
                        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
                        onDismissRequest = { showMenuBottomSheet = false },
                    ) {
                        OneLineListItem(
                            modifier = Modifier.testTag(TEST_TAG_MENU_RESET_TO_DEFAULT),
                            text = stringResource(sharedR.string.home_configuration_screen_menu_reset_to_default),
                            onClickListener = {
                                Analytics.tracker.trackEvent(
                                    WidgetCustomisationResetToDefaultMenuItemEvent
                                )
                                dismissSheet(onResetToDefault)
                            },
                        )
                        OneLineListItem(
                            modifier = Modifier.testTag(TEST_TAG_MENU_CHOOSE_DEFAULT_START_SCREEN),
                            text = stringResource(sharedR.string.home_configuration_screen_menu_choose_default_start_screen),
                            onClickListener = {
                                Analytics.tracker.trackEvent(
                                    WidgetCustomisationChooseDefaultStartScreenMenuItemEvent
                                )
                                dismissSheet(onChooseDefaultStartScreen)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeConfigurationContentView(
    state: HomeConfigurationUiState.Data,
    onWidgetEnabledChange: (WidgetConfigurationItem, Boolean) -> Unit,
    onWidgetOrderChange: (List<WidgetConfigurationItem>) -> Unit,
    onWidgetStateChangeFailed: () -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    var draggableItems by remember(state.draggableWidgets) { mutableStateOf(state.draggableWidgets) }
    var orderAtDragStart by remember { mutableStateOf<List<String>?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        state.fixedWidgets.forEach { item ->
            if (item.isConfigurable) {
                WidgetConfigurationRow(
                    item = item,
                    allowRemoval = state.allowRemoval,
                    onEnabledChange = onWidgetEnabledChange,
                    onStateChangeFailed = onWidgetStateChangeFailed,
                )
            }
        }

        MegaReorderableLazyColumn(
            items = draggableItems,
            lazyListState = lazyListState,
            key = { it.identifier },
            modifier = Modifier
                .fillMaxSize()
                .testTag(TEST_TAG_WIDGET_CONFIGURATION_VIEW),
            onMove = { from, to ->
                draggableItems = draggableItems.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
                onWidgetOrderChange(draggableItems)
            },
            onDragStarted = { _, _ ->
                orderAtDragStart = draggableItems.map { it.identifier }
            },
            onDragStopped = {
                if (orderAtDragStart != draggableItems.map { it.identifier }) {
                    Analytics.tracker.trackEvent(WidgetCustomisationWidgetsReorderedEvent)
                }
                orderAtDragStart = null
            },
        ) { item ->
            if (item.isConfigurable) {
                WidgetConfigurationRow(
                    item = item,
                    allowRemoval = state.allowRemoval,
                    onEnabledChange = onWidgetEnabledChange,
                    onStateChangeFailed = onWidgetStateChangeFailed,
                )
            }
        }
    }
}

@Composable
private fun WidgetConfigurationRow(
    item: WidgetConfigurationItem,
    allowRemoval: Boolean,
    onEnabledChange: (WidgetConfigurationItem, Boolean) -> Unit,
    onStateChangeFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .testTag(TEST_TAG_WIDGET_CONFIGURATION_ITEM + item.identifier)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (item.isDraggable) {
            MegaIcon(
                painter = rememberVectorPainter(IconPack.Small.Thin.Outline.QueueLine),
                contentDescription = "Reorder icon",
                tint = IconColor.Secondary,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Spacer(Modifier.size(16.dp))
        }

        MegaText(text = item.name.text, modifier = Modifier.weight(1f))

        Toggle(
            isChecked = item.enabled,
            modifier = Modifier.testTag(TEST_TAG_WIDGET_CONFIGURATION_ITEM_TOGGLE + item.identifier),
            onCheckedChange = {
                if (allowRemoval || !item.enabled) {
                    Analytics.tracker.trackEvent(
                        if (it) {
                            WidgetCustomisationWidgetEnabledEvent(item.identifier)
                        } else {
                            WidgetCustomisationWidgetDisabledEvent(item.identifier)
                        }
                    )
                    onEnabledChange(item, it)
                } else {
                    onStateChangeFailed()
                }
            },
        )
    }
}

const val TEST_TAG_WIDGET_CONFIGURATION_VIEW = "widget_configuration:list"
const val TEST_TAG_WIDGET_CONFIGURATION_ITEM = "widget_configuration:item_"
const val TEST_TAG_WIDGET_CONFIGURATION_ITEM_TOGGLE = "widget_configuration:toggle_"
const val TEST_TAG_MENU_RESET_TO_DEFAULT = "widget_configuration:menu_reset_to_default"
const val TEST_TAG_MENU_CHOOSE_DEFAULT_START_SCREEN =
    "widget_configuration:menu_choose_default_start_screen"
