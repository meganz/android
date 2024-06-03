package mega.privacy.android.app.presentation.offline.confirmremovedialog

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

@AndroidEntryPoint
internal class ConfirmRemoveFromOfflineDialogFragment : DialogFragment() {
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val activityViewModel by activityViewModels<ManagerViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val handles = requireArguments().getLongArray(EXTRA_HANDLES)?.toList().orEmpty()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                    MegaAlertDialog(
                        text = stringResource(id = R.string.confirmation_delete_from_save_for_offline),
                        confirmButtonText = stringResource(id = R.string.general_remove),
                        cancelButtonText = stringResource(id = R.string.general_cancel),
                        onConfirm = {
                            activityViewModel.removeOfflineNodes(handles)
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
        private const val EXTRA_HANDLES = "EXTRA_HANDLES"

        fun newInstance(handles: List<Long>): ConfirmRemoveFromOfflineDialogFragment {
            return ConfirmRemoveFromOfflineDialogFragment().apply {
                arguments = Bundle().apply {
                    putLongArray(EXTRA_HANDLES, handles.toLongArray())
                }
            }
        }
    }
}
