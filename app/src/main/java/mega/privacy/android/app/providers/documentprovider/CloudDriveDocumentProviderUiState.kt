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
     * @property rootNodeDocumentId
     */
    data class LoadingDocument(
        override val accountName: String,
        val currentDocumentId: String,
        override val rootNodeDocumentId: String,
    ) : CloudDriveDocumentProviderUiState, HasRoot

    /**
     * Loading children
     *
     * @property accountName
     * @property currentParentDocumentId
     * @property rootNodeDocumentId
     */
    data class LoadingChildren(
        override val accountName: String,
        val currentParentDocumentId: String,
        override val rootNodeDocumentId: String,
    ) : CloudDriveDocumentProviderUiState, HasRoot

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
    data object LoadingRoot : CloudDriveDocumentProviderUiState

    /**
     * Root
     *
     * @property accountName
     * @property rootNodeDocumentId
     */
    data class Root(
        override val accountName: String,
        override val rootNodeDocumentId: String,
    ) : CloudDriveDocumentProviderUiState, HasRoot

    /**
     * Document data
     *
     * @property accountName
     * @property documentId
     * @property document
     * @property rootNodeDocumentId
     */
    data class DocumentData(
        override val accountName: String,
        val documentId: String,
        val document: CloudDriveDocumentRow,
        override val rootNodeDocumentId: String,
    ) : CloudDriveDocumentProviderUiState, HasRoot

    /**
     * Child data
     *
     * @property accountName
     * @property parentId
     * @property children
     * @property hasMore
     * @property rootNodeDocumentId
     */
    data class ChildData(
        override val accountName: String,
        val parentId: String,
        val children: List<CloudDriveDocumentRow>,
        val hasMore: Boolean,
        override val rootNodeDocumentId: String,
    ) : CloudDriveDocumentProviderUiState, HasRoot

    /**
     * File not found
     *
     * @property accountName
     * @property documentId
     * @property rootNodeDocumentId
     */
    data class FileNotFound(
        override val accountName: String,
        val documentId: String,
        override val rootNodeDocumentId: String,
    ) : CloudDriveDocumentProviderUiState, HasRoot
}

/**
 * Has credentials
 */
internal interface HasCredentials {
    val accountName: String
}

/**
 * Has root
 */
internal interface HasRoot : HasCredentials {
    val rootNodeDocumentId: String
}
