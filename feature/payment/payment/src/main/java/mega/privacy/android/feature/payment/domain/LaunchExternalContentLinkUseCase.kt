package mega.privacy.android.feature.payment.domain

import android.app.Activity
import android.net.Uri
import mega.privacy.android.data.repository.AndroidBillingRepository
import mega.privacy.android.domain.entity.billing.ExternalContentLinkResult
import javax.inject.Inject

/**
 * Launch external content link using Google Play Billing Library's external content links API.
 *
 * External content links allow apps to direct users to external websites for purchases
 * while still using Google Play Billing for verification.
 */
class LaunchExternalContentLinkUseCase @Inject constructor(
    private val androidBillingRepository: AndroidBillingRepository,
) {
    /**
     * Invoke - Launches external content link and returns operation result.
     *
     * This method:
     * 1. Creates billing program reporting details to get external transaction token
     * 2. Launches the external link with the token using Google Play Billing API
     *
     * @param activity The activity to launch from
     * @param linkUri The URI of the external website
     * @return [ExternalContentLinkResult] The result of the operation (Success, Cancelled, or Failed)
     */
    suspend operator fun invoke(
        activity: Activity,
        linkUri: Uri,
    ): ExternalContentLinkResult {
        return androidBillingRepository.launchExternalContentLink(activity, linkUri)
    }
}

