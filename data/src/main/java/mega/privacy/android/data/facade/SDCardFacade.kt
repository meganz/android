package mega.privacy.android.data.facade

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.gateway.SDCardGateway
import mega.privacy.android.data.wrapper.DocumentFileWrapper
import javax.inject.Inject

/**
 * Default implementation of [SDCardGateway]
 *
 * @property context [ApplicationContext]
 */
class SDCardFacade @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentFileWrapper: DocumentFileWrapper,
) : SDCardGateway {

    override suspend fun getDirectoryName(localPath: String): String {
        val uri = Uri.parse(localPath)
        val documentFile = documentFileWrapper.fromTreeUri(uri)
        return documentFile?.let {
            if (it.canWrite()) it.name else ""
        } ?: ""
    }

    override suspend fun doesFolderExists(localPath: String): Boolean {
        val fileList = context.getExternalFilesDirs(null)
        return if (fileList.isNotEmpty() && fileList[1] != null) {
            val rootSDCardPath = getRootSDCardPath(fileList[1].absolutePath)
            localPath.startsWith(rootSDCardPath)
        } else {
            false
        }
    }

    override suspend fun getRootSDCardPath(path: String): String {
        var count = 0
        var maxIndex = 0

        for (character in path.toCharArray()) {
            maxIndex++
            if (character == '/') count++
            if (count == 3) break
        }
        return path.substring(0, maxIndex)
    }
}