package mega.privacy.android.app.presentation.audiosection

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.navigation.rememberBottomSheetNavigator
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.SortByBottomSheetDialogFragment
import mega.privacy.android.app.presentation.audiosection.view.AudioSectionComposeScreen
import mega.privacy.android.app.presentation.audiosection.view.AudioSectionTopBar
import mega.privacy.android.app.presentation.meeting.chat.extension.getInfo
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.NodeActionsViewModel
import mega.privacy.android.app.presentation.node.view.toolbar.NodeToolbarViewModel
import mega.privacy.android.app.presentation.qrcode.findActivity
import mega.privacy.android.app.presentation.search.SearchActivity
import mega.privacy.android.app.presentation.search.navigation.searchForeignNodeDialog
import mega.privacy.android.app.presentation.search.navigation.searchOverQuotaDialog
import mega.privacy.android.app.presentation.search.view.MiniAudioPlayerView
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.presentation.transfers.widget.TransfersWidget
import mega.privacy.android.app.utils.Constants.ORDER_CLOUD
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.AUDIO_BROWSE_ADAPTER
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AudioSectionActivity : PasscodeActivity(), ActionNodeCallback {
    private var bottomSheetDialogFragment: BottomSheetDialogFragment? = null

    private val audioSectionViewModel: AudioSectionViewModel by viewModels()
    private val sortByHeaderViewModel: SortByHeaderViewModel by viewModels()
    private val nodeActionsViewModel: NodeActionsViewModel by viewModels()
    private val toolbarViewModel: NodeToolbarViewModel by viewModels()

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    /**
     * Mega Navigator
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val snackbarHostState = SnackbarHostState()

    /**
     * File type icon mapper
     */
    @Inject
    lateinit var fileTypeIconMapper: FileTypeIconMapper

    /**
     * Mapper to convert list to json for sending data in navigation
     */
    @Inject
    lateinit var listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper

    private val nameCollisionActivityLauncher = registerForActivityResult(
        NameCollisionActivityContract()
    ) { result ->
        if (result != null) {
            lifecycleScope.launch {
                snackbarHostState.showAutoDurationSnackbar(result)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val bottomSheetActionHandler = NodeActionHandler(this, nodeActionsViewModel)

        setContent {
            val mode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState)
            val nodeActionState by nodeActionsViewModel.state.collectAsStateWithLifecycle()
            val bottomSheetNavigator = rememberBottomSheetNavigator()
            val navHostController = rememberNavController(bottomSheetNavigator)
            val uiState by audioSectionViewModel.state.collectAsStateWithLifecycle()
            val toolbarState by toolbarViewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(key1 = uiState.selectedNodes) {
                toolbarViewModel.updateToolbarState(
                    selectedNodes = audioSectionViewModel.getSelectedNodes().toSet(),
                    resultCount = uiState.allAudios.size,
                    nodeSourceType = NodeSourceType.CLOUD_DRIVE
                )
            }

            BackHandler(uiState.isInSelection) {
                audioSectionViewModel.clearAllSelectedAudios()
            }

            OriginalTheme(isDark = mode.isDarkMode()) {
                MegaScaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .imePadding()
                        .semantics { testTagsAsResourceId = true },
                    scaffoldState = scaffoldState,
                    topBar = {
                        AudioSectionTopBar(
                            title = stringResource(R.string.upload_to_audio),
                            isActionMode = uiState.isInSelection,
                            selectedSize = uiState.selectedNodes.size,
                            onSearchClicked = { navigateToSearchActivity() },
                            onBackPressed = {
                                when {
                                    uiState.isInSelection -> audioSectionViewModel.clearAllSelectedAudios()
                                    else -> onBackPressedDispatcher.onBackPressed()
                                }
                            },
                            menuItems = toolbarState.toolbarMenuItems,
                            handler = bottomSheetActionHandler,
                            navHostController = navHostController,
                            clearSelection = audioSectionViewModel::clearAllSelectedAudios,
                            selectAllAction = audioSectionViewModel::selectAllNodes,
                        )
                    },
                    floatingActionButton = {
                        TransfersWidget(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .testTag(AUDIO_SECTION_SCREEN_TRANSFERS_WIDGET_TEST_TAG)
                        )
                    }
                ) { padding ->
                    ConstraintLayout(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        val (audioPlayer, audioSectionComposeView) = createRefs()
                        MiniAudioPlayerView(
                            modifier = Modifier
                                .constrainAs(audioPlayer) {
                                    bottom.linkTo(parent.bottom)
                                }
                                .fillMaxWidth(),
                            lifecycle = lifecycle,
                        )
                        AudioSectionComposeScreen(
                            viewModel = audioSectionViewModel,
                            modifier = Modifier
                                .constrainAs(audioSectionComposeView) {
                                    top.linkTo(parent.top)
                                    bottom.linkTo(audioPlayer.top)
                                    height = Dimension.fillToConstraints
                                }
                                .fillMaxWidth(),
                            onSortOrderClick = { showNewSortByPanel() },
                            navHostController = navHostController,
                            nodeActionHandler = bottomSheetActionHandler,
                            bottomSheetNavigator = bottomSheetNavigator,
                            fileTypeIconMapper = fileTypeIconMapper,
                            listToStringWithDelimitersMapper = listToStringWithDelimitersMapper,
                        )

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
                        event = nodeActionState.selectAll,
                        onConsumed = nodeActionsViewModel::selectAllConsumed,
                        action = audioSectionViewModel::selectAllNodes
                    )
                    EventEffect(
                        event = nodeActionState.clearAll,
                        onConsumed = nodeActionsViewModel::clearAllConsumed,
                        action = audioSectionViewModel::clearAllSelectedAudios
                    )
                    EventEffect(
                        event = nodeActionState.infoToShowEvent,
                        onConsumed = nodeActionsViewModel::onInfoToShowEventConsumed,
                    ) { info ->
                        info?.let {
                            info.getInfo(this@AudioSectionActivity).let { text ->
                                scaffoldState.snackbarHostState.showAutoDurationSnackbar(text)
                            }
                        } ?: findActivity()?.finish()
                    }
                }
            }
        }
        setupCollectFlow()
    }

    private fun showNewSortByPanel() {
        if (bottomSheetDialogFragment.isBottomSheetDialogShown()) {
            return
        }
        bottomSheetDialogFragment = SortByBottomSheetDialogFragment.newInstance(ORDER_CLOUD)
        bottomSheetDialogFragment?.show(
            supportFragmentManager,
            bottomSheetDialogFragment?.tag
        )
    }

    private fun navigateToSearchActivity() {
        val searchActivityIntent = SearchActivity.getIntent(
            context = this@AudioSectionActivity,
            nodeSourceType = NodeSourceType.AUDIO,
            parentHandle = INVALID_HANDLE
        )

        startActivity(searchActivityIntent)
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

    private fun setupCollectFlow() {
        collectFlow(sortByHeaderViewModel.orderChangeState) {
            audioSectionViewModel.refreshWhenOrderChanged()
        }

        collectFlow(
            audioSectionViewModel.state.map { it.isPendingRefresh }.distinctUntilChanged()
        ) { isPendingRefresh ->
            if (isPendingRefresh) {
                with(audioSectionViewModel) {
                    refreshNodes()
                    markHandledPendingRefresh()
                }
            }
        }

        collectFlow(
            audioSectionViewModel.state.map { it.allAudios }.distinctUntilChanged()
        ) { list ->
            if (list.isNotEmpty()) {
                invalidateOptionsMenu()
            }
        }

        collectFlow(
            audioSectionViewModel.state.map { it.clickedItem }.distinctUntilChanged()
        ) { fileNode ->
            Timber.d("AudioSectionActivityTesting clickedItem is changed fileNode is $fileNode")
            fileNode?.let { node ->
                val uiState = audioSectionViewModel.state.value
                lifecycleScope.launch {
                    runCatching {
                        val nodeContentUri =
                            audioSectionViewModel.getNodeContentUri(node) ?: return@launch
                        megaNavigator.openMediaPlayerActivityByFileNode(
                            context = this@AudioSectionActivity,
                            contentUri = nodeContentUri,
                            fileNode = node,
                            sortOrder = sortByHeaderViewModel.cloudSortOrder.value,
                            viewType = AUDIO_BROWSE_ADAPTER,
                            isFolderLink = false,
                            searchedItems = uiState.allAudios.map { it.id.longValue }
                        )
                        audioSectionViewModel.updateClickedItem(null)
                    }.onFailure { error ->
                        Timber.e(error)
                    }
                }
            }
        }
    }

    companion object {
        /**
         * audio section screen transfers widget test tag
         */
        const val AUDIO_SECTION_SCREEN_TRANSFERS_WIDGET_TEST_TAG =
            "audio_section_screen:transfers_widget_view"
    }
}
