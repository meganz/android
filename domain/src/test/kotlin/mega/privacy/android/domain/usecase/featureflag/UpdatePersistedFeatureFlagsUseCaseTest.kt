package mega.privacy.android.domain.usecase.featureflag

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.repository.FeatureFlagRepository
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class UpdatePersistedFeatureFlagsUseCaseTest {

    private val featureFlagRepository = mock<FeatureFlagRepository> {
        on { getCurrentPersistedSnapshot() } doReturn emptyMap()
    }
    private val managedFeatures: Set<Feature> = setOf(TestFeature.A, TestFeature.B)

    private val underTest = UpdatePersistedFeatureFlagsUseCase(
        featureFlagRepository = featureFlagRepository,
        managedFeatures = managedFeatures,
    )

    @Test
    fun `test that fresh source-priority value wins for each managed feature`() = runTest {
        whenever(featureFlagRepository.getFeatureValue(TestFeature.A, SOURCE_PRIORITIES))
            .thenReturn(true)
        whenever(featureFlagRepository.getFeatureValue(TestFeature.B, SOURCE_PRIORITIES))
            .thenReturn(false)

        underTest()

        assertThat(capturedSnapshot()).containsExactly(
            TestFeature.A, true,
            TestFeature.B, false,
        )
    }

    @Test
    fun `test that previous snapshot value is kept when source returns null`() = runTest {
        whenever(featureFlagRepository.getCurrentPersistedSnapshot())
            .thenReturn(mapOf(TestFeature.A to true))

        underTest()

        assertThat(capturedSnapshot()).containsExactly(
            TestFeature.A, true,   // kept from previous
            TestFeature.B, false,  // no source, no previous, no default → false
        )
    }

    @Test
    fun `test that default-priority value is used when source and previous are absent`() =
        runTest {
            whenever(featureFlagRepository.getFeatureValue(TestFeature.A, DEFAULT_PRIORITIES))
                .thenReturn(true)
            whenever(featureFlagRepository.getFeatureValue(TestFeature.B, DEFAULT_PRIORITIES))
                .thenReturn(false)

            underTest()

            assertThat(capturedSnapshot()).containsExactly(
                TestFeature.A, true,
                TestFeature.B, false,
            )
        }

    @Test
    fun `test that false is used when source, previous, and default are all absent`() = runTest {
        underTest()

        assertThat(capturedSnapshot()).containsExactly(
            TestFeature.A, false,
            TestFeature.B, false,
        )
    }

    @Test
    fun `test that source value wins over previous and default for the same feature`() = runTest {
        whenever(featureFlagRepository.getCurrentPersistedSnapshot())
            .thenReturn(mapOf(TestFeature.A to false))
        whenever(featureFlagRepository.getFeatureValue(TestFeature.A, SOURCE_PRIORITIES))
            .thenReturn(true)
        whenever(featureFlagRepository.getFeatureValue(TestFeature.A, DEFAULT_PRIORITIES))
            .thenReturn(false)

        underTest()

        assertThat(capturedSnapshot()[TestFeature.A]).isTrue()
    }

    @Test
    fun `test that previous wins over default when source is null`() = runTest {
        whenever(featureFlagRepository.getCurrentPersistedSnapshot())
            .thenReturn(mapOf(TestFeature.A to true))
        whenever(featureFlagRepository.getFeatureValue(TestFeature.A, DEFAULT_PRIORITIES))
            .thenReturn(false)

        underTest()

        assertThat(capturedSnapshot()[TestFeature.A]).isTrue()
    }

    private suspend fun capturedSnapshot(): Map<Feature, Boolean> {
        val captor = argumentCaptor<Map<Feature, Boolean>>()
        verify(featureFlagRepository).applySnapshot(captor.capture())
        return captor.firstValue
    }

    private companion object {
        val SOURCE_PRIORITIES = setOf(
            FeatureFlagValuePriority.ConfigurationFile,
            FeatureFlagValuePriority.BuildTimeOverride,
            FeatureFlagValuePriority.RemoteToggled,
        )
        val DEFAULT_PRIORITIES = setOf(FeatureFlagValuePriority.Default)
    }

    private enum class TestFeature(override val description: String) : Feature {
        A("A"),
        B("B"),
    }
}
