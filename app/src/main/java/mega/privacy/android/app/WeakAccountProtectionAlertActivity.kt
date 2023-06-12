package mega.privacy.android.app

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.MegaApplication.Companion.isBlockedDueToWeakAccount
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.databinding.ActivityWeakAccountProtectionAlertBinding
import mega.privacy.android.app.listeners.ResendVerificationEmailListener
import mega.privacy.android.app.listeners.WhyAmIBlockedListener
import mega.privacy.android.app.main.controllers.AccountController.Companion.logout
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.qualifier.ApplicationScope
import timber.log.Timber
import javax.inject.Inject

/**
 * Weak account protection activity
 */
@AndroidEntryPoint
class WeakAccountProtectionAlertActivity : PasscodeActivity(), View.OnClickListener {

    /**
     * Application scope
     */
    @ApplicationScope
    @Inject
    lateinit var sharingScope: CoroutineScope

    private lateinit var activityWeakAccountProtectionAlertBinding: ActivityWeakAccountProtectionAlertBinding

    private var infoDialog: AlertDialog? = null
    private var isInfoDialogShown = false
    private var isAccountBlocked = true

    /**
     * onCreate lifecycle callback
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isBlockedDueToWeakAccount = true
        activityWeakAccountProtectionAlertBinding =
            ActivityWeakAccountProtectionAlertBinding.inflate(layoutInflater)
        setContentView(activityWeakAccountProtectionAlertBinding.root)
        if (savedInstanceState != null) {
            isInfoDialogShown = savedInstanceState.getBoolean(IS_INFO_DIALOG_SHOWN, false)
            isAccountBlocked = savedInstanceState.getBoolean(IS_ACCOUNT_BLOCKED, true)
            if (isInfoDialogShown) {
                showInfoDialog()
            }
        }
        setView()
    }

    private fun setView() {
        activityWeakAccountProtectionAlertBinding.apply {
            verifyEmailText.text = getVerifyEmailAndFollowStepsText()
            whyAmISeeingThisLayout.setOnClickListener(this@WeakAccountProtectionAlertActivity)
            resendEmailButton.setOnClickListener(this@WeakAccountProtectionAlertActivity)
            logoutButton.setOnClickListener(this@WeakAccountProtectionAlertActivity)
        }
    }

    private fun getVerifyEmailAndFollowStepsText(): Spanned? {
        var text = String.format(getString(R.string.verify_email_and_follow_steps))
        text = text.replace("[A]", "<b>")
        text = text.replace("[/A]", "</b>")
        return Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
    }

    /**
     * onSaveInstanceState
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(IS_INFO_DIALOG_SHOWN, isInfoDialogShown)
        outState.putBoolean(IS_ACCOUNT_BLOCKED, isAccountBlocked)
        super.onSaveInstanceState(outState)
    }

    /**
     * onResume
     */
    override fun onResume() {
        super.onResume()
        if (isAccountBlocked) {
            megaApi.whyAmIBlocked(WhyAmIBlockedListener(this))
        }
    }

    /**
     * onBackPressed
     */
    override fun onBackPressed() {
        if (psaWebBrowser?.consumeBack() == true) return
        //        Do nothing: do not permit to skip the warning, account blocked
    }

    /**
     * onDestroy
     */
    override fun onDestroy() {
        super.onDestroy()
        isBlockedDueToWeakAccount = false
    }

    /**
     * onClick
     * Handles button clicks
     */
    override fun onClick(v: View) {
        when (v.id) {
            R.id.why_am_i_seeing_this_layout -> showInfoDialog()

            R.id.resend_email_button -> {
                megaApi.resendVerificationEmail(ResendVerificationEmailListener(this))
            }

            R.id.ok_button -> {
                isInfoDialogShown = false
                try {
                    infoDialog?.dismiss()
                } catch (e: Exception) {
                    Timber.w("Exception dismissing infoDialog")
                }
            }

            R.id.logout_button -> logout(this, megaApi, sharingScope)
        }
    }

    /**
     * Creates and shows an info dialog with possible causes why
     * the user is blocked
     */
    private fun showInfoDialog() {
        if (infoDialog?.isShowing == true) return
        val builder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
        val view = layoutInflater.inflate(R.layout.dialog_locked_accounts, null)
        builder.setView(view)
        val okButton = view.findViewById<Button>(R.id.ok_button)
        okButton.setOnClickListener(this)
        infoDialog = builder.create().apply {
            setCanceledOnTouchOutside(false)
            show()
        }
        isInfoDialogShown = true
    }

    /**
     * Manages the result of the request whyAmIBlocked().
     * If the result is due to weak account protection (700), it does nothing.
     * If not, it starts a new complete login and hide the alert.
     *
     * @param result the reason code of why I am blocked
     */
    fun whyAmIBlockedResult(result: String) {
        if (result != Constants.WEAK_PROTECTION_ACCOUNT_BLOCK && isAccountBlocked) {
            isAccountBlocked = false
            if (megaApi.rootNode == null) {
                val intentLogin = Intent(this, LoginActivity::class.java).apply {
                    action = Constants.ACTION_REFRESH_AFTER_BLOCKED
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intentLogin)
            }
            finish()
        }
    }

    /**
     * shows snack bar
     */
    fun showSnackbar(stringResource: Int) {
        showSnackbar(
            activityWeakAccountProtectionAlertBinding.scrollContentLayout,
            getString(stringResource)
        )
    }

    companion object {
        private const val IS_INFO_DIALOG_SHOWN = "IS_INFO_DIALOG_SHOWN"
        private const val IS_ACCOUNT_BLOCKED = "IS_ACCOUNT_BLOCKED"
    }
}