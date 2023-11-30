package mega.privacy.android.app.presentation.achievements.invites.view

import android.content.Intent
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.data.extensions.toUnitString
import mega.privacy.android.app.main.InviteContactActivity
import mega.privacy.android.app.presentation.achievements.invites.model.InviteFriendsUIState
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.dark_blue_500_dark_blue_200
import mega.privacy.android.core.ui.theme.extensions.grey_020_dark_grey
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_038_white_alpha_038
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.legacy.core.ui.controls.appbar.SimpleTopAppBar

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
 * Invite Friends Screen in Jetpack Compose
 */
@Composable
fun InviteFriendsRoute(viewModel: InviteFriendsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    InviteFriendsView(uiState = uiState)
}

@Composable
internal fun InviteFriendsView(
    modifier: Modifier = Modifier,
    uiState: InviteFriendsUIState,
) {
    val context = LocalContext.current
    var isDialogVisible by remember { mutableStateOf(false) }
    var numberOfInvites by remember { mutableStateOf(0) }
    val storageFormatted = remember(uiState.grantStorageInBytes) {
        uiState.grantStorageInBytes.toUnitString(context)
    }
    val activityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            result.data?.let { intent ->
                numberOfInvites = intent.getIntExtra(InviteContactActivity.KEY_SENT_NUMBER, 1)
                isDialogVisible = true
            }
        }
    }
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = rememberScaffoldState(),
        topBar = {
            SimpleTopAppBar(
                modifier = Modifier.testTag(InviteFriendsViewTestTags.TOOLBAR),
                titleId = R.string.title_referral_bonuses,
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
                .verticalScroll(rememberScrollState())
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
                    painter = painterResource(id = R.drawable.ic_invite_friends_big),
                    contentDescription = "Invite Friends Image"
                )
                Text(
                    modifier = Modifier
                        .testTag(InviteFriendsViewTestTags.DESCRIPTION)
                        .padding(top = 24.dp, start = 24.dp, end = 24.dp)
                        .align(Alignment.CenterHorizontally),
                    text = stringResource(
                        id = R.string.figures_achievements_text_referrals,
                        storageFormatted
                    ),
                    color = MaterialTheme.colors.textColorSecondary,
                    textAlign = TextAlign.Center
                )
                RaisedDefaultMegaButton(
                    modifier = Modifier
                        .testTag(InviteFriendsViewTestTags.INVITE_CONTACTS_BUTTON)
                        .padding(top = 16.dp, bottom = 26.dp)
                        .align(Alignment.CenterHorizontally),
                    textId = R.string.invite_contacts,
                    onClick = {
                        val intent = Intent(context, InviteContactActivity::class.java).apply {
                            putExtra(InviteContactActivity.KEY_FROM, true)
                        }
                        activityLauncher.launch(intent)
                    }
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
                    text = stringResource(id = R.string.title_achievement_invite_friends)
                )
                Text(
                    modifier = Modifier
                        .testTag(InviteFriendsViewTestTags.HOW_IT_WORKS_DESCRIPTION)
                        .padding(top = 24.dp, start = 24.dp, end = 24.dp)
                        .align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.body2.copy(letterSpacing = 0.sp),
                    color = MaterialTheme.colors.textColorSecondary,
                    text = stringResource(id = R.string.first_paragraph_achievement_invite_friends)
                )
                Text(
                    modifier = Modifier
                        .testTag(InviteFriendsViewTestTags.FOOTER)
                        .padding(top = 40.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
                        .align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.caption.copy(letterSpacing = 0.sp),
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colors.grey_alpha_038_white_alpha_038,
                    text = stringResource(id = R.string.second_paragraph_achievement_invite_friends)
                )
            }

            InviteConfirmationDialog(
                isDialogVisible = isDialogVisible,
                description = if (numberOfInvites > 1) R.string.invite_sent_text_multi else R.string.invite_sent_text,
                onDismiss = {
                    isDialogVisible = false
                }
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
                        painter = painterResource(id = R.drawable.ic_success_invite),
                        contentDescription = "Invite Friends Check Icon"
                    )
                    Text(
                        modifier = Modifier
                            .testTag(InviteFriendsViewTestTags.DIALOG_TITLE)
                            .padding(top = 34.dp, start = 17.dp, end = 17.dp)
                            .align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colors.textColorSecondary,
                        fontWeight = FontWeight.Bold,
                        text = stringResource(id = R.string.subtitle_confirmation_invite_friends)
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
                        textId = R.string.general_close,
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
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        InviteFriendsView(
            modifier = Modifier,
            uiState = InviteFriendsUIState(
                grantStorageInBytes = 5368709120
            )
        )
    }
}

@Composable
@CombinedThemePreviews
internal fun InviteConfirmationDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        InviteConfirmationDialog(
            isDialogVisible = true,
            description = R.string.invite_sent_text_multi,
            onDismiss = {}
        )
    }
}