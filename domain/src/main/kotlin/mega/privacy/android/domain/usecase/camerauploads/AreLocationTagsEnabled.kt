package mega.privacy.android.domain.usecase.camerauploads

/**
 * Use Case that checks whether Location Tags are added or not, when uploading Photos
 */
fun interface AreLocationTagsEnabled {

    /**
     * Invocation function
     *
     * @return true if Location Tags should be added when uploading Photos, and false if otherwise
     */
    suspend operator fun invoke(): Boolean
}
