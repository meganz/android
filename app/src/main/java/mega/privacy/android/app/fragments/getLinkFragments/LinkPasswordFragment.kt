package mega.privacy.android.app.fragments.getLinkFragments

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

    private var isPasswordValid: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetLinkPasswordBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(binding.passwordText) {
            background.clearColorFilter()

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
                    quitError(this@with)
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
            background.clearColorFilter()

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
            if (isTextEmpty(getLinkInterface.getPasswordLink())) R.string.button_set
            else R.string.action_reset
        )

        binding.buttonCancel.setOnClickListener { cancelClick() }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun checkPasswordStrength(s: String) {
        val passwordStrength: Int = megaApi.getPasswordStrength(s)
        val veryWeak = ContextCompat.getDrawable(context, R.drawable.passwd_very_weak)
        val weak = ContextCompat.getDrawable(context, R.drawable.passwd_weak)
        val medium = ContextCompat.getDrawable(context, R.drawable.passwd_medium)
        val good = ContextCompat.getDrawable(context, R.drawable.passwd_good)
        val strong = ContextCompat.getDrawable(context, R.drawable.passwd_strong)
        val shape = ContextCompat.getDrawable(context, R.drawable.shape_password)

        when (passwordStrength) {
            MegaApiJava.PASSWORD_STRENGTH_VERYWEAK -> {
                binding.shapePasswordFirst.background = veryWeak
                binding.shapePasswordSecond.background = shape
                binding.shapePasswordThird.background = shape
                binding.shapePasswordFourth.background = shape
                binding.shapePasswordFifth.background = shape
                binding.passwordType.text = getString(R.string.pass_very_weak)
                binding.passwordType.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.login_warning
                    )
                )
                binding.passwordAdviceText.text = getString(R.string.passwd_weak)
                isPasswordValid = false
            }
            MegaApiJava.PASSWORD_STRENGTH_WEAK -> {
                binding.shapePasswordFirst.background = weak
                binding.shapePasswordSecond.background = weak
                binding.shapePasswordThird.background = shape
                binding.shapePasswordFourth.background = shape
                binding.shapePasswordFifth.background = shape
                binding.passwordType.text = getString(R.string.pass_weak)
                binding.passwordType.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.pass_weak
                    )
                )
                binding.passwordAdviceText.text = getString(R.string.passwd_weak)
                isPasswordValid = true
            }
            MegaApiJava.PASSWORD_STRENGTH_MEDIUM -> {
                binding.shapePasswordFirst.background = medium
                binding.shapePasswordSecond.background = medium
                binding.shapePasswordThird.background = medium
                binding.shapePasswordFourth.background = shape
                binding.shapePasswordFifth.background = shape
                binding.passwordType.text = getString(R.string.pass_medium)
                binding.passwordType.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.green_unlocked_rewards
                    )
                )
                binding.passwordAdviceText.text = getString(R.string.passwd_medium)
                isPasswordValid = true
            }
            MegaApiJava.PASSWORD_STRENGTH_GOOD -> {
                binding.shapePasswordFirst.background = good
                binding.shapePasswordSecond.background = good
                binding.shapePasswordThird.background = good
                binding.shapePasswordFourth.background = good
                binding.shapePasswordFifth.background = shape
                binding.passwordType.text = getString(R.string.pass_good)
                binding.passwordType.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.pass_good
                    )
                )
                binding.passwordAdviceText.text = getString(R.string.passwd_good)
                isPasswordValid = true
            }
            else -> {
                binding.shapePasswordFirst.background = strong
                binding.shapePasswordSecond.background = strong
                binding.shapePasswordThird.background = strong
                binding.shapePasswordFourth.background = strong
                binding.shapePasswordFifth.background = strong
                binding.passwordType.text = getString(R.string.pass_strong)
                binding.passwordType.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.blue_unlocked_rewards
                    )
                )
                binding.passwordAdviceText.text = getString(R.string.passwd_strong)
                isPasswordValid = true
            }
        }
    }

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

    private fun setError(editText: AppCompatEditText, error: String?) {
        if (isTextEmpty(error)) {
            return
        }

        when (editText.id) {
            R.id.password_text -> {
                binding.passwordLayout.error = error
                binding.passwordLayout.setHintTextAppearance(R.style.InputTextAppearanceError)
                binding.passwordErrorIcon.visibility = VISIBLE
            }
            R.id.confirm_password_text -> {
                binding.confirmPasswordLayout.error = error
                binding.confirmPasswordLayout.setHintTextAppearance(R.style.InputTextAppearanceError)
                binding.confirmPasswordErrorIcon.visibility = VISIBLE
            }
        }
    }

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

    private fun confirmClick() {
        if (validateForm()) {
            val password = binding.passwordText.text.toString();
            getLinkInterface.setPasswordLink(password)

            megaApi.encryptLinkWithPassword(
                getLinkInterface.getNode().publicLink,
                password,
                PasswordLinkListener(context)
            )
        }
    }

    private fun cancelClick() {
        resetView()
        activity?.onBackPressed()
    }

    fun resetView() {
        binding.passwordText.text = null
        binding.confirmPasswordText.text = null
    }
}