package mega.privacy.android.app

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.databinding.ActivityAuthenticityCredentialsBinding
import mega.privacy.android.app.listeners.GetAttrUserListener
import mega.privacy.android.app.listeners.VerifyCredentialsListener
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.dp2px
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaUser
import org.jetbrains.anko.configuration
import org.jetbrains.anko.portrait
import timber.log.Timber
import javax.inject.Inject

/**
 * Authenticity Credentials Activity.
 *
 * @property passCodeFacade [PasscodeCheck]
 */
@AndroidEntryPoint
class AuthenticityCredentialsActivity : BaseActivity() {

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    private lateinit var binding: ActivityAuthenticityCredentialsBinding

    private val verifyCredentialsListener by lazy { VerifyCredentialsListener(this@AuthenticityCredentialsActivity) }
    private lateinit var contact: MegaUser
    private var contactCredentialsString: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAuthenticityCredentialsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupData(savedInstanceState)
        setupView()
    }

    private fun setupData(savedInstanceState: Bundle?) {
        intent.extras?.getString(Constants.EMAIL).let {
            if (it.isNullOrEmpty()) {
                Timber.e("Cannot init view, contact' email is empty")
                finish()
            } else {
                megaApi.getContact(it).let { user ->
                    if (user == null) {
                        Timber.e("Cannot init view, contact is null")
                        finish()
                    } else {
                        contact = user
                    }
                }
            }
        }

        if (savedInstanceState != null) {
            contactCredentialsString = savedInstanceState.getString(CONTACT_CREDENTIALS)
        }
    }

    private fun setupView() = with(binding) {
        setSupportActionBar(binding.credentialsToolbar)

        supportActionBar?.apply {
            elevation = 0f
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = getFormattedStringOrDefault(R.string.authenticity_credentials_label)
        }

        credentialsScrollview.setOnScrollChangeListener { _: View?, _: Int, _: Int, _: Int, _: Int ->
            Util.changeViewElevation(supportActionBar,
                credentialsScrollview.canScrollVertically(-1),
                outMetrics)
        }

        val isPortrait = configuration.portrait

        if (!isPortrait) {
            with(contactCredentialsLayout) {
                val params = layoutParams as RelativeLayout.LayoutParams
                params.topMargin = dp2px(MARGIN_TOP_NAME)
                layoutParams = params
            }
        }

        contactCredentialsName.text = getFormattedStringOrDefault(
            R.string.label_contact_credentials,
            ContactUtil.getContactNameDB(contact.handle))

        contactCredentialsEmail.text = contact.email


        if (isPortrait) {
            contactCredentialsRow.isVisible = true
        } else {
            with(contactCredentials) {
                val params = layoutParams as RelativeLayout.LayoutParams
                params.topMargin = dp2px(MARGIN_TOP_CONTACT_CREDENTIALS)
                params.rightMargin = dp2px(MARGIN_RIGHT_CONTACT_CREDENTIALS)
                layoutParams = params

                contactCredentials10.isVisible = true
                setColumnStretchable(5, true)
                contactCredentials11.isVisible = true
                setColumnStretchable(6, true)
                contactCredentials12.isVisible = true
                setColumnStretchable(7, true)
                contactCredentials13.isVisible = true
                setColumnStretchable(8, true)
                contactCredentials14.isVisible = true
                setColumnStretchable(9, true)
            }

            contactCredentialsRow.isVisible = false
        }

        if (contactCredentialsString.isNullOrEmpty()) {
            megaApi.getUserCredentials(contact,
                GetAttrUserListener(this@AuthenticityCredentialsActivity))
        }

        val marginTopBottomButtonAndMyCredentials =
            dp2px(MARGIN_TOP_BOTTOM_BUTTON_AND_MY_CREDENTIALS)
        val marginLeftExplanationAndMyCredentials =
            dp2px(MARGIN_LEFT_EXPLANATION_AND_MY_CREDENTIALS)
        val marginRightExplanationAndMyCredentials =
            dp2px(MARGIN_RIGHT_EXPLANATION_AND_MY_CREDENTIALS)


        updateButtonText()

        with(credentialsButton) {
            if (!isPortrait) {
                val params = layoutParams as RelativeLayout.LayoutParams
                params.bottomMargin = marginTopBottomButtonAndMyCredentials
                params.topMargin = params.bottomMargin
                layoutParams = params

                with(credentialsExplanation) {
                    val explanationParams = layoutParams as RelativeLayout.LayoutParams
                    explanationParams.topMargin = dp2px(MARGIN_TOP_EXPLANATION)
                    explanationParams.leftMargin = marginLeftExplanationAndMyCredentials
                    explanationParams.rightMargin = marginRightExplanationAndMyCredentials
                    layoutParams = explanationParams
                }
            }

            setOnClickListener {
                if (verifyCredentialsListener.isVerifyingCredentials()) {
                    showSnackbar(getFormattedStringOrDefault(R.string.already_verifying_credentials))
                } else {
                    alpha = 0.5f

                    if (megaApi.areCredentialsVerified(contact)) {
                        megaApi.resetCredentials(contact, verifyCredentialsListener)
                    } else {
                        megaApi.verifyCredentials(contact, verifyCredentialsListener)
                    }
                }
            }
        }

        if (!isPortrait) {
            with(myCredentialsContainer) {
                val params = layoutParams as RelativeLayout.LayoutParams
                params.leftMargin = marginLeftExplanationAndMyCredentials
                params.bottomMargin = marginTopBottomButtonAndMyCredentials
                params.topMargin = params.bottomMargin
                params.rightMargin = marginRightExplanationAndMyCredentials
                layoutParams = params
            }


            with(myCredentials) {
                myCredentials10.isVisible = true
                setColumnStretchable(5, true)
                myCredentials11.isVisible = true
                setColumnStretchable(6, true)
                myCredentials12.isVisible = true
                setColumnStretchable(7, true)
                myCredentials13.isVisible = true
                setColumnStretchable(8, true)
                myCredentials14.isVisible = true
                setColumnStretchable(9, true)
            }

            myCredentialsRow.isVisible = false
        }

        setMyCredentials()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(CONTACT_CREDENTIALS, contactCredentialsString)
        super.onSaveInstanceState(outState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Updates the text of the button depending on if the contact' credentials are verified or not.
     */
    private fun updateButtonText() = with(binding.credentialsButton) {
        text = getFormattedStringOrDefault(
            if (megaApi.areCredentialsVerified(contact)) R.string.action_reset
            else R.string.general_verify)

        alpha = if (verifyCredentialsListener.isVerifyingCredentials()) 0.5f else 1f
    }

    /**
     * Finishes the verify or reset action of credentials when the request finishes.
     * If everything went well, updates the text of the button. Otherwise, shows an error message.
     *
     * @param request MegaRequest that contains the results of the request
     * @param e       MegaError that contains the error result of the request
     */
    fun finishVerifyCredentialsAction(request: MegaRequest, e: MegaError) {
        if (request.nodeHandle != contact.handle) return

        if (e.errorCode == MegaError.API_OK) {
            updateButtonText()
            if (megaApi.areCredentialsVerified(contact)) {
                showSnackbar(getString(R.string.label_verified))
            }
            return
        }

        showSnackbar(StringResourcesUtils.getTranslatedErrorString(e))
    }

    /**
     * Sets the credentials of the current logged in account in the view.
     */
    private fun setMyCredentials() = with(binding) {
        val myCredentialsList = getCredentialsList(megaApi.myCredentials)
        if (myCredentialsList.size != 10) {
            Timber.w("Error, my credentials are wrong")
            return
        }

        myCredentials00.text = myCredentialsList[0]
        myCredentials01.text = myCredentialsList[1]
        myCredentials02.text = myCredentialsList[2]
        myCredentials03.text = myCredentialsList[3]
        myCredentials04.text = myCredentialsList[4]
        myCredentials10.text = myCredentialsList[5]
        myCredentials11.text = myCredentialsList[6]
        myCredentials12.text = myCredentialsList[7]
        myCredentials13.text = myCredentialsList[8]
        myCredentials14.text = myCredentialsList[9]
    }

    /**
     * Sets the credentials of the contact in question in the view.
     *
     * @param contactCredentials String that contains the contact's credentials
     */
    private fun setContactCredentials(contactCredentials: String) = with(binding) {
        val contactCredentialsList = getCredentialsList(contactCredentials)
        if (contactCredentialsList.size != 10) {
            Timber.w("Error, the contact's credentials are wrong")
            return
        }

        contactCredentials00.text = contactCredentialsList[0]
        contactCredentials01.text = contactCredentialsList[1]
        contactCredentials02.text = contactCredentialsList[2]
        contactCredentials03.text = contactCredentialsList[3]
        contactCredentials04.text = contactCredentialsList[4]
        contactCredentials10.text = contactCredentialsList[5]
        contactCredentials11.text = contactCredentialsList[6]
        contactCredentials12.text = contactCredentialsList[7]
        contactCredentials13.text = contactCredentialsList[8]
        contactCredentials14.text = contactCredentialsList[9]
    }

    /**
     * Sets the contact's credentials after the request for ask for them finishes.
     * If everything went well, sets contact's credentials in the view. Otherwise, shows an error message.
     *
     * @param request MegaRequest that contains the results of the request
     * @param e       MegaError that contains the error result of the request
     */
    fun setContactCredentials(request: MegaRequest, e: MegaError) {
        if (e.errorCode == MegaError.API_OK && request.flag) {
            setContactCredentials(request.password)
            return
        }
        showSnackbar(StringResourcesUtils.getTranslatedErrorString(e))
    }

    /**
     * Divides in 10 equal parts the credentials string to show it in the view.
     *
     * @param credentials String that contains the credentials
     * @return An ArrayList of Strings containing the credentials divided in 10.
     */
    private fun getCredentialsList(credentials: String?): ArrayList<String> {
        val credentialsList = ArrayList<String>()

        if (credentials.isNullOrEmpty()) {
            Timber.w("Error getting credentials list")
            return credentialsList
        }

        var index = 0

        for (i in 0 until LENGTH_CREDENTIALS_LIST) {
            credentialsList.add(credentials.substring(index, index + LENGTH_CREDENTIALS_CHARACTERS))
            index += LENGTH_CREDENTIALS_CHARACTERS
        }

        return credentialsList
    }

    /**
     * Shows a Snackbar.
     *
     * @param s String that contains the message to show in the Snackbar.
     */
    private fun showSnackbar(s: String) {
        showSnackbar(binding.authenticityCredentialsLayout, s)
    }

    companion object {
        private const val CONTACT_CREDENTIALS = "CONTACT_CREDENTIALS"
        private const val LENGTH_CREDENTIALS_LIST = 10
        private const val LENGTH_CREDENTIALS_CHARACTERS = 4

        //Landscape layout params
        private const val MARGIN_TOP_NAME = 12F
        private const val MARGIN_RIGHT_CONTACT_CREDENTIALS = 188F
        private const val MARGIN_TOP_CONTACT_CREDENTIALS = 16F
        private const val MARGIN_TOP_BOTTOM_BUTTON_AND_MY_CREDENTIALS = 20F
        private const val MARGIN_TOP_EXPLANATION = 18F
        private const val MARGIN_LEFT_EXPLANATION_AND_MY_CREDENTIALS = 72F
        private const val MARGIN_RIGHT_EXPLANATION_AND_MY_CREDENTIALS = 123F
    }
}