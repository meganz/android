package mega.privacy.android.app.presentation.changepassword

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityChangePasswordBinding
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.TestPasswordActivity
import mega.privacy.android.app.main.VerifyTwoFactorActivity
import mega.privacy.android.app.main.VerifyTwoFactorActivity.Companion.KEY_NEW_PASSWORD
import mega.privacy.android.app.main.VerifyTwoFactorActivity.Companion.KEY_VERIFY_TYPE
import mega.privacy.android.app.main.controllers.AccountController
import mega.privacy.android.app.presentation.changepassword.model.ChangePasswordUIState
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.utils.ColorUtils.getThemeColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.CHANGE_PASSWORD_2FA
import mega.privacy.android.app.utils.ConstantsUrl.RECOVERY_URL
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.domain.qualifier.ApplicationScope
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class ChangePasswordActivity : PasscodeActivity(), View.OnClickListener {
    @ApplicationScope
    @Inject
    internal lateinit var sharingScope: CoroutineScope
    private val viewModel: ChangePasswordViewModel by viewModels()

    // TOP for 'terms of password'
    private val imm: InputMethodManager by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    }

    private val binding: ActivityChangePasswordBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityChangePasswordBinding.inflate(layoutInflater)
    }

    private val passwordText: String
        get() = binding.changePasswordNewPassword1.text.toString()

    private val progress: AlertDialog by lazy(LazyThreadSafetyMode.NONE) {
        createProgressDialog(this, getString(R.string.my_account_changing_password))
            .apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
            }
    }

    /**
     * Checks whether device is connected to network
     * @return true when currently device has network connection, else false
     * only true during this time, network connection status may change because of how flow works.
     */
    private val isConnectedToNetwork: Boolean
        get() = viewModel.uiState.value.isConnectedToNetwork

    /**
     * Checks whether the password the user enters is the same as the current password
     * returns true if it's the same, else false
     */
    private val isPasswordTheSame: Boolean
        get() = viewModel.uiState.value.isCurrentPassword

    /**
     * Checks whether the password the user enters is a valid password
     * returns true when valid, else false
     */
    private val isPasswordValid: Boolean
        get() = viewModel.uiState.value.passwordStrength > PasswordStrength.INVALID

    /**
     * Checks whether current screen mode is reset password
     * returns true if reset password, false if change password
     */
    private val isResetPasswordMode: Boolean
        get() = viewModel.uiState.value.isResetPasswordMode

    /**
     * Checks whether reset link is valid, returns true if valid, else false
     */
    private val isResetLinkValid: Boolean
        get() = viewModel.uiState.value.isResetPasswordLinkValid

    private fun initState() {
        if (intent.getStringExtra(KEY_LINK_TO_RESET).isNullOrBlank()) {
            intent.putExtra(KEY_LINK_TO_RESET, intent.dataString)
        }

        if (intent.getStringExtra(KEY_ACTION).isNullOrBlank()) {
            intent.putExtra(KEY_ACTION, intent.action)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initState()
        collectUIState()
        bindView(savedInstanceState)
        viewModel.determineIfScreenIsResetPasswordMode()
    }

    private fun bindView(savedInstanceState: Bundle?) {
        with(binding) {
            containerPasswdElements.visibility = View.GONE
            changePasswordNewPassword1Layout.isEndIconVisible = false
            changePasswordNewPassword1.setOnFocusChangeListener { _: View?, hasFocus: Boolean ->
                changePasswordNewPassword1Layout.isEndIconVisible = hasFocus
                if (!hasFocus) {
                    checkFirstPasswordField()
                }
            }
            changePasswordNewPassword1ErrorIcon.visibility = View.GONE
            changePasswordNewPassword1.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int,
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    Timber.d("Text changed: ${s}_${start}_${before}_${count}")
                    if (s.isNotEmpty()) {
                        val temp = s.toString()
                        containerPasswdElements.visibility = View.VISIBLE
                        viewModel.checkPasswordStrength(temp.trim())
                    } else {
                        containerPasswdElements.visibility = View.GONE
                    }
                }

                override fun afterTextChanged(editable: Editable) {
                    val normalHint =
                        StringResourcesUtils.getString(R.string.my_account_change_password_newPassword1)
                    if (changePasswordNewPassword1Layout.hint != null
                        && changePasswordNewPassword1Layout.hint.toString() != normalHint
                    ) {
                        changePasswordNewPassword1Layout.hint = normalHint
                        actionChangePassword.isEnabled = true
                        actionChangePassword.alpha = ENABLED_BUTTON_ALPHA
                    }
                    if (editable.toString().isEmpty()) {
                        quitError(changePasswordNewPassword1)
                    }
                    if (savedInstanceState != null && !changePasswordNewPassword1.hasFocus()) {
                        checkFirstPasswordField()
                    }
                }
            })
            changePasswordNewPassword2Layout.isEndIconVisible = false
            changePasswordNewPassword2.onFocusChangeListener =
                View.OnFocusChangeListener { _, hasFocus: Boolean ->
                    changePasswordNewPassword2Layout.isEndIconVisible = hasFocus
                }
            changePasswordNewPassword2ErrorIcon.visibility = View.GONE
            changePasswordNewPassword2.doAfterTextChanged {
                quitError(changePasswordNewPassword2)
            }
            actionChangePassword.setOnClickListener {
                onClickChangePasswordButton()
            }
            actionCancel.setOnClickListener { finish() }
            checkboxContainer.top.text =
                Html.fromHtml(getCheckboxTNCText(), Html.FROM_HTML_MODE_LEGACY)

            checkboxContainer.top.setOnClickListener { onClickTNCCheckbox() }

            setSupportActionBar(changePasswordToolbar)
            supportActionBar?.apply {
                title = getString(R.string.my_account_change_password)
                setHomeButtonEnabled(true)
                setDisplayHomeAsUpEnabled(true)
            }
            changePasswordScrollView.setOnScrollChangeListener { _: View?, _: Int, _: Int, _: Int, _: Int ->
                Util.changeActionBarElevation(
                    this@ChangePasswordActivity,
                    appBarLayoutChangePassword,
                    changePasswordScrollView.canScrollVertically(-1),
                )
            }
        }
    }

    private fun collectUIState() {
        this.collectFlow(viewModel.uiState) { state ->
            handleScreenModeState(state)
            handleAlertMessageState(state)
            handleSnackBarMessageState(state)
            handleLoadingProgressState(state)
            handleMultiFactorAuthState(state)
            handlePasswordChangedState(state)
            handlePasswordResetState(state)
            handlePasswordValidationState(state)
        }
    }

    private fun handleScreenModeState(state: ChangePasswordUIState) {
        if (state.isResetPasswordLinkValid.not()) {
            finish()
            return
        }

        if (state.isResetPasswordMode) {
            supportActionBar?.title = getString(R.string.title_enter_new_password)
        }
    }

    private fun handleAlertMessageState(state: ChangePasswordUIState) {
        if (state.isShowAlertMessage) {
            showAlert()
            viewModel.onAlertMessageShown()
        }
    }

    private fun handleSnackBarMessageState(state: ChangePasswordUIState) {
        if (state.snackBarMessage != null) {
            showSnackbar(getString(state.snackBarMessage))
            viewModel.onSnackBarShown()
        }
    }

    private fun handleLoadingProgressState(state: ChangePasswordUIState) {
        if (state.loadingMessage != null) {
            progress.apply {
                setMessage(getString(state.loadingMessage))
                show()
            }
        } else {
            progress.dismiss()
        }
    }

    private fun handleMultiFactorAuthState(uiState: ChangePasswordUIState) {
        if (uiState.isPromptedMultiFactorAuth) {
            navigateToMultiFactorAuthScreen()
            viewModel.onMultiFactorAuthShown()
        }
    }

    private fun handlePasswordChangedState(state: ChangePasswordUIState) {
        if (state.isPasswordChanged) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            if (intent != null && intent.getBooleanExtra(KEY_IS_LOGOUT, false)) {
                AccountController.logout(this, megaApi, sharingScope)
            } else {
                navigateAfterPasswordChanged()
            }

            viewModel.onPasswordChanged()
        }
    }

    private fun handlePasswordResetState(state: ChangePasswordUIState) {
        if (state.isPasswordReset) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            navigateAfterPasswordReset(state.isUserLoggedIn, state.errorCode)
            viewModel.onPasswordReset()
        }
    }

    private fun navigateAfterPasswordChanged() {
        val intent = Intent(this, ManagerActivity::class.java).apply {
            action = Constants.ACTION_PASS_CHANGED
            putExtra(Constants.RESULT, MegaError.API_OK)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateAfterPasswordReset(isUserLoggedIn: Boolean, errorCode: Int?) {
        val intent: Intent = if (isUserLoggedIn) {
            Intent(this, ManagerActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java).apply {
                putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
            }
        }.apply {
            action = Constants.ACTION_PASS_CHANGED
            putExtra(Constants.RESULT, errorCode)
        }

        startActivity(intent)
        finish()
    }

    private fun handlePasswordValidationState(state: ChangePasswordUIState) {
        if (state.passwordStrength > PasswordStrength.INVALID) {
            checkPasswordStrength(
                state.passwordStrength.value,
                state.isCurrentPassword
            )
        }

        if (state.isCurrentPassword) {
            checkFirstPasswordField()
        }
    }

    private fun navigateToMultiFactorAuthScreen() {
        val intent = Intent(this, VerifyTwoFactorActivity::class.java).apply {
            putExtra(KEY_VERIFY_TYPE, CHANGE_PASSWORD_2FA)
            putExtra(KEY_NEW_PASSWORD, binding.changePasswordNewPassword1.text.toString())
            putExtra(KEY_IS_LOGOUT, intent != null && intent.getBooleanExtra(KEY_IS_LOGOUT, false))
        }

        startActivity(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        Timber.d("onBackPressed")
        if (intent?.getBooleanExtra(KEY_IS_LOGOUT, false) == true) {
            val intent = Intent(this, TestPasswordActivity::class.java)
            intent.putExtra(KEY_IS_LOGOUT, true)
            startActivity(intent)
        }
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View) {
        Timber.d("onClick")
        when (v.id) {
            R.id.lost_authentication_device -> {
                try {
                    val openTermsIntent = Intent(this, WebViewActivity::class.java)
                    openTermsIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    openTermsIntent.data = Uri.parse(RECOVERY_URL)
                    startActivity(openTermsIntent)
                } catch (e: Exception) {
                    val viewIntent = Intent(Intent.ACTION_VIEW)
                    viewIntent.data = Uri.parse(RECOVERY_URL)
                    startActivity(viewIntent)
                }
            }
            R.id.top -> {
                try {
                    val openTermsIntent = Intent(this, WebViewActivity::class.java)
                    openTermsIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    openTermsIntent.data = Uri.parse(Constants.URL_E2EE)
                    startActivity(openTermsIntent)
                } catch (e: Exception) {
                    val viewIntent = Intent(Intent.ACTION_VIEW)
                    viewIntent.data = Uri.parse(Constants.URL_E2EE)
                    startActivity(viewIntent)
                }
            }
        }
    }

    private fun checkPasswordStrength(passwordStrength: Int, isCurrentPassword: Boolean) {
        binding.apply {
            changePasswordNewPassword1Layout.isErrorEnabled = false

            when {
                isCurrentPassword || passwordStrength == MegaApiJava.PASSWORD_STRENGTH_VERYWEAK -> {
                    shapePasswdFirst.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.passwd_very_weak
                        )
                    shapePasswdSecond.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.shape_password
                        )
                    shapePasswdThird.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.shape_password
                        )
                    shapePasswdFourth.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.shape_password
                        )
                    shapePasswdFifth.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.shape_password
                        )
                    passwordType.text = getString(R.string.pass_very_weak)
                    passwordType.setTextColor(
                        ContextCompat.getColor(
                            this@ChangePasswordActivity,
                            R.color.red_600_red_300
                        )
                    )
                    passwordAdviceText.text = getString(R.string.passwd_weak)
                    changePasswordNewPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_VeryWeak)
                    changePasswordNewPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_VeryWeak)
                }
                passwordStrength == MegaApiJava.PASSWORD_STRENGTH_WEAK -> {
                    shapePasswdFirst.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.passwd_weak
                        )
                    shapePasswdSecond.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.passwd_weak
                        )
                    shapePasswdThird.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.shape_password
                        )
                    shapePasswdFourth.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.shape_password
                        )
                    shapePasswdFifth.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.shape_password
                        )
                    passwordType.text = getString(R.string.pass_weak)
                    passwordType.setTextColor(
                        ContextCompat.getColor(
                            this@ChangePasswordActivity,
                            R.color.yellow_600_yellow_300
                        )
                    )
                    passwordAdviceText.text = getString(R.string.passwd_weak)
                    changePasswordNewPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_Weak)
                    changePasswordNewPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Weak)
                }
                passwordStrength == MegaApiJava.PASSWORD_STRENGTH_MEDIUM -> {
                    shapePasswdFirst.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.passwd_medium
                        )
                    shapePasswdSecond.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.passwd_medium
                        )
                    shapePasswdThird.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.passwd_medium
                        )
                    shapePasswdFourth.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.shape_password
                        )
                    shapePasswdFifth.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.shape_password
                        )
                    passwordType.text = getString(R.string.pass_medium)
                    passwordType.setTextColor(
                        ContextCompat.getColor(
                            this@ChangePasswordActivity,
                            R.color.green_500_green_400
                        )
                    )
                    passwordAdviceText.text = getString(R.string.passwd_medium)
                    changePasswordNewPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_Medium)
                    changePasswordNewPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Medium)
                }
                passwordStrength == MegaApiJava.PASSWORD_STRENGTH_GOOD -> {
                    shapePasswdFirst.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.passwd_good
                        )
                    shapePasswdSecond.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.passwd_good
                        )
                    shapePasswdThird.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.passwd_good
                        )
                    shapePasswdFourth.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.passwd_good
                        )
                    shapePasswdFifth.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.shape_password
                        )
                    passwordType.text = getString(R.string.pass_good)
                    passwordType.setTextColor(
                        ContextCompat.getColor(
                            this@ChangePasswordActivity,
                            R.color.lime_green_500_200
                        )
                    )
                    passwordAdviceText.text = getString(R.string.passwd_good)
                    changePasswordNewPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_Good)
                    changePasswordNewPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Good)
                }
                else -> {
                    shapePasswdFirst.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.passwd_strong
                        )
                    shapePasswdSecond.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.passwd_strong
                        )
                    shapePasswdThird.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.passwd_strong
                        )
                    shapePasswdFourth.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.passwd_strong
                        )
                    shapePasswdFifth.background =
                        ContextCompat.getDrawable(
                            this@ChangePasswordActivity,
                            R.drawable.passwd_strong
                        )
                    passwordType.text = getString(R.string.pass_strong)
                    passwordType.setTextColor(
                        ContextCompat.getColor(
                            this@ChangePasswordActivity,
                            R.color.dark_blue_500_200
                        )
                    )
                    passwordAdviceText.text = getString(R.string.passwd_strong)
                    changePasswordNewPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_Strong)
                    changePasswordNewPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Strong)
                }
            }
            changePasswordNewPassword1ErrorIcon.visibility = View.GONE
            changePasswordNewPassword1Layout.error = " "
            changePasswordNewPassword1Layout.isErrorEnabled = true
        }
    }

    /*
    * Format and replace HTML text for TNC checkbox text
    */
    private fun getCheckboxTNCText(): String? {
        return try {
            getString(R.string.top).replace(
                "[B]", "<font color=\'"
                        + getThemeColorHexString(this, R.attr.colorSecondary)
                        + "\'>"
            ).replace("[/B]", "</font>")
                .replace("[A]", "<u>")
                .replace("[/A]", "</u>")
        } catch (e: Exception) {
            Timber.e(e, "Exception formatting string")
            null
        }
    }

    /*
    * Action when TNC checkbox link is clicked
    */
    private fun onClickTNCCheckbox() {
        try {
            val intent = Intent(this, WebViewActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                data = Uri.parse(Constants.URL_E2EE)
            }
            startActivity(intent)
        } catch (e: Exception) {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(Constants.URL_E2EE)
            }
            startActivity(viewIntent)
        }
    }

    /*
     * Action when change password button is clicked
     */
    private fun onClickChangePasswordButton() {
        if (isResetPasswordMode) {
            if (isResetLinkValid) {
                onTriggerResetPassword()
            } else {
                showAlert()
            }
        } else {
            onTriggerChangePassword()
        }
    }

    /*
     * Action trigger when screen mode is reset password
     */
    private fun onTriggerResetPassword() {
        validatePassword(withOldPassword = false) {
            viewModel.onExecuteResetPassword(passwordText)
        }
    }

    /*
     * Action trigger when screen mode is change password
     */
    private fun onTriggerChangePassword() {
        validatePassword(withOldPassword = true) {
            viewModel.onUserClickChangePassword(passwordText)
        }
    }

    private fun validatePassword(withOldPassword: Boolean, onValidated: () -> Unit) {
        if (isConnectedToNetwork.not()) {
            showSnackbar(getString(R.string.error_server_connection_problem))
            return
        }
        if (!validateForm(withOldPassword)) {
            return
        }

        listOf(binding.changePasswordNewPassword1, binding.changePasswordNewPassword2).forEach {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }

        onValidated()
    }

    /*
     * Validate old password and new passwords
     */
    private fun validateForm(withOldPass: Boolean): Boolean {
        if (withOldPass) {
            val newPassword1Error = getNewPassword1Error()
            val newPassword2Error = getNewPassword2Error()
            setError(binding.changePasswordNewPassword1, newPassword1Error)
            setError(binding.changePasswordNewPassword2, newPassword2Error)
            if (newPassword1Error != null) {
                binding.changePasswordNewPassword1.requestFocus()
                return false
            } else if (newPassword2Error != null) {
                binding.changePasswordNewPassword2.requestFocus()
                return false
            }
        } else {
            val newPassword2Error = getNewPassword2Error()
            setError(binding.changePasswordNewPassword2, newPassword2Error)
            if (checkFirstPasswordField()) {
                binding.changePasswordNewPassword1.requestFocus()
                return false
            } else if (newPassword2Error != null) {
                binding.changePasswordNewPassword2.requestFocus()
                return false
            }
        }
        if (!binding.checkboxContainer.chkTop.isChecked) {
            showSnackbar(getString(R.string.create_account_no_top))
            return false
        }
        return true
    }

    /*
     * Validate new password1
     */
    private fun getNewPassword1Error(): String? {
        return when {
            passwordText.isEmpty() -> getString(R.string.error_enter_password)
            isPasswordTheSame -> StringResourcesUtils.getString(R.string.error_same_password)
            isPasswordValid.not() -> getString(R.string.error_password).also {
                binding.containerPasswdElements.visibility = View.GONE
            }
            else -> null
        }
    }

    /*
     * Validate new password2
     */
    private fun getNewPassword2Error(): String? {
        val value = binding.changePasswordNewPassword2.text.toString()

        if (value.isEmpty()) {
            return getString(R.string.error_enter_password)
        } else if (value != passwordText) {
            return getString(R.string.error_passwords_dont_match)
        }
        return null
    }

    @SuppressLint("NonConstantResourceId")
    private fun setError(editText: EditText, error: String?) {
        Timber.d("setError")
        if (error.isNullOrEmpty()) return
        when (editText.id) {
            R.id.change_password_newPassword1 -> {
                val samePasswordError = StringResourcesUtils.getString(R.string.error_same_password)
                if (error == samePasswordError) {
                    viewModel.checkPasswordStrength(editText.text.toString())
                    binding.changePasswordNewPassword1Layout.hint = samePasswordError
                    binding.changePasswordNewPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_Error)
                    binding.changePasswordNewPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Error)
                    binding.actionChangePassword.isEnabled = false
                    binding.actionChangePassword.alpha = DISABLED_BUTTON_ALPHA
                } else {
                    binding.changePasswordNewPassword1Layout.error = error
                    binding.changePasswordNewPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_Error)
                    binding.changePasswordNewPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Error)
                    binding.changePasswordNewPassword1ErrorIcon.visibility = View.VISIBLE
                }
            }
            R.id.change_password_newPassword2 -> {
                binding.changePasswordNewPassword2Layout.error = error
                binding.changePasswordNewPassword2Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_Error)
                binding.changePasswordNewPassword2ErrorIcon.visibility = View.VISIBLE
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    private fun quitError(editText: EditText) {
        when (editText.id) {
            R.id.change_password_newPassword1 -> {
                binding.changePasswordNewPassword1Layout.error = null
                binding.changePasswordNewPassword1Layout.setHintTextAppearance(R.style.TextAppearance_Design_Hint)
                binding.changePasswordNewPassword1ErrorIcon.visibility = View.GONE
            }
            R.id.change_password_newPassword2 -> {
                binding.changePasswordNewPassword2Layout.error = null
                binding.changePasswordNewPassword2Layout.setHintTextAppearance(R.style.TextAppearance_Design_Hint)
                binding.changePasswordNewPassword2ErrorIcon.visibility = View.GONE
            }
        }
    }

    private fun showAlert() {
        Util.showAlert(
            this,
            getString(R.string.general_text_error),
            getString(R.string.general_error_word)
        )
    }

    private fun showSnackbar(s: String) {
        showSnackbar(binding.changePasswordContainer, s)
    }

    private fun checkFirstPasswordField(): Boolean {
        val error = getNewPassword1Error()
        if (error != null) {
            setError(binding.changePasswordNewPassword1, error)
            return true
        }
        return false
    }

    companion object {
        const val KEY_LINK_TO_RESET = "key_link_to_reset"
        const val KEY_ACTION = "key_action"
        internal const val KEY_IS_LOGOUT = "logout"
        private const val DISABLED_BUTTON_ALPHA = 0.5f
        private const val ENABLED_BUTTON_ALPHA = 1f
    }
}