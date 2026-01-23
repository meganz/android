package mega.privacy.android.feature.photos.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.exception.account.PlaylistNameValidationException
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Mapper to map [PlaylistNameValidationException] to error message string
 */
class VideoPlaylistTitleValidationErrorMessageMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Map [PlaylistNameValidationException] to error message string
     * @param exception The [PlaylistNameValidationException]
     * @return The error message string or null if no mapping found
     */
    operator fun invoke(exception: PlaylistNameValidationException) =
        when (exception) {
            is PlaylistNameValidationException.Empty ->
                context.getString(sharedR.string.general_invalid_string)

            is PlaylistNameValidationException.Exists ->
                context.getString(sharedR.string.video_section_playlists_error_message_playlist_name_exists)

            is PlaylistNameValidationException.InvalidCharacters ->
                context.getString(
                    sharedR.string.general_invalid_characters_defined,
                    exception.chars
                )

            else -> null
        }
}