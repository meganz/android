package mega.privacy.android.app.menu.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.SecondaryFilledButton
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.list.SecondaryHeaderListItem
import mega.android.core.ui.components.profile.MediumProfilePicture
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.ACCOUNT_ITEM
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.LOGOUT_BUTTON
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.MY_ACCOUNT_ITEM
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.NOTIFICATION_ICON
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.PRIVACY_SUITE_HEADER
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.PRIVACY_SUITE_ITEM
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.TOOLBAR
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.navigation.contract.NavDrawerItem
import timber.log.Timber

@Composable
fun MenuHomeScreen(
    navigateToFeature: (Any) -> Unit,
    viewModel: MenuViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    MenuHomeScreenUi(uiState, navigateToFeature)
}

@Composable
fun MenuHomeScreenUi(
    uiState: MenuUiState,
    navigateToFeature: (Any) -> Unit,
) {
    var isPrivacySuiteExpanded by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current

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
                    IconButton(
                        modifier = Modifier.testTag(NOTIFICATION_ICON),
                        onClick = { Timber.d("Notification icon clicked") }
                    ) {
                        MegaIcon(
                            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Bell),
                            tint = IconColor.Primary,
                            contentDescription = "Notification Icon"
                        )
                    }
                },
                navigationType = AppBarNavigationType.None
            )

        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
        )
        {
            item {
                FlexibleLineListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(MY_ACCOUNT_ITEM),
                    title = uiState.name.orEmpty(),
                    subtitle = uiState.email.orEmpty(),
                    leadingElement = {
                        key(uiState.name, uiState.avatarColor, uiState.avatar) {
                            MediumProfilePicture(
                                imageFile = uiState.avatar,
                                contentDescription = uiState.name,
                                name = uiState.name,
                                avatarColor = uiState.avatarColor
                            )
                        }
                    },
                    trailingElement = {
                        MegaIcon(
                            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ChevronRight),
                            tint = IconColor.Secondary,
                            contentDescription = "arrow right",
                        )
                    },
                    onClickListener = {
                        Timber.d("My account item clicked")
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
                    isPrivacySuiteExpanded = !isPrivacySuiteExpanded
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
                LogoutButton {
                    Timber.d("Logout button clicked")
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
        onClickListener = onNavigate
    )
}

@Composable
private fun PrivacySuiteHeader(isExpanded: Boolean, onClick: () -> Unit) {
    SecondaryHeaderListItem(
        modifier = Modifier.testTag(PRIVACY_SUITE_HEADER),
        text = "MEGA privacy suite",
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
    onLogout: () -> Unit,
) {
    SecondaryFilledButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .testTag(LOGOUT_BUTTON),
        text = "Log out",
        onClick = onLogout
    )
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
    const val ACCOUNT_ITEM = "$MENU_HOME_SCREEN:account_item"
    const val PRIVACY_SUITE_HEADER = "$MENU_HOME_SCREEN:privacy_suite_header"
    const val PRIVACY_SUITE_ITEM = "$MENU_HOME_SCREEN:privacy_suite_item"
    const val LOGOUT_BUTTON = "$MENU_HOME_SCREEN:logout_button"
}
