package mega.privacy.android.data.mapper.transfer

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.transfer.TransferAppData
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullAndEmptySource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TransferAppDataStringMapperTest {
    private lateinit var underTest: TransferAppDataStringMapper

    @BeforeAll
    fun setup() {
        underTest = TransferAppDataStringMapper()
    }

    @ParameterizedTest
    @NullAndEmptySource
    fun `test that a null and an empty list are mapped to null`(
        transferAppDataList: List<TransferAppData>?,
    ) {
        Truth.assertThat(underTest(transferAppDataList)).isNull()
    }

    @ParameterizedTest(name = "Input parameters: {1} expected result: {0}")
    @MethodSource("provideSingleParameters")
    fun `test that the expected string is returned when mapping a single TransferAppData`(
        expected: String,
        transferAppDataList: List<TransferAppData>,
    ) {
        Truth.assertThat(underTest(transferAppDataList)).isEqualTo(expected)
    }

    @Test
    fun `test that the expected string is returned when mapping a list of parameters`() {
        val (expected, transferAppDataList) = provideJoinedParameters()
        Truth.assertThat(underTest(transferAppDataList)).isEqualTo(expected)
    }

    private fun provideSingleParameters() = TransferAppDataMapperTest.correctParameters.map {
        Arguments.of(it.first, it.second)
    }

    private fun provideJoinedParameters() =
        TransferAppDataMapperTest.correctParameters.reduce { acc: Pair<String, List<TransferAppData>>, pair: Pair<String, List<TransferAppData>> ->
            acc.first + TransferAppDataMapper.APP_DATA_SEPARATOR + pair.first to acc.second + pair.second
        }


}