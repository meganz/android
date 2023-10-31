package mega.privacy.android.core.ui.controls.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalElevationOverlay
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.appbar.AppBarForCollapsibleHeader
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.LocalMegaAppBarColors
import mega.privacy.android.core.ui.controls.appbar.LocalMegaAppBarElevation
import mega.privacy.android.core.ui.controls.appbar.MegaAppBarColors
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionString
import mega.privacy.android.core.ui.model.MenuActionWithoutIcon
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.darkColorPalette

/**
 * ScaffoldWithCollapsibleHeader
 *
 * Scaffold wrapper that includes a collapsible header that includes the status bar and will be fade out when collapsed.
 * @param headerBelowAppBar a Composable for the header to be drawn below the app bar. If set, it's assumed that it will have dark content and values for Local providers such as LocalMegaAppBarElevation or LocalMegaAppBarColors will be set to transition the app bar from transparent with light content when expanded to an ordinary app bar when collapsed. Please notice that automation tool (appium) doesn't see anything behind a scaffold, so anything here won't be visible to appium, consider using [headerAboveAppBar] for this.
 * @param topBar top app bar of the screen. Consider using [AppBarForCollapsibleHeader] to have an animated title.
 * @param headerAboveAppBar a Composable for the header to be drawn above the app bar. Usually have a transparent background to don't completely hide the [topBar]. Consider using [CollapsibleHeaderWithTitle] to have an animated title.
 * @param modifier the [Modifier] to be applied to the layout
 * @param snackbarHost component to host [Snackbar]s that are pushed to be shown via
 * @param content content of your screen.
 */
@Composable
fun ScaffoldWithCollapsibleHeader(
    topBar: @Composable () -> Unit,
    headerAboveAppBar: @Composable (BoxScope.() -> Unit)?,
    modifier: Modifier = Modifier,
    headerBelowAppBar: @Composable (BoxScope.() -> Unit)? = null,
    titleDisplacementFactor: Float = 0.5f,
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    content: @Composable () -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MegaTheme.colors.background.pageBackground)
    ) {
        // calculate the dimensions and colors that are derived from scrollState
        val density = LocalDensity.current.density
        val iconTintColorBase = MegaTheme.colors.icon.primary
        val titleColorBase = MegaTheme.colors.text.primary
        val targetAppBarElevation = LocalMegaAppBarElevation.current
        val statusBarHeight: Float =
            WindowInsets.Companion.statusBars.getTop(LocalDensity.current).toFloat()
        val hasHeaderBelow = headerBelowAppBar != null

        val headerHeight by remember {
            derivedStateOf {
                (headerMaxHeight(statusBarHeight) - (scrollState.value / density))
                    .coerceAtLeast(headerMinHeight(statusBarHeight))
            }
        }

        val headerAlpha by remember {
            derivedStateOf {
                ((headerHeight - headerGoneHeight(statusBarHeight))
                        / (headerStartGoneHeight(statusBarHeight) - headerGoneHeight(statusBarHeight)))
                    .coerceIn(0f, 1f)
            }
        }
        val topBarBackgroundAlpha by remember(hasHeaderBelow) {
            derivedStateOf {
                if (hasHeaderBelow) {
                    (topBarOpacityTransitionDelta(statusBarHeight, headerHeight) * 10)
                        .coerceIn(0f, 1f)
                } else {
                    1f
                }
            }
        }

        val elevation by remember {
            derivedStateOf {
                val elevationFactor = ((topBarOpacityTransitionDelta(
                    statusBarHeight,
                    headerHeight
                ) - 0.1f) * 2).coerceIn(0f, 1f)
                targetAppBarElevation * elevationFactor
            }
        }

        val iconTintColor by remember(hasHeaderBelow) {
            derivedStateOf {
                if (hasHeaderBelow) {
                    lerp(iconTintColorBase, darkColorPalette.icon.primary, headerAlpha)
                } else {
                    iconTintColorBase
                }
            }
        }
        val titleColor by remember(hasHeaderBelow) {
            derivedStateOf {
                if (hasHeaderBelow) {
                    lerp(titleColorBase, darkColorPalette.text.primary, headerAlpha)
                } else {
                    titleColorBase
                }
            }
        }
        val subtitleColor by remember(hasHeaderBelow) {
            derivedStateOf {
                if (hasHeaderBelow) {
                    titleColor
                } else {
                    Color.Unspecified
                }
            }
        }
        val collapsibleHeaderTitleTransition by remember {
            derivedStateOf {
                //The offset is 0 when it's collapsed, and it grows [titleDisplacementFactor] of the expansion when expanded.
                val offset = ((headerHeight - headerGoneHeight(statusBarHeight))
                        * titleDisplacementFactor).coerceAtLeast(0f).dp
                // alpha transition will be done in last 20dp, title alpha will start earlier because we want the title to be opaque all the time (to views with 0.5f alpha are like 0.75 alpha, not 1f alpha)
                val startShowingToolbarTitle = 20.dp
                val startHidingHeaderTitle = 15.dp
                val headerTitleAlpha = (offset / startHidingHeaderTitle).coerceIn(0f, 1f)
                val toolbarTitleAlpha = 1 -
                        ((offset - startHidingHeaderTitle) / (startShowingToolbarTitle - startShowingToolbarTitle))
                            .coerceIn(0f, 1f)
                CollapsibleHeaderTitleTransition(offset, headerTitleAlpha, toolbarTitleAlpha)
            }
        }

        //set the status bar color to match toolbar color
        if (!LocalView.current.isInEditMode) {
            val statusColor = MaterialTheme.colors.surface
                .surfaceColorAtElevation(absoluteElevation = elevation)
                .copy(alpha = topBarBackgroundAlpha)
            val systemUiController = rememberSystemUiController()
            DisposableEffect(systemUiController, statusColor) {
                systemUiController.setStatusBarColor(
                    color = statusColor,
                    darkIcons = systemUiController.statusBarDarkContentEnabled
                )
                onDispose { }
            }
        }

        //draw the composables
        if (headerBelowAppBar != null && headerAlpha > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight.dp)
                    .alpha(headerAlpha)
            ) {
                CompositionLocalProvider(
                    LocalMegaAppBarColors provides MegaAppBarColors(
                        iconsTintColor = iconTintColor,
                        titleColor = titleColor,
                        subtitleColor = subtitleColor,
                        backgroundAlpha = topBarBackgroundAlpha,
                    ),
                    LocalCollapsibleHeaderTitleTransition provides collapsibleHeaderTitleTransition,
                ) {
                    headerBelowAppBar()
                }
            }
        }
        Scaffold(
            modifier = Modifier.systemBarsPadding(),
            backgroundColor = Color.Transparent,
            topBar = {
                CompositionLocalProvider(
                    LocalMegaAppBarColors provides MegaAppBarColors(
                        iconsTintColor = iconTintColor,
                        titleColor = titleColor,
                        subtitleColor = subtitleColor,
                        backgroundAlpha = topBarBackgroundAlpha,
                    ),
                    LocalMegaAppBarElevation provides elevation,
                    LocalCollapsibleHeaderTitleTransition provides collapsibleHeaderTitleTransition,
                ) {
                    topBar()
                }
            },
            snackbarHost = snackbarHost,
        ) { innerPadding ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                //BoxWithConstraints to set the minimum height of the colum so it's always possible to collapse the header
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(innerPadding)
                ) {
                    Spacer(Modifier.height(spacerHeight(statusBarHeight).dp)) //to give space for the header (that it's outside this column)
                    Box(
                        modifier = Modifier
                            .heightIn(min = this@BoxWithConstraints.maxHeight),
                    ) {
                        content()
                    }
                }
            }
        }
        if (headerAboveAppBar != null && headerAlpha > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight.dp)
                    .alpha(collapsibleHeaderTitleTransition.headerAlpha)
            ) {
                CompositionLocalProvider(
                    LocalMegaAppBarColors provides MegaAppBarColors(
                        iconsTintColor = iconTintColor,
                        titleColor = titleColor,
                        subtitleColor = subtitleColor,
                        backgroundAlpha = topBarBackgroundAlpha,
                    ),
                    LocalCollapsibleHeaderTitleTransition provides collapsibleHeaderTitleTransition,
                ) {
                    headerAboveAppBar()
                }
            }
        }
    }
}

/**
 * Values for animating the transition between the title in the header (when expanded) and the title in the toolbar (when collapsed).
 * @param offset the y offset of the title respect toolbar ordinary position
 * @param headerAlpha the alpha of the title in the header
 * @param toolbarAlpha the alpha of the title in the toolbar
 */
internal data class CollapsibleHeaderTitleTransition(
    val offset: Dp,
    val headerAlpha: Float,
    val toolbarAlpha: Float,
)

internal val LocalCollapsibleHeaderTitleTransition =
    compositionLocalOf { CollapsibleHeaderTitleTransition(0.dp, 0f, 1f) }

internal const val APP_BAR_HEIGHT = 56f
private fun headerMinHeight(statusBarHeight: Float) = APP_BAR_HEIGHT + statusBarHeight
private fun headerMaxHeight(statusBarHeight: Float) = headerMinHeight(statusBarHeight) + 96f
private fun headerStartGoneHeight(statusBarHeight: Float) = headerMinHeight(statusBarHeight) + 76f
private fun headerGoneHeight(statusBarHeight: Float) = headerMinHeight(statusBarHeight) + 18f

private fun topBarOpacityTransitionDelta(statusBarHeight: Float, headerHeight: Float) =
    1 - ((headerHeight - headerMinHeight(statusBarHeight))
            / (headerGoneHeight(statusBarHeight) - headerMinHeight(statusBarHeight)))
        .coerceIn(0f, 1f)

private fun spacerHeight(statusBarHeight: Float) =
    headerMaxHeight(statusBarHeight) - APP_BAR_HEIGHT - statusBarHeight

@Composable
private fun Color.surfaceColorAtElevation(
    absoluteElevation: Dp,
): Color = LocalElevationOverlay.current?.apply(this, absoluteElevation) ?: this


@CombinedThemePreviews
@Composable
private fun ScaffoldWithCollapsibleHeaderPreview(
    @PreviewParameter(BooleanProvider::class) hasHeaderBelowAppbar: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val title = "Title very long that can take up to 3 lines when the header is expanded"
        val appBarType = if (hasHeaderBelowAppbar) AppBarType.NONE else AppBarType.BACK_NAVIGATION
        ScaffoldWithCollapsibleHeader(
            headerBelowAppBar = if (!hasHeaderBelowAppbar) null else {
                @Composable {
                    Header()
                }
            },
            topBar = {
                AppBarForCollapsibleHeader(
                    appBarType = appBarType,
                    title = title,
                    actions = getSampleToolbarActions(),
                )
            },
            headerAboveAppBar = {
                CollapsibleHeaderWithTitle(appBarType = appBarType, title = title) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Header above app bar",
                            color = MegaTheme.colors.text.primary,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp)
                        )
                    }
                }
            },
        ) {
            Content()
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ScaffoldWithCollapsibleHeaderWithSubtitlePreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val title = "Collapsable With Subtitle"
        val subtitle = "Subtitle"
        val appBarType = AppBarType.BACK_NAVIGATION
        val titleIcons: @Composable RowScope.() -> Unit = {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .padding(6.dp)
                    .background(MegaTheme.colors.components.selectionControl, shape = CircleShape)
            )
        }
        ScaffoldWithCollapsibleHeader(
            topBar = {
                AppBarForCollapsibleHeader(
                    appBarType = appBarType,
                    title = title,
                    subtitle = subtitle,
                    actions = getSampleToolbarActions(),
                    titleIcons = titleIcons,
                )
            },
            headerAboveAppBar = {
                CollapsibleHeaderWithTitle(
                    appBarType = appBarType,
                    title = title,
                    subtitle = subtitle,
                    titleIcons = titleIcons,
                ) {
                }
            },
            headerBelowAppBar = {
                Header()
            },
            titleDisplacementFactor = 1f,
        ) {
            Content()
        }
    }
}

@Composable
private fun Content() = Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
) {
    Text("Content", color = MegaTheme.colors.text.primary)
    Text("Content", color = MegaTheme.colors.text.primary)
    Text("Content", color = MegaTheme.colors.text.primary)
    Text("Content", color = MegaTheme.colors.text.primary)
}

@Composable
private fun Header() = Box(
    modifier = Modifier
        .fillMaxSize()
        .background(MegaTheme.colors.background.blur)
)

private fun getSampleToolbarActions(): List<MenuAction> = listOf(
    object : MenuActionString(R.drawable.ic_alert_circle, R.string.password_text, "circle") {},
    object : MenuActionWithoutIcon(R.string.password_text, "password") {},
)