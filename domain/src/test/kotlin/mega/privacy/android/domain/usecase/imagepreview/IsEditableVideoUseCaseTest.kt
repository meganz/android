package mega.privacy.android.domain.usecase.imagepreview

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.time.Duration

class IsEditableVideoUseCaseTest {

    private val underTest = IsEditableVideoUseCase()

    @ParameterizedTest
    @ValueSource(
        strings = [
            "video/mp4",
            "video/x-m4v",
            "video/quicktime",
            "video/x-matroska",
            "video/webm",
            "video/3gpp",
            "video/3gpp2",
            "video/mp2t",
            "video/mpeg",
            "video/avi",
            "video/x-msvideo",
            "video/msvideo",
            "video/x-flv",
        ]
    )
    fun `test that invoke returns true when file type is a video with a supported mime type`(mimeType: String) {
        val fileType = VideoFileTypeInfo(mimeType, mimeType.substringAfterLast("/"), Duration.parse("10s"))

        assertThat(underTest(fileType)).isTrue()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "video/ogg",
            "video/x-ms-wmv",
            "video/divx",
            "video/x-f4v",
        ]
    )
    fun `test that invoke returns false when video mime type is not supported`(mimeType: String) {
        val fileType = VideoFileTypeInfo(mimeType, mimeType.substringAfterLast("/"), Duration.parse("10s"))

        assertThat(underTest(fileType)).isFalse()
    }

    @Test
    fun `test that invoke returns false when file type is not a video`() {
        val fileType = StaticImageFileTypeInfo("video/mp4", "mp4")

        assertThat(underTest(fileType)).isFalse()
    }
}
