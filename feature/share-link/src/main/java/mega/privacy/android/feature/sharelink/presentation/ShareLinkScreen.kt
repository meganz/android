package mega.privacy.android.feature.sharelink.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.button.AnchoredButtonGroup
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.android.core.ui.model.Button
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.LinkCopyAllLinksButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkCopyDecryptionKeyButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkCopyLinkButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkCopyPasswordButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkCopyrightAgreeButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkCopyrightCancelButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkCopyrightWarningDialogEvent
import mega.privacy.mobile.analytics.event.LinkHiddenItemsCancelButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkHiddenItemsContinueButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkHiddenItemsWarningDialogEvent
import mega.privacy.mobile.analytics.event.LinkShareButtonPressedEvent
import mega.privacy.mobile.analytics.event.ShareLinkScreenEvent
import mega.privacy.mobile.analytics.event.SingleAlbumLinkScreenEvent

/**
 * Revamped Share link result screen.
 *
 * The scaffold and its bars live here; the per-state body composables and the preview live in
 * `ShareLinkStateContent.kt` and `ShareLinkScreenPreview.kt` respectively.
 *
 * @param uiState The current [ShareLinkUiState].
 * @param onBack Invoked when the Close action is tapped.
 * @param onOpenSettings Invoked when the settings (gear) action is tapped.
 * @param onShareLink Invoked with the shareable text once the user has settled what to send: the
 * single link on its own, or followed by its password or decryption key when the user opts to
 * include it, or, for multiple nodes, every link joined by newlines.
 * @param onCopyLink Invoked when the copy icon on a link is tapped.
 * @param onCopyKey Invoked when the copy icon on the separate key card is tapped.
 * @param onCopyPassword Invoked when the copy icon on the password card is tapped.
 * @param onLinksCopied Invoked once when the screen opens and the link — or every link, for a
 * multi-node selection — has been copied to the clipboard automatically.
 * @param onSensitiveWarningConfirmed Invoked when the user confirms the hidden-items warning.
 * @param onSensitiveWarningDismissed Invoked when the user cancels the hidden-items warning.
 * @param onCopyrightAgreed Invoked when the user agrees to the first-time copyright consent.
 * @param onCopyrightDisagreed Invoked when the user declines the first-time copyright consent.
 * @param isAlbum Whether an album's link is being shared, selecting the album screen-view event.
 * Taken from the navigation key rather than [uiState], because the screen view is reported at first
 * composition, while the state is still loading and cannot say what the subject is.
 * @param modifier Modifier for the scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareLinkScreen(
    uiState: ShareLinkUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onShareLink: (String) -> Unit,
    onCopyLink: () -> Unit,
    onCopyKey: () -> Unit,
    modifier: Modifier = Modifier,
    isAlbum: Boolean = false,
    onCopyPassword: () -> Unit = {},
    onLinksCopied: () -> Unit = {},
    onSensitiveWarningConfirmed: () -> Unit = {},
    onSensitiveWarningDismissed: () -> Unit = {},
    onCopyrightAgreed: () -> Unit = {},
    onCopyrightDisagreed: () -> Unit = {},
) {
    val linkCount = (uiState as? ShareLinkUiState.Data)?.handles?.size ?: 1
    var showSharePasswordDialog by rememberSaveable { mutableStateOf(false) }
    var showShareKeyDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedOnceEffect(Unit) {
        // Albums keep the screen-view event the legacy album screen already fires, so the metric
        // stays continuous across the revamp.
        Analytics.tracker.trackEvent(
            if (isAlbum) SingleAlbumLinkScreenEvent else ShareLinkScreenEvent
        )
    }
    LaunchedEffect(uiState is ShareLinkUiState.CopyrightConsent) {
        if (uiState is ShareLinkUiState.CopyrightConsent) {
            Analytics.tracker.trackEvent(LinkCopyrightWarningDialogEvent)
        }
    }
    LaunchedEffect(uiState is ShareLinkUiState.SensitiveWarning) {
        if (uiState is ShareLinkUiState.SensitiveWarning) {
            Analytics.tracker.trackEvent(LinkHiddenItemsWarningDialogEvent)
        }
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier,
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(SHARE_LINK_APP_BAR_TAG),
                title = if (uiState is ShareLinkUiState.CopyrightConsent) {
                    ""
                } else {
                    pluralStringResource(sharedR.plurals.label_share_links, linkCount)
                },
                subtitle = null,
                navigationType = AppBarNavigationType.Close(onBack),
                actions = buildList {
                    if (uiState is ShareLinkUiState.Data && !uiState.isMultiNode) {
                        add(MenuActionWithClick(ShareLinkSettingsAction, onOpenSettings))
                    }
                },
            )
        },
        bottomBar = {
            when (uiState) {
                is ShareLinkUiState.Data -> {
                    val shareText =
                        pluralStringResource(sharedR.plurals.label_share_links, linkCount)
                    AnchoredButtonGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        buttonGroup = listOf(
                            {
                                Button.PrimaryButton(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag(SHARE_LINK_SHARE_BUTTON_TAG),
                                    text = shareText,
                                    onClick = {
                                        Analytics.tracker.trackEvent(LinkShareButtonPressedEvent)
                                        when {
                                            uiState.sharePassword != null ->
                                                showSharePasswordDialog = true

                                            uiState.shareKey != null ->
                                                showShareKeyDialog = true

                                            else -> onShareLink(uiState.shareableLinksText())
                                        }
                                    },
                                )
                            },
                        ),
                    )
                }

                ShareLinkUiState.CopyrightConsent -> AnchoredButtonGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    buttonGroup = listOf(
                        {
                            Button.PrimaryButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(SHARE_LINK_COPYRIGHT_AGREE_TAG),
                                text = stringResource(sharedR.string.copyright_action_agree),
                                onClick = {
                                    Analytics.tracker.trackEvent(LinkCopyrightAgreeButtonPressedEvent)
                                    onCopyrightAgreed()
                                },
                            )
                        },
                        {
                            Button.SecondaryButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(SHARE_LINK_COPYRIGHT_DISAGREE_TAG),
                                text = stringResource(sharedR.string.general_dialog_cancel_button),
                                onClick = {
                                    Analytics.tracker.trackEvent(LinkCopyrightCancelButtonPressedEvent)
                                    onCopyrightDisagreed()
                                },
                            )
                        },
                    ),
                )

                else -> Unit
            }
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when (uiState) {
                ShareLinkUiState.Loading -> ShareLinkLoading()
                ShareLinkUiState.Error -> ShareLinkError()
                ShareLinkUiState.CopyrightConsent -> CopyrightConsent()
                is ShareLinkUiState.SensitiveWarning -> {
                    ShareLinkLoading()
                    SensitiveItemsWarningDialog(
                        type = uiState.type,
                        nodeCount = uiState.nodeCount,
                        onConfirm = {
                            Analytics.tracker.trackEvent(LinkHiddenItemsContinueButtonPressedEvent)
                            onSensitiveWarningConfirmed()
                        },
                        onDismiss = {
                            Analytics.tracker.trackEvent(LinkHiddenItemsCancelButtonPressedEvent)
                            onSensitiveWarningDismissed()
                        },
                    )
                }

                is ShareLinkUiState.Data -> if (uiState.isMultiNode) {
                    CopyLinksOnFirstOpen(
                        uiState = uiState,
                        onCopied = {
                            Analytics.tracker.trackEvent(LinkCopyAllLinksButtonPressedEvent)
                            onLinksCopied()
                        },
                    )
                    MultiNodeContent(
                        uiState = uiState,
                        onCopyLink = {
                            Analytics.tracker.trackEvent(LinkCopyLinkButtonPressedEvent)
                            onCopyLink()
                        },
                    )
                } else {
                    CopyLinksOnFirstOpen(uiState = uiState, onCopied = onLinksCopied)
                    ShareLinkContent(
                        uiState = uiState,
                        onCopyLink = {
                            Analytics.tracker.trackEvent(LinkCopyLinkButtonPressedEvent)
                            onCopyLink()
                        },
                        onCopyKey = {
                            Analytics.tracker.trackEvent(LinkCopyDecryptionKeyButtonPressedEvent)
                            onCopyKey()
                        },
                        onCopyPassword = {
                            Analytics.tracker.trackEvent(LinkCopyPasswordButtonPressedEvent)
                            onCopyPassword()
                        },
                    )
                }
            }
        }
    }

    val data = uiState as? ShareLinkUiState.Data
    val sharePassword = data?.sharePassword
    if (showSharePasswordDialog && data != null && sharePassword != null) {
        val linkAndPassword = stringResource(
            sharedR.string.share_link_with_password,
            data.shareableLinksText(),
            sharePassword,
        )
        BasicDialog(
            modifier = Modifier.testTag(SHARE_LINK_PASSWORD_DIALOG_TAG),
            title = stringResource(sharedR.string.share_link_password_dialog_title),
            description = stringResource(sharedR.string.share_link_password_dialog_message),
            positiveButtonText = stringResource(sharedR.string.general_share),
            onPositiveButtonClicked = {
                showSharePasswordDialog = false
                onShareLink(linkAndPassword)
            },
            negativeButtonText = stringResource(sharedR.string.general_dismiss_dialog),
            onNegativeButtonClicked = {
                showSharePasswordDialog = false
                onShareLink(data.shareableLinksText())
            },
            onDismiss = { showSharePasswordDialog = false },
        )
    }

    val shareKey = data?.shareKey
    if (showShareKeyDialog && data != null && shareKey != null) {
        val linkAndKey = stringResource(
            sharedR.string.album_get_link_share_link_with_key,
            data.shareableLinksText(),
            shareKey,
        )
        BasicDialog(
            modifier = Modifier.testTag(SHARE_LINK_KEY_DIALOG_TAG),
            title = stringResource(sharedR.string.album_get_link_share_link_dialog_title),
            description = stringResource(sharedR.string.album_get_link_share_link_dialog_description),
            positiveButtonText = stringResource(
                sharedR.string.album_get_link_share_link_dialog_share_action_link_key
            ),
            onPositiveButtonClicked = {
                showShareKeyDialog = false
                onShareLink(linkAndKey)
            },
            negativeButtonText = stringResource(
                sharedR.string.album_get_link_share_link_dialog_share_action_only_link
            ),
            onNegativeButtonClicked = {
                showShareKeyDialog = false
                onShareLink(data.shareableLinksText())
            },
            onDismiss = { showShareKeyDialog = false },
        )
    }
}

/**
 * Toolbar action that opens the Link settings editor from the Share link screen.
 */
internal data object ShareLinkSettingsAction : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.GearSix)

    override val testTag = "share_link_screen:action_settings"

    @Composable
    override fun getDescription() = stringResource(sharedR.string.general_settings)
}

internal const val SHARE_LINK_APP_BAR_TAG = "share_link_screen:app_bar"
internal const val SHARE_LINK_SHARE_BUTTON_TAG = "share_link_screen:button_share"
internal const val SHARE_LINK_NODE_HEADER_TAG = "share_link_screen:node_header"
internal const val SHARE_LINK_ALBUM_HEADER_TAG = "share_link_screen:album_header"
internal const val SHARE_LINK_ALBUM_COVER_TAG = "share_link_screen:album_cover"
internal const val SHARE_LINK_MULTI_NODE_LIST_TAG = "share_link_screen:multi_node_list"
internal const val SHARE_LINK_SENSITIVE_WARNING_TAG = "share_link_screen:sensitive_warning"
internal const val SHARE_LINK_ACCESS_BANNER_TAG = "share_link_screen:access_banner"
internal const val SHARE_LINK_LOADING_TAG = "share_link_screen:loading"
internal const val SHARE_LINK_ERROR_TAG = "share_link_screen:error"
internal const val SHARE_LINK_COPYRIGHT_TAG = "share_link_screen:copyright"
internal const val SHARE_LINK_COPYRIGHT_AGREE_TAG = "share_link_screen:copyright_agree"
internal const val SHARE_LINK_COPYRIGHT_DISAGREE_TAG = "share_link_screen:copyright_disagree"
internal const val SHARE_LINK_PASSWORD_DIALOG_TAG = "share_link_screen:share_password_dialog"
internal const val SHARE_LINK_KEY_DIALOG_TAG = "share_link_screen:share_key_dialog"
