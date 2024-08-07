package mega.privacy.android.app.presentation.filelink

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication.Companion.isClosedChat
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.extensions.isPortrait
import mega.privacy.android.app.featuretoggle.ABTestFeatures
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.DecryptAlertDialog
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.ManagerActivity.Companion.TRANSFERS_TAB
import mega.privacy.android.app.presentation.advertisements.model.AdsSlotIDs
import mega.privacy.android.app.presentation.clouddrive.FileLinkViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.filelink.view.FileLinkView
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.PublicFileImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.presentation.transfers.TransfersManagementActivity
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.presentation.transfers.view.IN_PROGRESS_TAB_INDEX
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.ACTION_SHOW_TRANSFERS
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * FileLinkActivity with compose view
 */
@AndroidEntryPoint
class FileLinkComposeActivity : TransfersManagementActivity(),
    DecryptAlertDialog.DecryptDialogListener {

    /**
     * MegaNavigator
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val viewModel: FileLinkViewModel by viewModels()

    private var mKey: String? = null

    private val selectImportFolderResult =
        ActivityResultCallback<ActivityResult> { activityResult ->
            viewModel.handleSelectImportFolderResult(activityResult)
        }

    private val selectImportFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        selectImportFolderResult
    )

    private val nameCollisionActivityLauncher = registerForActivityResult(
        NameCollisionActivityContract()
    ) { result ->
        result?.let {
            showSnackbar(SNACKBAR_TYPE, it, INVALID_HANDLE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate()")
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        viewModel.handleIntent(intent)
        viewModel.checkLoginRequired()

        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.state.collectAsStateWithLifecycle()
            val transferState by transfersManagementViewModel.state.collectAsStateWithLifecycle()
            val adsUiState by adsViewModel.uiState.collectAsStateWithLifecycle()

            EventEffect(
                event = uiState.openFile,
                onConsumed = viewModel::resetOpenFile,
                action = ::onOpenFile
            )

            val snackBarHostState = remember { SnackbarHostState() }
            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                FileLinkView(
                    viewState = uiState,
                    snackBarHostState = snackBarHostState,
                    transferState = transferState,
                    onBackPressed = { onBackPressedDispatcher.onBackPressed() },
                    onShareClicked = ::onShareClicked,
                    onPreviewClick = ::onPreviewClick,
                    onSaveToDeviceClicked = viewModel::handleSaveFile,
                    onImportClicked = ::onImportClicked,
                    onTransferWidgetClick = ::onTransfersWidgetClick,
                    onConfirmErrorDialogClick = ::onConfirmErrorDialogClick,
                    onErrorMessageConsumed = viewModel::resetErrorMessage,
                    onOverQuotaErrorConsumed = viewModel::resetOverQuotaError,
                    onForeignNodeErrorConsumed = viewModel::resetForeignNodeError,
                    adsUiState = adsUiState,
                    onAdClicked = { uri ->
                        uri?.let {
                            val intent = Intent(Intent.ACTION_VIEW, it)
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                                adsViewModel.onAdConsumed()
                            } else {
                                Timber.d("No Application found to can handle Ads intent")
                                adsViewModel.fetchNewAd()
                            }
                        }
                        adsViewModel.fetchNewAd(AdsSlotIDs.SHARED_LINK_SLOT_ID)
                    },
                    onAdDismissed = adsViewModel::onAdConsumed
                )
                StartTransferComponent(
                    event = uiState.downloadEvent,
                    onConsumeEvent = viewModel::resetDownloadFile,
                    snackBarHostState = snackBarHostState
                )
            }
        }
        setupObserver()
        checkForInAppAdvertisement()
    }

    private fun checkForInAppAdvertisement() {
        lifecycleScope.launch {
            runCatching {
                val isInAppAdvertisementEnabled = true
                val isAdsEnabled = getFeatureFlagValueUseCase(ABTestFeatures.ads)

                if (isInAppAdvertisementEnabled && isAdsEnabled) {
                    if (this@FileLinkComposeActivity.isPortrait()) {
                        adsViewModel.enableAdsFeature()
                        adsViewModel.fetchNewAd(AdsSlotIDs.SHARED_LINK_SLOT_ID)
                    }
                }
            }.onFailure {
                Timber.e("Failed to fetch feature flags or ab_ads test flag with error: ${it.message}")
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adsViewModel.onScreenOrientationChanged(isPortrait())
    }

    private fun setupObserver() {
        this.collectFlow(viewModel.state) {
            when {
                it.shouldLogin == true -> {
                    showLoginScreen()
                }

                it.askForDecryptionDialog -> {
                    askForDecryptionKeyDialog()
                }

                it.collision != null -> {
                    nameCollisionActivityLauncher.launch(arrayListOf(it.collision))
                    viewModel.resetCollision()
                }

                it.copySuccess -> {
                    launchManagerActivity()
                }
            }
        }
    }

    /**
     * Open the file in a separate activity
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

    private fun onConfirmErrorDialogClick() {
        if (isClosedChat) {
            startActivity(Intent(this@FileLinkComposeActivity, ManagerActivity::class.java))
        }
        finish()
    }

    /**
     * Handle widget click
     */
    private fun onTransfersWidgetClick() {
        transfersManagement.setAreFailedTransfers(false)
        if (transfersManagement.isOnTransferOverQuota()) {
            transfersManagement.setHasNotToBeShowDueToTransferOverQuota(true)
        }
        lifecycleScope.launch {
            val credentials = runCatching { getAccountCredentialsUseCase() }.getOrNull()
            if (megaApi.isLoggedIn == 0 || credentials == null) {
                Timber.w("Not logged in, no action.")
                return@launch
            }
            if (getFeatureFlagValueUseCase(AppFeatures.TransfersSection)) {
                navigator.openTransfers(this@FileLinkComposeActivity, IN_PROGRESS_TAB_INDEX)
            } else {
                startActivity(
                    Intent(this@FileLinkComposeActivity, ManagerActivity::class.java)
                        .setAction(ACTION_SHOW_TRANSFERS)
                        .putExtra(TRANSFERS_TAB, TransfersTab.PENDING_TAB)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            finish()
        }
    }

    /**
     * Open folder selection for importing the node
     */
    private fun onImportClicked() {
        val intent = Intent(this, FileExplorerActivity::class.java)
        intent.action = FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER
        selectImportFolderLauncher.launch(intent)
    }

    private fun launchManagerActivity() {
        startActivity(
            Intent(this@FileLinkComposeActivity, ManagerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private fun showLoginScreen() {
        Timber.d("Refresh session - sdk or karere")
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
        intent.data = Uri.parse(viewModel.state.value.url)
        intent.action = Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    /**
     * Show dialog for getting decryption key
     */
    private fun askForDecryptionKeyDialog() {
        Timber.d("askForDecryptionKeyDialog")
        val decryptAlertDialog = DecryptAlertDialog.Builder()
            .setTitle(getString(R.string.alert_decryption_key))
            .setPosText(R.string.general_decryp).setNegText(R.string.general_cancel)
            .setMessage(getString(R.string.message_decryption_key))
            .setErrorMessage(R.string.invalid_decryption_key).setKey(mKey)
            .build()
        decryptAlertDialog.show(supportFragmentManager, TAG_DECRYPT)
        viewModel.resetAskForDecryptionKeyDialog()
    }

    private fun onPreviewClick() {
        with(viewModel.state.value) {
            val nameType = typeForName(title)
            when {
                nameType.isImage -> lifecycleScope.launch {
                    val intent = ImagePreviewActivity.createIntent(
                        context = this@FileLinkComposeActivity,
                        imageSource = ImagePreviewFetcherSource.PUBLIC_FILE,
                        menuOptionsSource = ImagePreviewMenuSource.PUBLIC_FILE,
                        params = mapOf(PublicFileImageNodeFetcher.URL to url),
                    )
                    viewModel.updateImageIntent(intent)
                }

                nameType.isVideoMimeType || nameType.isAudio -> {
                    lifecycleScope.launch {
                        if (fileNode is TypedFileNode) {
                            runCatching {
                                val contentUri = viewModel.getNodeContentUri()
                                megaNavigator.openMediaPlayerActivityByFileNode(
                                    context = this@FileLinkComposeActivity,
                                    contentUri = contentUri,
                                    fileNode = fileNode,
                                    isFolderLink = true,
                                    viewType = FILE_LINK_ADAPTER,
                                )
                            }.onFailure {
                                Toast.makeText(
                                    this@FileLinkComposeActivity,
                                    getString(R.string.intent_not_available),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }

                nameType.isPdf -> {
                    val intent = Intent(this@FileLinkComposeActivity, PdfViewerActivity::class.java)
                    viewModel.updatePdfIntent(intent, nameType.type)
                }

                nameType.isOpenableTextFile(sizeInBytes) -> {
                    val intent =
                        Intent(this@FileLinkComposeActivity, TextEditorActivity::class.java)
                    viewModel.updateTextEditorIntent(intent)
                }

                else -> {
                    Timber.w("Unknown File Type")
                    null
                }
            }
        }
    }

    override fun onDialogPositiveClick(key: String?) {
        mKey = key
        viewModel.decrypt(key)
    }

    override fun onDialogNegativeClick() {
        finish()
    }

    companion object {
        private const val TAG_DECRYPT = "decrypt"
    }
}