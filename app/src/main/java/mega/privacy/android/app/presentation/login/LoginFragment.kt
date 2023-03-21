package mega.privacy.android.app.presentation.login

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
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
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication.Companion.getChatManagement
import mega.privacy.android.app.MegaApplication.Companion.isIsHeartBeatAlive
import mega.privacy.android.app.MegaApplication.Companion.isLoggingIn
import mega.privacy.android.app.MegaApplication.Companion.setHeartBeatAlive
import mega.privacy.android.app.R
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.EditTextPIN
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.databinding.FragmentLoginBinding
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.FileLinkActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity
import mega.privacy.android.app.presentation.extensions.error
import mega.privacy.android.app.presentation.extensions.messageId
import mega.privacy.android.app.presentation.folderlink.FolderLinkActivity
import mega.privacy.android.app.presentation.login.LoginViewModel.Companion.ACTION_FORCE_RELOAD_ACCOUNT
import mega.privacy.android.app.presentation.login.LoginViewModel.Companion.ACTION_OPEN_APP
import mega.privacy.android.app.presentation.login.model.LoginIntentState
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.model.MultiFactorAuthState
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.setStartScreenTimeStamp
import mega.privacy.android.app.providers.FileProviderActivity
import mega.privacy.android.app.upgradeAccount.ChooseAccountActivity
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.ChangeApiServerUtil.showChangeApiServerDialog
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ConstantsUrl.RECOVERY_URL
import mega.privacy.android.app.utils.ConstantsUrl.RECOVERY_URL_EMAIL
import mega.privacy.android.app.utils.JobUtil
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.ViewUtils.hideKeyboard
import mega.privacy.android.app.utils.ViewUtils.removeLeadingAndTrailingSpaces
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.exception.LoginException
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.QuerySignupLinkException
import mega.privacy.android.domain.exception.login.FetchNodesException
import nz.mega.sdk.MegaError
import timber.log.Timber

/**
 * Login fragment.
 */
@AndroidEntryPoint
class LoginFragment : Fragment() {

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
    private val pin2FAColor by lazy {
        ContextCompat.getColor(requireContext(), R.color.grey_087_white_087)
    }
    private val pin2FAErrorColor by lazy {
        ContextCompat.getColor(requireContext(), R.color.red_600_red_300)
    }

    private var was2FAErrorShown = false
    private var isPinLongClick: Boolean = false
    private var pendingClicksKarere = CLICKS_TO_ENABLE_LOGS
    private var pendingClicksSDK = CLICKS_TO_ENABLE_LOGS

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
        viewLifecycleOwner.collectFlow(state) { uiState ->
            with(uiState) {
                intentState?.apply {
                    when (this) {
                        LoginIntentState.ReadyForInitialSetup -> finishSetupIntent(uiState)
                        LoginIntentState.ReadyForFinalSetup -> readyToFinish(uiState)
                        else -> { /* Nothing to update */
                        }
                    }
                }

                when {
                    querySignupLinkResult != null -> {
                        showQuerySignupLinkResult(querySignupLinkResult)
                    }
                    ongoingTransfersExist != null -> {
                        if (ongoingTransfersExist) {
                            showCancelTransfersDialog()
                        } else {
                            loginClicked()
                        }
                    }
                    isLoginInProgress -> {
                        showLoginInProgress()
                    }
                    isLoginRequired -> {
                        confirmLogoutDialog?.dismiss()
                        returnToLogin()
                    }
                    fetchNodesUpdate != null -> {
                        with(fetchNodesUpdate) {
                            if (progress == null && temporaryError == null) {
                                if (is2FAEnabled) {
                                    binding.login2fa.isVisible = false
                                    (requireActivity() as LoginActivity).hideAB()
                                }
                                showFetchingNodes()
                            } else {
                                showFetchNodesProgress(progress)

                                temporaryError?.let {
                                    showFetchNodesTemporaryError(it.messageId)
                                } ?: timer?.apply {
                                    cancel()
                                    binding.loginServersBusyText.isVisible = false
                                }
                            }
                        }
                    }
                    multiFactorAuthState != null -> {
                        if (multiFactorAuthState == MultiFactorAuthState.Failed) {
                            binding.progressbarVerify2fa.isVisible = false
                            show2FAError()
                        } else {
                            hideError()
                        }
                    }
                    is2FARequired -> {
                        show2FAScreen()
                    }
                }

                if (isLocalLogoutInProgress) disableLoginButton() else enableLoginButton()

                error?.let { exception ->
                    when (exception) {
                        is LoginLoggedOutFromOtherLocation -> {
                            (requireActivity() as LoginActivity).showAlertLoggedOut()
                            null
                        }
                        is LoginException -> exception.error
                        is FetchNodesException -> exception.error
                        else -> null
                    }?.let {
                        (requireActivity() as LoginActivity).showSnackbar(getString(it))
                    }

                    setErrorShown()
                }
            }
        }
    }

    private fun showFetchNodesProgress(progress: Progress?) = with(binding) {
        progress?.let {
            with(it.floatValue) {
                val newProgress = (this * 100).toInt()
                loginFetchingNodesBar.apply {
                    isVisible = true
                    setProgress(newProgress, true)
                    loginPrepareNodesText.isVisible = true
                    loginProgressBar.isVisible = true
                }
            }
        }
    }

    private fun showFetchNodesTemporaryError(@StringRes stringId: Int) {
        timer = object : CountDownTimer(10000, 2000) {
            override fun onTick(millisUntilFinished: Long) {
                Timber.d("TemporaryError one more")
            }

            override fun onFinish() {
                Timber.d("The timer finished, message shown")
                binding.loginServersBusyText.apply {
                    text = getString(stringId)
                    isVisible = true
                }
            }
        }.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupView() = with(binding) {
        loginTextView.apply {
            text = getString(R.string.login_to_mega)
            setOnClickListener { clickKarereLogs() }
            val onLongPress = Runnable {
                if (!isAlertDialogShown(changeApiServerDialog)) {
                    changeApiServerDialog = showChangeApiServerDialog(requireActivity())
                }
            }
            val onTap = Runnable {
                handler.postDelayed(
                    onLongPress,
                    LONG_CLICK_DELAY - ViewConfiguration.getTapTimeout()
                )
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
                            view.top + event.y.toInt()
                        )
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
                    checkTypedValues()
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
            text = getString(R.string.login_text)
            setOnClickListener {
                Timber.d("Click on button_login_login")
                viewModel.updatePressedBackWhileLogin(false)
                LoginActivity.isBackFromLoginPage = false
                loginEmailText.removeLeadingAndTrailingSpaces()
                checkTypedValues()
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

        textNewToMega.setOnClickListener { clickSDKLogs() }

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

    private fun clickKarereLogs() {
        pendingClicksKarere = if (pendingClicksKarere == 1) {
            viewModel.checkAndUpdateKarereLogs(requireActivity())
            CLICKS_TO_ENABLE_LOGS
        } else {
            pendingClicksKarere - 1
        }
    }

    private fun clickSDKLogs() {
        pendingClicksSDK = if (pendingClicksSDK == 1) {
            viewModel.checkAndUpdateSDKLogs(requireActivity())
            CLICKS_TO_ENABLE_LOGS
        } else {
            pendingClicksSDK - 1
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
                isPinLongClick = true
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
                        !was2FAErrorShown && !isPinLongClick -> clear2FAFields(id)
                        isPinLongClick -> pasteClipboard()
                    }
                } else {
                    viewModel.checkAndUpdate2FAState()
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

    private fun finishSetupIntent(uiState: LoginState) {
        (requireActivity() as LoginActivity).intent?.apply {
            if (uiState.isAlreadyLoggedIn && !LoginActivity.isBackFromLoginPage) {
                Timber.d("Credentials NOT null")

                intentAction?.let { action ->
                    when (action) {
                        Constants.ACTION_REFRESH -> {
                            viewModel.performFetchNodes(true)
                            return@apply
                        }
                        Constants.ACTION_REFRESH_API_SERVER -> {
                            intentParentHandle = getLongExtra("PARENT_HANDLE", -1)
                            startFastLogin()
                            return@apply
                        }
                        Constants.ACTION_REFRESH_AFTER_BLOCKED -> {
                            startFastLogin()
                            return@apply
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
                                    intentDataString = dataString
                                }
                                Constants.ACTION_FILE_PROVIDER -> {
                                    intentData = data
                                    intentExtras = extras
                                    intentDataString = null
                                }
                                Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL,
                                Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL,
                                -> {
                                    intentData = data
                                }
                            }

                            if (viewModel.rootNodeExists()) {
                                var newIntent =
                                    Intent(requireContext(), ManagerActivity::class.java)

                                when (action) {
                                    Constants.ACTION_FILE_PROVIDER -> {
                                        newIntent =
                                            Intent(
                                                requireContext(),
                                                FileProviderActivity::class.java
                                            )
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
                                        if (newIntent.getLongExtra(
                                                Constants.CONTACT_HANDLE,
                                                -1
                                            ) != -1L
                                        ) {
                                            newIntent.putExtra(
                                                Constants.CONTACT_HANDLE,
                                                newIntent.getLongExtra(Constants.CONTACT_HANDLE, -1)
                                            )
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

                            return@apply
                        }
                    }
                }

                if (viewModel.rootNodeExists() && uiState.fetchNodesUpdate == null && !isIsHeartBeatAlive) {
                    Timber.d("rootNode != null")

                    var newIntent = Intent(requireContext(), ManagerActivity::class.java)

                    intentAction?.let { action ->
                        when (action) {
                            Constants.ACTION_FILE_PROVIDER -> {
                                newIntent =
                                    Intent(requireContext(), FileProviderActivity::class.java)
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

                                if (getLongExtra(Constants.CONTACT_HANDLE, -1) != -1L) {
                                    newIntent.putExtra(
                                        Constants.CONTACT_HANDLE,
                                        getLongExtra(Constants.CONTACT_HANDLE, -1)
                                    )
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

                    startActivity(newIntent)
                    (requireActivity() as LoginActivity).finish()
                } else {
                    Timber.d("rootNode is null or heart beat is alive -> do fast login")
                    setHeartBeatAlive(false)
                    startFastLogin()
                }

                return@apply
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
                        intentDataString = dataString
                    }
                }
            }
        }

        viewModel.intentSet()
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
                        viewModel.intentSet()
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
                    viewModel.intentSet()
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
                    viewModel.setPendingAction(ACTION_FORCE_RELOAD_ACCOUNT)
                    isLoggingIn = true
                    showFetchingNodes()
                    viewModel.intentSet()
                    return
                }
            }
        } ?: Timber.w("ACTION NULL")

        Timber.d("et_user.getText(): %s", binding.loginEmailText.text)
    } ?: Timber.w("No INTENT")

    /**
     * Shows login in progress screen.
     *
     * @param fetchingNodes      True if it's fetching nodes, false otherwise.
     * @param queryingSignupLink True if it's checking a signup link, false otherwise.
     * @param generatingKeys     True if it's generating keys, false otherwise.
     * @param activatingAccount  True if it's activating account, false otherwise.
     */
    private fun showFetchingNodes(
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
    private fun returnToLogin() {
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
        viewModel.checkAndUpdate2FAState()
        pin2faErrorLogin.isVisible = false
        listOf(
            pinFirstLogin,
            pinSecondLogin,
            pinThirdLogin,
            pinFourthLogin,
            pinFifthLogin,
            pinSixthLogin
        ).forEach { it.setTextColor(pin2FAColor) }
    }

    /**
     * Shows UI errors.
     */
    private fun show2FAError() = with(binding) {
        was2FAErrorShown = true
        pin2faErrorLogin.isVisible = true
        confirmLogoutDialog?.dismiss()
        listOf(
            pinFirstLogin,
            pinSecondLogin,
            pinThirdLogin,
            pinFourthLogin,
            pinFifthLogin,
            pinSixthLogin
        ).forEach { it.setTextColor(pin2FAErrorColor) }
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

        if (!pin2faErrorLogin.isVisible) {
            Timber.d("Login with factor login")
            progressbarVerify2fa.isVisible = true
            viewModel.performLoginWith2FA(twoFAPin)
        }
    }

    /**
     * Updates the UI for starting fast login.
     */
    private fun startFastLogin() {
        Timber.d("startFastLogin")
        if (!viewModel.updateEmailAndSession()) {
            return
        }

        showFetchingNodes(fetchingNodes = false)

        if (!isLoggingIn) {
            viewModel.performFastLogin(requireActivity().intent?.action == Constants.ACTION_REFRESH_API_SERVER)
        } else {
            viewModel.setPendingAction(ACTION_OPEN_APP)
            Timber.w("Another login is processing")
        }
    }

    /**
     * Submit typed data for confirming account.
     */
    private fun submitFormConfirmAccount() = with(binding) {
        Timber.d("fromConfirmAccount - true email")
        viewModel.setTemporalCredentialsAsCurrentCredentials()
        loginEmailText.hideKeyboard()

        if (!isConnected()) {
            return
        }

        showFetchingNodes(fetchingNodes = false, generatingKeys = true)
        Timber.d("Generating keys")

        viewModel.performLogin()
    }

    /**
     * Updates the UI for logging.
     */
    private fun loginClicked() = with(binding) {
        loginEmailText.hideKeyboard()

        if (!isConnected()) {
            viewModel.resetOngoingTransfers()
            return
        }

        showFetchingNodes(fetchingNodes = false, generatingKeys = true)

        val typedEmail = loginEmailText.text.toString().lowercase().trim { it <= ' ' }
        val typedPassword = loginPasswordText.text.toString()
        Timber.d("Generating keys")

        viewModel.performLogin(typedEmail, typedPassword)
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

    private fun showLoginInProgress() = with(binding) {
        loginLoggingInText.isVisible = true
        loginFetchNodesText.isVisible = false
        loginPrepareNodesText.isVisible = false
        loginServersBusyText.isVisible = false
    }

    /**
     * Checks if there is a typed email and if has the correct format.
     * Gets the error string if not.
     */
    private val emailError: String?
        get() {
            val value = binding.loginEmailText.text.toString()
            return when {
                value.isEmpty() -> getString(R.string.error_enter_email)
                !Constants.EMAIL_ADDRESS.matcher(value)
                    .matches() -> getString(R.string.error_invalid_email)
                else -> null
            }
        }

    /**
     * Checks if there is a typed email.
     * Gets the error string if not.
     */
    private val passwordError: String?
        get() {
            return if (binding.loginPasswordText.text?.isEmpty() == true) {
                getString(R.string.error_enter_password)
            } else null
        }

    /**
     * Checks if the email and password are typed and has the correct format.
     * Shows errors if not.
     */
    private fun checkTypedValues() {
        val emailError = emailError
        val passwordError = passwordError
        setError(binding.loginEmailText, emailError)
        setError(binding.loginPasswordText, passwordError)

        when {
            emailError != null -> binding.loginEmailText.requestFocus()
            passwordError != null -> binding.loginPasswordText.requestFocus()
            else -> viewModel.checkOngoingTransfers()
        }
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
            text = getString(R.string.login_in_progress)
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

        if (!viewModel.isConnected) {
            showMessage(R.string.error_server_connection_problem)
            return
        }

        showFetchingNodes(fetchingNodes = false, queryingSignupLink = true)
        Timber.d("querySignupLink")
        accountConfirmationLink?.let { viewModel.checkSignupLink(it) }
    }

    /**
     * Checks pending actions and setups the final intent for launch before finish.
     */
    fun readyToFinish(uiState: LoginState) {
        (requireActivity() as LoginActivity).intent?.apply {
            @Suppress("UNCHECKED_CAST")
            intentShareInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getSerializableExtra(
                        FileExplorerActivity.EXTRA_SHARE_INFOS,
                        ArrayList::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    getSerializableExtra(FileExplorerActivity.EXTRA_SHARE_INFOS)
                } as ArrayList<ShareInfo>?

            when {
                intentShareInfo?.isNotEmpty() == true -> {
                    toSharePage()
                }
                Constants.ACTION_FILE_EXPLORER_UPLOAD == action && Constants.TYPE_TEXT_PLAIN == type -> {
                    startActivity(
                        Intent(
                            requireContext(),
                            FileExplorerActivity::class.java
                        ).putExtra(
                            Intent.EXTRA_TEXT,
                            getStringExtra(Intent.EXTRA_TEXT)
                        )
                            .putExtra(
                                Intent.EXTRA_SUBJECT,
                                getStringExtra(Intent.EXTRA_SUBJECT)
                            )
                            .putExtra(
                                Intent.EXTRA_EMAIL,
                                getStringExtra(Intent.EXTRA_EMAIL)
                            )
                            .setAction(Intent.ACTION_SEND)
                            .setType(Constants.TYPE_TEXT_PLAIN)
                    )
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

        confirmLogoutDialog?.dismiss()
        val loginActivity = requireActivity() as LoginActivity

        if (loginActivity.intent.getStringExtra(Constants.EXTRA_CONFIRMATION) == null
            && !uiState.isAccountConfirmed
        ) {
            if (getChatManagement().isPendingJoinLink()) {
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
                    if (intentAction == Constants.ACTION_REFRESH_API_SERVER
                        || intentAction == Constants.ACTION_REFRESH_AFTER_BLOCKED
                    ) {
                        intent.action = intentAction
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
            if (getChatManagement().isPendingJoinLink()) {
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

    /**
     * Shows a dialog for changing password.
     *
     * @param link Reset password link.
     */
    private fun showDialogInsertMKToChangePass(link: String?) {
        Timber.d("link: %s", link)
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(
            Util.scaleWidthPx(20, resources.displayMetrics),
            Util.scaleHeightPx(20, resources.displayMetrics),
            Util.scaleWidthPx(17, resources.displayMetrics),
            0
        )
        val input = EditText(requireContext())
        layout.addView(input, params)
        input.setSingleLine()
        input.hint = getString(R.string.edit_text_insert_mk)
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
                    input.error = getString(R.string.invalid_string)
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
        input.setImeActionLabel(getString(R.string.general_add), EditorInfo.IME_ACTION_DONE)
        val builder = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog
        ).setTitle(getString(R.string.title_dialog_insert_MK))
            .setMessage(getString(R.string.text_dialog_insert_MK))
            .setPositiveButton(getString(R.string.general_ok), null)
            .setNegativeButton(getString(R.string.general_cancel), null)
            .setView(layout)
            .setOnDismissListener {
                Util.hideKeyboard(requireActivity(), InputMethodManager.HIDE_NOT_ALWAYS)
            }
        insertMKDialog = builder.create().apply {
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                Timber.d("OK BUTTON PASSWORD")
                val value = input.text.toString().trim { it <= ' ' }
                if (value == "" || value.isEmpty()) {
                    Timber.w("Input is empty")
                    input.error = getString(R.string.invalid_string)
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
        confirmLogoutDialog?.dismiss()
        changeApiServerDialog?.dismiss()
        super.onDestroy()
    }

    /**
     * Shows a confirmation dialog before cancelling the current in progress login.
     */
    private fun showConfirmLogoutDialog() {
        confirmLogoutDialog = MaterialAlertDialogBuilder(requireContext())
            .setCancelable(true)
            .setMessage(getString(R.string.confirm_cancel_login))
            .setPositiveButton(getString(R.string.general_positive_button)) { _, _ ->
                backToLoginForm()
                viewModel.stopLogin()
            }.setNegativeButton(getString(R.string.general_negative_button), null)
            .show()
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

        return if (isLoggingIn || binding.loginProgressBar.isVisible || binding.login2fa.isVisible) {
            showConfirmLogoutDialog()
            2
        } else {
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
        isPinLongClick = false
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
            loginClicked()
        }
        setNegativeButton(R.string.general_cancel) { _, _ -> viewModel.resetOngoingTransfers() }
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
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.incorrect_MK_title))
            .setMessage(getString(R.string.incorrect_MK))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.general_ok), null)
            .show()
    }

    private fun showMessage(stringId: Int) =
        (requireActivity() as LoginActivity).showSnackbar(getString(stringId))

    private fun isConnected(): Boolean = if (!viewModel.isConnected) {
        showLoginScreen()
        showMessage(R.string.error_server_connection_problem)
        false
    } else true

    private fun showQuerySignupLinkResult(result: Result<String>) {
        showLoginScreen()
        binding.buttonForgotPass.isInvisible = true

        if (result.isSuccess) {
            with(binding) {
                buttonForgotPass.isInvisible = false
                loginProgressBar.isVisible = false
                loginTextView.text = getString(R.string.login_to_mega)
                buttonLogin.text = getString(R.string.login_text)
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

        viewModel.querySignupLinkResultShown()
    }

    companion object {
        private const val LONG_CLICK_DELAY: Long = 5000

        /**
         * Necessary times to click in a view to enable or disable logs.
         */
        const val CLICKS_TO_ENABLE_LOGS = 5
    }
}
