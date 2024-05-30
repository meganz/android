package mega.privacy.android.app.presentation.offline.offlinecompose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.main.HomepageFragment
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.offline.optionbottomsheet.OfflineOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.offline.view.OfflineFeatureScreen
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import timber.log.Timber
import javax.inject.Inject

/**
 * OfflineFragment with Compose
 */
@AndroidEntryPoint
class OfflineComposeFragment : Fragment(), ActionMode.Callback {

    /**
     * getThemeMode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * fileTypeIconMapper [FileTypeIconMapper]
     */
    @Inject
    lateinit var fileTypeIconMapper: FileTypeIconMapper

    private val viewModel: OfflineComposeViewModel by activityViewModels()
    private val args: OfflineComposeFragmentArgs by navArgs()
    private var actionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments == null) {
            arguments =
                HomepageFragmentDirections.actionHomepageFragmentToOfflineFragmentCompose().arguments
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val backgroundColor = Color(
            ColorUtils.getColorForElevation(
                requireContext(),
                Util.dp2px(HomepageFragment.BOTTOM_SHEET_ELEVATION).toFloat()
            )
        )
        return ComposeView(requireContext()).apply {
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDarkMode = themeMode.isDarkMode()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                OriginalTempTheme(isDark = isDarkMode) {
                    OfflineFeatureScreen(
                        uiState = uiState,
                        backgroundColor = backgroundColor,
                        fileTypeIconMapper = fileTypeIconMapper,
                        rootFolderOnly = args.rootFolderOnly,
                        onOfflineItemClicked = viewModel::onItemClicked,
                        onItemLongClicked = {
                            if (args.rootFolderOnly.not()) {
                                viewModel.onLongItemClicked(it)
                                if (actionMode == null) {
                                    actionMode =
                                        (requireActivity() as AppCompatActivity).startSupportActionMode(
                                            this@OfflineComposeFragment
                                        )
                                }
                            } else {
                                viewModel.onItemClicked(it)
                            }
                        },
                        onOptionClicked = {
                            showOptionPanelBottomSheet(it.offlineNode.handle)
                        }
                    )
                    (requireActivity() as? ManagerActivity)?.setToolbarTitleFromFullscreenOfflineFragment(
                        title = uiState.title.ifEmpty { stringResource(id = R.string.section_saved_for_offline_new) },
                        firstNavigationLevel = false,
                        showSearch = true
                    )
                    updateActionModeTitle(
                        selectedItemsCount = uiState.selectedNodeHandles.size
                    )
                }
            }
        }
    }

    private fun showOptionPanelBottomSheet(nodeHandle: Long) {
        val tag = OfflineOptionsBottomSheetDialogFragment::class.java.simpleName
        if (childFragmentManager.findFragmentByTag(tag) != null) return
        OfflineOptionsBottomSheetDialogFragment.newInstance(nodeHandle)
            .show(
                childFragmentManager,
                OfflineOptionsBottomSheetDialogFragment::class.java.simpleName
            )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        callManager {
            if (args.rootFolderOnly) {
                it.pagerOfflineComposeFragmentOpened(this)
            } else {
                it.fullscreenOfflineFragmentComposeOpened(this)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        callManager {
            if (args.rootFolderOnly) {
                it.pagerOfflineComposeFragmentClosed(this)
            } else {
                it.fullscreenOfflineFragmentComposeClosed(this)
            }
        }
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        Timber.d("ActionBarCallBack::onPrepareActionMode")

        menu?.findItem(R.id.cab_menu_select_all)?.isVisible =
            viewModel.uiState.value.selectedNodeHandles.size < viewModel.uiState.value.offlineNodes.size

        return true
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        Timber.d("ActionBarCallBack::onCreateActionMode")
        val inflater = mode!!.menuInflater

        inflater.inflate(R.menu.offline_browser_action, menu)

        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        Timber.d("ActionBarCallBack::onDestroyActionMode")
        viewModel.clearSelection()
        actionMode = null
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        Timber.d("ActionBarCallBack::onActionItemClicked")

        when (item!!.itemId) {
            R.id.cab_menu_download -> {
                callManager {
                    it.saveHandlesToDevice(
                        viewModel.uiState.value.selectedNodeHandles,
                        true
                    )
                }
                viewModel.clearSelection()
            }

            R.id.cab_menu_share_out -> {
                viewModel.clearSelection()
            }

            R.id.cab_menu_delete -> {
                viewModel.clearSelection()
            }

            R.id.cab_menu_select_all -> {
                viewModel.selectAll()
            }

            R.id.cab_menu_clear_selection -> {
                viewModel.clearSelection()
                actionMode?.finish()
            }
        }

        return false
    }

    private fun updateActionModeTitle(selectedItemsCount: Int) {
        actionMode?.let {
            actionMode?.title = when {
                (selectedItemsCount == 0) -> {
                    actionMode?.finish()
                    0.toString()
                }

                else -> "$selectedItemsCount"
            }

            runCatching {
                actionMode?.invalidate()
            }.onFailure {
                Timber.e(it, "Invalidate error")
            }
        } ?: run {
            return
        }
    }
}