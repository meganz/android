package mega.privacy.android.data.featuretoggle.remote

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.RemoteConfigGateway
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.FirebaseABTestFeature
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseFeatureFlagValueProviderTest {
    private lateinit var underTest: FirebaseFeatureFlagValueProvider

    private val remoteConfigGateway = mock<RemoteConfigGateway>()

    private val firebaseFeature = object : FirebaseABTestFeature {
        override val remoteConfigKey: String = "testRemoteConfigKey"
        override val name: String = "testFeatureName"
        override val description: String = "description"
    }

    @BeforeEach
    internal fun setUp() {
        underTest = FirebaseFeatureFlagValueProvider(
            ioDispatcher = UnconfinedTestDispatcher(),
            remoteConfigGateway = remoteConfigGateway,
        )
    }

    @Test
    internal fun `test that isEnabled returns null when feature is not a firebase feature`() =
        runTest {
            val feature = object : Feature {
                override val name: String = "testFeatureName"
                override val description: String = "description"
            }

            assertThat(underTest.isEnabled(feature)).isNull()
        }

    @Test
    internal fun `test that remote config key is passed to gateway`() = runTest {
        underTest.isEnabled(firebaseFeature)

        verify(remoteConfigGateway).getBoolean(firebaseFeature.remoteConfigKey)
    }

    @Test
    internal fun `test that isEnabled returns gateway value when feature is a firebase feature`() =
        runTest {
            whenever(remoteConfigGateway.getBoolean(firebaseFeature.remoteConfigKey))
                .thenReturn(true)

            assertThat(underTest.isEnabled(firebaseFeature)).isTrue()
        }

    @Test
    internal fun `test that isEnabled returns null when no remote value exists`() = runTest {
        whenever(remoteConfigGateway.getBoolean(firebaseFeature.remoteConfigKey))
            .thenReturn(null)

        assertThat(underTest.isEnabled(firebaseFeature)).isNull()
    }
}
