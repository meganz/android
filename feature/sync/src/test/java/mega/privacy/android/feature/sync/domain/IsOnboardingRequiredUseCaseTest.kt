package mega.privacy.android.feature.sync.domain

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.IsOnboardingRequiredUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsOnboardingRequiredUseCaseTest {

    private val syncPreferencesRepository: SyncPreferencesRepository = mock()
    private val syncRepository: SyncRepository = mock()

    private val underTest = IsOnboardingRequiredUseCase(
        syncPreferencesRepository,
        syncRepository
    )

    @AfterEach
    fun resetAndTearDown() {
        reset(
            syncPreferencesRepository,
            syncRepository,
        )
    }

    @Test
    fun `test that if onboarding was previously shown then onboarding is not required`() = runTest {
        whenever(syncPreferencesRepository.getOnboardingShown()).thenReturn(true)
        whenever(syncRepository.getFolderPairs()).thenReturn(listOf())

        val result = underTest()

        assertThat(result).isFalse()
    }

    @Test
    fun `test that if user has established syncs then onboarding is not required`() = runTest {
        val fakeFolderPair = FolderPair(
            1,
            "",
            "",
            RemoteFolder(1, ""),
            SyncStatus.SYNCING
        )
        whenever(syncPreferencesRepository.getOnboardingShown()).thenReturn(false)
        whenever(syncRepository.getFolderPairs()).thenReturn(listOf(fakeFolderPair))

        val result = underTest()

        assertThat(result).isFalse()
    }

    @Test
    fun `test that if user has not seen onboarding and has no syncs then onboarding is required`() =
        runTest {
            whenever(syncPreferencesRepository.getOnboardingShown()).thenReturn(false)
            whenever(syncRepository.getFolderPairs()).thenReturn(listOf())

            val result = underTest()

            assertThat(result).isTrue()
        }
}