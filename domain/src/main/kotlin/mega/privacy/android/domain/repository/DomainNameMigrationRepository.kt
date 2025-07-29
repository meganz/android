package mega.privacy.android.domain.repository

/**
 * Repository to access app's domain while migrating the domain
 */
interface DomainNameMigrationRepository {

    /**
     * Set whether the domain name is `mega.app` or not (`mega.nz`)
     */
    suspend fun setDomainNameMegaDotApp(domainNameMegaDotApp: Boolean)

    /**
     * Get flag value from app preferences
     * @return if true, the domain name is `mega.app`, `mega.nz` otherwise
     */
    suspend fun isDomainNameMegaDotApp(): Boolean

    /**
     * Get in-memory cached flag value from
     * @return if true, the domain name is `mega.app`, `mega.nz` otherwise
     */
    fun isDomainNameMegaDotAppFromCache(): Boolean

}