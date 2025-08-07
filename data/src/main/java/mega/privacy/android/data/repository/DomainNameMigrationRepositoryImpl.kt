package mega.privacy.android.data.repository

import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.domain.repository.DomainNameMigrationRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [DomainNameMigrationRepository] that caches the value in app preferences.
 */
@Singleton
class DomainNameMigrationRepositoryImpl @Inject constructor(
    private val appPreferencesGateway: AppPreferencesGateway,
) : DomainNameMigrationRepository {
    private var isDomainNameMegaDotApp = false
    override suspend fun setDomainNameMegaDotApp(domainNameMegaDotApp: Boolean) {
        Timber.d("New MegaDotAppDomain (ff_site) value: $domainNameMegaDotApp")
        this.isDomainNameMegaDotApp = domainNameMegaDotApp
        appPreferencesGateway.putBoolean(DOMAIN_NAME_MEGA_APP_KEY, domainNameMegaDotApp)
    }

    override suspend fun isDomainNameMegaDotApp() =
        (appPreferencesGateway.monitorBoolean(DOMAIN_NAME_MEGA_APP_KEY, false).firstOrNull()
            ?: false).also {
            Timber.d("MegaDotAppDomain (ff_site) value: $it")
            isDomainNameMegaDotApp = it
        }

    override fun isDomainNameMegaDotAppFromCache() = isDomainNameMegaDotApp

    companion object {
        internal const val DOMAIN_NAME_MEGA_APP_KEY = "Domain_Name_Mega_app"
    }
}