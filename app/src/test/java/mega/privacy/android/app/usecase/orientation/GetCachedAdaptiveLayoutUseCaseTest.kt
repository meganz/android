package mega.privacy.android.app.usecase.orientation

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.orientation.AdaptiveLayoutMemoryManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit tests for [GetCachedAdaptiveLayoutUseCase]
 */
@RunWith(MockitoJUnitRunner::class)
class GetCachedAdaptiveLayoutUseCaseTest {

    private val adaptiveLayoutMemoryManager: AdaptiveLayoutMemoryManager = mock()

    private lateinit var underTest: GetCachedAdaptiveLayoutUseCase

    @Before
    fun setUp() {
        underTest = GetCachedAdaptiveLayoutUseCase(
            adaptiveLayoutMemoryManager = adaptiveLayoutMemoryManager
        )
    }

    @Test
    fun `test that invoke returns true when memory manager returns true`() {
        // Given
        whenever(adaptiveLayoutMemoryManager.getAdaptiveLayoutEnabled()).thenReturn(true)

        // When
        val result = underTest()

        // Then
        assertThat(result).isTrue()
        verify(adaptiveLayoutMemoryManager).getAdaptiveLayoutEnabled()
    }

    @Test
    fun `test that invoke returns false when memory manager returns false`() {
        // Given
        whenever(adaptiveLayoutMemoryManager.getAdaptiveLayoutEnabled()).thenReturn(false)

        // When
        val result = underTest()

        // Then
        assertThat(result).isFalse()
        verify(adaptiveLayoutMemoryManager).getAdaptiveLayoutEnabled()
    }

    @Test
    fun `test that invoke returns false when memory manager returns null`() {
        // Given
        whenever(adaptiveLayoutMemoryManager.getAdaptiveLayoutEnabled()).thenReturn(null)

        // When
        val result = underTest()

        // Then
        assertThat(result).isFalse()
        verify(adaptiveLayoutMemoryManager).getAdaptiveLayoutEnabled()
    }

    @Test
    fun `test that invoke calls memory manager only once`() {
        // Given
        whenever(adaptiveLayoutMemoryManager.getAdaptiveLayoutEnabled()).thenReturn(true)

        // When
        underTest()

        // Then
        verify(adaptiveLayoutMemoryManager).getAdaptiveLayoutEnabled()
    }

    @Test
    fun `test that invoke returns correct value for multiple calls`() {
        // Given
        whenever(adaptiveLayoutMemoryManager.getAdaptiveLayoutEnabled()).thenReturn(true)

        // When
        val result1 = underTest()
        val result2 = underTest()

        // Then
        assertThat(result1).isTrue()
        assertThat(result2).isTrue()
        verify(adaptiveLayoutMemoryManager, org.mockito.kotlin.times(2)).getAdaptiveLayoutEnabled()
    }

    @Test
    fun `test that invoke handles different return values correctly`() {
        // Given
        whenever(adaptiveLayoutMemoryManager.getAdaptiveLayoutEnabled()).thenReturn(true)

        // When
        val result1 = underTest()

        // Then
        assertThat(result1).isTrue()

        // Given
        whenever(adaptiveLayoutMemoryManager.getAdaptiveLayoutEnabled()).thenReturn(false)

        // When
        val result2 = underTest()

        // Then
        assertThat(result2).isFalse()

        // Given
        whenever(adaptiveLayoutMemoryManager.getAdaptiveLayoutEnabled()).thenReturn(null)

        // When
        val result3 = underTest()

        // Then
        assertThat(result3).isFalse()
    }
}
