package mega.privacy.android.app.presentation.qrcode

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.filestorage.FileStorageActivity
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.settings.model.qrTargetPreference
import mega.privacy.android.app.presentation.settings.model.storageTargetPreference
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import javax.inject.Inject

/**
 * QR code compose activity
 */
@AndroidEntryPoint
class QRCodeComposeActivity : PasscodeActivity() {

    private val viewModel: QRCodeViewModel by viewModels()
    private var inviteContacts = false
    private var showScanQrView = false

    /**
     * QR code mapper
     */
    @Inject
    lateinit var qrCodeMapper: QRCodeMapper

    /**
     * Get theme mode
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    /**
     * Mega navigator
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val nameCollisionActivityLauncher = registerForActivityResult(
        NameCollisionActivityContract()
    ) { result ->
        result?.let {
            showSnackbar(SNACKBAR_TYPE, it, INVALID_HANDLE)
        }
    }

    private val selectStorageDestinationLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            if (result.resultCode == RESULT_OK) {
                data?.getStringExtra(FileStorageActivity.EXTRA_PATH)?.let { parentPath ->
                    viewModel.saveToFileSystem(parentPath)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        showScanQrView = intent.getBooleanExtra(Constants.OPEN_SCAN_QR, false)
        inviteContacts = intent.getBooleanExtra(Constants.INVITE_CONTACT, false)
        var showLoader = true

        viewModel.setFinishActivityOnScanComplete(inviteContacts)
        setContent {
            val mode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val viewState by viewModel.uiState.collectAsStateWithLifecycle()
            OriginalTheme(isDark = mode.isDarkMode()) {
                QRCodeView(
                    viewState = viewState,
                    onBackPressed = onBackPressedDispatcher::onBackPressed,
                    onCreateQRCode = { viewModel.createQRCode(true) },
                    onDeleteQRCode = viewModel::deleteQRCode,
                    onResetQRCode = viewModel::resetQRCode,
                    onScanQrCodeClicked = { viewModel.scanCode(this) },
                    onCopyLinkClicked = viewModel::copyContactLink,
                    onViewContactClicked = ::onViewContact,
                    onInviteContactClicked = viewModel::sendInvite,
                    onResultMessageConsumed = viewModel::resetResultMessage,
                    onScannedContactLinkResultConsumed = viewModel::resetScannedContactLinkResult,
                    onInviteContactResultConsumed = viewModel::resetInviteContactResult,
                    onInviteResultDialogDismiss = viewModel::resetScannedContactEmail,
                    onInviteContactDialogDismiss = viewModel::resetScannedContactAvatar,
                    onCloudDriveClicked = viewModel::saveToCloudDrive,
                    onFileSystemClicked = ::saveToFileSystem,
                    onShowCollision = ::showCollision,
                    onShowCollisionConsumed = viewModel::resetShowCollision,
                    onUploadFile = {
                        viewModel.uploadFile(
                            qrFile = it.first,
                            parentHandle = it.second
                        )
                    },
                    onUploadFileConsumed = viewModel::resetUploadFile,
                    onScanCancelConsumed = viewModel::resetScanCancel,
                    onUploadEventConsumed = viewModel::onUploadEventConsumed,
                    qrCodeMapper = qrCodeMapper,
                    navigateToQrSettings = { megaNavigator.openSettings(this, qrTargetPreference) },
                    navigateToStorageSettings = {
                        megaNavigator.openSettings(
                            this,
                            storageTargetPreference
                        )
                    },
                )
            }
        }
        if (showScanQrView || inviteContacts) {
            showLoader = false
            viewModel.scanCode(this)
        }
        viewModel.createQRCode(showLoader)
    }

    private fun onViewContact(email: String) {
        ContactUtil.openContactInfoActivity(this, email)
        finish()
    }

    private fun saveToFileSystem() {
        val intent = Intent(this, FileStorageActivity::class.java).apply {
            putExtra(
                FileStorageActivity.PICK_FOLDER_TYPE,
                FileStorageActivity.PickFolderType.DOWNLOAD_FOLDER.folderType
            )
            action = FileStorageActivity.Mode.PICK_FOLDER.action
        }
        selectStorageDestinationLauncher.launch(intent)
    }

    private fun showCollision(collision: NameCollision) {
        nameCollisionActivityLauncher.launch(arrayListOf(collision))
    }
}

/**
 * Find the Activity in a given Context.
 */
internal fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}