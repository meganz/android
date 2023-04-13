package mega.privacy.android.app.presentation.folderlink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import mega.privacy.android.app.databinding.ActivityFolderLinkComposeBinding
import mega.privacy.android.app.main.DecryptAlertDialog
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.folderlink.view.FolderLinkView
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.transfers.TransfersManagementActivity
import mega.privacy.android.app.utils.AlertDialogUtil
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import timber.log.Timber
import javax.inject.Inject

/**
 * FolderLinkActivity with compose view
 */
@AndroidEntryPoint
class FolderLinkComposeActivity : TransfersManagementActivity(),
    DecryptAlertDialog.DecryptDialogListener {

    private lateinit var binding: ActivityFolderLinkComposeBinding

    /**
     * String formatter for file desc
     */
    @Inject
    lateinit var stringUtilWrapper: StringUtilWrapper

    private val viewModel: FolderLinkViewModel by viewModels()

    private var mKey: String? = null
    private var statusDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFolderLinkComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                onMoreClicked = { },
                stringUtilWrapper = stringUtilWrapper,
                onMenuClick = { },
                onItemClicked = { },
                onLongClick = viewModel::onItemLongClick,
                onChangeViewTypeClick = viewModel::onChangeViewTypeClicked,
                onSortOrderClick = { },
                onSelectAllActionClicked = viewModel::onSelectAllClicked,
                onClearAllActionClicked = viewModel::onClearAllClicked,
                onSaveToDeviceClicked = { },
                getEmptyViewString()
            )
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
                        }
                        it.isLoginComplete && it.isNodesFetched -> {}
                        it.askForDecryptionKeyDialog -> {
                            askForDecryptionKeyDialog()
                        }
                        else -> {
                            try {
                                Timber.w("Show error dialog")
                                showErrorDialog(it.errorDialogTitle, it.errorDialogContent)

                            } catch (ex: Exception) {
                                //showSnackbar(it.snackBarMessage)
                                finish()
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

    companion object {
        private const val TAG_DECRYPT = "decrypt"
    }
}