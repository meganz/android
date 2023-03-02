package mega.privacy.android.app.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.EphemeralCredentials
import mega.privacy.android.app.LegacyDatabaseHandler
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.databinding.FragmentCreateAccountBinding
import mega.privacy.android.app.interfaces.OnKeyboardVisibilityListener
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.utils.ColorUtils.resetEditTextUnderlineColor
import mega.privacy.android.app.utils.ColorUtils.setEditTextUnderlineColor
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.ViewUtils.hideKeyboard
import mega.privacy.android.app.utils.ViewUtils.removeLeadingAndTrailingSpaces
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber
import javax.inject.Inject

/**
 * Create account fragment.
 *
 * @property dbH     [LegacyDatabaseHandler]
 * @property megaApi [MegaApiAndroid]
 */
@AndroidEntryPoint
class CreateAccountFragment : Fragment(), MegaRequestListenerInterface,
    OnKeyboardVisibilityListener {

    @Inject
    lateinit var dbH: LegacyDatabaseHandler

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    private var _binding: FragmentCreateAccountBinding? = null
    private val binding get() = _binding!!

    private var passwdValid = false

    private val veryWeakColor by lazy {
        ContextCompat.getColor(requireContext(), R.color.red_600_red_300)
    }
    private val weakColor by lazy {
        ContextCompat.getColor(requireContext(), R.color.yellow_600_yellow_300)
    }
    private val mediumColor by lazy {
        ContextCompat.getColor(requireContext(), R.color.green_500_green_400)
    }
    private val goodColor by lazy {
        ContextCompat.getColor(requireContext(), R.color.lime_green_500_200)
    }
    private val strongColor by lazy {
        ContextCompat.getColor(requireContext(), R.color.dark_blue_500_200)
    }
    private val veryWeakDrawable by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.passwd_very_weak)
    }
    private val weakDrawable by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.passwd_weak)
    }
    private val mediumDrawable by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.passwd_medium)
    }
    private val goodDrawable by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.passwd_good)
    }
    private val strongDrawable by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.passwd_strong)
    }
    private val shapeDrawable by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.shape_password)
    }
    private val emailError: String?
        get() {
            val text = binding.createAccountEmailText.text
            return when {
                text.isNullOrEmpty() -> {
                    requireContext().getFormattedStringOrDefault(R.string.error_enter_email)
                }
                Constants.EMAIL_ADDRESS.matcher(text.toString()).matches().not() -> {
                    requireContext().getFormattedStringOrDefault(R.string.error_invalid_email)
                }
                else -> {
                    null
                }
            }
        }
    private val usernameError: String?
        get() = if (binding.createAccountNameText.text.isNullOrEmpty()) {
            requireContext().getFormattedStringOrDefault(R.string.error_enter_username)
        } else {
            null
        }
    private val userLastnameError: String?
        get() = if (binding.createAccountLastNameText.text.isNullOrEmpty()) {
            requireContext().getFormattedStringOrDefault(R.string.error_enter_userlastname)
        } else {
            null
        }
    private val passwordError: String?
        get() = when {
            binding.createAccountPasswordText.text.isNullOrEmpty() -> {
                requireContext().getFormattedStringOrDefault(R.string.error_enter_password)
            }
            !passwdValid -> {
                binding.containerPasswdElements.isVisible = false
                requireContext().getFormattedStringOrDefault(R.string.error_password)
            }
            else -> {
                null
            }
        }
    private val passwordConfirmError: String?
        get() {
            val password = binding.createAccountPasswordText.text.toString()
            val confirm = binding.createAccountPasswordTextConfirm.text.toString()
            return when {
                confirm.isEmpty() -> {
                    requireContext().getFormattedStringOrDefault(R.string.error_enter_password)
                }
                password != confirm -> {
                    requireContext().getFormattedStringOrDefault(R.string.error_passwords_dont_match)
                }
                else -> {
                    null
                }
            }
        }

    private val loginActivity: LoginActivity?
        get() = (requireActivity() as? LoginActivity)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("onCreateView")
        _binding = FragmentCreateAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d("onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        setupView()
        loginActivity?.setKeyboardVisibilityListener(this)
    }

    private fun setupView() = with(binding) {
        createAccountNameTextErrorIcon.isVisible = false
        createAccountLastNameTextErrorIcon.isVisible = false
        requireActivity().intent?.getStringExtra(Constants.EMAIL)?.let {
            createAccountEmailText.setText(it)
        }
        createAccountEmailTextErrorIcon.isVisible = false
        createAccountPasswordTextLayout.isEndIconVisible = false
        createAccountPasswordTextErrorIcon.isVisible = false
        createAccountPasswordTextConfirmLayout.isEndIconVisible = false
        createAccountPasswordTextConfirmErrorIcon.isVisible = false
        createAccountNameText.apply {
            requestFocus()
            doAfterTextChanged {
                quitError(createAccountNameTextLayout, createAccountNameTextErrorIcon)
            }
        }
        createAccountLastNameText.doAfterTextChanged {
            quitError(createAccountLastNameTextLayout, createAccountLastNameTextErrorIcon)
        }
        createAccountEmailText.apply {
            doAfterTextChanged {
                quitError(createAccountEmailTextLayout, createAccountEmailTextErrorIcon)
            }
            onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (!hasFocus) {
                    removeLeadingAndTrailingSpaces()
                }
            }
        }
        createAccountPasswordText.apply {
            doOnTextChanged { text, start, before, count ->
                Timber.d("Text changed: ${text.toString()}__${start}__${before}__$count")

                if (!text.isNullOrEmpty()) {
                    containerPasswdElements.isVisible = true
                    checkPasswordStrength(text.toString().trim { it <= ' ' })
                } else {
                    passwdValid = false
                    containerPasswdElements.isVisible = false
                    createAccountPasswordTextLayout.setHintTextAppearance(R.style.TextAppearance_Design_Hint)
                    resetEditTextUnderlineColor(createAccountPasswordText)
                }
            }
            doAfterTextChanged {
                quitError(createAccountPasswordTextLayout, createAccountPasswordTextErrorIcon)
            }
            onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                createAccountPasswordTextLayout.isEndIconVisible = hasFocus
            }
        }
        createAccountPasswordTextConfirm.apply {
            doAfterTextChanged {
                quitError(
                    createAccountPasswordTextConfirmLayout,
                    createAccountPasswordTextConfirmErrorIcon
                )
            }
            onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                createAccountPasswordTextConfirmLayout.isEndIconVisible = hasFocus
            }
        }

        tos.apply {
            text = applyUnderlinedFormat(requireContext().getFormattedStringOrDefault(R.string.tos))
            setOnClickListener {
                Timber.d("Show ToS")
                openLink(TOS)
            }
        }
        var textToShowTOP = requireContext().getFormattedStringOrDefault(R.string.top)
        try {
            textToShowTOP = textToShowTOP.replace("[B]", "<font color=\'#00BFA5\'>")
                .replace("[/B]", "</font>")
        } catch (e: Exception) {
            Timber.e(e, "Exception formatting string")
        }
        checkboxTop.top.apply {
            text = applyUnderlinedFormat(textToShowTOP)
            setOnClickListener {
                Timber.d("Show terms of password")
                openLink(Constants.URL_E2EE)
            }
        }

        buttonCreateAccountCreate.apply {
            text = requireContext().getFormattedStringOrDefault(R.string.create_account)
            setOnClickListener {
                binding.createAccountEmailText.removeLeadingAndTrailingSpaces()
                submitForm()
            }
        }

        buttonLoginCreate.apply {
            text = requireContext().getFormattedStringOrDefault(R.string.login_text)
            setOnClickListener {
                loginActivity?.showFragment(Constants.LOGIN_FRAGMENT)
            }
        }
        createAccountCreateLayout.isVisible = true
        createAccountCreatingLayout.isVisible = false
        createAccountCreatingText.isVisible = false
        createAccountProgressBar.isVisible = false
        containerPasswdElements.isVisible = false
    }

    private fun applyUnderlinedFormat(text: String): Spanned {
        var formattedText = text

        try {
            formattedText = text.replace("[A]", "<u>")
                .replace("[/A]", "</u>")
        } catch (e: Exception) {
            Timber.e(e, "Exception formatting string")
        }

        return Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)
    }

    private fun openLink(link: String) = try {
        val openTermsIntent = Intent(requireContext(), WebViewActivity::class.java)
        openTermsIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        openTermsIntent.data = Uri.parse(link)
        startActivity(openTermsIntent)
    } catch (e: Exception) {
        Timber.w("Exception opening WebViewActivity", e)
        val viewIntent = Intent(Intent.ACTION_VIEW)
        viewIntent.data = Uri.parse(link)
        startActivity(viewIntent)
    }

    private fun checkPasswordStrength(s: String) = with(binding) {
        if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_VERYWEAK || s.length < 4) {
            shapePasswdFirst.background = veryWeakDrawable
            shapePasswdSecond.background = shapeDrawable
            shapePasswdThird.background = shapeDrawable
            shapePasswdFourth.background = shapeDrawable
            shapePasswdFifth.background = shapeDrawable
            passwordType.apply {
                text = requireContext().getFormattedStringOrDefault(R.string.pass_very_weak)
                setTextColor(veryWeakColor)
            }
            passwordAdviceText.text =
                requireContext().getFormattedStringOrDefault(R.string.passwd_weak)
            passwdValid = false
            createAccountPasswordTextLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_VeryWeak)
            setEditTextUnderlineColor(createAccountPasswordText, R.color.red_600_red_300)
        } else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_WEAK) {
            shapePasswdFirst.background = weakDrawable
            shapePasswdSecond.background = weakDrawable
            shapePasswdThird.background = shapeDrawable
            shapePasswdFourth.background = shapeDrawable
            shapePasswdFifth.background = shapeDrawable
            passwordType.apply {
                text = requireContext().getFormattedStringOrDefault(R.string.pass_weak)
                setTextColor(weakColor)
            }
            passwordAdviceText.text =
                requireContext().getFormattedStringOrDefault(R.string.passwd_weak)
            passwdValid = true
            createAccountPasswordTextLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_Weak)
            setEditTextUnderlineColor(createAccountPasswordText, R.color.yellow_600_yellow_300)
        } else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_MEDIUM) {
            shapePasswdFirst.background = mediumDrawable
            shapePasswdSecond.background = mediumDrawable
            shapePasswdThird.background = mediumDrawable
            shapePasswdFourth.background = shapeDrawable
            shapePasswdFifth.background = shapeDrawable
            passwordType.apply {
                text = requireContext().getFormattedStringOrDefault(R.string.pass_medium)
                setTextColor(mediumColor)
            }
            passwordAdviceText.text =
                requireContext().getFormattedStringOrDefault(R.string.passwd_medium)
            passwdValid = true
            createAccountPasswordTextLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_Medium)
            setEditTextUnderlineColor(createAccountPasswordText, R.color.green_500_green_400)
        } else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_GOOD) {
            shapePasswdFirst.background = goodDrawable
            shapePasswdSecond.background = goodDrawable
            shapePasswdThird.background = goodDrawable
            shapePasswdFourth.background = goodDrawable
            shapePasswdFifth.background = shapeDrawable
            passwordType.apply {
                text = requireContext().getFormattedStringOrDefault(R.string.pass_good)
                setTextColor(goodColor)
            }
            passwordAdviceText.text =
                requireContext().getFormattedStringOrDefault(R.string.passwd_good)
            passwdValid = true
            createAccountPasswordTextLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_Good)
            setEditTextUnderlineColor(createAccountPasswordText, R.color.lime_green_500_200)
        } else {
            shapePasswdFirst.background = strongDrawable
            shapePasswdSecond.background = strongDrawable
            shapePasswdThird.background = strongDrawable
            shapePasswdFourth.background = strongDrawable
            shapePasswdFifth.background = strongDrawable
            passwordType.apply {
                text = requireContext().getFormattedStringOrDefault(R.string.pass_strong)
                setTextColor(strongColor)
            }
            passwordAdviceText.text =
                requireContext().getFormattedStringOrDefault(R.string.passwd_strong)
            passwdValid = true
            createAccountPasswordTextLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_Strong)
            setEditTextUnderlineColor(createAccountPasswordText, R.color.dark_blue_500_200)
        }
        createAccountPasswordTextErrorIcon.isVisible = false
    }

    private fun submitForm() {
        Timber.d("submit form!")
        dbH.clearCredentials()

        if (!validateForm()) {
            return
        }
        binding.createAccountEmailText.hideKeyboard()

        if (!Util.isOnline(requireContext())) {
            loginActivity?.showSnackbar(
                requireContext().getFormattedStringOrDefault(R.string.error_server_connection_problem)
            )
            return
        }

        with(binding) {
            createAccountCreateLayout.isVisible = false
            createAccountCreatingLayout.isVisible = true
            createAccountCreatingText.isVisible = true
            createAccountProgressBar.isVisible = true
            createAccountAndAcceptLayout.isVisible = false

            val email = createAccountEmailText.text.toString().trim { it <= ' ' }.lowercase()
            val password = createAccountPasswordText.text.toString()
            val name = createAccountNameText.text.toString()
            val lastName = createAccountLastNameText.text.toString()
            val attributes = getInstance().dbH.attributes
            val lastPublicHandle = attributes?.lastPublicHandle ?: MegaApiJava.INVALID_HANDLE

            if (lastPublicHandle == MegaApiJava.INVALID_HANDLE) {
                megaApi.createAccount(email, password, name, lastName, this@CreateAccountFragment)
            } else {
                megaApi.createAccount(
                    email,
                    password,
                    name,
                    lastName,
                    lastPublicHandle,
                    attributes?.lastPublicHandleType ?: -1,
                    attributes?.lastPublicHandleTimeStamp ?: -1L,
                    this@CreateAccountFragment
                )
            }
        }
    }

    private fun validateForm(): Boolean = with(binding) {
        setError(createAccountNameTextLayout, createAccountNameTextErrorIcon, usernameError)
        setError(
            createAccountLastNameTextLayout,
            createAccountLastNameTextErrorIcon,
            userLastnameError
        )
        setError(createAccountEmailTextLayout, createAccountEmailTextErrorIcon, emailError)
        setError(createAccountPasswordTextLayout, createAccountPasswordTextErrorIcon, passwordError)
        setError(
            createAccountPasswordTextConfirmLayout,
            createAccountPasswordTextConfirmErrorIcon,
            passwordConfirmError
        )

        // Return false on any error or true on success
        return when {
            usernameError != null -> {
                createAccountNameText.requestFocus()
                false
            }
            userLastnameError != null -> {
                createAccountLastNameText.requestFocus()
                false
            }
            emailError != null -> {
                createAccountEmailText.requestFocus()
                false
            }
            passwordError != null -> {
                createAccountPasswordText.requestFocus()
                false
            }
            passwordConfirmError != null -> {
                createAccountPasswordTextConfirm.requestFocus()
                false
            }
            !createAccountChkTOS.isChecked -> {
                loginActivity?.showSnackbar(
                    requireContext().getFormattedStringOrDefault(R.string.create_account_no_terms)
                )
                false
            }
            !checkboxTop.chkTop.isChecked -> {
                loginActivity?.showSnackbar(
                    requireContext().getFormattedStringOrDefault(R.string.create_account_no_top)
                )
                false
            }
            else -> {
                true
            }
        }
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestStart: ${request.requestString}")
    }

    override fun onRequestFinish(
        api: MegaApiJava, request: MegaRequest,
        e: MegaError,
    ) = with(binding) {
        Timber.d("onRequestFinish")
        if (!isAdded) return@with

        if (e.errorCode != MegaError.API_OK) {
            Timber.w("ERROR CODE: ${e.errorCode} ERROR MESSAGE: ${e.errorString}")
            if (e.errorCode == MegaError.API_EEXIST) {
                loginActivity?.showSnackbar(
                    requireContext().getFormattedStringOrDefault(R.string.error_email_registered)
                )

                createAccountCreateLayout.isVisible = true
                createAccountCreatingLayout.isVisible = false
                createAccountCreatingText.isVisible = false
                createAccountProgressBar.isVisible = false
            } else {
                val message = e.errorString
                loginActivity?.apply {
                    showSnackbar(message)
                    showFragment(Constants.LOGIN_FRAGMENT)
                }
                createAccountCreateLayout.isVisible = true
                createAccountCreatingLayout.isVisible = false
                createAccountCreatingText.isVisible = false
                createAccountProgressBar.isVisible = false
            }
        } else {
            if (createAccountEmailText.text != null && createAccountNameText.text != null
                && createAccountLastNameText.text != null && createAccountPasswordText.text != null
            ) {
                (requireActivity() as LoginActivity?)?.setTemporalDataForAccountCreation(
                    createAccountEmailText.text.toString().lowercase().trim { it <= ' ' },
                    createAccountNameText.text.toString(),
                    createAccountLastNameText.text.toString(),
                    createAccountPasswordText.text.toString()
                )
            }

            dbH.clearEphemeral()
            val ephemeral = EphemeralCredentials(
                request.email,
                request.password,
                request.sessionKey,
                request.name,
                request.text
            )
            dbH.saveEphemeral(ephemeral)
            loginActivity?.showFragment(Constants.CONFIRM_EMAIL_FRAGMENT)
        }
    }

    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.w("onRequestTemporaryError")
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

    private fun setError(layout: TextInputLayout, icon: ImageView, error: String?) = with(binding) {
        if (error.isNullOrEmpty()) {
            return
        }

        layout.apply {
            this.error = error
            setHintTextAppearance(R.style.TextAppearance_InputHint_Error)

            if (id == createAccountPasswordTextLayout.id) {
                setErrorTextAppearance(R.style.TextAppearance_InputHint_Error)
            }
        }
        icon.isVisible = true
    }

    private fun quitError(layout: TextInputLayout, icon: ImageView) = with(binding) {
        layout.apply {
            error = null
            setHintTextAppearance(R.style.TextAppearance_Design_Hint)
        }
        icon.isVisible = false
    }

    override fun onVisibilityChanged(visible: Boolean) {
        if (isAdded) {
            with(binding) {
                createAccountAndAcceptLayout.isVisible =
                    !visible && createAccountCreateLayout.isVisible
            }
        }
    }

    companion object {
        private const val TOS = "https://mega.nz/terms"
    }
}