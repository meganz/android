package mega.privacy.android.feature.pdfviewer.presentation.components

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import java.util.UUID

/**
 * App bar "Share" action for the PDF viewer.
 *
 * Reused for two cases that share different things:
 *  - a PDF opened from a public file link → shares the original link URL
 *    ([startPdfPublicLinkShareIntent]), mirroring the legacy viewer and the file-link screen;
 *  - a PDF opened from an external app → shares the file itself via its content URI
 *    ([startPdfFileShareIntent]), mirroring the legacy `FileUtil.shareWithUri`.
 */
internal data object PdfShareAction : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.ShareNetwork)

    override val testTag = "pdf_viewer:share"

    @Composable
    override fun getDescription() = stringResource(sharedR.string.general_share)
}

/**
 * Start a chooser to share a public file-link URL as plain text.
 *
 * @param link the public link to share.
 * @param title the share intent subject (file name).
 */
internal fun Context.startPdfPublicLinkShareIntent(link: String?, title: String?) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, link)
        putExtra(Intent.EXTRA_SUBJECT, title ?: "${UUID.randomUUID()}.url")
    }
    startActivity(
        Intent.createChooser(shareIntent, getString(sharedR.string.general_share))
    )
}

/**
 * Start a chooser to share the PDF file itself (for externally-opened PDFs), mirroring the legacy
 * viewer's `FileUtil.shareWithUri`. Grants temporary read access to the receiving app.
 *
 * @param contentUri the content/file URI string the PDF was opened with.
 * @param mimeType the MIME type to advertise (e.g. `application/pdf`).
 * @param title the share intent subject (file name).
 */
internal fun Context.startPdfFileShareIntent(contentUri: String, mimeType: String, title: String?) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, contentUri.toUri())
        putExtra(Intent.EXTRA_SUBJECT, title ?: "${UUID.randomUUID()}.pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(
        Intent.createChooser(shareIntent, getString(sharedR.string.general_share))
    )
}
