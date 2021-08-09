package mega.privacy.android.app.getLink

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.constants.EventConstants.EVENT_COPY_LINK_TO_CLIPBOARD
import mega.privacy.android.app.databinding.GetLinkActivityLayoutBinding
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.lollipop.megachat.ChatExplorerActivity
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import java.util.*

class GetLinkActivity : PasscodeActivity(), SnackbarShower {
    companion object {
        const val SHARE = 0
        const val SEND_TO_CHAT = 1
    }

    private val viewModel: GetLinkViewModel by viewModels()

    private lateinit var binding: GetLinkActivityLayoutBinding
    private lateinit var navController: NavController

    private var menu: Menu? = null

    private val copyLinkObserver = Observer<Pair<String, String>> { copyInfo ->
        copyToClipboard(copyInfo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GetLinkActivityLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent == null || shouldRefreshSessionDueToSDK()) {
            return
        }

        setupView()
        setupObservers()
    }

    override fun onDestroy() {
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_COPY_LINK_TO_CLIPBOARD)
            .removeObserver(copyLinkObserver as Observer<Any>)

        super.onDestroy()
    }

    private fun setupView() {
        val handle = intent.getLongExtra(HANDLE, INVALID_HANDLE)
        if (handle == INVALID_HANDLE) {
            finish()
            return
        }

        viewModel.setLink(handle)

        binding.toolbarGetLink.visibility = GONE
        setSupportActionBar(binding.toolbarGetLink)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupNavController()
    }

    private fun setupObservers() {
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_COPY_LINK_TO_CLIPBOARD)
            .observeForever(copyLinkObserver as Observer<Any>)
    }

    private fun setupNavController() {
        navController =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
                .navController

        navController.addOnDestinationChangedListener { _, _, _ ->
            when (navController.currentDestination?.id) {
                R.id.main_get_link -> {
                    supportActionBar?.apply {
                        title = viewModel.getLinkFragmentTitle()
                        show()
                    }
                }
                R.id.copyright -> supportActionBar?.hide()
                R.id.decryption_key -> {
                    supportActionBar?.title =
                        StringResourcesUtils.getString(R.string.option_decryption_key)
                            .toUpperCase(Locale.getDefault())
                }
                R.id.password -> {
                    supportActionBar?.title = StringResourcesUtils.getString(
                        if (viewModel.getLinkPassword() == null) R.string.set_password_protection_dialog
                        else R.string.reset_password_label
                    ).toUpperCase(Locale.getDefault())
                }
            }

            refreshMenuOptionsVisibility()
        }

        if (viewModel.shouldShowCopyright()) {
            navController.navigate(R.id.copyright)
        }
    }

    /**
     * Copies a link, decryption key or password into clipboard and shows a snackbar.
     *
     * @param copyInfo First is the  content to copy, second the text to show as confirmation.
     */
    private fun copyToClipboard(copyInfo: Pair<String, String>) {
        val clipManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(COPIED_TEXT_LABEL, copyInfo.first)
        clipManager.setPrimaryClip(clip)

        showSnackbar(SNACKBAR_TYPE, copyInfo.second, MEGACHAT_INVALID_HANDLE)
    }

    /**
     * Shows a warning before share link when the user has the Send decryption key separately or
     * the password protection enabled, asking if they want to share also the key or the password.
     *
     * @param type Indicates if the share is send to chat or share outside the app.
     * @param data Intent containing the info to share to chat or null if is sharing outside the app.
     */
    private fun showShareKeyOrPasswordDialog(type: Int, data: Intent?) {
        val shareKeyDialogBuilder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)

        shareKeyDialogBuilder.setMessage(
            getString(
                if (!isTextEmpty(viewModel.getLinkPassword())) R.string.share_password_warning
                else R.string.share_key_warning
            ) + "\n"
        )
            .setCancelable(false)
            .setPositiveButton(
                if (!isTextEmpty(viewModel.getLinkPassword())) R.string.button_share_password
                else R.string.button_share_key
            ) { _, _ ->
                if (type == SHARE) {
                    viewModel.shareLinkAndKeyOrPassword { intent -> startActivity(intent) }
                } else if (type == SEND_TO_CHAT) {
                    viewModel.sendLinkToChat(data, true) { intent ->
                        handleActivityResult(intent)
                    }
                }
            }
            .setNegativeButton(R.string.general_dismiss) { _, _ ->
                if (type == SHARE) {
                    viewModel.shareCompleteLink { intent -> startActivity(intent) }
                } else if (type == SEND_TO_CHAT) {
                    viewModel.sendLinkToChat(data, false) { intent ->
                        handleActivityResult(intent)
                    }
                }
            }

        shareKeyDialogBuilder.create().show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SEND_LINK) {
            if (viewModel.shouldShowShareKeyOrPasswordDialog()) {
                showShareKeyOrPasswordDialog(SEND_TO_CHAT, data)
            } else {
                viewModel.sendToChat(data, shouldAttachKeyOrPassword = false) { intent ->
                    handleActivityResult(intent)
                }
            }
        }
    }

    private fun handleActivityResult(data: Intent?) {
        MegaAttacher(this).handleActivityResult(
            REQUEST_CODE_SELECT_CHAT,
            RESULT_OK,
            data,
            this
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_get_link, menu)
        this.menu = menu

        refreshMenuOptionsVisibility()

        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Sets the right Toolbar options depending on current situation.
     */
    private fun refreshMenuOptionsVisibility() {
        val menu = this.menu ?: return

        when (navController.currentDestination?.id) {
            R.id.main_get_link -> menu.toggleAllMenuItemsVisibility(true)
            else -> menu.toggleAllMenuItemsVisibility(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.action_share -> {
                if (viewModel.shouldShowShareKeyOrPasswordDialog()) {
                    showShareKeyOrPasswordDialog(SHARE, null)
                } else {
                    viewModel.shareLink { intent -> startActivity(intent) }
                }
            }
            R.id.action_chat -> {
                startActivityForResult(
                    Intent(this, ChatExplorerActivity::class.java),
                    REQUEST_CODE_SEND_LINK
                )
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (psaWebBrowser.consumeBack()) return

        if (!navController.navigateUp()) {
            finish()
        }
//        if (visibleFragment == DECRYPTION_KEY_FRAGMENT || visibleFragment == PASSWORD_FRAGMENT) {
//            if (visibleFragment == PASSWORD_FRAGMENT) {
//                passwordFragment.resetView()
//            }
//
//            showFragment(GET_LINK_FRAGMENT)
//        } else {
//            finish()
//        }
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, binding.getLinkCoordinatorLayout, content, chatId)
    }
}
