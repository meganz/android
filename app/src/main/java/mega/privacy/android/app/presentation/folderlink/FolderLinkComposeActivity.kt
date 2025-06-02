package mega.privacy.android.app.presentation.folderlink

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.text.HtmlCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.DecryptAlertDialog
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.ManagerActivity.Companion.TRANSFERS_TAB
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.advertisements.GoogleAdsManager
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.folderlink.view.FolderLinkView
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.FolderLinkImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryActivity
import mega.privacy.android.app.presentation.settings.model.StorageTargetPreference
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.presentation.transfers.view.ACTIVE_TAB_INDEX
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.utils.AlertDialogUtil
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.ACTION_SHOW_TRANSFERS
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaProgressDialogUtil
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * FolderLinkActivity with compose view
 */
@AndroidEntryPoint
class FolderLinkComposeActivity : PasscodeActivity(),
    DecryptAlertDialog.DecryptDialogListener {

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * [GetFeatureFlagValueUseCase]
     */
    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    /**
     * Mapper to get the icon of a file type
     */
    @Inject
    lateinit var fileTypeIconMapper: FileTypeIconMapper

    /**
     * MegaNavigator
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    /**
     * [GoogleAdsManager]
     */
    @Inject
    lateinit var googleAdsManager: GoogleAdsManager

    private val viewModel: FolderLinkViewModel by viewModels()

    private var mKey: String? = null
    private var statusDialog: AlertDialog? = null

    private val nameCollisionActivityLauncher = registerForActivityResult(
        NameCollisionActivityContract()
    ) { result ->
        result?.let {
            showSnackbar(SNACKBAR_TYPE, it, INVALID_HANDLE)
        }
    }

    @SuppressLint("CheckResult")
    private val selectImportFolderResult =
        ActivityResultCallback<ActivityResult> { activityResult ->
            val resultCode = activityResult.resultCode
            val intent = activityResult.data

            if (resultCode != RESULT_OK || intent == null) {
                viewModel.resetImportNode()
                return@ActivityResultCallback
            }

            if (!viewModel.isConnected) {
                try {
                    statusDialog?.dismiss()
                } catch (exception: Exception) {
                    Timber.e(exception)
                }

                viewModel.resetImportNode()
                viewModel.showSnackbar(R.string.error_server_connection_problem)
                return@ActivityResultCallback
            }

            val toHandle = intent.getLongExtra("IMPORT_TO", 0)
            statusDialog =
                MegaProgressDialogUtil.createProgressDialog(
                    this,
                    getString(R.string.general_importing)
                )
            statusDialog?.show()

            viewModel.importNodes(toHandle)
        }

    private val selectImportFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        selectImportFolderResult
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeAndConsumeInsets()
        super.onCreate(savedInstanceState)

        viewModel.state.map { it.shouldShowAdsForLink }
            .distinctUntilChanged()
            .combine(googleAdsManager.isAdsFeatureEnabled) { shouldShowAdsForLink, isAdsFeatureEnabled ->
                if (shouldShowAdsForLink && isAdsFeatureEnabled) {
                    googleAdsManager.checkLatestConsentInformation(
                        activity = this,
                        onConsentInformationUpdated = { googleAdsManager.fetchAdRequest() }
                    )
                }
            }.launchIn(lifecycleScope)

        setContent {
            StartFolderLinkView()
        }

        intent?.let { viewModel.handleIntent(it) }
        viewModel.checkLoginRequired()
        checkForInAppAdvertisement()
        collectFlow(viewModel.monitorMiscLoadedUseCase()) {
            checkForInAppAdvertisement()
        }
    }

    private fun checkForInAppAdvertisement() {
        lifecycleScope.launch {
            runCatching {
                googleAdsManager.checkForAdsAvailability()
            }.onFailure {
                Timber.e("Failed to check for ads availability: $it")
            }
        }
    }

    @Composable
    private fun StartFolderLinkView() {
        val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        val request by googleAdsManager.request.collectAsStateWithLifecycle()
        val transferState by transfersManagementViewModel.state.collectAsStateWithLifecycle()

        val scaffoldState = rememberScaffoldState()
        OriginalTheme(isDark = themeMode.isDarkMode()) {
            FolderLinkView(
                state = uiState,
                transferState = transferState,
                scaffoldState = scaffoldState,
                onBackPressed = viewModel::handleBackPress,
                onShareClicked = ::onShareClicked,
                onMoreOptionClick = viewModel::handleMoreOptionClick,
                onItemClicked = ::onItemClick,
                onLongClick = viewModel::onItemLongClick,
                onChangeViewTypeClick = viewModel::onChangeViewTypeClicked,
                onSortOrderClick = { },
                onSelectAllActionClicked = viewModel::onSelectAllClicked,
                onClearAllActionClicked = viewModel::clearAllSelection,
                onSaveToDeviceClicked = viewModel::handleSaveToDevice,
                onImportClicked = viewModel::handleImportClick,
                onOpenFile = ::onOpenFile,
                onResetOpenFile = viewModel::resetOpenFile,
                onSelectImportLocation = ::onSelectImportLocation,
                onResetSelectImportLocation = viewModel::resetSelectImportLocation,
                onResetSnackbarMessage = viewModel::resetSnackbarMessage,
                onResetMoreOptionNode = viewModel::resetMoreOptionNode,
                onResetOpenMoreOption = viewModel::resetOpenMoreOption,
                onStorageStatusDialogDismiss = viewModel::dismissStorageStatusDialog,
                onStorageDialogActionButtonClick = { viewModel.handleActionClick(this) },
                onStorageDialogAchievementButtonClick = ::navigateToAchievements,
                emptyViewString = getEmptyViewString(),
                onDisputeTakeDownClicked = this::launchUrl,
                onLinkClicked = this::launchUrl,
                onEnterMediaDiscoveryClick = ::onEnterMediaDiscoveryClick,
                onTransferWidgetClick = ::onTransfersWidgetClick,
                fileTypeIconMapper = fileTypeIconMapper,
                request = request
            )
            StartTransferComponent(
                event = uiState.downloadEvent,
                onConsumeEvent = viewModel::resetDownloadNode,
                snackBarHostState = scaffoldState.snackbarHostState,
                navigateToStorageSettings = {
                    megaNavigator.openSettings(
                        this,
                        StorageTargetPreference
                    )
                }
            )

            EventEffect(
                event = uiState.showLoginEvent,
                onConsumed = viewModel::onShowLoginEventConsumed
            ) {
                showLoginScreen()
            }

            EventEffect(
                event = uiState.finishActivityEvent,
                onConsumed = viewModel::onFinishActivityEventConsumed
            ) {
                finish()
            }

            EventEffect(
                event = uiState.askForDecryptionKeyDialogEvent,
                onConsumed = viewModel::resetAskForDecryptionKeyDialog
            ) {
                showAskForDecryptionKeyDialog()
            }

            EventEffect(
                event = uiState.collisionsEvent,
                onConsumed = {
                    viewModel.resetLaunchCollisionActivity()
                    viewModel.clearAllSelection()
                }
            ) {
                AlertDialogUtil.dismissAlertDialogIfExists(statusDialog)
                nameCollisionActivityLauncher.launch(ArrayList(it))
            }

            EventEffect(
                event = uiState.copyResultEvent,
                onConsumed = viewModel::resetShowCopyResult
            ) { (resultText, throwable) ->
                showCopyResult(copyResultText = resultText, throwable = throwable)
            }

            BackHandler {
                viewModel.handleBackPress()
            }
        }
    }

    private fun onEnterMediaDiscoveryClick() {
        viewModel.clearAllSelection()
        val mediaHandle = viewModel.state.value.parentNode?.id?.longValue ?: -1
        MediaDiscoveryActivity.startMDActivity(
            context = this@FolderLinkComposeActivity,
            mediaHandle = mediaHandle,
            folderName = viewModel.state.value.title,
            isOpenByMDIcon = true
        )
    }

    /**
     * Handle item click
     *
     * @param nodeUIItem    Item that is clicked
     */
    private fun onItemClick(nodeUIItem: NodeUIItem<TypedNode>) {
        if (viewModel.isMultipleNodeSelected()) {
            viewModel.onItemLongClick(nodeUIItem)
        } else {
            if (nodeUIItem.node is FolderNode) {
                viewModel.openFolder(nodeUIItem)
            } else if (nodeUIItem.node is FileNode) {
                val fileNode = nodeUIItem.node
                val nameType = MimeTypeList.typeForName(fileNode.name)
                when {
                    nameType.isImage -> {
                        lifecycleScope.launch {
                            val parentNodeLongValue =
                                viewModel.state.value.parentNode?.id?.longValue ?: 0
                            val intent = ImagePreviewActivity.createIntent(
                                context = this@FolderLinkComposeActivity,
                                imageSource = ImagePreviewFetcherSource.FOLDER_LINK,
                                menuOptionsSource = ImagePreviewMenuSource.FOLDER_LINK,
                                anchorImageNodeId = fileNode.id,
                                isForeign = true,
                                params = mapOf(
                                    FolderLinkImageNodeFetcher.PARENT_ID to parentNodeLongValue,
                                ),
                            )
                            viewModel.updateImageIntent(intent)
                        }
                    }

                    nameType.isVideoMimeType || nameType.isAudio -> {
                        lifecycleScope.launch {
                            if (fileNode is TypedFileNode) {
                                runCatching {
                                    val contentUri =
                                        viewModel.getNodeContentUri(fileNode = fileNode)
                                    megaNavigator.openMediaPlayerActivityByFileNode(
                                        context = this@FolderLinkComposeActivity,
                                        contentUri = contentUri,
                                        fileNode = fileNode,
                                        isFolderLink = true,
                                        viewType = FOLDER_LINK_ADAPTER,
                                    )
                                }.onFailure {
                                    Toast.makeText(
                                        this@FolderLinkComposeActivity,
                                        getString(R.string.intent_not_available),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }

                    nameType.isPdf -> {
                        val intent =
                            Intent(this@FolderLinkComposeActivity, PdfViewerActivity::class.java)
                        viewModel.updatePdfIntent(intent, fileNode, nameType.type)
                    }

                    nameType.isOpenableTextFile(fileNode.size) -> {
                        val intent =
                            Intent(this@FolderLinkComposeActivity, TextEditorActivity::class.java)
                        viewModel.updateTextEditorIntent(intent, fileNode)
                    }

                    else -> {
                        Timber.w("Unknown File Type")
                        viewModel.openOtherTypeFile(this, fileNode)
                    }
                }
            }
        }
    }

    /**
     * Handle widget click
     */
    private fun onTransfersWidgetClick() {
        lifecycleScope.launch {
            val credentials = runCatching { getAccountCredentialsUseCase() }.getOrNull()
            if (megaApi.isLoggedIn == 0 || credentials == null) {
                Timber.w("Not logged in, no action.")
                return@launch
            }
            if (getFeatureFlagValueUseCase(AppFeatures.TransfersSection)) {
                megaNavigator.openTransfers(this@FolderLinkComposeActivity, ACTIVE_TAB_INDEX)
            } else {
                startActivity(
                    Intent(this@FolderLinkComposeActivity, ManagerActivity::class.java)
                        .setAction(ACTION_SHOW_TRANSFERS)
                        .putExtra(TRANSFERS_TAB, TransfersTab.PENDING_TAB)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            finish()
        }
    }

    private fun onSelectImportLocation() {
        val intent = Intent(this, FileExplorerActivity::class.java)
        intent.action = FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER
        selectImportFolderLauncher.launch(intent)
    }

    /**
     * Open the selected file in a separate activity
     *
     * @param intent    Intent of the activity to open
     */
    private fun onOpenFile(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.intent_not_available),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun onShareClicked() {
        MegaNodeUtil.shareLink(this, viewModel.state.value.url, viewModel.state.value.title)
    }

    private fun showLoginScreen() {
        Timber.d("Refresh session - sdk or karere")
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
        intent.data = Uri.parse(viewModel.state.value.url)
        intent.action = Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun showAskForDecryptionKeyDialog() {
        Timber.d("askForDecryptionKeyDialog")
        val builder = DecryptAlertDialog.Builder()
        val decryptAlertDialog = builder
            .setTitle(getString(R.string.alert_decryption_key))
            .setPosText(R.string.general_decryp)
            .setNegText(sharedR.string.general_dialog_cancel_button)
            .setMessage(getString(R.string.message_decryption_key))
            .setErrorMessage(R.string.invalid_decryption_key)
            .setKey(mKey)
            .build()

        decryptAlertDialog.show(supportFragmentManager, TAG_DECRYPT)
    }

    /**
     * Shows the copy Result.
     *
     * @param copyResultText Copy result text.
     * @param throwable
     */

    private fun showCopyResult(copyResultText: String?, throwable: Throwable?) {
        AlertDialogUtil.dismissAlertDialogIfExists(statusDialog)
        viewModel.clearAllSelection()
        if (copyResultText != null) {
            viewModel.showSnackbar(copyResultText)
        } else throwable?.let { handleMoveCopyException(it) }
            ?: viewModel.showSnackbar(R.string.context_correctly_copied)
    }

    private fun handleMoveCopyException(throwable: Throwable) {
        when (throwable) {
            is QuotaExceededMegaException, is NotEnoughQuotaMegaException -> {
                viewModel.handleQuotaException(throwable)
            }

            else -> {
                manageCopyMoveException(throwable)
            }
        }
    }

    override fun onDialogPositiveClick(key: String?) {
        mKey = key
        viewModel.apply {
            resetAskForDecryptionKeyDialog()
            decrypt(mKey, state.value.url)
        }
    }

    override fun onDialogNegativeClick() {
        finish()
    }

    private fun getEmptyViewString(): String {
        var textToShow = getString(R.string.file_browser_empty_folder_new)
        try {
            textToShow = textToShow.replace(
                "[A]",
                "<font color=\'${
                    ColorUtils.getColorHexString(this, R.color.grey_900_grey_100)
                }\'>"
            )
            textToShow = textToShow.replace("[/A]", "</font>")
            textToShow = textToShow.replace(
                "[B]",
                "<font color=\'${
                    ColorUtils.getColorHexString(this, R.color.grey_300_grey_600)
                }\'>"
            )
            textToShow = textToShow.replace("[/B]", "</font>")
        } catch (_: Exception) {
        }

        return HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    }

    private fun navigateToAchievements() {
        viewModel.dismissStorageStatusDialog()
        AlertDialogUtil.dismissAlertDialogIfExists(statusDialog)
        val accountIntent = Intent(this, MyAccountActivity::class.java)
            .setAction(IntentConstants.ACTION_OPEN_ACHIEVEMENTS)
        startActivity(accountIntent)
    }

    companion object {
        private const val TAG_DECRYPT = "decrypt"
    }
}