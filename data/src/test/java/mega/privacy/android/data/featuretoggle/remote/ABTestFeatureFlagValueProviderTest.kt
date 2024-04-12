package mega.privacy.android.data.featuretoggle.remote

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.entity.featureflag.ABTestFeature
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class ABTestFeatureFlagValueProviderTest {
    private lateinit var underTest: ABTestFeatureFlagValueProvider

    private val megaApiGateway = mock<MegaApiGateway>()

    @BeforeEach
    internal fun setUp() {
        underTest = ABTestFeatureFlagValueProvider(
            ioDispatcher = UnconfinedTestDispatcher(),
            megaApiGateway = megaApiGateway,
        )
    }

    @Test
    internal fun `test that null is returned if check remote is false`() = runTest {
        val feature = object : ABTestFeature {
            override val experimentName: String = "testExperimentName"
            override val checkRemote: Boolean = false
            override val name: String = "testFeatureName"
            override val description: String = "description"
        }

        assertThat(underTest.isEnabled(feature)).isNull()
    }

    @Test
    internal fun `test that experiment name is passed to gateway`() = runTest {
        Mockito.reset(megaApiGateway)

        val expected = "testExperimentName"
        val feature = object : ABTestFeature {
            override val experimentName: String = expected
            override val checkRemote: Boolean = true
            override val name: String = "testFeatureName"
            override val description: String = "description"

        }

        underTest.isEnabled(feature)

        verify(megaApiGateway).getABTestValue(expected)

    }
}