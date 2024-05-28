package mega.privacy.android.data.mapper.featureflag

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.featureflag.FlagTypes
import mega.privacy.android.domain.entity.featureflag.GroupFlagTypes
import nz.mega.sdk.MegaFlag
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class FlagMapperTest {
    private lateinit var underTest: FlagMapper

    private val expectedTypeFeature = 2L

    @Before
    fun setUp() {
        underTest =
            FlagMapper(
                flagTypesMapper = FlagTypesMapper(),
            )
    }

    @Test
    fun `test mapping feature received flag disabled`() {
        val flag = getMockFlag(group = 0)
        val actual = underTest(flag)
        Truth.assertThat(actual.type).isEqualTo(FlagTypes.Feature)
        Truth.assertThat(actual.group).isEqualTo(GroupFlagTypes.Disabled)
    }

    @Test
    fun `test mapping feature received flag enabled`() {
        val flag = getMockFlag(group = 2)
        val actual = underTest(flag)
        Truth.assertThat(actual.type).isEqualTo(FlagTypes.Feature)
        Truth.assertThat(actual.group).isEqualTo(GroupFlagTypes.Enabled)
    }

    private fun getMockFlag(
        type: Long = expectedTypeFeature,
        group: Long = 0,
    ): MegaFlag {
        val megaFlag = mock<MegaFlag> {
            on { this.type }.thenReturn(type)
            on { this.group }.thenReturn(group)
        }

        return megaFlag
    }
}