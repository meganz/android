package mega.privacy.android.app.presentation.photos.compose.photosfilter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.photos.timeline.photosfilter.view.PhotosFilterView
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.applyFilter
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.onMediaTypeSelected
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.onSourceSelected
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.showingFilterPage

@Composable
fun PhotosFilterScreen(
    timelineViewModel: TimelineViewModel,
    onExitScreen: () -> Unit,
) {
    val timeState by timelineViewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    PhotosFilterView(
        timelineViewState = timeState,
        onMediaTypeSelected = timelineViewModel::onMediaTypeSelected,
        onSourceSelected = timelineViewModel::onSourceSelected,
        applyFilter = {
            coroutineScope.launch {
                timelineViewModel.showingFilterPage(isShowing = false)
                timelineViewModel.applyFilter()

                onExitScreen()
            }
        },
    )
}
