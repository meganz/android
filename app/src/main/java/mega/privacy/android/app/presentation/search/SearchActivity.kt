package mega.privacy.android.app.presentation.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.modalbottomsheet.SortByBottomSheetDialogFragment
import mega.privacy.android.app.presentation.clouddrive.FileBrowserViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.mapper.GetIntentToOpenFileMapper
import mega.privacy.android.app.presentation.node.NodeBottomSheetActionHandler
import mega.privacy.android.app.presentation.node.dialogs.changeextension.ChangeNodeExtensionAction
import mega.privacy.android.app.presentation.node.dialogs.changeextension.ChangeNodeExtensionDialogViewModel
import mega.privacy.android.app.presentation.node.dialogs.renamenode.RenameNodeDialogViewModel
import mega.privacy.android.app.presentation.node.dialogs.deletenode.MoveToRubbishOrDeleteNodeDialogViewModel
import mega.privacy.android.app.presentation.node.dialogs.removelink.RemoveNodeLinkViewModel
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchType
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.mobile.analytics.event.SearchAudioFilterPressedEvent
import mega.privacy.mobile.analytics.event.SearchDocsFilterPressedEvent
import mega.privacy.mobile.analytics.event.SearchImageFilterPressedEvent
import mega.privacy.mobile.analytics.event.SearchResetFilterPressedEvent
import mega.privacy.mobile.analytics.event.SearchVideosFilterPressedEvent
import mega.privacy.android.app.presentation.node.dialogs.renamenode.RenameNodeDialogAction.OnRenameSucceeded
import timber.log.Timber
import javax.inject.Inject

/**
 * Search activity to search Nodes and display
 */
@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    private val viewModel: SearchActivityViewModel by viewModels()
    private val moveToRubbishOrDeleteNodeDialogViewModel: MoveToRubbishOrDeleteNodeDialogViewModel by viewModels()
    private val renameNodeDialogViewModel: RenameNodeDialogViewModel by viewModels()
    private val removeNodeLinkViewModel: RemoveNodeLinkViewModel by viewModels()
    private val changeNodeExtensionDialogViewModel: ChangeNodeExtensionDialogViewModel by viewModels()

    private val sortByHeaderViewModel: SortByHeaderViewModel by viewModels()

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    companion object {
        /**
         * Checks if first navigation level
         */
        const val IS_FIRST_LEVEL = "isFirstLevel"

        /**
         * Parent search handle
         */
        const val PARENT_HANDLE = "parentHandle"

        /**
         * Search type
         */
        const val SEARCH_TYPE = "searchType"

        /**
         * Search node handle
         */
        const val SEARCH_NODE_HANDLE = "searchNodeHandle"

        /**
         * Get Search activity Intent
         */
        fun getIntent(
            context: Context,
            searchType: SearchType,
            parentHandle: Long,
            isFirstNavigationLevel: Boolean = false,
        ): Intent = Intent(context, SearchActivity::class.java).apply {
            putExtra(IS_FIRST_LEVEL, isFirstNavigationLevel)
            putExtra(SEARCH_TYPE, searchType)
            putExtra(PARENT_HANDLE, parentHandle)
        }
    }

    /**
     * Mapper to open file
     */
    @Inject
    lateinit var getIntentToOpenFileMapper: GetIntentToOpenFileMapper

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            val moveToRubbishState by moveToRubbishOrDeleteNodeDialogViewModel.state.collectAsStateWithLifecycle()
            val renameFolderState by renameNodeDialogViewModel.state.collectAsStateWithLifecycle()
            val removeLinkState by removeNodeLinkViewModel.state.collectAsStateWithLifecycle()
            val changeNodeExtensionDialogViewModelState by changeNodeExtensionDialogViewModel.state.collectAsStateWithLifecycle()

            val scaffoldState = rememberScaffoldState()
            MegaAppTheme(isDark = themeMode.isDarkMode()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    scaffoldState = scaffoldState,
                ) { padding ->
                    SearchNavHostController(
                        modifier = Modifier.padding(padding),
                        viewModel = viewModel,
                        moveToRubbishOrDeleteNodeDialogViewModel = moveToRubbishOrDeleteNodeDialogViewModel,
                        renameNodeDialogViewModel = renameNodeDialogViewModel,
                        removeNodeLinkViewModel = removeNodeLinkViewModel,
                        changeNodeExtensionDialogViewModel = changeNodeExtensionDialogViewModel,
                        handleClick = ::handleClick,
                        navigateToLink = ::navigateToLink,
                        showSortOrderBottomSheet = ::showSortOrderBottomSheet,
                        trackAnalytics = ::trackAnalytics,
                        nodeBottomSheetActionHandler = NodeBottomSheetActionHandler(this),
                        onBackPressed = {
                            if (viewModel.state.value.selectedNodes.isNotEmpty()) {
                                viewModel.clearSelection()
                            } else {
                                onBackPressedDispatcher.onBackPressed()
                            }
                        }
                    )
                }

                EventEffect(
                    moveToRubbishState.deleteEvent,
                    onConsumed = {
                        moveToRubbishOrDeleteNodeDialogViewModel.consumeDeleteEvent()
                    }
                ) {
                    scaffoldState.snackbarHostState.showSnackbar(it)
                }

                val renameSuccessMessage = stringResource(R.string.context_correctly_renamed)
                EventEffect(
                    event = renameFolderState.renameSuccessfulEvent,
                    onConsumed = {
                        renameNodeDialogViewModel.handleAction(OnRenameSucceeded)
                    }
                ) {
                    scaffoldState.snackbarHostState.showSnackbar(renameSuccessMessage)
                }

                EventEffect(
                    event = removeLinkState.removeLinkEvent,
                    onConsumed = {
                        removeNodeLinkViewModel.consumeDeleteEvent()
                    }
                ) {
                    scaffoldState.snackbarHostState.showSnackbar(it)
                }

                EventEffect(
                    event = changeNodeExtensionDialogViewModelState.renameSuccessfulEvent,
                    onConsumed = {
                        changeNodeExtensionDialogViewModel.handleAction(
                            ChangeNodeExtensionAction.OnChangeExtensionConsumed
                        )
                    },
                    action = {
                        scaffoldState.snackbarHostState.showSnackbar(
                            renameSuccessMessage
                        )
                    }
                )
            }
        }

        sortByHeaderViewModel.orderChangeEvent.observe(this) {
            viewModel.onSortOrderChanged()
        }
    }

    private fun handleClick(node: TypedNode?) = node?.let {
        when (it) {
            is FileNode -> openFileClicked(it)
            is FolderNode -> openFolderClicked(it.id.longValue)
            else -> Timber.e("Unsupported click")
        }
    }

    /**
     * Clicked on link
     * @param link
     */
    private fun navigateToLink(link: String) {
        val uriUrl = Uri.parse(link)
        val launchBrowser = Intent(this, WebViewActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setData(uriUrl)
        startActivity(launchBrowser)
    }

    /**
     * On Item click event received from [FileBrowserViewModel]
     *
     * @param folderHandle FolderHandle of current selected Folder
     */
    private fun openFolderClicked(folderHandle: Long?) {
        folderHandle?.let {
            val intent = Intent().apply {
                putExtra(SEARCH_NODE_HANDLE, it)
            }
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    /**
     * On Item click event received from [FileBrowserViewModel]
     *
     * @param currentFileNode [FileNode]
     */
    private fun openFileClicked(currentFileNode: FileNode?) {
        currentFileNode?.let {
            openFile(fileNode = it)
            viewModel.onItemPerformedClicked()
        } ?: run {
            // Update toolbar title here
        }
    }

    /**
     * Open File
     * @param fileNode [FileNode]
     */
    private fun openFile(fileNode: FileNode) {
        lifecycleScope.launch {
            runCatching {
                val intent = getIntentToOpenFileMapper(
                    activity = this@SearchActivity,
                    fileNode = fileNode,
                    viewType = Constants.FILE_BROWSER_ADAPTER
                )
                intent?.let {
                    if (MegaApiUtils.isIntentAvailable(this@SearchActivity, it)) {
                        startActivity(it)
                    } else {
                        Toast.makeText(
                            this@SearchActivity,
                            getString(R.string.intent_not_available),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }.onFailure {
                Timber.e("itemClick:ERROR:httpServerGetLocalLink")
                viewModel.showShowErrorMessage(errorMessageResId = R.string.general_text_error)
            }
        }
    }

    private fun showSortOrderBottomSheet() {
        val bottomSheetDialogFragment =
            SortByBottomSheetDialogFragment.newInstance(Constants.ORDER_CLOUD)
        bottomSheetDialogFragment.show(
            supportFragmentManager,
            bottomSheetDialogFragment.tag
        )
    }

    private fun trackAnalytics(selectedFilter: SearchFilter?) {
        val event = if (viewModel.state.value.selectedFilter?.filter == selectedFilter?.filter) {
            SearchResetFilterPressedEvent
        } else {
            when (selectedFilter?.filter) {
                SearchCategory.IMAGES -> SearchImageFilterPressedEvent
                SearchCategory.DOCUMENTS -> SearchDocsFilterPressedEvent
                SearchCategory.AUDIO -> SearchAudioFilterPressedEvent
                SearchCategory.VIDEO -> SearchVideosFilterPressedEvent
                else -> SearchResetFilterPressedEvent
            }
        }
        Analytics.tracker.trackEvent(event)
    }
}