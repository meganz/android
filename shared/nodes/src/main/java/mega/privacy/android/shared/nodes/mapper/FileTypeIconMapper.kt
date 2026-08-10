package mega.privacy.android.shared.nodes.mapper

import mega.privacy.android.icon.pack.R as iconPackR
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper which will return icon for FileTypeInfo
 */
@Singleton
class FileTypeIconMapper @Inject constructor() {
    /**
     * invoke
     * @param fileExtension [String]
     * @param iconType [IconType]
     */
    operator fun invoke(fileExtension: String, iconType: IconType = IconType.Solid): Int {
        val category = FileTypeCategory.fromExtension(fileExtension)
        return categoryIcons[category]?.getOrNull(iconType.index)
            ?: iconPackR.drawable.ic_generic_medium_solid
    }
}

/**
 * The solid + outline icons for each category, indexed by [IconType.index]. Built once so the
 * lookup allocates nothing per call (mappers are hit per list item).
 */
private val categoryIcons: Map<FileTypeCategory, List<Int>> = mapOf(
    FileTypeCategory.Text to listOf(
        iconPackR.drawable.ic_text_medium_solid,
        iconPackR.drawable.ic_text_thumbnail_outline,
    ),
    FileTypeCategory.WebData to listOf(
        iconPackR.drawable.ic_web_data_medium_solid,
        iconPackR.drawable.ic_web_data_thumbnail_outline,
    ),
    FileTypeCategory.ThreeD to listOf(
        iconPackR.drawable.ic_3d_medium_solid,
        iconPackR.drawable.ic_3d_thumbnail_outline,
    ),
    FileTypeCategory.Audio to listOf(
        iconPackR.drawable.ic_audio_medium_solid,
        iconPackR.drawable.ic_audio_thumbnail_outline,
    ),
    FileTypeCategory.Cad to listOf(
        iconPackR.drawable.ic_cad_medium_solid,
        iconPackR.drawable.ic_cad_thumbnail_outline,
    ),
    FileTypeCategory.Compressed to listOf(
        iconPackR.drawable.ic_compressed_medium_solid,
        iconPackR.drawable.ic_compressed_thumbnail_outline,
    ),
    FileTypeCategory.Executable to listOf(
        iconPackR.drawable.ic_executable_medium_solid,
        iconPackR.drawable.ic_executable_thumbnail_outline,
    ),
    FileTypeCategory.Excel to listOf(
        iconPackR.drawable.ic_excel_medium_solid,
        iconPackR.drawable.ic_excel_thumbnail_outline,
    ),
    FileTypeCategory.Dmg to listOf(
        iconPackR.drawable.ic_dmg_medium_solid,
        iconPackR.drawable.ic_dmg_thumbnail_outline,
    ),
    FileTypeCategory.WebLang to listOf(
        iconPackR.drawable.ic_web_lang_medium_solid,
        iconPackR.drawable.ic_web_lang_thumbnail_outline,
    ),
    FileTypeCategory.Font to listOf(
        iconPackR.drawable.ic_font_medium_solid,
        iconPackR.drawable.ic_font_thumbnail_outline,
    ),
    FileTypeCategory.Illustrator to listOf(
        iconPackR.drawable.ic_illustrator_medium_solid,
        iconPackR.drawable.ic_illustrator_thumbnail_outline,
    ),
    FileTypeCategory.Image to listOf(
        iconPackR.drawable.ic_image_medium_solid,
        iconPackR.drawable.ic_image_thumbnail_outline,
    ),
    FileTypeCategory.InDesign to listOf(
        iconPackR.drawable.ic_indesign_medium_solid,
        iconPackR.drawable.ic_indesign_thumbnail_outline,
    ),
    FileTypeCategory.Pdf to listOf(
        iconPackR.drawable.ic_pdf_medium_solid,
        iconPackR.drawable.ic_pdf_thumbnail_outline,
    ),
    FileTypeCategory.Photoshop to listOf(
        iconPackR.drawable.ic_photoshop_medium_solid,
        iconPackR.drawable.ic_photoshop_thumbnail_outline,
    ),
    FileTypeCategory.PowerPoint to listOf(
        iconPackR.drawable.ic_powerpoint_medium_solid,
        iconPackR.drawable.ic_powerpoint_thumbnail_outline,
    ),
    FileTypeCategory.Premiere to listOf(
        iconPackR.drawable.ic_premiere_medium_solid,
        iconPackR.drawable.ic_premiere_thumbnail_outline,
    ),
    FileTypeCategory.Raw to listOf(
        iconPackR.drawable.ic_raw_medium_solid,
        iconPackR.drawable.ic_raw_thumbnail_outline,
    ),
    FileTypeCategory.Spreadsheet to listOf(
        iconPackR.drawable.ic_spreadsheet_medium_solid,
        iconPackR.drawable.ic_spreadsheet_thumbnail_outline,
    ),
    FileTypeCategory.Torrent to listOf(
        iconPackR.drawable.ic_torrent_medium_solid,
        iconPackR.drawable.ic_torrent_thumbnail_outline,
    ),
    FileTypeCategory.Vector to listOf(
        iconPackR.drawable.ic_vector_medium_solid,
        iconPackR.drawable.ic_vector_thumbnail_outline,
    ),
    FileTypeCategory.Word to listOf(
        iconPackR.drawable.ic_word_medium_solid,
        iconPackR.drawable.ic_word_thumbnail_outline,
    ),
    FileTypeCategory.Pages to listOf(
        iconPackR.drawable.ic_pages_medium_solid,
        iconPackR.drawable.ic_pages_thumbnail_outline,
    ),
    FileTypeCategory.Xd to listOf(
        iconPackR.drawable.ic_experiencedesign_medium_solid,
        iconPackR.drawable.ic_experiencedesign_thumbnail_outline,
    ),
    FileTypeCategory.Keynote to listOf(
        iconPackR.drawable.ic_keynote_medium_solid,
        iconPackR.drawable.ic_keynote_thumbnail_outline,
    ),
    FileTypeCategory.Numbers to listOf(
        iconPackR.drawable.ic_numbers_medium_solid,
        iconPackR.drawable.ic_numbers_thumbnail_outline,
    ),
    FileTypeCategory.Url to listOf(
        iconPackR.drawable.ic_url_medium_solid,
        iconPackR.drawable.ic_url_thumbnail_outline,
    ),
    FileTypeCategory.OpenOffice to listOf(
        iconPackR.drawable.ic_openoffice_medium_solid,
        iconPackR.drawable.ic_openoffice_thumbnail_outline,
    ),
    FileTypeCategory.AfterEffects to listOf(
        iconPackR.drawable.ic_aftereffects_medium_solid,
        iconPackR.drawable.ic_aftereffects_thumbnail_outline,
    ),
    FileTypeCategory.Video to listOf(
        iconPackR.drawable.ic_video_medium_solid,
        iconPackR.drawable.ic_video_thumbnail_outline,
    ),
)

/**
 * Type of icon
 *
 * @property index index of type
 */
enum class IconType(val index: Int) {

    /**
     * Solid icon
     */
    Solid(0),

    /**
     * Outline icon
     */
    Outlined(1),
}
