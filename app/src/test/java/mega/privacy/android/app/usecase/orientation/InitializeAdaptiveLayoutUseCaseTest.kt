package mega.privacy.android.app.usecase.orientation

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.orientation.AdaptiveLayoutMemoryManager
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit tests for [InitializeAdaptiveLayoutUseCase]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class InitializeAdaptiveLayoutUseCaseTest {

    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val adaptiveLayoutMemoryManager: AdaptiveLayoutMemoryManager = mock()

    private lateinit var underTest: InitializeAdaptiveLayoutUseCase

    @Before
    fun setUp() {
        underTest = InitializeAdaptiveLayoutUseCase(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            adaptiveLayoutMemoryManager = adaptiveLayoutMemoryManager
        )
    }

    @Test
    fun `test that invoke calls getFeatureFlagValueUseCase and stores result`() = runTest {
        // Given
        val expectedResult = true
        whenever(getFeatureFlagValueUseCase(ApiFeatures.Android16OrientationMigrationEnabled))
            .thenReturn(expectedResult)

        // When
        underTest()

        // Then
        verify(getFeatureFlagValueUseCase).invoke(ApiFeatures.Android16OrientationMigrationEnabled)
        verify(adaptiveLayoutMemoryManager).setAdaptiveLayoutEnabled(expectedResult)
    }

    @Test
    fun `test that invoke stores false when getFeatureFlagValueUseCase returns false`() = runTest {
        // Given
        val expectedResult = false
        whenever(getFeatureFlagValueUseCase(ApiFeatures.Android16OrientationMigrationEnabled))
            .thenReturn(expectedResult)

        // When
        underTest()

        // Then
        verify(getFeatureFlagValueUseCase).invoke(ApiFeatures.Android16OrientationMigrationEnabled)
        verify(adaptiveLayoutMemoryManager).setAdaptiveLayoutEnabled(expectedResult)
    }

    @Test
    fun `test that invoke stores true when getFeatureFlagValueUseCase returns true`() = runTest {
        // Given
        val expectedResult = true
        whenever(getFeatureFlagValueUseCase(ApiFeatures.Android16OrientationMigrationEnabled))
            .thenReturn(expectedResult)

        // When
        underTest()

        // Then
        verify(getFeatureFlagValueUseCase).invoke(ApiFeatures.Android16OrientationMigrationEnabled)
        verify(adaptiveLayoutMemoryManager).setAdaptiveLayoutEnabled(expectedResult)
    }

    @Test
    fun `test that invoke propagates exception from getFeatureFlagValueUseCase`() = runTest {
        // Given
        val exception = RuntimeException("Test exception")
        whenever(getFeatureFlagValueUseCase(ApiFeatures.Android16OrientationMigrationEnabled))
            .thenThrow(exception)

        // When & Then
        try {
            underTest()
            assertThat(false).isTrue() // Should not reach here
        } catch (e: RuntimeException) {
            assertThat(e).isEqualTo(exception)
        }

        // Verify that setAdaptiveLayoutEnabled was not called due to exception
        verify(adaptiveLayoutMemoryManager, org.mockito.kotlin.never()).setAdaptiveLayoutEnabled(org.mockito.kotlin.any())
    }

    @Test
    fun `test that invoke calls getFeatureFlagValueUseCase only once`() = runTest {
        // Given
        whenever(getFeatureFlagValueUseCase(ApiFeatures.Android16OrientationMigrationEnabled))
            .thenReturn(true)

        // When
        underTest()

        // Then
        verify(getFeatureFlagValueUseCase).invoke(ApiFeatures.Android16OrientationMigrationEnabled)
        verify(adaptiveLayoutMemoryManager).setAdaptiveLayoutEnabled(true)
    }
}
