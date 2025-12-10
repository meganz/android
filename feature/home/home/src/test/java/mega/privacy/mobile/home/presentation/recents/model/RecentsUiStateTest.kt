package mega.privacy.mobile.home.presentation.recents.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecentsUiStateTest {

    @Test
    fun `test that excludeSensitives is false when hidden nodes disabled`() {
        val state = RecentsUiState(
            isHiddenNodesEnabled = false,
            showHiddenNodes = false
        )

        assertThat(state.excludeSensitives).isFalse()
    }

    @Test
    fun `test that excludeSensitives is false when hidden nodes enabled but showHiddenNodes is true`() {
        val state = RecentsUiState(
            isHiddenNodesEnabled = true,
            showHiddenNodes = true
        )

        assertThat(state.excludeSensitives).isFalse()
    }

    @Test
    fun `test that excludeSensitives is true when hidden nodes enabled and showHiddenNodes is false`() {
        val state = RecentsUiState(
            isHiddenNodesEnabled = true,
            showHiddenNodes = false
        )

        assertThat(state.excludeSensitives).isTrue()
    }

    @Test
    fun `test that isLoading is true when isNodesLoading is true`() {
        val state = RecentsUiState(
            isNodesLoading = true,
            isHiddenNodeSettingsLoading = false
        )

        assertThat(state.isLoading).isTrue()
    }

    @Test
    fun `test that isLoading is true when isHiddenNodeSettingsLoading is true`() {
        val state = RecentsUiState(
            isNodesLoading = false,
            isHiddenNodeSettingsLoading = true
        )

        assertThat(state.isLoading).isTrue()
    }

    @Test
    fun `test that isLoading is true when both loading flags are true`() {
        val state = RecentsUiState(
            isNodesLoading = true,
            isHiddenNodeSettingsLoading = true
        )

        assertThat(state.isLoading).isTrue()
    }

    @Test
    fun `test that isLoading is false when both loading flags are false`() {
        val state = RecentsUiState(
            isNodesLoading = false,
            isHiddenNodeSettingsLoading = false
        )

        assertThat(state.isLoading).isFalse()
    }
}

