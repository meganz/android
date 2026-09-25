package mega.privacy.android.domain.usecase.setting.cookies

import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import javax.inject.Inject

/**
 * Use Case to get the URL of the cookie policy page.
 */
class GetCookieUrlUseCase @Inject constructor(
    private val getDomainNameUseCase: GetDomainNameUseCase,
) {
    suspend operator fun invoke() = "https://${getDomainNameUseCase()}/cookie"
}