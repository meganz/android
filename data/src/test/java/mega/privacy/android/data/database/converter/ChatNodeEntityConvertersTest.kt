package mega.privacy.android.data.database.converter

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatNodeEntityConvertersTest {
    private val underTest = ChatNodeEntityConverters()

    @ParameterizedTest
    @MethodSource("provideParameters")
    internal fun `test that convertFromFileTypeInfo returns a string with its properties separated by commas`(
        fileTypeInfo: FileTypeInfo,
        expected: String,
    ) {
        val actual = underTest.convertFromFileTypeInfo(fileTypeInfo)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    internal fun `test that convertToFileTypeInfo returns a file type info with correct values and type`(
        expected: FileTypeInfo,
        string: String,
    ) {
        val actual = underTest.convertToFileTypeInfo(string)
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters() = listOf(
        Arguments.of(
            StaticImageFileTypeInfo("image/jpeg", "jpg"),
            "image/jpeg,jpg,0"
        ),
        Arguments.of(
            AudioFileTypeInfo(
                "audio/wave", "wav",
                123.toDuration(DurationUnit.SECONDS)
            ),
            "audio/wave,wav,123"
        ),
        Arguments.of(
            VideoFileTypeInfo(
                "video/mpeg4", "mpg", 456.toDuration(DurationUnit.SECONDS)
            ),
            "video/mpeg4,mpg,456"
        )
    )
}