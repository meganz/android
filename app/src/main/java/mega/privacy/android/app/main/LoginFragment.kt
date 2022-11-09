package mega.privacy.android.app.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Base64
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.MegaApplication.Companion.getChatManagement
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.MegaApplication.Companion.isIsHeartBeatAlive
import mega.privacy.android.app.MegaApplication.Companion.isLoggingIn
import mega.privacy.android.app.MegaApplication.Companion.setHeartBeatAlive
import mega.privacy.android.app.R
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.components.EditTextPIN
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.databinding.FragmentLoginBinding
import mega.privacy.android.app.listeners.ChatLogoutListener
import mega.privacy.android.app.logging.LegacyLoggingSettings
import mega.privacy.android.app.main.controllers.AccountController.Companion.localLogoutApp
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.setStartScreenTimeStamp
import mega.privacy.android.app.providers.FileProviderActivity
import mega.privacy.android.app.upgradeAccount.ChooseAccountActivity
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.ChangeApiServerUtil.showChangeApiServerDialog
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ConstantsUrl.RECOVERY_URL
import mega.privacy.android.app.utils.ConstantsUrl.RECOVERY_URL_EMAIL
import mega.privacy.android.app.utils.JobUtil
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.ViewUtils.hideKeyboard
import mega.privacy.android.app.utils.ViewUtils.removeLeadingAndTrailingSpaces
import mega.privacy.android.app.utils.permission.PermissionUtils.getAudioPermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getImagePermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getReadExternalStoragePermission
import mega.privacy.android.app.utils.permission.PermissionUtils.getVideoPermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.model.ChatSettings
import mega.privacy.android.data.model.UserCredentials
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.repository.LoginRepository
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import javax.inject.Inject

/**
 * Login fragment.
 *
 * @property sharingScope         [CoroutineScope]
 * @property loggingSettings      [LegacyLoggingSettings]
 * @property loginRepository      [LoginRepository]
 * @property megaApi              [MegaApiAndroid]
 * @property megaApiFolder        [MegaApiAndroid] used for folder links management.
 * @property megaChatApi          [MegaChatApiAndroid]
 * @property dbH                  [DatabaseHandler]
 * @property numberOfClicksKarere Number of clicks for enabling MEGAChat logs.
 * @property numberOfClicksSDK    Number of clicks for enabling SDK logs.
 * @property emailTemp            Typed email.
 * @property passwdTemp           Typed password.
 */
@AndroidEntryPoint
class LoginFragment : Fragment(), MegaRequestListenerInterface {

    @ApplicationScope
    @Inject
    lateinit var sharingScope: CoroutineScope

    @Inject
    lateinit var loggingSettings: LegacyLoggingSettings

    @Inject
    lateinit var loginRepository: LoginRepository

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @MegaApiFolder
    @Inject
    lateinit var megaApiFolder: MegaApiAndroid

    @Inject
    lateinit var megaChatApi: MegaChatApiAndroid

    @Inject
    lateinit var dbH: DatabaseHandler

    var numberOfClicksKarere = 0
    var numberOfClicksSDK = 0

    var emailTemp: String? = null
    var passwdTemp: String? = null

    private val viewModel by viewModels<LoginViewModel>()

    private lateinit var binding: FragmentLoginBinding

    private var insertMKDialog: AlertDialog? = null
    private var changeApiServerDialog: AlertDialog? = null
    private var confirmLogoutDialog: AlertDialog? = null

    private var loginTitleBoundaries: Rect? = null
    private var timer: CountDownTimer? = null
    private var firstRequestUpdate = true

    private var chatSettings: ChatSettings? = null
    private var lastEmail: String? = null
    private var lastPassword: String? = null
    private var gSession: String? = null
    private var resumeSession = false
    private var confirmLink: String? = null

    private var isFetchingNodes = false
    private var firstTime = true
    private var backWhileLogin = false
    private var loginClicked = false
    private var intentReceived: Intent? = null
    private var extras: Bundle? = null
    private var uriData: Uri? = null
    private var action: String? = null
    private var url: String? = null
    private var parentHandle: Long = -1
    private val sb by lazy { StringBuilder() }
    private var pin: String? = null
    private var isFirstTime = true
    private var isErrorShown = false
    private var is2FAEnabled = false
    private var accountConfirmed = false
    private var pinLongClick = false
    private var twoFA = false
    private var receivedIntent: Intent? = null
    private var shareInfos: ArrayList<ShareInfo>? = null
    private val scaleW by lazy {
        Util.getScaleW(resources.displayMetrics,
            resources.displayMetrics.density)
    }

    private val requestMediaPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.values.contains(false)) {
                readyToManager()
            } else {
                toSharePage()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("onCreateView")
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInitialState()
        setupView()
        setupIntent()
    }

    private fun setupInitialState() {
        is2FAEnabled = false
        accountConfirmed = false
        loginClicked = false
        backWhileLogin = false
        firstTime = if (dbH.credentials != null) {
            Timber.d("Credentials NOT null")
            false
        } else {
            true
        }

        chatSettings = dbH.chatSettings

        if (chatSettings == null) {
            Timber.d("chatSettings is null --> enable chat by default")
            chatSettings = ChatSettings()
            dbH.chatSettings = chatSettings
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupView() = with(binding) {
        loginTextView.apply {
            text = requireContext().getFormattedStringOrDefault(R.string.login_to_mega)
            setOnClickListener {
                numberOfClicksKarere++
                if (numberOfClicksKarere == Constants.CLICKS_ENABLE_DEBUG) {
                    if (loggingSettings.areKarereLogsEnabled()) {
                        numberOfClicksKarere = 0
                        loggingSettings.setStatusLoggerKarere(requireActivity(), false)
                    } else {
                        (requireActivity() as LoginActivity).showConfirmationEnableLogsKarere()
                    }
                }
            }
            val onLongPress = Runnable {
                if (!isAlertDialogShown(changeApiServerDialog)) {
                    changeApiServerDialog =
                        showChangeApiServerDialog((requireActivity() as LoginActivity), megaApi)
                }
            }
            val onTap = Runnable {
                handler.postDelayed(onLongPress,
                    LONG_CLICK_DELAY - ViewConfiguration.getTapTimeout())
            }
            setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        loginTitleBoundaries =
                            Rect(view.left, view.top, view.right, view.bottom)
                        handler.postDelayed(onTap, ViewConfiguration.getTapTimeout().toLong())
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(onLongPress)
                        handler.removeCallbacks(onTap)
                    }
                    MotionEvent.ACTION_MOVE -> if (loginTitleBoundaries != null && !loginTitleBoundaries!!.contains(
                            view.left + event.x.toInt(),
                            view.top + event.y.toInt())
                    ) {
                        handler.removeCallbacks(onLongPress)
                        handler.removeCallbacks(onTap)
                    }
                }
                false
            }
        }

        loginEmailTextErrorIcon.isVisible = false

        loginEmailText.apply {
            isCursorVisible = true
            background.clearColorFilter()
            requestFocus()
            doAfterTextChanged { quitError(this) }
            onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (!hasFocus) {
                    removeLeadingAndTrailingSpaces()
                }
            }
        }

        loginPasswordTextLayout.isEndIconVisible = false
        loginPasswordTextErrorIcon.isVisible = false

        loginPasswordText.apply {
            isCursorVisible = true
            background.clearColorFilter()
            setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submitForm()
                    return@OnEditorActionListener true
                }
                false
            })
            doAfterTextChanged { quitError(this) }
            onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                loginPasswordTextLayout.isEndIconVisible = hasFocus
            }
        }

        buttonLogin.apply {
            text = requireContext().getFormattedStringOrDefault(R.string.login_text)
            setOnClickListener {
                Timber.d("Click on button_login_login")
                loginClicked = true
                backWhileLogin = false
                LoginActivity.isBackFromLoginPage = false
                loginEmailText.removeLeadingAndTrailingSpaces()
                submitForm()
            }
        }

        buttonForgotPass.setOnClickListener {
            Timber.d("Click on button_forgot_pass")
            try {
                val openTermsIntent = Intent(requireContext(), WebViewActivity::class.java)
                openTermsIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

                if (loginEmailText.text.toString().isNotEmpty()) {
                    val typedEmail = loginEmailText.text.toString()
                    var encodedEmail =
                        Base64.encodeToString(typedEmail.toByteArray(), Base64.DEFAULT)
                    encodedEmail = encodedEmail.replace("\n", "")
                    openTermsIntent.data = Uri.parse(RECOVERY_URL_EMAIL + encodedEmail)
                } else {
                    openTermsIntent.data = Uri.parse(RECOVERY_URL)
                }
                startActivity(openTermsIntent)
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(RECOVERY_URL)))
            }
        }

        textNewToMega.setOnClickListener {
            numberOfClicksSDK++
            if (numberOfClicksSDK == Constants.CLICKS_ENABLE_DEBUG) {
                if (loggingSettings.areSDKLogsEnabled()) {
                    numberOfClicksSDK = 0
                    loggingSettings.setStatusLoggerSDK(requireActivity(), false)
                } else {
                    (requireActivity() as LoginActivity).showConfirmationEnableLogsSDK()
                }
            }
        }

        buttonCreateAccountLogin.setOnClickListener {
            Timber.d("Click on button_create_account_login")
            (requireActivity() as LoginActivity).showFragment(Constants.CREATE_ACCOUNT_FRAGMENT)
        }

        loginLayout.isVisible = true
        loginCreateAccountLayout.isVisible = true
        loginLoggingInLayout.isVisible = false
        loginGeneratingKeysText.isVisible = false
        loginLoggingInText.isVisible = false
        loginFetchNodesText.isVisible = false
        loginPrepareNodesText.isVisible = false
        loginProgressBar.isVisible = false
        loginQuerySignupLinkText.isVisible = false
        loginConfirmAccountText.isVisible = false
        loginServersBusyText.isVisible = false
        login2fa.isVisible = false

        lostAuthenticationDevice.setOnClickListener {
            try {
                startActivity(Intent(requireContext(), WebViewActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    data = Uri.parse(RECOVERY_URL)
                })
            } catch (e: Exception) {
                val viewIntent = Intent(Intent.ACTION_VIEW)
                viewIntent.data = Uri.parse(RECOVERY_URL)
                startActivity(viewIntent)
            }
        }

        pin2faErrorLogin.isVisible = false

        addEditTextPintListeners(pinFirstLogin)
        addEditTextPintListeners(pinSecondLogin)
        addEditTextPintListeners(pinThirdLogin)
        addEditTextPintListeners(pinFourthLogin)
        addEditTextPintListeners(pinFifthLogin)
        addEditTextPintListeners(pinSixthLogin)

        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        pinSecondLogin.setEt(pinFirstLogin)
        pinThirdLogin.setEt(pinSecondLogin)
        pinFourthLogin.setEt(pinThirdLogin)
        pinFifthLogin.setEt(pinFourthLogin)
        pinSixthLogin.setEt(pinFifthLogin)

        if (passwdTemp != null && emailTemp != null) {
            submitFormConfirmAccount()
        }
    }

    /**
     * Adds long click, focus change, and after text change listeners to the 2FA fields.
     *
     * @param editTextPIN The field.
     */
    private fun addEditTextPintListeners(editTextPIN: EditTextPIN) {
        editTextPIN.apply {
            setOnLongClickListener {
                pinLongClick = true
                requestFocus()
                return@setOnLongClickListener false
            }
            onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (hasFocus) setText("")
            }
            doAfterTextChanged {
                if (length() != 0) {
                    requestEditTextPinFocus(
                        with(binding) {
                            when (id) {
                                R.id.pin_first_login -> pinSecondLogin
                                R.id.pin_second_login -> pinThirdLogin
                                R.id.pin_third_login -> pinFourthLogin
                                R.id.pin_fourth_login -> pinFifthLogin
                                else -> pinSixthLogin
                            }
                        }
                    )

                    if (id == R.id.pin_sixth_login) hideKeyboard()

                    when {
                        areAllFieldsFilled() -> verify2FA()
                        isFirstTime && !pinLongClick -> clear2FAFields(id)
                        pinLongClick -> pasteClipboard()
                    }
                } else if (isErrorShown) {
                    hideError()
                }
            }
        }
    }

    /**
     * Clears 2FA fields depending on the current field focus.
     *
     * @param fieldId Current field id.
     */
    private fun clear2FAFields(fieldId: Int) = with(binding) {
        if (fieldId == R.id.pin_sixth_login) return
        pinSixthLogin.setText("")
        if (fieldId == R.id.pin_fifth_login) return
        pinFifthLogin.setText("")
        if (fieldId == R.id.pin_fourth_login) return
        pinFourthLogin.setText("")
        if (fieldId == R.id.pin_third_login) return
        pinThirdLogin.setText("")
        if (fieldId == R.id.pin_second_login) return
        pinSecondLogin.setText("")
    }

    /**
     * Requests the focus for 2FA fields.
     *
     * @param editTextPIN The field.
     */
    private fun requestEditTextPinFocus(editTextPIN: EditTextPIN) {
        editTextPIN.apply {
            requestFocus()
            isCursorVisible = true
        }
    }

    /**
     * Gets data from the intent and performs the corresponding action if necessary.
     */
    private fun setupIntent() {
        intentReceived = (requireActivity() as LoginActivity).intent

        intentReceived?.let { intent ->
            action = intent.action

            action?.let { action ->
                Timber.d("action is: %s", action)
                when {
                    Constants.ACTION_CONFIRM == action -> {
                        handleConfirmationIntent(intent)
                        return
                    }
                    action == Constants.ACTION_RESET_PASS -> {
                        val link = intent.dataString
                        if (link != null) {
                            Timber.d("Link to resetPass: %s", link)
                            showDialogInsertMKToChangePass(link)
                            return
                        }
                    }
                    action == Constants.ACTION_PASS_CHANGED -> {
                        when (intent.getIntExtra(Constants.RESULT, MegaError.API_OK)) {
                            MegaError.API_OK -> (requireActivity() as LoginActivity).showSnackbar(
                                requireContext().getFormattedStringOrDefault(
                                    R.string.pass_changed_alert))
                            MegaError.API_EKEY -> (requireActivity() as LoginActivity).showAlertIncorrectRK()
                            MegaError.API_EBLOCKED -> (requireActivity() as LoginActivity).showSnackbar(
                                requireContext().getFormattedStringOrDefault(
                                    R.string.error_reset_account_blocked))
                            else -> (requireActivity() as LoginActivity).showSnackbar(requireContext().getFormattedStringOrDefault(
                                R.string.general_text_error))
                        }
                        return
                    }
                    action == Constants.ACTION_CANCEL_DOWNLOAD -> {
                        (requireActivity() as LoginActivity).showConfirmationCancelAllTransfers()
                    }
                    action == Constants.ACTION_SHOW_WARNING_ACCOUNT_BLOCKED -> {
                        val accountBlockedString =
                            intent.getStringExtra(Constants.ACCOUNT_BLOCKED_STRING)
                        if (!TextUtil.isTextEmpty(accountBlockedString)) {
                            Util.showErrorAlertDialog(accountBlockedString, false, activity)
                        }
                    }
                }
            } ?: Timber.w("ACTION NULL")
        } ?: Timber.w("No INTENT")

        Timber.d("et_user.getText(): %s", binding.loginEmailText.text)

        if (dbH.credentials != null && !LoginActivity.isBackFromLoginPage) {
            Timber.d("Credentials NOT null")

            intentReceived?.let { intent ->
                action?.let { action ->
                    when (action) {
                        Constants.ACTION_REFRESH -> {
                            isLoggingIn = true
                            startFetchNodes()
                            return
                        }
                        Constants.ACTION_REFRESH_API_SERVER -> {
                            twoFA = true
                            parentHandle = intent.getLongExtra("PARENT_HANDLE", -1)
                            startFastLogin()
                            return
                        }
                        Constants.ACTION_REFRESH_AFTER_BLOCKED -> {
                            startFastLogin()
                            return
                        }
                        else -> {
                            when (action) {
                                Constants.ACTION_OPEN_MEGA_FOLDER_LINK -> {
                                    url = intent.dataString
                                }
                                Constants.ACTION_IMPORT_LINK_FETCH_NODES -> {
                                    url = intent.dataString
                                }
                                Constants.ACTION_CHANGE_MAIL -> {
                                    Timber.d("intent received ACTION_CHANGE_MAIL")
                                    url = intent.dataString
                                }
                                Constants.ACTION_CANCEL_ACCOUNT -> {
                                    Timber.d("intent received ACTION_CANCEL_ACCOUNT")
                                    url = intent.dataString
                                }
                                Constants.ACTION_FILE_PROVIDER -> {
                                    uriData = intent.data
                                    extras = intent.extras
                                    url = null
                                }
                                Constants.ACTION_OPEN_HANDLE_NODE -> {
                                    url = intent.dataString
                                }
                                Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL -> {
                                    uriData = intent.data
                                }
                                Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL -> {
                                    uriData = intent.data
                                }
                                Constants.ACTION_OPEN_CHAT_LINK -> {
                                    url = intent.dataString
                                }
                                Constants.ACTION_JOIN_OPEN_CHAT_LINK -> {
                                    url = intent.dataString
                                }
                            }

                            if (megaApi.rootNode != null) {
                                var newIntent =
                                    Intent(requireContext(), ManagerActivity::class.java)

                                when (action) {
                                    Constants.ACTION_FILE_PROVIDER -> {
                                        newIntent =
                                            Intent(requireContext(),
                                                FileProviderActivity::class.java)
                                        extras?.let { newIntent.putExtras(it) }
                                        newIntent.data = uriData
                                    }
                                    Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL -> {
                                        newIntent =
                                            Intent(requireContext(), FileLinkActivity::class.java)
                                        newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        this@LoginFragment.action = Constants.ACTION_OPEN_MEGA_LINK
                                        newIntent.data = uriData
                                    }
                                    Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL -> {
                                        newIntent =
                                            Intent(requireContext(), FolderLinkActivity::class.java)
                                        newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        this@LoginFragment.action =
                                            Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                                        newIntent.data = uriData
                                    }
                                    Constants.ACTION_OPEN_CONTACTS_SECTION -> {
                                        newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        this@LoginFragment.action =
                                            Constants.ACTION_OPEN_CONTACTS_SECTION
                                        if (newIntent.getLongExtra(Constants.CONTACT_HANDLE,
                                                -1) != -1L
                                        ) {
                                            newIntent.putExtra(Constants.CONTACT_HANDLE,
                                                newIntent.getLongExtra(
                                                    Constants.CONTACT_HANDLE, -1))
                                        }
                                    }
                                }

                                newIntent.action = this@LoginFragment.action

                                if (url != null) {
                                    newIntent.data = Uri.parse(url)
                                }

                                newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                    startCameraUploadService(false, 5 * 60 * 1000)
                                }

                                startActivity(newIntent)
                                (requireActivity() as LoginActivity).finish()
                            } else {
                                startFastLogin()
                            }

                            return
                        }
                    }
                }
            }

            if (megaApi.rootNode != null && !isFetchingNodes && !isIsHeartBeatAlive) {
                Timber.d("rootNode != null")

                var intent = Intent(requireContext(), ManagerActivity::class.java)

                action?.let { action ->
                    when (action) {
                        Constants.ACTION_FILE_PROVIDER -> {
                            intent = Intent(requireContext(), FileProviderActivity::class.java)
                            extras?.let { intent.putExtras(it) }
                            intent.data = uriData
                        }
                        Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL -> {
                            intent = Intent(requireContext(), FileLinkActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            this@LoginFragment.action = Constants.ACTION_OPEN_MEGA_LINK
                            intent.data = uriData
                        }
                        Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL -> {
                            intent = Intent(requireContext(), FolderLinkActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            this@LoginFragment.action = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                            intent.data = uriData
                        }
                        Constants.ACTION_OPEN_CONTACTS_SECTION -> {
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

                            if (intentReceived?.getLongExtra(Constants.CONTACT_HANDLE, -1) != -1L) {
                                intent.putExtra(Constants.CONTACT_HANDLE,
                                    intentReceived?.getLongExtra(Constants.CONTACT_HANDLE, -1))
                            }
                        }
                    }

                    intent.action = action

                    if (url != null) {
                        intent.data = Uri.parse(url)
                    }
                }

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

                dbH.preferences?.let { preferences ->
                    preferences.camSyncEnabled?.let { enabled ->
                        if (enabled.toBoolean() && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                            startCameraUploadService(false, 30 * 1000)
                        }
                    }
                }

                this.startActivity(intent)
                (requireActivity() as LoginActivity).finish()
            } else {
                Timber.d("rootNode is null or heart beat is alive -> do fast login")
                setHeartBeatAlive(false)
                startFastLogin()
            }

            return
        }

        Timber.d("Credentials IS NULL")

        intentReceived?.let { intent ->
            Timber.d("INTENT NOT NULL")

            action?.let { action ->
                Timber.d("ACTION NOT NULL")
                val newIntent: Intent
                when (action) {
                    Constants.ACTION_FILE_PROVIDER -> {
                        newIntent = Intent(requireContext(), FileProviderActivity::class.java)
                        extras?.let { newIntent.putExtras(it) }
                        newIntent.data = uriData
                        newIntent.action = action
                    }
                    Constants.ACTION_FILE_EXPLORER_UPLOAD -> {
                        (requireActivity() as LoginActivity).showSnackbar(requireContext().getFormattedStringOrDefault(
                            R.string.login_before_share))
                    }
                    Constants.ACTION_JOIN_OPEN_CHAT_LINK -> {
                        url = intent.dataString
                    }
                }
            }
        }
    }

    /**
     * Shows the login screen.
     */
    fun returnToLogin() = with(binding) {
        (requireActivity() as LoginActivity).hideAB()
        login2fa.isVisible = false
        loginLoggingInLayout.isVisible = false
        loginLayout.isVisible = true
        confirmLogoutDialog?.dismiss()
        loginCreateAccountLayout.isVisible = true
        loginQuerySignupLinkText.isVisible = false
        loginConfirmAccountText.isVisible = false
        loginGeneratingKeysText.isVisible = false
        loginLoggingInText.isVisible = false
        loginFetchNodesText.isVisible = false
        loginPrepareNodesText.isVisible = false
        loginServersBusyText.isVisible = false
    }

    /**
     * Hides UI errors.
     */
    private fun hideError() = with(binding) {
        isErrorShown = false
        pin2faErrorLogin.isVisible = false
        pinFirstLogin.setTextColor(ContextCompat.getColor(requireContext(),
            R.color.grey_087_white_087))
        pinSecondLogin.setTextColor(ContextCompat.getColor(requireContext(),
            R.color.grey_087_white_087))
        pinThirdLogin.setTextColor(ContextCompat.getColor(requireContext(),
            R.color.grey_087_white_087))
        pinFourthLogin.setTextColor(ContextCompat.getColor(requireContext(),
            R.color.grey_087_white_087))
        pinFifthLogin.setTextColor(ContextCompat.getColor(requireContext(),
            R.color.grey_087_white_087))
        pinSixthLogin.setTextColor(ContextCompat.getColor(requireContext(),
            R.color.grey_087_white_087))
    }

    /**
     * Shows UI errors.
     */
    private fun showShowError() = with(binding) {
        isFirstTime = false
        isErrorShown = true
        pin2faErrorLogin.isVisible = true
        confirmLogoutDialog?.dismiss()
        pinFirstLogin.setTextColor(ContextCompat.getColor(requireContext(),
            R.color.red_600_red_300))
        pinSecondLogin.setTextColor(ContextCompat.getColor(requireContext(),
            R.color.red_600_red_300))
        pinThirdLogin.setTextColor(ContextCompat.getColor(requireContext(),
            R.color.red_600_red_300))
        pinFourthLogin.setTextColor(ContextCompat.getColor(requireContext(),
            R.color.red_600_red_300))
        pinFifthLogin.setTextColor(ContextCompat.getColor(requireContext(),
            R.color.red_600_red_300))
        pinSixthLogin.setTextColor(ContextCompat.getColor(requireContext(),
            R.color.red_600_red_300))
    }


    /**
     * Checks if all the 2FA fields are filled.
     *
     * @return True if all fields are already filled, false otherwise.
     */
    private fun areAllFieldsFilled() = with(binding) {
        pinFirstLogin.length() == 1 && pinSecondLogin.length() == 1
                && pinThirdLogin.length() == 1 && pinFourthLogin.length() == 1
                && pinFifthLogin.length() == 1 && pinSixthLogin.length() == 1
    }

    /**
     * Verifies if the typed 2FA is correct.
     */
    private fun verify2FA() = with(binding) {
        Util.hideKeyboard(requireActivity() as LoginActivity, 0)
        if (sb.isNotEmpty()) {
            sb.delete(0, sb.length)
        }
        sb.append(pinFirstLogin.text)
        sb.append(pinSecondLogin.text)
        sb.append(pinThirdLogin.text)
        sb.append(pinFourthLogin.text)
        sb.append(pinFifthLogin.text)
        sb.append(pinSixthLogin.text)
        pin = sb.toString()

        if (!isErrorShown) {
            Timber.d("Login with factor login")
            progressbarVerify2fa.isVisible = true
            isLoggingIn = true
            megaApi.multiFactorAuthLogin(lastEmail, lastPassword, pin, this@LoginFragment)
        }
    }

    /**
     * Updates the UI for fetching nodes.
     */
    private fun startFetchNodes() = with(binding) {
        Timber.d("startLoginInProcess")
        val credentials = dbH.credentials ?: return
        lastEmail = credentials.email
        gSession = credentials.session
        loginLayout.isVisible = false
        loginCreateAccountLayout.isVisible = false
        loginQuerySignupLinkText.isVisible = false
        loginConfirmAccountText.isVisible = false
        loginLoggingInLayout.isVisible = true
        loginProgressBar.isVisible = true
        loginFetchingNodesBar.isVisible = false
        loginLoggingInText.isVisible = true
        loginFetchNodesText.isVisible = true
        loginPrepareNodesText.isVisible = false
        loginServersBusyText.isVisible = false
        megaApi.fetchNodes(this@LoginFragment)
    }

    /**
     * Updates the UI for starting fast login.
     */
    private fun startFastLogin() {
        Timber.d("startFastLogin")
        val credentials = dbH.credentials ?: return

        lastEmail = credentials.email
        gSession = credentials.session

        with(binding) {
            loginLayout.isVisible = false
            loginCreateAccountLayout.isVisible = false
            loginQuerySignupLinkText.isVisible = false
            loginConfirmAccountText.isVisible = false
            loginLoggingInLayout.isVisible = true
            loginProgressBar.isVisible = true
            loginFetchingNodesBar.isVisible = false
            loginLoggingInText.isVisible = true
            loginFetchNodesText.isVisible = false
            loginPrepareNodesText.isVisible = false
            loginServersBusyText.isVisible = false
        }

        resumeSession = true

        if (!isLoggingIn) {
            isLoggingIn = true
            ChatUtil.initMegaChatApi(gSession,
                ChatLogoutListener(requireActivity(), loggingSettings))
            disableLoginButton()

            megaApi.fastLogin(gSession, this)

            if (intentReceived?.action == Constants.ACTION_REFRESH_API_SERVER) {
                Timber.d("megaChatApi.refreshUrl()")
                megaChatApi.refreshUrl()
            }
        } else {
            Timber.w("Another login is processing")
        }
    }

    /**
     * Submit typed data for confirming account.
     */
    private fun submitFormConfirmAccount() = with(binding) {
        Timber.d("fromConfirmAccount - true email: %s__%s", emailTemp, passwdTemp)
        lastEmail = emailTemp
        lastPassword = passwdTemp
        loginEmailText.hideKeyboard()

        if (!Util.isOnline(requireActivity())) {
            loginLoggingInLayout.isVisible = false
            loginLayout.isVisible = true
            confirmLogoutDialog?.dismiss()
            loginCreateAccountLayout.isVisible = true
            loginQuerySignupLinkText.isVisible = false
            loginConfirmAccountText.isVisible = false
            loginGeneratingKeysText.isVisible = false
            loginLoggingInText.isVisible = false
            loginFetchNodesText.isVisible = false
            loginPrepareNodesText.isVisible = false
            loginServersBusyText.isVisible = false
            (requireActivity() as LoginActivity).showSnackbar(requireContext().getFormattedStringOrDefault(
                R.string.error_server_connection_problem))
            return
        }
        loginLayout.isVisible = false
        loginCreateAccountLayout.isVisible = false
        loginLoggingInLayout.isVisible = true
        loginGeneratingKeysText.isVisible = true
        loginProgressBar.isVisible = true
        loginFetchingNodesBar.isVisible = false
        loginQuerySignupLinkText.isVisible = false
        loginConfirmAccountText.isVisible = false
        Timber.d("Generating keys")
        onKeysGenerated(lastEmail, lastPassword)
    }

    /**
     * Submit typed data for a login.
     */
    private fun submitForm() {
        if (checkTypedValues()) {
            performLogin()
        }
    }

    /**
     * Updates the UI for logging.
     */
    private fun performLogin() = with(binding) {
        loginEmailText.hideKeyboard()

        if (!Util.isOnline(requireActivity())) {
            loginLoggingInLayout.isVisible = false
            loginLayout.isVisible = true
            confirmLogoutDialog?.dismiss()
            loginCreateAccountLayout.isVisible = true
            loginQuerySignupLinkText.isVisible = false
            loginConfirmAccountText.isVisible = false
            loginGeneratingKeysText.isVisible = false
            loginLoggingInText.isVisible = false
            loginFetchNodesText.isVisible = false
            loginPrepareNodesText.isVisible = false
            loginServersBusyText.isVisible = false
            (requireActivity() as LoginActivity).showSnackbar(requireContext().getFormattedStringOrDefault(
                R.string.error_server_connection_problem))
            return
        }

        loginLayout.isVisible = false
        loginCreateAccountLayout.isVisible = false
        loginLoggingInLayout.isVisible = true
        loginGeneratingKeysText.isVisible = true
        loginProgressBar.isVisible = true
        loginFetchingNodesBar.isVisible = false
        loginQuerySignupLinkText.isVisible = false
        loginConfirmAccountText.isVisible = false
        lastEmail = loginEmailText.text.toString().lowercase().trim { it <= ' ' }
        lastPassword = loginPasswordText.text.toString()
        Timber.d("Generating keys")
        onKeysGenerated(lastEmail, lastPassword)
    }

    /**
     * If login [onKeysGeneratedLogin].
     * If confirm account updates the UI and launches the confirmAccount request if the is
     * network connection.
     *
     * @param email    Typed email.
     * @param password Typed password.
     */
    private fun onKeysGenerated(email: String?, password: String?) {
        Timber.d("onKeysGenerated")
        lastEmail = email
        lastPassword = password

        if (confirmLink == null) {
            onKeysGeneratedLogin(email, password)
        } else {
            if (!Util.isOnline(requireActivity())) {
                (requireActivity() as LoginActivity).showSnackbar(requireContext().getFormattedStringOrDefault(
                    R.string.error_server_connection_problem))
                return
            }

            with(binding) {
                loginLayout.isVisible = false
                loginCreateAccountLayout.isVisible = false
                loginLoggingInLayout.isVisible = true
                loginGeneratingKeysText.isVisible = true
                loginProgressBar.isVisible = true
                loginFetchingNodesBar.isVisible = false
                loginQuerySignupLinkText.isVisible = false
                loginConfirmAccountText.isVisible = true
                loginFetchNodesText.isVisible = false
                loginPrepareNodesText.isVisible = false
                loginServersBusyText.isVisible = false
            }

            Timber.d("fastConfirm")
            megaApi.confirmAccount(confirmLink, lastPassword, this)
        }
    }

    /**
     * Returns to login form page.
     */
    private fun backToLoginForm() = with(binding) {
        loginLayout.isVisible = true
        confirmLogoutDialog?.dismiss()
        loginCreateAccountLayout.isVisible = true
        loginLoggingInLayout.isVisible = false
        loginGeneratingKeysText.isVisible = false
        loginProgressBar.isVisible = false
        loginFetchingNodesBar.isVisible = false
        loginQuerySignupLinkText.isVisible = true
        loginConfirmAccountText.isVisible = false
        loginLoggingInText.isVisible = true
        loginFetchNodesText.isVisible = false
        loginPrepareNodesText.isVisible = false
        loginServersBusyText.isVisible = false
        resumeSession = false

        //reset 2fa page
        login2fa.isVisible = false
        progressbarVerify2fa.isVisible = false
        pinFirstLogin.setText("")
        pinSecondLogin.setText("")
        pinThirdLogin.setText("")
        pinFourthLogin.setText("")
        pinFifthLogin.setText("")
        pinSixthLogin.setText("")
        loginEmailText.requestFocus()
    }

    /**
     * Updates the UI and launches the login request if there is network connection.
     *
     * @param email    Typed email.
     * @param password Typed password.
     */
    private fun onKeysGeneratedLogin(email: String?, password: String?) {
        Timber.d("onKeysGeneratedLogin")
        if (!Util.isOnline(requireActivity())) {
            with(binding) {
                loginLoggingInLayout.isVisible = false
                loginLayout.isVisible = true
                confirmLogoutDialog?.dismiss()
                loginCreateAccountLayout.isVisible = true
                loginQuerySignupLinkText.isVisible = false
                loginConfirmAccountText.isVisible = false
                loginGeneratingKeysText.isVisible = false
                loginLoggingInText.isVisible = false
                loginFetchNodesText.isVisible = false
                loginPrepareNodesText.isVisible = false
                loginServersBusyText.isVisible = false
            }

            (requireActivity() as LoginActivity).showSnackbar(requireContext().getFormattedStringOrDefault(
                R.string.error_server_connection_problem))
            return
        }

        if (!isLoggingIn) {
            isLoggingIn = true

            with(binding) {
                loginLoggingInText.isVisible = true
                loginFetchNodesText.isVisible = false
                loginPrepareNodesText.isVisible = false
                loginServersBusyText.isVisible = false
            }

            Timber.d("fastLogin with publicKey & privateKey")
            resumeSession = false
            val initState = megaChatApi.initState
            Timber.d("INIT STATE: %s", initState)
            if (initState == MegaChatApi.INIT_NOT_DONE || initState == MegaChatApi.INIT_ERROR) {
                val ret = megaChatApi.init(null)
                Timber.d("result of init ---> %s", ret)
                when (ret) {
                    MegaChatApi.INIT_WAITING_NEW_SESSION -> {
                        disableLoginButton()
                        megaApi.login(email, password, this)
                    }
                    MegaChatApi.INIT_ERROR -> {
                        Timber.w("ERROR INIT CHAT: %s", ret)
                        megaChatApi.logout(ChatLogoutListener(requireActivity(), loggingSettings))
                        disableLoginButton()
                        megaApi.login(email, password, this)
                    }
                }
            }
        }
    }

    /**
     * Checks if there is a typed email and if has the correct format.
     * Gets the error string if not.
     */
    private val emailError: String?
        get() {
            val value = binding.loginEmailText.text.toString()
            if (value.isEmpty()) {
                return requireContext().getFormattedStringOrDefault(R.string.error_enter_email)
            }
            return if (!Constants.EMAIL_ADDRESS.matcher(value).matches()) {
                requireContext().getFormattedStringOrDefault(R.string.error_invalid_email)
            } else null
        }

    /**
     * Checks if there is a typed email.
     * Gets the error string if not.
     */
    private val passwordError: String?
        get() {
            return if (binding.loginPasswordText.text?.isEmpty() == true) {
                requireContext().getFormattedStringOrDefault(R.string.error_enter_password)
            } else null
        }

    /**
     * Checks if the email and password are typed and has the correct format.
     * Shows errors if not.
     *
     * @return True if the typed data is correct, false otherwise.
     */
    private fun checkTypedValues(): Boolean {
        val emailError = emailError
        val passwordError = passwordError
        setError(binding.loginEmailText, emailError)
        setError(binding.loginPasswordText, passwordError)
        if (emailError != null) {
            binding.loginEmailText.requestFocus()
            return false
        } else if (passwordError != null) {
            binding.loginPasswordText.requestFocus()
            return false
        } else if (Util.existOngoingTransfers(megaApi)) {
            showCancelTransfersDialog()
            return false
        }
        return true
    }

    /**
     * Disables login button.
     */
    private fun disableLoginButton() = with(binding) {
        Timber.d("Disable login button")
        //disable login button
        buttonLogin.isEnabled = false
        //display login info
        pbLoginInProgress.isVisible = true
        textLoginTip.apply {
            isVisible = true
            text = requireContext().getFormattedStringOrDefault(R.string.login_in_progress)
        }
    }

    /**
     * Enables login button.
     */
    private fun enableLoginButton() = with(binding) {
        Timber.d("Enable login button")
        buttonLogin.isEnabled = true
        pbLoginInProgress.isVisible = false
        textLoginTip.isVisible = false
    }


    /**
     * Gets email address from confirmation code and sets to emailView.
     *
     * @param link Link to check.
     */
    private fun updateConfirmEmail(link: String?) {
        if (!Util.isOnline(requireActivity())) {
            (requireActivity() as LoginActivity).showSnackbar(requireContext().getFormattedStringOrDefault(
                R.string.error_server_connection_problem))
            return
        }

        with(binding) {
            loginLayout.isVisible = false
            loginCreateAccountLayout.isVisible = false
            loginLoggingInLayout.isVisible = true
            loginGeneratingKeysText.isVisible = false
            loginQuerySignupLinkText.isVisible = true
            loginConfirmAccountText.isVisible = false
            loginFetchNodesText.isVisible = false
            loginPrepareNodesText.isVisible = false
            loginServersBusyText.isVisible = false
            loginProgressBar.isVisible = true
        }

        Timber.d("querySignupLink")
        megaApi.querySignupLink(link, this)
    }

    /**
     * Handles intent from confirmation email.
     *
     * @param intent Intent.
     */
    private fun handleConfirmationIntent(intent: Intent) {
        confirmLink = intent.getStringExtra(Constants.EXTRA_CONFIRMATION)
        binding.loginTextView.text = requireContext()
            .getFormattedStringOrDefault(R.string.login_confirm_account)
        binding.buttonLogin.text =
            requireContext().getFormattedStringOrDefault(R.string.login_confirm_account)
        updateConfirmEmail(confirmLink)
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        timer?.let {
            it.cancel()
            binding.loginServersBusyText.isVisible = false
        }

        if (request.type == MegaRequest.TYPE_FETCH_NODES) {
            if (firstRequestUpdate) {
                binding.loginProgressBar.isVisible = false
                firstRequestUpdate = false
            }
            binding.loginFetchingNodesBar.apply {
                isVisible = true
                layoutParams.width = Util.dp2px(250 * scaleW)
                if (request.totalBytes > 0) {
                    var progressValue = 100.0 * request.transferredBytes / request.totalBytes
                    if (progressValue > 99 || progressValue < 0) {
                        progressValue = 100.0
                        binding.loginPrepareNodesText.isVisible = true
                        binding.loginProgressBar.isVisible = true
                    }
                    progress = progressValue.toInt()
                }
            }
        }
    }

    /**
     * Launches Manager activity.
     */
    private fun readyToManager() {
        confirmLogoutDialog?.dismiss()
        val loginActivity = requireActivity() as LoginActivity

        if (confirmLink == null && !accountConfirmed) {
            if (getChatManagement().isPendingJoinLink) {
                LoginActivity.isBackFromLoginPage = false
                getChatManagement().pendingJoinLink = null
            }
            Timber.d("confirmLink==null")
            Timber.d("OK fetch nodes")
            Timber.d("value of resumeSession: %s", resumeSession)
            if (action != null && url != null) {
                when (action) {
                    Constants.ACTION_CHANGE_MAIL -> {
                        Timber.d("Action change mail after fetch nodes")
                        val changeMailIntent = Intent(requireContext(), ManagerActivity::class.java)
                        changeMailIntent.action = Constants.ACTION_CHANGE_MAIL
                        changeMailIntent.data = Uri.parse(url)
                        loginActivity.startActivity(changeMailIntent)
                        loginActivity.finish()
                    }
                    Constants.ACTION_RESET_PASS -> {
                        Timber.d("Action reset pass after fetch nodes")
                        val resetPassIntent = Intent(requireContext(), ManagerActivity::class.java)
                        resetPassIntent.action = Constants.ACTION_RESET_PASS
                        resetPassIntent.data = Uri.parse(url)
                        loginActivity.startActivity(resetPassIntent)
                        loginActivity.finish()
                    }
                    Constants.ACTION_CANCEL_ACCOUNT -> {
                        Timber.d("Action cancel Account after fetch nodes")
                        val cancelAccountIntent =
                            Intent(requireContext(), ManagerActivity::class.java)
                        cancelAccountIntent.action = Constants.ACTION_CANCEL_ACCOUNT
                        cancelAccountIntent.data = Uri.parse(url)
                        loginActivity.startActivity(cancelAccountIntent)
                        loginActivity.finish()
                    }
                }
            }
            if (!backWhileLogin) {
                Timber.d("NOT backWhileLogin")
                if (parentHandle != -1L) {
                    val intent = Intent()
                    intent.putExtra("PARENT_HANDLE", parentHandle)
                    loginActivity.setResult(Activity.RESULT_OK, intent)
                    loginActivity.finish()
                } else {
                    var intent: Intent?
                    if (firstTime) {
                        Timber.d("First time")
                        intent = Intent(requireContext(), ManagerActivity::class.java)
                        intent.putExtra(IntentConstants.EXTRA_FIRST_LOGIN, true)
                        setStartScreenTimeStamp(requireContext())
                        if (action != null) {
                            Timber.d("Action not NULL")
                            if (action == Constants.ACTION_EXPORT_MASTER_KEY) {
                                Timber.d("ACTION_EXPORT_MK")
                                intent.action = action
                            } else if (action == Constants.ACTION_JOIN_OPEN_CHAT_LINK && url != null) {
                                intent.action = action
                                intent.data = Uri.parse(url)
                            }
                        }
                    } else {
                        var initialCam = false
                        val prefs = dbH.preferences
                        if (prefs != null) {
                            if (prefs.camSyncEnabled != null) {
                                if (java.lang.Boolean.parseBoolean(prefs.camSyncEnabled)) {
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                        startCameraUploadService(false, 30 * 1000)
                                    }
                                }
                            } else {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                    startCameraUploadService(true, 30 * 1000)
                                }
                                initialCam = true
                            }
                        } else {
                            intent = Intent(requireContext(), ManagerActivity::class.java)
                            intent.putExtra(IntentConstants.EXTRA_FIRST_LOGIN, true)
                            initialCam = true
                            setStartScreenTimeStamp(requireContext())
                        }
                        if (!initialCam) {
                            Timber.d("NOT initialCam")
                            intent = Intent(requireContext(), ManagerActivity::class.java)
                            if (action != null) {
                                Timber.d("The action is: %s", action)
                                when (action) {
                                    Constants.ACTION_FILE_PROVIDER -> {
                                        intent = Intent(requireContext(),
                                            FileProviderActivity::class.java)
                                        extras?.let { intent?.putExtras(it) }
                                        if (uriData != null) {
                                            intent.data = uriData
                                        }
                                    }
                                    Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL -> {
                                        intent =
                                            Intent(requireContext(), FileLinkActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        intent.data = uriData
                                    }
                                    Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL -> {
                                        intent =
                                            Intent(requireContext(), FolderLinkActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        action = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                                        intent.data = uriData
                                    }
                                    Constants.ACTION_OPEN_CONTACTS_SECTION -> intent.putExtra(
                                        Constants.CONTACT_HANDLE, intentReceived?.getLongExtra(
                                            Constants.CONTACT_HANDLE, -1))
                                }
                                intent.action = action
                                if (url != null) {
                                    intent.data = Uri.parse(url)
                                }
                            } else {
                                Timber.w("The intent action is NULL")
                            }
                        } else {
                            Timber.d("initialCam YES")
                            intent = Intent(requireContext(), ManagerActivity::class.java)
                            if (action != null) {
                                Timber.d("The action is: %s", action)
                                intent.action = action
                            }
                            if (url != null) {
                                intent.data = Uri.parse(url)
                            }
                        }
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    if (twoFA) {
                        intent.action = Constants.ACTION_REFRESH_API_SERVER
                        twoFA = false
                    }
                    if (action != null && action == Constants.ACTION_REFRESH_AFTER_BLOCKED) {
                        intent.action = Constants.ACTION_REFRESH_AFTER_BLOCKED
                    }
                    if (viewModel.getStorageState() === StorageState.PayWall) {
                        showOverDiskQuotaPaywallWarning(true)
                    } else {
                        loginActivity.startActivity(intent)
                    }
                    loginActivity.finish()
                }
            }
        } else {
            Timber.d("Go to ChooseAccountFragment")
            accountConfirmed = false
            if (getChatManagement().isPendingJoinLink) {
                LoginActivity.isBackFromLoginPage = false
                val intent = Intent(requireContext(), ManagerActivity::class.java)
                intent.action = Constants.ACTION_JOIN_OPEN_CHAT_LINK
                intent.data = Uri.parse(getChatManagement().pendingJoinLink)
                startActivity(intent)
                getChatManagement().pendingJoinLink = null
                loginActivity.finish()
            } else if (dbH.credentials != null) {
                startActivity(Intent(loginActivity, ChooseAccountActivity::class.java))
                loginActivity.finish()
            }
        }
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestStart: %s", request.requestString)
        if (request.type == MegaRequest.TYPE_LOGIN) {
            disableLoginButton()
        }
        if (request.type == MegaRequest.TYPE_FETCH_NODES) {
            binding.loginFetchingNodesBar.apply {
                isVisible = true
                layoutParams.width = Util.dp2px(250 * scaleW)
                progress = 0
            }
            isFetchingNodes = true
            disableLoginButton()
        }
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, error: MegaError) {
        enableLoginButton()
        timer?.let {
            it.cancel()
            binding.loginServersBusyText.isVisible = false
        }
        Timber.d("onRequestFinish: %s,error code: %d", request.requestString, error.errorCode)
        if (request.type == MegaRequest.TYPE_LOGIN) {
            //cancel login process by press back.
            if (!isLoggingIn) {
                Timber.w("Terminate login process when login")
                return
            }
            if (error.errorCode != MegaError.API_OK) {
                isLoggingIn = false
                confirmLogoutDialog?.dismiss()
                enableLoginButton()
                val errorMessage: String
                if (error.errorCode == MegaError.API_ESID) {
                    Timber.w("MegaError.API_ESID %s",
                        requireContext().getFormattedStringOrDefault(R.string.error_server_expired_session))
                    (requireActivity() as LoginActivity).showAlertLoggedOut()
                } else if (error.errorCode == MegaError.API_EMFAREQUIRED) {
                    Timber.d("require 2fa")
                    is2FAEnabled = true

                    with(binding) {
                        (requireActivity() as LoginActivity).showAB(toolbarLogin)
                        loginLayout.isVisible = false
                        loginCreateAccountLayout.isVisible = false
                        loginLoggingInLayout.isVisible = false
                        loginGeneratingKeysText.isVisible = false
                        loginProgressBar.isVisible = false
                        loginFetchingNodesBar.isVisible = false
                        loginQuerySignupLinkText.isVisible = false
                        loginConfirmAccountText.isVisible = false
                        loginFetchNodesText.isVisible = false
                        loginPrepareNodesText.isVisible = false
                        loginServersBusyText.isVisible = false
                        login2fa.isVisible = true
                        confirmLogoutDialog?.dismiss()
                        pinFirstLogin.apply {
                            requestFocus()
                            isCursorVisible = true
                        }
                    }
                    return
                } else if (error.errorCode == MegaError.API_EFAILED || error.errorCode == MegaError.API_EEXPIRED) {
                    binding.progressbarVerify2fa.isVisible = false
                    showShowError()
                    return
                } else {
                    errorMessage = when (error.errorCode) {
                        MegaError.API_ENOENT -> {
                            requireContext().getFormattedStringOrDefault(R.string.error_incorrect_email_or_password)
                        }
                        MegaError.API_ETOOMANY -> {
                            requireContext().getFormattedStringOrDefault(R.string.too_many_attempts_login)
                        }
                        MegaError.API_EINCOMPLETE -> {
                            requireContext().getFormattedStringOrDefault(R.string.account_not_validated_login)
                        }
                        MegaError.API_EACCESS -> {
                            error.errorString
                        }
                        MegaError.API_EBLOCKED -> {
                            //It will processed at the `onEvent` when receive an EVENT_ACCOUNT_BLOCKED
                            Timber.w("Suspended account - Reason: %s", request.number)
                            return
                        }
                        else -> {
                            error.errorString
                        }
                    }
                    Timber.e("LOGIN_ERROR: %d %s", error.errorCode, error.errorString)
                    megaChatApi.logout(ChatLogoutListener(requireActivity(), loggingSettings))
                    if (errorMessage.isNotEmpty()) {
                        if (!backWhileLogin) {
                            (requireActivity() as LoginActivity).showSnackbar(errorMessage)
                        }
                    }
                    if (chatSettings == null) {
                        Timber.d("Reset chat setting enable")
                        chatSettings = ChatSettings()
                        dbH.chatSettings = chatSettings
                    }
                }
                returnToLogin()
            } else {
                Timber.d("Logged in. Setting account auth token for folder links.")
                megaApiFolder.accountAuth = megaApi.accountAuth

                with(binding) {
                    if (is2FAEnabled) {
                        login2fa.isVisible = false
                        (requireActivity() as LoginActivity).hideAB()
                    }
                    loginLayout.isVisible = false
                    loginLoggingInLayout.isVisible = true
                    loginProgressBar.isVisible = true
                    loginFetchingNodesBar.isVisible = false
                    loginLoggingInText.isVisible = true
                    loginFetchNodesText.isVisible = true
                    loginPrepareNodesText.isVisible = false
                    loginServersBusyText.isVisible = false
                }

                saveCredentials()
                Timber.d("Logged in with session")
                dbH.clearEphemeral()
                megaApi.fetchNodes(this)

                // Get cookies settings after login.
                getInstance().checkEnabledCookies()
            }
        } else if (request.type == MegaRequest.TYPE_LOGOUT) {
            Timber.d("TYPE_LOGOUT")
            if (error.errorCode == MegaError.API_OK) {
                localLogoutApp(requireActivity(), sharingScope)
            }
        } else if (request.type == MegaRequest.TYPE_GET_RECOVERY_LINK) {
            Timber.d("TYPE_GET_RECOVERY_LINK")
            when (error.errorCode) {
                MegaError.API_OK -> {
                    Timber.d("The recovery link has been sent")
                    Util.showAlert(requireContext(),
                        requireContext().getFormattedStringOrDefault(R.string.email_verification_text),
                        requireContext().getFormattedStringOrDefault(R.string.email_verification_title))
                }
                MegaError.API_ENOENT -> {
                    Timber.e("No account with this mail: %s %d", error.errorString, error.errorCode)
                    Util.showAlert(requireContext(),
                        requireContext().getFormattedStringOrDefault(R.string.invalid_email_text),
                        requireContext().getFormattedStringOrDefault(R.string.invalid_email_title))
                }
                else -> {
                    Timber.e("Error when asking for recovery pass link \n %s__%d",
                        error.errorString,
                        error.errorCode)
                    Util.showAlert(requireContext(),
                        requireContext().getFormattedStringOrDefault(R.string.general_text_error),
                        requireContext().getFormattedStringOrDefault(R.string.general_error_word))
                }
            }
        } else if (request.type == MegaRequest.TYPE_FETCH_NODES) {
            //cancel login process by press back.
            if (!isLoggingIn) {
                Timber.d("Terminate login process when fetch nodes")
                return
            }
            isFetchingNodes = false
            isLoggingIn = false
            if (error.errorCode == MegaError.API_OK) {
                receivedIntent = (requireActivity() as LoginActivity).intent
                if (receivedIntent != null) {
                    @Suppress("UNCHECKED_CAST")
                    shareInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intentReceived?.getSerializableExtra(FileExplorerActivity.EXTRA_SHARE_INFOS,
                            ArrayList::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intentReceived?.getSerializableExtra(FileExplorerActivity.EXTRA_SHARE_INFOS)
                    } as ArrayList<ShareInfo>?

                    if (shareInfos?.isNotEmpty() == true) {
                        val permissions = arrayOf(
                            getImagePermissionByVersion(),
                            getAudioPermissionByVersion(),
                            getVideoPermissionByVersion(),
                            getReadExternalStoragePermission()
                        )
                        if (hasPermissions(requireContext(), *permissions)) {
                            toSharePage()
                        } else {
                            requestMediaPermission.launch(permissions)
                        }
                        return
                    } else if (Constants.ACTION_FILE_EXPLORER_UPLOAD == action && Constants.TYPE_TEXT_PLAIN == receivedIntent?.type) {
                        startActivity(Intent(requireContext(), FileExplorerActivity::class.java)
                            .putExtra(Intent.EXTRA_TEXT,
                                receivedIntent?.getStringExtra(Intent.EXTRA_TEXT))
                            .putExtra(Intent.EXTRA_SUBJECT,
                                receivedIntent?.getStringExtra(Intent.EXTRA_SUBJECT))
                            .putExtra(Intent.EXTRA_EMAIL,
                                receivedIntent?.getStringExtra(Intent.EXTRA_EMAIL))
                            .setAction(Intent.ACTION_SEND)
                            .setType(Constants.TYPE_TEXT_PLAIN))
                        requireActivity().finish()
                        return
                    } else if (Constants.ACTION_REFRESH == action && activity != null) {
                        requireActivity().apply {
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                        return
                    }
                }
                readyToManager()
            } else {
                confirmLogoutDialog?.dismiss()
                enableLoginButton()
                Timber.e("Error fetch nodes: %s", error.errorCode)

                with(binding) {
                    loginLoggingInLayout.isVisible = false
                    loginLayout.isVisible = true
                    confirmLogoutDialog?.dismiss()
                    loginCreateAccountLayout.isVisible = true
                    loginGeneratingKeysText.isVisible = false
                    loginLoggingInText.isVisible = false
                    loginFetchNodesText.isVisible = false
                    loginPrepareNodesText.isVisible = false
                    loginServersBusyText.isVisible = false
                    loginQuerySignupLinkText.isVisible = false
                    loginConfirmAccountText.isVisible = false
                }

                if (error.errorCode == MegaError.API_EACCESS) {
                    Timber.e("Error API_EACCESS")
                    if (!backWhileLogin) {
                        (requireActivity() as LoginActivity).showSnackbar(error.errorString)
                    }
                } else if (error.errorCode != MegaError.API_EBLOCKED) {
                    //It will processed at the `onEvent` when receive an EVENT_ACCOUNT_BLOCKED
                    Timber.w("Suspended account - Reason: %s", request.number)
                    return
                }

                if (chatSettings == null) {
                    Timber.d("Reset chat setting enable")
                    chatSettings = ChatSettings()
                    dbH.chatSettings = chatSettings
                }
            }
        } else if (request.type == MegaRequest.TYPE_QUERY_SIGNUP_LINK) {
            Timber.d("MegaRequest.TYPE_QUERY_SIGNUP_LINK")

            with(binding) {
                loginLayout.isVisible = true
                confirmLogoutDialog?.dismiss()
                buttonForgotPass.isInvisible = true
                loginCreateAccountLayout.isVisible = true
                loginLoggingInLayout.isVisible = false
                loginGeneratingKeysText.isVisible = false
                loginQuerySignupLinkText.isVisible = false
                loginConfirmAccountText.isVisible = false
                loginFetchNodesText.isVisible = false
                loginPrepareNodesText.isVisible = false
                loginServersBusyText.isVisible = false
            }

            if (error.errorCode == MegaError.API_OK) {
                Timber.d("MegaRequest.TYPE_QUERY_SIGNUP_LINK MegaError API_OK")
                if (request.flag) {
                    with(binding) {
                        buttonForgotPass.isInvisible = false
                        loginProgressBar.isVisible = false
                        loginTextView.text =
                            requireContext().getFormattedStringOrDefault(R.string.login_to_mega)
                        buttonLogin.text =
                            requireContext().getFormattedStringOrDefault(R.string.login_text)
                    }
                    confirmLink = null
                    (requireActivity() as LoginActivity).showSnackbar(requireContext().getFormattedStringOrDefault(
                        R.string.account_confirmed))
                    accountConfirmed = true
                } else {
                    accountConfirmed = false
                    (requireActivity() as LoginActivity).showSnackbar(requireContext().getFormattedStringOrDefault(
                        R.string.confirm_account))
                }
                binding.loginEmailText.setText(request.email)
                binding.loginPasswordText.requestFocus()
            } else {
                Timber.w("MegaRequest.TYPE_QUERY_SIGNUP_LINK MegaError not API_OK %s",
                    error.errorCode)
                val loginActivity = requireActivity() as LoginActivity
                if (error.errorCode == MegaError.API_ENOENT) {
                    loginActivity.showSnackbar(requireContext().getFormattedStringOrDefault(R.string.reg_link_expired))
                } else {
                    loginActivity.showSnackbar(error.errorString)
                }
                confirmLink = null
            }
        } else if (request.type == MegaRequest.TYPE_CONFIRM_ACCOUNT) {
            if (error.errorCode == MegaError.API_OK) {
                Timber.d("fastConfirm finished - OK")
                onKeysGeneratedLogin(lastEmail, lastPassword)
            } else {
                with(binding) {
                    loginLayout.isVisible = true
                    confirmLogoutDialog?.dismiss()
                    loginCreateAccountLayout.isVisible = true
                    loginLoggingInLayout.isVisible = false
                    loginGeneratingKeysText.isVisible = false
                    loginQuerySignupLinkText.isVisible = false
                    loginConfirmAccountText.isVisible = false
                    loginFetchNodesText.isVisible = false
                    loginPrepareNodesText.isVisible = false
                    loginServersBusyText.isVisible = false
                }
                if (error.errorCode == MegaError.API_ENOENT || error.errorCode == MegaError.API_EKEY) {
                    (requireActivity() as LoginActivity).showSnackbar(requireContext().getFormattedStringOrDefault(
                        R.string.error_incorrect_email_or_password))
                } else {
                    (requireActivity() as LoginActivity).showSnackbar(error.errorString)
                }
            }
        }
    }

    /**
     * Launches an intent to [FileExplorerActivity]
     */
    private fun toSharePage() {
        val shareIntent = Intent(requireContext(), FileExplorerActivity::class.java)
        shareIntent.putExtra(FileExplorerActivity.EXTRA_SHARE_INFOS, shareInfos)
        shareIntent.action =
            receivedIntent?.getStringExtra(FileExplorerActivity.EXTRA_SHARE_ACTION)
        shareIntent.type = receivedIntent?.getStringExtra(FileExplorerActivity.EXTRA_SHARE_TYPE)
        startActivity(shareIntent)
        (requireActivity() as LoginActivity).finish()
    }

    /**
     * Saves credentials.
     */
    private fun saveCredentials() {
        gSession = megaApi.dumpSession()
        val myUser = megaApi.myUser
        var myUserHandle = ""
        if (myUser != null) {
            lastEmail = megaApi.myUser.email
            myUserHandle = megaApi.myUser.handle.toString() + ""
        }
        val credentials = UserCredentials(lastEmail, gSession, "", "", myUserHandle)
        dbH.saveCredentials(credentials)
    }

    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.w("onRequestTemporaryError: %s%d", request.requestString, e.errorCode)
        timer = object : CountDownTimer(10000, 2000) {
            override fun onTick(millisUntilFinished: Long) {
                Timber.d("TemporaryError one more")
            }

            override fun onFinish() {
                Timber.d("The timer finished, message shown")
                binding.loginServersBusyText.apply {
                    isVisible = true
                    text = requireContext().getFormattedStringOrDefault(
                        if (e.errorCode == MegaError.API_EAGAIN) {
                            Timber.w("onRequestTemporaryError:onFinish:API_EAGAIN: :value: %s",
                                e.value)
                            when (e.value) {
                                MegaApiJava.RETRY_CONNECTIVITY.toLong() -> {
                                    binding.textLoginTip.text =
                                        requireContext().getFormattedStringOrDefault(R.string.login_connectivity_issues)
                                    R.string.login_connectivity_issues
                                }
                                MegaApiJava.RETRY_SERVERS_BUSY.toLong() -> {
                                    binding.textLoginTip.text =
                                        requireContext().getFormattedStringOrDefault(R.string.login_servers_busy)
                                    R.string.login_servers_busy
                                }
                                MegaApiJava.RETRY_API_LOCK.toLong() -> {
                                    binding.textLoginTip.text =
                                        requireContext().getFormattedStringOrDefault(R.string.login_API_lock)
                                    R.string.login_API_lock
                                }
                                MegaApiJava.RETRY_RATE_LIMIT.toLong() -> {
                                    binding.textLoginTip.text =
                                        requireContext().getFormattedStringOrDefault(R.string.login_API_rate)
                                    R.string.login_API_rate
                                }
                                else -> {
                                    binding.textLoginTip.text =
                                        requireContext().getFormattedStringOrDefault(R.string.servers_busy)
                                    R.string.servers_busy
                                }
                            }
                        } else {
                            binding.textLoginTip.text =
                                requireContext().getFormattedStringOrDefault(R.string.servers_busy)
                            R.string.servers_busy
                        }
                    )
                }
            }
        }.start()
    }

    /**
     * Shows a dialog for changing password.
     *
     * @param link Reset password link.
     */
    private fun showDialogInsertMKToChangePass(link: String?) {
        Timber.d("link: %s", link)
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(Util.scaleWidthPx(20, resources.displayMetrics),
            Util.scaleHeightPx(20, resources.displayMetrics),
            Util.scaleWidthPx(17, resources.displayMetrics),
            0)
        val input = EditText(requireContext())
        layout.addView(input, params)
        input.setSingleLine()
        input.hint = requireContext().getFormattedStringOrDefault(R.string.edit_text_insert_mk)
        input.setTextColor(getThemeColor(requireContext(), android.R.attr.textColorSecondary))
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        input.imeOptions = EditorInfo.IME_ACTION_DONE
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                Timber.d("IME OK BUTTON PASSWORD")
                val value = input.text.toString().trim { it <= ' ' }
                if (value == "" || value.isEmpty()) {
                    Timber.w("Input is empty")
                    input.error =
                        requireContext().getFormattedStringOrDefault(R.string.invalid_string)
                    input.requestFocus()
                } else {
                    Timber.d("Positive button pressed - reset pass")
                    val intent = Intent(requireContext(), ChangePasswordActivity::class.java)
                    intent.action = Constants.ACTION_RESET_PASS_FROM_LINK
                    intent.data = Uri.parse(link)
                    intent.putExtra(IntentConstants.EXTRA_MASTER_KEY, value)
                    startActivity(intent)
                    insertMKDialog!!.dismiss()
                }
            } else {
                Timber.d("Other IME%s", actionId)
            }
            false
        }
        input.setImeActionLabel(requireContext().getFormattedStringOrDefault(R.string.general_add),
            EditorInfo.IME_ACTION_DONE)
        val builder = MaterialAlertDialogBuilder(
            requireContext(), R.style.ThemeOverlay_Mega_MaterialAlertDialog)
        builder.setTitle(requireContext().getFormattedStringOrDefault(R.string.title_dialog_insert_MK))
        builder.setMessage(requireContext().getFormattedStringOrDefault(R.string.text_dialog_insert_MK))
        builder.setPositiveButton(requireContext().getFormattedStringOrDefault(R.string.general_ok)
        ) { _, _ -> }
        builder.setOnDismissListener {
            Util.hideKeyboard(requireActivity() as LoginActivity,
                InputMethodManager.HIDE_NOT_ALWAYS)
        }
        builder.setNegativeButton(requireContext().getFormattedStringOrDefault(R.string.general_cancel),
            null)
        builder.setView(layout)
        insertMKDialog = builder.create().apply {
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                Timber.d("OK BUTTON PASSWORD")
                val value = input.text.toString().trim { it <= ' ' }
                if (value == "" || value.isEmpty()) {
                    Timber.w("Input is empty")
                    input.error =
                        requireContext().getFormattedStringOrDefault(R.string.invalid_string)
                    input.requestFocus()
                } else {
                    Timber.d("Positive button pressed - reset pass")
                    val intent = Intent(requireContext(), ChangePasswordActivity::class.java)
                    intent.action = Constants.ACTION_RESET_PASS_FROM_LINK
                    intent.data = Uri.parse(link)
                    intent.putExtra(IntentConstants.EXTRA_MASTER_KEY, value)
                    startActivity(intent)
                    dismiss()
                }
            }
        }
    }

    override fun onDestroy() {
        megaApi.removeRequestListener(this)
        confirmLogoutDialog?.dismiss()
        changeApiServerDialog?.dismiss()
        super.onDestroy()
    }

    /**
     * Shows a confirmation dialog before cancelling the current in progress login.
     */
    private fun showConfirmLogoutDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val dialogClickListener =
            DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        backToLoginForm()
                        backWhileLogin = true
                        isLoggingIn = false
                        isFetchingNodes = false
                        loginClicked = false
                        firstTime = true
                        megaChatApi.logout(ChatLogoutListener(requireActivity(), loggingSettings))
                        megaApi.localLogout(this@LoginFragment)
                    }
                    DialogInterface.BUTTON_NEGATIVE -> dialog.dismiss()
                }
            }
        val message = requireContext().getFormattedStringOrDefault(R.string.confirm_cancel_login)
        confirmLogoutDialog = builder.setCancelable(true)
            .setMessage(message)
            .setPositiveButton(requireContext().getFormattedStringOrDefault(R.string.general_positive_button),
                dialogClickListener)
            .setNegativeButton(requireContext().getFormattedStringOrDefault(R.string.general_negative_button),
                dialogClickListener)
            .show()
    }

    /**
     * Performs on back pressed.
     */
    fun onBackPressed(): Int {
        Timber.d("onBackPressed")
        //refresh, point to staging server, enable chat. block the back button
        if (Constants.ACTION_REFRESH == action || Constants.ACTION_REFRESH_API_SERVER == action) {
            return -1
        }
        //login is in process
        val onLoginPage = binding.loginLayout.isVisible
        val on2faPage = binding.login2fa.isVisible
        return if ((isLoggingIn || isFetchingNodes) && !onLoginPage && !on2faPage) {
            showConfirmLogoutDialog()
            2
        } else {
            if (on2faPage) {
                Timber.d("Back from 2fa page")
                showConfirmLogoutDialog()
                return 2
            }

            LoginActivity.isBackFromLoginPage = true
            (requireActivity() as LoginActivity).showFragment(Constants.TOUR_FRAGMENT)
            1
        }
    }

    /**
     * Shows an error in some field.
     *
     * @param editText The field in which the error has to be shown.
     * @param error    The error to show.
     */
    private fun setError(editText: EditText, error: String?) {
        if (error.isNullOrEmpty()) {
            return
        }

        with(binding) {
            when (editText.id) {
                R.id.login_email_text -> {
                    loginEmailTextLayout.apply {
                        setError(error)
                        setHintTextAppearance(R.style.TextAppearance_InputHint_Error)
                    }
                    loginEmailTextErrorIcon.isVisible = true
                }
                R.id.login_password_text -> {
                    loginPasswordTextLayout.apply {
                        setError(error)
                        setHintTextAppearance(R.style.TextAppearance_InputHint_Error)
                    }
                    loginPasswordTextErrorIcon.isVisible = true
                }
            }
        }
    }

    /**
     * Hides the error from some field.
     *
     * @param editText The field in which the error has to be hidden.
     */
    private fun quitError(editText: EditText) = with(binding) {
        when (editText.id) {
            R.id.login_email_text -> {
                loginEmailTextLayout.apply {
                    error = null
                    setHintTextAppearance(R.style.TextAppearance_Design_Hint)
                }
                loginEmailTextErrorIcon.isVisible = false
            }
            R.id.login_password_text -> {
                loginPasswordTextLayout.apply {
                    error = null
                    setHintTextAppearance(R.style.TextAppearance_Design_Hint)
                }
                loginPasswordTextErrorIcon.isVisible = false
            }
        }
    }

    /**
     * Pastes a code for 2FA.
     */
    private fun pasteClipboard() {
        Timber.d("pasteClipboard")
        pinLongClick = false
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        clipboard.primaryClip?.let {
            val code = it.getItemAt(0).text.toString()
            Timber.d("code: %s", code)
            if (code.length == 6) {
                var areDigits = true
                for (i in 0..5) {
                    if (!Character.isDigit(code[i])) {
                        areDigits = false
                        break
                    }
                }

                with(binding) {
                    if (areDigits) {
                        pinFirstLogin.setText("" + code[0])
                        pinSecondLogin.setText("" + code[1])
                        pinThirdLogin.setText("" + code[2])
                        pinFourthLogin.setText("" + code[3])
                        pinFifthLogin.setText("" + code[4])
                        pinSixthLogin.setText("" + code[5])
                    } else {
                        pinFirstLogin.setText("")
                        pinSecondLogin.setText("")
                        pinThirdLogin.setText("")
                        pinFourthLogin.setText("")
                        pinFifthLogin.setText("")
                        pinSixthLogin.setText("")
                    }
                }
            }
        }
    }

    /**
     * Shows the cancel transfers dialog.
     */
    private fun showCancelTransfersDialog() = AlertDialog.Builder(requireContext()).apply {
        setMessage(R.string.login_warning_abort_transfers)
        setPositiveButton(R.string.login_text) { _, _ ->
            megaApi.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD)
            megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD)
            performLogin()
        }
        setNegativeButton(R.string.general_cancel, null)
        setCancelable(false)
        show()
    }

    /**
     * Starts CU service.
     *
     * @param firstTimeCam
     * @param time
     */
    private fun startCameraUploadService(firstTimeCam: Boolean, time: Int) {
        Timber.d("firstTimeCam: $firstTimeCam: $time")

        with(requireActivity()) {
            if (firstTimeCam) {
                setStartScreenTimeStamp(this)

                startActivity(Intent(this,
                    ManagerActivity::class.java).apply {
                    putExtra(IntentConstants.EXTRA_FIRST_LOGIN, true)
                })

                finish()
            } else {
                Timber.d("Start the Camera Uploads service")
                Handler(Looper.getMainLooper()).postDelayed({
                    Timber.d("Now I start the service")
                    JobUtil.scheduleCameraUploadJob(this)
                }, time.toLong())
            }
        }
    }

    companion object {
        private const val LONG_CLICK_DELAY: Long = 5000
    }
}