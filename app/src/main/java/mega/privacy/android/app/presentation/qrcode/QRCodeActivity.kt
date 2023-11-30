package mega.privacy.android.app.presentation.qrcode

import android.Manifest
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.StatFs
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.QRCodeSaveBottomSheetDialogFragment
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.qrcode.model.MyQRTab
import mega.privacy.android.app.presentation.qrcode.mycode.MyCodeFragment
import mega.privacy.android.app.presentation.qrcode.mycode.MyCodeViewModel
import mega.privacy.android.app.presentation.qrcode.scan.ScanCodeViewModel
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.presentation.settings.model.TargetPreference
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

/**
 * QR code Activity
 */
@AndroidEntryPoint
class QRCodeActivity : PasscodeActivity() {

    /**
     * Get theme mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val qrCodeViewModel: MyCodeViewModel by viewModels()
    private val scanCodeViewModel: ScanCodeViewModel by viewModels()

    private lateinit var rootView: View

    private var inviteContacts = false
    private var showScanQrView = false

    private var qrCodeSaveBottomSheetDialogFragment: QRCodeSaveBottomSheetDialogFragment? = null

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            showScanQrView = it.getBoolean(Constants.OPEN_SCAN_QR, false)
            inviteContacts = it.getBoolean(Constants.INVITE_CONTACT, false)
        } ?: run {
            showScanQrView = intent.getBooleanExtra(Constants.OPEN_SCAN_QR, false)
            inviteContacts = intent.getBooleanExtra(Constants.INVITE_CONTACT, false)
        }

        setContent {
            val mode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val qrCodeUIState by qrCodeViewModel.uiState.collectAsState()
            MegaAppTheme(isDark = mode.isDarkMode()) {

                val cameraPermissionState = rememberPermissionState(
                    Manifest.permission.CAMERA
                )

                if (!cameraPermissionState.status.isGranted) {
                    LaunchedEffect(Unit) {
                        cameraPermissionState.launchPermissionRequest()
                    }
                }
                QRCodeBody(
                    qrCodeUIState = qrCodeUIState,
                    onBackPressed = { onBackPressedDispatcher.onBackPressed() },
                    onDeleteQRCode = ::deleteQR,
                    onResetQRCode = ::resetQR,
                    onGotoSettings = ::gotoSettings,
                    onSaveQRCode = ::saveQR,
                    onShareQRCode = { qrCodeViewModel.startSharing() },
                    initialTab = if (showScanQrView || inviteContacts) MyQRTab.ScanQRCode else MyQRTab.MyQRCode
                )
            }
        }
        setupFlow()
        rootView = findViewById(android.R.id.content)
        scanCodeViewModel.updateFinishActivityOnScanComplete(inviteContacts)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("deprecation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        intent?.takeIf { requestCode == Constants.REQUEST_DOWNLOAD_FOLDER && resultCode == RESULT_OK }
            ?.getStringExtra(FileStorageActivity.EXTRA_PATH)
            ?.let { parentPath -> saveToFileSystem(parentPath) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(Constants.OPEN_SCAN_QR, showScanQrView)
        outState.putBoolean(Constants.INVITE_CONTACT, inviteContacts)
    }

    private fun setupFlow() {
        collectFlow(qrCodeViewModel.uiState, Lifecycle.State.RESUMED) { uiState ->
            with(uiState) {
                localQRCodeFile?.let {
                    shareQR()
                    qrCodeViewModel.finishSharing()
                }
            }
        }
    }

    private fun saveToFileSystem(parentPath: String) {
        val myEmail = megaApi.myEmail
        val qrFile: File? = CacheFolderManager.buildQrFile(
            fileName = myEmail + MyCodeFragment.QR_IMAGE_FILE_NAME
        )

        if (qrFile == null) {
            showSnackbar(rootView, getString(R.string.general_error))
            return
        }

        if (!qrFile.exists()) {
            showSnackbar(rootView, getString(R.string.error_download_qr))
            return
        }

        val hasStoragePermission =
            PermissionUtils.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (!hasStoragePermission) {
            PermissionUtils.requestPermission(
                this,
                Constants.REQUEST_WRITE_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        val availableFreeSpace = try {
            StatFs(parentPath).run {
                availableBlocksLong.toDouble() * blockSizeLong.toDouble()
            }
        } catch (e: Exception) {
            Timber.e(e)
            Double.MAX_VALUE
        }

        if (availableFreeSpace < qrFile.length()) {
            showSnackbar(
                rootView,
                getString(R.string.error_not_enough_free_space)
            )
            return
        }

        val newQrFile =
            File(parentPath, "$myEmail${MyCodeFragment.QR_IMAGE_FILE_NAME}")

        // For Android 11+ device, force to refresh MediaStore. Otherwise it is possible
        // that target file cannot be written.
        if (Util.isAndroid11OrUpper()) {
            MediaScannerConnection.scanFile(
                this,
                arrayOf(newQrFile.absolutePath),
                arrayOf("image/jpeg"),
                null
            )
        }

        try {
            newQrFile.createNewFile()
            val src = FileInputStream(qrFile).channel
            val dst = FileOutputStream(newQrFile, false).channel
            dst.transferFrom(src, 0, src.size())
            src.close()
            dst.close()
            showSnackbar(
                rootView,
                getString(R.string.success_download_qr, parentPath)
            )
        } catch (e: IOException) {
            Timber.e(e)
            showSnackbar(
                rootView,
                getString(R.string.general_error)
            )
        }
        return
    }

    private fun resetQR() {
        Timber.d("resetQRCode")
        val penColor = ContextCompat.getColor(this, R.color.dark_grey)
        val bgColor = ContextCompat.getColor(this, R.color.white_grey_700)
        val avatarBorderColor = ContextCompat.getColor(this, R.color.white_dark_grey)
        qrCodeViewModel.resetQRCode(
            width = MyCodeFragment.QRCODE_WIDTH,
            height = MyCodeFragment.QRCODE_WIDTH,
            penColor = penColor,
            bgColor = bgColor,
            avatarWidth = MyCodeFragment.AVATAR_WIDTH,
            avatarBorderWidth = MyCodeFragment.AVATAR_BORDER_WIDTH,
            avatarBorderColor = avatarBorderColor,
        )
    }

    private fun deleteQR() {
        Timber.d("deleteQR")
        qrCodeViewModel.deleteQR()
    }

    private fun gotoSettings() {
        val settingsIntent = SettingsActivity.getIntent(this, TargetPreference.QR).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(settingsIntent)
        finish()
    }

    private fun saveQR() {
        if (!qrCodeSaveBottomSheetDialogFragment.isBottomSheetDialogShown()) {
            qrCodeSaveBottomSheetDialogFragment =
                QRCodeSaveBottomSheetDialogFragment().also {
                    it.show(supportFragmentManager, it.tag)
                }
        }
    }

    private fun shareQR() {
        Timber.d("shareQR")

        qrCodeViewModel.uiState.value.localQRCodeFile?.let { qrCodeFile ->
            Timber.d("Use provider to share")

            val uri = FileProvider.getUriForFile(
                this@QRCodeActivity,
                Constants.AUTHORITY_STRING_FILE_PROVIDER,
                qrCodeFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, Uri.parse(uri.toString()))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, getString(R.string.context_share)))
        } ?: run {
            showSnackbar(rootView, getString(R.string.error_share_qr))
        }

    }
}
