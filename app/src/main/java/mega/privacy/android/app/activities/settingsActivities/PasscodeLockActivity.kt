package mega.privacy.android.app.activities.settingsActivities

import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.text.Editable
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.PinUtil
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomTextWatcher
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.databinding.ActivityPasscodeBinding
import mega.privacy.android.app.lollipop.controllers.AccountController
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.PasscodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.utils.Constants.*
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

    private lateinit var binding: ActivityPasscodeBinding
    private var passcodeType = PIN_4

    private var secondRound = false
    private val sbFirst = StringBuilder()
    private val sbSecond = StringBuilder()

    private var passcodeOptionsBottomSheetDialogFragment: PasscodeOptionsBottomSheetDialogFragment? =
        null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        window.statusBarColor = ContextCompat.getColor(this, R.color.lollipop_dark_primary_color)

        binding = ActivityPasscodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarPasscode)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title =
            StringResourcesUtils.getString(R.string.settings_passcode_lock).toUpperCase(Locale.ROOT)

        initPasscodeScreen()
        setListeners()
    }

    private fun incrementAttempts() {
        attempts++
        dbH.setAttrAttemps(attempts)
    }

    private fun resetAttempts() {
        attempts = 0
        dbH.setAttrAttemps(attempts)
    }

    private fun logout() {
        resetAttempts()
        AccountController.logout(this, megaApi)
    }

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
                addTextChangedListener(object : CustomTextWatcher() {
                    override fun afterTextChanged(editable: Editable?) {
                        if (editable.toString().isNotEmpty()) {
                            binding.passSecondInput.requestFocus()
                        }
                    }
                })
            }

            binding.passSecondInput.apply {
                visibility = VISIBLE
                addTextChangedListener(object : CustomTextWatcher() {
                    override fun afterTextChanged(editable: Editable?) {
                        if (editable.toString().isNotEmpty()) {
                            binding.passThirdInput.requestFocus()
                        }
                    }
                })
            }


            binding.passThirdInput.apply {
                visibility = VISIBLE
                addTextChangedListener(object : CustomTextWatcher() {
                    override fun afterTextChanged(editable: Editable?) {
                        if (editable.toString().isNotEmpty()) {
                            binding.passFourthInput.requestFocus()
                        }
                    }
                })
            }

            binding.passFourthInput.apply {
                visibility = VISIBLE
                addTextChangedListener(object : CustomTextWatcher() {
                    override fun afterTextChanged(editable: Editable?) {
                        if (editable.toString().isNotEmpty()) {
                            if (passcodeType == PIN_4) {
                                checkPasscode()
                            } else {
                                binding.passFifthInput.requestFocus()
                            }
                        }
                    }
                })
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
                    addTextChangedListener(object : CustomTextWatcher() {
                        override fun afterTextChanged(editable: Editable?) {
                            if (editable.toString().isNotEmpty()) {
                                binding.passSixthInput.requestFocus()
                            }
                        }
                    })
                }

                binding.passSixthInput.apply {
                    visibility = VISIBLE
                    addTextChangedListener(object : CustomTextWatcher() {
                        override fun afterTextChanged(editable: Editable?) {
                            if (editable.toString().isNotEmpty()) {
                                checkPasscode()
                            }
                        }
                    })
                }
            }

            binding.passFourthInput.layoutParams = params

            binding.passwordInput.visibility = GONE
        }
    }

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
                topToBottom = binding.failedAttemptsErrorText.id

                topMargin = dp2px(30F, resources.displayMetrics)
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

    private fun savePin4(sb: StringBuilder) {
        sb.apply {
            append(binding.passFirstInput.text)
            append(binding.passSecondInput.text)
            append(binding.passThirdInput.text)
            append(binding.passFourthInput.text)
        }
    }

    private fun savePin6(sb: StringBuilder) {
        savePin4(sb)
        sb.apply {
            append(binding.passFifthInput.text)
            append(binding.passSixthInput.text)
        }
    }

    private fun confirmPasscode() {
        if (sbFirst.toString() == sbSecond.toString()) {
            dbH.setPasscodeLockCode(sbFirst.toString())
            dbH.setPasscodeLockType(passcodeType)
            dbH.setPasscodeLockEnabled(true)
            PinUtil.update()
            setResult(RESULT_OK)
            finish()
        } else {
            clearTypedPasscode(true)
            sbSecond.clear()
            binding.doNotMatchWarning.visibility = VISIBLE
        }
    }

    private fun confirmUnlockPasscode() {
        if (sbFirst.toString() == dbH.preferences.passcodeLockCode) {
            PinUtil.update()
            resetAttempts()
            finish()
        } else {
            sbFirst.clear()
            incrementAttempts()
            clearTypedPasscode(false)
            showAttemptsError()
        }
    }

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
            else -> {
                binding.failedAttemptsErrorText.visibility = GONE
                binding.logoutButton.visibility = GONE
            }
        }
    }

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

    private fun showPasscodeOptions() {
        if (isBottomSheetDialogShown(passcodeOptionsBottomSheetDialogFragment)) return

        passcodeOptionsBottomSheetDialogFragment =
            PasscodeOptionsBottomSheetDialogFragment(passcodeType)
        passcodeOptionsBottomSheetDialogFragment?.show(
            supportFragmentManager,
            passcodeOptionsBottomSheetDialogFragment?.tag
        )
    }

    fun setPasscodeType(passcodeType: String) {
        this.passcodeType = passcodeType
        initPasscodeScreen()
        clearTypedPasscode(false)
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
                RESET_MODE -> MegaApplication.setShowPinScreen(false)
                else -> finish()
            }
        } else {
            moveTaskToBack(false)
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
        MegaApplication.setShowPinScreen(isFinishing)
        super.onDestroy()
    }

    override fun onUserLeaveHint() {
        if (mode != UNLOCK_MODE) {
            finish()
        }
    }
}