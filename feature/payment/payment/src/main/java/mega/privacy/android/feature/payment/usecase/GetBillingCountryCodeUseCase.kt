package mega.privacy.android.feature.payment.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.repository.EnvironmentRepository
import java.util.Locale
import javax.inject.Inject

internal class GetBillingCountryCodeUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
    private val billingRepository: BillingRepository,
) {

    suspend operator fun invoke(): String =
        billingRepository.getBillingCountryCode() ?: getDefaultCountryCodeFromLocale()


    private fun getDefaultCountryCodeFromLocale(): String =
        environmentRepository.getLocale().country
}
