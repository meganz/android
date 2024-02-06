package mega.privacy.android.app.presentation.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.components.transferWidget.TransfersWidgetView
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.modalbottomsheet.SortByBottomSheetDialogFragment
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.presentation.clouddrive.FileBrowserViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.filelink.view.animationScale
import mega.privacy.android.app.presentation.filelink.view.animationSpecs
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.mapper.GetIntentToOpenFileMapper
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.presentation.node.NodeBottomSheetActionHandler
import mega.privacy.android.app.presentation.node.NodeOptionsBottomSheetViewModel
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.app.presentation.search.navigation.contactArraySeparator
import mega.privacy.android.app.presentation.search.navigation.searchForeignNodeDialog
import mega.privacy.android.app.presentation.search.navigation.searchOverQuotaDialog
import mega.privacy.android.app.presentation.search.navigation.shareFolderAccessDialog
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarShower
import mega.privacy.android.app.presentation.transfers.TransfersManagementViewModel
import mega.privacy.android.app.presentation.transfers.startdownload.view.StartDownloadComponent
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.mobile.analytics.event.SearchAudioFilterPressedEvent
import mega.privacy.mobile.analytics.event.SearchDocsFilterPressedEvent
import mega.privacy.mobile.analytics.event.SearchImageFilterPressedEvent
import mega.privacy.mobile.analytics.event.SearchResetFilterPressedEvent
import mega.privacy.mobile.analytics.event.SearchVideosFilterPressedEvent
import timber.log.Timber
import javax.inject.Inject

/**
 * Search activity to search Nodes and display
 */
@AndroidEntryPoint
class SearchActivity : AppCompatActivity(), MegaSnackbarShower {
    private val viewModel: SearchActivityViewModel by viewModels()
    private val nodeOptionsBottomSheetViewModel: NodeOptionsBottomSheetViewModel by viewModels()
    private val sortByHeaderViewModel: SortByHeaderViewModel by viewModels()
    private val transfersManagementViewModel: TransfersManagementViewModel by viewModels()

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * Transfers management
     */
    @Inject
    lateinit var transfersManagement: TransfersManagement

    private val nameCollisionActivityContract =
        registerForActivityResult(NameCollisionActivityContract()) { result: String? ->
            if (result != null) {
                lifecycleScope.launch {
                    snackbarHostState.showSnackbar(result)
                }
            }
        }

    /**
     * Move request message mapper
     */
    @Inject
    lateinit var moveRequestMessageMapper: MoveRequestMessageMapper

    private val snackbarHostState = SnackbarHostState()

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
            nodeSourceType: NodeSourceType,
            parentHandle: Long,
            isFirstNavigationLevel: Boolean = false,
        ): Intent = Intent(context, SearchActivity::class.java).apply {
            putExtra(IS_FIRST_LEVEL, isFirstNavigationLevel)
            putExtra(SEARCH_TYPE, nodeSourceType)
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
    @OptIn(ExperimentalMaterialNavigationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        //Should be done in onCreate to avoid the issue that the activity is attempting to register while current state is RESUMED. LifecycleOwners must call register before they are STARTED.
        val bottomSheetActionHandler =
            NodeBottomSheetActionHandler(this, nodeOptionsBottomSheetViewModel)
        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            val nodeOptionsBottomSheetState by nodeOptionsBottomSheetViewModel.state.collectAsStateWithLifecycle()
            val transferState by transfersManagementViewModel.state.collectAsStateWithLifecycle()

            // Remember a SystemUiController
            val systemUiController = rememberSystemUiController()
            val useDarkIcons = themeMode.isDarkMode().not()
            systemUiController.setSystemBarsColor(
                color = Color.Transparent,
                darkIcons = useDarkIcons
            )

            val scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState)
            val bottomSheetNavigator = rememberBottomSheetNavigator()
            val navHostController = rememberNavController(bottomSheetNavigator)

            MegaAppTheme(isDark = themeMode.isDarkMode()) {
                Scaffold(
                    modifier = Modifier,
                    scaffoldState = scaffoldState,
                    snackbarHost = {
                        SnackbarHost(
                            modifier = Modifier.navigationBarsPadding(),
                            hostState = snackbarHostState
                        ) { data ->
                            MegaSnackbar(snackbarData = data)
                        }
                    },
                    floatingActionButton = {
                        AnimatedVisibility(
                            visible = transferState.widgetVisible,
                            enter = scaleIn(animationSpecs, initialScale = animationScale) +
                                    fadeIn(animationSpecs),
                            exit = scaleOut(animationSpecs, targetScale = animationScale) +
                                    fadeOut(animationSpecs),
                        ) {
                            TransfersWidgetView(
                                transfersData = transferState.transfersInfo,
                                onClick = ::transfersWidgetClicked,
                            )
                        }
                    },
                ) { padding ->
                    SearchNavHostController(
                        modifier = Modifier
                            .padding(padding)
                            .statusBarsPadding(),
                        viewModel = viewModel,
                        nodeOptionsBottomSheetViewModel = nodeOptionsBottomSheetViewModel,
                        handleClick = ::handleClick,
                        navigateToLink = ::navigateToLink,
                        showSortOrderBottomSheet = ::showSortOrderBottomSheet,
                        trackAnalytics = ::trackAnalytics,
                        nodeBottomSheetActionHandler = bottomSheetActionHandler,
                        navHostController = navHostController,
                        bottomSheetNavigator = bottomSheetNavigator,
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
                    event = nodeOptionsBottomSheetState.nodeNameCollisionResult,
                    onConsumed = nodeOptionsBottomSheetViewModel::markHandleNodeNameCollisionResult,
                    action = {
                        handleNodesNameCollisionResult(it)
                    }
                )
                EventEffect(
                    event = nodeOptionsBottomSheetState.showForeignNodeDialog,
                    onConsumed = nodeOptionsBottomSheetViewModel::markForeignNodeDialogShown,
                    action = { navHostController.navigate(searchForeignNodeDialog) }
                )
                EventEffect(
                    event = nodeOptionsBottomSheetState.showQuotaDialog,
                    onConsumed = nodeOptionsBottomSheetViewModel::markQuotaDialogShown,
                    action = {
                        navHostController.navigate(searchOverQuotaDialog.plus("/${it}"))
                    }
                )
                EventEffect(
                    event = nodeOptionsBottomSheetState.contactsData,
                    onConsumed = nodeOptionsBottomSheetViewModel::markShareFolderAccessDialogShown,
                    action = {
                        val contactList = it.first.joinToString(separator = contactArraySeparator)
                        navHostController.navigate(
                            shareFolderAccessDialog.plus("/${contactList}").plus("/${it.second}")
                        )
                    },
                )
                StartDownloadComponent(
                    event = nodeOptionsBottomSheetState.downloadEvent,
                    onConsumeEvent = nodeOptionsBottomSheetViewModel::markDownloadEventConsumed,
                    snackBarHostState = snackbarHostState,
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
                SearchCategory.ALL_DOCUMENTS -> SearchDocsFilterPressedEvent
                SearchCategory.AUDIO -> SearchAudioFilterPressedEvent
                SearchCategory.VIDEO -> SearchVideosFilterPressedEvent
                else -> SearchResetFilterPressedEvent
            }
        }
        Analytics.tracker.trackEvent(event)
    }

    private fun handleNodesNameCollisionResult(result: NodeNameCollisionResult) {
        if (result.conflictNodes.isNotEmpty()) {
            nameCollisionActivityContract
                .launch(
                    ArrayList(
                        result.conflictNodes.values.map {
                            when (result.type) {
                                NodeNameCollisionType.RESTORE,
                                NodeNameCollisionType.MOVE,
                                -> NameCollision.Movement.getMovementCollision(it)

                                NodeNameCollisionType.COPY -> NameCollision.Copy.getCopyCollision(it)
                            }
                        },
                    )
                )
        }
        if (result.noConflictNodes.isNotEmpty()) {
            when (result.type) {
                NodeNameCollisionType.MOVE -> nodeOptionsBottomSheetViewModel.moveNodes(result.noConflictNodes)
                NodeNameCollisionType.COPY -> nodeOptionsBottomSheetViewModel.copyNodes(result.noConflictNodes)
                else -> Timber.d("Not implemented")
            }
        }
    }

    override fun showMegaSnackbar(
        message: String,
        actionLabel: String?,
        duration: MegaSnackbarDuration,
    ) {
        lifecycleScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = when (duration) {
                    MegaSnackbarDuration.Short -> SnackbarDuration.Short
                    MegaSnackbarDuration.Long -> SnackbarDuration.Long
                    MegaSnackbarDuration.Indefinite -> SnackbarDuration.Indefinite
                }
            )
        }
    }

    private fun transfersWidgetClicked() {
        transfersManagement.setAreFailedTransfers(false)
        startActivity(
            Intent(this, ManagerActivity::class.java)
                .setAction(Constants.ACTION_SHOW_TRANSFERS)
                .putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.PENDING_TAB)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
        if (transfersManagement.isOnTransferOverQuota()) {
            transfersManagement.setHasNotToBeShowDueToTransferOverQuota(true)
        }
    }

}