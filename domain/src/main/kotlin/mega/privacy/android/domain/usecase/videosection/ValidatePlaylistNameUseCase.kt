package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.exception.account.PlaylistNameValidationException
import mega.privacy.android.domain.repository.VideoSectionRepository
import javax.inject.Inject

/**
 * Use Case to validate video playlist name
 */
class ValidatePlaylistNameUseCase @Inject constructor(
    private val videoSectionRepository: VideoSectionRepository,
) {
    /**
     * Validate video playlist name
     *
     * @param title The title to validate
     * @return true if valid, throws exception if invalid
     * @throws PlaylistNameValidationException.Empty if title is blank
     * @throws PlaylistNameValidationException.Exists if title already exists
     * @throws PlaylistNameValidationException.InvalidCharacters if title contains invalid characters
     */
    @Throws(PlaylistNameValidationException::class)
    suspend operator fun invoke(title: String) {
        if (title.isBlank()) throw PlaylistNameValidationException.Empty
        val titles = videoSectionRepository.getVideoPlaylistTitles()
        when {
            title in titles -> throw PlaylistNameValidationException.Exists
            "[\\\\*/:<>?\"|]".toRegex().containsMatchIn(title) ->
                throw PlaylistNameValidationException.InvalidCharacters
        }
    }
}