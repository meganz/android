package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder
import mega.privacy.android.app.presentation.meeting.RingingViewModel
import mega.privacy.android.app.presentation.meeting.view.RingingScreen
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.app.utils.permission.permissionsBuilder
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber

/**
 * Fragment to show the incoming call
 */
@AndroidEntryPoint
class RingingMeetingFragment : MeetingBaseFragment() {

    private val inMeetingViewModel by viewModels<InMeetingViewModel>()
    private val ringingViewModel by viewModels<RingingViewModel>()

    /**
     * On create view
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MainComposeView()
        }
    }

    /**
     * On view created
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        meetingActivity.binding.toolbar.apply {
            isVisible = false
        }
        initViewModel()
        permissionsRequester.launch(true)
    }

    @Composable
    private fun MainComposeView() {
        OriginalTheme(isDark = true) {
            RingingScreen(
                viewModel = ringingViewModel,
                onAudioClicked = {
                    if (ringingViewModel.state.value.call != null &&
                        ringingViewModel.state.value.chatConnectionStatus == ChatConnectionStatus.Online
                    ) {
                        checkAndAnswerCall(enableVideo = false)
                    } else {
                        ringingViewModel.onAudioClicked(isClicked = true)
                    }
                },
                onVideoClicked = {
                    if (ringingViewModel.state.value.call != null &&
                        ringingViewModel.state.value.chatConnectionStatus == ChatConnectionStatus.Online
                    ) {
                        checkAndAnswerCall(enableVideo = true)
                    } else {
                        ringingViewModel.onVideoClicked(isClicked = true)
                    }
                },
                onBackPressed = {
                    requireActivity().finish()
                },
            )
        }
    }

    /**
     * Method to answer the call with audio enabled or start the meeting
     *
     * @param enableVideo True, if it should be answered with video on. False, if it should be answered with video off
     */
    private fun checkAndAnswerCall(enableVideo: Boolean) {
        inMeetingViewModel.checkAnotherCallsInProgress(ringingViewModel.state.value.chatId)

        val audio =
            PermissionUtils.hasPermissions(requireContext(), Manifest.permission.RECORD_AUDIO)

        if (!audio) {
            ringingViewModel.showSnackbar()
            return
        }
        var video = enableVideo
        if (video) {
            video = PermissionUtils.hasPermissions(requireContext(), Manifest.permission.CAMERA)
        }

        sharedModel.answerCall(
            chatId = ringingViewModel.state.value.chatId,
            enableVideo = enableVideo,
            enableAudio = true,
            speakerAudio = video
        )
    }

    /**
     * Initialize ViewModel
     */
    private fun initViewModel() {

        collectFlows()

        sharedModel.cameraPermissionCheck.observe(viewLifecycleOwner) { allowed ->
            if (allowed) {
                permissionsRequester = permissionsBuilder(
                    arrayOf(Manifest.permission.CAMERA)
                )
                    .setOnRequiresPermission { l ->
                        run {
                            onRequiresPermission(l)
                            // Continue expected action after granted
                            sharedModel.clickCamera(true)
                        }
                    }
                    .setOnShowRationale { l -> onShowRationale(l) }
                    .setOnNeverAskAgain { l -> onPermNeverAskAgain(l) }
                    .build()
                permissionsRequester.launch(false)
            }
        }

        sharedModel.recordAudioPermissionCheck.observe(viewLifecycleOwner) { allowed ->
            if (allowed) {
                permissionsRequester = permissionsBuilder(
                    arrayOf(Manifest.permission.RECORD_AUDIO)
                )
                    .setOnRequiresPermission { l ->
                        run {
                            onRequiresPermission(l)
                            // Continue expected action after granted
                            sharedModel.clickMic(true)
                        }
                    }
                    .setOnShowRationale { l -> onShowRationale(l) }
                    .setOnNeverAskAgain { l -> onPermNeverAskAgain(l) }
                    .build()
                permissionsRequester.launch(false)
            }
        }
    }

    private fun collectFlows() {
        viewLifecycleOwner.collectFlow(ringingViewModel.state.map { it.chatId }
            .distinctUntilChanged()) {
            if (it != MEGACHAT_INVALID_HANDLE) {
                sharedModel.updateChatRoomId(it)
            }
        }

        viewLifecycleOwner.collectFlow(ringingViewModel.state.map { it.call }
            .distinctUntilChanged()) {
            it?.run {
                if (ringingViewModel.state.value.chatConnectionStatus == ChatConnectionStatus.Online) {
                    if (ringingViewModel.state.value.isAnswerWithAudioClicked) {
                        ringingViewModel.onAudioClicked(isClicked = false)
                        checkAndAnswerCall(enableVideo = false)
                    }

                    if (ringingViewModel.state.value.isAnswerWithVideoClicked) {
                        ringingViewModel.onVideoClicked(isClicked = false)
                        checkAndAnswerCall(enableVideo = true)
                    }
                }
            }
        }

        viewLifecycleOwner.collectFlow(ringingViewModel.state.map { it.chatConnectionStatus }
            .distinctUntilChanged()) {
            if (it == ChatConnectionStatus.Online) {
                ringingViewModel.state.value.call?.let {
                    if (ringingViewModel.state.value.isAnswerWithAudioClicked) {
                        ringingViewModel.onAudioClicked(isClicked = false)
                        checkAndAnswerCall(enableVideo = false)
                    }

                    if (ringingViewModel.state.value.isAnswerWithVideoClicked) {
                        ringingViewModel.onVideoClicked(isClicked = false)
                        checkAndAnswerCall(enableVideo = true)
                    }
                }
            }
        }

        viewLifecycleOwner.collectFlow(ringingViewModel.state.map { it.showMissedCallNotification }
            .distinctUntilChanged()) {
            if (it) {
                Timber.d("Show missed call notification")
                ChatAdvancedNotificationBuilder.newInstance(requireContext())
                    .showMissedCallNotification(ringingViewModel.state.value.chatId, -1L)
                requireActivity().finish()
            }
        }

        viewLifecycleOwner.collectFlow(ringingViewModel.state.map { it.shouldFinish }
            .distinctUntilChanged()) {
            if (it) {
                requireActivity().finish()
            }
        }

        viewLifecycleOwner.collectFlow(sharedModel.state.map { it.answerResult }
            .distinctUntilChanged()) {
            it?.apply {
                val action = RingingMeetingFragmentDirections.actionGlobalInMeeting(
                    actionString,
                    chatHandle
                )
                findNavController().navigate(action)
            }
        }
    }

    /**
     * On attach
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        permissionsRequester = permissionsBuilder(permissions)
            .setOnPermissionDenied { l -> onPermissionDenied(l) }
            .setOnRequiresPermission { l -> onRequiresPermission(l) }
            .setOnShowRationale { l -> onShowRationale(l) }
            .setOnNeverAskAgain { l -> onNeverAskAgain(l) }
            .setPermissionEducation { showPermissionsEducation() }
            .build()
    }

    /**
     * user denies the RECORD_AUDIO or CAMERA permission
     *
     * @param permissions permission list
     */
    private fun onPermNeverAskAgain(permissions: ArrayList<String>) {
        if (permissions.contains(Manifest.permission.RECORD_AUDIO)
            || permissions.contains(Manifest.permission.CAMERA)
        ) {
            Timber.d("user denies the permission")
            ringingViewModel.showSnackbar()
        }
    }

    /**
     * On destroy
     */
    override fun onDestroy() {
        RunOnUIThreadUtils.stop()
        super.onDestroy()
    }
}
