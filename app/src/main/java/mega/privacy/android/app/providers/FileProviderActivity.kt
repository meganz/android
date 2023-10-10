package mega.privacy.android.app.providers

import android.Manifest
import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.StatFs
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity.Companion.showSimpleSnackbar
import mega.privacy.android.app.DownloadService
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.components.CustomViewPager
import mega.privacy.android.app.components.EditTextPIN
import mega.privacy.android.app.main.providers.CloudDriveProviderFragment
import mega.privacy.android.app.main.providers.IncomingSharesProviderFragment
import mega.privacy.android.app.main.providers.ProviderPageAdapter
import mega.privacy.android.app.presentation.provider.FileProviderViewModel
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ColorUtils.changeStatusBarColorForElevation
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.ColorUtils.setStatusBarTextColor
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ConstantsUrl.RECOVERY_URL
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaNodeUtil.cloudRootHandle
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.checkNotificationsPermission
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.user.UserCredentials
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaEvent
import nz.mega.sdk.MegaGlobalListenerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransferListenerInterface
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import org.jetbrains.anko.displayMetrics
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * This activity is launched by 3rd apps, for example, when compose email pick attachments from MEGA.
 *
 * @property megaApi
 * @property megaApiFolder
 * @property megaChatApi
 * @property dbH
 * @property tabShown
 * @property incomingDeepBrowserTree
 * @property parentHandle
 * @property incParentHandle
 */
@SuppressLint("NewApi")
@AndroidEntryPoint
class FileProviderActivity : PasscodeFileProviderActivity(), MegaRequestListenerInterface,
    MegaGlobalListenerInterface, MegaTransferListenerInterface {

    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    @Inject
    @MegaApiFolder
    lateinit var megaApiFolder: MegaApiAndroid

    @Inject
    lateinit var megaChatApi: MegaChatApiAndroid

    @Inject
    lateinit var dbH: DatabaseHandler

    private val viewModel by viewModels<FileProviderViewModel>()

    private var lastEmail: String? = null
    private var lastPassword: String? = null
    private var searchMenuItem: MenuItem? = null
    private var timer: CountDownTimer? = null
    private var tB: MaterialToolbar? = null
    private var aB: ActionBar? = null
    private var aBL: AppBarLayout? = null
    private var scrollView: ScrollView? = null
    private var loginLayout: LinearLayout? = null
    private var loginCreateAccount: LinearLayout? = null
    private var loginLoggingIn: LinearLayout? = null
    private var queryingSignupLinkText: TextView? = null
    private var confirmingAccountText: TextView? = null
    private var loginProgressBar: ProgressBar? = null
    private var loginFetchNodesProgressBar: ProgressBar? = null
    private var loggingInText: TextView? = null
    private var fetchingNodesText: TextView? = null
    private var prepareNodesText: TextView? = null
    private var serversBusyText: TextView? = null
    private var loginTitle: TextView? = null
    private var generatingKeysText: TextView? = null
    private var etUser: AppCompatEditText? = null
    private var etPasswordLayout: TextInputLayout? = null
    private var etPassword: AppCompatEditText? = null
    private var bRegisterLol: TextView? = null
    private var bLoginLol: Button? = null

    private var folderSelected = false
    var tabShown = INVALID_TAB
        private set
    private var cloudDriveProviderFragment: CloudDriveProviderFragment? = null
    private var incomingSharesProviderFragment: IncomingSharesProviderFragment? = null
    private var statusDialog: AlertDialog? = null
    private var cancelButton: Button? = null
    private var attachButton: Button? = null
    private var tabLayoutProvider: TabLayout? = null
    private var mTabsAdapterProvider: ProviderPageAdapter? = null
    private var viewPagerProvider: CustomViewPager? = null
    private var nodes: ArrayList<MegaNode>? = null

    @JvmField
    var incomingDeepBrowserTree = -1
    var parentHandle = INVALID_HANDLE

    @JvmField
    var incParentHandle = INVALID_HANDLE
    private var selectedNodes: List<MegaNode>? = null
    private var totalTransfers = 0
    private var progressTransfersFinish = 0
    private var clipDataTransfers: ClipData? = null
    private val contentUris = ArrayList<Uri>()
    private var loginVerificationLayout: LinearLayout? = null
    private var imm: InputMethodManager? = null
    private var firstPin: EditTextPIN? = null
    private var secondPin: EditTextPIN? = null
    private var thirdPin: EditTextPIN? = null
    private var fourthPin: EditTextPIN? = null
    private var fifthPin: EditTextPIN? = null
    private var sixthPin: EditTextPIN? = null
    private val sb = StringBuilder()
    private var pin: String? = null
    private var pinError: TextView? = null
    private var lostYourDeviceButton: RelativeLayout? = null
    private var verify2faProgressBar: ProgressBar? = null
    private val isFirstTime = true
    private var isErrorShown = false
    private var is2FAEnabled = false
    private var pinLongClick = false
    private var fileProviderActivity: FileProviderActivity? = null
    private var gSession: String? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Timber.d("tabShown: %s", tabShown)
            if (tabShown == CLOUD_TAB) {
                cloudDriveProviderFragment = supportFragmentManager.findFragmentByTag(
                    getFragmentTag(
                        R.id.provider_tabs_pager,
                        CLOUD_TAB
                    )
                ) as CloudDriveProviderFragment?
                if (cloudDriveProviderFragment != null) {
                    if (cloudDriveProviderFragment?.onBackPressed() == 0) {
                        finish()
                        return
                    }
                }
            } else if (tabShown == INCOMING_TAB) {
                incomingSharesProviderFragment = supportFragmentManager.findFragmentByTag(
                    getFragmentTag(
                        R.id.provider_tabs_pager, INCOMING_TAB
                    )
                ) as IncomingSharesProviderFragment?
                if (incomingSharesProviderFragment != null) {
                    if (incomingSharesProviderFragment?.onBackPressed() == 0) {
                        finish()
                        return
                    }
                }
            } else {
                MegaApplication.isLoggingIn = false
                finish()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_MENU) {
            // do nothing
            true
        } else super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate first")
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        setStatusBarTextColor(this@FileProviderActivity)
        fileProviderActivity = this@FileProviderActivity
        is2FAEnabled = false

        if (savedInstanceState != null) {
            folderSelected = savedInstanceState.getBoolean("folderSelected", false)
            incParentHandle =
                savedInstanceState.getLong("incParentHandle", INVALID_HANDLE)
            parentHandle = savedInstanceState.getLong("parentHandle", INVALID_HANDLE)
            incomingDeepBrowserTree = savedInstanceState.getInt("deepBrowserTree", -1)
            tabShown = savedInstanceState.getInt("tabShown", CLOUD_TAB)
        }

        megaApi.addGlobalListener(this@FileProviderActivity)
        megaApi.addTransferListener(this@FileProviderActivity)
        checkLogin()

        val credentials = dbH.credentials
        if (credentials == null) {
            loginLayout?.visibility = View.VISIBLE
            loginCreateAccount?.visibility = View.INVISIBLE
            loginLoggingIn?.visibility = View.GONE
            generatingKeysText?.visibility = View.GONE
            loggingInText?.visibility = View.GONE
            fetchingNodesText?.visibility = View.GONE
            prepareNodesText?.visibility = View.GONE
            if (serversBusyText != null) {
                serversBusyText?.visibility = View.GONE
            }
            loginProgressBar?.visibility = View.GONE
            queryingSignupLinkText?.visibility = View.GONE
            confirmingAccountText?.visibility = View.GONE
        } else {
            Timber.d("dbH.getCredentials() NOT null")
            if (megaApi.rootNode == null) {
                Timber.d("megaApi.getRootNode() == null")
                lastEmail = credentials.email
                val gSession = credentials.session
                if (!MegaApplication.isLoggingIn) {
                    MegaApplication.isLoggingIn = true
                    loginLayout?.visibility = View.GONE
                    loginCreateAccount?.visibility = View.GONE
                    queryingSignupLinkText?.visibility = View.GONE
                    confirmingAccountText?.visibility = View.GONE
                    loginLoggingIn?.visibility = View.VISIBLE
                    loginProgressBar?.visibility = View.VISIBLE
                    loginFetchNodesProgressBar?.visibility = View.GONE
                    loggingInText?.visibility = View.VISIBLE
                    fetchingNodesText?.visibility = View.GONE
                    prepareNodesText?.visibility = View.GONE
                    if (serversBusyText != null) {
                        serversBusyText?.visibility = View.GONE
                    }
                    ChatUtil.initMegaChatApi(gSession)
                    megaApi.fastLogin(gSession, this@FileProviderActivity)
                }
            } else {
                setContentView(R.layout.activity_file_provider)
                Timber.d("megaApi.getRootNode() NOT null")
                aBL = findViewById(R.id.app_bar_layout_provider)

                //Set toolbar
                tB = findViewById(R.id.toolbar_provider)
                setSupportActionBar(tB)
                aB = supportActionBar?.apply {
                    setDisplayHomeAsUpEnabled(true)
                    setDisplayShowHomeEnabled(true)
                }

                cancelButton = findViewById<Button?>(R.id.cancel_button).apply {
                    setOnClickListener { finish() }
                    text = getString(R.string.general_cancel)
                    val cancelButtonParams = layoutParams as LinearLayout.LayoutParams
                    cancelButtonParams.setMargins(Util.scaleWidthPx(10, displayMetrics), 0, 0, 0)
                    layoutParams = cancelButtonParams
                }

                //Left and Right margin

                attachButton = findViewById<Button?>(R.id.attach_button).apply {
                    setOnClickListener { onAttachClicked() }
                    text = getString(R.string.general_attach)
                }

                activateButton(false)

                //TABS section
                tabLayoutProvider = findViewById(R.id.sliding_tabs_provider)
                viewPagerProvider = findViewById<CustomViewPager?>(R.id.provider_tabs_pager).apply {
                    if (mTabsAdapterProvider == null) {
                        Timber.d("mTabsAdapterProvider == null")
                        Timber.d("tabShown: %s", tabShown)
                        Timber.d("parentHandle INCOMING: %s", incParentHandle)
                        Timber.d("parentHandle CLOUD: %s", parentHandle)
                        currentItem = tabShown
                        if (tabShown == INVALID_TAB) {
                            tabShown = CLOUD_TAB
                        }
                        mTabsAdapterProvider =
                            ProviderPageAdapter(supportFragmentManager, this@FileProviderActivity)
                        adapter = mTabsAdapterProvider
                        tabLayoutProvider?.setupWithViewPager(this)
                        currentItem = tabShown
                    } else {
                        Timber.d("mTabsAdapterProvider NOT null")
                        Timber.d("tabShown: %s", tabShown)
                        Timber.d("parentHandle INCOMING: %s", incParentHandle)
                        Timber.d("parentHandle CLOUD: %s", parentHandle)
                        currentItem = tabShown
                        if (tabShown == INVALID_TAB) {
                            tabShown = CLOUD_TAB
                        }
                    }
                    addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                        override fun onPageScrollStateChanged(state: Int) {}
                        override fun onPageScrolled(
                            position: Int,
                            positionOffset: Float,
                            positionOffsetPixels: Int,
                        ) {
                        }

                        override fun onPageSelected(position: Int) {
                            Timber.d("onTabChanged TabId :%s", position)
                            if (position == CLOUD_TAB) {
                                tabShown = CLOUD_TAB
                                cloudDriveProviderFragment =
                                    supportFragmentManager.findFragmentByTag(
                                        getFragmentTag(
                                            R.id.provider_tabs_pager, CLOUD_TAB
                                        )
                                    ) as CloudDriveProviderFragment?
                                if (cloudDriveProviderFragment != null) {
                                    if (cloudDriveProviderFragment?.parentHandle == INVALID_HANDLE || cloudDriveProviderFragment?.parentHandle == megaApi.rootNode?.handle) {
                                        aB?.setTitle(getString(R.string.file_provider_title))
                                    } else {
                                        aB?.setTitle(
                                            megaApi.getNodeByHandle(
                                                cloudDriveProviderFragment?.parentHandle
                                                    ?: INVALID_HANDLE
                                            )?.name
                                        )
                                    }
                                }
                            } else if (position == INCOMING_TAB) {
                                tabShown = INCOMING_TAB
                                incomingSharesProviderFragment =
                                    supportFragmentManager.findFragmentByTag(
                                        getFragmentTag(
                                            R.id.provider_tabs_pager, INCOMING_TAB
                                        )
                                    ) as IncomingSharesProviderFragment?
                                if (incomingSharesProviderFragment != null) {
                                    if (incomingSharesProviderFragment?.deepBrowserTree == 0) {
                                        aB?.setTitle(getString(R.string.file_provider_title))
                                    } else {
                                        aB?.setTitle(
                                            megaApi.getNodeByHandle(
                                                incomingSharesProviderFragment?.parentHandle
                                                    ?: INVALID_HANDLE
                                            )?.name
                                        )
                                    }
                                }
                            }
                        }
                    })
                }
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                )
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                )
            }
        }
    }

    private fun onAttachClicked() {
        val temp: AlertDialog
        try {
            temp = createProgressDialog(
                this@FileProviderActivity,
                getString(R.string.context_preparing_provider)
            )
            temp.show()
        } catch (e: Exception) {
            return
        }

        statusDialog = temp
        progressTransfersFinish = 0
        clipDataTransfers = null
        selectedNodes?.let {
            var hashes = LongArray(it.size)
            val totalHashes = ArrayList<Long>()
            for (i in it.indices) {
                hashes[i] = it[i].handle
                getTotalTransfers(it[i], totalHashes)
            }
            hashes = LongArray(totalTransfers)
            for (i in totalHashes.indices) {
                hashes[i] = totalHashes[i]
            }
            downloadAndAttach(hashes)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("NewApi")
    private fun checkLogin() {
        setContentView(R.layout.fragment_login)
        scrollView = findViewById(R.id.scroll_view_login)
        loginTitle = findViewById<TextView?>(R.id.login_text_view).apply {
            text = getString(R.string.login_to_mega)
        }
        etUser = findViewById(R.id.login_email_text)
        etPasswordLayout = findViewById(R.id.login_password_text_layout)
        etPassword = findViewById<AppCompatEditText?>(R.id.login_password_text).apply {
            setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submitForm()
                    return@setOnEditorActionListener true
                }
                false
            }
            onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                Util.setPasswordToggle(
                    etPasswordLayout,
                    hasFocus
                )
            }
        }
        bLoginLol = findViewById<Button?>(R.id.button_login).apply {
            text = getString(R.string.login_text)
            setOnClickListener { submitForm() }
        }
        loginCreateAccount = findViewById<LinearLayout?>(R.id.login_create_account_layout).apply {
            visibility = View.INVISIBLE
        }

        bRegisterLol = findViewById<TextView?>(R.id.button_create_account_login).apply {
            text = getString(R.string.create_account)
        }

        loginLayout = findViewById(R.id.login_layout)
        loginLoggingIn = findViewById(R.id.login_logging_in_layout)
        loginProgressBar = findViewById(R.id.login_progress_bar)
        loginFetchNodesProgressBar = findViewById(R.id.login_fetching_nodes_bar)
        generatingKeysText = findViewById(R.id.login_generating_keys_text)
        queryingSignupLinkText = findViewById(R.id.login_query_signup_link_text)
        confirmingAccountText = findViewById(R.id.login_confirm_account_text)
        loggingInText = findViewById(R.id.login_logging_in_text)
        fetchingNodesText = findViewById(R.id.login_fetch_nodes_text)
        prepareNodesText = findViewById(R.id.login_prepare_nodes_text)
        serversBusyText = findViewById(R.id.login_servers_busy_text)
        tB = findViewById(R.id.toolbar_login)
        loginVerificationLayout = findViewById<LinearLayout?>(R.id.login_2fa).apply {
            isVisible = false
        }
        lostYourDeviceButton =
            findViewById<RelativeLayout?>(R.id.lost_authentication_device).apply {
                setOnClickListener {
                    try {
                        val openTermsIntent =
                            Intent(this@FileProviderActivity, WebViewActivity::class.java)
                        openTermsIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        openTermsIntent.data = Uri.parse(RECOVERY_URL)
                        startActivity(openTermsIntent)
                    } catch (e: Exception) {
                        val viewIntent = Intent(Intent.ACTION_VIEW)
                        viewIntent.data = Uri.parse(RECOVERY_URL)
                        startActivity(viewIntent)
                    }
                }
            }
        pinError = findViewById<TextView?>(R.id.pin_2fa_error_login).apply {
            isVisible = false
        }
        verify2faProgressBar = findViewById(R.id.progressbar_verify_2fa)
        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        firstPin = findViewById<EditTextPIN?>(R.id.pin_first_login).apply {
            setOnLongClickListener {
                pinLongClick = true
                requestFocus()
            }
            onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (hasFocus) {
                    setText("")
                }
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (length() != 0) {
                        secondPin?.requestFocus()
                        secondPin?.isCursorVisible = true
                        if (isFirstTime && !pinLongClick) {
                            secondPin?.setText("")
                            thirdPin?.setText("")
                            fourthPin?.setText("")
                            fifthPin?.setText("")
                            sixthPin?.setText("")
                        } else if (pinLongClick) {
                            pasteClipboard()
                        } else {
                            permitVerify()
                        }
                    } else {
                        if (isErrorShown) {
                            verifyQuitError()
                        }
                        permitVerify()
                    }
                }
            })
        }
        secondPin = findViewById<EditTextPIN?>(R.id.pin_second_login).apply {
            setOnLongClickListener {
                pinLongClick = true
                requestFocus()
            }
            onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (hasFocus) {
                    setText("")
                }
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (length() != 0) {
                        thirdPin?.requestFocus()
                        thirdPin?.isCursorVisible = true
                        if (isFirstTime && !pinLongClick) {
                            thirdPin?.setText("")
                            fourthPin?.setText("")
                            fifthPin?.setText("")
                            sixthPin?.setText("")
                        } else if (pinLongClick) {
                            pasteClipboard()
                        } else {
                            permitVerify()
                        }
                    } else {
                        if (isErrorShown) {
                            verifyQuitError()
                        }
                        permitVerify()
                    }
                }
            })
        }
        thirdPin = findViewById<EditTextPIN?>(R.id.pin_third_login).apply {
            setOnLongClickListener {
                pinLongClick = true
                requestFocus()
            }
            onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (hasFocus) {
                    setText("")
                }
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (length() != 0) {
                        fourthPin?.requestFocus()
                        fourthPin?.isCursorVisible = true
                        if (isFirstTime && !pinLongClick) {
                            fourthPin?.setText("")
                            fifthPin?.setText("")
                            sixthPin?.setText("")
                        } else if (pinLongClick) {
                            pasteClipboard()
                        } else {
                            permitVerify()
                        }
                    } else {
                        if (isErrorShown) {
                            verifyQuitError()
                        }
                    }
                }
            })
        }
        fourthPin = findViewById<EditTextPIN?>(R.id.pin_fourth_login).apply {
            setOnLongClickListener {
                pinLongClick = true
                requestFocus()
            }
            onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (hasFocus) {
                    setText("")
                }
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (length() != 0) {
                        fifthPin?.requestFocus()
                        fifthPin?.isCursorVisible = true
                        if (isFirstTime && !pinLongClick) {
                            fifthPin?.setText("")
                            sixthPin?.setText("")
                        } else if (pinLongClick) {
                            pasteClipboard()
                        } else {
                            permitVerify()
                        }
                    } else {
                        if (isErrorShown) {
                            verifyQuitError()
                        }
                    }
                }
            })
        }
        fifthPin = findViewById<EditTextPIN?>(R.id.pin_fifth_login).apply {
            setOnLongClickListener {
                pinLongClick = true
                requestFocus()
            }
            onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (hasFocus) {
                    setText("")
                }
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (length() != 0) {
                        sixthPin?.requestFocus()
                        sixthPin?.isCursorVisible = true
                        if (isFirstTime && !pinLongClick) {
                            sixthPin?.setText("")
                        } else if (pinLongClick) {
                            pasteClipboard()
                        } else {
                            permitVerify()
                        }
                    } else {
                        if (isErrorShown) {
                            verifyQuitError()
                        }
                    }
                }
            })
        }
        sixthPin = findViewById<EditTextPIN?>(R.id.pin_sixth_login).apply {
            setOnLongClickListener {
                pinLongClick = true
                requestFocus()
            }
            onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (hasFocus) {
                    setText("")
                }
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (length() != 0) {
                        isCursorVisible = true
                        Util.hideKeyboard(fileProviderActivity, 0)
                        if (pinLongClick) {
                            pasteClipboard()
                        } else {
                            permitVerify()
                        }
                    } else {
                        if (isErrorShown) {
                            verifyQuitError()
                        }
                    }
                }
            })
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        secondPin?.previousDigitEditText = firstPin
        thirdPin?.previousDigitEditText = secondPin
        fourthPin?.previousDigitEditText = thirdPin
        fifthPin?.previousDigitEditText = fourthPin
        sixthPin?.previousDigitEditText = fifthPin
    }

    private fun permitVerify() {
        Timber.d("permitVerify")
        if (firstPin?.length() == 1 && secondPin?.length() == 1 && thirdPin?.length() == 1 && fourthPin?.length() == 1 && fifthPin?.length() == 1 && sixthPin?.length() == 1) {
            Util.hideKeyboard(this@FileProviderActivity, 0)
            if (sb.isNotEmpty()) {
                sb.delete(0, sb.length)
            }
            sb.append(firstPin?.text)
            sb.append(secondPin?.text)
            sb.append(thirdPin?.text)
            sb.append(fourthPin?.text)
            sb.append(fifthPin?.text)
            sb.append(sixthPin?.text)
            pin = sb.toString()
            Timber.d("PIN: %s", pin)
            if (!isErrorShown && pin != null) {
                verify2faProgressBar?.visibility = View.VISIBLE
                Timber.d("lastEmail: %s lastPasswd: %s", lastEmail, lastPassword)
                megaApi.multiFactorAuthLogin(
                    lastEmail,
                    lastPassword,
                    pin,
                    this@FileProviderActivity
                )
            }
        }
    }

    private fun pasteClipboard() {
        Timber.d("pasteClipboard")
        pinLongClick = false
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        if (clipData != null) {
            val code = clipData.getItemAt(0).text.toString()
            Timber.d("Code: %s", code)
            if (code.length == 6) {
                var areDigits = true
                for (i in 0..5) {
                    if (!Character.isDigit(code[i])) {
                        areDigits = false
                        break
                    }
                }
                if (areDigits) {
                    firstPin?.setText(code[0].toString())
                    secondPin?.setText(code[1].toString())
                    thirdPin?.setText(code[2].toString())
                    fourthPin?.setText(code[3].toString())
                    fifthPin?.setText(code[4].toString())
                    sixthPin?.setText(code[5].toString())
                } else {
                    firstPin?.setText("")
                    secondPin?.setText("")
                    thirdPin?.setText("")
                    fourthPin?.setText("")
                    fifthPin?.setText("")
                    sixthPin?.setText("")
                }
            }
        }
    }

    private fun verifyQuitError() {
        isErrorShown = false
        pinError?.visibility = View.GONE
        firstPin?.setTextColor(
            ContextCompat.getColor(
                this@FileProviderActivity,
                R.color.grey_087_white_087
            )
        )
        secondPin?.setTextColor(
            ContextCompat.getColor(
                this@FileProviderActivity,
                R.color.grey_087_white_087
            )
        )
        thirdPin?.setTextColor(
            ContextCompat.getColor(
                this@FileProviderActivity,
                R.color.grey_087_white_087
            )
        )
        fourthPin?.setTextColor(
            ContextCompat.getColor(
                this@FileProviderActivity,
                R.color.grey_087_white_087
            )
        )
        fifthPin?.setTextColor(
            ContextCompat.getColor(
                this@FileProviderActivity,
                R.color.grey_087_white_087
            )
        )
        sixthPin?.setTextColor(
            ContextCompat.getColor(
                this@FileProviderActivity,
                R.color.grey_087_white_087
            )
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Timber.d("onCreateOptionsMenu")

        // Inflate the menu items for use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.activity_fileprovider, menu)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.action_search)
        val searchView = searchMenuItem?.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setIconifiedByDefault(true)
        searchMenuItem?.isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Change title
     *
     * @param title
     */
    fun changeTitle(title: String?) {
        aB?.title = title
    }

    private fun getFragmentTag(viewPagerId: Int, fragmentPosition: Int): String {
        return "android:switcher:$viewPagerId:$fragmentPosition"
    }

    /**
     * Download and attach after click
     *
     * @param hash Node handle.
     */
    fun downloadAndAttachAfterClick(hash: Long) {
        val temp: AlertDialog
        try {
            temp = createProgressDialog(
                this@FileProviderActivity,
                getString(R.string.context_preparing_provider)
            )
            temp.show()
        } catch (e: Exception) {
            return
        }
        statusDialog = temp
        progressTransfersFinish = 0
        clipDataTransfers = null
        downloadAndAttach(longArrayOf(hash))
    }

    private fun downloadAndAttach(hashes: LongArray?) {
        if (intent != null && intent.getBooleanExtra(FROM_MEGA_APP, false)) {
            dismissAlertDialogIfExists(statusDialog)
            setResult(
                RESULT_OK, Intent()
                    .setAction(Intent.ACTION_GET_CONTENT)
                    .putExtra(FROM_MEGA_APP, true)
                    .putExtra(Constants.NODE_HANDLES, hashes)
            )
            finish()
            return
        }
        Timber.d("downloadAndAttach")
        val hasStoragePermission =
            hasPermissions(this@FileProviderActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (!hasStoragePermission) {
            requestPermission(
                this@FileProviderActivity,
                Constants.REQUEST_WRITE_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        if (viewModel.getStorageState() === StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            return
        }
        val destination: File? = cacheDir
        val pathToDownload = destination?.path
        var availableFreeSpace = Double.MAX_VALUE
        try {
            val stat = StatFs(destination?.path)
            availableFreeSpace = stat.availableBlocksLong.toDouble() * stat.blockSizeLong.toDouble()
        } catch (ex: Exception) {
            Timber.w("Exception: $ex")
        }
        val dlFiles: MutableMap<MegaNode, String> = HashMap()
        if (hashes != null && hashes.isNotEmpty()) {
            for (hash in hashes) {
                val tempNode = megaApi.getNodeByHandle(hash)
                // If node doesn't exist continue to the next one
                if (tempNode == null) {
                    Timber.w("Temp node is null")
                    continue
                }
                val localPath = FileUtil.getLocalFile(tempNode)
                if (localPath != null) {
                    try {
                        Timber.d("COPY_FILE")
                        val fileToShare = File(pathToDownload, tempNode.name)
                        FileUtil.copyFile(File(localPath), fileToShare)
                        if (fileToShare.exists()) {
                            val contentUri = FileProvider.getUriForFile(
                                this@FileProviderActivity,
                                Constants.AUTHORITY_STRING_FILE_PROVIDER,
                                fileToShare
                            )
                            grantUriPermission(
                                "*",
                                contentUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            Timber.d("CONTENT URI: %s", contentUri)
                            if (totalTransfers == 0) {
                                val result = Intent()
                                result.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                result.data = contentUri
                                result.action = Intent.ACTION_GET_CONTENT
                                if (parent == null) {
                                    setResult(RESULT_OK, result)
                                } else {
                                    parent.setResult(RESULT_OK, result)
                                }
                                finish()
                            } else {
                                contentUris.add(contentUri)
                                progressTransfersFinish++
                                //Send it
                                if (progressTransfersFinish == totalTransfers) {
                                    val result = Intent()
                                    result.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    if (clipDataTransfers == null) {
                                        clipDataTransfers = ClipData.newUri(
                                            contentResolver, "", contentUris[0]
                                        )
                                    } else {
                                        clipDataTransfers?.addItem(
                                            ClipData.Item(
                                                contentUris[0]
                                            )
                                        )
                                    }
                                    for (i in 1 until contentUris.size) {
                                        clipDataTransfers?.addItem(
                                            ClipData.Item(
                                                contentUris[i]
                                            )
                                        )
                                    }
                                    result.clipData = clipDataTransfers
                                    result.action = Intent.ACTION_GET_CONTENT
                                    if (parent == null) {
                                        setResult(RESULT_OK, result)
                                    } else {
                                        parent.setResult(RESULT_OK, result)
                                    }
                                    totalTransfers = 0
                                    finish()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        finish()
                    }
                }
                dlFiles[tempNode] = pathToDownload.orEmpty()
            }
        }
        if (dlFiles.isNotEmpty()) {
            for (document in dlFiles.keys) {
                val path = dlFiles[document]
                if (availableFreeSpace < document.size) {
                    Util.showErrorAlertDialog(
                        getString(R.string.error_not_enough_free_space) + " (" + document.name + ")",
                        false,
                        this@FileProviderActivity
                    )
                    continue
                }
                checkNotificationsPermission(this@FileProviderActivity)
                val service = Intent(this@FileProviderActivity, DownloadService::class.java)
                service.putExtra(DownloadService.EXTRA_HASH, document.handle)
                service.putExtra(DownloadService.EXTRA_SIZE, document.size)
                service.putExtra(DownloadService.EXTRA_PATH, path)
                service.putExtra(DownloadService.EXTRA_OPEN_FILE, false)
                startService(service)
            }
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        bundle.putBoolean("folderSelected", folderSelected)
        bundle.putInt("tabShown", tabShown)
        bundle.putInt("deepBrowserTree", incomingDeepBrowserTree)
        bundle.putLong("parentHandle", parentHandle)
        bundle.putLong("incParentHandle", incParentHandle)
        bundle.putBoolean("is2FAEnabled", is2FAEnabled)
        bundle.putString("lastEmail", lastEmail)
        bundle.putString("lastPassword", lastPassword)
        super.onSaveInstanceState(bundle)
    }

    private fun getTotalTransfers(n: MegaNode, totalHashes: ArrayList<Long>) {
        val total = 0
        if (n.isFile) {
            totalTransfers++
            totalHashes.add(n.handle)
        } else {
            val nodes = megaApi.getChildren(n)
            for (i in nodes.indices) {
                getTotalTransfers(nodes[i], totalHashes)
            }
            totalTransfers += total
        }
    }

    private val emailError: String?
        get() {
            val value = etUser?.text.toString()
            if (value.isEmpty()) {
                return getString(R.string.error_enter_email)
            }
            return if (!Constants.EMAIL_ADDRESS.matcher(value).matches()) {
                getString(R.string.error_invalid_email)
            } else null
        }
    private val passwordError: String?
        get() {
            val value = etPassword?.text.toString()
            return if (value.isEmpty()) {
                getString(R.string.error_enter_password)
            } else null
        }

    private fun validateForm(): Boolean {
        val emailError = emailError
        val passwordError = passwordError
        etUser?.error = emailError
        etPassword?.error = passwordError
        if (emailError != null) {
            etUser?.requestFocus()
            return false
        } else if (passwordError != null) {
            etPassword?.requestFocus()
            return false
        }
        return true
    }

    private fun submitForm() {
        if (!validateForm()) {
            return
        }
        Util.hideKeyboard(this@FileProviderActivity, 0)
        if (!viewModel.isConnected()) {
            loginLoggingIn?.visibility = View.GONE
            loginLayout?.visibility = View.VISIBLE
            loginCreateAccount?.visibility = View.INVISIBLE
            queryingSignupLinkText?.visibility = View.GONE
            confirmingAccountText?.visibility = View.GONE
            generatingKeysText?.visibility = View.GONE
            loggingInText?.visibility = View.GONE
            fetchingNodesText?.visibility = View.GONE
            prepareNodesText?.visibility = View.GONE
            if (serversBusyText != null) {
                serversBusyText?.visibility = View.GONE
            }
            Util.showErrorAlertDialog(
                getString(R.string.error_server_connection_problem),
                false,
                this@FileProviderActivity
            )
            return
        }
        loginLayout?.visibility = View.GONE
        loginCreateAccount?.visibility = View.GONE
        loginLoggingIn?.visibility = View.VISIBLE
        generatingKeysText?.visibility = View.VISIBLE
        loginProgressBar?.visibility = View.VISIBLE
        loginFetchNodesProgressBar?.visibility = View.GONE
        queryingSignupLinkText?.visibility = View.GONE
        confirmingAccountText?.visibility = View.GONE
        lastEmail = etUser?.text.toString().lowercase().trim { it <= ' ' }
        lastPassword = etPassword?.text.toString()
        Timber.d("Generating keys")
        onKeysGenerated(lastEmail, lastPassword)
    }

    private fun onKeysGenerated(email: String?, password: String?) {
        Timber.d("Key generation finished")
        lastEmail = email
        lastPassword = password
        onKeysGeneratedLogin()
    }

    private fun onKeysGeneratedLogin() {
        if (!viewModel.isConnected()) {
            loginLoggingIn?.visibility = View.GONE
            loginLayout?.visibility = View.VISIBLE
            loginCreateAccount?.visibility = View.INVISIBLE
            queryingSignupLinkText?.visibility = View.GONE
            confirmingAccountText?.visibility = View.GONE
            generatingKeysText?.visibility = View.GONE
            loggingInText?.visibility = View.GONE
            fetchingNodesText?.visibility = View.GONE
            prepareNodesText?.visibility = View.GONE
            if (serversBusyText != null) {
                serversBusyText?.visibility = View.GONE
            }
            Util.showErrorAlertDialog(
                getString(R.string.error_server_connection_problem),
                false,
                this@FileProviderActivity
            )
            return
        }
        if (!MegaApplication.isLoggingIn) {
            MegaApplication.isLoggingIn = true
            loggingInText?.visibility = View.VISIBLE
            fetchingNodesText?.visibility = View.GONE
            prepareNodesText?.visibility = View.GONE
            if (serversBusyText != null) {
                serversBusyText?.visibility = View.GONE
            }
            Timber.d("fastLogin with publicKey and privateKey")

            val ret = megaChatApi.init(null)
            Timber.d("Result of init ---> %s", ret)
            if (ret == MegaChatApi.INIT_WAITING_NEW_SESSION) {
                Timber.d("Start fastLogin: condition ret == MegaChatApi.INIT_WAITING_NEW_SESSION")
                megaApi.login(lastEmail, lastPassword, this@FileProviderActivity)
            } else {
                Timber.e("ERROR INIT CHAT: %s", ret)
                megaChatApi.logout()
            }
        }
    }

    private fun showAB(tB: MaterialToolbar?) {
        setSupportActionBar(tB)
        aB = supportActionBar
        aB?.show()
        aB?.setHomeButtonEnabled(true)
        aB?.setDisplayShowHomeEnabled(true)
        aB?.setDisplayHomeAsUpEnabled(true)
    }

    private fun hideAB() {
        aB?.hide()
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestStart")
    }

    override fun onRequestFinish(
        api: MegaApiJava, request: MegaRequest,
        e: MegaError,
    ) {
        Timber.d("onRequestFinish: %s", request.file)
        Timber.d("Timer cancel")
        try {
            if (timer != null) {
                timer?.cancel()
                if (serversBusyText != null) {
                    serversBusyText?.visibility = View.GONE
                }
            }
        } catch (ex: Exception) {
            Timber.e(ex, "TIMER EXCEPTION")
        }
        if (request.type == MegaRequest.TYPE_LOGIN) {
            Timber.d("REQUEST LOGIN")
            statusDialog?.dismiss()
            if (e.errorCode != MegaError.API_OK) {
                MegaApplication.isLoggingIn = false
                val errorMessage = when (e.errorCode) {
                    MegaError.API_ENOENT -> {
                        getString(R.string.error_incorrect_email_or_password)
                    }

                    MegaError.API_ESID -> {
                        getString(R.string.error_server_expired_session)
                    }

                    MegaError.API_EMFAREQUIRED -> {
                        is2FAEnabled = true
                        showAB(tB)
                        loginLayout?.visibility = View.GONE
                        loginCreateAccount?.visibility = View.GONE
                        loginLoggingIn?.visibility = View.GONE
                        generatingKeysText?.visibility = View.GONE
                        loginProgressBar?.visibility = View.GONE
                        loginFetchNodesProgressBar?.visibility = View.GONE
                        queryingSignupLinkText?.visibility = View.GONE
                        confirmingAccountText?.visibility = View.GONE
                        fetchingNodesText?.visibility = View.GONE
                        prepareNodesText?.visibility = View.GONE
                        serversBusyText?.visibility = View.GONE
                        loginVerificationLayout?.visibility = View.VISIBLE
                        firstPin?.requestFocus()
                        firstPin?.isCursorVisible = true
                        ""
                    }

                    else -> {
                        e.errorString
                    }
                }
                if (!is2FAEnabled) {
                    loginLoggingIn?.visibility = View.GONE
                    loginLayout?.visibility = View.VISIBLE
                    loginCreateAccount?.visibility = View.INVISIBLE
                    queryingSignupLinkText?.visibility = View.GONE
                    confirmingAccountText?.visibility = View.GONE
                    generatingKeysText?.visibility = View.GONE
                    loggingInText?.visibility = View.GONE
                    fetchingNodesText?.visibility = View.GONE
                    prepareNodesText?.visibility = View.GONE
                    if (serversBusyText != null) {
                        serversBusyText?.visibility = View.GONE
                    }
                    Util.showErrorAlertDialog(errorMessage, false, this@FileProviderActivity)
                }
                if (dbH.preferences != null) {
                    dbH.clearPreferences()
                    dbH.setFirstTime(false)
                    viewModel.stopCameraUploads()
                }
            } else {
                Timber.d("Logged in. Setting account auth token for folder links.")
                megaApiFolder.accountAuth = megaApi.accountAuth
                if (is2FAEnabled) {
                    is2FAEnabled = false
                    loginVerificationLayout?.visibility = View.GONE
                    hideAB()
                }
                loginLoggingIn?.visibility = View.VISIBLE
                generatingKeysText?.visibility = View.VISIBLE
                loginProgressBar?.visibility = View.VISIBLE
                loginFetchNodesProgressBar?.visibility = View.GONE
                queryingSignupLinkText?.visibility = View.GONE
                confirmingAccountText?.visibility = View.GONE
                loggingInText?.visibility = View.VISIBLE
                fetchingNodesText?.visibility = View.VISIBLE
                prepareNodesText?.visibility = View.GONE
                if (serversBusyText != null) {
                    serversBusyText?.visibility = View.GONE
                }
                gSession = megaApi.dumpSession()
                Timber.d("Logged in")
                megaApi.fetchNodes(this@FileProviderActivity)

                // Get cookies settings after login.
                getInstance().checkEnabledCookies()
            }
        } else if (request.type == MegaRequest.TYPE_FETCH_NODES) {
            if (e.errorCode != MegaError.API_OK) {
                loginLoggingIn?.visibility = View.GONE
                loginLayout?.visibility = View.VISIBLE
                loginCreateAccount?.visibility = View.INVISIBLE
                generatingKeysText?.visibility = View.GONE
                loggingInText?.visibility = View.GONE
                fetchingNodesText?.visibility = View.GONE
                prepareNodesText?.visibility = View.GONE
                if (serversBusyText != null) {
                    serversBusyText?.visibility = View.GONE
                }
                queryingSignupLinkText?.visibility = View.GONE
                confirmingAccountText?.visibility = View.GONE
                val errorMessage: String = when (e.errorCode) {
                    MegaError.API_ESID -> getString(R.string.error_server_expired_session)
                    MegaError.API_ETOOMANY -> getString(R.string.too_many_attempts_login)
                    MegaError.API_EINCOMPLETE -> getString(R.string.account_not_validated_login)
                    MegaError.API_EBLOCKED -> getString(R.string.error_account_suspended)
                    else -> e.errorString
                }
                showSnackbar(errorMessage)
            } else {
                val credentials =
                    UserCredentials(lastEmail, gSession, "", "", megaApi.myUserHandle)
                dbH.saveCredentials(credentials)
                setContentView(R.layout.activity_file_provider)
                tabShown = CLOUD_TAB
                MegaApplication.isLoggingIn = false
                afterFetchNodes()
            }
        }
    }

    private fun showSnackbar(s: String?) {
        if (scrollView != null) {
            showSimpleSnackbar(displayMetrics, scrollView, s)
        }
    }

    private fun afterFetchNodes() {
        Timber.d("afterFetchNodes")
        //Set toolbar
        tB = findViewById(R.id.toolbar_provider)
        //Set app bar layout
        aBL = findViewById(R.id.app_bar_layout_provider)
        val params = tB?.layoutParams as AppBarLayout.LayoutParams
        params.setMargins(0, 0, 0, 0)
        showAB(tB)
        cancelButton = findViewById<Button?>(R.id.cancel_button).apply {
            setOnClickListener { finish() }
        }
        attachButton = findViewById<Button?>(R.id.attach_button).apply {
            setOnClickListener { onAttachClicked() }
        }
        activateButton(false)

        //TABS section
        tabLayoutProvider = findViewById(R.id.sliding_tabs_provider)
        viewPagerProvider = findViewById<CustomViewPager?>(R.id.provider_tabs_pager).apply {
            if (mTabsAdapterProvider == null) {
                mTabsAdapterProvider =
                    ProviderPageAdapter(supportFragmentManager, this@FileProviderActivity)
                adapter = mTabsAdapterProvider
                tabLayoutProvider?.setupWithViewPager(viewPagerProvider)
            }
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {}
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int,
                ) {
                }

                override fun onPageSelected(position: Int) {
                    Timber.d("onTabChanged TabId: %s", position)
                    if (position == CLOUD_TAB) {
                        tabShown = CLOUD_TAB
                        cloudDriveProviderFragment = supportFragmentManager.findFragmentByTag(
                            getFragmentTag(
                                R.id.provider_tabs_pager, CLOUD_TAB
                            )
                        ) as CloudDriveProviderFragment?
                        if (cloudDriveProviderFragment != null) {
                            if (cloudDriveProviderFragment?.parentHandle == INVALID_HANDLE || cloudDriveProviderFragment?.parentHandle == megaApi.rootNode?.handle) {
                                aB?.setTitle(getString(R.string.file_provider_title))
                            } else {
                                aB?.setTitle(
                                    megaApi.getNodeByHandle(
                                        cloudDriveProviderFragment?.parentHandle ?: INVALID_HANDLE
                                    )?.name
                                )
                            }
                        }
                    } else if (position == INCOMING_TAB) {
                        tabShown = INCOMING_TAB
                        incomingSharesProviderFragment = supportFragmentManager.findFragmentByTag(
                            getFragmentTag(
                                R.id.provider_tabs_pager, INCOMING_TAB
                            )
                        ) as IncomingSharesProviderFragment?
                        if (incomingSharesProviderFragment != null) {
                            if (incomingSharesProviderFragment?.deepBrowserTree == 0) {
                                aB?.setTitle(getString(R.string.file_provider_title))
                            } else {
                                aB?.setTitle(incomingSharesProviderFragment?.name)
                            }
                        }
                    }
                }
            })
        }
    }

    override fun onRequestTemporaryError(
        api: MegaApiJava, request: MegaRequest,
        e: MegaError,
    ) {
        Timber.w("onRequestTemporaryError: %s", request.requestString)
        Timber.d("Start timer")
        try {
            timer = object : CountDownTimer(10000, 2000) {
                override fun onTick(millisUntilFinished: Long) {
                    Timber.d("TemporaryError one more")
                }

                override fun onFinish() {
                    Timber.d("The timer finished, message shown")
                    if (serversBusyText != null) {
                        serversBusyText?.visibility = View.VISIBLE
                    }
                }
            }.start()
        } catch (exception: Exception) {
            Timber.e(exception, "EXCEPTION when starting count")
        }
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestUpdate")
        Timber.d("Cancel timer")
        try {
            if (timer != null) {
                timer?.cancel()
                if (serversBusyText != null) {
                    serversBusyText?.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "TIMER EXCEPTION")
        }
    }

    override fun onUsersUpdate(api: MegaApiJava, users: ArrayList<MegaUser>?) {
        Timber.d("onUsersUpdate")
    }

    override fun onUserAlertsUpdate(api: MegaApiJava, userAlerts: ArrayList<MegaUserAlert>?) {
        Timber.d("onUserAlertsUpdate")
    }

    override fun onNodesUpdate(api: MegaApiJava, nodeList: ArrayList<MegaNode>?) {
        Timber.d("onNodesUpdate")
        if (cloudDriveProviderFragment != null) {
            if (megaApi.getNodeByHandle(
                    cloudDriveProviderFragment?.parentHandle ?: INVALID_HANDLE
                ) != null
            ) {
                nodes =
                    megaApi.getChildren(
                        megaApi.getNodeByHandle(
                            cloudDriveProviderFragment?.parentHandle ?: INVALID_HANDLE
                        )
                    )
                cloudDriveProviderFragment?.setNodes(nodes)
                cloudDriveProviderFragment?.listView?.invalidate()
            }
        }
    }

    override fun onReloadNeeded(api: MegaApiJava) {}
    public override fun onDestroy() {
        megaApi.removeRequestListener(this@FileProviderActivity)
        megaApi.removeTransferListener(this@FileProviderActivity)
        megaApi.removeGlobalListener(this@FileProviderActivity)
        super.onDestroy()
    }

    override fun onTransferStart(api: MegaApiJava, transfer: MegaTransfer) {}
    override fun onTransferFinish(
        api: MegaApiJava, transfer: MegaTransfer,
        e: MegaError,
    ) {
        Timber.d("onTransferFinish: %s", transfer.path)
        if (transfer.isStreamingTransfer) {
            return
        }
        try {
            //Get the URI of the file
            val fileToShare = File(transfer.path)
            //		File newFile = new File(fileToShare, "default_image.jpg");
            val contentUri = FileProvider.getUriForFile(
                this@FileProviderActivity,
                Constants.AUTHORITY_STRING_FILE_PROVIDER,
                fileToShare
            )
            grantUriPermission("*", contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (totalTransfers == 0) {
                Timber.d("CONTENT URI: %s", contentUri)
                //Send it
                val result = Intent()
                result.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                result.data = contentUri
                result.action = Intent.ACTION_GET_CONTENT
                if (parent == null) {
                    setResult(RESULT_OK, result)
                } else {
                    Toast.makeText(
                        this@FileProviderActivity,
                        "parent no null",
                        Toast.LENGTH_LONG
                    ).show()
                    parent.setResult(RESULT_OK, result)
                }
                finish()
            } else {
                contentUris.add(contentUri)
                progressTransfersFinish++
                if (progressTransfersFinish == totalTransfers) {
                    val result = Intent()
                    result.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    if (clipDataTransfers == null) {
                        clipDataTransfers = ClipData.newUri(contentResolver, "", contentUris[0])
                    } else {
                        clipDataTransfers?.addItem(ClipData.Item(contentUris[0]))
                    }
                    for (i in 1 until contentUris.size) {
                        clipDataTransfers?.addItem(ClipData.Item(contentUris[i]))
                    }
                    result.clipData = clipDataTransfers
                    result.action = Intent.ACTION_GET_CONTENT
                    if (parent == null) {
                        setResult(RESULT_OK, result)
                    } else {
                        Toast.makeText(
                            this@FileProviderActivity,
                            "parent no null",
                            Toast.LENGTH_LONG
                        ).show()
                        parent.setResult(RESULT_OK, result)
                    }
                    totalTransfers = 0
                    finish()
                }
            }
        } catch (exception: Exception) {
            finish()
        }
    }

    override fun onTransferUpdate(api: MegaApiJava, transfer: MegaTransfer) {}
    override fun onTransferTemporaryError(
        api: MegaApiJava,
        transfer: MegaTransfer, e: MegaError,
    ) {

        //Answer to the Intent GET_CONTENT with null
    }

    override fun onTransferData(
        api: MegaApiJava, transfer: MegaTransfer,
        buffer: ByteArray,
    ): Boolean {
        return false
    }

    override fun onAccountUpdate(api: MegaApiJava) {}
    override fun onContactRequestsUpdate(
        api: MegaApiJava,
        requests: ArrayList<MegaContactRequest>?,
    ) {
    }

    override fun onEvent(api: MegaApiJava, event: MegaEvent?) {}
    override fun onSetsUpdate(api: MegaApiJava, sets: ArrayList<MegaSet>?) {}
    override fun onSetElementsUpdate(api: MegaApiJava, elements: ArrayList<MegaSetElement>?) {}

    /**
     * Activate button
     *
     * @param show True if should enable, false otherwise.
     */
    fun activateButton(show: Boolean) {
        attachButton?.isEnabled = show
        if (show) {
            attachButton?.setTextColor(
                getThemeColor(
                    this@FileProviderActivity,
                    com.google.android.material.R.attr.colorSecondary
                )
            )
        } else {
            attachButton?.setTextColor(
                ContextCompat.getColor(
                    this@FileProviderActivity,
                    R.color.teal_300_038_teal_200_038
                )
            )
        }
    }

    /**
     * Attach files
     *
     * @param nodes List of [MegaNode]
     */
    fun attachFiles(nodes: List<MegaNode>?) {
        selectedNodes = nodes
    }

    private val cDriveProviderLol: CloudDriveProviderFragment?
        /**
         * Gets Cloud Drive fragment.
         *
         * @return The fragment if available, null if does not exist or is not added.
         */
        get() {
            val cDriveProviderLol = supportFragmentManager
                .findFragmentByTag(
                    getFragmentTag(
                        R.id.provider_tabs_pager,
                        CLOUD_TAB
                    )
                ) as CloudDriveProviderFragment?
            return if (cDriveProviderLol != null && cDriveProviderLol.isAdded) {
                cDriveProviderLol.also { cloudDriveProviderFragment = it }
            } else null
        }

    /**
     * Gets Incoming Shares fragment.
     *
     * @return The fragment if available, null if does not exist or is not added.
     */
    private fun getIncomingSharesProviderFragment(): IncomingSharesProviderFragment? {
        val iSharesProviderFragment = supportFragmentManager
            .findFragmentByTag(
                getFragmentTag(
                    R.id.provider_tabs_pager,
                    INCOMING_TAB
                )
            ) as IncomingSharesProviderFragment?
        return if (iSharesProviderFragment != null && iSharesProviderFragment.isAdded) {
            iSharesProviderFragment.also { incomingSharesProviderFragment = it }
        } else null
    }

    /**
     * Hides or shows tabs of a section depending on the navigation level
     * and if select mode is enabled or not.
     *
     * @param hide       If true, hides the tabs, else shows them.
     * @param currentTab The current tab where the action happens.
     */
    fun hideTabs(hide: Boolean, currentTab: Int) {
        when (currentTab) {
            CLOUD_TAB -> if (cDriveProviderLol == null || !hide && parentHandle != cloudRootHandle && parentHandle != INVALID_HANDLE) {
                return
            }

            INCOMING_TAB -> if (getIncomingSharesProviderFragment() == null || !hide && incParentHandle != INVALID_HANDLE) {
                return
            }
        }
        viewPagerProvider?.disableSwipe(hide)
        tabLayoutProvider?.visibility = if (hide) View.GONE else View.VISIBLE
    }

    /**
     * Changes the elevation.
     *
     * @param withElevation True if should show elevation, false otherwise.
     * @param fragmentIndex Fragment index which wants to modify the elevation.
     */
    fun changeActionBarElevation(withElevation: Boolean, fragmentIndex: Int) {
        if (viewPagerProvider == null || viewPagerProvider?.currentItem != fragmentIndex) {
            return
        }
        changeStatusBarColorForElevation(this@FileProviderActivity, withElevation)
        val elevation = resources.getDimension(R.dimen.toolbar_elevation)
        aBL?.elevation = if (withElevation) elevation else 0F
    }

    override fun onGlobalSyncStateChanged(api: MegaApiJava) {}

    companion object {
        private const val INVALID_TAB = -1

        /**
         * Cloud Tab
         */
        const val CLOUD_TAB = 0

        /**
         * Incoming Tab
         */
        const val INCOMING_TAB = 1

        /**
         * From Mega App
         */
        const val FROM_MEGA_APP = "FROM_MEGA_APP"
    }
}