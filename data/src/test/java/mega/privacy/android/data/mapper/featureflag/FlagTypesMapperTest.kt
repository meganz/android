package mega.privacy.android.data.mapper.featureflag

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.featureflag.FlagTypes
import nz.mega.sdk.MegaFlag
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FlagTypesMapperTest {
    private val underTest = FlagTypesMapper()

    @ParameterizedTest(name = "test that mega account type from SDK {0} is mapped correctly to Flag type {1}")
    @MethodSource("provideParameters")
    fun `test flag type mapped correctly`(type: Int, expected: FlagTypes) {
        val actual = underTest(type)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters() = listOf(
        arrayOf(MegaFlag.FLAG_TYPE_FEATURE, FlagTypes.Feature),
        arrayOf(MegaFlag.FLAG_TYPE_AB_TEST, FlagTypes.ABTest),
        arrayOf(MegaFlag.FLAG_TYPE_INVALID, FlagTypes.Invalid),
        arrayOf(-1, FlagTypes.Invalid)
    )
}