package mega.privacy.android.shared.ads.rewarded

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RewardedAdGateViewModelTest {

    private lateinit var underTest: RewardedAdGateViewModel

    @BeforeEach
    fun setUp() {
        underTest = RewardedAdGateViewModel()
    }

    @Test
    fun `test that initial state has no dialog`() {
        val state = underTest.uiState.value
        assertThat(state.showDialog).isFalse()
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `test that showDialog sets showDialog true and clears error`() {
        underTest.setError("some error")

        underTest.showDialog()

        assertThat(underTest.uiState.value.showDialog).isTrue()
        assertThat(underTest.uiState.value.errorMessage).isNull()
    }

    @Test
    fun `test that dismiss resets all state`() {
        underTest.showDialog()
        underTest.setLoading()

        underTest.dismiss()

        val state = underTest.uiState.value
        assertThat(state.showDialog).isFalse()
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `test that setLoading sets isLoading true and clears error`() {
        underTest.setError("error")

        underTest.setLoading()

        assertThat(underTest.uiState.value.isLoading).isTrue()
        assertThat(underTest.uiState.value.errorMessage).isNull()
    }

    @Test
    fun `test that setLoadingComplete sets isLoading false`() {
        underTest.setLoading()

        underTest.setLoadingComplete()

        assertThat(underTest.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `test that setError sets error message and clears loading`() {
        underTest.setLoading()

        underTest.setError("Ad failed to load")

        assertThat(underTest.uiState.value.isLoading).isFalse()
        assertThat(underTest.uiState.value.errorMessage).isEqualTo("Ad failed to load")
    }
}
