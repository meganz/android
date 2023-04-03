package mega.privacy.android.app.getLink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentSetLinkPasswordBinding
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * Fragment of [GetLinkActivity] to allow encrypt a link of a node with a password.
 */
@AndroidEntryPoint
class LinkPasswordFragment : Fragment(), Scrollable {

    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    companion object {
        private const val ALREADY_RESET = "ALREADY_RESET"
    }

    private val viewModel: GetLinkViewModel by activityViewModels()
    private val args: LinkPasswordFragmentArgs by navArgs()

    private lateinit var binding: FragmentSetLinkPasswordBinding

    private var isPasswordValid = false
    private var alreadyReset = false

    private val veryWeakShape by lazy {
        ContextCompat.getDrawable(
            requireContext(),
            R.drawable.passwd_very_weak
        )
    }
    private val weakShape by lazy {
        ContextCompat.getDrawable(
            requireContext(),
            R.drawable.passwd_weak
        )
    }
    private val mediumShape by lazy {
        ContextCompat.getDrawable(
            requireContext(),
            R.drawable.passwd_medium
        )
    }
    private val goodShape by lazy {
        ContextCompat.getDrawable(
            requireContext(),
            R.drawable.passwd_good
        )
    }
    private val strongShape by lazy {
        ContextCompat.getDrawable(
            requireContext(),
            R.drawable.passwd_strong
        )
    }
    private val emptyShape by lazy {
        ContextCompat.getDrawable(
            requireContext(),
            R.drawable.shape_password
        )
    }

    private val veryWeakColor by lazy {
        ContextCompat.getColor(
            requireContext(),
            R.color.red_600_red_300
        )
    }
    private val weakColor by lazy {
        ContextCompat.getColor(
            requireContext(),
            R.color.yellow_600_yellow_300
        )
    }
    private val mediumColor by lazy {
        ContextCompat.getColor(
            requireContext(),
            R.color.green_500_green_400
        )
    }
    private val goodColor by lazy {
        ContextCompat.getColor(
            requireContext(),
            R.color.lime_green_500_200
        )
    }
    private val strongColor by lazy {
        ContextCompat.getColor(
            requireContext(),
            R.color.dark_blue_500_200
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSetLinkPasswordBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        alreadyReset = savedInstanceState?.getBoolean(ALREADY_RESET, false) ?: false
        setupView()
        setupObservers()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(ALREADY_RESET, alreadyReset)
        super.onSaveInstanceState(outState)
    }

    private fun setupView() {
        binding.scrollViewSetLinkPassword.setOnScrollChangeListener { _, _, _, _, _ ->
            checkScroll()
        }

        checkScroll()

        binding.passwordLayout.isEndIconVisible = false
        binding.passwordText.apply {
            doOnTextChanged { text, _, _, _ ->
                if (!text.isNullOrEmpty()) {
                    val temp = text.toString()
                    binding.containerPasswdElements.visibility = VISIBLE
                    checkPasswordStrength(temp.trim())
                } else {
                    isPasswordValid = false
                    binding.containerPasswdElements.visibility = GONE
                }
            }

            doAfterTextChanged {
                if (text.toString().isEmpty()) {
                    quitError(this)
                }
            }

            setOnFocusChangeListener { _, hasFocus ->
                binding.passwordLayout.isEndIconVisible = hasFocus
            }
        }

        binding.confirmPasswordLayout.isEndIconVisible = false
        binding.confirmPasswordText.apply {
            doAfterTextChanged { quitError(this) }

            setOnFocusChangeListener { _, hasFocus ->
                binding.confirmPasswordLayout.isEndIconVisible = hasFocus
            }

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    confirmClick()
                    true
                } else false
            }
        }

        binding.containerPasswdElements.visibility = GONE

        binding.buttonConfirmPassword.setOnClickListener { confirmClick() }
        binding.buttonConfirmPassword.text = getString(
            if (!viewModel.isPasswordSet()) R.string.button_set
            else R.string.action_reset
        )

        binding.buttonCancel.setOnClickListener {
            resetView()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupObservers() {
        viewModel.getPassword().observe(viewLifecycleOwner, ::onPasswordSet)
    }

    /**
     * Updates the UI when the password has been set or removed.
     *
     * @param password The password if has been set, null if has been removed.
     */
    private fun onPasswordSet(password: String?) {
        resetView()

        if (!password.isNullOrEmpty() && (!args.isReset || alreadyReset)) {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
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
                binding.passwordLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_VeryWeak)
                binding.passwordLayout.setErrorTextAppearance(R.style.TextAppearance_InputHint_VeryWeak)
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
                binding.passwordLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_Weak)
                binding.passwordLayout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Weak)
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
                binding.passwordLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_Medium)
                binding.passwordLayout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Medium)
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
                binding.passwordLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_Good)
                binding.passwordLayout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Good)
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
                binding.passwordLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_Strong)
                binding.passwordLayout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Strong)
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
                binding.passwordLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_Error)
                binding.passwordLayout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Error)
                binding.passwordErrorIcon.visibility = VISIBLE
                binding.passwordLayout.isErrorEnabled = true
            }
            R.id.confirm_password_text -> {
                binding.confirmPasswordLayout.isErrorEnabled = false
                binding.confirmPasswordLayout.error = error
                binding.confirmPasswordLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_Error)
                binding.confirmPasswordLayout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Error)
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
            return getString(R.string.error_enter_password)
        } else if (!isPasswordValid) {
            binding.containerPasswdElements.visibility = GONE
            return getString(R.string.error_password)
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
            return getString(R.string.error_enter_password)
        } else if (password != confirm) {
            return getString(R.string.error_passwords_dont_match)
        }

        return null
    }

    /**
     * Manages the click on confirm button by validating the fields.
     */
    private fun confirmClick() {
        if (validateForm()) {
            alreadyReset = true
            viewModel.encryptLink(binding.passwordText.text.toString())
        }
    }

    /**
     * Resets the view to the initial state.
     */
    fun resetView() {
        binding.passwordText.text = null
        binding.confirmPasswordText.text = null
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized) {
            return
        }

        val withElevation = binding.scrollViewSetLinkPassword
            .canScrollVertically(Constants.SCROLLING_UP_DIRECTION)

        viewModel.setElevation(withElevation)
    }
}