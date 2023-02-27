package mega.privacy.android.domain.usecase.camerauploads

/**
 * Use Case that updates the value in the Database, as to whether Location Tags are added or not
 * when uploading Photos in Camera Uploads
 */
fun interface SetLocationTagsEnabled {

    /**
     * Invocation function
     *
     * @param enable true if Location Tags should be added when uploading Photos, and false if otherwise
     */
    suspend operator fun invoke(enable: Boolean)
}