package mega.privacy.android.feature.payment.usecase

import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.GetSessionTransferURLUseCase
import mega.privacy.android.feature.payment.domain.featuretoggle.PaymentFeatures
import javax.inject.Inject

internal class GeneratePurchaseUrlUseCase @Inject constructor(
    private val getDomainNameUseCase: GetDomainNameUseCase,
    private val environmentRepository: EnvironmentRepository,
    private val getSessionTransferURLUseCase: GetSessionTransferURLUseCase,
) {

    suspend operator fun invoke(productId: String, months: Int?): String {
        val domain = getDomain()
        val uao = getUao()
        val monthsQueryParam = months?.let { "&m=$it" } ?: ""
        return getSessionTransferURLUseCase("https://$domain/$productId/uao=$uao$monthsQueryParam")
    }

    private fun getDomain(): String = getDomainNameUseCase()

    private fun getUao(): String =
        "Android app Ver ${environmentRepository.getAppInfo().appVersion}"
}
