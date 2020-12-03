package mega.privacy.android.app.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.GetLinkActivityLayoutBinding
import mega.privacy.android.app.fragments.getLinkFragments.CopyrightFragment
import mega.privacy.android.app.fragments.getLinkFragments.DecryptionKeyFragment
import mega.privacy.android.app.fragments.getLinkFragments.LinkFragment
import mega.privacy.android.app.fragments.getLinkFragments.LinkPasswordFragment
import mega.privacy.android.app.interfaces.GetLinkInterface
import mega.privacy.android.app.lollipop.controllers.ChatController
import mega.privacy.android.app.lollipop.megachat.ChatExplorerActivity
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LinksUtil.getKeyLink
import mega.privacy.android.app.utils.LinksUtil.getLinkWithoutKey
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaNode
import java.util.*

class GetLinkActivity : BaseActivity(), GetLinkInterface {
    companion object {
        const val GET_LINK_FRAGMENT = 0
        const val COPYRIGHT_FRAGMENT = 1
        const val DECRYPTION_KEY_FRAGMENT = 2
        const val PASSWORD_FRAGMENT = 3

        const val COPY_LINK = 0
        const val COPY_KEY = 1
        const val COPY_PASSWORD = 2

        const val SHARE = 0
        const val SEND_TO_CHAT = 1
    }

    private lateinit var binding: GetLinkActivityLayoutBinding

    private lateinit var linkFragmentTitle: String
    private lateinit var linkFragment: LinkFragment
    private lateinit var copyrightFragment: CopyrightFragment
    private lateinit var decryptionKeyFragment: DecryptionKeyFragment
    private lateinit var passwordFragment: LinkPasswordFragment

    private lateinit var node: MegaNode
    private lateinit var linkWithoutKey: String
    private lateinit var key: String
    private var linkWithPassword: String? = null
    private var passwordLink: String? = null

    private var visibleFragment = COPYRIGHT_FRAGMENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GetLinkActivityLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent == null || shouldRefreshSessionDueToSDK()) {
            return
        }

        val handle = intent.getLongExtra(HANDLE, INVALID_HANDLE)
        if (handle == INVALID_HANDLE) {
            finish()
            return
        }

        node = megaApi.getNodeByHandle(handle)

        binding.toolbarGetLink.visibility = GONE
        setSupportActionBar(binding.toolbarGetLink)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        visibleFragment =
            if (dbH.showCopyright.toBoolean() && (megaApi.publicLinks == null || megaApi.publicLinks.size == 0)) COPYRIGHT_FRAGMENT
            else GET_LINK_FRAGMENT

        showFragment(visibleFragment)
    }

    fun showSnackbar(snackbarType: Int, message: String?, chatId: Long) {
        showSnackbar(snackbarType, binding.getLinkCoordinatorLayout, message, chatId)
    }

    override fun showFragment(visibleFragment: Int) {
        this.visibleFragment = visibleFragment

        val ft = supportFragmentManager.beginTransaction()

        when (visibleFragment) {
            GET_LINK_FRAGMENT -> {
                window.statusBarColor =
                    ContextCompat.getColor(this, R.color.lollipop_dark_primary_color)

                if (!this::linkFragmentTitle.isInitialized) {
                    linkFragmentTitle =
                        if (node.isExported) getString(R.string.edit_link_option).toUpperCase(
                            Locale.getDefault()
                        )
                        else getString(R.string.context_get_link_menu).toUpperCase(
                            Locale.getDefault()
                        )
                }

                supportActionBar?.title = linkFragmentTitle
                supportActionBar?.show()

                if (!this::linkFragment.isInitialized) {
                    linkFragment = LinkFragment(this)
                }

                ft.replace(R.id.fragment_container_get_link, linkFragment)
            }
            COPYRIGHT_FRAGMENT -> {
                window.statusBarColor = ContextCompat.getColor(this, R.color.transparent_black)
                supportActionBar?.hide()

                if (!this::copyrightFragment.isInitialized) {
                    copyrightFragment = CopyrightFragment(this)
                }

                ft.replace(R.id.fragment_container_get_link, copyrightFragment)
            }
            DECRYPTION_KEY_FRAGMENT -> {
                supportActionBar?.title = getString(R.string.option_decryption_key).toUpperCase(
                    Locale.getDefault()
                )

                if (!this::decryptionKeyFragment.isInitialized) {
                    decryptionKeyFragment = DecryptionKeyFragment()
                }

                ft.replace(R.id.fragment_container_get_link, decryptionKeyFragment)
            }
            PASSWORD_FRAGMENT -> {
                supportActionBar?.title =
                    if (linkWithPassword == null) getString(R.string.set_password_protection_dialog).toUpperCase(
                        Locale.getDefault()
                    ) else getString(R.string.reset_password_label).toUpperCase(
                        Locale.getDefault()
                    )

                if (!this::passwordFragment.isInitialized) {
                    passwordFragment = LinkPasswordFragment(this)
                }

                ft.replace(R.id.fragment_container_get_link, passwordFragment)
            }
        }

        ft.commitNowAllowingStateLoss()
        invalidateOptionsMenu()
    }

    override fun getNode(): MegaNode {
        return node
    }

    override fun getLinkWithoutKey(): String {
        return linkWithoutKey
    }

    override fun getLinkKey(): String {
        return key
    }

    override fun copyLink(link: String) {
        copyToClipboard(link, COPY_LINK)
    }

    override fun copyLinkKey() {
        copyToClipboard(key, COPY_KEY)
    }

    override fun copyLinkPassword() {
        if (isTextEmpty(passwordLink)) {
            return
        }

        copyToClipboard(passwordLink!!, COPY_PASSWORD)
    }

    override fun showUpgradeToProWarning() {
        val upgradeToProDialogBuilder = AlertDialog.Builder(this, R.style.ResumeTransfersWarning)

        upgradeToProDialogBuilder.setTitle(R.string.upgrade_pro)
            .setMessage(getString(R.string.link_upgrade_pro_explanation) + "\n")
            .setCancelable(false)
            .setPositiveButton(R.string.button_plans_almost_full_warning) { _, _ ->
                navigateToUpgradeAccount()
                finish()
            }
            .setNegativeButton(R.string.verify_account_not_now_button) { dialog, _ ->
                dialog.dismiss()
            }

        upgradeToProDialogBuilder.create().show()
    }

    override fun getLinkWithPassword(): String? {
        return linkWithPassword
    }

    override fun getLinkPassword(): String? {
        return passwordLink
    }

    override fun removeLinkWithPassword() {
        linkWithPassword = null
        passwordLink = null
    }

    override fun setLinkPassword(passwordLink: String) {
        this.passwordLink = passwordLink
    }

    /**
     * Copies a link, decryption key or password into clipboard and shows a snackbar.
     *
     * @param textToCopy The content to copy.
     * @param type       The type of content to copy. It can be: COPY_KEY, COPY_PASSWORD or COPY_LINK.
     */
    private fun copyToClipboard(textToCopy: String, type: Int) {
        val clipManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(COPIED_TEXT_LABEL, textToCopy)
        clipManager.setPrimaryClip(clip)

        showSnackbar(
            SNACKBAR_TYPE, getString(
                when (type) {
                    COPY_KEY -> R.string.key_copied_clipboard
                    COPY_PASSWORD -> R.string.password_copied_clipboard
                    else -> R.string.link_copied_clipboard
                }
            ), MEGACHAT_INVALID_HANDLE
        )
    }

    /**
     * Launches an intent to share the link outside the app.
     *
     * @param link The link to share.
     */
    private fun shareLink(link: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = PLAIN_TEXT_SHARE_TYPE
        intent.putExtra(Intent.EXTRA_TEXT, link)
        startActivity(Intent.createChooser(intent, getString(R.string.context_get_link)))
    }

    /**
     * Updates the node from which the link is getting or managing.
     * Gets the link without its decryption key and the key separately.
     * Updates the UI of linkFragment.
     */
    fun setLink() {
        node = megaApi.getNodeByHandle(node.handle)
        linkWithoutKey = getLinkWithoutKey(node.publicLink)
        key = getKeyLink(node.publicLink)

        linkFragment.updateLink()
    }

    /**
     * Finish the action of set or reset the password protection, updates the UI resetting the set
     * passwordFragment view and showing again the linkFragment view with the password protection
     * enabled.
     *
     * @param linkWithPassword Link with password protection.
     */
    fun setLinkWithPassword(linkWithPassword: String) {
        this.linkWithPassword = linkWithPassword
        passwordFragment.resetView()
        showFragment(GET_LINK_FRAGMENT)
        linkFragment.updatePasswordLayouts()
    }

    /**
     * Shows a warning before share link when the user has the Send decryption key separately or
     * the password protection enabled, asking if they want to share also the key or the password.
     *
     * @param type Indicates if the share is send to chat or share outside the app.
     * @param data Intent containing the info to share to chat or null if is sharing outside the app.
     */
    private fun showShareKeyOrPasswordDialog(type: Int, data: Intent?) {
        val shareKeyDialogBuilder = AlertDialog.Builder(this, R.style.ResumeTransfersWarning)

        shareKeyDialogBuilder.setMessage(
            getString(
                if (!isTextEmpty(linkWithPassword)) R.string.share_password_warning
                else R.string.share_key_warning
            ) + "\n"
        )
            .setCancelable(false)
            .setPositiveButton(
                if (!isTextEmpty(linkWithPassword)) R.string.button_share_password
                else R.string.button_share_key
            ) { _, _ ->
                if (type == SHARE) {
                    shareLink(getLinkAndKeyOrPasswordToShare())
                } else if (type == SEND_TO_CHAT) {
                    sendToChat(data, getLinkToShare(), true)
                }
            }
            .setNegativeButton(R.string.general_dismiss) { _, _ ->
                if (type == SHARE) {
                    shareLink(getLinkToShare())
                } else if (type == SEND_TO_CHAT) {
                    sendToChat(data, getLinkToShare(), false)
                }
            }

        shareKeyDialogBuilder.create().show()
    }

    /**
     * Gets the string containing the link without its key and its key separately or the link
     * protected with password and the password depending on the current enabled option.
     *
     * @return The string with the info described.
     */
    private fun getLinkAndKeyOrPasswordToShare(): String {
        return if (!isTextEmpty(linkWithPassword)) getString(
            R.string.share_link_with_password,
            linkWithPassword,
            passwordLink
        )
        else getString(
            R.string.share_link_with_key,
            getLinkWithoutKey(node.publicLink),
            getKeyLink(node.publicLink)
        )
    }

    /**
     * Gets the link to share depending on the current enabled option. It can be:
     * - The link along with its decryption key
     * - The link without the decryption key
     * - The link along with its decryption key and with password protection
     *
     * @return The string with the info described.
     */
    private fun getLinkToShare(): String {
        return if (!isTextEmpty(linkWithPassword)) linkWithPassword!!
        else if (linkFragment.isSendDecryptedKeySeparatelyEnabled()) getLinkWithoutKey(node.publicLink)
        else node.publicLink
    }

    /**
     * Checks if should show the warning to share the decryption key or password protection.
     *
     * @return True if password protection or send decryption key separately option is enabled.
     *         False otherwise.
     */
    private fun shouldShowShareKeyOrPasswordDialog(): Boolean {
        return !isTextEmpty(linkWithPassword) || linkFragment.isSendDecryptedKeySeparatelyEnabled()
    }

    /**
     * Shares the link and extra content if enabled (decryption key or password) to chat.
     *
     * @param data                      Intent containing the info to share the content to chats.
     * @param link                      The link to share.
     * @param shouldAttachKeyOrPassword True if should share the decryption key or password. False otherwise.
     */
    private fun sendToChat(data: Intent?, link: String, shouldAttachKeyOrPassword: Boolean) {
        data?.putExtra(EXTRA_LINK, link)

        if (shouldAttachKeyOrPassword) {
            if (!isTextEmpty(linkWithPassword)) {
                data?.putExtra(EXTRA_PASSWORD, passwordLink)
            } else {
                data?.putExtra(EXTRA_KEY, getKeyLink(node.publicLink))
            }
        }

        ChatController(this).checkIntentToShareSomething(data)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SEND_LINK) {
            if (shouldShowShareKeyOrPasswordDialog()) {
                showShareKeyOrPasswordDialog(SEND_TO_CHAT, data)
            } else {
                sendToChat(data, node.publicLink, false)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_get_link, menu)

        val visible = visibleFragment == GET_LINK_FRAGMENT

        menu?.findItem(R.id.action_share)?.isVisible = visible
        menu?.findItem(R.id.action_chat)?.isVisible = visible

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.action_share -> {
                if (shouldShowShareKeyOrPasswordDialog()) {
                    showShareKeyOrPasswordDialog(SHARE, null)
                } else {
                    shareLink(node.publicLink)
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
        if (visibleFragment == DECRYPTION_KEY_FRAGMENT || visibleFragment == PASSWORD_FRAGMENT) {
            if (visibleFragment == PASSWORD_FRAGMENT) {
                passwordFragment.resetView()
            }

            showFragment(GET_LINK_FRAGMENT)
        } else {
            finish()
        }
    }
}