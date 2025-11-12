package mega.privacy.android.feature.photos.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

class AlbumNameValidationExceptionMessageMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    operator fun invoke(exception: AlbumNameValidationException): String {
        return when (exception) {
            is AlbumNameValidationException.Empty -> context.getString(sharedR.string.general_invalid_string)
            is AlbumNameValidationException.Proscribed -> context.getString(sharedR.string.album_invalid_name_error_message)
            is AlbumNameValidationException.Exists -> context.getString(sharedR.string.album_name_exists_error_message)
            is AlbumNameValidationException.InvalidCharacters -> context.getString(
                sharedR.string.general_invalid_characters_defined,
                exception.chars
            )
        }
    }
}