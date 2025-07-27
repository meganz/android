package mega.privacy.android.core.nodecomponents.mapper

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.navigation.ExtraConstant
import javax.inject.Inject

/**
 * Node content uri content mapper
 *
 */
class NodeContentUriIntentMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Invoke
     *
     * @param intent Intent source
     * @param content
     * @param mimeType
     * @param isSupported true if the content is supported by the app, false otherwise and we open it with a third party app
     */
    operator fun invoke(
        intent: Intent,
        content: NodeContentUri,
        mimeType: String,
        isSupported: Boolean = true,
    ) {
        when {
            content is NodeContentUri.LocalContentUri && isSupported -> {
                intent.setDataAndType(
                    Uri.fromFile(content.file),
                    mimeType
                )
            }

            content is NodeContentUri.LocalContentUri && !isSupported -> {
                // Legacy logic is wrong, we only need to secure the file in case we open it with a third party app
                val mediaFileUri = FileProvider.getUriForFile(
                    context,
                    context.packageName + ".providers.fileprovider",
                    content.file
                )
                intent.setDataAndType(
                    mediaFileUri,
                    mimeType
                )
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            content is NodeContentUri.RemoteContentUri -> {
                intent.putExtra(
                    ExtraConstant.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER,
                    content.shouldStopHttpSever
                )
                intent.setDataAndType(
                    content.url.toUri(),
                    mimeType
                )
            }
        }
    }
}