package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.view.NodeAttachmentBottomSheetContent
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.offline.optionbottomsheet.OfflineOptionsViewModel.Companion.NODE_HANDLE
import mega.privacy.android.app.presentation.transfers.starttransfer.StartDownloadViewModel
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

@AndroidEntryPoint
internal class NodeAttachmentBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val viewModel: NodeAttachmentBottomSheetViewModel by viewModels()
    private val startDownloadViewModel: StartDownloadViewModel by activityViewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var fileTypeIconMapper: FileTypeIconMapper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                    NodeAttachmentBottomSheetContent(
                        uiState = uiState,
                        fileTypeIconMapper = fileTypeIconMapper,
                        onAvailableOfflineChecked = {},
                        onSaveToDeviceClicked = {},
                        onImportClicked = {}
                    )
                }

                EventEffect(
                    event = uiState.errorEvent,
                    onConsumed = viewModel::onErrorEventConsumed
                ) { onBackPressed() }
            }
        }
    }

    private fun onBackPressed() {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }

    companion object {
        fun newInstance(nodeHandle: Long) = NodeAttachmentBottomSheetDialogFragment().apply {
            arguments = bundleOf(NODE_HANDLE to nodeHandle)
        }
    }
}