package mega.privacy.android.feature.sync.domain.usecase.sync

import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetSyncWorkerForegroundPreferenceUseCaseTest {

    private lateinit var underTest: SetSyncWorkerForegroundPreferenceUseCase

    private val syncPreferencesRepository = mock<SyncPreferencesRepository>()

    @BeforeAll
    fun setup() {
        underTest = SetSyncWorkerForegroundPreferenceUseCase(
            syncPreferencesRepository = syncPreferencesRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncPreferencesRepository)
    }

    @ParameterizedTest(name = "set should run foreground to: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the sync worker foreground preference is set correctly`(
        shouldRunForeground: Boolean,
    ) = runTest {
        underTest(shouldRunForeground)

        verify(syncPreferencesRepository).setShouldRunForeground(shouldRunForeground)
    }
}
