package mega.privacy.android.app.presentation.login.confirmemail

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.FragmentConfirmEmailBinding
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.utils.Constants.EMAIL_ADDRESS
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import timber.log.Timber

/**
 * Confirm email fragment.
 *
 * @property megaApi       [MegaApiAndroid].
 * @property emailTemp     Temporary email.
 * @property firstNameTemp Temporary first name.
 */
@AndroidEntryPoint
class ConfirmEmailFragment : Fragment() {

    private val viewModel: ConfirmEmailViewModel by viewModels()
    private val loginViewModel: LoginViewModel by activityViewModels()

    private var _binding: FragmentConfirmEmailBinding? = null

    private val binding get() = _binding!!

    var emailTemp: String? = null
    var firstNameTemp: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("onCreateView")
        _binding = FragmentConfirmEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupView()
    }

    private fun setupObservers() {
        viewLifecycleOwner.collectFlow(viewModel.uiState) { uiState ->
            with(uiState) {
                if (isPendingToShowFragment != null) {
                    (requireActivity() as LoginActivity).showFragment(isPendingToShowFragment)
                    viewModel.isPendingToShowFragmentConsumed()
                }
            }
        }

        viewLifecycleOwner.collectFlow(
            viewModel.uiState.map { it.registeredEmail }.distinctUntilChanged()
        ) { registeredEmail ->
            registeredEmail?.let {
                (requireActivity() as LoginActivity).setTemporalEmail(it)
                loginViewModel.saveLastRegisteredEmail(it)
            }
        }
    }

    private fun setupView() = with(binding) {
        confirmEmailNewEmail.apply {
            doAfterTextChanged { quitEmailError() }
            isCursorVisible = true
            setText(emailTemp)
            requestFocus()
        }

        confirmEmailNewEmailErrorIcon.isVisible = false

        var textMisspelled =
            String.format(requireContext().getFormattedStringOrDefault(R.string.confirm_email_misspelled))
        try {
            textMisspelled = textMisspelled.replace("[A]", "<b>")
            textMisspelled = textMisspelled.replace("[/A]", "</b>")
        } catch (e: Exception) {
            Timber.w("Exception formatting string ${e.message}")
        }

        confirmEmailMisspelled.text = Html.fromHtml(textMisspelled, Html.FROM_HTML_MODE_LEGACY)
        confirmEmailNewEmailResend.setOnClickListener { submitForm() }
        confirmEmailCancel.setOnClickListener {
            viewModel.cancelCreateAccount()
            (requireActivity() as LoginActivity).cancelConfirmationAccount()
        }
    }

    /**
     * Launches the request if the typed email is correct.
     */
    private fun submitForm() {
        if (!validateForm()) {
            return
        }

        with(requireActivity() as LoginActivity) {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(binding.confirmEmailNewEmail.windowToken, 0)

            if (!Util.isOnline(requireContext())) {
                showSnackbar(requireContext().getFormattedStringOrDefault(R.string.error_server_connection_problem))
                return
            }

            binding.confirmEmailNewEmail.text.toString().lowercase().trim { it <= ' ' }.let {
                viewModel.resendSignUpLink(email = it, fullName = firstNameTemp)
            }
        }
    }

    /**
     * Checks if the typed email is correct.
     *
     * @return True if the email is correct, false otherwise.
     */
    private fun validateForm(): Boolean =
        if (!emailError.isNullOrEmpty()) {
            with(binding) {
                confirmEmailNewEmailLayout.apply {
                    error = emailError
                    setHintTextAppearance(R.style.TextAppearance_InputHint_Error)
                }
                confirmEmailNewEmailErrorIcon.isVisible = true
            }

            false
        } else true

    /**
     * Error to show if the typed email is not correct.
     */
    private val emailError: String?
        get() {
            binding.confirmEmailNewEmail.text.toString().let {
                return when {
                    it.isEmpty() -> requireContext().getFormattedStringOrDefault(R.string.error_enter_email)
                    !EMAIL_ADDRESS.matcher(it).matches() ->
                        requireContext().getFormattedStringOrDefault(R.string.error_invalid_email)

                    else -> null
                }
            }
        }

    /**
     * Hides the email error.
     */
    private fun quitEmailError() = with(binding) {
        confirmEmailNewEmailLayout.apply {
            error = null
            setHintTextAppearance(com.google.android.material.R.style.TextAppearance_Design_Hint)
        }
        confirmEmailNewEmailErrorIcon.isVisible = false
    }
}