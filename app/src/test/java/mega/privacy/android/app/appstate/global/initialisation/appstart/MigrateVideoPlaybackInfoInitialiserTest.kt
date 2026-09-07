package mega.privacy.android.app.appstate.global.initialisation.appstart

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.MigrateVideoPlaybackInfosUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigrateVideoPlaybackInfoInitialiserTest {

    private lateinit var underTest: MigrateVideoPlaybackInfoInitialiser

    private val migrateVideoPlaybackInfosUseCase = mock<MigrateVideoPlaybackInfosUseCase>()

    @BeforeEach
    fun setUp() {
        reset(migrateVideoPlaybackInfosUseCase)
        underTest = MigrateVideoPlaybackInfoInitialiser(migrateVideoPlaybackInfosUseCase)
    }

    @Test
    fun `test that invoke runs the migration`() = runTest {
        underTest()

        verify(migrateVideoPlaybackInfosUseCase).invoke()
    }

    @Test
    fun `test that invoke does not propagate a failed migration`() = runTest {
        migrateVideoPlaybackInfosUseCase.stub {
            onBlocking { invoke() } doThrow RuntimeException("migration failed")
        }

        underTest()
    }
}
