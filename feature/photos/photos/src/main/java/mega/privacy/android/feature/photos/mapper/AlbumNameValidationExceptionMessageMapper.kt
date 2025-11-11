package mega.privacy.android.feature.photos.mapper

import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

class AlbumNameValidationExceptionMessageMapper @Inject constructor() {
    operator fun invoke(exception: AlbumNameValidationException): Int =
        when (exception) {
            is AlbumNameValidationException.Empty -> sharedR.string.general_invalid_string
            is AlbumNameValidationException.Exists -> sharedR.string.album_invalid_name_error_message
            is AlbumNameValidationException.InvalidCharacters -> sharedR.string.album_name_exists_error_message
            is AlbumNameValidationException.Proscribed -> sharedR.string.general_invalid_characters_defined
        }
}