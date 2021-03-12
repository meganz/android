package mega.privacy.android.app.activities.settingsActivities

import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo.*
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.widget.doAfterTextChanged
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.databinding.ActivityPasscodeBinding
import mega.privacy.android.app.lollipop.controllers.AccountController
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.PasscodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.PasscodeUtil
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util.*
import java.util.*

class PasscodeLockActivity : BaseActivity() {

    companion object {
        const val ACTION_SET_PASSCODE_LOCK = "ACTION_SET"
        const val ACTION_RESET_PASSCODE_LOCK = "ACTION_RESET"
        const val MAX_ATTEMPTS = 10
        const val MIN_ATTEMPTS_TO_SHOW_WARNING = 5
        const val UNLOCK_MODE = 0
        const val SET_MODE = 1
        const val RESET_MODE = 2
    }

    private var attempts = 0
    private var mode = UNLOCK_MODE
    private var setOrUnlockMode = true

    private var screenOrientation = 0

    private lateinit var passcodeUtil: PasscodeUtil
    private lateinit var binding: ActivityPasscodeBinding
    private var passcodeType = PIN_4

    private var secondRound = false
    private val sbFirst = StringBuilder()
    private val sbSecond = StringBuilder()

    private var passcodeOptionsBottomSheetDialogFragment: PasscodeOptionsBottomSheetDialogFragment? =
        null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        passcodeUtil = PasscodeUtil(this, dbH)

        mode = when (intent.action) {
            ACTION_SET_PASSCODE_LOCK -> SET_MODE
            ACTION_RESET_PASSCODE_LOCK -> RESET_MODE
            else -> UNLOCK_MODE
        }

        setOrUnlockMode = mode == SET_MODE || mode == UNLOCK_MODE

        val prefs = dbH.preferences

        passcodeType =
            if (prefs != null && !isTextEmpty(prefs.passcodeLockType)) prefs.passcodeLockType else PIN_4

        screenOrientation = resources.configuration.orientation

        binding = ActivityPasscodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarPasscode)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title =
            StringResourcesUtils.getString(R.string.settings_passcode_lock).toUpperCase(Locale.ROOT)

        initPasscodeScreen()
        setListeners()
    }

    /**
     * Increments the number of failed attempts.
     */
    private fun incrementAttempts() {
        attempts++
        dbH.setAttrAttemps(attempts)
    }

    /**
     * Resets the number of failed attempts to 0.
     */
    private fun resetAttempts() {
        attempts = 0
        dbH.setAttrAttemps(attempts)
    }

    /**
     * Resets the number of failed attempts and then logs out.
     */
    private fun logout() {
        resetAttempts()
        AccountController.logout(this, megaApi)
    }

    /**
     * Sets the whole initial passcode screen.
     */
    private fun initPasscodeScreen() {
        updateViewOrientation()

        binding.titleText.text = StringResourcesUtils.getString(
            if (setOrUnlockMode) R.string.unlock_pin_title
            else R.string.reset_pin_title
        )

        if (mode == UNLOCK_MODE) {
            attempts = dbH.attributes.attemps
            binding.passcodeOptionsButton.visibility = GONE
        }

        binding.doNotMatchWarning.visibility = GONE

        if (attempts > 0) {
            showAttemptsError()
        } else {
            binding.failedAttemptsText.visibility = GONE
            binding.failedAttemptsErrorText.visibility = GONE
            binding.logoutButton.visibility = GONE
        }

        if (passcodeType == PIN_ALPHANUMERIC) {
            binding.passFirstInput.visibility = GONE
            binding.passSecondInput.visibility = GONE
            binding.passThirdInput.visibility = GONE
            binding.passFourthInput.visibility = GONE
            binding.passFifthInput.visibility = GONE
            binding.passSixthInput.visibility = GONE

            binding.passwordInput.apply {
                visibility = VISIBLE
                requestFocus()

                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == IME_ACTION_DONE) {
                        checkPasscode()
                        true
                    } else false

                }
            }
        } else {
            binding.passFirstInput.apply {
                visibility = VISIBLE
                requestFocus()

                doAfterTextChanged {
                    if (this.text.toString().isNotEmpty()) {
                        binding.passSecondInput.requestFocus()
                    }
                }
            }

            binding.passSecondInput.apply {
                visibility = VISIBLE

                doAfterTextChanged {
                    if (this.text.toString().isNotEmpty()) {
                        binding.passThirdInput.requestFocus()
                    }
                }

                setEt(binding.passFirstInput)
            }


            binding.passThirdInput.apply {
                visibility = VISIBLE

                doAfterTextChanged {
                    if (this.text.toString().isNotEmpty()) {
                        binding.passFourthInput.requestFocus()
                    }
                }

                setEt(binding.passSecondInput)
            }

            binding.passFourthInput.apply {
                visibility = VISIBLE

                doAfterTextChanged {
                    if (this.text.toString().isNotEmpty()) {
                        if (passcodeType == PIN_4) {
                            binding.passFirstInput.apply {
                                isCursorVisible = false
                                requestFocus()
                            }

                            checkPasscode()
                        } else {
                            binding.passFifthInput.requestFocus()
                        }
                    }
                }

                setEt(binding.passThirdInput)
            }

            val params = binding.passFourthInput.layoutParams as ConstraintLayout.LayoutParams

            if (passcodeType == PIN_4) {
                binding.passFourthInput.imeOptions = IME_ACTION_DONE or IME_FLAG_NO_FULLSCREEN

                params.marginEnd = 0

                binding.passFifthInput.visibility = GONE
                binding.passSixthInput.visibility = GONE
            } else {
                binding.passFourthInput.imeOptions = IME_ACTION_NEXT or IME_FLAG_NO_FULLSCREEN

                params.marginEnd = dp2px(16F, resources.displayMetrics)

                binding.passFifthInput.apply {
                    visibility = VISIBLE

                    doAfterTextChanged {
                        if (this.text.toString().isNotEmpty()) {
                            binding.passSixthInput.requestFocus()
                        }
                    }

                    setEt(binding.passFourthInput)
                }

                binding.passSixthInput.apply {
                    visibility = VISIBLE

                    doAfterTextChanged {
                        if (this.text.toString().isNotEmpty()) {
                            binding.passFirstInput.apply {
                                isCursorVisible = false
                                requestFocus()
                            }

                            checkPasscode()
                        }
                    }

                    setEt(binding.passFifthInput)
                }
            }

            binding.passFourthInput.layoutParams = params

            binding.passwordInput.visibility = GONE
        }
    }

    /**
     * Sets the necessary listeners to all available views in passcode screen.
     */
    private fun setListeners() {
        ListenScrollChangesHelper().addViewToListen(
            binding.passcodeScrollView
        ) { _, _, _, _, _ ->
            binding.toolbarPasscode.elevation =
                if (binding.passcodeScrollView.canScrollVertically(-1)) {
                    dp2px(4F, resources.displayMetrics).toFloat()
                } else 0F
        }

        binding.logoutButton.setOnClickListener {
            logout()
        }

        binding.passcodeOptionsButton.setOnClickListener {
            showPasscodeOptions()
        }
    }

    /**
     * Updates the layout params of some views depending on de orientation of the screen.
     */
    private fun updateViewOrientation() {
        val titleParams = binding.titleText.layoutParams as ConstraintLayout.LayoutParams
        titleParams.topMargin = dp2px(
            if (screenOrientation == ORIENTATION_PORTRAIT) 40F else 20F,
            resources.displayMetrics
        )


        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.passcodeParentView)
        constraintSet.clear(binding.passcodeOptionsButton.id, ConstraintSet.BOTTOM)
        constraintSet.clear(binding.passcodeOptionsButton.id, ConstraintSet.END)
        constraintSet.clear(binding.passcodeOptionsButton.id, ConstraintSet.START)
        constraintSet.clear(binding.passcodeOptionsButton.id, ConstraintSet.TOP)
        constraintSet.clear(binding.logoutButton.id, ConstraintSet.END)
        constraintSet.clear(binding.logoutButton.id, ConstraintSet.START)
        constraintSet.clear(binding.logoutButton.id, ConstraintSet.TOP)
        constraintSet.applyTo(binding.passcodeParentView)

        val passcodeOptionsParams =
            binding.passcodeOptionsButton.layoutParams as ConstraintLayout.LayoutParams

        passcodeOptionsParams.apply {
            if (screenOrientation == ORIENTATION_PORTRAIT) {
                bottomToBottom = binding.passcodeParentView.id
                endToEnd = binding.passcodeParentView.id
                startToStart = binding.passcodeParentView.id

                topMargin = 0
                bottomMargin = dp2px(40F, resources.displayMetrics)
            } else {
                endToEnd = binding.passcodeParentView.id
                topToBottom = binding.titleText.id

                topMargin = dp2px(20F, resources.displayMetrics)
                bottomMargin = 0
            }
        }

        binding.passcodeOptionsButton.layoutParams = passcodeOptionsParams

        val logoutParams = binding.logoutButton.layoutParams as ConstraintLayout.LayoutParams

        logoutParams.apply {
            if (screenOrientation == ORIENTATION_PORTRAIT) {
                endToEnd = binding.passcodeParentView.id
                startToStart = binding.passcodeParentView.id
                topToBottom = binding.failedAttemptsText.id

                topMargin = dp2px(92F, resources.displayMetrics)
                marginEnd = 0
            } else {
                endToEnd = binding.passcodeParentView.id
                topToBottom = binding.titleText.id

                topMargin = dp2px(20F, resources.displayMetrics)
                marginEnd = dp2px(20F, resources.displayMetrics)
            }
        }

        binding.logoutButton.layoutParams = logoutParams
    }

    /**
     * Saves the typed passcode after check if it is completed and confirms it:
     *  - After the first time the passcode was typed if it's unlocking.
     *  - After the second time the passcode was typed if it's setting or resetting.
     */
    private fun checkPasscode() {
        if (!isPassCodeComplete()) {
            return
        }

        val sb = if (secondRound) sbSecond else sbFirst

        when (passcodeType) {
            PIN_4 -> savePin4(sb)
            PIN_6 -> savePin6(sb)
            PIN_ALPHANUMERIC -> sb.append(binding.passwordInput.text)
        }

        when {
            secondRound -> confirmPasscode()
            mode == UNLOCK_MODE -> confirmUnlockPasscode()
            else -> {
                secondRound = true
                clearTypedPasscode(true)
                binding.passcodeOptionsButton.visibility = GONE
            }
        }
    }

    /**
     * Saves the typed pin if passcode type is PIN_4.
     *
     * @param sb StringBuilder in which the pin has to be saved.
     */
    private fun savePin4(sb: StringBuilder) {
        sb.apply {
            append(binding.passFirstInput.text)
            append(binding.passSecondInput.text)
            append(binding.passThirdInput.text)
            append(binding.passFourthInput.text)
        }
    }

    /**
     * Saves the typed pin if passcode type is PIN_6.
     *
     * @param sb StringBuilder in which the pin has to be saved.
     */
    private fun savePin6(sb: StringBuilder) {
        savePin4(sb)
        sb.apply {
            append(binding.passFifthInput.text)
            append(binding.passSixthInput.text)
        }
    }

    /**
     * Confirms the passcode after type it the second time if setting or resetting.
     * Updates the passcode in DB and finishes if successful.
     * Shows an error if not successful.
     */
    private fun confirmPasscode() {
        if (sbFirst.toString() == sbSecond.toString()) {
            passcodeUtil.enablePasscode(passcodeType, sbFirst.toString())
            setResult(RESULT_OK)
            finish()
        } else {
            clearTypedPasscode(true)
            sbSecond.clear()
            binding.doNotMatchWarning.visibility = VISIBLE
        }
    }

    /**
     * Confirms the passcode after type it the first time if unlocking.
     * Updates the passcode behaviour, failed attempts and finishes if successful.
     * Shows an error and increments failed attempts if not successful.
     */
    private fun confirmUnlockPasscode() {
        if (sbFirst.toString() == dbH.preferences.passcodeLockCode) {
            passcodeUtil.update()
            resetAttempts()
            finish()
        } else {
            sbFirst.clear()
            incrementAttempts()
            clearTypedPasscode(false)
            showAttemptsError()
        }
    }

    /**
     * Shows the right attempts error depending on the number of failed attempts.
     */
    private fun showAttemptsError() {
        binding.failedAttemptsText.apply {
            visibility = VISIBLE
            text = StringResourcesUtils.getQuantityString(
                R.plurals.passcode_lock_alert_attempts,
                attempts,
                attempts
            )
        }

        when {
            attempts == MAX_ATTEMPTS -> {
                binding.passcodeParentView.isEnabled = false
                hideKeyboardView(this, currentFocus, 0)
                logout()
            }
            attempts >= MIN_ATTEMPTS_TO_SHOW_WARNING -> {
                binding.failedAttemptsErrorText.visibility = VISIBLE
                binding.logoutButton.visibility = VISIBLE
            }
            attempts > 0 -> {
                binding.failedAttemptsErrorText.visibility = GONE
                binding.logoutButton.visibility = VISIBLE
            }
            else -> {
                binding.failedAttemptsErrorText.visibility = GONE
                binding.logoutButton.visibility = GONE
            }
        }
    }

    /**
     * Checks if all the fields are filled in before confirm the passcode.
     */
    private fun isPassCodeComplete(): Boolean {
        when (passcodeType) {
            PIN_4 -> {
                return binding.passFirstInput.length() == 1
                        && binding.passSecondInput.length() == 1
                        && binding.passThirdInput.length() == 1
                        && binding.passFourthInput.length() == 1
            }
            PIN_6 -> {
                return binding.passFirstInput.length() == 1
                        && binding.passSecondInput.length() == 1
                        && binding.passThirdInput.length() == 1
                        && binding.passFourthInput.length() == 1
                        && binding.passFifthInput.length() == 1
                        && binding.passSixthInput.length() == 1
            }
            PIN_ALPHANUMERIC -> {
                return binding.passwordInput.text.isNotEmpty()
            }
        }

        return false
    }

    /**
     * Clears the passcode fields.
     */
    private fun clearTypedPasscode(reEnter: Boolean) {
        binding.passFirstInput.text.clear()
        binding.passSecondInput.text.clear()
        binding.passThirdInput.text.clear()
        binding.passFourthInput.text.clear()
        binding.passFifthInput.text.clear()
        binding.passSixthInput.text.clear()

        binding.passwordInput.text.clear()

        binding.titleText.text = StringResourcesUtils.getString(
            if (reEnter && setOrUnlockMode) R.string.unlock_pin_title_2
            else if (reEnter) R.string.reset_pin_title_2
            else if (setOrUnlockMode) R.string.reset_pin_title
            else R.string.unlock_pin_title
        )

        if (passcodeType == PIN_ALPHANUMERIC) {
            binding.passwordInput.requestFocus()
        } else {
            binding.passFirstInput.requestFocus()
        }
    }

    /**
     * Opens the bottom sheet dialog to change the passcode type.
     */
    private fun showPasscodeOptions() {
        if (isBottomSheetDialogShown(passcodeOptionsBottomSheetDialogFragment)) return

        passcodeOptionsBottomSheetDialogFragment =
            PasscodeOptionsBottomSheetDialogFragment(passcodeType)
        passcodeOptionsBottomSheetDialogFragment?.show(
            supportFragmentManager,
            passcodeOptionsBottomSheetDialogFragment?.tag
        )
    }

    /**
     * Updates the passcode screen after change the type.
     *
     * @param passcodeType New passcode type.
     */
    fun setPasscodeType(passcodeType: String) {
        this.passcodeType = passcodeType
        initPasscodeScreen()
        clearTypedPasscode(false)


        Handler().postDelayed({
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                currentFocus,
                InputMethodManager.SHOW_IMPLICIT
            )
        }, 300)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == screenOrientation) return

        screenOrientation = newConfig.orientation
        updateViewOrientation()
    }

    override fun onBackPressed() {
        if (attempts < MAX_ATTEMPTS) {
            when (mode) {
                UNLOCK_MODE -> moveTaskToBack(true)
                RESET_MODE -> MegaApplication.getPasscodeManagement().showPasscodeScreen = false
                else -> finish()
            }
        }

        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            onBackPressed()

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        MegaApplication.getPasscodeManagement().showPasscodeScreen = isFinishing
        super.onDestroy()
    }

    override fun onUserLeaveHint() {
        if (mode != UNLOCK_MODE) {
            finish()
        }
    }
}