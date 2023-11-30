package mega.privacy.android.app.presentation.photos.timeline.photosfilter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.compose.photosfilter.PhotosFilterScreen
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject

/**
 * Fragment for the filter in Photos page
 */
@AndroidEntryPoint
class PhotosFilterFragment : Fragment() {

    private lateinit var mManagerActivity: ManagerActivity
    private val timelineViewModel: TimelineViewModel by activityViewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var getFeatureFlagUseCase: GetFeatureFlagValueUseCase

    companion object {
        @JvmStatic
        fun getInstance(): PhotosFilterFragment = PhotosFilterFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mManagerActivity = activity as ManagerActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mManagerActivity.hideFabButton()
        return ComposeView(requireContext()).apply {
            setContent {
                val mode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                MegaAppTheme(isDark = mode.isDarkMode()) {
                    PhotosFilterScreen(
                        timelineViewModel = timelineViewModel,
                        onExitScreen = { activity?.onBackPressedDispatcher?.onBackPressed() },
                        getFeatureFlagUseCase = getFeatureFlagUseCase,
                    )
                }
            }
        }
    }
}
