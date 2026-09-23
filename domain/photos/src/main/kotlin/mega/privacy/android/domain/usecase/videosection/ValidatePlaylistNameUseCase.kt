package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.exception.account.PlaylistNameValidationException
import mega.privacy.android.domain.repository.VideoSectionRepository
import mega.privacy.android.domain.usecase.node.CheckForValidNameUseCase.Companion.isInvalidDotName
import mega.privacy.android.domain.usecase.node.CheckForValidNameUseCase.Companion.isInvalidDoubleDotName
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
        when {
            title.isBlank() -> throw PlaylistNameValidationException.Empty
            title.isInvalidDotName() -> throw PlaylistNameValidationException.InvalidDot()
            title.isInvalidDoubleDotName() -> throw PlaylistNameValidationException.InvalidDoubleDot()
            title.hasSpecialCharacters() ->
                throw PlaylistNameValidationException.InvalidCharacters(SPECIAL_CHARACTERS)

            title in videoSectionRepository.getVideoPlaylistTitles() -> throw PlaylistNameValidationException.Exists
        }
    }

    private fun String.hasSpecialCharacters() =
        SPECIAL_CHARACTERS.toRegex().containsMatchIn(this)

    companion object {
        private const val SPECIAL_CHARACTERS = "[\\\\*/:<>?\"|]"
    }
}