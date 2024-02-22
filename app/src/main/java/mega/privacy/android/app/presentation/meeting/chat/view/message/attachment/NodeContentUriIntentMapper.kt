package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.node.NodeContentUri
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
     * @param isSupported Is supported by in-app or 3rd app
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
                    Constants.AUTHORITY_STRING_FILE_PROVIDER,
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
                    Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER,
                    content.shouldStopHttpSever
                )
                intent.setDataAndType(
                    Uri.parse(content.url),
                    mimeType
                )
            }
        }
    }
}