package mega.privacy.android.app.getLink

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.databinding.GetLinkActivityLayoutBinding
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.lollipop.megachat.ChatExplorerActivity
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util.isDarkMode
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import java.util.*

class GetLinkActivity : PasscodeActivity(), SnackbarShower {
    companion object {
        const val SHARE = 0
        const val SEND_TO_CHAT = 1

        private const val TYPE_NODE = 1
        private const val TYPE_LIST = 2

        private const val VIEW_TYPE = "VIEW_TYPE"
    }

    private val viewModelNode: GetLinkViewModel by viewModels()
    private val viewModelList: GetSeveralLinksViewModel by viewModels()

    private lateinit var binding: GetLinkActivityLayoutBinding
    private lateinit var navController: NavController

    private var menu: Menu? = null

    private val transparentColor by lazy {
        ContextCompat.getColor(
            this,
            android.R.color.transparent
        )
    }
    private val elevation by lazy { resources.getDimension(R.dimen.toolbar_elevation) }
    private val toolbarElevationColor by lazy { getColorForElevation(this, elevation) }

    private var viewType = INVALID_VALUE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GetLinkActivityLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent == null || shouldRefreshSessionDueToSDK()) {
            return
        }

        if (savedInstanceState != null) {
            viewType = savedInstanceState.getInt(VIEW_TYPE, INVALID_VALUE)
        }

        if (viewType == INVALID_VALUE) {
            handleIntent()
        }

        setupView()
        setupObservers()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(VIEW_TYPE, viewType)
        super.onSaveInstanceState(outState)
    }

    private fun handleIntent() {
        val handle = intent.getLongExtra(HANDLE, INVALID_HANDLE)
        val handleList = intent.getLongArrayExtra(HANDLE_LIST)

        if (handle == INVALID_HANDLE && handleList == null) {
            logError("No extras to manage.")
            finish()
            return
        }

        if (handle != INVALID_HANDLE) {
            viewModelNode.initNode(handle)
            viewType = TYPE_NODE
        } else if (handleList != null) {
            viewModelList.initNodes(handleList)
            viewType = TYPE_LIST
        }
    }

    private fun setupView() {
        setSupportActionBar(binding.toolbarGetLink)
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        setupNavController()
    }

    private fun setupObservers() {
        viewModelNode.checkElevation().observe(this, ::changeElevation)
    }

    private fun setupNavController() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navHostFragment.navController.graph =
            navHostFragment.navController.navInflater.inflate(
                if (viewType == TYPE_LIST) R.navigation.get_several_links
                else R.navigation.get_link
            )

        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, _, _ ->
            when (navController.currentDestination?.id) {
                R.id.main_get_link -> {
                    supportActionBar?.apply {
                        title = viewModelNode.getLinkFragmentTitle()
                        if (!isShowing) show()
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
                        if (viewModelNode.getPasswordText()
                                .isNullOrEmpty()
                        ) R.string.set_password_protection_dialog
                        else R.string.reset_password_label
                    ).toUpperCase(Locale.getDefault())
                }
                R.id.main_get_several_links -> {
                    viewModelNode.setElevation(true)
                    supportActionBar?.apply {
                        title = StringResourcesUtils.getString(R.string.title_get_links)
                        if (!isShowing) show()
                    }
                }
            }

            refreshMenuOptionsVisibility()
        }

        if (viewModelNode.shouldShowCopyright()) {
            navController.navigate(R.id.show_copyright)
        }
    }


    /**
     * Changes the ActionBar elevation depending on the withElevation value received.
     *
     * @param withElevation True if should set elevation, false otherwise.
     */
    private fun changeElevation(withElevation: Boolean) {
        val isDark = isDarkMode(this)
        val darkAndElevation = withElevation && isDark

        if (darkAndElevation) {
            ColorUtils.changeStatusBarColorForElevation(this, true)
        } else {
            window?.statusBarColor = transparentColor
        }

        binding.toolbarGetLink.setBackgroundColor(
            if (darkAndElevation) toolbarElevationColor else transparentColor
        )

        binding.appBarGetLink.elevation = if (withElevation && !isDark) elevation else 0F
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
                if (viewModelNode.isPasswordSet()) R.string.share_password_warning
                else R.string.share_key_warning
            ) + "\n"
        )
            .setCancelable(false)
            .setPositiveButton(
                if (viewModelNode.isPasswordSet()) R.string.button_share_password
                else R.string.button_share_key
            ) { _, _ ->
                if (type == SHARE) {
                    viewModelNode.shareLinkAndKeyOrPassword { intent -> startActivity(intent) }
                } else if (type == SEND_TO_CHAT) {
                    viewModelNode.sendLinkToChat(data, true) { intent ->
                        handleActivityResult(intent)
                    }
                }
            }
            .setNegativeButton(R.string.general_dismiss) { _, _ ->
                if (type == SHARE) {
                    viewModelNode.shareCompleteLink { intent -> startActivity(intent) }
                } else if (type == SEND_TO_CHAT) {
                    viewModelNode.sendLinkToChat(data, false) { intent ->
                        handleActivityResult(intent)
                    }
                }
            }

        shareKeyDialogBuilder.create().show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SEND_LINK) {
            when {
                viewType == TYPE_LIST -> handleActivityResult(
                    data?.putExtra(
                        EXTRA_SEVERAL_LINKS,
                        viewModelList.getLinksString()
                    )
                )

                viewModelNode.shouldShowShareKeyOrPasswordDialog() -> showShareKeyOrPasswordDialog(
                    SEND_TO_CHAT,
                    data
                )
                else -> viewModelNode.sendToChat(
                    data,
                    shouldAttachKeyOrPassword = false
                ) { intent -> handleActivityResult(intent) }
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
            R.id.main_get_link, R.id.main_get_several_links ->
                menu.toggleAllMenuItemsVisibility(true)
            else -> menu.toggleAllMenuItemsVisibility(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.action_share -> {
                when {
                    viewType == TYPE_LIST -> startActivity(
                        Intent(Intent.ACTION_SEND)
                            .setType(PLAIN_TEXT_SHARE_TYPE)
                            .putExtra(Intent.EXTRA_TEXT, viewModelList.getLinksString())
                    )
                    viewModelNode.shouldShowShareKeyOrPasswordDialog() -> showShareKeyOrPasswordDialog(
                        SHARE,
                        null
                    )
                    else -> viewModelNode.shareLink { intent -> startActivity(intent) }
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
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, binding.getLinkCoordinatorLayout, content, chatId)
    }
}
