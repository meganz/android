package mega.privacy.android.app.main

import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.components.EditTextPIN
import mega.privacy.android.app.constants.EventConstants.EVENT_2FA_UPDATED
import mega.privacy.android.app.databinding.ActivityVerifyTwoFactorBinding
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.main.controllers.AccountController
import mega.privacy.android.app.utils.Constants.ACTION_PASS_CHANGED
import mega.privacy.android.app.utils.Constants.CANCEL_ACCOUNT_2FA
import mega.privacy.android.app.utils.Constants.CHANGE_MAIL_2FA
import mega.privacy.android.app.utils.Constants.CHANGE_PASSWORD_2FA
import mega.privacy.android.app.utils.Constants.DISABLE_2FA
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.RESULT
import mega.privacy.android.app.utils.ConstantsUrl.RECOVERY_URL
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.hideKeyboard
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_EACCESS
import nz.mega.sdk.MegaError.API_EEXIST
import nz.mega.sdk.MegaError.API_EEXPIRED
import nz.mega.sdk.MegaError.API_EFAILED
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequest.TYPE_CHANGE_PW
import nz.mega.sdk.MegaRequest.TYPE_GET_CANCEL_LINK
import nz.mega.sdk.MegaRequest.TYPE_GET_CHANGE_EMAIL_LINK
import nz.mega.sdk.MegaRequest.TYPE_MULTI_FACTOR_AUTH_CHECK
import nz.mega.sdk.MegaRequest.TYPE_MULTI_FACTOR_AUTH_SET
import timber.log.Timber

/**
 * Activity for verifying two factor authentication code
 */
@AndroidEntryPoint
class VerifyTwoFactorActivity : PasscodeActivity() {

    private lateinit var binding: ActivityVerifyTwoFactorBinding

    private var onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {}
    }

    /**
     * @see KEY_VERIFY_TYPE
     *
     * Possible types:
     * @see CANCEL_ACCOUNT_2FA
     * @see CHANGE_MAIL_2FA
     * @see DISABLE_2FA
     * @see CHANGE_PASSWORD_2FA
     */
    private var verifyType: Int = 0

    /**
     * Only when the account has enabled 2fa, then can enter this page.
     * So the initial value should be true.
     */
    private var is2FAEnabled = true

    /** Input 2fa pin code for first time. */
    private var isFirstTime2fa = true

    /** Is error message shown. */
    private var isErrorShown = false

    /** Is long clicked on pin code input text. */
    private var pinLongClick = false

    /**
     * The new email the user wants to change to, collect from intent.
     * @see KEY_NEW_EMAIL
     */
    private var newEmail: String? = null

    /**
     * The new pas the user wants to change to, collect from intent.
     * @see KEY_NEW_PASSWORD
     */
    private var newPassword: String? = null

    companion object {
        /** Specifies intent data for verification type value */
        const val KEY_VERIFY_TYPE = "key_verify_type"

        /** Specifies intent data for the new email value */
        const val KEY_NEW_EMAIL = "key_new_email"

        /** Specifies intent data for the new password value */
        const val KEY_NEW_PASSWORD = "key_new_password"
    }

    /**
     * Perform initialization of the activity for verifying two factor authentication code
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyTwoFactorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Popup soft input.
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        verifyType = intent.getIntExtra(KEY_VERIFY_TYPE, 0)
        newEmail = intent.getStringExtra(KEY_NEW_EMAIL)
        newPassword = intent.getStringExtra(KEY_NEW_PASSWORD)

        initializeView()

        // Check current 2fa enable state again.
        megaApi.multiFactorAuthCheck(megaApi.myEmail, listener)
    }

    /**
     * Setup all UI elements.
     */
    private fun initializeView() {
        setSubTitle()

        binding.lostAuthenticationDevice.setOnClickListener {
            try {
                val openTermsIntent = Intent(this, WebViewActivity::class.java)
                openTermsIntent.apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    data = Uri.parse(RECOVERY_URL)
                }

                startActivity(openTermsIntent)
            } catch (e: Exception) {
                val viewIntent = Intent(Intent.ACTION_VIEW)
                viewIntent.data = Uri.parse(RECOVERY_URL)
                startActivity(viewIntent)
            }
        }

        setEditTextPINs()
    }

    /**
     * Set tool bar subtitle by verify type.
     */
    private fun setSubTitle() {
        val stringId = when (verifyType) {
            CANCEL_ACCOUNT_2FA -> R.string.verify_2fa_subtitle_delete_account
            CHANGE_MAIL_2FA -> R.string.verify_2fa_subtitle_change_email
            CHANGE_PASSWORD_2FA -> R.string.verify_2fa_subtitle_change_password
            DISABLE_2FA -> R.string.verify_2fa_subtitle_diable_2fa
            else -> 0
        }

        if (stringId != 0) {
            binding.toolbar.subtitle = StringResourcesUtils.getString(stringId)
        }
    }

    /**
     * Setup listener for each [EditTextPIN].
     */
    private fun setEditTextPINs() {
        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        processEditTextPINs { index, total, editTextPIN ->
            editTextPIN.apply {
                if (index == 0) {
                    requestFocus()
                } else {
                    setEt(getEditTextPINByIndex(index - 1))
                }

                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)

                setOnLongClickListener {
                    pinLongClick = true
                    requestFocus()
                    false
                }

                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        setText("")
                    }
                }

                doAfterTextChanged {
                    if (length() != 0) {
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
                    } else if (isErrorShown) {
                        verifyQuitError()
                    }
                }
            }
        }
    }

    /**
     * Hide soft input.
     */
    private fun hideKeyboard() = hideKeyboard(this, 0)

    /**
     * Show alert dialog.
     *
     * @param messageResId Res id for the text shown in the alert dialog as content.
     * @param titleResId Res id for the title of the alert dialog. May be [INVALID_VALUE], if the dialog has no title.
     * @param callback Callback executes when the dialog dismisses.
     *                 By default, the activity finishes when press the OK button in the dialog.
     */
    private fun showAlert(
        messageResId: Int,
        titleResId: Int,
        callback: () -> Unit = this::finish,
    ) {
        if (isActivityInBackground) return

        Util.showAlert(
            this,
            StringResourcesUtils.getString(messageResId),
            if (titleResId == INVALID_VALUE) null else StringResourcesUtils.getString(titleResId)
        ) { callback.invoke() }
    }

    /**
     * Setup each [EditTextPIN] with process.
     */
    private fun processEditTextPINs(process: (index: Int, total: Int, editTextPIN: EditTextPIN) -> Unit) {
        val childrenCount = binding.sixPinVerify.childCount

        // For each EditTextPIN in the parent layout.
        for (i in 0 until childrenCount) {
            process(i, childrenCount, getEditTextPINByIndex(i))
        }
    }

    /**
     * Paste the content in the clipboard to each [EditTextPIN].
     */
    private fun pasteClipboard() {
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

    /**
     * Get [EditTextPIN] by its index in parent layout.
     */
    private fun getEditTextPINByIndex(index: Int): EditTextPIN {
        val totalCount = binding.sixPinVerify.childCount

        // Return the first one.
        if (index < 0) {
            return getEditTextPINByIndex(0)
        }

        // Return the last one.
        if (index == totalCount) {
            return getEditTextPINByIndex(totalCount - 1)
        }

        return binding.sixPinVerify.getChildAt(index) as EditTextPIN
    }

    /**
     * Quit showing error.
     */
    private fun verifyQuitError() {
        isErrorShown = false

        binding.pin2faErrorVerify.isVisible = false

        processEditTextPINs { _, _, editTextPIN ->
            editTextPIN.setTextColor(ContextCompat.getColor(this, R.color.grey_087_white_087))
        }
    }

    /**
     * Showing 2fa verification error.
     */
    fun verifyShowError() {
        Timber.w("Pin not correct verifyShowError")
        isFirstTime2fa = false
        isErrorShown = true

        binding.pin2faErrorVerify.isVisible = true

        processEditTextPINs { _, _, editTextPIN ->
            editTextPIN.setTextColor(ContextCompat.getColor(this, R.color.red_600_red_300))
        }
    }

    /**
     * Permit PIN code for verification.
     * PIN code is from every single [EditTextPIN].
     */
    private fun permitVerify() {
        Timber.d("permitVerify")
        var allInput = true
        val sb = StringBuilder(6)

        processEditTextPINs { _, _, editTextPIN ->
            if (editTextPIN.length() != 1) {
                allInput = false
            }

            sb.append(editTextPIN.text)
        }

        if (allInput && !isErrorShown) {
            binding.progressbarVerify2fa.isVisible = true
            verify2FA(sb.toString())
        }
    }

    /**
     * Verify the PIN code string according to the verification type.
     *
     * @param pin The pin code string.
     */
    private fun verify2FA(pin: String) {
        when (verifyType) {
            CANCEL_ACCOUNT_2FA -> megaApi.multiFactorAuthCancelAccount(pin, listener)
            CHANGE_MAIL_2FA -> megaApi.multiFactorAuthChangeEmail(newEmail, pin, listener)
            DISABLE_2FA -> megaApi.multiFactorAuthDisable(pin, listener)
            CHANGE_PASSWORD_2FA -> megaApi.multiFactorAuthChangePassword(
                null,
                newPassword,
                pin,
                listener
            )
        }
    }

    /**
     * Callback when check 2fa state finished.
     */
    private fun check2faFinish(request: MegaRequest, e: MegaError) {
        when (e.errorCode) {
            API_OK -> {
                is2FAEnabled = request.flag
                Timber.d("2fa is enabled: $is2FAEnabled")
            }

            else -> Timber.w("Check 2fa enable state error. error code: ${e.errorCode}")
        }
    }

    /**
     * Callback when change email request finished.
     */
    private fun changeEmailFinish(e: MegaError) {
        when (e.errorCode) {
            API_OK -> {
                Timber.d("The change link has been sent")
                showAlert(
                    R.string.email_verification_text_change_mail,
                    R.string.email_verification_title
                )
            }

            API_EACCESS -> {
                Timber.w("The new mail already exists")
                showAlert(R.string.mail_already_used, R.string.email_verification_title)
            }

            API_EEXIST -> {
                Timber.w("Email change already requested (confirmation link already sent).")
                showAlert(
                    R.string.mail_changed_confirm_requested,
                    R.string.email_verification_title
                )
            }

            else -> {
                Timber.e("Error when asking for change mail link: ${e.errorString}___${e.errorCode}")
                showAlert(R.string.general_text_error, R.string.general_error_word)
            }
        }
    }

    /**
     * Callback when cancel account request finish.
     */
    private fun cancelAccountFinish(e: MegaError) {
        when (e.errorCode) {
            API_OK -> {
                Timber.d("Cancellation link received!")
                showAlert(R.string.email_verification_text, R.string.email_verification_title)
            }

            else -> {
                Timber.e("Error when asking for the cancellation link: ${e.errorString}___${e.errorCode}")
                showAlert(R.string.general_text_error, R.string.general_error_word)
            }
        }
    }

    /**
     * Callback when disable 2fa request finish.
     */
    private fun disable2faFinish(request: MegaRequest, e: MegaError) {
        when (e.errorCode) {
            API_OK -> {
                if (!request.flag) {
                    // Send live event to notify.
                    Timber.d("Pin correct: Two-Factor Authentication disabled")
                    LiveEventBus.get(EVENT_2FA_UPDATED, Boolean::class.java).post(false)
                    showAlert(R.string.label_2fa_disabled, INVALID_VALUE)
                } else {
                    Timber.w("Disable 2fa failed.")
                }
            }

            else -> {
                Timber.e("An error occurred trying to disable Two-Factor Authentication")
                showAlert(R.string.general_text_error, R.string.error_disable_2fa)
            }
        }
    }

    /**
     * Callback when change password request finish.
     */
    private fun changePasswordFinish(e: MegaError) {
        when (e.errorCode) {
            API_OK -> {
                Timber.d("Pass changed OK")
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

                if (intent != null && intent.getBooleanExtra(
                        ChangePasswordActivity.KEY_IS_LOGOUT,
                        false
                    )
                ) {
                    AccountController.logout(this, megaApi, lifecycleScope)
                } else {
                    //Intent to MyAccount
                    val resetPassIntent = Intent(this, ManagerActivity::class.java)
                    resetPassIntent.action = ACTION_PASS_CHANGED
                    resetPassIntent.putExtra(RESULT, e.errorCode)
                    startActivity(resetPassIntent)
                    finish()
                }
            }

            else -> showAlert(R.string.general_text_error, INVALID_VALUE)
        }
    }

    private val listener = OptionalMegaRequestListenerInterface(onRequestStart = { request ->
        Timber.d("Start ${request.type}: ${request.requestString} request.")

        // If there's an in-progress request (except checking 2fa status request, TYPE_MULTI_FACTOR_AUTH_CHECK),
        // quitting the activity is not allowed.
        if (request.type != TYPE_MULTI_FACTOR_AUTH_CHECK) {
            onBackPressedCallback.isEnabled = true
        }
    },
        onRequestFinish = { request, error ->
            Timber.d("${request.type}: ${request.requestString} finished.")

            // Reset to false in order to allow quitting the activity.
            onBackPressedCallback.isEnabled = false

            if (request.type != TYPE_MULTI_FACTOR_AUTH_CHECK) {
                hideKeyboard()
            }

            binding.progressbarVerify2fa.isVisible = false

            // PIN code verification error.
            if (error.errorCode == API_EFAILED or API_EEXPIRED) {
                Timber.w("Pin not correct")
                if (is2FAEnabled) {
                    verifyShowError()
                }

                return@OptionalMegaRequestListenerInterface
            }

            when (request.type) {
                TYPE_MULTI_FACTOR_AUTH_CHECK -> check2faFinish(request, error)
                TYPE_GET_CHANGE_EMAIL_LINK -> changeEmailFinish(error)
                TYPE_GET_CANCEL_LINK -> cancelAccountFinish(error)
                TYPE_MULTI_FACTOR_AUTH_SET -> disable2faFinish(request, error)
                TYPE_CHANGE_PW -> changePasswordFinish(error)
            }
        })
}