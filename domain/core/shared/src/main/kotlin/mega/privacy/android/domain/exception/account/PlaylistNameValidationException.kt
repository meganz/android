package mega.privacy.android.domain.exception.account

sealed class PlaylistNameValidationException(message: String) : Exception(message) {

    data object Empty : PlaylistNameValidationException("Playlist name is empty")

    data object Proscribed : PlaylistNameValidationException("Playlist name is not allowed")

    data object Exists : PlaylistNameValidationException("Playlist name already exists")

    data class InvalidDot(override val message: String = "Playlist name not allowed: ``.´´") :
        PlaylistNameValidationException(message)

    data class InvalidDoubleDot(override val message: String = "Playlist name not allowed: ``..´´") :
        PlaylistNameValidationException(message)

    data class InvalidCharacters(val chars: String) :
        PlaylistNameValidationException("Playlist name contains invalid characters")
}