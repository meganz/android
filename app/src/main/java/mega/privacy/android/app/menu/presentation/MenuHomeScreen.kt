package mega.privacy.android.app.menu.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.badge.NotificationBadge
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.SecondaryFilledButton
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.list.SecondaryHeaderListItem
import mega.android.core.ui.components.profile.MediumProfilePicture
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.ACCOUNT_ITEM
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.BADGE
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.LOGOUT_BUTTON
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.MY_ACCOUNT_ITEM
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.NOTIFICATION_BADGE
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.NOTIFICATION_ICON
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.PRIVACY_SUITE_HEADER
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.PRIVACY_SUITE_ITEM
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.TOOLBAR
import mega.privacy.android.app.presentation.logout.LogoutConfirmationDialogM3NavKey
import mega.privacy.android.core.sharedcomponents.extension.excludingBottomPadding
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.navigation.contract.NavDrawerItem
import mega.privacy.android.navigation.destination.MyAccountNavKey
import mega.privacy.android.navigation.destination.NotificationsNavKey
import mega.privacy.android.navigation.destination.TestPasswordNavKey
import mega.privacy.android.shared.original.core.ui.utils.composeLet
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.navigation.snowflake.NavigationBadge

@Composable
fun MenuHomeScreen(
    navigateToFeature: (NavKey) -> Unit,
    viewModel: MenuViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    MenuHomeScreenUi(
        uiState = uiState,
        navigateToFeature = navigateToFeature,
        onLogoutClicked = viewModel::logout,
        onResetTestPasswordScreenEvent = viewModel::resetTestPasswordScreenEvent,
        onResetLogoutConfirmationEvent = viewModel::resetLogoutConfirmationEvent
    )
}

@Composable
fun MenuHomeScreenUi(
    uiState: MenuUiState,
    navigateToFeature: (NavKey) -> Unit,
    onLogoutClicked: () -> Unit,
    onResetTestPasswordScreenEvent: () -> Unit,
    onResetLogoutConfirmationEvent: () -> Unit,
) {
    var isPrivacySuiteExpanded by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    EventEffect(
        event = uiState.showTestPasswordScreenEvent,
        onConsumed = onResetTestPasswordScreenEvent
    ) {
        navigateToFeature(TestPasswordNavKey(isLogoutMode = true))
    }

    EventEffect(
        event = uiState.showLogoutConfirmationEvent,
        onConsumed = onResetLogoutConfirmationEvent
    ) {
        navigateToFeature(LogoutConfirmationDialogM3NavKey)
    }

    MegaScaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            MegaTopAppBar(
                modifier = Modifier
                    .testTag(TOOLBAR),
                title = "",
                trailingIcons = {
                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(
                            modifier = Modifier
                                .size(48.dp)
                                .testTag(NOTIFICATION_ICON),
                            onClick = { navigateToFeature(NotificationsNavKey) }
                        ) {
                            MegaIcon(
                                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Bell),
                                tint = IconColor.Primary,
                                contentDescription = "Notification Icon",
                            )
                        }
                        if (uiState.unreadNotificationsCount > 0) {
                            NotificationBadge(
                                uiState.unreadNotificationsCount,
                                modifier = Modifier
                                    .padding(end = 8.dp, top = 4.dp)
                                    .testTag(NOTIFICATION_BADGE),
                            )
                        }
                    }
                },
                navigationType = AppBarNavigationType.None
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .animateContentSize(),
            contentPadding = paddingValues.excludingBottomPadding(),
        )
        {
            item(key = "${uiState.name} ${uiState.email} ${uiState.lastModifiedTime}") {
                FlexibleLineListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(MY_ACCOUNT_ITEM),
                    title = uiState.name.orEmpty(),
                    subtitle = uiState.email.orEmpty(),
                    leadingElement = {
                        MediumProfilePicture(
                            imageFile = uiState.avatar,
                            contentDescription = uiState.name,
                            name = uiState.name,
                            avatarColor = uiState.avatarColor
                        )
                    },
                    trailingElement = {
                        MegaIcon(
                            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ChevronRight),
                            tint = IconColor.Secondary,
                            contentDescription = "arrow right",
                        )
                    },
                    onClickListener = {
                        navigateToFeature(MyAccountNavKey)
                    }
                )
            }

            items(
                items = uiState.myAccountItems.values.toList()
            ) { item ->
                AccountItem(
                    item = item,
                    onNavigate = {
                        navigateToFeature(item.destination)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                PrivacySuiteHeader(isExpanded = isPrivacySuiteExpanded, onClick = {
                    coroutineScope.launch {
                        val wasExpanded = isPrivacySuiteExpanded
                        isPrivacySuiteExpanded = !isPrivacySuiteExpanded

                        // Scroll to bottom only when expanding
                        if (!wasExpanded) {
                            delay(100)
                            val itemCount = listState.layoutInfo.totalItemsCount
                            if (itemCount > 0) {
                                listState.animateScrollToItem(itemCount - 1)
                            }
                        }
                    }
                })
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isPrivacySuiteExpanded) {
                items(
                    items = uiState.privacySuiteItems.values.toList()
                ) { item ->
                    PrivacySuiteItem(
                        item = item,
                        onNavigate = {
                            item.appPackage?.let { appPackage ->
                                openInSpecificApp(
                                    context = context,
                                    link = item.link,
                                    packageName = appPackage
                                )
                            } ?: context.launchUrl(item.link)
                        }
                    )
                }
            }

            item {
                LogoutButton(uiState.isLoggingOut) {
                    onLogoutClicked()
                }
            }
        }
    }
}


@Composable
private fun AccountItem(
    item: NavDrawerItem.Account,
    onNavigate: () -> Unit,
) {
    val hasActionLabel = item.actionLabel != null
    val subtitle by item.subTitle?.collectAsState(null) ?: remember { mutableStateOf(null) }
    val badge by item.badge?.collectAsState(null) ?: remember { mutableStateOf(null) }
    FlexibleLineListItem(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ACCOUNT_ITEM),
        title = stringResource(id = item.title),
        subtitle = subtitle,
        leadingElement = {
            MegaIcon(
                painter = rememberVectorPainter(item.icon),
                tint = IconColor.Primary,
                contentDescription = null
            )
        },
        trailingElement = {
            item.actionLabel?.let {
                PrimaryFilledButton(
                    modifier = Modifier
                        .wrapContentSize(),
                    text = stringResource(id = it),
                    isLoading = false,
                    onClick = onNavigate,
                )
            } ?: MegaIcon(
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ChevronRight),
                tint = IconColor.Primary,
                contentDescription = "arrow right",
            )
        },
        enableClick = hasActionLabel.not(),
        onClickListener = onNavigate,
        titleTrailingElement = badge?.composeLet { count ->
            NavigationBadge(
                navigationBadge = count,
                small = false,
                modifier = Modifier.testTag(BADGE)
            )
        },
    )
}

@Composable
private fun PrivacySuiteHeader(isExpanded: Boolean, onClick: () -> Unit) {
    SecondaryHeaderListItem(
        modifier = Modifier.testTag(PRIVACY_SUITE_HEADER),
        text = stringResource(sharedR.string.general_mega_privacy_suite),
        secondaryRightIconRes = if (isExpanded) IconPackR.drawable.ic_chevron_up_small_regular_outline else IconPackR.drawable.ic_chevron_down_small_regular_outline,
        onClickListener = onClick
    )
}

@Composable
private fun PrivacySuiteItem(
    item: NavDrawerItem.PrivacySuite,
    onNavigate: () -> Unit,
) {
    FlexibleLineListItem(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PRIVACY_SUITE_ITEM),
        title = stringResource(id = item.title),
        subtitle = stringResource(id = item.subTitle),
        leadingElement = {
            MegaIcon(
                painter = rememberVectorPainter(item.icon),
                tint = IconColor.Primary,
                contentDescription = null
            )
        },
        trailingElement = {
            MegaIcon(
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ExternalLink),
                tint = IconColor.Secondary,
                contentDescription = null,
            )
        },
        onClickListener = onNavigate
    )
}

@Composable
private fun LogoutButton(
    isLoggingOut: Boolean,
    onLogoutClicked: () -> Unit,
) {
    SecondaryFilledButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .testTag(LOGOUT_BUTTON),
        text = "Log out",
        onClick = onLogoutClicked,
        isLoading = isLoggingOut
    )
}

@CombinedThemePreviews
@Composable
private fun MenuHomeScreenUiPreview(
    @PreviewParameter(BooleanProvider::class) showBadge: Boolean,
) {
    AndroidThemeForPreviews {
        MenuHomeScreenUi(
            uiState = MenuUiState(
                name = "John Doe",
                email = "john.doe@example.com",
                unreadNotificationsCount = if (showBadge) 2 else 0
            ),
            navigateToFeature = {},
            onLogoutClicked = {},
            onResetTestPasswordScreenEvent = {},
            onResetLogoutConfirmationEvent = {},
        )
    }
}

private fun openInSpecificApp(context: Context, link: String, packageName: String) {
    runCatching {
        Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
            setPackage(packageName)
            context.startActivity(this)
        }
    }.onFailure {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")
                )
            )
        } catch (_: Exception) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }
}

internal object MenuHomeScreenUiTestTags {
    private const val MENU_HOME_SCREEN = "menu_home_screen"
    const val TOOLBAR = "$MENU_HOME_SCREEN:toolbar"
    const val MY_ACCOUNT_ITEM = "$MENU_HOME_SCREEN:my_account_item"
    const val NOTIFICATION_ICON = "$MENU_HOME_SCREEN:notification_icon"
    const val NOTIFICATION_BADGE = "$MENU_HOME_SCREEN:notification_badge"
    const val ACCOUNT_ITEM = "$MENU_HOME_SCREEN:account_item"
    const val PRIVACY_SUITE_HEADER = "$MENU_HOME_SCREEN:privacy_suite_header"
    const val PRIVACY_SUITE_ITEM = "$MENU_HOME_SCREEN:privacy_suite_item"
    const val LOGOUT_BUTTON = "$MENU_HOME_SCREEN:logout_button"
    const val BADGE = "$MENU_HOME_SCREEN:badge"
}
