package mega.privacy.android.app.presentation.folderlink

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.databinding.ActivityFolderLinkComposeBinding
import mega.privacy.android.app.extensions.isPortrait
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.DecryptAlertDialog
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.mediaplayer.AudioPlayerActivity
import mega.privacy.android.app.mediaplayer.VideoPlayerActivity
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.advertisements.AdsViewModel
import mega.privacy.android.app.presentation.advertisements.model.AdsSlotIDs
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.folderlink.view.FolderLinkView
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryActivity
import mega.privacy.android.app.presentation.transfers.TransfersManagementActivity
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.usecase.exception.NotEnoughQuotaMegaException
import mega.privacy.android.app.usecase.exception.QuotaExceededMegaException
import mega.privacy.android.app.utils.AlertDialogUtil
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaProgressDialogUtil
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import nz.mega.sdk.MegaNode
import timber.log.Timber

/**
 * FolderLinkActivity with compose view
 */
@AndroidEntryPoint
class FolderLinkComposeActivity : TransfersManagementActivity(),
    DecryptAlertDialog.DecryptDialogListener {

    private lateinit var binding: ActivityFolderLinkComposeBinding

    private val viewModel: FolderLinkViewModel by viewModels()
    private val adsViewModel: AdsViewModel by viewModels()

    private var mKey: String? = null
    private var statusDialog: AlertDialog? = null
    private val nodeSaver = NodeSaver(
        this, this, this,
        showSaveToDeviceConfirmDialog(this)
    )

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            nodeSaver.handleRequestPermissionsResult(Constants.REQUEST_WRITE_STORAGE)
        }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            viewModel.handleBackPress()
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

    override val isOnFileManagementManagerSection: Boolean
        get() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFolderLinkComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        binding.folderLinkView.apply {
            setContent {
                StartFolderLinkView()
            }
        }

        setTransfersWidgetLayout(binding.transfersWidgetLayout)
        intent?.let { viewModel.handleIntent(it) }
        setupObservers()
        viewModel.checkLoginRequired()
        if (isPortrait()) {
            adsViewModel.fetchNewAd(AdsSlotIDs.SHARED_LINK_SLOT_ID)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adsViewModel.onScreenOrientationChanged(isPortrait())
    }

    @Composable
    private fun StartFolderLinkView() {
        val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        val adsUiState by adsViewModel.uiState.collectAsStateWithLifecycle()

        AndroidTheme(isDark = themeMode.isDarkMode()) {
            FolderLinkView(
                state = uiState,
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
                onDownloadNode = ::downloadNodes,
                onResetDownloadNode = viewModel::resetDownloadNode,
                onSelectImportLocation = ::onSelectImportLocation,
                onResetSelectImportLocation = viewModel::resetSelectImportLocation,
                onResetSnackbarMessage = viewModel::resetSnackbarMessage,
                onResetMoreOptionNode = viewModel::resetMoreOptionNode,
                onResetOpenMoreOption = viewModel::resetOpenMoreOption,
                onStorageStatusDialogDismiss = viewModel::dismissStorageStatusDialog,
                onStorageDialogActionButtonClick = { viewModel.handleActionClick(this) },
                onStorageDialogAchievementButtonClick = ::navigateToAchievements,
                emptyViewString = getEmptyViewString(),
                onDisputeTakeDownClicked = ::navigateToLink,
                onLinkClicked = ::navigateToLink,
                onEnterMediaDiscoveryClick = ::onEnterMediaDiscoveryClick,
                adsUiState = adsUiState,
                onAdClicked = { uri ->
                    uri?.let {
                        val intent = Intent(Intent.ACTION_VIEW, it)
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                        } else {
                            Timber.d("No Application found to can handle Ads intent")
                        }
                    }
                    adsViewModel.fetchNewAd(AdsSlotIDs.SHARED_LINK_SLOT_ID)
                },
                onAdDismissed = adsViewModel::onAdDismissed
            )
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
                        val intent = ImageViewerActivity.getIntentForChildren(
                            this@FolderLinkComposeActivity,
                            viewModel.state.value.nodesList.map { it.id.longValue }.toLongArray(),
                            fileNode.id.longValue,
                            fromFolderLink = true
                        )
                        viewModel.updateImageIntent(intent)
                    }

                    nameType.isVideoMimeType || nameType.isAudio -> {
                        val intent =
                            if (nameType.isVideoNotSupported || nameType.isAudioNotSupported) {
                                Intent(Intent.ACTION_VIEW)
                            } else {
                                if (nameType.isAudio) {
                                    Intent(
                                        this@FolderLinkComposeActivity,
                                        AudioPlayerActivity::class.java
                                    )
                                } else {
                                    Intent(
                                        this@FolderLinkComposeActivity,
                                        VideoPlayerActivity::class.java
                                    )
                                }
                            }
                        viewModel.updateAudioVideoIntent(intent, fileNode, nameType)
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
                        viewModel.updateNodesToDownload(listOf(fileNode.id.longValue))
                    }
                }
            }
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
        MegaNodeUtil.shareLink(this, viewModel.state.value.url)
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect {
                    when {
                        it.finishActivity -> {
                            finish()
                        }

                        it.isInitialState -> {
                            it.shouldLogin?.let { showLogin ->
                                if (showLogin) {
                                    showLoginScreen()
                                } else {
                                    it.url?.let { url -> viewModel.folderLogin(url) }
                                }
                            }
                            return@collect
                        }

                        it.isLoginComplete && !it.isNodesFetched -> {
                            viewModel.fetchNodes(it.folderSubHandle)
                            // Get cookies settings after login.
                            MegaApplication.getInstance().checkEnabledCookies()
                        }

                        it.collisions != null -> {
                            AlertDialogUtil.dismissAlertDialogIfExists(statusDialog)
                            nameCollisionActivityContract?.launch(it.collisions)
                            viewModel.resetLaunchCollisionActivity()
                            viewModel.clearAllSelection()
                        }

                        it.copyResultText != null || it.copyThrowable != null -> {
                            showCopyResult(it.copyResultText, it.copyThrowable)
                            viewModel.resetShowCopyResult()
                        }

                        it.askForDecryptionKeyDialog -> {
                            askForDecryptionKeyDialog()
                        }

                        it.errorDialogTitle != -1 && it.errorDialogContent != -1 -> {
                            Timber.w("Show error dialog")
                            showErrorDialog(it.errorDialogTitle, it.errorDialogContent)
                        }

                        else -> {}
                    }
                }
            }
        }
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

    private fun askForDecryptionKeyDialog() {
        Timber.d("askForDecryptionKeyDialog")
        val builder = DecryptAlertDialog.Builder()
        val decryptAlertDialog = builder
            .setTitle(getString(R.string.alert_decryption_key))
            .setPosText(R.string.general_decryp).setNegText(R.string.general_cancel)
            .setMessage(getString(R.string.message_decryption_key))
            .setErrorMessage(R.string.invalid_decryption_key)
            .setKey(mKey)
            .build()

        decryptAlertDialog.show(supportFragmentManager, TAG_DECRYPT)
        viewModel.resetAskForDecryptionKeyDialog()
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

    private fun showErrorDialog(@StringRes title: Int, @StringRes message: Int) {
        val builder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
        builder.apply {
            setTitle(getString(title))
            setMessage(getString(message))
            setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                val closedChat = MegaApplication.isClosedChat
                if (closedChat) {
                    val backIntent = Intent(
                        this@FolderLinkComposeActivity,
                        ManagerActivity::class.java
                    )
                    startActivity(backIntent)
                }
                finish()
            }
        }
        builder.create().show()
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

    /**
     * Download nodes
     *
     * @param nodes List of nodes to download
     */
    fun downloadNodes(nodes: List<MegaNode>) {
        val hasStoragePermission =
            PermissionUtils.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (!hasStoragePermission) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        PermissionUtils.checkNotificationsPermission(this)
        nodeSaver.saveNodes(
            nodes,
            highPriority = false,
            isFolderLink = true,
            fromMediaViewer = false,
            needSerialize = false
        )
    }

    private fun navigateToAchievements() {
        viewModel.dismissStorageStatusDialog()
        AlertDialogUtil.dismissAlertDialogIfExists(statusDialog)
        val accountIntent = Intent(this, MyAccountActivity::class.java)
            .setAction(IntentConstants.ACTION_OPEN_ACHIEVEMENTS)
        startActivity(accountIntent)
    }

    @SuppressLint("CheckResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Timber.d("onActivityResult")
        if (intent == null) {
            return
        }
        if (nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)) {
            return
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

    companion object {
        private const val TAG_DECRYPT = "decrypt"
    }
}