package mega.privacy.android.app.mediaplayer

import android.app.Dialog
import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.android.app.databinding.FragmentAudioPlayerBinding
import mega.privacy.android.app.di.mediaplayer.AudioPlayer
import mega.privacy.android.app.mediaplayer.gateway.AudioPlayerServiceViewModelGateway
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.model.AudioSpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.presentation.videoplayer.model.PlaybackPositionStatus
import mega.privacy.android.app.utils.Constants.AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.mobile.analytics.event.AudioPlayerSpeedChange1XEvent
import mega.privacy.mobile.analytics.event.AudioPlayerSpeedChange2XEvent
import mega.privacy.mobile.analytics.event.AudioPlayerSpeedChangeHalfXEvent
import mega.privacy.mobile.analytics.event.AudioPlayerSpeedChangeOneAndHalfXEvent
import mega.privacy.mobile.analytics.event.AudioPlayerSpeedChangeTo_0_75XEvent
import mega.privacy.mobile.analytics.event.AudioPlayerSpeedChangeTo_1_25XEvent
import mega.privacy.mobile.analytics.event.AudioPlayerSpeedChangeTo_1_75XEvent
import timber.log.Timber
import javax.inject.Inject

/**
 * MediaPlayer Fragment
 */
@AndroidEntryPoint
class AudioPlayerFragment : Fragment() {
    /**
     * Inject [GetFeatureFlagValueUseCase] to the Fragment
     */
    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    /**
     * MediaPlayerGateway for audio player
     */
    @AudioPlayer
    @Inject
    lateinit var mediaPlayerGateway: MediaPlayerGateway

    private val audioViewModel by viewModels<AudioPlayerViewModel>()

    private var playerViewHolder: AudioPlayerViewHolder? = null

    private var serviceGateway: MediaPlayerServiceGateway? = null
    private var serviceViewModelGateway: AudioPlayerServiceViewModelGateway? = null

    private var playlistObserved = false

    private var delayHideToolbarCanceled = false

    private var playbackPositionDialog: Dialog? = null

    private var toolbarVisible = true

    private var retryFailedDialog: AlertDialog? = null

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceGateway = null
            serviceViewModelGateway = null
        }

        /**
         * Called after a successful bind with our AudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MediaPlayerServiceBinder) {
                serviceGateway = service.serviceGateway
                serviceViewModelGateway =
                    service.playerServiceViewModelGateway as? AudioPlayerServiceViewModelGateway

                setupPlayer()
                observeFlow()
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            playerViewHolder?.updateLoadingAnimation(state)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        FragmentAudioPlayerBinding.inflate(inflater, container, false).apply {
            playerViewHolder = AudioPlayerViewHolder(this)
        }.root


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context?.bindService(
            Intent(
                requireContext(),
                AudioPlayerService::class.java
            ).putExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false),
            connection,
            BIND_AUTO_CREATE
        )

        observeFlow()
        delayHideToolbar()
    }

    override fun onResume() {
        super.onResume()

        if (serviceGateway != null && serviceViewModelGateway != null) {
            setupPlayer()
        }

        if (!toolbarVisible) {
            showToolbar()
            delayHideToolbar()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playlistObserved = false
        // Close the dialog after fragment is destroyed to avoid adding dialog view repeatedly after screen is rotated.
        playbackPositionDialog?.run {
            if (isShowing) dismiss()
        }
        playerViewHolder = null
        serviceGateway?.removeListener(playerListener)
        serviceGateway = null
        context?.unbindService(connection)
    }

    private fun observeFlow() {
        if (view != null) {
            serviceGateway?.metadataUpdate()?.let {
                viewLifecycleOwner.collectFlow(it) { metadata ->
                    playerViewHolder?.displayMetadata(metadata)
                }
            }

            serviceViewModelGateway?.let { gateway ->
                if (!playlistObserved) {
                    playlistObserved = true
                    viewLifecycleOwner.collectFlow(gateway.playlistUpdate()) { info ->
                        Timber.d("MediaPlayerService observed playlist ${info.first.size} items")

                        playerViewHolder?.togglePlaylistEnabled(
                            requireContext(),
                            info.first,
                            gateway.shuffleEnabled()
                        )
                    }

                    viewLifecycleOwner.collectFlow(gateway.retryUpdate()) { isRetry ->
                        when {
                            !isRetry && retryFailedDialog == null -> {
                                retryFailedDialog = MaterialAlertDialogBuilder(requireContext())
                                    .setCancelable(false)
                                    .setMessage(
                                        getString(
                                            if (isOnline(requireContext())) R.string.error_fail_to_open_file_general
                                            else R.string.error_fail_to_open_file_no_network
                                        )
                                    )
                                    .setPositiveButton(
                                        getString(sharedResR.string.general_ok)
                                    ) { _, _ ->
                                        serviceGateway?.stopPlayer()
                                        requireActivity().finish()
                                    }
                                    .show()
                            }

                            isRetry -> {
                                retryFailedDialog?.dismiss()
                                retryFailedDialog = null
                            }
                        }
                    }

                    viewLifecycleOwner.collectFlow(gateway.monitorMediaItemTransitionState()) { handle ->
                        handle?.let {
                            val name = gateway.getPlaylistItem(it.toString())?.nodeName
                                ?: activity?.intent?.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME)
                                ?: ""
                            audioViewModel.checkPlaybackPositionOfPlayingItem(
                                handle = handle,
                                name = name,
                                status = serviceGateway?.getPlaybackPositionStatus()
                                    ?: PlaybackPositionStatus.Initial
                            ) { status ->
                                serviceGateway?.updatePlaybackPositionStatus(status)
                            }
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.collectFlow(
            audioViewModel.uiState.map { it.currentSpeedPlayback }.distinctUntilChanged()
        ) { speedPlaybackItem ->
            playerViewHolder?.updateSpeedPlaybackText(speedPlaybackItem)
            mediaPlayerGateway.updatePlaybackSpeed(speedPlaybackItem)
        }
    }

    private fun setupPlayer() {
        playerViewHolder?.let { viewHolder ->
            serviceGateway?.run {
                setupPlayerView(this, viewHolder.binding.playerView)
                viewHolder.layoutArtwork()
            }

            serviceViewModelGateway?.run {
                viewHolder.setupPlaylistButton(
                    requireContext(),
                    getPlaylistItems(),
                    shuffleEnabled()
                ) {
                    findNavController().let {
                        viewLifecycleOwner.lifecycleScope.launch {
                            if (it.currentDestination?.id == R.id.audio_main_player) {
                                it.navigate(AudioPlayerFragmentDirections.actionAudioPlayerToQueue())
                            }
                        }
                    }
                }

                initSpeedPlaybackPopup(viewHolder.speedPlaybackPopup)
                viewHolder.speedPlaybackButton.setOnClickListener {
                    audioViewModel.updateIsSpeedPopupShown(true)
                }
                initPlaybackPositionDialog(viewHolder.playbackPositionDialog)
            }
        }
    }

    private fun initSpeedPlaybackPopup(composeView: ComposeView) {
        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by audioViewModel.uiState.collectAsStateWithLifecycle()
                OriginalTheme(isDark = isSystemInDarkTheme()) {
                    SpeedSelectedPopup(
                        items = AudioSpeedPlaybackItem.entries,
                        isShown = state.isSpeedPopupShown,
                        currentPlaybackSpeed = state.currentSpeedPlayback,
                        onDismissRequest = { audioViewModel.updateIsSpeedPopupShown(false) }
                    ) { speedPlaybackItem ->
                        addAudioSpeedPlaybackEvent(speedPlaybackItem)
                        audioViewModel.updateCurrentSpeedPlaybackItem(speedPlaybackItem)
                        audioViewModel.updateIsSpeedPopupShown(false)
                    }
                }
            }
        }
    }

    private fun initPlaybackPositionDialog(composeView: ComposeView) {
        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val uiState by audioViewModel.uiState.collectAsStateWithLifecycle()
                OriginalTheme(isDark = isSystemInDarkTheme()) {
                    PlaybackPositionDialog(
                        type = MediaType.Audio,
                        showPlaybackDialog = uiState.showPlaybackDialog,
                        currentPlayingItemName = uiState.currentPlayingItemName ?: "",
                        playbackPosition = uiState.playbackPosition ?: 0,
                        onPlaybackPositionStatusUpdated = { status ->
                            audioViewModel.updatePlaybackPositionStatus(status)
                            serviceGateway?.updatePlaybackPositionStatus(status)
                        }
                    )
                }
            }
        }
    }

    private fun addAudioSpeedPlaybackEvent(item: SpeedPlaybackItem) {
        when (item) {
            AudioSpeedPlaybackItem.PlaybackSpeed_0_5X -> AudioPlayerSpeedChangeHalfXEvent
            AudioSpeedPlaybackItem.PlaybackSpeed_0_75X -> AudioPlayerSpeedChangeTo_0_75XEvent
            AudioSpeedPlaybackItem.PlaybackSpeed_1X -> AudioPlayerSpeedChange1XEvent
            AudioSpeedPlaybackItem.PlaybackSpeed_1_25X -> AudioPlayerSpeedChangeTo_1_25XEvent
            AudioSpeedPlaybackItem.PlaybackSpeed_1_5X -> AudioPlayerSpeedChangeOneAndHalfXEvent
            AudioSpeedPlaybackItem.PlaybackSpeed_1_75X -> AudioPlayerSpeedChangeTo_1_75XEvent
            AudioSpeedPlaybackItem.PlaybackSpeed_2X -> AudioPlayerSpeedChange2XEvent
            else -> null
        }?.let {
            Analytics.tracker.trackEvent(it)
        }
    }

    private fun setupPlayerView(
        mediaPlayerServiceGateway: MediaPlayerServiceGateway,
        playerView: PlayerView,
    ) {
        mediaPlayerServiceGateway.setupPlayerView(
            playerView = playerView,
            isAudioPlayer = true,
            controllerHideOnTouch = false,
            showShuffleButton = true,
        )

        playerView.setOnClickListener {
            if (toolbarVisible) {
                hideToolbar()
            } else {
                delayHideToolbarCanceled = true
                showToolbar()
            }
        }
        playerViewHolder?.updateLoadingAnimation(mediaPlayerServiceGateway.getPlaybackState())
        mediaPlayerServiceGateway.addPlayerListener(playerListener)
    }

    private fun delayHideToolbar() {
        delayHideToolbarCanceled = false

        runDelay(AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS) {
            if (isResumed && !delayHideToolbarCanceled) {
                hideToolbar()
            }
        }
    }

    private fun hideToolbar(animate: Boolean = true) {
        toolbarVisible = false
        (activity as? AudioPlayerActivity)?.hideToolbar(animate)
    }

    private fun showToolbar() {
        toolbarVisible = true
        (activity as? AudioPlayerActivity)?.showToolbar()
    }

    /**
     * On drag activated
     *
     * @param activated true is activated, otherwise is false
     */
    fun onDragActivated(activated: Boolean) {
        if (activated) {
            delayHideToolbarCanceled = true
            hideToolbar(animate = false)
        }
    }
}
