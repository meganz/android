package mega.privacy.android.app.getLink

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.databinding.GetLinkActivityLayoutBinding
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util.isDarkMode
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import java.util.*

/**
 * Activity which allows create and manage a link of a node
 * @see[GetLinkFragment], [CopyrightFragment], [DecryptionKeyFragment], [LinkPasswordFragment].
 *
 * Or the creation of multiple links @see[GetSeveralLinksFragment].
 */
class GetLinkActivity : PasscodeActivity(), SnackbarShower {
    companion object {
        private const val TYPE_NODE = 1
        private const val TYPE_LIST = 2

        private const val VIEW_TYPE = "VIEW_TYPE"
    }

    private val viewModelNode: GetLinkViewModel by viewModels()
    private val viewModelList: GetSeveralLinksViewModel by viewModels()

    private lateinit var binding: GetLinkActivityLayoutBinding
    private lateinit var navController: NavController

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

    /**
     * Gets all the info from the Intent.
     */
    private fun handleIntent() {
        val handle = intent.getLongExtra(HANDLE, INVALID_HANDLE)
        val handleList = intent.getLongArrayExtra(HANDLE_LIST)

        if (handle == INVALID_HANDLE && handleList == null) {
            Timber.e("No extras to manage.")
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
                }
                R.id.password -> {
                    supportActionBar?.title = StringResourcesUtils.getString(
                        if (viewModelNode.getPasswordText()
                                .isNullOrEmpty()
                        ) R.string.set_password_protection_dialog
                        else R.string.reset_password_label
                    )
                }
                R.id.main_get_several_links -> {
                    viewModelNode.setElevation(true)
                    supportActionBar?.apply {
                        title = StringResourcesUtils.getQuantityString(
                            R.plurals.get_links,
                            viewModelList.getLinksNumber()
                        )

                        if (!isShowing) show()
                    }
                }
            }
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (psaWebBrowser != null && psaWebBrowser?.consumeBack() == true) return

        if (!navController.navigateUp()) {
            finish()
        }
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, binding.getLinkCoordinatorLayout, content, chatId)
    }
}
