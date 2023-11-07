package mega.privacy.android.data.mapper.camerauploads

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [UploadOptionMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadOptionMapperTest {
    private lateinit var underTest: UploadOptionMapper

    @BeforeAll
    fun setUp() {
        underTest = UploadOptionMapper()
    }

    @ParameterizedTest(name = "when input is {0}, mapped to {1}")
    @MethodSource("provideParams")
    fun `test that UploadOption can be mapped correctly`(state: Int?, uploadOption: UploadOption?) {
        assertThat(underTest(state)).isEqualTo(uploadOption)
    }

    private fun provideParams() = Stream.of(
        Arguments.of(1004, null),
        Arguments.of(1001, UploadOption.PHOTOS),
        Arguments.of(1002, UploadOption.VIDEOS),
        Arguments.of(1003, UploadOption.PHOTOS_AND_VIDEOS),
        Arguments.of(null, null),
    )
}
