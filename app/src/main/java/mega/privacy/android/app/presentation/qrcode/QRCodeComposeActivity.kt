package mega.privacy.android.app.presentation.qrcode

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.usecase.UploadUseCase
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * QR code compose activity
 */
@AndroidEntryPoint
class QRCodeComposeActivity : PasscodeActivity() {

    private val viewModel: QRCodeViewModel by viewModels()

    /**
     * QR code mapper
     */
    @Inject
    lateinit var qrCodeMapper: QRCodeMapper

    /**
     * Get theme mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * Upload use case
     */
    @Inject
    lateinit var uploadUseCase: UploadUseCase

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
        super.onCreate(savedInstanceState)

        setContent {
            val mode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val viewState by viewModel.uiState.collectAsStateWithLifecycle()
            AndroidTheme(isDark = mode.isDarkMode()) {
                QRCodeView(
                    viewState = viewState,
                    onBackPressed = onBackPressedDispatcher::onBackPressed,
                    onCreateQRCode = viewModel::createQRCode,
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
                    onUploadFile = { uploadFile(qrFile = it.first, parentHandle = it.second) },
                    onShowCollisionConsumed = viewModel::resetShowCollision,
                    onUploadFileConsumed = viewModel::resetUploadFile,
                    qrCodeMapper = qrCodeMapper,
                )
            }
        }
        viewModel.createQRCode()
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

    @SuppressLint("CheckResult")
    private fun uploadFile(qrFile: File, parentHandle: Long) {
        PermissionUtils.checkNotificationsPermission(this)
        uploadUseCase.upload(this, qrFile, parentHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewModel.setResultMessage(R.string.save_qr_cloud_drive, arrayOf(qrFile.name))
            }) { t: Throwable? -> Timber.e(t) }
    }

    private fun showCollision(collision: NameCollision) {
        nameCollisionActivityContract?.launch(arrayListOf(collision))
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