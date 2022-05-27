package mega.privacy.android.app.domain.usecase

/**
 * Fetch nodes use case.
 */
interface FetchNodes {

    suspend operator fun invoke()
}