package mega.privacy.android.app.listeners

import android.content.Intent
import android.net.Uri
import mega.privacy.android.app.OpenLinkActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber

/**
 * Listener to handle query recovery response
 *
 * @param activity : OpenLinkActivity
 */
class QueryRecoveryLinkListener(
    private val activity: OpenLinkActivity,
) : MegaRequestListenerInterface {

    /**
     * Callback function for onRequestStart
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        // Do nothing
    }

    /**
     * Callback function for onRequestUpdate
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        // Do nothing
    }

    /**
     * Callback function for onRequestFinish
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type == MegaRequest.TYPE_QUERY_RECOVERY_LINK) {
            with(activity) {
                val url = request.link
                if (url.isNullOrEmpty()) {
                    Timber.w("Error opening link URL null: %s___%d", e.errorString, e.errorCode)
                    this.setError(this.getString(R.string.general_text_error))
                    return
                }

                when (e.errorCode) {
                    MegaError.API_OK -> {
                        if (Util.matchRegexs(url, Constants.RESET_PASSWORD_LINK_REGEXS)) {
                            Intent(this, LoginActivity::class.java).apply {
                                putExtra(Constants.VISIBLE_FRAGMENT, Constants.TOUR_FRAGMENT)
                                action = if (request.flag) {
                                    Constants.ACTION_RESET_PASS
                                } else {
                                    Constants.ACTION_PARK_ACCOUNT
                                }
                                data = Uri.parse(url)
                            }.run {
                                startActivity(this)
                                finish()
                            }
                        }
                    }

                    MegaError.API_EEXPIRED ->
                        if (Util.matchRegexs(url, Constants.RESET_PASSWORD_LINK_REGEXS)) {
                            this.setError(this.getString(R.string.recovery_link_expired))
                        }

                    MegaError.API_EACCESS ->
                        if (Util.matchRegexs(url, Constants.RESET_PASSWORD_LINK_REGEXS)) {
                            Timber.w(
                                "Error opening link not related to this account: %s___%d",
                                e.errorString,
                                e.errorCode)
                            this.setError(this.getString(R.string.error_not_logged_with_correct_account))
                        }

                    else -> this.setError(this.getString(R.string.invalid_link))
                }
            }
        }
    }

    /**
     * Callback function for onRequestTemporaryError
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        // Do nothing
    }
}