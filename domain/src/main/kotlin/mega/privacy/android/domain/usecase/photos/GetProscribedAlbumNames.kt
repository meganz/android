package mega.privacy.android.domain.usecase.photos

/**
 * Get proscribed album names
 */
fun interface GetProscribedAlbumNames {

    /**
     * Invoke
     */
    suspend operator fun invoke(): List<String>
}