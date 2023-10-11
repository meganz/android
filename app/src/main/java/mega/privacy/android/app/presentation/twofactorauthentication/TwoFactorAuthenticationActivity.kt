package mega.privacy.android.app.presentation.twofactorauthentication

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.settings.exportrecoverykey.ExportRecoveryKeyActivity
import mega.privacy.android.app.presentation.twofactorauthentication.view.TwoFactorAuthenticationView
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import timber.log.Timber
import javax.inject.Inject

/**
 * TwoFactorAuthenticationActivity
 */
@AndroidEntryPoint
class TwoFactorAuthenticationActivity : PasscodeActivity() {

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var qrCodeMapper: QRCodeMapper

    private val viewModel: TwoFactorAuthenticationViewModel by viewModels()

    private val downloadFolderActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val dataIntent = result.data
            if (result.resultCode == RESULT_OK && dataIntent != null) {
                exportRecoveryKey(dataIntent)
            }
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty() || requestCode != ExportRecoveryKeyActivity.WRITE_STORAGE_TO_SAVE_RK) {
            Timber.w("Permissions ${permissions[0]} not granted")
        }
        onPermissionAsked()
    }

    @Composable
    private fun TwoFactorAuthenticationScreen(isDarkMode: Boolean) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        TwoFactorAuthenticationView(
            uiState = uiState,
            isDarkMode = isDarkMode,
            qrCodeMapper = qrCodeMapper,
            onBackPressedDispatcher = onBackPressedDispatcher,
            onFinishActivity = ::finish,
            isIntentAvailable = { isIntentAvailable(uiState.twoFactorAuthUrl) },
            onOpenInClicked = this::onOpenInClicked,
            openPlayStore = this::openPlayStore,
            on2FAPinChanged = viewModel::on2FAPinChanged,
            on2FAChanged = viewModel::on2FAChanged,
            onFirstTime2FAConsumed = viewModel::onFirstTime2FAConsumed,
            on2FAPinReset = viewModel::on2FAPinReset,
            onExportRkClicked = { chooseRecoverySaveLocation() },
            onDismissClicked = {
                update2FASetting()
                finish()
            },
            onCopySeedLongClicked = { copySeed(uiState.seed.orEmpty()) },
            onIsRkExportSuccessfullyConsumed = viewModel::onIsRkExportSuccessfullyEventConsumed,
            onIsWritePermissionDeniedConsumed = viewModel::onWritePermissionDeniedEventConsumed,
            onIsSeedCopiedToClipboardConsumed = viewModel::onSeedCopiedToClipboardEventConsumed
        )
    }

    private fun isIntentAvailable(url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        return MegaApiUtils.isIntentAvailable(
            this@TwoFactorAuthenticationActivity,
            intent
        )
    }

    private fun onOpenInClicked(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
            AndroidTheme(isDark = themeMode.isDarkMode()) {
                TwoFactorAuthenticationScreen(themeMode.isDarkMode())
            }
        }
        viewModel.getAuthenticationCode()
    }

    /**
     * Action when permission has been asked to the user
     * Will save to storage if permission granted
     * else it will display a Snackbar telling that the user denied the request
     */
    private fun onPermissionAsked() {
        if (PermissionUtils.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            saveRecoveryKeyToStorage()
        } else {
            viewModel.triggerWritePermissionDeniedEvent()
        }
    }

    /**
     * Open folder selection, where user can select the location the recovery key will be stored.
     */
    private fun saveRecoveryKeyToStorage() {
        val intent = Intent(
            this@TwoFactorAuthenticationActivity,
            FileStorageActivity::class.java
        ).apply {
            action = FileStorageActivity.Mode.PICK_FOLDER.action
            putExtra(FileStorageActivity.EXTRA_SAVE_RECOVERY_KEY, true)
        }
        downloadFolderActivityResult.launch(intent)
    }

    /**
     * Action when User finished choosing folder to save recovery key
     * Will show SnackBar message from Compose
     */
    private fun exportRecoveryKey(result: Intent) = lifecycleScope.launch {
        val key = viewModel.getRecoveryKey()
        when {
            key.isNullOrBlank() -> {
                viewModel.setIsRkExportSuccessfullyEvent(false)
            }

            isSaveToTextFileSuccessful(key, result) -> {
                val intent =
                    Intent(
                        this@TwoFactorAuthenticationActivity,
                        ManagerActivity::class.java
                    )
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
                viewModel.setIsRkExportSuccessfullyEvent(true)
            }

            else -> {
                viewModel.setIsRkExportSuccessfullyEvent(false)
            }
        }
    }

    /**
     * Saving the recovery key to text file
     * @return is save successful as [Boolean]
     */
    private suspend fun isSaveToTextFileSuccessful(key: String, result: Intent): Boolean =
        withContext(Dispatchers.IO) {
            FileUtil.saveTextOnContentUri(
                this@TwoFactorAuthenticationActivity.contentResolver,
                result.data,
                key
            )
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun update2FASetting() {
        setResult(RESULT_OK)
    }

    private fun openPlayStore() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=authenticator&c=apps"))
        startActivity(intent)
    }

    /**
     * Action when save button is clicked. Will save to storage if permission granted
     * else will ask for permission to write external storage
     */
    private fun chooseRecoverySaveLocation() {
        if (PermissionUtils.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            saveRecoveryKeyToStorage()
        } else {
            PermissionUtils.requestPermission(
                this,
                ExportRecoveryKeyActivity.WRITE_STORAGE_TO_SAVE_RK,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    private fun copySeed(seed: String) {
        Timber.d("Copy seed")
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        seed.let {
            ClipData.newPlainText("seed", seed)?.let {
                clipboard.setPrimaryClip(it)
                viewModel.triggerSeedCopiedToClipboardEvent()
            }
        }
    }
}