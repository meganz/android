package mega.privacy.android.app.providers.documentprovider

import androidx.compose.runtime.Stable

/**
 * Cloud drive document provider ui state
 */
@Stable
sealed interface CloudDriveDocumentProviderUiState {

    /**
     * Loading document
     *
     * @property accountName
     * @property currentDocumentId
     */
    data class LoadingDocument(
        override val accountName: String,
        val currentDocumentId: String,
    ) : CloudDriveDocumentProviderUiState, HasCredentials

    /**
     * Loading children
     *
     * @property accountName
     * @property currentParentDocumentId
     */
    data class LoadingChildren(
        override val accountName: String,
        val currentParentDocumentId: String,
    ) : CloudDriveDocumentProviderUiState, HasCredentials

    /**
     * Not logged in
     */
    data object NotLoggedIn : CloudDriveDocumentProviderUiState

    /**
     * Root node not loaded
     *
     * @property accountName
     */
    data class RootNodeNotLoaded(override val accountName: String) :
        CloudDriveDocumentProviderUiState, HasCredentials

    /**
     * Loading root
     */
    data object Initialising : CloudDriveDocumentProviderUiState

    /**
     * Document data
     *
     * @property accountName
     * @property documentId
     * @property document
     */
    data class DocumentData(
        override val accountName: String,
        val documentId: String,
        val document: CloudDriveDocumentRow,
    ) : CloudDriveDocumentProviderUiState, HasCredentials

    /**
     * Child data
     *
     * @property accountName
     * @property parentId
     * @property children
     * @property hasMore
     */
    data class ChildData(
        override val accountName: String,
        val parentId: String,
        val children: List<CloudDriveDocumentRow>,
        val hasMore: Boolean,
    ) : CloudDriveDocumentProviderUiState, HasCredentials

    /**
     * File not found
     *
     * @property accountName
     * @property documentId
     */
    data class FileNotFound(
        override val accountName: String,
        val documentId: String,
    ) : CloudDriveDocumentProviderUiState, HasCredentials
}

/**
 * Has credentials
 */
internal interface HasCredentials {
    val accountName: String
}
