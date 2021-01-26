package mega.privacy.android.app.activities.settingsActivities

import android.content.res.Configuration
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
import mega.privacy.android.app.databinding.ActivityPasscodeBinding
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.PasscodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util.dp2px
import java.util.*

class PasscodeLockActivity : BaseActivity() {

    companion object {
        const val ACTION_SET_PIN_LOCK = "ACTION_SET"
        const val ACTION_RESET_PIN_LOCK = "ACTION_RESET"
        const val MAX_ATTEMPTS = 10
        const val UNLOCK_MODE = 0
        const val SET_MODE = 1
        const val RESET_MODE = 2
    }

    private val attempts = 0
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
            ACTION_SET_PIN_LOCK -> SET_MODE
            ACTION_RESET_PIN_LOCK -> RESET_MODE
            else -> UNLOCK_MODE
        }

        setOrUnlockMode = mode == SET_MODE || mode == UNLOCK_MODE

        screenOrientation = resources.configuration.orientation

        window.statusBarColor = ContextCompat.getColor(this, R.color.lollipop_dark_primary_color)

        binding = ActivityPasscodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarPasscode)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title =
            StringResourcesUtils.getString(R.string.settings_pin_lock).toUpperCase(Locale.ROOT)

        val prefs = dbH.preferences
        passcodeType =
            if (prefs != null && !isTextEmpty(prefs.pinLockType)) prefs.pinLockType else PIN_4

        initPasscodeScreen()

        binding.passcodeOptionsButton.setOnClickListener {
            showPasscodeOptions()
        }
    }

    private fun initPasscodeScreen() {
        updateViewOrientation()

        binding.titleText.text = StringResourcesUtils.getString(
            if (setOrUnlockMode) R.string.unlock_pin_title
            else R.string.reset_pin_title
        )

        binding.doNotMatchWarning.visibility = GONE

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

    private fun updateViewOrientation() {
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.passcodeParentView)
        constraintSet.clear(binding.passcodeOptionsButton.id, ConstraintSet.BOTTOM)
        constraintSet.clear(binding.passcodeOptionsButton.id, ConstraintSet.END)
        constraintSet.clear(binding.passcodeOptionsButton.id, ConstraintSet.START)
        constraintSet.clear(binding.passcodeOptionsButton.id, ConstraintSet.TOP)
        constraintSet.applyTo(binding.passcodeParentView)

        val params = binding.passcodeOptionsButton.layoutParams as ConstraintLayout.LayoutParams

        params.apply {
            if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) {
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

        binding.passcodeOptionsButton.layoutParams = params
    }

    private fun checkPasscode() {
        if (!isPassCodeComplete()) {
            return
        }

        if (secondRound) {
            when (passcodeType) {
                PIN_4 -> {
                    sbSecond.apply {
                        append(binding.passFirstInput.text)
                        append(binding.passSecondInput.text)
                        append(binding.passThirdInput.text)
                        append(binding.passFourthInput.text)
                    }
                }
                PIN_6 -> {
                    sbSecond.apply {
                        append(binding.passFirstInput.text)

                        append(binding.passSecondInput.text)
                        append(binding.passThirdInput.text)
                        append(binding.passFourthInput.text)
                        append(binding.passFifthInput.text)
                        append(binding.passSixthInput.text)
                    }
                }
                PIN_ALPHANUMERIC -> {
                    sbSecond.append(binding.passwordInput.text)
                }
            }

            confirmPasscode()
        } else {
            when (passcodeType) {
                PIN_4 -> {
                    sbFirst.apply {
                        append(binding.passFirstInput.text)
                        append(binding.passSecondInput.text)
                        append(binding.passThirdInput.text)
                        append(binding.passFourthInput.text)
                    }
                }
                PIN_6 -> {
                    sbFirst.apply {
                        append(binding.passFirstInput.text)
                        append(binding.passSecondInput.text)
                        append(binding.passThirdInput.text)
                        append(binding.passFourthInput.text)
                        append(binding.passFifthInput.text)
                        append(binding.passSixthInput.text)
                    }
                }
                PIN_ALPHANUMERIC -> {
                    sbFirst.append(binding.passwordInput.text)
                }
            }

            secondRound = true
            clearTypedPasscode(true)
            binding.passcodeOptionsButton.visibility = GONE
        }
    }

    private fun confirmPasscode() {
        if (sbFirst.toString() == sbSecond.toString()) {
            dbH.setPinLockCode(sbFirst.toString())
            dbH.setPinLockType(passcodeType)
            dbH.setPinLockEnabled(true)
            PinUtil.update()
            setResult(RESULT_OK)
            finish()
        } else {
            clearTypedPasscode(true)
            sbSecond.clear()
            binding.doNotMatchWarning.visibility = VISIBLE
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
}