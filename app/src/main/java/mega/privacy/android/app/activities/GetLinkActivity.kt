package mega.privacy.android.app.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View.GONE
import androidx.core.content.ContextCompat
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.GetLinkActivityLayoutBinding
import mega.privacy.android.app.fragments.getLinkFragments.CopyrightFragment
import mega.privacy.android.app.fragments.getLinkFragments.GetLinkFragment
import mega.privacy.android.app.interfaces.GetLinkInterface
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.COPIED_TEXT_LABEL
import mega.privacy.android.app.utils.Constants.HANDLE
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode

class GetLinkActivity: BaseActivity(), GetLinkInterface {
    companion object {
        const val GET_LINK_FRAGMENT = 0
        const val COPYRIGHT_FRAGMENT = 1
    }

    private lateinit var binding: GetLinkActivityLayoutBinding

    private var getLinkFragment: GetLinkFragment? = null
    private var copyrightFragment: CopyrightFragment? = null

    private lateinit var node: MegaNode
    private lateinit var link: String

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

        val visibleFragment =
            if (dbH.showCopyright.toBoolean() && (megaApi.publicLinks == null || megaApi.publicLinks.size == 0)) COPYRIGHT_FRAGMENT
            else GET_LINK_FRAGMENT

        showFragment(visibleFragment)
    }

    fun showSnackbar(message: String) {
        showSnackbar(binding.getLinkCoordinatorLayout, message)
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

    override fun shareLink(link: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = Constants.PLAIN_TEXT_SHARE_TYPE
        intent.putExtra(Intent.EXTRA_TEXT, link)
        startActivity(Intent.createChooser(intent, getString(R.string.context_get_link)))
    }

    override fun copyLink(link: String) {
        val clipManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(COPIED_TEXT_LABEL, link)
        clipManager.setPrimaryClip(clip)
        showSnackbar(getString(R.string.file_properties_get_link))
    }

    override fun startSetPassword() {

    }

    override fun setLink(link: String) {
        this.link = link
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item)
    }
}