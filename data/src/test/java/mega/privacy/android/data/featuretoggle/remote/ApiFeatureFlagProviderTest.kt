package mega.privacy.android.data.featuretoggle.remote

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.ApiFeature
import mega.privacy.android.domain.entity.featureflag.Flag
import mega.privacy.android.domain.entity.featureflag.GroupFlagTypes
import mega.privacy.android.domain.usecase.featureflag.GetFlagUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiFeatureFlagProviderTest {

    private lateinit var underTest: ApiFeatureFlagProvider

    private val getFlagUseCase = mock<GetFlagUseCase>()

    @BeforeEach
    internal fun setUp() {
        underTest = ApiFeatureFlagProvider(
            getFlagUseCase = getFlagUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(getFlagUseCase)
    }

    @Test
    internal fun `test that null is returned if feature flag is not ApiFeature`() = runTest {
        val feature = mock<Feature> {
            on { name } doReturn "feature"
        }
        Truth.assertThat(underTest.isEnabled(feature)).isNull()
    }

    @Test
    internal fun `test that null is returned if check remote is false`() = runTest {
        val feature = mock<ApiFeature> {
            on { checkRemote } doReturn false
        }
        Truth.assertThat(underTest.isEnabled(feature)).isNull()
    }

    @Test
    internal fun `test that true  is returned when feature flag in enabled`() = runTest {
        val feature = mock<ApiFeature> {
            on { experimentName } doReturn "chmon"
            on { checkRemote } doReturn true
        }
        val flag = mock<Flag> {
            on { group } doReturn GroupFlagTypes.Enabled
        }
        whenever(getFlagUseCase(feature.experimentName)).thenReturn(flag)

        val expected = underTest.isEnabled(feature)
        Truth.assertThat(expected).isTrue()
    }

    @Test
    internal fun `test that false  is returned when feature flag in disabled`() = runTest {
        val feature = mock<ApiFeature> {
            on { experimentName } doReturn "chmon"
            on { checkRemote } doReturn true
        }
        val flag = mock<Flag> {
            on { group } doReturn GroupFlagTypes.Disabled
        }
        whenever(getFlagUseCase(feature.experimentName)).thenReturn(flag)

        val expected = underTest.isEnabled(feature)
        Truth.assertThat(expected).isFalse()
    }
}
