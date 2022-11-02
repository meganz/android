package mega.privacy.android.app.smsVerification

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.Html
import android.text.Spanned
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication.Companion.smsVerifyShowed
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.main.CountryCodePickerActivity
import mega.privacy.android.app.main.LoginFragment
import mega.privacy.android.app.sync.removeBackupsBeforeLogout
import mega.privacy.android.app.utils.ColorUtils.changeStatusBarColor
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.ColorUtils.getThemeColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaAchievementsDetails
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

/**
 * Activity for SMS Verification
 */
@AndroidEntryPoint
class SMSVerificationActivity : PasscodeActivity(), View.OnClickListener,
    MegaRequestListenerInterface {
    private lateinit var container: ScrollView
    private lateinit var helperText: TextView
    private lateinit var selectedCountry: TextView
    private lateinit var errorInvalidCountryCode: TextView
    private lateinit var errorInvalidPhoneNumber: TextView
    private lateinit var title: TextView
    private lateinit var titleCountryCode: TextView
    private lateinit var titlePhoneNumber: TextView
    private lateinit var textLogout: TextView
    private lateinit var divider1: View
    private lateinit var divider2: View
    private lateinit var errorInvalidPhoneNumberIcon: ImageView
    private lateinit var countrySelector: RelativeLayout
    private lateinit var phoneNumberInput: EditText
    private lateinit var nextButton: Button
    private lateinit var notNowButton: Button
    private var isSelectedCountryValid = false
    private var isPhoneNumberValid = false
    private var isUserLocked = false
    private var shouldDisableNextButton = false
    private var selectedCountryCode: String? = null
    private var selectedCountryName: String? = null
    private var selectedDialCode: String? = null
    private var countryCodeList: ArrayList<String>? = null
    private var pendingSelectingCountryCode = false
    private var inferredCountryCode: String? = null
    private var bonusStorageSMS = "GB"

    private val onBackPressedCallback by lazy {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Timber.d("onBackPressed")
                if (isUserLocked) {
                    return
                }
                finish()
            }
        }
    }

    /**
     * listener for [MegaChatRequestHandler]
     */
    @Inject
    lateinit var chatRequestHandler: MegaChatRequestHandler

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        smsVerifyShowed(true)
        setContentView(R.layout.activity_sms_verification)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        // For this page, designer requires to change status bar color to match the background color.
        changeStatusBarColor(this, R.color.blue_400_blue_200)
        container = findViewById(R.id.scroller_container)
        val intent = intent
        if (intent != null) {
            isUserLocked = intent.getBooleanExtra(NAME_USER_LOCKED, false)
        }
        Timber.d("Is user locked %s", isUserLocked)

        //divider
        divider1 = findViewById(R.id.verify_account_divider1)
        divider2 = findViewById(R.id.verify_account_divider2)

        //titles
        titleCountryCode = findViewById(R.id.verify_account_country_label)
        titlePhoneNumber = findViewById(R.id.verify_account_phone_number_label)
        selectedCountry = findViewById(R.id.verify_account_selected_country)
        title = findViewById(R.id.title)
        val tm = this.getSystemService(TELEPHONY_SERVICE) as TelephonyManager?
        if (tm != null) {
            inferredCountryCode = tm.networkCountryIso
            Timber.d("Inferred Country Code is: %s", inferredCountryCode)
        }
        megaApi.getCountryCallingCodes(this)
        //set helper text
        helperText = findViewById(R.id.verify_account_helper)
        if (isUserLocked) {
            val text = resources.getString(R.string.verify_account_helper_locked)
            helperText.text = text
            title.setText(R.string.verify_account_title)
            //logout button
            val textToShow = getString(R.string.sms_logout)
                .replace("[A]",
                    "<font color=\' " + getThemeColorHexString(this,
                        R.attr.colorSecondary) + "\'><u>")
                .replace("[/A]", "</u></font>")
            val result: Spanned = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                @Suppress("DEPRECATION")
                Html.fromHtml(textToShow)
            } else {
                Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
            }
            textLogout = findViewById(R.id.sms_logout)
            textLogout.text = result
            textLogout.visibility = View.VISIBLE
            textLogout.setOnClickListener(this)
            Handler(mainLooper).postDelayed({ container.fullScroll(View.FOCUS_DOWN) }, 100)
        } else {
            title.setText(R.string.add_phone_number_label)
            val isAchievementUser = megaApi.isAchievementsEnabled
            Timber.d("Is achievement user: %s", isAchievementUser)
            if (isAchievementUser) {
                megaApi.getAccountAchievements(this)
                val message =
                    String.format(getString(R.string.sms_add_phone_number_dialog_msg_achievement_user),
                        bonusStorageSMS)
                helperText.text = message
            } else {
                helperText.setText(R.string.sms_add_phone_number_dialog_msg_non_achievement_user)
            }
        }

        //country selector
        countrySelector = findViewById(R.id.verify_account_country_selector)
        countrySelector.setOnClickListener(this)

        //phone number input
        phoneNumberInput = findViewById(R.id.verify_account_phone_number_input)
        phoneNumberInput.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                nextButtonClicked()
                return@OnEditorActionListener true
            }
            false
        })
        phoneNumberInput.setImeActionLabel(getString(R.string.general_create),
            EditorInfo.IME_ACTION_DONE)
        phoneNumberInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                errorInvalidPhoneNumber.visibility = View.GONE
                errorInvalidPhoneNumberIcon.visibility = View.GONE
                divider2.setBackgroundColor(ContextCompat.getColor(this@SMSVerificationActivity,
                    R.color.grey_012_white_012))
            }

            override fun afterTextChanged(s: Editable) {
                val inputLength = s.toString().length
                if (inputLength > 0) {
                    titlePhoneNumber.setTextColor(getThemeColor(this@SMSVerificationActivity,
                        R.attr.colorSecondary))
                    titlePhoneNumber.visibility = View.VISIBLE
                } else {
                    phoneNumberInput.setHint(R.string.verify_account_phone_number_placeholder)
                    titlePhoneNumber.visibility = View.GONE
                }
            }
        })

        //buttons
        nextButton = findViewById(R.id.verify_account_next_button)
        nextButton.setOnClickListener(this)
        notNowButton = findViewById(R.id.verify_account_not_now_button)
        notNowButton.setOnClickListener(this)
        if (isUserLocked) {
            notNowButton.visibility = View.GONE
        } else {
            notNowButton.visibility = View.VISIBLE
        }

        //error message and icon
        errorInvalidCountryCode = findViewById(R.id.verify_account_invalid_country_code)
        errorInvalidPhoneNumber = findViewById(R.id.verify_account_invalid_phone_number)
        errorInvalidPhoneNumberIcon = findViewById(R.id.verify_account_invalid_phone_number_icon)

        //set saved state
        if (savedInstanceState != null) {
            selectedCountryCode =
                savedInstanceState.getString(CountryCodePickerActivity.COUNTRY_CODE)
            selectedCountryName =
                savedInstanceState.getString(CountryCodePickerActivity.COUNTRY_NAME)
            selectedDialCode = savedInstanceState.getString(CountryCodePickerActivity.DIAL_CODE)
            if (selectedCountryCode != null && selectedCountryName != null && selectedDialCode != null) {
                val label = "$selectedCountryName ($selectedDialCode)"
                selectedCountry.text = label
                errorInvalidCountryCode.visibility = View.GONE
                titleCountryCode.visibility = View.VISIBLE
                titleCountryCode.setTextColor(getThemeColor(this, R.attr.colorSecondary))
                divider1.setBackgroundColor(ContextCompat.getColor(this,
                    R.color.grey_012_white_012))
            }
        }
    }

    /**
     * onDestroy
     */
    override fun onDestroy() {
        super.onDestroy()
        smsVerifyShowed(false)
    }

    /**
     * onClick
     */
    override fun onClick(v: View) {
        when (v.id) {
            R.id.verify_account_country_selector -> {
                Timber.d("verify_account_country_selector clicked")
                if (countryCodeList != null) {
                    launchCountryPicker()
                } else {
                    Timber.d("Country code is not loaded")
                    megaApi.getCountryCallingCodes(this)
                    pendingSelectingCountryCode = true
                }
            }
            R.id.verify_account_next_button -> {
                Timber.d("verify_account_next_button clicked")
                nextButtonClicked()
            }
            R.id.verify_account_not_now_button -> {
                Timber.d("verify_account_not_now_button clicked")
                finish()
            }
            R.id.sms_logout -> {
                showConfirmLogoutDialog()
            }
            else -> {}
        }
    }

    private fun showConfirmLogoutDialog() {
        val builder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
        val dialogClickListener =
            DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        removeBackupsBeforeLogout()

                        /*
                       If the account is trying to login,
                       at this stage should set isLoggingRunning as `false` to indicate the login process is ended.
                     */chatRequestHandler.setIsLoggingRunning(false)
                        megaApi.logout()
                    }
                    DialogInterface.BUTTON_NEGATIVE -> {}
                }
                dialog.dismiss()
            }
        val message = getString(R.string.confirm_logout_from_sms_verification)
        builder.setCancelable(true)
            .setMessage(message)
            .setPositiveButton(getString(R.string.general_positive_button), dialogClickListener)
            .setNegativeButton(getString(R.string.general_negative_button), dialogClickListener)
            .show()
    }

    /**
     * onActivityResult
     */
    @Suppress("Deprecation")
    @Deprecated("")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == Constants.REQUEST_CODE_COUNTRY_PICKER && resultCode == RESULT_OK) {
            Timber.d("REQUEST_CODE_COUNTRY_PICKER OK")
            selectedCountryCode = intent?.getStringExtra(CountryCodePickerActivity.COUNTRY_CODE)
            selectedCountryName = intent?.getStringExtra(CountryCodePickerActivity.COUNTRY_NAME)
            selectedDialCode = intent?.getStringExtra(CountryCodePickerActivity.DIAL_CODE)
            val label = "$selectedCountryName ($selectedDialCode)"
            selectedCountry.text = label
            errorInvalidCountryCode.visibility = View.GONE
            titleCountryCode.visibility = View.VISIBLE
            titleCountryCode.setTextColor(getThemeColor(this, R.attr.colorSecondary))
            divider1.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_012_white_012))
        } else if (requestCode == Constants.REQUEST_CODE_VERIFY_CODE && resultCode == RESULT_OK) {
            Timber.d("REQUEST_CODE_VERIFY_CODE OK")
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun launchCountryPicker() {
        val intent = Intent(applicationContext, CountryCodePickerActivity::class.java)
        intent.putStringArrayListExtra("country_code", countryCodeList)
        @Suppress("Deprecation")
        startActivityForResult(intent, Constants.REQUEST_CODE_COUNTRY_PICKER)
    }

    private fun nextButtonClicked() {
        Util.hideKeyboard(this)
        hideError()
        validateFields()
        if (isPhoneNumberValid && isSelectedCountryValid) {
            hideError()
            requestTxt()
        } else {
            Timber.w("Phone number or country code is invalid.")
            showCountryCodeValidationError()
            showPhoneNumberValidationError(null)
        }
    }

    private fun validateFields() {
        val inputPhoneNumber = phoneNumberInput.text.toString()
        Timber.d("Generate normalized number for [%s] with country code: %s",
            inputPhoneNumber,
            selectedCountryCode)
        val phoneNumber = PhoneNumberUtils.formatNumberToE164(inputPhoneNumber, selectedCountryCode)
        // a valid normalized phone number must start with "+".
        if (phoneNumber != null && phoneNumber.startsWith("+")) {
            isPhoneNumberValid = true
        } else {
            phoneNumberInput.hint = ""
            isPhoneNumberValid = false
        }
        isSelectedCountryValid = selectedDialCode != null
        Timber.d("isSelectedCountryValid: %s , isPhoneNumberValid: %s",
            isSelectedCountryValid,
            isPhoneNumberValid)
    }

    private fun showCountryCodeValidationError() {
        if (!isSelectedCountryValid) {
            if (selectedDialCode == null) {
                selectedCountry.text = ""
            } else {
                selectedCountry.setText(R.string.sms_region_label)
            }
            Timber.w("Invalid country code")
            errorInvalidCountryCode.visibility = View.VISIBLE
            titleCountryCode.visibility = View.VISIBLE
            titleCountryCode.setTextColor(getThemeColor(this, R.attr.colorError))
            divider1.setBackgroundColor(getThemeColor(this, R.attr.colorError))
        }
    }

    private fun showPhoneNumberValidationError(errorMessage: String?) {
        if (!isPhoneNumberValid) {
            Timber.w("Invalid phone number: %s", errorMessage)
            errorInvalidPhoneNumber.visibility = View.VISIBLE
            errorInvalidPhoneNumberIcon.visibility = View.VISIBLE
            titlePhoneNumber.visibility = View.VISIBLE
            titlePhoneNumber.setTextColor(getThemeColor(this, R.attr.colorError))
            divider2.setBackgroundColor(getThemeColor(this, R.attr.colorError))
            if (errorMessage != null) {
                errorInvalidPhoneNumber.text = errorMessage
            }
        }
    }

    private fun hideError() {
        errorInvalidCountryCode.visibility = View.GONE
        errorInvalidPhoneNumber.visibility = View.GONE
        errorInvalidPhoneNumberIcon.visibility = View.GONE
        titleCountryCode.setTextColor(getThemeColor(this, R.attr.colorSecondary))
        titlePhoneNumber.setTextColor(getThemeColor(this, R.attr.colorSecondary))
        divider1.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_012_white_012))
        divider2.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_012_white_012))
    }

    private fun requestTxt() {
        Timber.d("shouldDisableNextButton is %s", shouldDisableNextButton)
        if (!shouldDisableNextButton) {
            nextButton.setBackgroundColor(ContextCompat.getColor(this,
                R.color.grey_600_white_087))
            val phoneNumber = PhoneNumberUtils.formatNumberToE164(
                phoneNumberInput.text.toString(), selectedCountryCode)
            Timber.d("Phone number is %s", phoneNumber)
            shouldDisableNextButton = true
            megaApi.sendSMSVerificationCode(phoneNumber, this)
        }
    }

    /**
     * onRequestStart
     */
    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {}

    /**
     * onRequestUpdate
     */
    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

    /**
     * onRequestFinish
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        shouldDisableNextButton = false
        nextButton.setBackgroundColor(getThemeColor(this, R.attr.colorSecondary))
        nextButton.setTextColor(getThemeColor(this, R.attr.colorOnSecondary))
        if (request.type == MegaRequest.TYPE_SEND_SMS_VERIFICATIONCODE) {
            Timber.d("send phone number,get code%s", e.errorCode)
            when (e.errorCode) {
                MegaError.API_OK -> {
                    Timber.d("The SMS verification request has been sent successfully.")
                    val enteredPhoneNumber = phoneNumberInput.text.toString()
                    val intent = Intent(this, SMSVerificationReceiveTxtActivity::class.java)
                    intent.putExtra(SELECTED_COUNTRY_CODE, selectedDialCode)
                    intent.putExtra(ENTERED_PHONE_NUMBER, enteredPhoneNumber)
                    intent.putExtra(NAME_USER_LOCKED, isUserLocked)
                    @Suppress("Deprecation")
                    startActivityForResult(intent, Constants.REQUEST_CODE_VERIFY_CODE)
                }
                MegaError.API_ETEMPUNAVAIL -> {
                    Timber.w("Reached daily limitation.")
                    errorInvalidPhoneNumber.visibility = View.VISIBLE
                    errorInvalidPhoneNumber.setText(R.string.verify_account_error_reach_limit)
                }
                MegaError.API_EACCESS -> {
                    Timber.w("The account is already verified with an SMS number.")
                    isPhoneNumberValid = false
                    val errorMessage =
                        resources.getString(R.string.verify_account_invalid_phone_number)
                    showPhoneNumberValidationError(errorMessage)
                }
                MegaError.API_EARGS -> {
                    Timber.w("Invalid phone number")
                    isPhoneNumberValid = false
                    val errorMessage =
                        resources.getString(R.string.verify_account_invalid_phone_number)
                    showPhoneNumberValidationError(errorMessage)
                }
                MegaError.API_EEXIST -> {
                    Timber.w("The phone number is already verified for some other account.")
                    isPhoneNumberValid = false
                    val errorMessage =
                        resources.getString(R.string.verify_account_error_phone_number_register)
                    showPhoneNumberValidationError(errorMessage)
                }
                else -> {
                    Timber.w("Request TYPE_SEND_SMS_VERIFICATION_CODE error: %s", e.errorString)
                    isPhoneNumberValid = false
                    val errorMessage =
                        resources.getString(R.string.verify_account_invalid_phone_number)
                    showPhoneNumberValidationError(errorMessage)
                }
            }
        }
        if (request.type == MegaRequest.TYPE_GET_ACHIEVEMENTS) {
            if (e.errorCode == MegaError.API_OK) {
                bonusStorageSMS =
                    Util.getSizeString(request.megaAchievementsDetails.getClassStorage(
                        MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE))
            }
            val message =
                String.format(getString(R.string.sms_add_phone_number_dialog_msg_achievement_user),
                    bonusStorageSMS)
            helperText.text = message
        }
        if (request.type == MegaRequest.TYPE_GET_COUNTRY_CALLING_CODES) {
            if (e.errorCode == MegaError.API_OK) {
                val codedCountryCode = ArrayList<String>()
                val listMap = request.megaStringListMap
                val keyList = listMap.keys
                for (i in 0 until keyList.size()) {
                    val key = keyList[i]
                    val contentBuffer = StringBuffer()
                    contentBuffer.append("$key:")
                    for (j in 0 until listMap[key].size()) {
                        val dialCode = listMap[key][j]
                        if (key.equals(inferredCountryCode, ignoreCase = true)) {
                            inferredCountryCode?.let {
                                val locale = Locale("", it)
                                selectedCountryName = locale.displayName
                                selectedCountryCode = key
                                selectedDialCode = "+$dialCode"
                                val label = "$selectedCountryName ($selectedDialCode)"
                                selectedCountry.text = label
                            }
                        }
                        contentBuffer.append("$dialCode,")
                    }
                    codedCountryCode.add(contentBuffer.toString())
                }
                countryCodeList = codedCountryCode
                if (pendingSelectingCountryCode) {
                    launchCountryPicker()
                    pendingSelectingCountryCode = false
                }
            } else {
                Timber.w("The country code is not responded correctly")
                Util.showSnackbar(this, getString(R.string.verify_account_not_loading_country_code))
            }
        }
    }

    /**
     * onRequestTemporaryError
     */
    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {}

    /**
     * onSaveInstanceState
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putCharSequence(CountryCodePickerActivity.COUNTRY_CODE, selectedCountryCode)
        outState.putCharSequence(CountryCodePickerActivity.COUNTRY_NAME, selectedCountryName)
        outState.putCharSequence(CountryCodePickerActivity.DIAL_CODE, selectedDialCode)
        super.onSaveInstanceState(outState)
    }

    companion object {
        /**
         * bundle key for COUNTRY_CODE
         */
        const val SELECTED_COUNTRY_CODE = "COUNTRY_CODE"

        /**
         * bundle key for ENTERED_PHONE_NUMBER
         */
        const val ENTERED_PHONE_NUMBER = "ENTERED_PHONE_NUMBER"
    }
}
