package mega.privacy.android.domain.exception.account

sealed class AlbumNameValidationException : Exception("Album name validation failed!") {
    data object Empty : AlbumNameValidationException() {
        private fun readResolve(): Any = Empty
    }

    data object Proscribed : AlbumNameValidationException() {
        private fun readResolve(): Any = Proscribed
    }

    data object Exists : AlbumNameValidationException() {
        private fun readResolve(): Any = Exists
    }

    data class InvalidCharacters(val chars: String) : AlbumNameValidationException()
}