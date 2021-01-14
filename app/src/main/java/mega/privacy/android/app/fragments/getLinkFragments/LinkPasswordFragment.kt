package mega.privacy.android.app.fragments.getLinkFragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentSetLinkPasswordBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.interfaces.GetLinkInterface
import mega.privacy.android.app.listeners.PasswordLinkListener
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util.setPasswordToggle
import nz.mega.sdk.MegaApiJava

class LinkPasswordFragment(private val getLinkInterface: GetLinkInterface) : BaseFragment() {

    private lateinit var binding: FragmentSetLinkPasswordBinding

    private var isPasswordValid = false

    private lateinit var veryWeakShape: Drawable
    private lateinit var weakShape: Drawable
    private lateinit var mediumShape: Drawable
    private lateinit var goodShape: Drawable
    private lateinit var strongShape: Drawable
    private lateinit var emptyShape: Drawable

    private var veryWeakColor = 0
    private var weakColor = 0
    private var mediumColor = 0
    private var goodColor = 0
    private var strongColor = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetLinkPasswordBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        veryWeakShape = ContextCompat.getDrawable(context, R.drawable.passwd_very_weak)!!
        weakShape = ContextCompat.getDrawable(context, R.drawable.passwd_weak)!!
        mediumShape = ContextCompat.getDrawable(context, R.drawable.passwd_medium)!!
        goodShape = ContextCompat.getDrawable(context, R.drawable.passwd_good)!!
        strongShape = ContextCompat.getDrawable(context, R.drawable.passwd_strong)!!
        emptyShape = ContextCompat.getDrawable(context, R.drawable.shape_password)!!

        veryWeakColor = ContextCompat.getColor(context, R.color.login_warning)
        weakColor = ContextCompat.getColor(context, R.color.pass_weak)
        mediumColor = ContextCompat.getColor(context, R.color.green_unlocked_rewards)
        goodColor = ContextCompat.getColor(context, R.color.pass_good)
        strongColor = ContextCompat.getColor(context, R.color.blue_unlocked_rewards)

        with(binding.passwordText) {
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.isNotEmpty()) {
                        val temp = s.toString()
                        binding.containerPasswdElements.visibility = VISIBLE
                        checkPasswordStrength(temp.trim())
                    } else {
                        isPasswordValid = false
                        binding.containerPasswdElements.visibility = GONE
                    }
                }

                override fun afterTextChanged(editable: Editable) {
                    if (editable.toString().isEmpty()) {
                        quitError(this@with)
                    }
                }
            })

            setOnFocusChangeListener { _, hasFocus ->
                setPasswordToggle(
                    binding.passwordLayout,
                    hasFocus
                )
            }
        }

        with(binding.confirmPasswordText) {
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(editable: Editable) {
                    quitError(this@with)
                }
            })

            setOnFocusChangeListener { _, hasFocus ->
                setPasswordToggle(
                    binding.confirmPasswordLayout,
                    hasFocus
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    confirmClick()
                    true
                }

                false
            }
        }

        binding.containerPasswdElements.visibility = GONE

        binding.buttonConfirmPassword.setOnClickListener { confirmClick() }
        binding.buttonConfirmPassword.text = getString(
            if (isTextEmpty(getLinkInterface.getLinkPassword())) R.string.button_set
            else R.string.action_reset
        )

        binding.buttonCancel.setOnClickListener { cancelClick() }

        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * Checks if the password is enough strong to allow set it and updates the UI in consequence.
     *
     * @param s Text containing the typed password.
     */
    private fun checkPasswordStrength(s: String) {
        binding.passwordLayout.isErrorEnabled = false

        when (megaApi.getPasswordStrength(s)) {
            MegaApiJava.PASSWORD_STRENGTH_VERYWEAK -> {
                binding.shapePasswordFirst.background = veryWeakShape
                binding.shapePasswordSecond.background = emptyShape
                binding.shapePasswordThird.background = emptyShape
                binding.shapePasswordFourth.background = emptyShape
                binding.shapePasswordFifth.background = emptyShape
                binding.passwordType.text = getString(R.string.pass_very_weak)
                binding.passwordType.setTextColor(veryWeakColor)
                binding.passwordAdviceText.text = getString(R.string.passwd_weak)
                isPasswordValid = false
                binding.passwordLayout.setHintTextAppearance(R.style.InputTextAppearanceVeryWeak)
                binding.passwordLayout.setErrorTextAppearance(R.style.InputTextAppearanceVeryWeak)
            }
            MegaApiJava.PASSWORD_STRENGTH_WEAK -> {
                binding.shapePasswordFirst.background = weakShape
                binding.shapePasswordSecond.background = weakShape
                binding.shapePasswordThird.background = emptyShape
                binding.shapePasswordFourth.background = emptyShape
                binding.shapePasswordFifth.background = emptyShape
                binding.passwordType.text = getString(R.string.pass_weak)
                binding.passwordType.setTextColor(weakColor)
                binding.passwordAdviceText.text = getString(R.string.passwd_weak)
                isPasswordValid = true
                binding.passwordLayout.setHintTextAppearance(R.style.InputTextAppearanceWeak)
                binding.passwordLayout.setErrorTextAppearance(R.style.InputTextAppearanceWeak)
            }
            MegaApiJava.PASSWORD_STRENGTH_MEDIUM -> {
                binding.shapePasswordFirst.background = mediumShape
                binding.shapePasswordSecond.background = mediumShape
                binding.shapePasswordThird.background = mediumShape
                binding.shapePasswordFourth.background = emptyShape
                binding.shapePasswordFifth.background = emptyShape
                binding.passwordType.text = getString(R.string.pass_medium)
                binding.passwordType.setTextColor(mediumColor)
                binding.passwordAdviceText.text = getString(R.string.passwd_medium)
                isPasswordValid = true
                binding.passwordLayout.setHintTextAppearance(R.style.InputTextAppearanceMedium)
                binding.passwordLayout.setErrorTextAppearance(R.style.InputTextAppearanceMedium)
            }
            MegaApiJava.PASSWORD_STRENGTH_GOOD -> {
                binding.shapePasswordFirst.background = goodShape
                binding.shapePasswordSecond.background = goodShape
                binding.shapePasswordThird.background = goodShape
                binding.shapePasswordFourth.background = goodShape
                binding.shapePasswordFifth.background = emptyShape
                binding.passwordType.text = getString(R.string.pass_good)
                binding.passwordType.setTextColor(goodColor)
                binding.passwordAdviceText.text = getString(R.string.passwd_good)
                isPasswordValid = true
                binding.passwordLayout.setHintTextAppearance(R.style.InputTextAppearanceGood)
                binding.passwordLayout.setErrorTextAppearance(R.style.InputTextAppearanceGood)
            }
            else -> {
                binding.shapePasswordFirst.background = strongShape
                binding.shapePasswordSecond.background = strongShape
                binding.shapePasswordThird.background = strongShape
                binding.shapePasswordFourth.background = strongShape
                binding.shapePasswordFifth.background = strongShape
                binding.passwordType.text = getString(R.string.pass_strong)
                binding.passwordType.setTextColor(strongColor)
                binding.passwordAdviceText.text = getString(R.string.passwd_strong)
                isPasswordValid = true
                binding.passwordLayout.setHintTextAppearance(R.style.InputTextAppearanceStrong)
                binding.passwordLayout.setErrorTextAppearance(R.style.InputTextAppearanceStrong)
            }
        }

        binding.passwordErrorIcon.visibility = GONE
        binding.passwordLayout.error = " "
        binding.passwordLayout.isErrorEnabled = true
    }

    /**
     * Removes the error from the view received as param.
     *
     * @param editText View from which the error has to be removed.
     */
    private fun quitError(editText: AppCompatEditText) {
        when (editText.id) {
            R.id.password_text -> {
                binding.passwordLayout.error = null
                binding.passwordLayout.setHintTextAppearance(R.style.TextAppearance_Design_Hint)
                binding.passwordErrorIcon.visibility = GONE
            }
            R.id.confirm_password_text -> {
                binding.confirmPasswordLayout.error = null
                binding.confirmPasswordLayout.setHintTextAppearance(R.style.TextAppearance_Design_Hint)
                binding.confirmPasswordErrorIcon.visibility = GONE
            }
        }
    }

    /**
     * Checks if all the fields are right to allow set or not the password.
     *
     * @return True if the fields are right, false otherwise.
     */
    private fun validateForm(): Boolean {
        val passwordError = getPasswordError()
        val passwordConfirmError = getPasswordConfirmError()

        setError(binding.passwordText, passwordError)
        setError(binding.confirmPasswordText, passwordConfirmError)

        if (!isTextEmpty(passwordError)) {
            binding.passwordText.requestFocus()
            return false
        } else if (!isTextEmpty(passwordConfirmError)) {
            binding.confirmPasswordText.requestFocus()
            return false
        }

        return true
    }

    /**
     * Sets an error in a view received by param.
     *
     * @param editText The view to set the error.
     * @param error    The error to set.
     *
     */
    private fun setError(editText: AppCompatEditText, error: String?) {
        if (isTextEmpty(error)) {
            return
        }

        when (editText.id) {
            R.id.password_text -> {
                binding.passwordLayout.isErrorEnabled = false
                binding.passwordLayout.error = error
                binding.passwordLayout.setHintTextAppearance(R.style.InputTextAppearanceError)
                binding.passwordLayout.setErrorTextAppearance(R.style.InputTextAppearanceError)
                binding.passwordErrorIcon.visibility = VISIBLE
                binding.passwordLayout.isErrorEnabled = true
            }
            R.id.confirm_password_text -> {
                binding.confirmPasswordLayout.isErrorEnabled = false
                binding.confirmPasswordLayout.error = error
                binding.confirmPasswordLayout.setHintTextAppearance(R.style.InputTextAppearanceError)
                binding.confirmPasswordLayout.setErrorTextAppearance(R.style.InputTextAppearanceError)
                binding.confirmPasswordText.requestFocus()
                binding.confirmPasswordErrorIcon.visibility = VISIBLE
                binding.confirmPasswordLayout.isErrorEnabled = true
            }
        }
    }

    /**
     * Gets the error of the first password field if it is wrong.
     *
     * @return The error ir the field is wrong, null otherwise.
     */
    private fun getPasswordError(): String? {
        val value: String = binding.passwordText.text.toString()

        if (isTextEmpty(value)) {
            return context.getString(R.string.error_enter_password)
        } else if (!isPasswordValid) {
            binding.containerPasswdElements.visibility = GONE
            return context.getString(R.string.error_password)
        }

        return null
    }

    /**
     * Gets the error of the confirm password field if it is wrong.
     *
     * @return The error if the field is wrong, null otherwise.
     */
    private fun getPasswordConfirmError(): String? {
        val password: String = binding.passwordText.text.toString()
        val confirm: String = binding.confirmPasswordText.text.toString()

        if (isTextEmpty(confirm)) {
            return context.getString(R.string.error_enter_password)
        } else if (password != confirm) {
            return context.getString(R.string.error_passwords_dont_match)
        }

        return null
    }

    /**
     * Manages the click on confirm button by validating the fields.
     */
    private fun confirmClick() {
        if (validateForm()) {
            val password = binding.passwordText.text.toString();
            getLinkInterface.setLinkPassword(password)

            megaApi.encryptLinkWithPassword(
                getLinkInterface.getNode().publicLink,
                password,
                PasswordLinkListener(context)
            )
        }
    }

    /**
     * Manages the click on cancel button by resetting the screen.
     */
    private fun cancelClick() {
        resetView()
        activity?.onBackPressed()
    }

    /**
     * Resets the view to the initial state.
     */
    fun resetView() {
        binding.passwordText.text = null
        binding.confirmPasswordText.text = null
    }
}