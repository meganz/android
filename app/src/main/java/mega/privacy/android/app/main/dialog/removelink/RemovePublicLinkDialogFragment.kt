package mega.privacy.android.app.main.dialog.removelink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

@AndroidEntryPoint
internal class RemovePublicLinkDialogFragment : DialogFragment() {
    private val viewModel: RemovePublicLinkViewModel by viewModels()

    private val activityViewModel: ManagerViewModel by activityViewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        val ids = requireArguments().getLongArray(EXTRA_NODE_IDS) ?: LongArray(0)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            MegaAppTheme(isDark = themeMode.isDarkMode()) {
                MegaAlertDialog(
                    text = pluralStringResource(
                        id = R.plurals.remove_links_warning_text,
                        count = ids.size
                    ),
                    confirmButtonText = stringResource(id = R.string.general_remove),
                    cancelButtonText = stringResource(id = R.string.general_cancel),
                    onConfirm = {
                        activityViewModel.disableExport(ids.toList())
                        dismissAllowingStateLoss()
                    },
                    onDismiss = { dismissAllowingStateLoss() },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.collectFlow(viewModel.state) { state ->
            if (state.isNodeTakenDown) {
                dismissAllowingStateLoss()
            }
        }
    }

    companion object {
        const val TAG = "RemovePublicLinkDialogFragment"
        const val EXTRA_NODE_IDS = "EXTRA_NODE_IDS"
        fun newInstance(ids: List<Long>) = RemovePublicLinkDialogFragment().apply {
            arguments = Bundle().apply { putLongArray(EXTRA_NODE_IDS, ids.toLongArray()) }
        }
    }
}