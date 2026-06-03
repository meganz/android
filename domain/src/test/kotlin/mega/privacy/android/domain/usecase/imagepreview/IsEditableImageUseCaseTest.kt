package mega.privacy.android.domain.usecase.imagepreview

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class IsEditableImageUseCaseTest {

    private val underTest = IsEditableImageUseCase()

    @ParameterizedTest
    @ValueSource(
        strings = [
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/bmp",
            "image/x-ms-bmp",
            "image/heif",
        ]
    )
    fun `test that invoke returns true when mime type is a supported editable image`(mimeType: String) {
        val fileType = StaticImageFileTypeInfo(mimeType, mimeType.substringAfterLast("/"))

        assertThat(underTest(fileType)).isTrue()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "image/webp",
            "image/gif",
            "image/svg+xml",
            "video/mp4",
        ]
    )
    fun `test that invoke returns false when mime type is not a supported editable image`(mimeType: String) {
        val fileType = StaticImageFileTypeInfo(mimeType, mimeType.substringAfterLast("/"))

        assertThat(underTest(fileType)).isFalse()
    }
}
