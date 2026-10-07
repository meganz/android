package mega.privacy.android.domain.featuretoggle

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ApiFeaturesTest {

    @Test
    fun `test that SyncProgress is disabled by default`() = runTest {
        assertThat(ApiFeatures.isEnabled(ApiFeatures.SyncProgress)).isFalse()
    }

    @Test
    fun `test that the SyncProgress experiment name is not used by another feature`() {
        assertThat(
            ApiFeatures.entries.filter {
                it.experimentName == ApiFeatures.SyncProgress.experimentName
            }
        ).containsExactly(ApiFeatures.SyncProgress)
    }
}
