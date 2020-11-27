package mega.privacy.android.app.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import androidx.core.content.ContextCompat
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.GetLinkActivityLayoutBinding
import mega.privacy.android.app.fragments.getLinkFragments.CopyrightFragment
import mega.privacy.android.app.fragments.getLinkFragments.GetLinkFragment
import mega.privacy.android.app.interfaces.GetLinkInterface
import mega.privacy.android.app.lollipop.controllers.ChatController
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.lollipop.megachat.ChatExplorerActivity
import mega.privacy.android.app.utils.Constants.*
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaNode

class GetLinkActivity : BaseActivity(), GetLinkInterface {
    companion object {
        const val GET_LINK_FRAGMENT = 0
        const val COPYRIGHT_FRAGMENT = 1
    }

    private lateinit var binding: GetLinkActivityLayoutBinding

    private var getLinkFragment: GetLinkFragment? = null
    private var copyrightFragment: CopyrightFragment? = null

    private lateinit var nC: NodeController
    private lateinit var node: MegaNode

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

        nC = NodeController(this)

        node = megaApi.getNodeByHandle(handle)

        binding.toolbarGetLink.visibility = GONE
        setSupportActionBar(binding.toolbarGetLink)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val visibleFragment =
            if (dbH.showCopyright.toBoolean() && (megaApi.publicLinks == null || megaApi.publicLinks.size == 0)) COPYRIGHT_FRAGMENT
            else GET_LINK_FRAGMENT

        showFragment(visibleFragment)
    }

    fun showSnackbar(snackbarType: Int, message: String, chatId: Long) {
        showSnackbar(snackbarType, binding.getLinkCoordinatorLayout, message, chatId)
    }


    override fun showFragment(visibleFragment: Int) {
        val ft = supportFragmentManager.beginTransaction()

        when (visibleFragment) {
            GET_LINK_FRAGMENT -> {
                window.statusBarColor =
                    ContextCompat.getColor(this, R.color.lollipop_dark_primary_color)

                supportActionBar?.title =
                    if (node.isExported) getString(R.string.edit_link_option)
                    else getString(R.string.context_get_link_menu)

                supportActionBar?.show()

                if (getLinkFragment == null) {
                    getLinkFragment = GetLinkFragment(this)
                }

                ft.replace(R.id.fragment_container_get_link, getLinkFragment!!)
            }
            COPYRIGHT_FRAGMENT -> {
                window.statusBarColor = ContextCompat.getColor(this, R.color.transparent_black)
                supportActionBar?.hide()

                if (copyrightFragment == null) {
                    copyrightFragment = CopyrightFragment(this)
                }

                ft.replace(R.id.fragment_container_get_link, copyrightFragment!!)
            }
        }

        ft.commitNowAllowingStateLoss()
    }

    override fun getNode(): MegaNode {
        return node
    }

    override fun copyLinkOrKey(linkOrKey: String, isLink: Boolean) {
        val clipManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(COPIED_TEXT_LABEL, linkOrKey)
        clipManager.setPrimaryClip(clip)
        showSnackbar(
            SNACKBAR_TYPE,
            getString(
                if (isLink) R.string.link_copied_clipboard
                else R.string.key_copied_clipboard
            ), MEGACHAT_INVALID_HANDLE
        )
    }

    override fun startSetPassword() {

    }

    override fun exportNode() {
        nC.exportLink(node)
    }

    private fun shareLink(link: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = PLAIN_TEXT_SHARE_TYPE
        intent.putExtra(Intent.EXTRA_TEXT, link)
        startActivity(Intent.createChooser(intent, getString(R.string.context_get_link)))
    }

    fun setLink() {
        node = megaApi.getNodeByHandle(node.handle)
        getLinkFragment?.updateLink()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SEND_LINK) {
            data?.putExtra(EXTRA_LINK, node.publicLink)
            ChatController(this).checkIntentToShareSomething(data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_get_link, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish();
            }
            R.id.action_share -> {
                shareLink(node.publicLink)
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
}