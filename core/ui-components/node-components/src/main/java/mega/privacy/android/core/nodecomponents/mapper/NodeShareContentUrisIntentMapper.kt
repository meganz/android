package mega.privacy.android.core.nodecomponents.mapper

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.entity.node.NodeShareContentUri
import mega.privacy.android.shared.resources.R as sharedResR
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
                    "${context.packageName}.providers.fileprovider",
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
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content.links.joinToString(separator = "\n\n"))
                // Only shown when sharing multiple links.
                // Keep the "one" plural item: some locales (e.g. Russian) classify numbers like 21 and 31 as "one".
                val linkCount = content.links.size
                if (linkCount > 1) {
                    putExtra(
                        Intent.EXTRA_TITLE,
                        context.resources.getQuantityString(
                            sharedResR.plurals.general_share_link_count_title,
                            linkCount,
                            linkCount,
                        ),
                    )
                }
            }
        }
    }.apply {
        putExtra(Intent.EXTRA_SUBJECT, title)
    }
}