package mega.privacy.android.app.features

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Feature
import org.junit.jupiter.api.Test

class CloudDriveFeatureTest {

    @Test
    fun `test companion object provides default values`() = runTest {
        val provider = CloudDriveFeature.Companion

        // Test that it returns the correct default value for INCOMING_SHARE_NAME_DUPLICATION_FIX
        val incomingShareResult = provider.isEnabled(CloudDriveFeature.INCOMING_SHARE_NAME_DUPLICATION_FIX)
        assertThat(incomingShareResult).isFalse() // Default value is false

        // Test that it returns null for unknown features
        val unknownFeature = object : Feature {
            override val description: String = "Unknown"
            override val name: String = "Unknown"
        }
        val unknownResult = provider.isEnabled(unknownFeature)
        assertThat(unknownResult).isNull()
    }

    @Test
    fun `test companion object priority`() {
        val provider = CloudDriveFeature.Companion
        assertThat(provider.priority).isEqualTo(
            mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority.Default
        )
    }
}
