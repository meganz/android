package mega.privacy.android.app.lollipop

import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_pin_lock.*
import kotlinx.android.synthetic.main.bottom_sheet_contact_file_list.*
import kotlinx.android.synthetic.main.content_file_info_activity.*
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.components.EditTextPIN
import mega.privacy.android.app.databinding.ActivityVerifyTwoFactorBinding
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.*
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util.*
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequest.*

@AndroidEntryPoint
class VerifyTwoFactorActivity : PinActivityLollipop() {

    private lateinit var binding: ActivityVerifyTwoFactorBinding

    private var verifyType: Int = 0

    private var is2FAEnabled = false

    private var isFirstTime2fa = true

    private var isErrorShown = false

    private var pinLongClick = false

    private var newEmail: String? = null
    private var newPassword: String? = null

    private lateinit var progressBar: ProgressBar
    private lateinit var pinError: TextView

    companion object {
        const val KEY_VERIFY_TYPE = "key_verify_type"
        const val KEY_NEW_EMAIL = "key_new_email"
        const val KEY_NEW_PASSWORD = "key_new_password"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyTwoFactorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        verifyType = intent.getIntExtra(KEY_VERIFY_TYPE, 0)
        newEmail = intent.getStringExtra(KEY_NEW_EMAIL)
        newPassword = intent.getStringExtra(KEY_NEW_PASSWORD)

        initializeView()

        // Check current 2fa enable state.
        megaApi.multiFactorAuthCheck(megaApi.myEmail, listener)
    }

    private fun initializeView() {
        setSubTitle()

        binding.lostAuthenticationDevice.setOnClickListener {
            try {
                val url = "https://mega.nz/recovery"
                val openTermsIntent = Intent(this, WebViewActivity::class.java)
                openTermsIntent.apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    data = Uri.parse(url)
                }

                startActivity(openTermsIntent)
            } catch (e: Exception) {
                val viewIntent = Intent(Intent.ACTION_VIEW)
                viewIntent.data = Uri.parse("https://mega.nz/recovery")
                startActivity(viewIntent)
            }
        }

        progressBar = binding.progressbarVerify2fa
        pinError = binding.pin2faErrorVerify

        setEditTextPINs()
    }

    private fun setSubTitle() {
        val stringId = when (verifyType) {
            CANCEL_ACCOUNT_2FA -> R.string.verify_2fa_subtitle_cancel_account
            CHANGE_MAIL_2FA -> R.string.verify_2fa_subtitle_change_email
            CHANGE_PASSWORD_2FA -> R.string.verify_2fa_subtitle_change_password
            else -> 0
        }

        if (stringId != 0) {
            binding.toolbar.subtitle = StringResourcesUtils.getString(stringId)
        }
    }

    private fun setEditTextPINs() {
        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        processEditTextPINs { index, total, editTextPIN ->
            if (index == 0) {
                editTextPIN.requestFocus()
            } else {
                editTextPIN.setEt(getEditTextPINByIndex(index - 1))
            }

            imm.showSoftInput(editTextPIN, InputMethodManager.SHOW_FORCED)

            editTextPIN.setOnLongClickListener {
                pinLongClick = true
                editTextPIN.requestFocus()
                false
            }

            editTextPIN.setOnFocusChangeListener { _, hasFoucs ->
                if (hasFoucs) {
                    editTextPIN.setText("")
                }
            }

            editTextPIN.addTextChangedListener(object : AfterTextChangedCallback() {

                override fun afterTextChanged(s: Editable) {
                    if (editTextPIN.length() != 0) {
                        val next = getEditTextPINByIndex(index + 1)
                        next.requestFocus()
                        next.isCursorVisible = true


                        val isLastOne = index == total - 1
                        if (isLastOne) {
                            hideKeyboard()
                        }

                        if (isFirstTime2fa && !pinLongClick && !isLastOne) {
                            for (i in index + 1 until total) {
                                getEditTextPINByIndex(i).setText("")
                            }
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
    }

    private fun hideKeyboard() = hideKeyboard(this@VerifyTwoFactorActivity, 0)

    private fun processEditTextPINs(process: (index: Int, total: Int, editTextPIN: EditTextPIN) -> Unit) {
        val childrenCount = binding.sixPinVerify.childCount

        for (i in 0 until childrenCount) {
            process(i, childrenCount, getEditTextPINByIndex(i))
        }
    }

    fun pasteClipboard() {
        logDebug("pasteClipboard")
        pinLongClick = false
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        if (clipData != null) {
            val code = clipData.getItemAt(0).text.toString()
            if (code.length == 6) {
                var areDigits = true
                for (i in 0..5) {
                    if (!Character.isDigit(code[i])) {
                        areDigits = false
                        break
                    }
                }

                if (areDigits) {
                    processEditTextPINs { index, _, editTextPIN ->
                        editTextPIN.setText("${code[index]}")
                    }
                } else {
                    processEditTextPINs { _, _, editTextPIN ->
                        editTextPIN.setText("")
                    }
                }
            }
        }
    }

    private fun getEditTextPINByIndex(index: Int): EditTextPIN {
        val totalCount = binding.sixPinVerify.childCount

        // Return first one.
        if (index < 0) {
            return getEditTextPINByIndex(0)
        }

        // Return last one.
        if (index == totalCount) {
            return getEditTextPINByIndex(totalCount - 1)
        }

        return binding.sixPinVerify.getChildAt(index) as EditTextPIN
    }

    fun verifyQuitError() {
        isErrorShown = false

        pinError.visibility = View.GONE

        processEditTextPINs { _, _, editTextPIN ->
            editTextPIN.setTextColor(ContextCompat.getColor(this, R.color.grey_087_white_087))
        }
    }

    fun verifyShowError() {
        logWarning("Pin not correct verifyShowError")
        isFirstTime2fa = false
        isErrorShown = true

        pinError.visibility = View.VISIBLE

        processEditTextPINs { _, _, editTextPIN ->
            editTextPIN.setTextColor(ContextCompat.getColor(this, R.color.red_600_red_300));
        }
    }

    fun permitVerify() {
        logDebug("permitVerify")
        var allInput = true
        val sb = StringBuilder(6)

        processEditTextPINs { _, _, editTextPIN ->
            if (editTextPIN.length() != 1) {
                allInput = false
            }

            sb.append(editTextPIN.text)
        }

        if (allInput && !isErrorShown) {
            progressBar.visibility = View.VISIBLE
            verify2FA(sb.toString())
        }
    }

    fun verify2FA(pin: String) {
        when (verifyType) {
            CANCEL_ACCOUNT_2FA -> megaApi.multiFactorAuthCancelAccount(pin, listener)
            CHANGE_MAIL_2FA -> megaApi.multiFactorAuthChangeEmail(newEmail, pin, listener)
            DISABLE_2FA -> megaApi.multiFactorAuthDisable(pin, null)
            CHANGE_PASSWORD_2FA -> megaApi.multiFactorAuthChangePassword(
                null,
                "newPassword",
                pin,
                null
            )
        }
    }

    private fun doCheck2fa(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (e.errorCode == MegaError.API_OK) {
            is2FAEnabled = request.flag
        }
    }

    private fun doChangeEmail(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        logDebug("${request.email}  ${e.errorCode}")
        if (e.errorCode == MegaError.API_OK) {
            logDebug("The change link has been sent")

            showAlert(
                this,
                getString(R.string.email_verification_text_change_mail),
                getString(R.string.email_verification_title)
            ) {
                finish()
            }
        } else if (e.errorCode == MegaError.API_EACCESS) {
            logWarning("The new mail already exists")

            showAlert(
                this,
                getString(R.string.mail_already_used),
                getString(R.string.email_verification_title)
            ) {
                finish()
            }
        } else if (e.errorCode == MegaError.API_EEXIST) {
            logWarning("Email change already requested (confirmation link already sent).")

            showAlert(
                this,
                getString(R.string.mail_changed_confirm_requested),
                getString(R.string.email_verification_title)
            ) {
                finish()
            }
        } else if (e.errorCode == MegaError.API_EFAILED || e.errorCode == MegaError.API_EEXPIRED) {
            if (is2FAEnabled) {
                verifyShowError()
            }
        } else {
            logError("Error when asking for change mail link: " + e.errorString + "___" + e.errorCode)
            showAlert(
                this,
                getString(R.string.general_text_error),
                getString(R.string.general_error_word)
            ) {
                finish()
            }
        }
    }

    private val listener = object : BaseListener(this) {

        override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
            hideKeyboard()
            progressBar.visibility = View.GONE

            when (request.type) {
                TYPE_MULTI_FACTOR_AUTH_CHECK -> doCheck2fa(api, request, e)
                TYPE_GET_CHANGE_EMAIL_LINK -> doChangeEmail(api, request, e)
            }
        }
    }

    private abstract class AfterTextChangedCallback : TextWatcher {

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

    }
}