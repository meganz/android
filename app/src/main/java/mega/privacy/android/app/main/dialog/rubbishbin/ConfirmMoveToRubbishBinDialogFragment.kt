package mega.privacy.android.app.main.dialog.rubbishbin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class ConfirmMoveToRubbishBinDialogFragment : DialogFragment() {
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel by viewModels<ConfirmMoveToRubbishBinViewModel>()
    private val activityViewModel by activityViewModels<ManagerViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("showConfirmMoveToRubbishBinDialog")
        val handles = requireArguments().getLongArray(EXTRA_HANDLES)?.toList().orEmpty()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val uiState by viewModel.state.collectAsStateWithLifecycle()
                val message = when {
                    uiState.isNodeInRubbish -> stringResource(id = R.string.confirmation_delete_from_mega)
                    else -> stringResource(id = R.string.confirmation_move_to_rubbish)
                }
                val positiveText = if (uiState.isNodeInRubbish) {
                    stringResource(id = R.string.rubbish_bin_delete_confirmation_dialog_button_delete)
                } else {
                    stringResource(id = R.string.general_move)
                }
                AndroidTheme(isDark = themeMode.isDarkMode()) {
                    MegaAlertDialog(
                        text = message,
                        confirmButtonText = positiveText,
                        cancelButtonText = stringResource(id = R.string.general_cancel),
                        onConfirm = {
                            if (uiState.isNodeInRubbish) {
                                activityViewModel.deleteNodes(handles)
                            } else {
                                activityViewModel.moveNodesToRubbishBin(handles)
                            }
                            dismissAllowingStateLoss()
                        },
                        onDismiss = {
                            dismissAllowingStateLoss()
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "ConfirmMoveToRubbishBinDialogFragment"
        const val EXTRA_HANDLES = "EXTRA_HANDLES"

        fun newInstance(handles: List<Long>): ConfirmMoveToRubbishBinDialogFragment {
            return ConfirmMoveToRubbishBinDialogFragment().apply {
                arguments = Bundle().apply {
                    putLongArray(EXTRA_HANDLES, handles.toLongArray())
                }
            }
        }
    }
}
