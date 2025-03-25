package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.icon.pack.R as IconR
import mega.privacy.android.app.presentation.chat.list.view.ChatAvatarView
import mega.privacy.android.app.presentation.meeting.RingingViewModel
import mega.privacy.android.app.presentation.meeting.model.RingingUIState
import mega.privacy.android.app.presentation.meeting.view.RingingViewTestTags.AUDIO_BUTTON
import mega.privacy.android.app.presentation.meeting.view.RingingViewTestTags.GROUP_AVATAR
import mega.privacy.android.app.presentation.meeting.view.RingingViewTestTags.HANG_UP_BUTTON
import mega.privacy.android.app.presentation.meeting.view.RingingViewTestTags.ONE_TO_ONE_AVATAR
import mega.privacy.android.app.presentation.meeting.view.RingingViewTestTags.VIDEO_BUTTON
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.chat.attachpanel.CellButton
import mega.privacy.android.shared.original.core.ui.controls.chat.attachpanel.CellButtonType
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

/**
 * Ringing Screen
 */
@Composable
internal fun RingingScreen(
    onBackPressed: () -> Unit,
    onAudioClicked: () -> Unit,
    onVideoClicked: () -> Unit,
    viewModel: RingingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    if (uiState.showSnackbar) {
        coroutineScope.launch {
            val result = snackbarHostState.showAutoDurationSnackbar(
                message = context.getString(R.string.meeting_required_permissions_warning),
                actionLabel = context.getString(R.string.action_settings)
            )

            when (result) {
                SnackbarResult.Dismissed -> viewModel.hideSnackbar()
                SnackbarResult.ActionPerformed -> {
                    context.navigateToAppSettings()
                    viewModel.hideSnackbar()
                }
            }
        }
    }

    RingingView(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackPressed = onBackPressed,
        onAudioClicked = onAudioClicked,
        onVideoClicked = onVideoClicked,
        onHangUpClicked = {
            viewModel.onHangUpClicked()
        }
    )
}

/**
 * Ringing view
 */
@Composable
fun RingingView(
    uiState: RingingUIState,
    snackbarHostState: SnackbarHostState,
    onBackPressed: () -> Unit = {},
    onAudioClicked: () -> Unit = {},
    onVideoClicked: () -> Unit = {},
    onHangUpClicked: () -> Unit = {},
) {
    MegaScaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                elevation = 1.dp,
                onNavigationPressed = onBackPressed,
                title = uiState.getTitle ?: stringResource(R.string.title_mega_info_empty_screen),
                subtitle = if (uiState.isCallAnsweredAndWaitingForCallInfo) stringResource(
                    R.string.chat_connecting
                ) else stringResource(R.string.outgoing_call_starting)
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                RingingViewContent(
                    uiState = uiState,
                    onAudioClicked = onAudioClicked,
                    onVideoClicked = onVideoClicked,
                    onHangUpClicked = onHangUpClicked,
                )

                SnackbarHost(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    hostState = snackbarHostState
                )
            }
        }
    )
}

/**
 * Ringing view content
 */
@Composable
fun RingingViewContent(
    uiState: RingingUIState,
    onAudioClicked: () -> Unit,
    onVideoClicked: () -> Unit,
    onHangUpClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5F),
            horizontalArrangement = Arrangement.Center,
        ) {
            Column {
                Spacer(Modifier.fillMaxHeight(0.25f))
                uiState.avatar?.let { avatar ->
                    Box(
                        modifier = Modifier
                            .border(
                                4.dp,
                                colorResource(id = R.color.white_alpha_030),
                                CircleShape
                            )
                            .size(80.dp)
                            .clipToBounds()

                    ) {
                        ChatAvatarView(
                            modifier = Modifier
                                .padding(4.dp)
                                .testTag(
                                    if (uiState.isOneToOneCall) ONE_TO_ONE_AVATAR else GROUP_AVATAR
                                ),
                            avatarUri = avatar.uri,
                            avatarPlaceholder = avatar.placeholderText,
                            avatarColor = avatar.color,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!uiState.isCallAnsweredAndWaitingForCallInfo) {
                ActionButtons(
                    onAudioClicked = onAudioClicked,
                    onVideoClicked = onVideoClicked,
                    onHangUpClicked = onHangUpClicked,
                )
            }
        }
    }
}


/**
 * MeetingActionButtons contains the buttons for the ringing fragment.
 */
@Composable
fun ActionButtons(
    onAudioClicked: (() -> Unit)?,
    onVideoClicked: (() -> Unit)?,
    onHangUpClicked: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(bottom = 80.dp)
            .padding(horizontal = 48.dp)

    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RingingActionButtons(
                    onAudioClicked = onAudioClicked,
                    onVideoClicked = onVideoClicked,
                    onHangUpClicked = onHangUpClicked
                )
            }
        }
    }
}

/**
 * RingingActionButtons contains the buttons for the ringing fragment.
 */
@Composable
fun RingingActionButtons(
    onAudioClicked: (() -> Unit)?,
    onVideoClicked: (() -> Unit)?,
    onHangUpClicked: (() -> Unit)?,
) {
    // Audio button
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CellButton(
            itemName = stringResource(id = R.string.calls_answer_audio_button),
            modifier = Modifier.testTag(AUDIO_BUTTON),
            type = CellButtonType.On,
            enabled = true,
            iconId = IconR.drawable.ic_phone_01,
            onItemClick = { onAudioClicked?.invoke() }
        )
    }

    // Video button
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CellButton(
            itemName = stringResource(id = R.string.upload_to_video),
            modifier = Modifier.testTag(VIDEO_BUTTON),
            type = CellButtonType.On,
            enabled = true,
            iconId = IconR.drawable.ic_video_on,
            onItemClick = { onVideoClicked?.invoke() }
        )
    }

    // Hang up Call button
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CellButton(
            itemName = stringResource(id = R.string.general_reject),
            modifier = Modifier.testTag(HANG_UP_BUTTON),
            type = CellButtonType.Interactive,
            enabled = true,
            iconId = IconR.drawable.hang_call_icon,
            onItemClick = { onHangUpClicked?.invoke() }
        )
    }
}

@CombinedThemePreviews
@Composable
internal fun PreviewRingingView() {
    OriginalTheme(isDark = true) {
        RingingView(
            uiState = RingingUIState(
                chatId = -1,
                chat = null,
                call = null,
                avatar = ChatAvatarItem(
                    placeholderText = "R",
                    color = 0xFFC70000.toInt(),
                    uri = null
                )
            ),
            snackbarHostState = SnackbarHostState(),
            onAudioClicked = {},
            onVideoClicked = {},
            onHangUpClicked = {}
        )
    }
}

internal object RingingViewTestTags {
    private const val RINGING_SCREEN = "ringing_screen"
    const val GROUP_AVATAR = "$RINGING_SCREEN:group_avatar"
    const val ONE_TO_ONE_AVATAR = "$RINGING_SCREEN:one_to_one_avatar"
    const val AUDIO_BUTTON = "$RINGING_SCREEN:audio_button"
    const val VIDEO_BUTTON = "$RINGING_SCREEN:video_button"
    const val HANG_UP_BUTTON = "$RINGING_SCREEN:hangup_button"
}
