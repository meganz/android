package mega.privacy.android.app.features

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Feature
import org.junit.jupiter.api.Test

class CloudDriveFeatureTest {

    @Test
    fun `test FAVORITE_MULTIPLE_SELECTION feature flag properties`() {
        val feature = CloudDriveFeature.FAVORITE_MULTIPLE_SELECTION

        assertThat(feature.description).isEqualTo(
            "Allow multiple selection for favorite in Cloud Drive (SAO-2344)"
        )
        assertThat(feature).isInstanceOf(Feature::class.java)
    }

    @Test
    fun `test companion object provides default values`() = runTest {
        val provider = CloudDriveFeature.Companion

        // Test that it returns the correct default value for our feature
        val result = provider.isEnabled(CloudDriveFeature.FAVORITE_MULTIPLE_SELECTION)
        assertThat(result).isTrue() // Default value is true

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
