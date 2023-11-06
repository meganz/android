package mega.privacy.android.app.main.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class ClearRubbishBinDialogFragment : DialogFragment() {
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val nodeController: NodeController by lazy { NodeController(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("showClearRubbishBinDialog")
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                AndroidTheme(isDark = themeMode.isDarkMode()) {
                    ConfirmationDialog(
                        title = stringResource(id = R.string.context_clear_rubbish),
                        text = stringResource(id = R.string.clear_rubbish_confirmation),
                        confirmButtonText = stringResource(id = R.string.general_clear),
                        cancelButtonText = stringResource(id = R.string.general_cancel),
                        onConfirm = {
                            nodeController.cleanRubbishBin()
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
        /**
         * Tag
         */
        const val TAG = "ClearRubbishBinDialogFragment"
    }
}