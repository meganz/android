package mega.privacy.android.app.presentation.login

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
import androidx.fragment.app.activityViewModels
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
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.FileLinkActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.presentation.extensions.messageId
import mega.privacy.android.app.presentation.folderlink.FolderLinkActivity
import mega.privacy.android.app.presentation.login.LoginActivity.Companion.ACTION_FORCE_RELOAD_ACCOUNT
import mega.privacy.android.app.presentation.login.LoginActivity.Companion.ACTION_OPEN_APP
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
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.exception.QuerySignupLinkException
import mega.privacy.android.domain.qualifier.ApplicationScope
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber
import javax.inject.Inject

/**
 * Login fragment.
 *
 * @property sharingScope         [CoroutineScope]
 * @property loggingSettings      [LegacyLoggingSettings]
 * @property megaApi              [MegaApiAndroid]
 * @property megaApiFolder        [MegaApiAndroid] used for folder links management.
 * @property megaChatApi          [MegaChatApiAndroid]
 */
@AndroidEntryPoint
class LoginFragment : Fragment(), MegaRequestListenerInterface {

    @ApplicationScope
    @Inject
    lateinit var sharingScope: CoroutineScope

    @Inject
    lateinit var loggingSettings: LegacyLoggingSettings

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @MegaApiFolder
    @Inject
    lateinit var megaApiFolder: MegaApiAndroid

    @Inject
    lateinit var megaChatApi: MegaChatApiAndroid

    private val viewModel: LoginViewModel by activityViewModels()

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private var insertMKDialog: AlertDialog? = null
    private var changeApiServerDialog: AlertDialog? = null
    private var confirmLogoutDialog: AlertDialog? = null

    private var loginTitleBoundaries: Rect? = null
    private var timer: CountDownTimer? = null
    private var intentExtras: Bundle? = null
    private var intentData: Uri? = null
    private var intentAction: String? = null
    private var intentDataString: String? = null
    private var intentParentHandle: Long = -1
    private var intentShareInfo: ArrayList<ShareInfo>? = null
    private val sb by lazy { StringBuilder() }
    private val scaleW by lazy {
        Util.getScaleW(resources.displayMetrics, resources.displayMetrics.density)
    }
    private val pin2FAColor by lazy {
        ContextCompat.getColor(requireContext(), R.color.grey_087_white_087)
    }
    private val pin2FAErrorColor by lazy {
        ContextCompat.getColor(requireContext(), R.color.red_600_red_300)
    }

    private val requestMediaPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.values.contains(false)) {
                readyToManager()
            } else {
                toSharePage()
            }
        }

    private val uiState
        get() = viewModel.state.value

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("onCreateView")
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setupInitialState()
        setupView()
        setupIntent()
        setupObservers()
    }

    private fun setupObservers() = with(viewModel) {
        onQuerySignupLinkFinished().observe(viewLifecycleOwner, ::showQuerySignupLinkResult)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupView() = with(binding) {
        loginTextView.apply {
            text = requireContext().getFormattedStringOrDefault(R.string.login_to_mega)
            setOnClickListener { viewModel.clickKarereLogs(requireActivity()) }
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
                        loginTitleBoundaries = Rect(view.left, view.top, view.right, view.bottom)
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
                viewModel.updatePressedBackWhileLogin(false)
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

        textNewToMega.setOnClickListener { viewModel.clickSDKLogs(requireActivity()) }

        buttonCreateAccountLogin.setOnClickListener {
            Timber.d("Click on button_create_account_login")
            (requireActivity() as LoginActivity).showFragment(Constants.CREATE_ACCOUNT_FRAGMENT)
        }

        showLoginScreen()

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

        pinSecondLogin.previousDigitEditText = pinFirstLogin
        pinThirdLogin.previousDigitEditText = pinSecondLogin
        pinFourthLogin.previousDigitEditText = pinThirdLogin
        pinFifthLogin.previousDigitEditText = pinFourthLogin
        pinSixthLogin.previousDigitEditText = pinFifthLogin

        if (viewModel.areThereValidTemporalCredentials()) {
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
                viewModel.updateIsPinLongClick(true)
                requestFocus()
                return@setOnLongClickListener false
            }
            onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (hasFocus) setText("")
            }
            doAfterTextChanged {
                if (length() != 0) {
                    requestEditTextPinFocus(with(binding) {
                        when (id) {
                            R.id.pin_first_login -> pinSecondLogin
                            R.id.pin_second_login -> pinThirdLogin
                            R.id.pin_third_login -> pinFourthLogin
                            R.id.pin_fourth_login -> pinFifthLogin
                            else -> pinSixthLogin
                        }
                    })

                    if (id == R.id.pin_sixth_login) hideKeyboard()

                    when {
                        areAllFieldsFilled() -> verify2FA()
                        !uiState.was2FAErrorShown && !uiState.isPinLongClick -> clear2FAFields(id)
                        uiState.isPinLongClick -> pasteClipboard()
                    }
                } else if (uiState.is2FAErrorShown) {
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
    private fun requestEditTextPinFocus(editTextPIN: EditTextPIN) = editTextPIN.apply {
        requestFocus()
        isCursorVisible = true
    }

    /**
     * Gets data from the intent and performs the corresponding action if necessary.
     */
    private fun setupIntent() = (requireActivity() as LoginActivity).intent?.let { intent ->
        intentAction = intent.action

        intentAction?.let { action ->
            Timber.d("action is: %s", action)
            when (action) {
                Constants.ACTION_CONFIRM -> {
                    handleConfirmationIntent(intent)
                    return
                }
                Constants.ACTION_RESET_PASS -> {
                    val link = intent.dataString
                    if (link != null) {
                        Timber.d("Link to resetPass: %s", link)
                        showDialogInsertMKToChangePass(link)
                        return
                    }
                }
                Constants.ACTION_PASS_CHANGED -> {
                    when (intent.getIntExtra(Constants.RESULT, MegaError.API_OK)) {
                        MegaError.API_OK -> showMessage(R.string.pass_changed_alert)
                        MegaError.API_EKEY -> showAlertIncorrectRK()
                        MegaError.API_EBLOCKED -> showMessage(R.string.error_reset_account_blocked)
                        else -> showMessage(R.string.general_text_error)
                    }
                    return
                }
                Constants.ACTION_SHOW_WARNING_ACCOUNT_BLOCKED -> {
                    val accountBlockedString =
                        intent.getStringExtra(Constants.ACCOUNT_BLOCKED_STRING)
                    if (!TextUtil.isTextEmpty(accountBlockedString)) {
                        Util.showErrorAlertDialog(accountBlockedString, false, activity)
                    }
                }
                ACTION_FORCE_RELOAD_ACCOUNT -> {
                    viewModel.setIntentAction(ACTION_FORCE_RELOAD_ACCOUNT)
                    isLoggingIn = true
                    showLoggingInScreen()
                    return
                }
            }
        } ?: Timber.w("ACTION NULL")

        Timber.d("et_user.getText(): %s", binding.loginEmailText.text)

        if (uiState.isAlreadyLoggedIn && !LoginActivity.isBackFromLoginPage) {
            Timber.d("Credentials NOT null")

            intentAction?.let { action ->
                when (action) {
                    Constants.ACTION_REFRESH -> {
                        isLoggingIn = true
                        startFetchNodes()
                        return
                    }
                    Constants.ACTION_REFRESH_API_SERVER -> {
                        viewModel.updateIsRefreshApiServer(true)
                        intentParentHandle = intent.getLongExtra("PARENT_HANDLE", -1)
                        startFastLogin()
                        return
                    }
                    Constants.ACTION_REFRESH_AFTER_BLOCKED -> {
                        startFastLogin()
                        return
                    }
                    else -> {
                        Timber.d("intent received $action")
                        when (action) {
                            Constants.ACTION_OPEN_MEGA_FOLDER_LINK,
                            Constants.ACTION_IMPORT_LINK_FETCH_NODES,
                            Constants.ACTION_CHANGE_MAIL,
                            Constants.ACTION_CANCEL_ACCOUNT,
                            Constants.ACTION_OPEN_HANDLE_NODE,
                            Constants.ACTION_OPEN_CHAT_LINK,
                            Constants.ACTION_JOIN_OPEN_CHAT_LINK,
                            -> {
                                intentDataString = intent.dataString
                            }
                            Constants.ACTION_FILE_PROVIDER -> {
                                intentData = intent.data
                                intentExtras = intent.extras
                                intentDataString = null
                            }
                            Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL,
                            Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL,
                            -> {
                                intentData = intent.data
                            }
                        }

                        if (viewModel.rootNodeExists()) {
                            var newIntent = Intent(requireContext(), ManagerActivity::class.java)

                            when (action) {
                                Constants.ACTION_FILE_PROVIDER -> {
                                    newIntent =
                                        Intent(requireContext(), FileProviderActivity::class.java)
                                    intentExtras?.let { newIntent.putExtras(it) }
                                    newIntent.data = intentData
                                }
                                Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL -> {
                                    newIntent =
                                        Intent(requireContext(), FileLinkActivity::class.java)
                                    newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    intentAction = Constants.ACTION_OPEN_MEGA_LINK
                                    newIntent.data = intentData
                                }
                                Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL -> {
                                    newIntent =
                                        Intent(requireContext(), FolderLinkActivity::class.java)
                                    newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    intentAction = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                                    newIntent.data = intentData
                                }
                                Constants.ACTION_OPEN_CONTACTS_SECTION -> {
                                    newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    intentAction = Constants.ACTION_OPEN_CONTACTS_SECTION
                                    if (newIntent.getLongExtra(Constants.CONTACT_HANDLE,
                                            -1) != -1L
                                    ) {
                                        newIntent.putExtra(Constants.CONTACT_HANDLE,
                                            newIntent.getLongExtra(Constants.CONTACT_HANDLE, -1))
                                    }
                                }
                            }

                            newIntent.action = intentAction

                            intentDataString?.let { newIntent.data = Uri.parse(it) }
                            newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

                            startCameraUploadService(false, 5 * 60 * 1000)
                            startActivity(newIntent)
                            requireActivity().finish()
                        } else {
                            startFastLogin()
                        }

                        return
                    }
                }
            }

            if (viewModel.rootNodeExists() && !uiState.isFetchingNodes && !isIsHeartBeatAlive) {
                Timber.d("rootNode != null")

                var newIntent = Intent(requireContext(), ManagerActivity::class.java)

                intentAction?.let { action ->
                    when (action) {
                        Constants.ACTION_FILE_PROVIDER -> {
                            newIntent = Intent(requireContext(), FileProviderActivity::class.java)
                            intentExtras?.let { newIntent.putExtras(it) }
                            newIntent.data = intentData
                        }
                        Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL -> {
                            newIntent = Intent(requireContext(), FileLinkActivity::class.java)
                            newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            intentAction = Constants.ACTION_OPEN_MEGA_LINK
                            newIntent.data = intentData
                        }
                        Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL -> {
                            newIntent = Intent(requireContext(), FolderLinkActivity::class.java)
                            newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            intentAction = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                            newIntent.data = intentData
                        }
                        Constants.ACTION_OPEN_CONTACTS_SECTION -> {
                            newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

                            if (intent.getLongExtra(Constants.CONTACT_HANDLE, -1) != -1L) {
                                newIntent.putExtra(Constants.CONTACT_HANDLE,
                                    intent.getLongExtra(Constants.CONTACT_HANDLE, -1))
                            }
                        }
                    }

                    newIntent.action = action
                    intentDataString?.let { newIntent.data = Uri.parse(it) }
                }

                newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

                if (uiState.isCUSettingEnabled) {
                    startCameraUploadService(false, 30 * 1000)
                }

                this.startActivity(newIntent)
                (requireActivity() as LoginActivity).finish()
            } else {
                Timber.d("rootNode is null or heart beat is alive -> do fast login")
                setHeartBeatAlive(false)
                startFastLogin()
            }

            return
        }

        Timber.d("Credentials IS NULL")
        Timber.d("INTENT NOT NULL")

        intentAction?.let { action ->
            Timber.d("ACTION NOT NULL")
            val newIntent: Intent
            when (action) {
                Constants.ACTION_FILE_PROVIDER -> {
                    newIntent = Intent(requireContext(), FileProviderActivity::class.java)
                    intentExtras?.let { newIntent.putExtras(it) }
                    newIntent.data = intentData
                    newIntent.action = action
                }
                Constants.ACTION_FILE_EXPLORER_UPLOAD -> {
                    showMessage(R.string.login_before_share)
                }
                Constants.ACTION_JOIN_OPEN_CHAT_LINK -> {
                    intentDataString = intent.dataString
                }
            }
        }
    } ?: Timber.w("No INTENT")

    /**
     * Shows login in progress screen.
     *
     * @param fetchingNodes      True if it's fetching nodes, false otherwise.
     * @param queryingSignupLink True if it's checking a signup link, false otherwise.
     * @param generatingKeys     True if it's generating keys, false otherwise.
     * @param activatingAccount  True if it's activating account, false otherwise.
     */
    private fun showLoggingInScreen(
        fetchingNodes: Boolean = true,
        queryingSignupLink: Boolean = false,
        generatingKeys: Boolean = false,
        activatingAccount: Boolean = false,
    ) = with(binding) {
        loginLayout.isVisible = false
        loginLoggingInLayout.isVisible = true
        loginProgressBar.isVisible = true
        loginFetchingNodesBar.isVisible = false
        loginLoggingInText.isVisible = true
        loginFetchNodesText.isVisible = fetchingNodes
        loginPrepareNodesText.isVisible = false
        loginServersBusyText.isVisible = false
        loginCreateAccountLayout.isVisible = false
        loginQuerySignupLinkText.isVisible = queryingSignupLink
        loginConfirmAccountText.isVisible = activatingAccount
        loginGeneratingKeysText.isVisible = generatingKeys
    }

    /**
     * Shows the login screen.
     */
    fun returnToLogin() {
        (requireActivity() as LoginActivity).hideAB()
        showLoginScreen()
    }

    /**
     * Shows the login screen.
     */
    private fun showLoginScreen() = with(binding) {
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
        loginProgressBar.isVisible = false
        loginFetchingNodesBar.isVisible = false
        login2fa.isVisible = false
    }

    private fun show2FAScreen() = with(binding) {
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
        requestEditTextPinFocus(pinFirstLogin)
    }

    /**
     * Hides UI errors.
     */
    private fun hideError() = with(binding) {
        viewModel.setIs2FAErrorNotShown()
        pin2faErrorLogin.isVisible = false
        listOf(pinFirstLogin,
            pinSecondLogin,
            pinThirdLogin,
            pinFourthLogin,
            pinFifthLogin,
            pinSixthLogin).forEach { it.setTextColor(pin2FAColor) }
    }

    /**
     * Shows UI errors.
     */
    private fun showShowError() = with(binding) {
        viewModel.setWas2FAErrorShown()
        pin2faErrorLogin.isVisible = true
        confirmLogoutDialog?.dismiss()
        listOf(pinFirstLogin,
            pinSecondLogin,
            pinThirdLogin,
            pinFourthLogin,
            pinFifthLogin,
            pinSixthLogin).forEach { it.setTextColor(pin2FAErrorColor) }
    }


    /**
     * Checks if all the 2FA fields are filled.
     *
     * @return True if all fields are already filled, false otherwise.
     */
    private fun areAllFieldsFilled() = with(binding) {
        pinFirstLogin.length() == 1 && pinSecondLogin.length() == 1 && pinThirdLogin.length() == 1 && pinFourthLogin.length() == 1 && pinFifthLogin.length() == 1 && pinSixthLogin.length() == 1
    }

    /**
     * Verifies if the typed 2FA is correct.
     */
    private fun verify2FA() = with(binding) {
        Util.hideKeyboard(requireActivity(), 0)
        if (sb.isNotEmpty()) {
            sb.delete(0, sb.length)
        }
        sb.append(pinFirstLogin.text)
        sb.append(pinSecondLogin.text)
        sb.append(pinThirdLogin.text)
        sb.append(pinFourthLogin.text)
        sb.append(pinFifthLogin.text)
        sb.append(pinSixthLogin.text)
        val twoFAPin = sb.toString()

        if (!uiState.is2FAErrorShown) {
            Timber.d("Login with factor login")
            progressbarVerify2fa.isVisible = true
            isLoggingIn = true
            with(uiState) {
                megaApi.multiFactorAuthLogin(accountSession?.email,
                    password,
                    twoFAPin,
                    this@LoginFragment)
            }
        }
    }

    /**
     * Updates the UI for fetching nodes.
     */
    private fun startFetchNodes() = with(binding) {
        Timber.d("startLoginInProcess")

        if (!viewModel.updateEmailAndSession()) {
            return
        }

        showLoggingInScreen()
        megaApi.fetchNodes(this@LoginFragment)
    }

    /**
     * Updates the UI for starting fast login.
     */
    private fun startFastLogin() {
        Timber.d("startFastLogin")
        if (!viewModel.updateEmailAndSession()) {
            return
        }

        showLoggingInScreen(fetchingNodes = false)

        if (!isLoggingIn) {
            isLoggingIn = true
            val gSession = uiState.accountSession?.session
            ChatUtil.initMegaChatApi(gSession,
                ChatLogoutListener(requireActivity(), loggingSettings))
            disableLoginButton()

            megaApi.fastLogin(gSession, this)

            if (requireActivity().intent?.action == Constants.ACTION_REFRESH_API_SERVER) {
                Timber.d("megaChatApi.refreshUrl()")
                megaChatApi.refreshUrl()
            }
        } else {
            viewModel.setIntentAction(ACTION_OPEN_APP)
            Timber.w("Another login is processing")
        }
    }

    /**
     * Submit typed data for confirming account.
     */
    private fun submitFormConfirmAccount() = with(binding) {
        Timber.d("fromConfirmAccount - true email: ${uiState.temporalEmail}")
        viewModel.setTemporalCredentialsAsCurrentCredentials()
        loginEmailText.hideKeyboard()

        if (!isConnected()) {
            return
        }

        showLoggingInScreen(fetchingNodes = false, generatingKeys = true)
        Timber.d("Generating keys")

        with(uiState) {
            onKeysGenerated(accountSession?.email, password)
        }
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

        if (!isConnected()) {
            return
        }

        showLoggingInScreen(fetchingNodes = false, generatingKeys = true)

        val lastEmail = loginEmailText.text.toString().lowercase().trim { it <= ' ' }
        val lastPassword = loginPasswordText.text.toString()
        Timber.d("Generating keys")
        viewModel.updateCredentials(email = lastEmail, password = lastPassword)
        onKeysGenerated(lastEmail, lastPassword)
    }

    /**
     * Returns to login form page.
     */
    private fun backToLoginForm() = with(binding) {
        showLoginScreen()

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
    private fun onKeysGenerated(email: String?, password: String?) {
        Timber.d("onKeysGenerated")
        if (!isConnected()) {
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
     * Handles intent from confirmation email.
     *
     * @param intent Intent.
     */
    private fun handleConfirmationIntent(intent: Intent) {
        val accountConfirmationLink = intent.getStringExtra(Constants.EXTRA_CONFIRMATION)
        viewModel.updateAccountConfirmationLink(accountConfirmationLink)

        if (!viewModel.isConnected) {
            showMessage(R.string.error_server_connection_problem)
            return
        }

        showLoggingInScreen(fetchingNodes = false, queryingSignupLink = true)
        Timber.d("querySignupLink")
        accountConfirmationLink?.let { viewModel.checkSignupLink(it) }
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        timer?.let {
            it.cancel()
            binding.loginServersBusyText.isVisible = false
        }

        if (request.type == MegaRequest.TYPE_FETCH_NODES) {
            if (uiState.isFirstFetchNodesUpdate) {
                binding.loginProgressBar.isVisible = false
                viewModel.updateFetchNodesUpdate()
            }
            binding.loginFetchingNodesBar.apply {
                layoutParams.width = Util.dp2px(250 * scaleW)

                if (request.totalBytes > 0) {
                    isVisible = true
                    var progressValue = 100.0 * request.transferredBytes / request.totalBytes
                    if (progressValue > 99 || progressValue < 0) {
                        progressValue = 100.0
                        binding.loginPrepareNodesText.isVisible = true
                        binding.loginProgressBar.isVisible = true
                    }
                    progress = progressValue.toInt()
                } else {
                    isVisible = false
                }
            }
        }
    }

    /**
     * Launches Manager activity.
     */
    fun readyToManager() {
        confirmLogoutDialog?.dismiss()
        val loginActivity = requireActivity() as LoginActivity

        if (uiState.accountConfirmationLink == null && !uiState.isAccountConfirmed) {
            if (getChatManagement().isPendingJoinLink) {
                LoginActivity.isBackFromLoginPage = false
                getChatManagement().pendingJoinLink = null
            }
            Timber.d("confirmLink==null")
            Timber.d("OK fetch nodes")

            if (intentAction != null && intentDataString != null) {
                when (intentAction) {
                    Constants.ACTION_CHANGE_MAIL -> {
                        Timber.d("Action change mail after fetch nodes")
                        val changeMailIntent = Intent(requireContext(), ManagerActivity::class.java)
                        changeMailIntent.action = Constants.ACTION_CHANGE_MAIL
                        changeMailIntent.data = Uri.parse(intentDataString)
                        loginActivity.startActivity(changeMailIntent)
                        loginActivity.finish()
                    }
                    Constants.ACTION_RESET_PASS -> {
                        Timber.d("Action reset pass after fetch nodes")
                        val resetPassIntent = Intent(requireContext(), ManagerActivity::class.java)
                        resetPassIntent.action = Constants.ACTION_RESET_PASS
                        resetPassIntent.data = Uri.parse(intentDataString)
                        loginActivity.startActivity(resetPassIntent)
                        loginActivity.finish()
                    }
                    Constants.ACTION_CANCEL_ACCOUNT -> {
                        Timber.d("Action cancel Account after fetch nodes")
                        val cancelAccountIntent =
                            Intent(requireContext(), ManagerActivity::class.java)
                        cancelAccountIntent.action = Constants.ACTION_CANCEL_ACCOUNT
                        cancelAccountIntent.data = Uri.parse(intentDataString)
                        loginActivity.startActivity(cancelAccountIntent)
                        loginActivity.finish()
                    }
                }
            }
            if (!uiState.pressedBackWhileLogin) {
                Timber.d("NOT backWhileLogin")
                if (intentParentHandle != -1L) {
                    val intent = Intent()
                    intent.putExtra("PARENT_HANDLE", intentParentHandle)
                    loginActivity.setResult(Activity.RESULT_OK, intent)
                    loginActivity.finish()
                } else {
                    var intent: Intent?
                    if (uiState.isAlreadyLoggedIn) {
                        intent = Intent(requireContext(), ManagerActivity::class.java)
                        setStartScreenTimeStamp(requireContext())
                        when (intentAction) {
                            Constants.ACTION_EXPORT_MASTER_KEY -> {
                                Timber.d("ACTION_EXPORT_MK")
                                intent.action = Constants.ACTION_EXPORT_MASTER_KEY
                            }
                            Constants.ACTION_JOIN_OPEN_CHAT_LINK -> {
                                if (intentDataString != null) {
                                    intent.action = Constants.ACTION_JOIN_OPEN_CHAT_LINK
                                    intent.data = Uri.parse(intentDataString)
                                }
                            }
                            else -> intent = handleLinkNavigation(loginActivity)
                        }
                        if (uiState.isFirstTime) {
                            Timber.d("First time")
                            intent.putExtra(IntentConstants.EXTRA_FIRST_LOGIN, true)
                        }
                    } else {
                        var initialCam = false
                        if (uiState.hasPreferences) {
                            if (uiState.hasCUSetting) {
                                if (uiState.isCUSettingEnabled) {
                                    startCameraUploadService(false, 30 * 1000)
                                }
                            } else {
                                startCameraUploadService(true, 30 * 1000)
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
                            intent = handleLinkNavigation(loginActivity)
                        } else {
                            Timber.d("initialCam YES")
                            intent = Intent(requireContext(), ManagerActivity::class.java)
                            Timber.d("The action is: %s", intentAction)
                            intent.action = intentAction
                            intentDataString?.let { intent.data = Uri.parse(it) }
                        }
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    if (uiState.isRefreshApiServer) {
                        intent.action = Constants.ACTION_REFRESH_API_SERVER
                        viewModel.updateIsRefreshApiServer(false)
                    }
                    if (intentAction == Constants.ACTION_REFRESH_AFTER_BLOCKED) {
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
            viewModel.updateIsAccountConfirmed(false)
            if (getChatManagement().isPendingJoinLink) {
                LoginActivity.isBackFromLoginPage = false
                val intent = Intent(requireContext(), ManagerActivity::class.java)
                intent.action = Constants.ACTION_JOIN_OPEN_CHAT_LINK
                intent.data = Uri.parse(getChatManagement().pendingJoinLink)
                startActivity(intent)
                getChatManagement().pendingJoinLink = null
                loginActivity.finish()
            } else if (uiState.isAlreadyLoggedIn) {
                startActivity(Intent(loginActivity, ChooseAccountActivity::class.java))
                loginActivity.finish()
            }
        }
    }

    private fun handleLinkNavigation(loginActivity: LoginActivity): Intent {
        var intent = Intent(requireContext(), ManagerActivity::class.java)
        if (intentAction != null) {
            Timber.d("The action is: %s", intentAction)
            when (intentAction) {
                Constants.ACTION_FILE_PROVIDER -> {
                    intent = Intent(requireContext(), FileProviderActivity::class.java)
                    intentExtras?.let { intent.putExtras(it) }
                    intent.data = intentData
                }
                Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL -> {
                    intent = Intent(requireContext(), FileLinkActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    intent.data = intentData
                }
                Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL -> {
                    intent = Intent(requireContext(), FolderLinkActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    intentAction = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                    intent.data = intentData
                }
                Constants.ACTION_OPEN_CONTACTS_SECTION -> {
                    intent.putExtra(
                        Constants.CONTACT_HANDLE,
                        loginActivity.intent?.getLongExtra(Constants.CONTACT_HANDLE, -1),
                    )
                }
            }
            intent.action = intentAction
            intentDataString?.let { intent.data = Uri.parse(it) }
        } else {
            Timber.w("The intent action is NULL")
        }
        return intent
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestStart: %s", request.requestString)
        if (request.type == MegaRequest.TYPE_LOGIN) {
            disableLoginButton()
        }
        if (request.type == MegaRequest.TYPE_FETCH_NODES) {
            viewModel.updateIsFetchingNodes(true)
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
                when (error.errorCode) {
                    MegaError.API_ESID -> {
                        Timber.w("MegaError.API_ESID %s",
                            requireContext().getFormattedStringOrDefault(R.string.error_server_expired_session))
                        (requireActivity() as LoginActivity).showAlertLoggedOut()
                    }
                    MegaError.API_EMFAREQUIRED -> {
                        Timber.d("require 2fa")
                        viewModel.setIs2FAEnabled()
                        show2FAScreen()
                        return
                    }
                    MegaError.API_EFAILED, MegaError.API_EEXPIRED -> {
                        binding.progressbarVerify2fa.isVisible = false
                        showShowError()
                        return
                    }
                    else -> {
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
                            if (!uiState.pressedBackWhileLogin) {
                                (requireActivity() as LoginActivity).showSnackbar(errorMessage)
                            }
                        }
                        viewModel.initChatSettings()
                    }
                }
                returnToLogin()
            } else {
                Timber.d("Logged in. Setting account auth token for folder links.")
                megaApiFolder.accountAuth = megaApi.accountAuth

                with(binding) {
                    if (uiState.is2FAEnabled) {
                        login2fa.isVisible = false
                        (requireActivity() as LoginActivity).hideAB()
                    }

                    showLoggingInScreen()
                }

                viewModel.saveCredentials()
                Timber.d("Logged in with session")
                megaApi.fetchNodes(this)

                // Get cookies settings after login.
                getInstance().checkEnabledCookies()
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
            viewModel.updateIsFetchingNodes(false)
            isLoggingIn = false
            if (error.errorCode == MegaError.API_OK) {
                if (!isAdded) return

                (requireActivity() as LoginActivity).intent?.apply {
                    @Suppress("UNCHECKED_CAST") intentShareInfo =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            getSerializableExtra(FileExplorerActivity.EXTRA_SHARE_INFOS,
                                ArrayList::class.java)
                        } else {
                            @Suppress("DEPRECATION") getSerializableExtra(FileExplorerActivity.EXTRA_SHARE_INFOS)
                        } as ArrayList<ShareInfo>?

                    when {
                        intentShareInfo?.isNotEmpty() == true -> {
                            val permissions = arrayOf(getImagePermissionByVersion(),
                                getAudioPermissionByVersion(),
                                getVideoPermissionByVersion(),
                                getReadExternalStoragePermission())
                            if (hasPermissions(requireContext(), *permissions)) {
                                toSharePage()
                            } else {
                                requestMediaPermission.launch(permissions)
                            }
                            return
                        }
                        Constants.ACTION_FILE_EXPLORER_UPLOAD == action && Constants.TYPE_TEXT_PLAIN == type -> {
                            startActivity(Intent(requireContext(),
                                FileExplorerActivity::class.java).putExtra(Intent.EXTRA_TEXT,
                                    getStringExtra(Intent.EXTRA_TEXT))
                                .putExtra(Intent.EXTRA_SUBJECT,
                                    getStringExtra(Intent.EXTRA_SUBJECT))
                                .putExtra(Intent.EXTRA_EMAIL, getStringExtra(Intent.EXTRA_EMAIL))
                                .setAction(Intent.ACTION_SEND).setType(Constants.TYPE_TEXT_PLAIN))
                            requireActivity().finish()
                            return
                        }
                        Constants.ACTION_REFRESH == action && activity != null -> {
                            requireActivity().apply {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                            return
                        }
                    }
                }
                readyToManager()
            } else {
                Timber.e("Error fetch nodes: %s", error.errorCode)

                enableLoginButton()
                showLoginScreen()

                if (error.errorCode == MegaError.API_EACCESS) {
                    Timber.e("Error API_EACCESS")
                    if (!uiState.pressedBackWhileLogin) {
                        (requireActivity() as LoginActivity).showSnackbar(error.errorString)
                    }
                } else if (error.errorCode != MegaError.API_EBLOCKED) {
                    //It will processed at the `onEvent` when receive an EVENT_ACCOUNT_BLOCKED
                    Timber.w("Suspended account - Reason: %s", request.number)
                    return
                }

                viewModel.initChatSettings()
            }
        }
    }

    /**
     * Launches an intent to [FileExplorerActivity]
     */
    private fun toSharePage() = with(requireActivity()) {
        startActivity(Intent(requireContext(), FileExplorerActivity::class.java).apply {
                putExtra(FileExplorerActivity.EXTRA_SHARE_INFOS, intentShareInfo)
                action = intent?.getStringExtra(FileExplorerActivity.EXTRA_SHARE_ACTION)
                type = intent?.getStringExtra(FileExplorerActivity.EXTRA_SHARE_TYPE)
            })
        finish()
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
                    text =
                        requireContext().getFormattedStringOrDefault(if (e.errorCode == MegaError.API_EAGAIN) {
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
                        })
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
        val builder = MaterialAlertDialogBuilder(requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog)
        builder.setTitle(requireContext().getFormattedStringOrDefault(R.string.title_dialog_insert_MK))
        builder.setMessage(requireContext().getFormattedStringOrDefault(R.string.text_dialog_insert_MK))
        builder.setPositiveButton(requireContext().getFormattedStringOrDefault(R.string.general_ok)) { _, _ -> }
        builder.setOnDismissListener {
            Util.hideKeyboard(requireActivity(), InputMethodManager.HIDE_NOT_ALWAYS)
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
                        viewModel.updatePressedBackWhileLogin(true)
                        isLoggingIn = false
                        viewModel.updateIsFetchingNodes(false)
                        viewModel.updateIsAlreadyLoggedIn(true)
                        megaChatApi.logout(ChatLogoutListener(requireActivity(), loggingSettings))
                        viewModel.performLocalLogout()
                    }
                    DialogInterface.BUTTON_NEGATIVE -> dialog.dismiss()
                }
            }
        val message = requireContext().getFormattedStringOrDefault(R.string.confirm_cancel_login)
        confirmLogoutDialog = builder.setCancelable(true).setMessage(message).setPositiveButton(
                requireContext().getFormattedStringOrDefault(R.string.general_positive_button),
                dialogClickListener)
            .setNegativeButton(requireContext().getFormattedStringOrDefault(R.string.general_negative_button),
                dialogClickListener).show()
    }

    /**
     * Performs on back pressed.
     */
    fun onBackPressed(): Int {
        Timber.d("onBackPressed")
        //refresh, point to staging server, enable chat. block the back button
        if (Constants.ACTION_REFRESH == intentAction || Constants.ACTION_REFRESH_API_SERVER == intentAction) {
            return -1
        }
        //login is in process
        val onLoginPage = binding.loginLayout.isVisible
        val on2faPage = binding.login2fa.isVisible
        return if ((isLoggingIn || uiState.isFetchingNodes) && !onLoginPage && !on2faPage) {
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
        viewModel.updateIsPinLongClick(false)
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
                        pinFirstLogin.setText(code[0].toString())
                        pinSecondLogin.setText(code[1].toString())
                        pinThirdLogin.setText(code[2].toString())
                        pinFourthLogin.setText(code[3].toString())
                        pinFifthLogin.setText(code[4].toString())
                        pinSixthLogin.setText(code[5].toString())
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
            viewModel.launchCancelTransfers()
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        Timber.d("firstTimeCam: $firstTimeCam: $time")

        with(requireActivity()) {
            if (firstTimeCam) {
                setStartScreenTimeStamp(this)

                startActivity(Intent(this, ManagerActivity::class.java).apply {
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

    /**
     * Shows a warning informing the Recovery Key is not correct.
     */
    private fun showAlertIncorrectRK() {
        Timber.d("showAlertIncorrectRK")
        MaterialAlertDialogBuilder(requireContext()).setTitle(requireContext().getFormattedStringOrDefault(
                R.string.incorrect_MK_title))
            .setMessage(requireContext().getFormattedStringOrDefault(R.string.incorrect_MK))
            .setCancelable(false)
            .setPositiveButton(requireContext().getFormattedStringOrDefault(R.string.general_ok),
                null).show()
    }

    private fun showMessage(stringId: Int) =
        (requireActivity() as LoginActivity).showSnackbar(requireContext().getFormattedStringOrDefault(
            stringId))

    private fun isConnected(): Boolean = if (!viewModel.isConnected) {
        showLoginScreen()
        showMessage(R.string.error_server_connection_problem)
        false
    } else true

    private fun showQuerySignupLinkResult(result: Result<String>) {
        showLoginScreen()
        binding.buttonForgotPass.isInvisible = true
        viewModel.updateAccountConfirmationLink(null)

        if (result.isSuccess) {
            with(binding) {
                buttonForgotPass.isInvisible = false
                loginProgressBar.isVisible = false
                loginTextView.text =
                    requireContext().getFormattedStringOrDefault(R.string.login_to_mega)
                buttonLogin.text = requireContext().getFormattedStringOrDefault(R.string.login_text)
                loginEmailText.setText(result.getOrNull())
                loginPasswordText.requestFocus()
            }
            showMessage(R.string.account_confirmed)
            viewModel.updateIsAccountConfirmed(true)
        } else {
            (result.exceptionOrNull() as QuerySignupLinkException).messageId?.let {
                showMessage(it)
            }
        }
    }

    companion object {
        private const val LONG_CLICK_DELAY: Long = 5000
    }
}
