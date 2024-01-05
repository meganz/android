package mega.privacy.android.app.main.dialog.link

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.OpenPasswordLinkActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.dialog.contactlink.ContactLinkDialogFragment
import mega.privacy.android.app.meeting.fragments.MeetingHasEndedDialogFragment
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.filelink.FileLinkComposeActivity
import mega.privacy.android.app.presentation.folderlink.FolderLinkComposeActivity
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.chat.ChatLinkContent
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.exception.chat.IAmOnAnotherCallException
import mega.privacy.android.domain.exception.chat.MeetingEndedException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.legacy.core.ui.controls.dialogs.InputDialog
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.theme.MegaAppTheme
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class OpenLinkDialogFragment : DialogFragment() {
    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var passcodeManagement: PasscodeManagement

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    @Inject
    lateinit var getChatRoomUseCase: GetChatRoomUseCase

    @Inject
    lateinit var openLinkErrorMapper: OpenLinkErrorMapper

    @Inject
    lateinit var openLinkPositiveTextMapper: OpenLinkPositiveTextMapper

    @Inject
    lateinit var navigator: MegaNavigator

    @ApplicationScope
    @Inject
    lateinit var applicationScope: CoroutineScope

    private val viewModel: OpenLinkViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val isChatScreen = requireArguments().getBoolean(IS_CHAT_SCREEN)
        val isJoinMeeting = requireArguments().getBoolean(IS_JOIN_MEETING)
        val title = when {
            isJoinMeeting -> getString(R.string.paste_meeting_link_guest_dialog_title)
            isChatScreen -> getString(R.string.action_open_chat_link)
            else -> getString(R.string.action_open_link)
        }
        val message = if (isJoinMeeting) {
            getString(R.string.paste_meeting_link_guest_instruction)
        } else {
            ""
        }
        val hint = when {
            isJoinMeeting -> getString(R.string.meeting_link)
            isChatScreen -> getString(R.string.hint_enter_chat_link)
            else -> getString(R.string.hint_paste_link)
        }
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val state by viewModel.state.collectAsStateWithLifecycle()
                MegaAppTheme(isDark = themeMode.isDarkMode()) {
                    InputDialog(
                        title = title,
                        message = message,
                        hint = hint,
                        text = viewModel.inputLink,
                        confirmButtonText = stringResource(id = openLinkPositiveTextMapper(state.linkType)),
                        cancelButtonText = stringResource(id = R.string.general_cancel),
                        onConfirm = viewModel::openLink,
                        onDismiss = { dismissAllowingStateLoss() },
                        onInputChange = viewModel::onLinkChanged,
                        error = openLinkErrorMapper(
                            isJoinMeeting = isJoinMeeting,
                            isChatScreen = isChatScreen,
                            submittedLink = state.submittedLink,
                            linkType = state.linkType,
                            checkLinkResult = state.checkLinkResult,
                        )?.let { stringResource(id = it) },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.collectFlow(viewModel.state) { uiState ->
            uiState.linkType?.let { linkType ->
                when (linkType) {
                    RegexPatternType.FILE_LINK -> openFileLink(viewModel.inputLink)
                    RegexPatternType.FOLDER_LINK -> openFolderLink(viewModel.inputLink)
                    RegexPatternType.PASSWORD_LINK -> openPasswordLink(viewModel.inputLink)
                    RegexPatternType.ALBUM_LINK -> openAlbumLink(viewModel.inputLink)
                    else -> Unit
                }
            }
            handleContactLink(uiState.openContactLinkHandle)
            uiState.checkLinkResult?.let {
                handleCheckLinkResult(it)
            }
        }
    }

    private fun handleContactLink(handle: Long) {
        Timber.d("handleContactLink Handle: $handle")
        if (handle > 0L) {
            ContactLinkDialogFragment.newInstance(handle)
                .show(requireActivity().supportFragmentManager, ContactLinkDialogFragment.TAG)
            dismissAllowingStateLoss()
        }
    }

    private fun openFileLink(url: String) {
        Timber.d("openFileLink: $url")
        startActivity(Intent(requireContext(), FileLinkComposeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = Constants.ACTION_OPEN_MEGA_LINK
            data = Uri.parse(url)
        })
        dismissAllowingStateLoss()
    }

    private fun openFolderLink(url: String) {
        Timber.d("openFolderLink: $url")
        startActivity(Intent(requireContext(), FolderLinkComposeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
            data = Uri.parse(url)
        })
        dismissAllowingStateLoss()
    }

    private fun openPasswordLink(url: String) {
        Timber.d("openPasswordLink: $url")
        startActivity(Intent(requireContext(), OpenPasswordLinkActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.parse(url)
        })
        dismissAllowingStateLoss()
    }

    private fun openAlbumLink(url: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.AlbumSharing)) {
                Timber.d("openAlbumLink: $url")
                startActivity(
                    AlbumScreenWrapperActivity.createAlbumImportScreen(
                        context = requireContext(),
                        albumLink = AlbumLink(url),
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
                dismissAllowingStateLoss()
            }
        }
    }

    private fun handleCheckLinkResult(result: Result<ChatLinkContent>) {
        if (result.isSuccess) {
            handleChatOrMeetingLinkResult(result)
        } else if (result.exceptionOrNull() != null) {
            handleCheckLinkException(result)
        }
    }

    private fun handleCheckLinkException(result: Result<ChatLinkContent>) {
        when (val e = result.exceptionOrNull()) {
            is IAmOnAnotherCallException -> {
                CallUtil.showConfirmationInACall(
                    requireActivity(),
                    getString(R.string.text_join_call),
                    passcodeManagement
                )
                dismissAllowingStateLoss()
            }

            is MeetingEndedException -> {
                MeetingHasEndedDialogFragment(object :
                    MeetingHasEndedDialogFragment.ClickCallback {
                    override fun onViewMeetingChat() {
                        showChatLink(e.link, e.chatId)
                    }

                    override fun onLeave() {}
                }, false).show(
                    requireActivity().supportFragmentManager,
                    MeetingHasEndedDialogFragment.TAG
                )
                dismissAllowingStateLoss()
            }
        }
    }

    private fun handleChatOrMeetingLinkResult(result: Result<ChatLinkContent>) {
        val chatLinkContent = result.getOrNull()
        when {
            chatLinkContent?.link.isNullOrEmpty() -> {
                return
            }

            chatLinkContent is ChatLinkContent.MeetingLink -> {
                Timber.d("It's a meeting link")
                applicationScope.launch {
                    runCatching {
                        getChatRoomUseCase(chatLinkContent.chatHandle)
                    }.onSuccess { chatRoom ->
                        chatRoom?.let {
                            if (chatRoom.isMeeting && chatRoom.isWaitingRoom && chatRoom.ownPrivilege == ChatRoomPermission.Moderator) {
                                viewModel.startOrAnswerMeetingWithWaitingRoomAsHost(chatId = chatLinkContent.chatHandle)
                            } else {
                                CallUtil.joinMeetingOrReturnCall(
                                    requireContext(),
                                    chatLinkContent.chatHandle,
                                    chatLinkContent.link,
                                    chatLinkContent.text,
                                    chatLinkContent.exist,
                                    chatLinkContent.userHandle,
                                    passcodeManagement,
                                    chatLinkContent.isWaitingRoom,
                                )
                            }
                        }
                    }.onFailure { exception ->
                        Timber.e(exception)
                    }
                }
            }

            chatLinkContent is ChatLinkContent.ChatLink -> {
                Timber.d("It's a chat link")
                showChatLink(chatLinkContent.link, chatLinkContent.chatHandle)
            }
        }
        dismissAllowingStateLoss()
    }

    fun showChatLink(link: String?, chatId: Long) {
        Timber.d("showChatLink: %s", link)
        navigator.openChat(
            context = requireContext(),
            chatId = chatId,
            link = link,
            action = Constants.ACTION_OPEN_CHAT_LINK
        )
    }

    companion object {
        const val TAG = "OpenLinkDialogFragment"
        const val IS_CHAT_SCREEN = "IS_CHAT_SCREEN"
        const val IS_JOIN_MEETING = "IS_JOIN_MEETING"
        fun newInstance(isChatScreen: Boolean, isJoinMeeting: Boolean): OpenLinkDialogFragment =
            OpenLinkDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(IS_CHAT_SCREEN, isChatScreen)
                    putBoolean(IS_JOIN_MEETING, isJoinMeeting)
                }
            }
    }
}