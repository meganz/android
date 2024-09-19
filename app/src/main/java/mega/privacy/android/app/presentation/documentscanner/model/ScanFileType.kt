package mega.privacy.android.app.presentation.documentscanner.model

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.annotation.DrawableRes

/**
 * An enumeration of different file types to upload the scanned document. Note that selection is only
 * applicable when there is only one scanned document
 *
 * @param fileSuffix The String to be appended to the filename when uploading the scanned document
 * @param iconRes The icon to be displayed for this File Type
 */
enum class ScanFileType(
    val fileSuffix: String,
    @DrawableRes val iconRes: Int,
) {

    /**
     * The scanned document should be uploaded as a PDF file
     */
    Pdf(
        fileSuffix = ".pdf",
        iconRes = IconPackR.drawable.ic_pdf_medium_solid,
    ),

    /**
     * The scanned document should be uploaded as a JPG file
     */
    Jpg(
        fileSuffix = ".jpg",
        iconRes = IconPackR.drawable.ic_image_medium_solid,
    ),
}