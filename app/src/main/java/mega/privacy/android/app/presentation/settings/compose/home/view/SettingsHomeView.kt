package mega.privacy.android.app.presentation.settings.compose.home.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowWidthSizeClass
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.MegaOutlinedButton
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.compose.container.view.SettingContainerView
import mega.privacy.android.app.presentation.settings.compose.home.model.SettingsHomeState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.settings.SettingEntryPoint
import mega.privacy.mobile.analytics.event.SettingsScreenEvent

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SettingsHomeView(
    state: SettingsHomeState,
    onBackPressed: () -> Unit,
    initialScreen: SettingEntryPoint.NavData?,
) {
    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(SettingsScreenEvent)
    }

    val navigator = rememberListDetailPaneScaffoldNavigator<SettingEntryPoint.NavData>()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(initialScreen) {
        initialScreen?.let {
            navigator.navigateTo(
                pane = ListDetailPaneScaffoldRole.Detail,
                contentKey = it
            )
        }
    }

    BackHandler(navigator.canNavigateBack()) {
        coroutineScope.launch {
            navigator.navigateBack()
        }
    }

    val detailNavigator = rememberNavController()

    ListDetailPaneScaffold(
        modifier = Modifier,
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                ListContent(
                    state = state,
                    onNavigateToItem = { settingEntryPoint ->
                        // Navigate to the detail pane with the passed item
                        coroutineScope.launch {
                            navigator.navigateTo(
                                pane = ListDetailPaneScaffoldRole.Detail,
                                contentKey = settingEntryPoint.navData
                            )
                        }
                    },
                    onBackPressed = onBackPressed
                )
            }
        },
        detailPane = {
            AnimatedPane {
                // Show the detail pane content if selected item is available
                val windowWidthSize =
                    currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
                val backVisible = when (windowWidthSize) {
                    WindowWidthSizeClass.EXPANDED -> false
                    else -> true
                }

                val settingEntryPoint = navigator.currentDestination?.contentKey
                val title = settingEntryPoint?.title?.let { stringResource(it) } ?: "Empty"
                MegaScaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        MegaTopAppBar(
                            modifier = Modifier.statusBarsPadding(),
                            navigationType = if (backVisible) AppBarNavigationType.Back {
                                coroutineScope.launch {
                                    navigator.handleDetailBackAction(
                                        detailNavigator
                                    )
                                }
                            } else AppBarNavigationType.None,
                            title = title,
                        )
                    }
                ) { paddingValues ->
                    SettingContainerView(
                        navHostController = detailNavigator,
                        destination = settingEntryPoint?.destination,
                        modifier = Modifier.padding(paddingValues)
                    )
                }

            }
        },
    )


}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private suspend fun ThreePaneScaffoldNavigator<*>.handleDetailBackAction(
    detailNavigator: NavHostController,
) {
    if (detailNavigator.navigateUp().not()) {
        navigateBack()
    }
}

@Composable
private fun ListContent(
    state: SettingsHomeState,
    onNavigateToItem: (SettingEntryPoint) -> Unit,
    onBackPressed: () -> Unit,
) {
    var pageTitle by remember { mutableIntStateOf(R.string.action_settings) }

    MegaScaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
        topBar = {
            MegaTopAppBar(
                title = stringResource(pageTitle),
                navigationType = AppBarNavigationType.Back(onBackPressed),
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                MegaText(text = "My Account", textColor = TextColor.Primary)
            }
            when (state) {
                is SettingsHomeState.Data -> myAccountSettingsEntryView(
                    data = state.myAccountState,
                )

                is SettingsHomeState.Loading -> myAccountSettingsEntryLoadingView()
            }

            item {
                MegaText(text = "Features", textColor = TextColor.Primary)
            }

            listEntryPoints(state.featureEntryPoints, onNavigateToItem)

            item {
                MegaText(text = "More", textColor = TextColor.Primary)
            }
            listEntryPoints(state.moreEntryPoints, onNavigateToItem)

            item {
                MegaOutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = "Log out",
                    onClick = {}
                )
            }
        }
    }
}

private fun LazyListScope.listEntryPoints(
    items: ImmutableList<SettingEntryPoint>,
    onNavigateToItem: (SettingEntryPoint) -> Unit,
) {
    items(items) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clickable {
                    Analytics.tracker.trackEvent(it.analyticsEvent)
                    onNavigateToItem(it)
                }
                .testTag(SETTING_ITEM_TEST_TAG_ROOT + it.navData.key),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                MegaIcon(
                    painter = painterResource(it.navData.icon),
                    tint = IconColor.Primary,
                    contentDescription = null
                )
                MegaText(text = stringResource(it.navData.title), textColor = TextColor.Primary)
            }
            MegaIcon(
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ChevronRight),
                tint = IconColor.Primary,
                contentDescription = null,
            )
        }
    }
}


@Composable
@Preview
private fun SettingsHomeViewPreview() {
    AndroidThemeForPreviews {
        SettingsHomeView(
            state = SettingsHomeState.Loading(
                persistentListOf(),
                persistentListOf(),
            ),
            onBackPressed = {},
            initialScreen = null,
        )
    }
}

internal const val SETTING_ITEM_TEST_TAG_ROOT = "settings_home_view:setting_item_"