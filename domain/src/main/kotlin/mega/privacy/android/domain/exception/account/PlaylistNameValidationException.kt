package mega.privacy.android.domain.exception.account

sealed class PlaylistNameValidationException(message: String) : Exception(message) {

    data object Empty : PlaylistNameValidationException("Playlist name is empty")

    data object Proscribed : PlaylistNameValidationException("Playlist name is not allowed")

    data object Exists : PlaylistNameValidationException("Playlist name already exists")

    data object InvalidCharacters :
        PlaylistNameValidationException("Playlist name contains invalid characters")
}