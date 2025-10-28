package mega.privacy.android.app.usecase.orientation

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.orientation.AdaptiveLayoutMemoryManager
import mega.privacy.android.app.usecase.orientation.ShouldShowAdaptiveLayoutUseCase
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

    private val shouldShowAdaptiveLayoutUseCase: ShouldShowAdaptiveLayoutUseCase = mock()
    private val adaptiveLayoutMemoryManager: AdaptiveLayoutMemoryManager = mock()

    private lateinit var underTest: InitializeAdaptiveLayoutUseCase

    @Before
    fun setUp() {
        underTest = InitializeAdaptiveLayoutUseCase(
            shouldShowAdaptiveLayoutUseCase = shouldShowAdaptiveLayoutUseCase,
            adaptiveLayoutMemoryManager = adaptiveLayoutMemoryManager
        )
    }

    @Test
    fun `test that invoke calls shouldShowAdaptiveLayoutUseCase and stores result`() = runTest {
        // Given
        val expectedResult = true
        whenever(shouldShowAdaptiveLayoutUseCase()).thenReturn(expectedResult)

        // When
        underTest()

        // Then
        verify(shouldShowAdaptiveLayoutUseCase).invoke()
        verify(adaptiveLayoutMemoryManager).setAdaptiveLayoutEnabled(expectedResult)
    }

    @Test
    fun `test that invoke stores false when shouldShowAdaptiveLayoutUseCase returns false`() = runTest {
        // Given
        val expectedResult = false
        whenever(shouldShowAdaptiveLayoutUseCase()).thenReturn(expectedResult)

        // When
        underTest()

        // Then
        verify(shouldShowAdaptiveLayoutUseCase).invoke()
        verify(adaptiveLayoutMemoryManager).setAdaptiveLayoutEnabled(expectedResult)
    }

    @Test
    fun `test that invoke stores true when shouldShowAdaptiveLayoutUseCase returns true`() = runTest {
        // Given
        val expectedResult = true
        whenever(shouldShowAdaptiveLayoutUseCase()).thenReturn(expectedResult)

        // When
        underTest()

        // Then
        verify(shouldShowAdaptiveLayoutUseCase).invoke()
        verify(adaptiveLayoutMemoryManager).setAdaptiveLayoutEnabled(expectedResult)
    }

    @Test
    fun `test that invoke propagates exception from shouldShowAdaptiveLayoutUseCase`() = runTest {
        // Given
        val exception = RuntimeException("Test exception")
        whenever(shouldShowAdaptiveLayoutUseCase()).thenThrow(exception)

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
    fun `test that invoke calls shouldShowAdaptiveLayoutUseCase only once`() = runTest {
        // Given
        whenever(shouldShowAdaptiveLayoutUseCase()).thenReturn(true)

        // When
        underTest()

        // Then
        verify(shouldShowAdaptiveLayoutUseCase).invoke()
        verify(adaptiveLayoutMemoryManager).setAdaptiveLayoutEnabled(true)
    }
}
