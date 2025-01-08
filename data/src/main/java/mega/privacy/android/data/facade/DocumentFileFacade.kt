package mega.privacy.android.data.facade

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.wrapper.DocumentFileWrapper
import java.io.File
import javax.inject.Inject

/**
 * Default implementation of [DocumentFileWrapper]
 *
 * @property context [ApplicationContext]
 */
class DocumentFileFacade @Inject constructor(
    @ApplicationContext private val context: Context,
) : DocumentFileWrapper {

    override fun fromTreeUri(uri: Uri): DocumentFile? =
        DocumentFile.fromTreeUri(context, uri)

    override fun fromSingleUri(uri: Uri): DocumentFile? =
        DocumentFile.fromSingleUri(context, uri)

    override fun getDocumentId(documentFile: DocumentFile): String =
        DocumentsContract.getDocumentId(documentFile.uri)

    override fun fromUri(uri: Uri): DocumentFile? =
        DocumentsContract.isTreeUri(uri).let { isTreeUri ->
            if (isTreeUri) {
                fromTreeUri(uri)
            } else {
                fromSingleUri(uri)
            }
        }

    override fun fromFile(file: File): DocumentFile =
        DocumentFile.fromFile(file)

    override fun getSdDocumentFile(
        folderUri: Uri,
        subFolders: List<String>,
        fileName: String,
        mimeType: String,
    ): DocumentFile? {
        var folderDocument = fromUri(folderUri)

        subFolders.forEach { folder ->
            folderDocument =
                folderDocument?.findFile(folder) ?: folderDocument?.createDirectory(folder)
        }
        folderDocument?.findFile(fileName)?.delete()

        return folderDocument?.createFile(mimeType, fileName)
    }
}