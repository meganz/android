package mega.privacy.android.app.presentation.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.modalbottomsheet.SortByBottomSheetDialogFragment
import mega.privacy.android.app.presentation.clouddrive.FileBrowserViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.BackupsImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.CloudDriveImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.RubbishBinImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.SharedItemsImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.meeting.chat.extension.getInfo
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.presentation.node.FileNodeContent
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.NodeActionsViewModel
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.presentation.qrcode.findActivity
import mega.privacy.android.app.presentation.search.mapper.NodeSourceTypeToViewTypeMapper
import mega.privacy.android.app.presentation.search.navigation.contactArraySeparator
import mega.privacy.android.app.presentation.search.navigation.searchForeignNodeDialog
import mega.privacy.android.app.presentation.search.navigation.searchOverQuotaDialog
import mega.privacy.android.app.presentation.search.navigation.shareFolderAccessDialog
import mega.privacy.android.app.presentation.search.view.MiniAudioPlayerView
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarShower
import mega.privacy.android.app.presentation.transfers.TransfersManagementViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.presentation.transfers.view.IN_PROGRESS_TAB_INDEX
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.textEditor.TextEditorViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.widgets.TransfersWidgetViewAnimated
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Search activity to search Nodes and display
 */
@OptIn(ExperimentalComposeUiApi::class)
@AndroidEntryPoint
class SearchActivity : AppCompatActivity(), MegaSnackbarShower {
    private val viewModel: SearchActivityViewModel by viewModels()
    private val nodeActionsViewModel: NodeActionsViewModel by viewModels()
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

    /**
     * Mapper to convert node source type to Int
     */
    @Inject
    lateinit var nodeSourceTypeToViewTypeMapper: NodeSourceTypeToViewTypeMapper

    /**
     * Mapper to convert list to json for sending data in navigation
     */
    @Inject
    lateinit var listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper

    /**
     * MegaNavigator
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    /**
     * [GetFeatureFlagValueUseCase]
     */
    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    private val nameCollisionActivityLauncher = registerForActivityResult(
        NameCollisionActivityContract()
    ) { result ->
        if (result != null) {
            lifecycleScope.launch {
                snackbarHostState.showAutoDurationSnackbar(result)
            }
        }
    }

    /**
     * Move request message mapper
     */
    @Inject
    lateinit var moveRequestMessageMapper: MoveRequestMessageMapper

    /**
     * File type icon mapper
     */
    @Inject
    lateinit var fileTypeIconMapper: FileTypeIconMapper

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
     * onCreate
     */
    @OptIn(ExperimentalMaterialNavigationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        //Should be done in onCreate to avoid the issue that the activity is attempting to register while current state is RESUMED. LifecycleOwners must call register before they are STARTED.
        val bottomSheetActionHandler =
            NodeActionHandler(this, nodeActionsViewModel)
        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            val nodeActionState by nodeActionsViewModel.state.collectAsStateWithLifecycle()
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
            val coroutineScope = rememberCoroutineScope()
            SessionContainer {
                OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                    MegaScaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                            .imePadding()
                            .semantics { testTagsAsResourceId = true },
                        scaffoldState = scaffoldState,
                        floatingActionButton = {
                            if (!transferState.hideTransfersWidget) {
                                TransfersWidgetViewAnimated(
                                    transfersInfo = transferState.transfersInfo,
                                    onClick = ::transfersWidgetClicked,
                                    modifier = Modifier
                                        .navigationBarsPadding()
                                        .testTag(SEARCH_SCREEN_TRANSFERS_WIDGET_TEST_TAG)
                                )
                            }
                        },
                    ) { padding ->
                        ConstraintLayout(
                            modifier = Modifier
                                .padding(padding)
                                .fillMaxSize()
                        ) {
                            val (audioPlayer, searchContainer) = createRefs()
                            MiniAudioPlayerView(
                                modifier = Modifier
                                    .constrainAs(audioPlayer) {
                                        bottom.linkTo(parent.bottom)
                                    }
                                    .fillMaxWidth()
                                    .testTag(SEARCH_SCREEN_MINI_AUDIO_PLAYER_TEST_TAG),
                                lifecycle = lifecycle,
                            )

                            SearchNavHostController(
                                modifier = Modifier
                                    .constrainAs(searchContainer) {
                                        top.linkTo(parent.top)
                                        bottom.linkTo(audioPlayer.top)
                                        height = Dimension.fillToConstraints
                                    }
                                    .fillMaxWidth(),
                                viewModel = viewModel,
                                nodeActionsViewModel = nodeActionsViewModel,
                                navigateToLink = ::navigateToLink,
                                showSortOrderBottomSheet = ::showSortOrderBottomSheet,
                                nodeActionHandler = bottomSheetActionHandler,
                                navHostController = navHostController,
                                bottomSheetNavigator = bottomSheetNavigator,
                                listToStringWithDelimitersMapper = listToStringWithDelimitersMapper,
                                handleClick = {
                                    coroutineScope.launch {
                                        when (it) {
                                            is TypedFileNode -> openFileClicked(it)
                                            is TypedFolderNode -> viewModel.openFolder(
                                                folderHandle = it.id.longValue,
                                                name = it.name
                                            )

                                            else -> Timber.e("Unsupported click")
                                        }
                                    }
                                },
                                fileTypeIconMapper = fileTypeIconMapper,
                                onBackPressed = {
                                    if (viewModel.state.value.selectedNodes.isNotEmpty()) {
                                        viewModel.clearSelection()
                                    } else if (viewModel.state.value.navigationLevel.isNotEmpty()) {
                                        viewModel.navigateBack()
                                    } else {
                                        onBackPressedDispatcher.onBackPressed()
                                    }
                                }
                            )
                        }
                    }

                    StartTransferComponent(
                        event = nodeActionState.downloadEvent,
                        onConsumeEvent = nodeActionsViewModel::markDownloadEventConsumed,
                        snackBarHostState = snackbarHostState,
                    )
                }

                EventEffect(
                    event = nodeActionState.nodeNameCollisionsResult,
                    onConsumed = nodeActionsViewModel::markHandleNodeNameCollisionResult,
                    action = {
                        handleNodesNameCollisionResult(it)
                    }
                )
                EventEffect(
                    event = nodeActionState.showForeignNodeDialog,
                    onConsumed = nodeActionsViewModel::markForeignNodeDialogShown,
                    action = { navHostController.navigate(searchForeignNodeDialog) }
                )
                EventEffect(
                    event = nodeActionState.showQuotaDialog,
                    onConsumed = nodeActionsViewModel::markQuotaDialogShown,
                    action = {
                        navHostController.navigate(searchOverQuotaDialog.plus("/${it}"))
                    }
                )
                EventEffect(
                    event = nodeActionState.contactsData,
                    onConsumed = nodeActionsViewModel::markShareFolderAccessDialogShown,
                    action = { (contactData, isFromBackups, nodeHandles) ->
                        val contactList =
                            contactData.joinToString(separator = contactArraySeparator)
                        navHostController.navigate(
                            shareFolderAccessDialog.plus("/${contactList}")
                                .plus("/${isFromBackups}")
                                .plus("/${nodeHandles}")
                        )
                    },
                )
                EventEffect(
                    event = nodeActionState.selectAll,
                    onConsumed = nodeActionsViewModel::selectAllConsumed,
                    action = viewModel::selectAll
                )
                EventEffect(
                    event = nodeActionState.clearAll,
                    onConsumed = nodeActionsViewModel::clearAllConsumed,
                    action = viewModel::clearSelection
                )
                EventEffect(
                    event = nodeActionState.infoToShowEvent,
                    onConsumed = nodeActionsViewModel::onInfoToShowEventConsumed,
                ) { info ->
                    info?.let {
                        info.getInfo(this@SearchActivity).let { text ->
                            scaffoldState.snackbarHostState.showAutoDurationSnackbar(text)
                        }
                    } ?: findActivity()?.finish()
                }
            }
        }

        collectFlow(sortByHeaderViewModel.orderChangeState) {
            viewModel.onSortOrderChanged()
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
     * @param currentFileNode [FileNode]
     */
    private suspend fun openFileClicked(currentFileNode: TypedFileNode) {
        runCatching {
            nodeActionsViewModel.handleFileNodeClicked(currentFileNode)
        }.onSuccess { content ->
            val type = nodeSourceTypeToViewTypeMapper(viewModel.state.value.nodeSourceType)
            when (content) {
                is FileNodeContent.Pdf -> openPdfActivity(
                    content = content.uri,
                    currentFileNode = currentFileNode,
                    viewType = type
                )

                is FileNodeContent.ImageForNode -> {
                    openImageViewerActivity(
                        currentFileNode = currentFileNode
                    )
                }

                is FileNodeContent.TextContent -> openTextEditorActivity(
                    currentFileNode = currentFileNode,
                    viewType = type
                )

                is FileNodeContent.AudioOrVideo -> {
                    openVideoOrAudioFile(
                        content = content.uri,
                        fileNode = currentFileNode,
                        viewType = type
                    )
                }

                is FileNodeContent.UrlContent -> {
                    openUrlFile(
                        content = content,
                    )
                }

                is FileNodeContent.Other -> {
                    content.localFile?.let {
                        if (currentFileNode.type is ZipFileTypeInfo) {
                            openZipFile(it, currentFileNode)
                        } else {
                            handleOtherFiles(it, currentFileNode)
                        }
                    } ?: run {
                        nodeActionsViewModel.downloadNodeForPreview(currentFileNode)
                    }
                }

                else -> {

                }
            }

        }.onFailure {
            Timber.e(it)
        }
    }

    private suspend fun handleOtherFiles(
        localFile: File,
        currentFileNode: TypedFileNode,
    ) {
        Intent(Intent.ACTION_VIEW).apply {
            nodeActionsViewModel.applyNodeContentUri(
                intent = this,
                content = NodeContentUri.LocalContentUri(localFile),
                mimeType = currentFileNode.type.mimeType,
                isSupported = false
            )
            runCatching {
                startActivity(this)
            }.onFailure { error ->
                Timber.e(error)
                openShareIntent()
            }
        }
    }

    private suspend fun Intent.openShareIntent() {
        if (resolveActivity(packageManager) == null) {
            action = Intent.ACTION_SEND
        }
        runCatching {
            startActivity(this)
        }.onFailure { error ->
            Timber.e(error)
            snackbarHostState.showAutoDurationSnackbar(getString(R.string.intent_not_available))
        }
    }

    private fun openZipFile(
        localFile: File,
        fileNode: TypedFileNode,
    ) {
        Timber.d("The file is zip, open in-app.")
        megaNavigator.openZipBrowserActivity(
            context = this,
            zipFilePath = localFile.absolutePath,
            nodeHandle = fileNode.id.longValue
        ) {
            lifecycleScope.launch {
                snackbarHostState.showAutoDurationSnackbar(getString(R.string.message_zip_format_error))
            }
        }
    }

    private fun openTextEditorActivity(currentFileNode: TypedFileNode, viewType: Int?) {
        val textFileIntent = Intent(this, TextEditorActivity::class.java)
        textFileIntent.putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, currentFileNode.id.longValue)
            .putExtra(TextEditorViewModel.MODE, TextEditorViewModel.VIEW_MODE)
            .putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, viewType)
        startActivity(textFileIntent)
    }

    private fun openPdfActivity(
        content: NodeContentUri,
        currentFileNode: TypedFileNode,
        viewType: Int?,
    ) {
        val pdfIntent = Intent(this, PdfViewerActivity::class.java)
        val mimeType = currentFileNode.type.mimeType
        pdfIntent.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, currentFileNode.id.longValue)
            putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
            putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, viewType)
            putExtra(Constants.INTENT_EXTRA_KEY_APP, true)
        }
        nodeActionsViewModel.applyNodeContentUri(
            intent = pdfIntent,
            content = content,
            mimeType = mimeType,
        )
        startActivity(pdfIntent)
    }

    private fun openImageViewerActivity(currentFileNode: TypedFileNode) {
        val nodeSourceType = viewModel.state.value.nodeSourceType
        val currentFileNodeParentId = currentFileNode.parentId.longValue

        val (imageSource, menuOptionsSource, paramKey) = when (nodeSourceType) {
            NodeSourceType.CLOUD_DRIVE, NodeSourceType.HOME ->
                Triple(
                    ImagePreviewFetcherSource.CLOUD_DRIVE,
                    ImagePreviewMenuSource.CLOUD_DRIVE,
                    CloudDriveImageNodeFetcher.PARENT_ID
                )

            NodeSourceType.RUBBISH_BIN ->
                Triple(
                    ImagePreviewFetcherSource.RUBBISH_BIN,
                    ImagePreviewMenuSource.RUBBISH_BIN,
                    RubbishBinImageNodeFetcher.PARENT_ID
                )

            NodeSourceType.INCOMING_SHARES, NodeSourceType.OUTGOING_SHARES ->
                Triple(
                    ImagePreviewFetcherSource.SHARED_ITEMS,
                    ImagePreviewMenuSource.SHARED_ITEMS,
                    SharedItemsImageNodeFetcher.PARENT_ID
                )

            NodeSourceType.LINKS ->
                Triple(
                    ImagePreviewFetcherSource.SHARED_ITEMS,
                    ImagePreviewMenuSource.LINKS,
                    SharedItemsImageNodeFetcher.PARENT_ID
                )

            NodeSourceType.BACKUPS ->
                Triple(
                    ImagePreviewFetcherSource.BACKUPS,
                    ImagePreviewMenuSource.BACKUPS,
                    BackupsImageNodeFetcher.PARENT_ID
                )

            else -> {
                Timber.e("Unsupported node source type")
                return
            }
        }

        val intent = ImagePreviewActivity.createIntent(
            context = this,
            imageSource = imageSource,
            menuOptionsSource = menuOptionsSource,
            anchorImageNodeId = currentFileNode.id,
            params = mapOf(paramKey to currentFileNodeParentId),
        )
        startActivity(intent)
    }


    private fun openVideoOrAudioFile(
        fileNode: TypedFileNode,
        content: NodeContentUri,
        viewType: Int?,
    ) {
        lifecycleScope.launch {
            runCatching {
                megaNavigator.openMediaPlayerActivityByFileNode(
                    context = this@SearchActivity,
                    fileNode = fileNode,
                    contentUri = content,
                    viewType = viewType ?: INVALID_VALUE,
                    sortOrder = viewModel.state.value.sortOrder
                )
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun openUrlFile(
        content: FileNodeContent.UrlContent,
    ) {
        content.path?.let {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(it)
            }
            startActivity(intent)
        } ?: run {
            showMegaSnackbar(
                message = getString(R.string.general_text_error),
                actionLabel = null,
                duration = MegaSnackbarDuration.Short
            )
        }
    }

    private fun safeLaunchActivity(intent: Intent) {
        runCatching {
            startActivity(intent)
        }.onFailure {
            Timber.e(it)
            showMegaSnackbar(
                message = getString(R.string.intent_not_available),
                actionLabel = null,
                duration = MegaSnackbarDuration.Short
            )
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

    private fun handleNodesNameCollisionResult(result: NodeNameCollisionsResult) {
        if (result.conflictNodes.isNotEmpty()) {
            nameCollisionActivityLauncher
                .launch(result.conflictNodes.values.toCollection(ArrayList()))
        }
        if (result.noConflictNodes.isNotEmpty()) {
            when (result.type) {
                NodeNameCollisionType.MOVE -> nodeActionsViewModel.moveNodes(result.noConflictNodes)
                NodeNameCollisionType.COPY -> nodeActionsViewModel.copyNodes(result.noConflictNodes)
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
        lifecycleScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.TransfersSection)) {
                megaNavigator.openTransfers(this@SearchActivity, IN_PROGRESS_TAB_INDEX)
            } else {
                startActivity(
                    Intent(this@SearchActivity, ManagerActivity::class.java)
                        .setAction(Constants.ACTION_SHOW_TRANSFERS)
                        .putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.PENDING_TAB)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
            }
            finish()
        }
        if (transfersManagement.isOnTransferOverQuota()) {
            transfersManagement.setHasNotToBeShowDueToTransferOverQuota(true)
        }
    }
}

/**
 * search screen mini audio player test tag
 */
const val SEARCH_SCREEN_MINI_AUDIO_PLAYER_TEST_TAG = "search_screen:mini_audio_player"

/**
 * search screen transfers widget test tag
 */
const val SEARCH_SCREEN_TRANSFERS_WIDGET_TEST_TAG = "search_screen:transfers_widget_view"