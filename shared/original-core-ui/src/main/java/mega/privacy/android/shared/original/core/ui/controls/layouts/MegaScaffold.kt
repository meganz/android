package mega.privacy.android.shared.original.core.ui.controls.layouts

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.map
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.LocalMegaAppBarElevation
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.buttons.MegaFloatingActionButton
import mega.privacy.android.shared.original.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.BackgroundColor
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.utils.accumulateDirectionalScrollOffsets

/**
 * MegaScaffold is a wrapper around Scaffold that provides a convenient way to show a snackBar from any view inside this scaffold.
 * It also provides the appropriate look and feel,such as background color or snack bar style.
 *
 * @param modifier Modifier
 * @param scaffoldState ScaffoldState
 * @param topBar TopBar, the elevation will be updated if the content is scrolled and the content state is set as [scrollableContentState]
 * @param bottomBar BottomBar
 * @param floatingActionButton FloatingActionButton
 * @param scrollableContentState [ScrollableState] of the content. It will be used to set the [topBar] elevation and to hide the [floatingActionButton] in case [hideFloatingActionButtonOnScrollUp] is true
 * @param scrollableContentIsReversed set to true if the scrollable content associated to [scrollableContentState] is reversed to set the elevation for [topBar] correctly
 * @param hideFloatingActionButtonOnScrollUp if true and the scroll state is set as [scrollableContentState] the fab button will be hidden when the content is scrolled down and shown when is scrolled up again
 * @param content content of your screen. The lambda receives an [PaddingValues] that should be
 * applied to the content root via Modifier.padding to properly offset top and bottom bars. If
 * you're using VerticalScroller, apply this modifier to the child of the scroller, and not on
 * the scroller itself.
 */
@Composable
fun MegaScaffold(
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    scrollableContentState: ScrollableState? = null,
    scrollableContentIsReversed: Boolean = false,
    hideFloatingActionButtonOnScrollUp: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scaffoldContentScrollOffset by remember(scrollableContentState) {
        derivedStateOf {
            scrollableContentState?.getApproximateScrollOffset() ?: -1
        }
    }
    val isScrolled by remember(scrollableContentIsReversed, scrollableContentState) {
        derivedStateOf {
            if (scrollableContentIsReversed) {
                scrollableContentState?.canScrollForward == true
            } else {
                scrollableContentState?.canScrollBackward == true
            }
        }
    }
    CompositionLocalProvider(
        LocalSnackBarHostState provides scaffoldState.snackbarHostState,
        LocalMegaAppBarElevation provides if (isScrolled) AppBarDefaults.TopAppBarElevation else 0.dp,
    ) {
        if (hideFloatingActionButtonOnScrollUp) {
            var isFabVisible by remember { mutableStateOf(true) }

            val localDensity = LocalDensity.current
            LaunchedEffect(Unit) {
                snapshotFlow { scaffoldContentScrollOffset }
                    .map { with(localDensity) { it.toDp() } }
                    .accumulateDirectionalScrollOffsets()
                    .collect { accumulatedDelta ->
                        if (!isFabVisible && accumulatedDelta < (-15).dp) isFabVisible = true
                        if (isFabVisible && accumulatedDelta > 15.dp) isFabVisible = false
                    }
            }

            MegaScaffold(
                modifier, scaffoldState, topBar, bottomBar,
                floatingActionButton = {
                    AnimatedVisibility(
                        visible = isFabVisible,
                        enter = scaleIn(animationSpecs, initialScale = animationScale) + fadeIn(
                            animationSpecs
                        ),
                        exit = scaleOut(animationSpecs, targetScale = animationScale) + fadeOut(
                            animationSpecs
                        ),
                    ) {
                        floatingActionButton()
                    }
                },
                content = content,
            )
        } else {
            MegaScaffold(
                modifier, scaffoldState, topBar, bottomBar, floatingActionButton, content
            )
        }
    }
}

@SuppressLint("MegaScaffold")
@Composable
private fun MegaScaffold(
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    CompositionLocalProvider(LocalSnackBarHostState provides scaffoldState.snackbarHostState) {
        Scaffold(
            modifier = modifier,
            scaffoldState = scaffoldState,
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = {
                SnackbarHost(hostState = it) { data ->
                    MegaSnackbar(snackbarData = data)
                }
            },
            floatingActionButton = floatingActionButton,
            backgroundColor = MegaOriginalTheme.colors.background.pageBackground,
            content = content
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MegaScaffoldPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        val scrollState = rememberScrollState()
        MegaScaffold(
            modifier = Modifier.background(MegaOriginalTheme.backgroundColor(backgroundColor = BackgroundColor.PageBackground)),
            topBar = { MegaAppBar(appBarType = AppBarType.NONE, title = "Top bar title") },
            floatingActionButton = { MegaFloatingActionButton(onClick = { }) {} },
            hideFloatingActionButtonOnScrollUp = true,
            scrollableContentState = scrollState,
        )
        {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                (0..100).forEach {
                    MegaText(
                        text = "Item - $it",
                        textColor = TextColor.Primary,
                        Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                    )
                }
            }
        }
    }
}

private fun ScrollableState.getApproximateScrollOffset(): Int? =
    when (this) {
        is ScrollState -> value
        is LazyListState -> {
            firstVisibleItemIndex *
                    (layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1) +
                    firstVisibleItemScrollOffset
        }

        is LazyGridState -> {
            firstVisibleItemIndex *
                    (layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height ?: 1) +
                    firstVisibleItemScrollOffset
        }

        else -> null
    }

/**
 * Provides SnackbarHostState to be used to show a snackBar from any view inside this scaffold.
 * This is a convenient accessor to [ScaffoldState.snackbarHostState] without the need to send it to all the view hierarchy
 */
val LocalSnackBarHostState = compositionLocalOf<SnackbarHostState?> { null }

private const val animationScale = 0.2f
private const val animationDuration = 300
private val animationSpecs = TweenSpec<Float>(durationMillis = animationDuration)

