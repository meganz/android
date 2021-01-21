package mega.privacy.android.app.activities.settingsActivities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
import androidx.core.content.ContextCompat
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ActivityPasscodeBinding
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.StringResourcesUtils
import java.util.*

class PasscodeActivity : BaseActivity() {

    companion object {
        const val ACTION_SET_PIN_LOCK = "ACTION_SET"
        const val ACTION_RESET_PIN_LOCK = "ACTION_RESET"
    }

    private lateinit var binding: ActivityPasscodeBinding
    private var passcodeType = PIN_4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        passcodeType = if (prefs != null) prefs.pinLockType else PIN_4

        if (passcodeType == PIN_ALPHANUMERIC) {
            binding.passFirstInput.visibility = GONE
            binding.passSecondInput.visibility = GONE
            binding.passThirdInput.visibility = GONE
            binding.passFourthInput.visibility = GONE
            binding.passFifthInput.visibility = GONE
            binding.passSixthInput.visibility = GONE

            binding.passwordInput.visibility = VISIBLE
            binding.passFirstInput.requestFocus()
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
                        if (editable.toString()
                                .isNotEmpty() && binding.passFifthInput.visibility == VISIBLE
                        ) {
                            binding.passFourthInput.requestFocus()
                        }
                    }
                })
            }

            if (passcodeType == PIN_4) {
                binding.passFirstInput.imeOptions = IME_ACTION_DONE

                binding.passFifthInput.visibility = GONE
                binding.passSixthInput.visibility = GONE
            } else {
                binding.passFirstInput.imeOptions = IME_ACTION_NEXT

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

                            }
                        }
                    })
                }
            }

            binding.passwordInput.visibility = GONE
        }
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