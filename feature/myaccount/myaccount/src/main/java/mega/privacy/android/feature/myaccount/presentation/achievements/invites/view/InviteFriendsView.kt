package mega.privacy.android.feature.myaccount.presentation.achievements.invites.view

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.model.InviteFriendsUIState
import mega.privacy.android.feature.myaccount.presentation.achievements.util.getTextByDurationInDays
import mega.privacy.android.feature.myaccount.presentation.achievements.util.toUnitString
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.legacy.core.ui.controls.appbar.SimpleTopAppBar
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.dark_blue_500_dark_blue_200
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_020_dark_grey
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_038_white_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.shared.resources.R as sharedR

internal object InviteFriendsViewTestTags {
    // InviteFriendsView
    private const val INVITE_FRIENDS_VIEW = "invite_friends_view"
    const val TOOLBAR = "$INVITE_FRIENDS_VIEW:toolbar"
    const val IMAGE_MAIN = "$INVITE_FRIENDS_VIEW:image_main"
    const val DESCRIPTION = "$INVITE_FRIENDS_VIEW:description"
    const val INVITE_CONTACTS_BUTTON = "$INVITE_FRIENDS_VIEW:invite_contacts_button"
    const val HOW_IT_WORKS_TITLE = "$INVITE_FRIENDS_VIEW:how_it_works_title"
    const val HOW_IT_WORKS_DESCRIPTION = "$INVITE_FRIENDS_VIEW:how_it_works_description"
    const val FOOTER = "$INVITE_FRIENDS_VIEW:footer"

    // InviteConfirmationDialog
    private const val INVITE_CONFIRMATION_DIALOG = "invite_friends_confirmation_dialog"
    const val DIALOG_CONTAINER = "$INVITE_CONFIRMATION_DIALOG:dialog_container"
    const val DIALOG_IMAGE_ICON = "$INVITE_CONFIRMATION_DIALOG:image_icon"
    const val DIALOG_TITLE = "$INVITE_CONFIRMATION_DIALOG:title"
    const val DIALOG_SUBTITLE = "$INVITE_CONFIRMATION_DIALOG:subtitle"
    const val DIALOG_BUTTON = "$INVITE_CONFIRMATION_DIALOG:button"
}

/**
 * Invite Friends Screen in Jetpack Compose.
 *
 * @param uiState the referral storage state driving the explainer copy.
 * @param sentInvitesCount the number of invitations returned by the invite-contact flow, or null
 * when no invitations were sent; a non-null value shows the confirmation dialog.
 * @param onInviteClick invoked when the invite-contacts button is tapped.
 * @param onConfirmationDismiss invoked when the confirmation dialog is dismissed.
 */
@Composable
internal fun InviteFriendsView(
    modifier: Modifier = Modifier,
    uiState: InviteFriendsUIState,
    sentInvitesCount: Int?,
    onInviteClick: () -> Unit,
    onConfirmationDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val storageFormatted = remember(uiState.grantStorageInBytes) {
        uiState.grantStorageInBytes.toUnitString(context)
    }
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = rememberScaffoldState(),
        topBar = {
            SimpleTopAppBar(
                modifier = Modifier.testTag(InviteFriendsViewTestTags.TOOLBAR),
                titleId = sharedR.string.title_referral_bonuses,
                elevation = scrollState.value > 0,
                onBackPressed = {
                    onBackPressedDispatcher?.onBackPressed()
                }
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colors.grey_020_dark_grey)
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 6.dp)
                    .background(MaterialTheme.colors.surface)
            ) {
                Image(
                    modifier = Modifier
                        .testTag(InviteFriendsViewTestTags.IMAGE_MAIN)
                        .padding(top = 20.dp)
                        .align(Alignment.CenterHorizontally),
                    painter = painterResource(id = iconPackR.drawable.ic_invite_friends_big),
                    contentDescription = "Invite Friends Image"
                )
                Text(
                    modifier = Modifier
                        .testTag(InviteFriendsViewTestTags.DESCRIPTION)
                        .padding(top = 24.dp, start = 24.dp, end = 24.dp)
                        .align(Alignment.CenterHorizontally),
                    text = uiState.durationInDays.getTextByDurationInDays(
                        context = context,
                        daysStringId = sharedR.string.figures_storage_referrals_achievements_text,
                        permanentStringId = sharedR.string.figures_storage_referrals_achievements_text_permanent,
                        storage = storageFormatted
                    ),
                    color = MaterialTheme.colors.textColorSecondary,
                    textAlign = TextAlign.Center
                )
                RaisedDefaultMegaButton(
                    modifier = Modifier
                        .testTag(InviteFriendsViewTestTags.INVITE_CONTACTS_BUTTON)
                        .padding(top = 16.dp, bottom = 26.dp)
                        .align(Alignment.CenterHorizontally),
                    textId = sharedR.string.invite_contacts,
                    onClick = onInviteClick
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 6.dp)
                    .background(MaterialTheme.colors.surface)
            ) {
                Text(
                    modifier = Modifier
                        .testTag(InviteFriendsViewTestTags.HOW_IT_WORKS_TITLE)
                        .padding(top = 24.dp)
                        .align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.subtitle1.copy(letterSpacing = 0.sp),
                    color = MaterialTheme.colors.dark_blue_500_dark_blue_200,
                    text = stringResource(id = sharedR.string.title_achievement_invite_friends)
                )
                Text(
                    modifier = Modifier
                        .testTag(InviteFriendsViewTestTags.HOW_IT_WORKS_DESCRIPTION)
                        .padding(top = 24.dp, start = 24.dp, end = 24.dp)
                        .align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.body2.copy(letterSpacing = 0.sp),
                    color = MaterialTheme.colors.textColorSecondary,
                    text = stringResource(id = sharedR.string.first_paragraph_achievement_invite_friends)
                )
                Text(
                    modifier = Modifier
                        .testTag(InviteFriendsViewTestTags.FOOTER)
                        .padding(top = 40.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
                        .align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.caption.copy(letterSpacing = 0.sp),
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colors.grey_alpha_038_white_alpha_038,
                    text = stringResource(id = sharedR.string.second_paragraph_achievement_invite_friends)
                )
            }

            InviteConfirmationDialog(
                isDialogVisible = sentInvitesCount != null,
                description = if ((sentInvitesCount ?: 0) > 1) {
                    sharedR.string.invite_sent_text_multi
                } else {
                    sharedR.string.invite_sent_text
                },
                onDismiss = onConfirmationDismiss
            )
        }
    }
}

/**
 * Confirmation Dialog to be shown after success sending invites
 */
@Composable
internal fun InviteConfirmationDialog(
    isDialogVisible: Boolean,
    @StringRes description: Int,
    onDismiss: () -> Unit,
) {
    if (isDialogVisible) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .testTag(InviteFriendsViewTestTags.DIALOG_CONTAINER)
                    .wrapContentHeight()
                    .fillMaxWidth(),
                elevation = 8.dp,
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                ) {
                    Image(
                        modifier = Modifier
                            .testTag(InviteFriendsViewTestTags.DIALOG_IMAGE_ICON)
                            .padding(top = 40.dp)
                            .align(Alignment.CenterHorizontally),
                        painter = painterResource(id = iconPackR.drawable.ic_success_invite),
                        contentDescription = "Invite Friends Check Icon"
                    )
                    Text(
                        modifier = Modifier
                            .testTag(InviteFriendsViewTestTags.DIALOG_TITLE)
                            .padding(top = 34.dp, start = 17.dp, end = 17.dp)
                            .align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colors.textColorSecondary,
                        fontWeight = FontWeight.Bold,
                        text = stringResource(id = sharedR.string.subtitle_confirmation_invite_friends)
                    )
                    Text(
                        modifier = Modifier
                            .testTag(InviteFriendsViewTestTags.DIALOG_SUBTITLE)
                            .padding(top = 50.dp, start = 17.dp, end = 17.dp)
                            .align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colors.textColorSecondary,
                        text = stringResource(id = description),
                        textAlign = TextAlign.Center
                    )
                    RaisedDefaultMegaButton(
                        modifier = Modifier
                            .testTag(InviteFriendsViewTestTags.DIALOG_BUTTON)
                            .fillMaxWidth()
                            .padding(top = 50.dp, start = 26.dp, end = 26.dp, bottom = 16.dp)
                            .align(Alignment.CenterHorizontally),
                        textId = sharedR.string.general_close,
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
@CombinedThemePreviews
internal fun InviteFriendsViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        InviteFriendsView(
            modifier = Modifier,
            uiState = InviteFriendsUIState(
                grantStorageInBytes = 5368709120,
                durationInDays = 365,
            ),
            sentInvitesCount = null,
            onInviteClick = {},
            onConfirmationDismiss = {},
        )
    }
}

@Composable
@CombinedThemePreviews
internal fun InviteConfirmationDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        InviteConfirmationDialog(
            isDialogVisible = true,
            description = sharedR.string.invite_sent_text_multi,
            onDismiss = {}
        )
    }
}
