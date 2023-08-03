package mega.privacy.android.app.presentation.filelink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.main.DecryptAlertDialog
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.clouddrive.FileLinkViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.filelink.view.FileLinkView
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.transfers.TransfersManagementActivity
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import timber.log.Timber

/**
 * FileLinkActivity with compose view
 */
@AndroidEntryPoint
class FileLinkComposeActivity : TransfersManagementActivity(),
    DecryptAlertDialog.DecryptDialogListener {

    private val viewModel: FileLinkViewModel by viewModels()

    private var mKey: String? = null

    private val nodeSaver = NodeSaver(
        this, this, this,
        AlertsAndWarnings.showSaveToDeviceConfirmDialog(this)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate()")
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        viewModel.handleIntent(intent)
        viewModel.checkLoginRequired()

        savedInstanceState?.let { nodeSaver.restoreState(savedInstanceState) }

        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.state.collectAsStateWithLifecycle()

            EventEffect(
                event = uiState.openFile,
                onConsumed = viewModel::resetOpenFile,
                action = ::onOpenFile
            )

            AndroidTheme(isDark = themeMode.isDarkMode()) {
                FileLinkView(
                    viewState = uiState,
                    onBackPressed = { onBackPressedDispatcher.onBackPressed() },
                    onShareClicked = ::onShareClicked,
                    onPreviewClick = { viewModel.onPreviewClick(this@FileLinkComposeActivity) },
                    onSaveToDeviceClicked = { },
                    onImportClicked = { },
                )
            }
        }
        setupObserver()
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
                    nameCollisionActivityContract?.launch(arrayListOf(it.collision))
                    viewModel.resetCollision()
                }

                it.collisionCheckThrowable != null -> {
                    viewModel.resetCollisionError()
                }

                it.copySuccess -> {
                    launchManagerActivity()
                }

                it.copyThrowable != null -> {
                    if (!manageCopyMoveException(it.copyThrowable)) {
                        launchManagerActivity()
                    } else {
                        viewModel.resetCopyError()
                    }
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
        MegaNodeUtil.shareLink(this, viewModel.state.value.url)
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