package mega.privacy.android.app.presentation.documentsection

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.modalbottomsheet.UploadBottomSheetDialogFragment.Companion.DOCUMENTS_UPLOAD
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntity
import mega.privacy.android.app.presentation.documentsection.view.DocumentSectionComposeView
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.presentation.mapper.GetOptionsForToolbarMapper
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.presentation.search.view.MiniAudioPlayerView
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.DOCUMENTS_BROWSE_ADAPTER
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.mobile.analytics.event.HideNodeMultiSelectMenuItemEvent
import nz.mega.sdk.MegaChatApiJava
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * The fragment for document section
 */
@AndroidEntryPoint
class DocumentSectionFragment : Fragment() {
    private val documentSectionViewModel by viewModels<DocumentSectionViewModel>()
    private val sortByHeaderViewModel: SortByHeaderViewModel by activityViewModels()

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    /**
     * Mapper to get options for Action Bar
     */
    @Inject
    lateinit var getOptionsForToolbarMapper: GetOptionsForToolbarMapper

    private var actionMode: ActionMode? = null

    private var tempNodeIds: List<NodeId> = listOf()


    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            val themeMode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by documentSectionViewModel.uiState.collectAsStateWithLifecycle()
            OriginalTheme(isDark = themeMode.isDarkMode()) {
                ConstraintLayout(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val (audioPlayer, documentComposeView) = createRefs()
                    MiniAudioPlayerView(
                        modifier = Modifier
                            .constrainAs(audioPlayer) {
                                bottom.linkTo(parent.bottom)
                            }
                            .fillMaxWidth(),
                        lifecycle = lifecycle,
                    )
                    DocumentSectionComposeView(
                        modifier = Modifier
                            .constrainAs(documentComposeView) {
                                top.linkTo(parent.top)
                                bottom.linkTo(audioPlayer.top)
                                height = Dimension.fillToConstraints
                            }
                            .fillMaxWidth(),
                        uiState = uiState,
                        onChangeViewTypeClick = documentSectionViewModel::onChangeViewTypeClicked,
                        onClick = { item, index ->
                            if (uiState.actionMode) {
                                documentSectionViewModel.onItemSelected(item, index)
                            } else {
                                openDoc(
                                    activity = requireActivity(),
                                    document = item
                                )
                            }
                        },
                        onSortOrderClick = ::showSortByPanel,
                        onMenuClick = { showOptionsMenuForItem(it.id) },
                        onLongClick = { item, index ->
                            documentSectionViewModel.onItemSelected(item, index)
                            activateActionMode()
                        },
                        onAddDocumentClick = {
                            (requireActivity() as ManagerActivity).showUploadPanel(DOCUMENTS_UPLOAD)
                        }
                    )
                }
            }
            updateActionModeTitle(count = uiState.selectedDocumentHandles.size)
        }
    }

    private fun openDoc(activity: Activity, document: DocumentUiEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            val nodeHandle = document.id.longValue
            val nodeFileType = document.fileTypeInfo.mimeType
            when {
                document.fileTypeInfo is PdfFileTypeInfo -> {
                    val intent = Intent(context, PdfViewerActivity::class.java).apply {
                        putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
                        putExtra(
                            INTENT_EXTRA_KEY_ADAPTER_TYPE,
                            DOCUMENTS_BROWSE_ADAPTER
                        )
                        putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, nodeHandle)
                    }

                    documentSectionViewModel.getLocalFilePath(nodeHandle)?.let { localPath ->
                        File(localPath).let { file ->
                            runCatching {
                                FileProvider.getUriForFile(
                                    activity,
                                    Constants.AUTHORITY_STRING_FILE_PROVIDER,
                                    file
                                )
                            }.onFailure {
                                Uri.fromFile(file)
                            }.map { mediaFileUri ->
                                intent.setDataAndType(mediaFileUri, nodeFileType)
                                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                        }
                        intent
                    } ?: documentSectionViewModel.updateIntent(
                        handle = nodeHandle,
                        fileType = nodeFileType,
                        intent = intent
                    ).let {
                        activity.startActivity(it)
                    }
                }

                document.fileTypeInfo is TextFileTypeInfo
                        && document.size <= TextFileTypeInfo.MAX_SIZE_OPENABLE_TEXT_FILE -> {
                    Intent(context, TextEditorActivity::class.java).apply {
                        putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, nodeHandle)
                        putExtra(
                            INTENT_EXTRA_KEY_ADAPTER_TYPE,
                            DOCUMENTS_BROWSE_ADAPTER
                        )
                    }.let {
                        activity.startActivity(it)
                    }
                }

                else -> {
                    documentSectionViewModel.getDocumentNodeByHandle(nodeHandle)?.let { node ->
                        MegaNodeUtil.onNodeTapped(
                            requireActivity(),
                            node,
                            { (requireActivity() as ManagerActivity).saveNodeByTap(it) },
                            requireActivity() as ManagerActivity,
                            requireActivity() as ManagerActivity
                        )
                    }
                }
            }
        }
    }

    private fun showSortByPanel() {
        (requireActivity() as? ManagerActivity)?.showNewSortByPanel(Constants.ORDER_CLOUD)
    }

    private fun showOptionsMenuForItem(id: NodeId) {
        doIfOnline {
            callManager { manager ->
                manager.showNodeOptionsPanel(
                    nodeId = id,
                    mode = NodeOptionsBottomSheetDialogFragment.CLOUD_DRIVE_MODE
                )
            }
        }
    }

    /**
     * Perform a specific operation when online
     *
     * @param operation lambda that specifies the operation to be executed
     */
    private fun doIfOnline(operation: () -> Unit) {
        if (documentSectionViewModel.isConnected) {
            operation()
        } else {
            callManager {
                it.hideKeyboardSearch()  // Make the snack bar visible to the user
                it.showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
            }
        }
    }

    private fun activateActionMode() {
        if (actionMode == null) {
            actionMode =
                (requireActivity() as? AppCompatActivity)?.startSupportActionMode(
                    DocumentSectionActionModeCallback(
                        fragment = this,
                        managerActivity = requireActivity() as ManagerActivity,
                        childFragmentManager = childFragmentManager,
                        documentSectionViewModel = documentSectionViewModel,
                        getOptionsForToolbarMapper = getOptionsForToolbarMapper
                    ) {
                        disableSelectMode()
                    }
                )
            documentSectionViewModel.setActionMode(true)
        }
    }

    private fun disableSelectMode() {
        actionMode = null
        documentSectionViewModel.clearAllSelectedDocuments()
        documentSectionViewModel.setActionMode(false)
    }

    private fun updateActionModeTitle(count: Int) {
        if (count == 0) actionMode?.finish()
        actionMode?.title = count.toString()

        runCatching {
            actionMode?.invalidate()
        }.onFailure {
            Timber.e(it, "Invalidate error")
        }
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.collectFlow(sortByHeaderViewModel.orderChangeState) {
            documentSectionViewModel.refreshWhenOrderChanged()
        }

        viewLifecycleOwner.collectFlow(
            documentSectionViewModel.uiState.map { it.allDocuments }.distinctUntilChanged()
        ) { list ->
            if (list.isNotEmpty()) {
                callManager {
                    it.invalidateOptionsMenu()
                }
            }
        }
    }

    suspend fun handleHideNodeClick() {
        Analytics.tracker.trackEvent(HideNodeMultiSelectMenuItemEvent)
        var isPaid: Boolean
        var isHiddenNodesOnboarded: Boolean
        var isBusinessAccountExpired: Boolean
        with(documentSectionViewModel.uiState.value) {
            isPaid = this.accountType?.isPaid ?: false
            isHiddenNodesOnboarded = this.isHiddenNodesOnboarded
            isBusinessAccountExpired = this.isBusinessAccountExpired
        }

        if (!isPaid || isBusinessAccountExpired) {
            val intent = HiddenNodesOnboardingActivity.createScreen(
                context = requireContext(),
                isOnboarding = false,
            )
            hiddenNodesOnboardingLauncher.launch(intent)
            activity?.overridePendingTransition(0, 0)
        } else if (isHiddenNodesOnboarded) {
            val nodes = documentSectionViewModel.getSelectedNodes()
            documentSectionViewModel.hideOrUnhideNodes(
                nodeIds = nodes.map { it.id },
                hide = true,
            )
            val message =
                resources.getQuantityString(
                    R.plurals.hidden_nodes_result_message,
                    nodes.size,
                    nodes.size,
                )
            Util.showSnackbar(requireActivity(), message)
        } else {
            tempNodeIds = documentSectionViewModel.getSelectedNodes().map { it.id }
            showHiddenNodesOnboarding()
        }
    }

    private fun showHiddenNodesOnboarding() {
        documentSectionViewModel.setHiddenNodesOnboarded()

        val intent = HiddenNodesOnboardingActivity.createScreen(
            context = requireContext(),
            isOnboarding = true,
        )
        hiddenNodesOnboardingLauncher.launch(intent)
        activity?.overridePendingTransition(0, 0)
    }

    private val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleHiddenNodesOnboardingResult,
        )

    private fun handleHiddenNodesOnboardingResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) return

        documentSectionViewModel.hideOrUnhideNodes(
            nodeIds = tempNodeIds,
            hide = true,
        )

        val message =
            resources.getQuantityString(
                R.plurals.hidden_nodes_result_message,
                tempNodeIds.size,
                tempNodeIds.size,
            )
        Util.showSnackbar(requireActivity(), message)
    }
}