package mega.privacy.android.shared.nodes.mapper

import androidx.annotation.StringRes
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps a file extension to a human-readable, localized type name, e.g. "PNG Image", "MPEG-4 Video",
 * "Word Document". Categories are shared with [FileTypeIconMapper] via [FileTypeCategory], so a
 * file's icon and type name always agree. A curated set of common extensions gets a specific name;
 * the rest get their category label; an unrecognized (but present) extension falls back to a
 * generic "<EXT> File".
 */
@Singleton
class FileTypeNameMapper @Inject constructor() {
    /**
     * @param fileExtension the file extension (without the dot)
     * @return the localized type name, or null when there is no extension to describe
     */
    operator fun invoke(fileExtension: String): LocalizedText? {
        val ext = fileExtension.lowercase()
        val nameRes = extensionOverrides[ext] ?: FileTypeCategory.fromExtension(ext)?.nameRes()
        return when {
            nameRes != null -> LocalizedText.StringRes(nameRes)
            ext.isNotBlank() -> LocalizedText.StringRes(
                sharedR.string.file_type_name_generic,
                listOf(ext.uppercase()),
            )

            else -> null
        }
    }
}

/**
 * Specific names for popular extensions; these win over the category label.
 */
private val extensionOverrides = mapOf(
    "png" to sharedR.string.file_type_name_png_image,
    "jpg" to sharedR.string.file_type_name_jpeg_image,
    "jpeg" to sharedR.string.file_type_name_jpeg_image,
    "gif" to sharedR.string.file_type_name_gif_image,
    "heic" to sharedR.string.file_type_name_heic_image,
    "webp" to sharedR.string.file_type_name_webp_image,
    "mp3" to sharedR.string.file_type_name_mp3_audio,
    "wav" to sharedR.string.file_type_name_wav_audio,
    "m4a" to sharedR.string.file_type_name_m4a_audio,
    "mp4" to sharedR.string.file_type_name_mpeg4_video,
    "m4v" to sharedR.string.file_type_name_mpeg4_video,
    "mov" to sharedR.string.file_type_name_quicktime_video,
    "avi" to sharedR.string.file_type_name_avi_video,
    "mkv" to sharedR.string.file_type_name_mkv_video,
    "zip" to sharedR.string.file_type_name_zip_compressed,
    "rar" to sharedR.string.file_type_name_rar_compressed,
    "7z" to sharedR.string.file_type_name_7zip_compressed,
)

@StringRes
private fun FileTypeCategory.nameRes(): Int = when (this) {
    FileTypeCategory.Text -> sharedR.string.file_type_name_text
    FileTypeCategory.WebData, FileTypeCategory.WebLang -> sharedR.string.file_type_name_web
    FileTypeCategory.ThreeD -> sharedR.string.file_type_name_three_d
    FileTypeCategory.Audio -> sharedR.string.file_type_name_audio
    FileTypeCategory.Cad -> sharedR.string.file_type_name_cad
    FileTypeCategory.Compressed -> sharedR.string.file_type_name_compressed
    FileTypeCategory.Executable -> sharedR.string.file_type_name_executable
    FileTypeCategory.Excel -> sharedR.string.file_type_name_excel
    FileTypeCategory.Dmg -> sharedR.string.file_type_name_disk_image
    FileTypeCategory.Font -> sharedR.string.file_type_name_font
    FileTypeCategory.Illustrator -> sharedR.string.file_type_name_illustrator
    FileTypeCategory.Image -> sharedR.string.file_type_name_image
    FileTypeCategory.InDesign -> sharedR.string.file_type_name_indesign
    FileTypeCategory.Pdf -> sharedR.string.file_type_name_pdf_document
    FileTypeCategory.Photoshop -> sharedR.string.file_type_name_photoshop
    FileTypeCategory.PowerPoint -> sharedR.string.file_type_name_powerpoint
    FileTypeCategory.Premiere -> sharedR.string.file_type_name_premiere
    FileTypeCategory.Raw -> sharedR.string.file_type_name_raw
    FileTypeCategory.Spreadsheet -> sharedR.string.file_type_name_spreadsheet
    FileTypeCategory.Torrent -> sharedR.string.file_type_name_torrent
    FileTypeCategory.Vector -> sharedR.string.file_type_name_vector
    FileTypeCategory.Word -> sharedR.string.file_type_name_word
    FileTypeCategory.Pages -> sharedR.string.file_type_name_pages
    FileTypeCategory.Xd -> sharedR.string.file_type_name_xd
    FileTypeCategory.Keynote -> sharedR.string.file_type_name_keynote
    FileTypeCategory.Numbers -> sharedR.string.file_type_name_numbers
    FileTypeCategory.Url -> sharedR.string.file_type_name_url
    FileTypeCategory.OpenOffice -> sharedR.string.file_type_name_openoffice
    FileTypeCategory.AfterEffects -> sharedR.string.file_type_name_aftereffects
    FileTypeCategory.Video -> sharedR.string.file_type_name_video
}
