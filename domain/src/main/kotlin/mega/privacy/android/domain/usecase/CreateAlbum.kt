package mega.privacy.android.domain.usecase

/**
 * Use Case to create an album
 */
fun interface CreateAlbum {
    /**
     * The invoke method
     *
     * @param name
     */
    suspend operator fun invoke(name: String)
}