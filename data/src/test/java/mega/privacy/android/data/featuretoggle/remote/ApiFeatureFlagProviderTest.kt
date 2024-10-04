package mega.privacy.android.data.featuretoggle.remote

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.featureflag.FlagMapper
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.ApiFeature
import mega.privacy.android.domain.entity.featureflag.Flag
import mega.privacy.android.domain.entity.featureflag.GroupFlagTypes
import nz.mega.sdk.MegaFlag
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiFeatureFlagProviderTest {

    private lateinit var underTest: ApiFeatureFlagProvider

    private val megaApiGateway = mock<MegaApiGateway>()
    private val flagMapper = mock<FlagMapper>()

    @BeforeEach
    internal fun setUp() {
        underTest = ApiFeatureFlagProvider(
            ioDispatcher = UnconfinedTestDispatcher(),
            megaApiGateway = megaApiGateway,
            flagMapper = flagMapper
        )
    }

    @Test
    internal fun `test that null is returned if feature flag is not ApiFeature`() = runTest {
        val feature = mock<Feature> {
            on { name } doReturn "feature"
        }
        assertThat(underTest.isEnabled(feature)).isNull()
    }

    @Test
    internal fun `test that null is returned if check remote is false`() = runTest {
        val feature = mock<ApiFeature> {
            on { checkRemote } doReturn false
        }
        assertThat(underTest.isEnabled(feature)).isNull()
    }

    @Test
    internal fun `test that true  is returned when feature flag in enabled`() = runTest {
        val feature = mock<ApiFeature> {
            on { experimentName } doReturn "chmon"
            on { checkRemote } doReturn true
            on { mapValue(GroupFlagTypes.Enabled) } doReturn true
        }
        val megaFlag = mock<MegaFlag> {
            on { group } doReturn 1L
        }
        val flag = mock<Flag> {
            on { group } doReturn GroupFlagTypes.Enabled
        }
        whenever(megaApiGateway.getFlag(feature.experimentName, true)).thenReturn(megaFlag)
        whenever(flagMapper(megaFlag)).thenReturn(flag)


        val expected = underTest.isEnabled(feature)
        assertThat(expected).isTrue()
    }

    @Test
    internal fun `test that false  is returned when feature flag in disabled`() = runTest {
        val feature = mock<ApiFeature> {
            on { experimentName } doReturn "chmon"
            on { checkRemote } doReturn true
            on { mapValue(GroupFlagTypes.Disabled) } doReturn false
        }
        val megaFlag = mock<MegaFlag> {
            on { group } doReturn 0L
        }
        val flag = mock<Flag> {
            on { group } doReturn GroupFlagTypes.Disabled
        }

        whenever(megaApiGateway.getFlag(feature.experimentName, true)).thenReturn(megaFlag)
        whenever(flagMapper(megaFlag)).thenReturn(flag)

        val expected = underTest.isEnabled(feature)
        assertThat(expected).isFalse()
    }
}
