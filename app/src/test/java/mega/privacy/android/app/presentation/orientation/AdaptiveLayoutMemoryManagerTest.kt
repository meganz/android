package mega.privacy.android.app.presentation.orientation

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AdaptiveLayoutMemoryManager]
 */
class AdaptiveLayoutMemoryManagerTest {

    private lateinit var underTest: AdaptiveLayoutMemoryManager

    @Before
    fun setUp() {
        underTest = AdaptiveLayoutMemoryManager()
    }

    @Test
    fun `test that initial state is null`() {
        // When
        val result = underTest.getAdaptiveLayoutEnabled()

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `test that setAdaptiveLayoutEnabled stores true value`() {
        // When
        underTest.setAdaptiveLayoutEnabled(true)

        // Then
        val result = underTest.getAdaptiveLayoutEnabled()
        assertThat(result).isTrue()
    }

    @Test
    fun `test that setAdaptiveLayoutEnabled stores false value`() {
        // When
        underTest.setAdaptiveLayoutEnabled(false)

        // Then
        val result = underTest.getAdaptiveLayoutEnabled()
        assertThat(result).isFalse()
    }

    @Test
    fun `test that setAdaptiveLayoutEnabled overwrites previous value`() {
        // Given
        underTest.setAdaptiveLayoutEnabled(true)

        // When
        underTest.setAdaptiveLayoutEnabled(false)

        // Then
        val result = underTest.getAdaptiveLayoutEnabled()
        assertThat(result).isFalse()
    }

    @Test
    fun `test that clearAdaptiveLayoutState resets to null`() {
        // Given
        underTest.setAdaptiveLayoutEnabled(true)

        // When
        underTest.clearAdaptiveLayoutState()

        // Then
        val result = underTest.getAdaptiveLayoutEnabled()
        assertThat(result).isNull()
    }

    @Test
    fun `test that clearAdaptiveLayoutState works when already null`() {
        // When
        underTest.clearAdaptiveLayoutState()

        // Then
        val result = underTest.getAdaptiveLayoutEnabled()
        assertThat(result).isNull()
    }

    @Test
    fun `test that multiple set operations work correctly`() {
        // When & Then
        underTest.setAdaptiveLayoutEnabled(true)
        assertThat(underTest.getAdaptiveLayoutEnabled()).isTrue()

        underTest.setAdaptiveLayoutEnabled(false)
        assertThat(underTest.getAdaptiveLayoutEnabled()).isFalse()

        underTest.setAdaptiveLayoutEnabled(true)
        assertThat(underTest.getAdaptiveLayoutEnabled()).isTrue()
    }
}
