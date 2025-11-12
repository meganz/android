package mega.privacy.android.domain.usecase.media

import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.usecase.photos.GetProscribedAlbumNamesUseCase
import javax.inject.Inject

class ValidateAlbumNameUseCase @Inject constructor(
    private val getProscribedAlbumNamesUseCase: GetProscribedAlbumNamesUseCase,
    private val albumsRepository: AlbumRepository,
) {
    @Throws(AlbumNameValidationException::class)
    suspend operator fun invoke(name: String) {
        if (name.isBlank()) {
            throw AlbumNameValidationException.Empty
        }

        val proscribedStrings = getProscribedAlbumNamesUseCase()
        val isProscribed = proscribedStrings.any { it.equals(name, true) }

        if (isProscribed) {
            throw AlbumNameValidationException.Proscribed
        }

        val albumNames = albumsRepository.getAllUserSets().map { it.name }

        if (name in albumNames) {
            throw AlbumNameValidationException.Exists
        }

        if (name.hasSpecialCharacters()) {
            throw AlbumNameValidationException.InvalidCharacters(SPECIAL_CHARACTERS)
        }
    }

    private fun String.hasSpecialCharacters(): Boolean {
        val specialCharacters = SPECIAL_CHARACTERS.toRegex()
        return specialCharacters.containsMatchIn(this)
    }

    companion object {
        private const val SPECIAL_CHARACTERS = "[\\\\*/:<>?\"|]"
    }
}