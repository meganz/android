package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.node.NodeShareContentUri
import javax.inject.Inject

/**
 * Node content uri content mapper
 *
 */
class NodeShareContentUrisIntentMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Invoke
     *
     * @param content
     * @param mimeType
     */
    operator fun invoke(
        title: String,
        content: NodeShareContentUri,
        mimeType: String? = null,
    ): Intent = when (content) {
        is NodeShareContentUri.LocalContentUris -> {
            val uris = content.files.map {
                FileProvider.getUriForFile(
                    context,
                    Constants.AUTHORITY_STRING_FILE_PROVIDER,
                    it
                )
            }
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        }

        is NodeShareContentUri.RemoteContentUris -> {
            Intent(Intent.ACTION_SEND).apply {
                type = Constants.TYPE_TEXT_PLAIN
                putExtra(Intent.EXTRA_TEXT, content.links.joinToString(separator = "\n\n"))
            }
        }
    }.apply {
        putExtra(Intent.EXTRA_SUBJECT, title)
    }
}