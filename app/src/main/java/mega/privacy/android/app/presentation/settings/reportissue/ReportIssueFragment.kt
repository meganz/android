package mega.privacy.android.app.presentation.settings.reportissue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.settings.reportissue.model.SubmitIssueResult
import mega.privacy.android.app.presentation.settings.reportissue.view.DiscardReportDialog
import mega.privacy.android.app.presentation.settings.reportissue.view.ReportIssueView
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

/**
 * Report issue fragment
 *
 */
@AndroidEntryPoint
class ReportIssueFragment : Fragment() {

    private val viewModel: ReportIssueViewModel by viewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            val themeMode by getThemeMode()
                .collectAsState(initial = ThemeMode.System)
            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                ReportIssueView(viewModel)
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val valid = viewModel.uiState.value.canSubmit
        val color = context?.getColor(if (valid) R.color.teal_300 else R.color.color_icon_disabled)
        menu.findItem(R.id.menu_report_issue_submit)?.let { menuItem ->
            menuItem.isEnabled = valid
            color?.let { menuItem.icon?.setTint(it) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.report_issue_submit_action, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_report_issue_submit -> {
                viewModel.submit()
                true
            }

            android.R.id.home -> {
                activity?.onBackPressedDispatcher?.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.uiState
                    .distinctUntilChanged { old, new -> old.canSubmit == new.canSubmit && old.result == new.result }
                    .collect { state ->
                        if (state.result != null) {
                            finishWithResult(state.result)
                        } else {
                            requireActivity().invalidateOptionsMenu()
                        }
                    }
            }
        }
    }

    private fun finishWithResult(result: SubmitIssueResult) {
        tag?.let { tag ->
            context?.let { context ->
                setFragmentResult(
                    tag,
                    bundleOf(
                        ReportIssueFragment::class.java.name to result.getResultString(
                            context
                        )
                    )
                )
            }
        }
        parentFragmentManager.popBackStack()
    }

    /**
     * Report issue view
     *
     * @param viewModel
     */
    @Composable
    fun ReportIssueView(
        viewModel: ReportIssueViewModel = hiltViewModel(),
    ) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        ReportIssueView(
            uiState = uiState,
            onDescriptionChanged = viewModel::setDescription,
            onIncludeLogsChanged = viewModel::setIncludeLogsEnabled,
            cancelUpload = viewModel::cancelUpload,
        )

        ReportIssueBackHandler(
            isEnabled = uiState.description.isNotBlank(),
            onDiscard = parentFragmentManager::popBackStack,
        )
    }

}

/**
 * Report issue back handler
 *
 * @param isEnabled
 * @param onDiscard
 */
@Composable
fun ReportIssueBackHandler(
    isEnabled: Boolean,
    onDiscard: () -> Unit,
) {
    var showDiscardDialog by remember { mutableStateOf(false) }

    BackHandler(isEnabled) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        DiscardReportDialog(
            onDiscardCancelled = { showDiscardDialog = false },
            onDiscard = onDiscard
        )
    }
}