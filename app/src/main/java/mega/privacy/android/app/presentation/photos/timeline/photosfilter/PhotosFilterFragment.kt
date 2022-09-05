package mega.privacy.android.app.presentation.photos.timeline.photosfilter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.timeline.photosfilter.view.PhotosFilterView
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.applyFilter
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.onMediaTypeSelected
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.onSourceSelected
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.showingFilterPage
import mega.privacy.android.presentation.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
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
                    .collectAsState(initial = ThemeMode.System)
                AndroidTheme(isDark = mode.isDarkMode()) {
                    PhotosFilterBody()
                }
            }
        }
    }

    @Composable
    fun PhotosFilterBody() {
        val timelineViewState by timelineViewModel.state.collectAsState()

        PhotosFilterView(
            timelineViewState = timelineViewState,
            onMediaTypeSelected = timelineViewModel::onMediaTypeSelected,
            onSourceSelected = timelineViewModel::onSourceSelected,
            applyFilter = this::applyFilter
        )
    }

    /**
     * Apply the filter from the Photos Filter
     */
    fun applyFilter() {
        lifecycleScope.launch {
            timelineViewModel.showingFilterPage(false)
            timelineViewModel.applyFilter()
            mManagerActivity.onBackPressed()
        }
    }
}