package mega.privacy.mobile.home.presentation.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.MegaReorderableLazyColumn
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.configuration.model.HomeConfigurationUiState
import mega.privacy.mobile.home.presentation.configuration.model.WidgetConfigurationItem

@Composable
fun HomeConfigurationScreen(
    state: HomeConfigurationUiState,
    onWidgetEnabledChange: (WidgetConfigurationItem, Boolean) -> Unit,
    onWidgetOrderChange: (orderedItems: List<WidgetConfigurationItem>) -> Unit,
    onBack: () -> Unit,
) {
    MegaScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.home_configuration_screen_toolbar_title),
                subtitle = stringResource(sharedR.string.home_configuration_screen_toolbar_subtitle),
                navigationType = AppBarNavigationType.Back(onBack),
                actions = listOf(
                    MenuActionWithClick(CommonMenuAction.More) {
                        // Todo: show bottom sheet to reset and choose default start screen
                    }
                )
            )
        }
    ) { paddingValues ->
        when (state) {
            HomeConfigurationUiState.Loading -> {}
            is HomeConfigurationUiState.Data -> HomeConfigurationContentView(
                modifier = Modifier.padding(paddingValues),
                state = state,
                onWidgetEnabledChange = onWidgetEnabledChange,
                onWidgetOrderChange = onWidgetOrderChange,
            )
        }
    }

}

@Composable
fun HomeConfigurationContentView(
    state: HomeConfigurationUiState.Data,
    onWidgetEnabledChange: (WidgetConfigurationItem, Boolean) -> Unit,
    onWidgetOrderChange: (List<WidgetConfigurationItem>) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    var currentItems by remember(state.widgets) { mutableStateOf(state.widgets.sortedBy { it.index }) }
    var draggedWidget by remember { mutableStateOf<WidgetConfigurationItem?>(null) }

    MegaReorderableLazyColumn(
        items = currentItems,
        lazyListState = lazyListState,
        key = { it.identifier },
        modifier = modifier
            .fillMaxSize()
            .testTag(TEST_TAG_WIDGET_CONFIGURATION_VIEW),
        onMove = { from, to ->
            currentItems = currentItems.toMutableList().apply { move(from.index, to.index) }
        },
        onDragStarted = { dragged, _ ->
            draggedWidget = dragged
        },
        onDragStopped = {
            draggedWidget = null
            onWidgetOrderChange(currentItems)
        },
        dragEnabled = { true }) { item ->

        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MegaIcon(
                painter = rememberVectorPainter(IconPack.Small.Thin.Outline.QueueLine),
                contentDescription = "Reorder icon",
                tint = IconColor.Secondary,
                modifier = Modifier.size(16.dp)
            )
            MegaText(
                text = item.name.text, modifier = Modifier.weight(1f)
            )
            Toggle(
                isEnabled = state.allowRemoval || item.enabled.not(),
                isChecked = item.enabled,
                onCheckedChange = { onWidgetEnabledChange(item, it) },
            )
        }
    }
}

fun <T> MutableList<T>.move(fromIndex: Int, toIndex: Int) {
    if (fromIndex == toIndex) return
    val item = removeAt(fromIndex)
    add(if (toIndex > fromIndex && toIndex != size) toIndex - 1 else toIndex, item)
}

const val TEST_TAG_WIDGET_CONFIGURATION_VIEW = "widget_configuration:list"
const val TEST_TAG_WIDGET_CONFIGURATION_ITEM = "widget_configuration:item_"
