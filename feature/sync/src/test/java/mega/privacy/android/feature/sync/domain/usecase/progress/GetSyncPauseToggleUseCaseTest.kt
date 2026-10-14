package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseToggle
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetSyncPauseToggleUseCaseTest {

    private lateinit var underTest: GetSyncPauseToggleUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetSyncPauseToggleUseCase()
    }

    @Test
    fun `test that invoke returns pause all when there are no syncs`() {
        assertThat(underTest(emptyList(), emptySet())).isEqualTo(SyncPauseToggle.PauseAll)
    }

    @Test
    fun `test that invoke returns pause all when every sync has an error`() {
        val syncs = listOf(
            sync(1, SyncRunState.Suspended, hasError = true),
            sync(2, SyncRunState.Suspended, hasError = true),
        )

        assertThat(underTest(syncs, setOf(1L, 2L))).isEqualTo(SyncPauseToggle.PauseAll)
    }

    @ParameterizedTest
    @EnumSource(value = SyncRunState::class, names = ["Pending", "Loading", "Running"])
    fun `test that invoke returns pause all when a sync without an error is active`(
        runState: SyncRunState,
    ) {
        val syncs = listOf(sync(1, runState), sync(2, SyncRunState.Suspended))

        assertThat(underTest(syncs, setOf(1L, 2L))).isEqualTo(SyncPauseToggle.PauseAll)
    }

    @Test
    fun `test that invoke returns resume all when every sync without an error is suspended and paused by the user`() {
        val syncs = listOf(sync(1, SyncRunState.Suspended), sync(2, SyncRunState.Suspended))

        assertThat(underTest(syncs, setOf(1L, 2L))).isEqualTo(SyncPauseToggle.ResumeAll)
    }

    @Test
    fun `test that invoke returns pause all when a suspended sync is not paused by the user`() {
        val syncs = listOf(sync(1, SyncRunState.Suspended), sync(2, SyncRunState.Suspended))

        assertThat(underTest(syncs, setOf(1L))).isEqualTo(SyncPauseToggle.PauseAll)
    }

    @Test
    fun `test that invoke returns pause all when a sync paused by the user is running`() {
        val syncs = listOf(sync(1, SyncRunState.Running))

        assertThat(underTest(syncs, setOf(1L))).isEqualTo(SyncPauseToggle.PauseAll)
    }

    @Test
    fun `test that invoke ignores syncs with an error`() {
        val syncs = listOf(
            sync(1, SyncRunState.Suspended),
            sync(2, SyncRunState.Running, hasError = true),
        )

        assertThat(underTest(syncs, setOf(1L))).isEqualTo(SyncPauseToggle.ResumeAll)
    }

    @Test
    fun `test that invoke treats a disabled sync without an error as not active`() {
        val syncs = listOf(sync(1, SyncRunState.Disabled), sync(2, SyncRunState.Suspended))

        assertThat(underTest(syncs, setOf(1L, 2L))).isEqualTo(SyncPauseToggle.ResumeAll)
    }

    private fun sync(id: Long, runState: SyncRunState, hasError: Boolean = false) = SyncWorkState(
        syncId = id,
        runState = runState,
        isStorageOverQuota = false,
        stats = null,
        runningSinceMillis = null,
        hasError = hasError,
    )
}
