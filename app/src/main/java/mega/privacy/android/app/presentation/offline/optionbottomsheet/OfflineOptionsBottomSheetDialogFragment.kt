package mega.privacy.android.app.presentation.offline.optionbottomsheet

import android.content.Intent
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
import mega.privacy.android.app.activities.OfflineFileInfoActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.offline.action.OfflineNodeActionsViewModel
import mega.privacy.android.app.presentation.offline.confirmremovedialog.ConfirmRemoveFromOfflineDialogFragment
import mega.privacy.android.app.presentation.offline.optionbottomsheet.OfflineOptionsViewModel.Companion.NODE_HANDLE
import mega.privacy.android.app.presentation.offline.optionbottomsheet.view.OfflineOptionsContent
import mega.privacy.android.app.presentation.transfers.starttransfer.StartDownloadViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

@AndroidEntryPoint
internal class OfflineOptionsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val viewModel: OfflineOptionsViewModel by viewModels()
    private val offlineNodeActionsViewModel: OfflineNodeActionsViewModel by activityViewModels()
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
                    OfflineOptionsContent(
                        uiState = uiState,
                        fileTypeIconMapper = fileTypeIconMapper,
                        onRemoveFromOfflineClicked = { showConfirmationRemoveOfflineNode(uiState.nodeId) },
                        onOpenInfoClicked = { openInfo(uiState.nodeId) },
                        onOpenWithClicked = { openWith(it) },
                        onSaveToDeviceClicked = { saveToDevice(uiState.nodeId) },
                        onShareNodeClicked = {
                            shareOfflineNode(
                                offlineFileInformation = it,
                                isOnline = uiState.isOnline
                            )
                        },
                    )
                }

                EventEffect(
                    event = uiState.errorEvent,
                    onConsumed = viewModel::onErrorEventConsumed
                ) { onBackPressed() }
            }
        }
    }

    private fun openInfo(nodeId: NodeId) {
        val offlineIntent = Intent(requireContext(), OfflineFileInfoActivity::class.java)
        offlineIntent.putExtra(Constants.HANDLE, nodeId.longValue.toString())
        startActivity(offlineIntent)
        dismissAllowingStateLoss()
    }

    private fun shareOfflineNode(
        offlineFileInformation: OfflineFileInformation,
        isOnline: Boolean,
    ) {
        offlineNodeActionsViewModel.handleShareOfflineNodes(
            nodes = listOf(offlineFileInformation),
            isOnline = isOnline
        )
        dismissAllowingStateLoss()
    }

    private fun openWith(offlineFileInformation: OfflineFileInformation) {
        offlineNodeActionsViewModel.handleOpenWithIntent(offlineFileInformation)
        dismissAllowingStateLoss()
    }

    private fun saveToDevice(nodeId: NodeId) {
        startDownloadViewModel.onCopyOfflineNodeClicked(listOf(nodeId))
        dismissAllowingStateLoss()
    }

    private fun showConfirmationRemoveOfflineNode(nodeId: NodeId) {
        ConfirmRemoveFromOfflineDialogFragment.newInstance(listOf(nodeId.longValue))
            .show(
                requireActivity().supportFragmentManager,
                ConfirmRemoveFromOfflineDialogFragment::class.java.simpleName
            )
        dismissAllowingStateLoss()
    }

    private fun onBackPressed() {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }

    companion object {
        fun newInstance(nodeHandle: Long) = OfflineOptionsBottomSheetDialogFragment().apply {
            arguments = bundleOf(NODE_HANDLE to nodeHandle)
        }
    }
}