package mega.privacy.android.domain.entity

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaStoreFileTypeTest {

    private lateinit var underTest: MediaStoreFileType

    @Test
    fun `test that IMAGES_INTERNAL is of type image`() {
        underTest = MediaStoreFileType.IMAGES_INTERNAL
        assertThat(underTest.isImageFileType()).isTrue()
    }

    @Test
    fun `test that IMAGES_EXTERNAL is of type image`() {
        underTest = MediaStoreFileType.IMAGES_EXTERNAL
        assertThat(underTest.isImageFileType()).isTrue()
    }

    @Test
    fun `test that VIDEO_INTERNAL is of type video`() {
        underTest = MediaStoreFileType.VIDEO_INTERNAL
        assertThat(underTest.isImageFileType()).isFalse()
    }

    @Test
    fun `test that VIDEO_EXTERNAL is of type video`() {
        underTest = MediaStoreFileType.VIDEO_EXTERNAL
        assertThat(underTest.isImageFileType()).isFalse()
    }


}
