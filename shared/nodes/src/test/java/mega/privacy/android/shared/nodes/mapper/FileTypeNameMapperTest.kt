package mega.privacy.android.shared.nodes.mapper

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.Test

class FileTypeNameMapperTest {
    private val underTest = FileTypeNameMapper()

    @Test
    fun `test that a curated extension returns its specific type name`() {
        assertThat(underTest("png")).isEqualTo(nameOf(sharedR.string.file_type_name_png_image))
        assertThat(underTest("jpeg")).isEqualTo(nameOf(sharedR.string.file_type_name_jpeg_image))
        assertThat(underTest("gif")).isEqualTo(nameOf(sharedR.string.file_type_name_gif_image))
        assertThat(underTest("heic")).isEqualTo(nameOf(sharedR.string.file_type_name_heic_image))
        assertThat(underTest("webp")).isEqualTo(nameOf(sharedR.string.file_type_name_webp_image))
        assertThat(underTest("mp3")).isEqualTo(nameOf(sharedR.string.file_type_name_mp3_audio))
        assertThat(underTest("wav")).isEqualTo(nameOf(sharedR.string.file_type_name_wav_audio))
        assertThat(underTest("m4a")).isEqualTo(nameOf(sharedR.string.file_type_name_m4a_audio))
        assertThat(underTest("mp4")).isEqualTo(nameOf(sharedR.string.file_type_name_mpeg4_video))
        assertThat(underTest("mov")).isEqualTo(nameOf(sharedR.string.file_type_name_quicktime_video))
        assertThat(underTest("avi")).isEqualTo(nameOf(sharedR.string.file_type_name_avi_video))
        assertThat(underTest("mkv")).isEqualTo(nameOf(sharedR.string.file_type_name_mkv_video))
        assertThat(underTest("zip")).isEqualTo(nameOf(sharedR.string.file_type_name_zip_compressed))
        assertThat(underTest("rar")).isEqualTo(nameOf(sharedR.string.file_type_name_rar_compressed))
        assertThat(underTest("7z")).isEqualTo(nameOf(sharedR.string.file_type_name_7zip_compressed))
        assertThat(underTest("pdf")).isEqualTo(nameOf(sharedR.string.file_type_name_pdf_document))
    }

    @Test
    fun `test that a non-curated extension falls back to its category name`() {
        assertThat(underTest("bmp")).isEqualTo(nameOf(sharedR.string.file_type_name_image))
        assertThat(underTest("flac")).isEqualTo(nameOf(sharedR.string.file_type_name_audio))
        assertThat(underTest("webm")).isEqualTo(nameOf(sharedR.string.file_type_name_video))
        assertThat(underTest("tar")).isEqualTo(nameOf(sharedR.string.file_type_name_compressed))
        assertThat(underTest("txt")).isEqualTo(nameOf(sharedR.string.file_type_name_text))
        assertThat(underTest("svg")).isEqualTo(nameOf(sharedR.string.file_type_name_vector))
        assertThat(underTest("docx")).isEqualTo(nameOf(sharedR.string.file_type_name_word))
        assertThat(underTest("xlsx")).isEqualTo(nameOf(sharedR.string.file_type_name_excel))
        assertThat(underTest("pptx")).isEqualTo(nameOf(sharedR.string.file_type_name_powerpoint))
        assertThat(underTest("psd")).isEqualTo(nameOf(sharedR.string.file_type_name_photoshop))
        assertThat(underTest("ttf")).isEqualTo(nameOf(sharedR.string.file_type_name_font))
        assertThat(underTest("exe")).isEqualTo(nameOf(sharedR.string.file_type_name_executable))
        assertThat(underTest("torrent")).isEqualTo(nameOf(sharedR.string.file_type_name_torrent))
    }

    @Test
    fun `test that the lookup is case-insensitive`() {
        assertThat(underTest("PNG")).isEqualTo(nameOf(sharedR.string.file_type_name_png_image))
        assertThat(underTest("PdF")).isEqualTo(nameOf(sharedR.string.file_type_name_pdf_document))
    }

    @Test
    fun `test that overlapping extensions resolve to the same category as the icon mapper`() {
        assertThat(underTest("apk")).isEqualTo(nameOf(sharedR.string.file_type_name_executable))
        assertThat(underTest("dmg")).isEqualTo(nameOf(sharedR.string.file_type_name_disk_image))
        assertThat(underTest("ods")).isEqualTo(nameOf(sharedR.string.file_type_name_openoffice))
    }

    @Test
    fun `test that an unrecognized extension falls back to a generic name with the uppercased extension`() {
        assertThat(underTest("xyz")).isEqualTo(
            LocalizedText.StringRes(sharedR.string.file_type_name_generic, listOf("XYZ"))
        )
    }

    @Test
    fun `test that a blank extension returns null`() {
        assertThat(underTest("")).isNull()
    }

    private fun nameOf(resId: Int) = LocalizedText.StringRes(resId)
}
