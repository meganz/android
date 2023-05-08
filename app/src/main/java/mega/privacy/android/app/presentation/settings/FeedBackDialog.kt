package mega.privacy.android.app.presentation.settings

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.service.RATE_APP_URL
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import timber.log.Timber

class FeedBackDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setView(
                createFeedbackDialogView(
                    email = arguments?.getString(emailKey).orEmpty(),
                    accountType = arguments?.getString(accountTypeKey).orEmpty()
                )
            )
            .setTitle(R.string.title_evaluate_the_app_panel)
            .create()


    private fun createFeedbackDialogView(
        email: String,
        accountType: String,
    ): View? {
        val dialogLayout = View.inflate(context, R.layout.evaluate_the_app_dialog, null)
        val displayMetrics = getDisplayMetrics()
        val rateAppCheck = dialogLayout.findViewById<CheckedTextView>(R.id.rate_the_app)
        setFeedbackMargins(rateAppCheck, displayMetrics)
        rateAppCheck.setOnClickListener {
            Timber.d("Rate the app")
            //Rate the app option:
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RATE_APP_URL)))
            }.onFailure {
                Timber.d("Can not handle action Intent.ACTION_VIEW")
            }
            dismiss()
        }

        val sendFeedbackCheck =
            dialogLayout.findViewById<CheckedTextView>(R.id.send_feedback)
        setFeedbackMargins(sendFeedbackCheck, displayMetrics)
        sendFeedbackCheck.setOnClickListener {
            Timber.d("Send Feedback")
            val body = generateBody(email, accountType)
            val versionApp = getString(R.string.app_version)
            val subject = getString(R.string.setting_feedback_subject) + " v" + versionApp
            sendEmail(subject, body)
            dismiss()
        }
        return dialogLayout
    }

    @Suppress("DEPRECATION")
    private fun getDisplayMetrics(): DisplayMetrics {
        val display = requireActivity().windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        return outMetrics
    }

    private fun setFeedbackMargins(
        sendFeedbackCheck: CheckedTextView,
        outMetrics: DisplayMetrics,
    ) {
        sendFeedbackCheck.compoundDrawablePadding = Util.scaleWidthPx(10, outMetrics)
        val sendFeedbackMLP = sendFeedbackCheck.layoutParams as ViewGroup.MarginLayoutParams
        sendFeedbackMLP.setMargins(
            Util.scaleWidthPx(15, outMetrics),
            Util.scaleHeightPx(10, outMetrics),
            0,
            Util.scaleHeightPx(10, outMetrics)
        )
    }

    private fun generateBody(email: String, accountType: String): String {
        return StringBuilder()
            .append(getString(R.string.setting_feedback_body))
            .append("\n\n\n\n\n\n\n\n\n\n\n")
            .append(getString(R.string.settings_feedback_body_device_model)).append("  ")
            .append(Util.getDeviceName()).append("\n")
            .append(getString(R.string.settings_feedback_body_android_version)).append("  ")
            .append(Build.VERSION.RELEASE)
            .append(" ").append(Build.DISPLAY).append("\n")
            .append(getString(R.string.user_account_feedback)).append("  ")
            .append(email)
            .append(" ($accountType)")
            .toString()
    }

    private fun sendEmail(subject: String, body: String) {
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = Constants.TYPE_TEXT_PLAIN
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(Constants.MAIL_ANDROID))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        emailIntent.putExtra(Intent.EXTRA_TEXT, body)
        startActivity(Intent.createChooser(emailIntent, " "))
    }

    companion object {
        const val TAG = "FeedBackDialog"
        private const val emailKey = "emailKey"
        private const val accountTypeKey = "accountTypeKey"

        fun newInstance(email: String, accountType: String): FeedBackDialog {
            val args = Bundle().apply {
                putString(emailKey, email)
                putString(accountTypeKey, accountType)
            }

            return FeedBackDialog().apply {
                arguments = args
            }
        }
    }
}