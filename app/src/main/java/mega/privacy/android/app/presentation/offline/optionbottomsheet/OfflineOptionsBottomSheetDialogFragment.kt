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
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.activities.OfflineFileInfoActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.offline.confirmremovedialog.ConfirmRemoveFromOfflineDialogFragment
import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.OfflineFileInfoComposeViewModel
import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.OfflineFileInfoComposeViewModel.Companion.NODE_HANDLE
import mega.privacy.android.app.presentation.offline.optionbottomsheet.view.OfflineOptionsContent
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

@AndroidEntryPoint
internal class OfflineOptionsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val viewModel: OfflineFileInfoComposeViewModel by viewModels()

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
                val uiState by viewModel.state.collectAsStateWithLifecycle()

                OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                    OfflineOptionsContent(
                        uiState = uiState,
                        fileTypeIconMapper = fileTypeIconMapper,
                        onRemoveFromOfflineClicked = { showConfirmationRemoveOfflineNode(uiState.handle) },
                        onOpenInfoClicked = { openInfo(uiState.handle) },
                        onOpenWithClicked = { openWith(uiState.handle) },
                        onSaveToDeviceClicked = { saveToDevice(uiState.handle) },
                        onShareNodeClicked = { shareOfflineNode(uiState.handle) },
                    )
                }

                EventEffect(
                    event = uiState.errorEvent,
                    onConsumed = viewModel::onErrorEventConsumed
                ) { onBackPressed() }
            }
        }
    }

    private fun openInfo(handle: Long) {
        val offlineIntent = Intent(requireContext(), OfflineFileInfoActivity::class.java)
        offlineIntent.putExtra(Constants.HANDLE, handle.toString())
        startActivity(offlineIntent)
        dismissAllowingStateLoss()
    }

    private fun shareOfflineNode(handle: Long) {
        OfflineUtils.shareOfflineNode(
            requireContext(),
            handle
        )
        dismissAllowingStateLoss()
    }

    private fun openWith(handle: Long) {
        OfflineUtils.openWithOffline(
            requireContext(),
            handle
        )
        dismissAllowingStateLoss()
    }

    private fun saveToDevice(handle: Long) {
        callManager {
            it.saveHandlesToDevice(
                listOf(handle),
                true
            )
        }
        dismissAllowingStateLoss()
    }

    private fun showConfirmationRemoveOfflineNode(handle: Long) {
        ConfirmRemoveFromOfflineDialogFragment.newInstance(listOf(handle))
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