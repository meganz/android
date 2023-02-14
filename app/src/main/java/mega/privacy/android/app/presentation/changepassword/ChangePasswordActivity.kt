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
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.databinding.ActivityChangePasswordBinding
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.TestPasswordActivity
import mega.privacy.android.app.main.VerifyTwoFactorActivity
import mega.privacy.android.app.main.controllers.AccountController.Companion.logout
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.utils.ColorUtils.getThemeColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ConstantsUrl.RECOVERY_URL
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.qualifier.ApplicationScope
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class ChangePasswordActivity : PasscodeActivity(), View.OnClickListener,
    MegaRequestListenerInterface {
    @ApplicationScope
    @Inject
    internal lateinit var sharingScope: CoroutineScope

    private val viewModel: ChangePasswordViewModel by viewModels()

    private var changePassword = true
    private var linkToReset: String? = null
    private var mk: String? = null

    // TOP for 'terms of password'
    private var passwdValid = false
    private val imm: InputMethodManager by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    }

    private val binding: ActivityChangePasswordBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityChangePasswordBinding.inflate(layoutInflater)
    }

    private val progress: AlertDialog by lazy(LazyThreadSafetyMode.NONE) {
        createProgressDialog(this, getString(R.string.my_account_changing_password))
            .apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        passwdValid = false
        binding.containerPasswdElements.visibility = View.GONE
        binding.changePasswordNewPassword1Layout.isEndIconVisible = false
        binding.changePasswordNewPassword1.setOnFocusChangeListener { _: View?, hasFocus: Boolean ->
            binding.changePasswordNewPassword1Layout.isEndIconVisible = hasFocus
            if (!hasFocus) {
                checkFirstPasswordField()
            }
        }
        binding.changePasswordNewPassword1ErrorIcon.visibility = View.GONE
        binding.changePasswordNewPassword1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                Timber.d("Text changed: ${s}_${start}_${before}_${count}")
                if (s.isNotEmpty()) {
                    val temp = s.toString()
                    binding.containerPasswdElements.visibility = View.VISIBLE
                    checkPasswordStrength(temp.trim(), false)
                } else {
                    passwdValid = false
                    binding.containerPasswdElements.visibility = View.GONE
                }
            }

            override fun afterTextChanged(editable: Editable) {
                val normalHint =
                    StringResourcesUtils.getString(R.string.my_account_change_password_newPassword1)
                if (binding.changePasswordNewPassword1Layout.hint != null
                    && binding.changePasswordNewPassword1Layout.hint.toString() != normalHint
                ) {
                    binding.changePasswordNewPassword1Layout.hint = normalHint
                    binding.actionChangePassword.isEnabled = true
                    binding.actionChangePassword.alpha = ENABLED_BUTTON_ALPHA
                }
                if (editable.toString().isEmpty()) {
                    quitError(binding.changePasswordNewPassword1)
                }
                if (savedInstanceState != null && !binding.changePasswordNewPassword1.hasFocus()) {
                    checkFirstPasswordField()
                }
            }
        })
        binding.changePasswordNewPassword2Layout.isEndIconVisible = false
        binding.changePasswordNewPassword2.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus: Boolean ->
                binding.changePasswordNewPassword2Layout.isEndIconVisible = hasFocus
            }
        binding.changePasswordNewPassword2ErrorIcon.visibility = View.GONE
        binding.changePasswordNewPassword2.doAfterTextChanged {
            quitError(binding.changePasswordNewPassword2)
        }
        binding.actionChangePassword.setOnClickListener(this)
        binding.actionCancel.setOnClickListener { finish() }
        var textToShowTOP = getString(R.string.top)
        try {
            textToShowTOP = textToShowTOP.replace(
                "[B]", "<font color=\'"
                        + getThemeColorHexString(this, R.attr.colorSecondary)
                        + "\'>"
            )
                .replace("[/B]", "</font>")
                .replace("[A]", "<u>")
                .replace("[/A]", "</u>")
        } catch (e: Exception) {
            Timber.e(e, "Exception formatting string")
        }
        val resultTOP = Html.fromHtml(textToShowTOP, Html.FROM_HTML_MODE_LEGACY)
        binding.checkboxContainer.top.text = resultTOP
        binding.checkboxContainer.top.setOnClickListener(this)
        binding.checkboxContainer.chkTop.setOnClickListener(this)
        setSupportActionBar(binding.changePasswordToolbar)
        supportActionBar?.apply {
            title = getString(R.string.my_account_change_password)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.changePasswordScrollView.setOnScrollChangeListener { _: View?, _: Int, _: Int, _: Int, _: Int ->
            Util.changeActionBarElevation(
                this,
                binding.appBarLayoutChangePassword,
                binding.changePasswordScrollView.canScrollVertically(-1),
            )
        }
        when (intent?.action) {
            Constants.ACTION_RESET_PASS_FROM_LINK -> {
                Timber.d("ACTION_RESET_PASS_FROM_LINK")
                changePassword = false
                linkToReset = intent.dataString
                if (linkToReset == null) {
                    Timber.w("link is NULL - close activity")
                    finish()
                }
                mk = intent.getStringExtra(IntentConstants.EXTRA_MASTER_KEY)
                if (mk == null) {
                    Timber.w("MK is NULL - close activity")
                    Util.showAlert(
                        this,
                        getString(R.string.general_text_error),
                        getString(R.string.general_error_word)
                    )
                }
                supportActionBar?.title = getString(R.string.title_enter_new_password)
            }
            Constants.ACTION_RESET_PASS_FROM_PARK_ACCOUNT -> {
                changePassword = false
                Timber.d("ACTION_RESET_PASS_FROM_PARK_ACCOUNT")
                linkToReset = intent.dataString
                if (linkToReset == null) {
                    Timber.w("link is NULL - close activity")
                    Util.showAlert(
                        this,
                        getString(R.string.general_text_error),
                        getString(R.string.general_error_word)
                    )
                }
                mk = null
                supportActionBar?.title = getString(R.string.title_enter_new_password)
            }
        }
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
            R.id.action_change_password -> {
                if (changePassword) {
                    Timber.d("Ok proceed to change")
                    onChangePasswordClick()
                } else {
                    Timber.d("Reset pass on click")
                    if (linkToReset == null) {
                        Timber.w("link is NULL")
                        Util.showAlert(
                            this,
                            getString(R.string.general_text_error),
                            getString(R.string.general_error_word)
                        )
                    } else {
                        if (mk == null) {
                            Timber.d("Proceed to park account")
                            onResetPasswordClick(false)
                        } else {
                            Timber.d("Ok proceed to reset")
                            onResetPasswordClick(true)
                        }
                    }
                }
            }
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
                Timber.d("Show top")
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

    private fun checkPasswordStrength(s: String, isSamePassword: Boolean) {
        binding.apply {
            changePasswordNewPassword1Layout.isErrorEnabled = false
            val passWordStrength = megaApi.getPasswordStrength(s)
            when {
                isSamePassword || passWordStrength == MegaApiJava.PASSWORD_STRENGTH_VERYWEAK || s.length < 4 -> {
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
                    passwdValid = false
                    changePasswordNewPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_VeryWeak)
                    changePasswordNewPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_VeryWeak)
                }
                passWordStrength == MegaApiJava.PASSWORD_STRENGTH_WEAK -> {
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
                    passwdValid = true
                    changePasswordNewPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_Weak)
                    changePasswordNewPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Weak)
                }
                passWordStrength == MegaApiJava.PASSWORD_STRENGTH_MEDIUM -> {
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
                    passwdValid = true
                    changePasswordNewPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_Medium)
                    changePasswordNewPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Medium)
                }
                passWordStrength == MegaApiJava.PASSWORD_STRENGTH_GOOD -> {
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
                    passwdValid = true
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
                    passwdValid = true
                    changePasswordNewPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_Strong)
                    changePasswordNewPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Strong)
                }
            }
            changePasswordNewPassword1ErrorIcon.visibility = View.GONE
            changePasswordNewPassword1Layout.error = " "
            changePasswordNewPassword1Layout.isErrorEnabled = true
        }
    }

    private fun onResetPasswordClick(hasMk: Boolean) {
        Timber.d("hasMk: %s", hasMk)
        if (viewModel.isConnectedToNetwork().not()) {
            showSnackbar(getString(R.string.error_server_connection_problem))
            return
        }
        if (!validateForm(false)) {
            return
        }
        imm.hideSoftInputFromWindow(binding.changePasswordNewPassword1.windowToken, 0)
        imm.hideSoftInputFromWindow(binding.changePasswordNewPassword2.windowToken, 0)
        val newPass1 = binding.changePasswordNewPassword1.text.toString()
        progress.setMessage(getString(R.string.my_account_changing_password))
        progress.show()
        if (hasMk) {
            Timber.d("reset with mk")
            megaApi.confirmResetPassword(linkToReset, newPass1, mk, this)
        } else {
            megaApi.confirmResetPassword(linkToReset, newPass1, null, this)
        }
    }

    private fun onChangePasswordClick() {
        Timber.d("onChangePasswordClick")
        if (viewModel.isConnectedToNetwork().not()) {
            showSnackbar(getString(R.string.error_server_connection_problem))
            return
        }
        if (!validateForm(true)) {
            return
        }
        imm.hideSoftInputFromWindow(binding.changePasswordNewPassword1.windowToken, 0)
        imm.hideSoftInputFromWindow(binding.changePasswordNewPassword2.windowToken, 0)
        megaApi.multiFactorAuthCheck(megaApi.myEmail, this)
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
        val value = binding.changePasswordNewPassword1.text.toString()
        return when {
            value.isEmpty() -> getString(R.string.error_enter_password)
            megaApi.checkPassword(value) -> StringResourcesUtils.getString(R.string.error_same_password)
            passwdValid.not() -> getString(R.string.error_password).also {
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
        val confirm = binding.changePasswordNewPassword1.text.toString()
        if (value.isEmpty()) {
            return getString(R.string.error_enter_password)
        } else if (value != confirm) {
            return getString(R.string.error_passwords_dont_match)
        }
        return null
    }

    private fun changePassword(newPassword: String) {
        Timber.d("changePassword")
        megaApi.changePassword(null, newPassword, this)
        progress.apply {
            setMessage(getString(R.string.my_account_changing_password))
            show()
        }
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestStart: %s", request.name)
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish")
        when (request.type) {
            MegaRequest.TYPE_CHANGE_PW -> {
                Timber.d("TYPE_CHANGE_PW")
                if (e.errorCode != MegaError.API_OK) {
                    Timber.w(
                        "e.getErrorCode = %d__ e.getErrorString = %s",
                        e.errorCode,
                        e.errorString
                    )
                    try {
                        progress.dismiss()
                    } catch (ex: Exception) {
                        Timber.w(ex, "Exception dismissing progress dialog")
                    }
                    showSnackbar(getString(R.string.general_text_error))
                } else {
                    Timber.d("Pass changed OK")
                    try {
                        progress.dismiss()
                    } catch (ex: Exception) {
                        Timber.w(ex, "Exception dismissing progress dialog")
                    }
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
                    if (intent != null && intent.getBooleanExtra(KEY_IS_LOGOUT, false)) {
                        logout(this, megaApi, sharingScope)
                    } else {
                        //Intent to MyAccount
                        val resetPassIntent = Intent(this, ManagerActivity::class.java)
                        resetPassIntent.action = Constants.ACTION_PASS_CHANGED
                        resetPassIntent.putExtra(Constants.RESULT, e.errorCode)
                        startActivity(resetPassIntent)
                        finish()
                    }
                }
            }
            MegaRequest.TYPE_CONFIRM_RECOVERY_LINK -> {
                Timber.d("TYPE_CONFIRM_RECOVERY_LINK")
                try {
                    progress.dismiss()
                } catch (ex: Exception) {
                    Timber.w(ex, "Exception dismissing progress dialog")
                }
                if (e.errorCode != MegaError.API_OK) {
                    Timber.w(
                        "e.getErrorCode = %d__ e.getErrorString = %s",
                        e.errorCode,
                        e.errorString
                    )
                } else {
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
                }
                val resetPassIntent: Intent
                if (megaApi.rootNode == null) {
                    Timber.d("Not logged in")

                    //Intent to Login
                    resetPassIntent = Intent(this, LoginActivity::class.java)
                    resetPassIntent.putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                } else {
                    Timber.d("Logged IN")
                    resetPassIntent = Intent(this, ManagerActivity::class.java)
                }
                resetPassIntent.action = Constants.ACTION_PASS_CHANGED
                resetPassIntent.putExtra(Constants.RESULT, e.errorCode)
                startActivity(resetPassIntent)
                finish()
            }
            MegaRequest.TYPE_MULTI_FACTOR_AUTH_CHECK -> {
                if (e.errorCode == MegaError.API_OK) {
                    if (request.flag) {
                        val intent = Intent(this, VerifyTwoFactorActivity::class.java)
                        intent.putExtra(
                            VerifyTwoFactorActivity.KEY_VERIFY_TYPE,
                            Constants.CHANGE_PASSWORD_2FA
                        )
                        intent.putExtra(
                            VerifyTwoFactorActivity.KEY_NEW_PASSWORD,
                            binding.changePasswordNewPassword1.text.toString()
                        )
                        intent.putExtra(
                            KEY_IS_LOGOUT,
                            getIntent() != null && getIntent().getBooleanExtra(
                                KEY_IS_LOGOUT, false
                            )
                        )
                        startActivity(intent)
                    } else {
                        changePassword(binding.changePasswordNewPassword1.text.toString())
                    }
                }
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    private fun setError(editText: EditText, error: String?) {
        Timber.d("setError")
        if (error.isNullOrEmpty()) return
        when (editText.id) {
            R.id.change_password_newPassword1 -> {
                val samePasswordError = StringResourcesUtils.getString(R.string.error_same_password)
                if (error == samePasswordError) {
                    checkPasswordStrength(editText.text.toString(), true)
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

    override fun onRequestTemporaryError(
        api: MegaApiJava, request: MegaRequest,
        e: MegaError,
    ) {
        Timber.w("onRequestTemporaryError: %s", request.name)
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {

    }

    public override fun onDestroy() {
        megaApi.removeRequestListener(this)
        super.onDestroy()
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
        internal const val KEY_IS_LOGOUT = "logout"
        private const val DISABLED_BUTTON_ALPHA = 0.5f
        private const val ENABLED_BUTTON_ALPHA = 1f
    }
}