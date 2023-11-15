package mega.privacy.android.app.presentation.videosection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.databinding.FragmentVideoSectionBinding
import mega.privacy.android.app.fragments.homepage.HomepageSearchable
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * The Fragment for video section
 */
@AndroidEntryPoint
class VideoSectionFragment : Fragment(), HomepageSearchable {

    private val videoSectionViewModel by viewModels<VideoSectionViewModel>()

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private var _binding: FragmentVideoSectionBinding? = null
    private val binding get() = _binding!!

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVideoSectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initVideoSectionComposeView()
        setupMiniAudioPlayer()
    }

    private fun initVideoSectionComposeView() {
        binding.videoSectionComposeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                AndroidTheme(isDark = themeMode.isDarkMode()) {
                    VideoSectionComposeView(videoSectionViewModel = videoSectionViewModel)
                }
            }
        }
    }

    /**
     * Establish the mini audio player
     */
    private fun setupMiniAudioPlayer() {
        val audioPlayerController = MiniAudioPlayerController(binding.miniAudioPlayer).apply {
            shouldVisible = true
        }
        lifecycle.addObserver(audioPlayerController)
    }

    /**
     * onDestroyView
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}