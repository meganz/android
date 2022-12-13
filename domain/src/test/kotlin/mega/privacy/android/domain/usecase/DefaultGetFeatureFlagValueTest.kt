package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FeatureFlagRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetFeatureFlagValueTest {
    private lateinit var underTest: GetFeatureFlagValue

    private val featureFlagRepository = mock<FeatureFlagRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetFeatureFlagValue(featureFlagRepository = featureFlagRepository)
    }

    @Test
    fun `test that false is returned if no value found`() = runTest{
        featureFlagRepository.stub {
            onBlocking { getFeatureValue(any()) }.thenReturn(null)
        }
        assertThat(underTest(mock())).isFalse()
    }

    @Test
    fun `test that value is returned if found`() = runTest{
        featureFlagRepository.stub {
            onBlocking { getFeatureValue(any()) }.thenReturn(true)
        }
        assertThat(underTest(mock())).isTrue()
    }
}