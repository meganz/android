package mega.privacy.android.feature.payment.presentation.cancelaccountplan.view.instructionscreens

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import javax.inject.Inject

/**
 * ViewModel for web cancellation/reactivation instruction screens.
 * Provides domain name for building MEGA URLs.
 */
@Keep
@HiltViewModel
internal class WebInstructionsViewModel @Inject constructor(
    private val getDomainNameUseCase: GetDomainNameUseCase,
) : ViewModel() {

    /**
     * Current MEGA domain name (e.g. "mega.nz" or "mega.app")
     */
    val domainName: String
        get() = runCatching { getDomainNameUseCase() }
            .getOrElse { GetDomainNameUseCase.MEGA_NZ_DOMAIN_NAME }

    /**
     * MEGA website URL for the current domain
     */
    val megaUrl: String
        get() = "https://$domainName/"
}
