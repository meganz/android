package mega.privacy.android.app.presentation.videosection

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.SortByBottomSheetDialogFragment
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.videosection.view.videoselected.VideoSelectedScreen
import mega.privacy.android.app.utils.Constants.ORDER_CLOUD
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

/**
 * The activity for video selected
 */
@AndroidEntryPoint
class VideoSelectedActivity : PasscodeActivity() {
    /**
     * [GetThemeMode] injection
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * [FileTypeIconMapper] injection
     */
    @Inject
    lateinit var fileTypeIconMapper: FileTypeIconMapper

    internal val viewModel by viewModels<VideoSelectedViewModel>()
    private val sortByHeaderViewModel by viewModels<SortByHeaderViewModel>()

    private var bottomSheetDialogFragment: BottomSheetDialogFragment? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val state = viewModel.state.value
            when {
                state.selectedNodeHandles.isNotEmpty() ->
                    viewModel.clearAllSelectedVideos()

                state.searchState == SearchWidgetState.EXPANDED -> viewModel.closeSearch()

                state.currentFolderHandle != -1L -> {
                    viewModel.backToParentFolder()
                }

                else -> {
                    setResult(RESULT_CANCELED)
                    this@VideoSelectedActivity.finish()
                }
            }
        }
    }

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(
                initialValue = ThemeMode.System
            )
            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                VideoSelectedScreen(
                    viewModel = viewModel,
                    onBackPressed = {
                        onBackPressedCallback.handleOnBackPressed()
                    },
                    onSortOrderClick = { showNewSortByPanel() },
                    onVideoSelected = { selectedItems ->
                        val items = ArrayList(selectedItems.map { it.toString() })
                        setResult(
                            RESULT_OK,
                            Intent().putStringArrayListExtra(INTENT_KEY_VIDEO_SELECTED, items)
                        )
                        this.finish()
                    },
                    fileTypeIconMapper = fileTypeIconMapper
                )
            }
        }

        collectFlow(sortByHeaderViewModel.orderChangeState) {
            viewModel.refreshWhenOrderChanged()
        }
    }

    private fun showNewSortByPanel() {
        if (bottomSheetDialogFragment.isBottomSheetDialogShown()) {
            return
        }
        bottomSheetDialogFragment = SortByBottomSheetDialogFragment.newInstance(ORDER_CLOUD)
        bottomSheetDialogFragment?.show(
            supportFragmentManager,
            bottomSheetDialogFragment?.tag
        )
    }

    companion object {
        /**
         * The intent key for passing selected video handle
         */
        const val INTENT_KEY_VIDEO_SELECTED = "INTENT_KEY_VIDEO_SELECTED"
    }
}