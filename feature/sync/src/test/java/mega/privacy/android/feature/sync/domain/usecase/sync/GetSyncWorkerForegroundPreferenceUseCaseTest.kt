package mega.privacy.android.feature.sync.domain.usecase.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetSyncWorkerForegroundPreferenceUseCaseTest {

    private lateinit var underTest: GetSyncWorkerForegroundPreferenceUseCase

    private val syncPreferencesRepository = mock<SyncPreferencesRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetSyncWorkerForegroundPreferenceUseCase(
            syncPreferencesRepository = syncPreferencesRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncPreferencesRepository)
    }

    @ParameterizedTest(name = "should run foreground: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the sync worker foreground preference is retrieved correctly`(
        shouldRunForeground: Boolean,
    ) = runTest {
        whenever(syncPreferencesRepository.getShouldRunForeground()).thenReturn(shouldRunForeground)

        val result = underTest()

        assertThat(result).isEqualTo(shouldRunForeground)
    }

    @Test
    fun `test that the sync worker foreground preference returns false when not set`() = runTest {
        whenever(syncPreferencesRepository.getShouldRunForeground()).thenReturn(false)

        val result = underTest()

        assertThat(result).isFalse()
    }
}
