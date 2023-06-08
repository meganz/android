package mega.privacy.android.app.presentation.folderlink

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
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
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.databinding.ActivityFolderLinkComposeBinding
import mega.privacy.android.app.main.DecryptAlertDialog
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.favourites.ThumbnailViewModel
import mega.privacy.android.app.presentation.folderlink.view.FolderLinkView
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.transfers.TransfersManagementActivity
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
    private val thumbnailViewModel: ThumbnailViewModel by viewModels()

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

        setTransfersWidgetLayout(findViewById(R.id.transfers_widget_layout))
        intent?.let { viewModel.handleIntent(it) }
        setupObservers()
        viewModel.checkLoginRequired()
    }

    @Composable
    private fun StartFolderLinkView() {
        val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        val uiState by viewModel.state.collectAsStateWithLifecycle()

        AndroidTheme(isDark = themeMode.isDarkMode()) {
            FolderLinkView(
                state = uiState,
                onBackPressed = viewModel::handleBackPress,
                onShareClicked = ::onShareClicked,
                onMoreOptionClick = viewModel::handleMoreOptionClick,
                onItemClicked = { viewModel.onItemClick(it, this) },
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
                onStorageDialogHorizontalActionButtonClick = { viewModel.handleActionClick(this) },
                onStorageDialogVerticalActionButtonClick = { viewModel.handleActionClick(this) },
                onStorageDialogAchievementButtonClick = ::navigateToAchievements,
                emptyViewString = getEmptyViewString(),
                thumbnailViewModel = thumbnailViewModel,
                onDisputeTakeDownClicked = ::navigateToLink,
                onLinkClicked = ::navigateToLink
            )
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
                            it.folderSubHandle?.let { handle -> viewModel.fetchNodes(handle) }
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

                        it.isLoginComplete && it.isNodesFetched -> {}
                        it.askForDecryptionKeyDialog -> {
                            askForDecryptionKeyDialog()
                        }

                        else -> {
                            if (it.errorDialogTitle != -1 && it.errorDialogContent != -1) {
                                Timber.w("Show error dialog")
                                showErrorDialog(it.errorDialogTitle, it.errorDialogContent)
                            }
                        }
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
        val decryptAlertDialog = builder.setListener(this)
            .setTitle(getString(R.string.alert_decryption_key))
            .setPosText(R.string.general_decryp).setNegText(R.string.general_cancel)
            .setMessage(getString(R.string.message_decryption_key))
            .setErrorMessage(R.string.invalid_decryption_key)
            .setKey(mKey)
            .build()

        decryptAlertDialog.dialog?.setOnDismissListener {
            viewModel.resetAskForDecryptionKeyDialog()
        }
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