package mega.privacy.android.app.presentation.photos.albums.importlink

internal data class AlbumImportState(
    val isInitialized: Boolean = false,
    val showInputDecryptionKeyDialog: Boolean = false,
    val showErrorDialog: Boolean = false,
)
