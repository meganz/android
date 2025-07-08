package mega.privacy.android.shared.original.core.ui.controls.buttons

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.core.R
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * MegaMultiFloatingActionButton
 *
 * @param items                     List of [MultiFloatingActionButtonItem]
 * @param modifier                  [Modifier]
 * @param icon                      Icon for the main button
 * @param enabled                   Whether the button is enabled
 * @param showLabels                Whether show items labels
 * @param mainButtonCollapsedStyle  [FloatingActionButtonStyle] of the main button when [MultiFloatingActionButtonState.COLLAPSED]
 * @param mainButtonExpandedStyle   [FloatingActionButtonStyle] of the main button when [MultiFloatingActionButtonState.EXPANDED]
 * @param multiFabState             Initial or current state of the multi FAB
 * @param onStateChanged            State changed listener. Return the current [MultiFloatingActionButtonState]
 */

@Composable
fun MegaMultiFloatingActionButton(
    items: List<MultiFloatingActionButtonItem>,
    modifier: Modifier = Modifier,
    icon: Painter = painterResource(id = R.drawable.ic_plus),
    enabled: Boolean = true,
    showLabels: Boolean = true,
    mainButtonCollapsedStyle: FloatingActionButtonStyle = FloatingActionButtonStyle.Big,
    mainButtonExpandedStyle: FloatingActionButtonStyle = FloatingActionButtonStyle.Medium,
    multiFabState: MutableState<MultiFloatingActionButtonState> = rememberMultiFloatingActionButtonState(),
    onStateChanged: ((state: MultiFloatingActionButtonState) -> Unit)? = null,
) {
    var currentState = multiFabState.value
    val stateTransition: Transition<MultiFloatingActionButtonState> =
        updateTransition(targetState = currentState, label = "Rotation Transition")
    val stateChange: () -> Unit = {
        currentState =
            if (stateTransition.currentState == MultiFloatingActionButtonState.EXPANDED) {
                MultiFloatingActionButtonState.COLLAPSED
            } else MultiFloatingActionButtonState.EXPANDED
        onStateChanged?.invoke(currentState)
    }
    val rotation: Float by stateTransition.animateFloat(
        transitionSpec = {
            if (targetState == MultiFloatingActionButtonState.EXPANDED) {
                spring(stiffness = Spring.StiffnessLow)
            } else {
                spring(stiffness = Spring.StiffnessMedium)
            }
        }, label = "FloatAnimation - Rotation Transition"
    ) { state ->
        if (state == MultiFloatingActionButtonState.EXPANDED) 135f else 0f
    }
    val isExpanded = currentState == MultiFloatingActionButtonState.EXPANDED

    BackHandler(isExpanded) {
        currentState = MultiFloatingActionButtonState.COLLAPSED
        onStateChanged?.invoke(currentState)
    }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom,
    ) {
        items.forEach { item ->
            AnimatedVisibility(visible = isExpanded) {
                MultiFloatingActionButtonItem(
                    item = item,
                    showLabel = showLabels,
                )
            }
        }
        Box(
            modifier = modifier
                .padding(top = 16.dp)
                .size(max(mainButtonCollapsedStyle.size, mainButtonExpandedStyle.size)),
            contentAlignment = Alignment.Center
        ) {
            MegaFloatingActionButton(
                onClick = { stateChange() },
                modifier = Modifier.testTag(tag = MULTI_FAB_MAIN_FAB_TEST_TAG),
                style = if (isExpanded) mainButtonExpandedStyle else mainButtonCollapsedStyle,
                enabled = enabled,
                backgroundColor = if (isExpanded) DSTokens.colors.button.secondary else DSTokens.colors.button.primary
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = if (isExpanded) DSTokens.colors.icon.primary else DSTokens.colors.icon.inverse,
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}

@Composable
private fun MultiFloatingActionButtonItem(
    item: MultiFloatingActionButtonItem,
    showLabel: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 16.dp, end = 4.dp)
            .clickable { if (item.enabled) item.onClicked() }
            .testTag("${MULTI_FAB_OPTION_ROW_TEST_TAG}_${item.label}"),
    ) {
        if (showLabel && item.label.isNotEmpty()) {
            TextButton(
                onClick = item.onClicked,
                modifier = Modifier
                    .defaultMinSize(minHeight = 26.dp, minWidth = 20.dp)
                    .padding(end = 16.dp)
                    .testTag("${MULTI_FAB_OPTION_LABEL_TEST_TAG}_${item.label}"),
                colors = ButtonDefaults.buttonColors(),
                enabled = item.enabled,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item.label,
                    color = DSTokens.colors.text.primary,
                    style = MaterialTheme.typography.subtitle2
                )
            }
        }
        MegaFloatingActionButton(
            onClick = item.onClicked,
            modifier = Modifier.testTag("${MULTI_FAB_OPTION_FAB_TEST_TAG}_${item.label}"),
            style = item.style,
            enabled = item.enabled,
        ) {
            Icon(
                painter = item.icon,
                contentDescription = item.label,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Creates and remember a [MultiFloatingActionButtonState]
 */
@Composable
fun rememberMultiFloatingActionButtonState(
    multiFabState: MultiFloatingActionButtonState = MultiFloatingActionButtonState.COLLAPSED,
): MutableState<MultiFloatingActionButtonState> = remember { mutableStateOf(multiFabState) }

/**
 * State of the multi floating action button
 */
enum class MultiFloatingActionButtonState {
    /**
     * Multi floating action button collapsed status
     */
    COLLAPSED,

    /**
     * Multi floating action button expanded status
     */
    EXPANDED
}

/**
 * Item for a sub-button of a [MegaMultiFloatingActionButton]
 *
 * @param icon      Button icon
 * @param label     Button label
 * @param style     [FloatingActionButtonStyle]
 * @param enabled   Whether the button is enabled
 * @param onClicked Click listener
 */
class MultiFloatingActionButtonItem(
    val icon: Painter,
    val label: String = "",
    val style: FloatingActionButtonStyle = FloatingActionButtonStyle.Small,
    val enabled: Boolean = true,
    val onClicked: () -> Unit
)

@CombinedThemePreviews
@Composable
private fun MegaMultiFloatingActionButtonCollapsedPreview(
    @PreviewParameter(BooleanProvider::class) enabled: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        val multiFabState = rememberMultiFloatingActionButtonState()
        MegaScaffold(
            topBar = { MegaAppBar(appBarType = AppBarType.NONE, title = "Top bar title") },
            floatingActionButton = {
                MegaMultiFloatingActionButton(
                    items = listOf(
                        MultiFloatingActionButtonItem(
                            icon = painterResource(id = iconPackR.drawable.ic_sync_01_medium_thin_outline),
                            label = "Sync",
                            onClicked = {}
                        ),
                        MultiFloatingActionButtonItem(
                            icon = painterResource(id = iconPackR.drawable.ic_database_medium_thin_outline),
                            label = "Backup",
                            onClicked = {}
                        ),
                    ),
                    enabled = enabled,
                    multiFabState = multiFabState,
                    onStateChanged = { state -> multiFabState.value = state }
                )
            },
            blurContent = if (multiFabState.value == MultiFloatingActionButtonState.EXPANDED) { ->
                multiFabState.value = MultiFloatingActionButtonState.COLLAPSED
            } else {
                null
            },
        ) { }
    }
}

@CombinedThemePreviews
@Composable
private fun MegaMultiFloatingActionButtonExpandedPreview(
    @PreviewParameter(BooleanProvider::class) enabled: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        val multiFabState =
            rememberMultiFloatingActionButtonState(MultiFloatingActionButtonState.EXPANDED)
        MegaScaffold(
            topBar = { MegaAppBar(appBarType = AppBarType.NONE, title = "Top bar title") },
            floatingActionButton = {
                MegaMultiFloatingActionButton(
                    items = listOf(
                        MultiFloatingActionButtonItem(
                            icon = painterResource(id = iconPackR.drawable.ic_sync_01_medium_thin_outline),
                            label = "Sync",
                            onClicked = {}
                        ),
                        MultiFloatingActionButtonItem(
                            icon = painterResource(id = iconPackR.drawable.ic_database_medium_thin_outline),
                            label = "Backup",
                            enabled = enabled,
                            onClicked = {}
                        ),
                    ),
                    multiFabState = multiFabState,
                    onStateChanged = { state -> multiFabState.value = state }
                )
            },
            blurContent = if (multiFabState.value == MultiFloatingActionButtonState.EXPANDED) { ->
                multiFabState.value = MultiFloatingActionButtonState.COLLAPSED
            } else {
                null
            },
        ) {}
    }
}

/**
 * Multi FAB main button's test tag
 */
const val MULTI_FAB_MAIN_FAB_TEST_TAG = "multi_fab:main_fab"

/**
 * Multi FAB option row's test tag
 */
const val MULTI_FAB_OPTION_ROW_TEST_TAG = "multi_fab:option_row"

internal const val MULTI_FAB_OPTION_LABEL_TEST_TAG = "multi_fab:option_label"
internal const val MULTI_FAB_OPTION_FAB_TEST_TAG = "multi_fab:option_fab"