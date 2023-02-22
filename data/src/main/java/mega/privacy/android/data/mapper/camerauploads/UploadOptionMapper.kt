package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption

/**
 * Mapper class that maps a given state into a corresponding [UploadOption]
 */
fun interface UploadOptionMapper {

    /**
     * Invocation function
     *
     * @param state The current upload option from the database
     *
     * @return the corresponding [UploadOption]
     */
    operator fun invoke(state: String?): UploadOption
}