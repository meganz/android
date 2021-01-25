package mega.privacy.android.app.activities.settingsActivities

import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ActivityPasscodeBinding
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util.dp2px
import java.lang.StringBuilder
import java.util.*

class PasscodeActivity : BaseActivity() {

    companion object {
        const val ACTION_SET_PIN_LOCK = "ACTION_SET"
        const val ACTION_RESET_PIN_LOCK = "ACTION_RESET"

        const val MAX_ATTEMPTS = 10
    }

    private var screenOrientation = 0

    private lateinit var binding: ActivityPasscodeBinding
    private var passcodeType = PIN_4

    private var secondRound = false
    private val sbFirst = StringBuilder()
    private val sbSecond = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        screenOrientation = resources.configuration.orientation

        window.statusBarColor = ContextCompat.getColor(this, R.color.lollipop_dark_primary_color)

        binding = ActivityPasscodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarPasscode)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title =
            StringResourcesUtils.getString(R.string.settings_pin_lock).toUpperCase(Locale.ROOT)

        initPasscodeScreen()
    }

    private fun initPasscodeScreen() {
        val prefs = dbH.preferences
        passcodeType =
            if (prefs != null && !isTextEmpty(prefs.pinLockType)) prefs.pinLockType else PIN_4

        updateViewOrientation()

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
                        confirmPasscode()
                        true
                    } else false

                }
            }
        } else {
            binding.passFirstInput.apply {
                visibility = VISIBLE
                requestFocus()
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        charSequence: CharSequence,
                        i: Int,
                        i1: Int,
                        i2: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(editable: Editable) {
                        if (editable.toString().isNotEmpty()) {
                            binding.passSecondInput.requestFocus()
                        }
                    }
                })
            }

            binding.passSecondInput.apply {
                visibility = VISIBLE
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        charSequence: CharSequence,
                        i: Int,
                        i1: Int,
                        i2: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(editable: Editable) {
                        if (editable.toString().isNotEmpty()) {
                            binding.passThirdInput.requestFocus()
                        }
                    }
                })
            }


            binding.passThirdInput.apply {
                visibility = VISIBLE
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        charSequence: CharSequence,
                        i: Int,
                        i1: Int,
                        i2: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(editable: Editable) {
                        if (editable.toString().isNotEmpty()) {
                            binding.passFourthInput.requestFocus()
                        }
                    }
                })
            }

            binding.passFourthInput.apply {
                visibility = VISIBLE
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        charSequence: CharSequence,
                        i: Int,
                        i1: Int,
                        i2: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(editable: Editable) {
                        if (editable.toString().isNotEmpty()) {
                            if (passcodeType == PIN_4) {
                                confirmPasscode()
                            } else {
                                binding.passFifthInput.requestFocus()
                            }
                        }
                    }
                })
            }

            val params = binding.passFourthInput.layoutParams as ConstraintLayout.LayoutParams

            if (passcodeType == PIN_4) {
                binding.passFirstInput.imeOptions = IME_ACTION_DONE

                params.marginEnd = 0

                binding.passFifthInput.visibility = GONE
                binding.passSixthInput.visibility = GONE
            } else {
                binding.passFirstInput.imeOptions = IME_ACTION_NEXT

                params.marginEnd = dp2px(16F, resources.displayMetrics)

                binding.passFifthInput.apply {
                    visibility = VISIBLE
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            charSequence: CharSequence,
                            i: Int,
                            i1: Int,
                            i2: Int
                        ) {
                        }

                        override fun onTextChanged(
                            s: CharSequence,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                        }

                        override fun afterTextChanged(editable: Editable) {
                            if (editable.toString().isNotEmpty()) {
                                binding.passSixthInput.requestFocus()
                            }
                        }
                    })
                }

                binding.passSixthInput.apply {
                    visibility = VISIBLE
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            charSequence: CharSequence,
                            i: Int,
                            i1: Int,
                            i2: Int
                        ) {
                        }

                        override fun onTextChanged(
                            s: CharSequence,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                        }

                        override fun afterTextChanged(editable: Editable) {
                            if (editable.toString().isNotEmpty()) {
                                confirmPasscode()
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

    private fun confirmPasscode() {
        if (!isPassCodeComplete()) {
            return
        }

        when (passcodeType) {
            PIN_4 -> {
                if (secondRound) {

                } else {

                }
            }
            PIN_6 -> {
                if (secondRound) {

                } else {

                }
            }
            PIN_ALPHANUMERIC -> {
                if (secondRound) {

                } else {

                }
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
}