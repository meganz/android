package mega.privacy.android.feature.sync.data.mapper.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncRunStateMapperTest {

    private lateinit var underTest: SyncRunStateMapper

    @BeforeEach
    fun setUp() {
        underTest = SyncRunStateMapper()
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("provideRunStates")
    fun `test that invoke maps the sdk run state`(runState: Int, expected: SyncRunState) {
        assertThat(underTest(runState)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(ints = [3, 99])
    fun `test that invoke maps an unknown run state to Disabled`(runState: Int) {
        assertThat(underTest(runState)).isEqualTo(SyncRunState.Disabled)
    }

    private fun provideRunStates() = listOf(
        Arguments.of(0, SyncRunState.Pending),
        Arguments.of(1, SyncRunState.Loading),
        Arguments.of(2, SyncRunState.Running),
        Arguments.of(4, SyncRunState.Suspended),
        Arguments.of(5, SyncRunState.Disabled),
    )
}
