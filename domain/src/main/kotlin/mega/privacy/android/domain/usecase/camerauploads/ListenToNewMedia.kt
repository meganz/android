package mega.privacy.android.domain.usecase.camerauploads

/**
 * Listen to new media
 *
 */
fun interface ListenToNewMedia {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke()
}