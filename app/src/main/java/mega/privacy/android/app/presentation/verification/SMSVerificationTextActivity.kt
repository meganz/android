package mega.privacy.android.app.presentation.verification

import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.EditTextPIN
import mega.privacy.android.app.presentation.verification.model.SmsVerificationTextState
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

@AndroidEntryPoint
class SMSVerificationTextActivity : PasscodeActivity(),
    View.OnClickListener, View.OnLongClickListener, View.OnFocusChangeListener {
    private lateinit var toolbar: MaterialToolbar
    private var actionBar: ActionBar? = null
    private lateinit var backButton: TextView
    private lateinit var pinError: TextView
    private lateinit var resendTextView: TextView
    private lateinit var confirmButton: Button
    private lateinit var firstPin: EditTextPIN
    private lateinit var secondPin: EditTextPIN
    private lateinit var thirdPin: EditTextPIN
    private lateinit var fourthPin: EditTextPIN
    private lateinit var fifthPin: EditTextPIN
    private lateinit var sixthPin: EditTextPIN
    private lateinit var inputContainer: LinearLayout

    private var imm: InputMethodManager? = null
    private var isErrorShown = false
    private var firstTime = false
    private var pinLongClick = false
    private var allowResend = false
    private var isUserLocked = false
    private val resendTimeLimit = 30 * 1000

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase
    private var legacyListener: MegaRequestListenerInterface? = null

    private val viewModel by viewModels<SMSVerificationTextViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)

        //navigation bar
        setContentView(R.layout.activity_sms_verification_receive_txt)
        toolbar = findViewById(R.id.account_verification_toolbar)
        setSupportActionBar(toolbar)
        actionBar = supportActionBar
        actionBar?.setHomeButtonEnabled(true)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.title = getString(R.string.verify_account_enter_code_title)

        intent?.let {
            val phoneNumber =
                "(${it.getCountryCode()}) ${it.getPhoneNumber()}"
            val phoneNumberLbl = findViewById<TextView>(R.id.entered_phone_number)
            phoneNumberLbl.text = phoneNumber
            isUserLocked = it.getBooleanExtra(NAME_USER_LOCKED, false)
        }

        //resend
        resendTextView = findViewById(R.id.verify_account_resend)
        var text = resources.getString(R.string.verify_account_resend_label)
        val start = text.length
        text += " " + getString(R.string.general_resend_button)
        val end = text.length
        val spanString = SpannableString(text)
        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(textView: View) {
                backButtonClicked()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.color =
                    getThemeColor(this@SMSVerificationTextActivity, com.google.android.material.R.attr.colorSecondary)
                ds.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        }
        spanString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        resendTextView.text = spanString
        resendTextView.movementMethod = LinkMovementMethod.getInstance()
        resendTextView.highlightColor = Color.TRANSPARENT

        //buttons
        backButton = findViewById(R.id.verify_account_back_button)
        backButton.setOnClickListener(this)
        confirmButton = findViewById(R.id.verify_account_confirm_button)
        confirmButton.setOnClickListener(this)
        hideResendAndBackButton()
        Handler(Looper.getMainLooper()).postDelayed({
            allowResend = true
            showResendAndBackButton()
        }, resendTimeLimit.toLong())
        pinError = findViewById(R.id.verify_account_pin_error)

        //input fields
        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val display = windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        inputContainer = findViewById(R.id.verify_account_input_code_layout)
        firstPin = findViewById(R.id.verify_account_input_code_first)
        firstPin.setOnLongClickListener(this)
        firstPin.onFocusChangeListener = this
        firstPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (firstPin.length() != 0) {
                    secondPin.requestFocus()
                    secondPin.isCursorVisible = true
                    if (firstTime && !pinLongClick) {
                        secondPin.setText("")
                        thirdPin.setText("")
                        fourthPin.setText("")
                        fifthPin.setText("")
                        sixthPin.setText("")
                    } else if (pinLongClick) {
                        pasteClipboard()
                    }
                }
                if (isErrorShown) {
                    hideError()
                }
            }
        })
        secondPin = findViewById(R.id.verify_account_input_code_second)
        secondPin.setOnLongClickListener(this)
        secondPin.onFocusChangeListener = this
        secondPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (secondPin.length() != 0) {
                    thirdPin.requestFocus()
                    thirdPin.isCursorVisible = true
                    if (firstTime && !pinLongClick) {
                        thirdPin.setText("")
                        fourthPin.setText("")
                        fifthPin.setText("")
                        sixthPin.setText("")
                    } else if (pinLongClick) {
                        pasteClipboard()
                    }
                }
                if (isErrorShown) {
                    hideError()
                }
            }
        })
        thirdPin = findViewById(R.id.verify_account_input_code_third)
        thirdPin.setOnLongClickListener(this)
        thirdPin.onFocusChangeListener = this
        thirdPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (thirdPin.length() != 0) {
                    fourthPin.requestFocus()
                    fourthPin.isCursorVisible = true
                    if (firstTime && !pinLongClick) {
                        fourthPin.setText("")
                        fifthPin.setText("")
                        sixthPin.setText("")
                    } else if (pinLongClick) {
                        pasteClipboard()
                    }
                }
                if (isErrorShown) {
                    hideError()
                }
            }
        })
        fourthPin = findViewById(R.id.verify_account_input_code_fourth)
        fourthPin.setOnLongClickListener(this)
        fourthPin.onFocusChangeListener = this
        fourthPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (fourthPin.length() != 0) {
                    fifthPin.requestFocus()
                    fifthPin.isCursorVisible = true
                    if (firstTime && !pinLongClick) {
                        fifthPin.setText("")
                        sixthPin.setText("")
                    } else if (pinLongClick) {
                        pasteClipboard()
                    }
                }
                if (isErrorShown) {
                    hideError()
                }
            }
        })
        fifthPin = findViewById(R.id.verify_account_input_code_fifth)
        fifthPin.setOnLongClickListener(this)
        fifthPin.onFocusChangeListener = this
        fifthPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (fifthPin.length() != 0) {
                    sixthPin.requestFocus()
                    sixthPin.isCursorVisible = true
                    if (firstTime && !pinLongClick) {
                        sixthPin.setText("")
                    } else if (pinLongClick) {
                        pasteClipboard()
                    }
                }
                if (isErrorShown) {
                    hideError()
                }
            }
        })
        sixthPin = findViewById(R.id.verify_account_input_code_sixth)
        sixthPin.setOnLongClickListener(this)
        sixthPin.onFocusChangeListener = this
        sixthPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (sixthPin.length() != 0) {
                    sixthPin.isCursorVisible = true
                    Util.hideKeyboard(this@SMSVerificationTextActivity)
                    if (pinLongClick) {
                        pasteClipboard()
                    }
                }
                if (isErrorShown) {
                    hideError()
                }
            }
        })
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        firstPin.gravity = Gravity.CENTER_HORIZONTAL
        val paramsb1 = firstPin.layoutParams
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb1.width = Util.scaleWidthPx(42, outMetrics)
        } else {
            paramsb1.width = Util.scaleWidthPx(25, outMetrics)
        }
        firstPin.layoutParams = paramsb1
        var textParams = firstPin.layoutParams as LinearLayout.LayoutParams
        textParams.setMargins(0, 0, Util.scaleWidthPx(8, outMetrics), 0)
        firstPin.layoutParams = textParams
        secondPin.gravity = Gravity.CENTER_HORIZONTAL
        val paramsb2 = secondPin.layoutParams
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb2.width = Util.scaleWidthPx(42, outMetrics)
        } else {
            paramsb2.width = Util.scaleWidthPx(25, outMetrics)
        }
        secondPin.layoutParams = paramsb2
        textParams = secondPin.layoutParams as LinearLayout.LayoutParams
        textParams.setMargins(0, 0, Util.scaleWidthPx(8, outMetrics), 0)
        secondPin.layoutParams = textParams
        secondPin.previousDigitEditText = firstPin
        thirdPin.gravity = Gravity.CENTER_HORIZONTAL
        val paramsb3 = thirdPin.layoutParams
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb3.width = Util.scaleWidthPx(42, outMetrics)
        } else {
            paramsb3.width = Util.scaleWidthPx(25, outMetrics)
        }
        thirdPin.layoutParams = paramsb3
        textParams = thirdPin.layoutParams as LinearLayout.LayoutParams
        textParams.setMargins(0, 0, Util.scaleWidthPx(25, outMetrics), 0)
        thirdPin.layoutParams = textParams
        thirdPin.previousDigitEditText = secondPin
        fourthPin.gravity = Gravity.CENTER_HORIZONTAL
        val paramsb4 = fourthPin.layoutParams
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb4.width = Util.scaleWidthPx(42, outMetrics)
        } else {
            paramsb4.width = Util.scaleWidthPx(25, outMetrics)
        }
        fourthPin.layoutParams = paramsb4
        textParams = fourthPin.layoutParams as LinearLayout.LayoutParams
        textParams.setMargins(0, 0, Util.scaleWidthPx(8, outMetrics), 0)
        fourthPin.layoutParams = textParams
        fourthPin.previousDigitEditText = thirdPin
        fifthPin.gravity = Gravity.CENTER_HORIZONTAL
        val paramsb5 = fifthPin.layoutParams
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb5.width = Util.scaleWidthPx(42, outMetrics)
        } else {
            paramsb5.width = Util.scaleWidthPx(25, outMetrics)
        }
        fifthPin.layoutParams = paramsb5
        textParams = fifthPin.layoutParams as LinearLayout.LayoutParams
        textParams.setMargins(0, 0, Util.scaleWidthPx(8, outMetrics), 0)
        fifthPin.layoutParams = textParams
        fifthPin.previousDigitEditText = fourthPin
        sixthPin.gravity = Gravity.CENTER_HORIZONTAL
        val paramsb6 = sixthPin.layoutParams
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb6.width = Util.scaleWidthPx(42, outMetrics)
        } else {
            paramsb6.width = Util.scaleWidthPx(25, outMetrics)
        }
        sixthPin.layoutParams = paramsb6
        textParams = sixthPin.layoutParams as LinearLayout.LayoutParams
        textParams.setMargins(0, 0, 0, 0)
        sixthPin.layoutParams = textParams
        sixthPin.previousDigitEditText = fifthPin

        monitorPinResult()
    }

    private fun monitorPinResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect { state ->
                    when (state) {
                        SmsVerificationTextState.Empty -> {
                            enableConfirmButton()
                        }
                        is SmsVerificationTextState.Failed -> {
                            enableConfirmButton()
                            showError(state.error)
                        }
                        SmsVerificationTextState.VerifiedSuccessfully -> {
                            enableConfirmButton()
                            hideError()
                            onSuccessfullyVerified()
                        }
                        SmsVerificationTextState.Loading -> {
                            disableConfirmButton()
                        }
                    }
                }
            }
        }
    }

    private fun enableConfirmButton() {
        confirmButton.isClickable = true
    }

    private fun disableConfirmButton() {
        confirmButton.isClickable = false
    }

    private fun Intent.getPhoneNumber() = getStringExtra(
        SMSVerificationActivity.ENTERED_PHONE_NUMBER
    )

    private fun Intent.getCountryCode() =
        getStringExtra(SMSVerificationActivity.SELECTED_COUNTRY_CODE)

    override fun onResume() {
        super.onResume()
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                (imm ?: return).showSoftInput(firstPin, 0)
            }
        }, 900)
    }

    override fun onBackPressed() {
        Timber.d("onBackPressed")
        if (psaWebBrowser != null && psaWebBrowser?.consumeBack() == true) return
        super.onBackPressed()
        if (allowResend) {
            finish()
        }
    }

    override fun onClick(v: View) {
        Timber.d("on click ")
        when (v.id) {
            R.id.verify_account_back_button -> {
                Timber.d("verify_account_back_button clicked")
                backButtonClicked()
            }
            R.id.verify_account_confirm_button -> {
                Timber.d("verify_account_confirm_button clicked")
                confirmButtonClicked()
            }
            else -> {}
        }
    }

    override fun onLongClick(v: View): Boolean {
        Timber.d("onLongClick")
        when (v.id) {
            R.id.verify_account_input_code_first, R.id.verify_account_input_code_second, R.id.verify_account_input_code_third, R.id.verify_account_input_code_fourth, R.id.verify_account_input_code_fifth, R.id.verify_account_input_code_sixth -> {
                pinLongClick = true
                v.requestFocus()
            }
        }
        return false
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        Timber.d("onFocusChange")
        when (v.id) {
            R.id.verify_account_input_code_first -> {
                if (hasFocus) {
                    firstPin.setText("")
                }
            }
            R.id.verify_account_input_code_second -> {
                if (hasFocus) {
                    secondPin.setText("")
                }
            }
            R.id.verify_account_input_code_third -> {
                if (hasFocus) {
                    thirdPin.setText("")
                }
            }
            R.id.verify_account_input_code_fourth -> {
                if (hasFocus) {
                    fourthPin.setText("")
                }
            }
            R.id.verify_account_input_code_fifth -> {
                if (hasFocus) {
                    fifthPin.setText("")
                }
            }
            R.id.verify_account_input_code_sixth -> {
                if (hasFocus) {
                    sixthPin.setText("")
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            Timber.d("nav back pressed")
            backButtonClicked()
        }
        return super.onOptionsItemSelected(item)
    }

    fun pasteClipboard() {
        Timber.d("pasteClipboard")
        pinLongClick = false
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        if (clipData != null) {
            val code = clipData.getItemAt(0).text.toString()
            Timber.d("code: %s", code)
            if (code.length == 6) {
                var areDigits = true
                for (i in 0..5) {
                    if (!Character.isDigit(code[i])) {
                        areDigits = false
                        break
                    }
                }
                if (areDigits) {
                    firstPin.setText("${code[0]}")
                    secondPin.setText("${code[1]}")
                    thirdPin.setText("${code[2]}")
                    fourthPin.setText("${code[3]}")
                    fifthPin.setText("${code[4]}")
                    sixthPin.setText("${code[5]}")
                } else {
                    firstPin.setText("")
                    secondPin.setText("")
                    thirdPin.setText("")
                    fourthPin.setText("")
                    fifthPin.setText("")
                    sixthPin.setText("")
                }
            }
        }
    }

    private fun hideError() {
        Timber.d("hideError")
        isErrorShown = false
        pinError.visibility = View.GONE
        firstPin.setTextColor(ContextCompat.getColor(this, R.color.grey_087_white_087))
        secondPin.setTextColor(ContextCompat.getColor(this, R.color.grey_087_white_087))
        thirdPin.setTextColor(ContextCompat.getColor(this, R.color.grey_087_white_087))
        fourthPin.setTextColor(ContextCompat.getColor(this, R.color.grey_087_white_087))
        fifthPin.setTextColor(ContextCompat.getColor(this, R.color.grey_087_white_087))
        sixthPin.setTextColor(ContextCompat.getColor(this, R.color.grey_087_white_087))
    }

    private fun showError(errorMessage: String?) {
        Timber.d("showError")
        firstTime = false
        isErrorShown = true
        firstPin.setTextColor(ContextCompat.getColor(this, R.color.red_600_red_300))
        secondPin.setTextColor(ContextCompat.getColor(this, R.color.red_600_red_300))
        thirdPin.setTextColor(ContextCompat.getColor(this, R.color.red_600_red_300))
        fourthPin.setTextColor(ContextCompat.getColor(this, R.color.red_600_red_300))
        fifthPin.setTextColor(ContextCompat.getColor(this, R.color.red_600_red_300))
        sixthPin.setTextColor(ContextCompat.getColor(this, R.color.red_600_red_300))
        pinError.visibility = View.VISIBLE
        if (errorMessage != null) {
            Timber.w("Error message is: %s", errorMessage)
            pinError.text = errorMessage
        }
    }

    private fun validateVerificationCode() {
        Timber.d("validateVerificationCode")
        if (firstPin.length() == 1 && secondPin.length() == 1 && thirdPin.length() == 1 && fourthPin.length() == 1 && fifthPin.length() == 1 && sixthPin.length() == 1) {
            Util.hideKeyboard(this)
            val sb = StringBuilder()
            sb.append(firstPin.text)
            sb.append(secondPin.text)
            sb.append(thirdPin.text)
            sb.append(fourthPin.text)
            sb.append(fifthPin.text)
            sb.append(sixthPin.text)
            val pin = sb.toString().trim { it <= ' ' }
            Timber.d("PIN: %s", pin)
            viewModel.submitPin(pin)
            return
        }
        showError(getString(R.string.verify_account_incorrect_code))
    }

    private fun confirmButtonClicked() {
        Timber.d("confirmButtonClicked")
        validateVerificationCode()
    }

    private fun backButtonClicked() {
        Timber.d("backButtonClicked")
        finish()
    }

    private fun showResendAndBackButton() {
        Timber.d("showResendAndBackButton")
        backButton.visibility = View.VISIBLE
        resendTextView.visibility = View.VISIBLE
        actionBar?.setHomeButtonEnabled(true)
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun hideResendAndBackButton() {
        Timber.d("hideResendAndBackButton")
        backButton.visibility = View.GONE
        resendTextView.visibility = View.GONE
        actionBar?.setHomeButtonEnabled(false)
        actionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun onSuccessfullyVerified() {
        showSnackbar(inputContainer, getString(R.string.verify_account_successfully))
        //showing the successful text for 2 secs, then finish itself back to previous page.
        Handler(Looper.getMainLooper()).postDelayed({ //haven't logged in, if has credential will auto-login
            if (megaApi.isLoggedIn == 0 || megaApi.rootNode == null) {
                refreshSession()
            } else {
                setResult(RESULT_OK)
            }
            finish()
        }, 2000)
    }
}