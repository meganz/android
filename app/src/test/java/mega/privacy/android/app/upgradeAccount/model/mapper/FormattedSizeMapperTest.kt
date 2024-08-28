package mega.privacy.android.app.upgradeAccount.model.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.FormattedSize
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FormattedSizeMapperTest {
    private val underTest = FormattedSizeMapper()

    private fun provideTestParameters() = Stream.of(
        Arguments.of(400, true, FormattedSize(R.string.label_file_size_giga_byte, "400")),
        Arguments.of(1024, true, FormattedSize(R.string.label_file_size_tera_byte, "1")),
        Arguments.of(
            400,
            false,
            FormattedSize(
                mega.privacy.android.shared.resources.R.string.general_giga_byte_standalone,
                "400"
            )
        ),
        Arguments.of(
            1024,
            false,
            FormattedSize(
                mega.privacy.android.shared.resources.R.string.general_tera_byte_standalone,
                "1"
            )
        )
    )

    @ParameterizedTest(name = "when size is {0} and usePlaceholder is {1} then return {2}")
    @MethodSource("provideTestParameters")
    fun `test that mapper returns correct formatted size and string id`(
        size: Int,
        usePlaceholder: Boolean,
        expectedResult: FormattedSize,
    ) {
        assertThat(
            underTest(
                size = size,
                usePlaceholder = usePlaceholder
            )
        ).isEqualTo(expectedResult)
    }
}