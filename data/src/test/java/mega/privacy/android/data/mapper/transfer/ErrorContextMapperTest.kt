package mega.privacy.android.data.mapper.transfer

import com.google.common.truth.Truth
import nz.mega.sdk.MegaError.ErrorContexts
import nz.mega.sdk.MegaTransfer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ErrorContextMapperTest {
    private lateinit var underTest: ErrorContextMapper

    @BeforeAll
    fun setup() {
        underTest = ErrorContextMapper()
    }

    @ParameterizedTest(name = " when invoked with {0} transfer type: returns {1}")
    @MethodSource("provideParameters")
    fun `test that error context mapper returns correctly`(
        transferInt: Int,
        errorContext: ErrorContexts,
    ) {
        val actual = underTest(transferInt)
        Truth.assertThat(actual).isEqualTo(errorContext)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(MegaTransfer.TYPE_DOWNLOAD, ErrorContexts.API_EC_DOWNLOAD),
        Arguments.of(MegaTransfer.TYPE_UPLOAD, ErrorContexts.API_EC_UPLOAD),
        Arguments.of(-1, ErrorContexts.API_EC_DEFAULT),
    )
}
