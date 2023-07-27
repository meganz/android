package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem
import mega.privacy.android.domain.repository.RemotePreferencesRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetMeetingTooltipsUseCaseTest {
    private lateinit var underTest: GetMeetingTooltipsUseCase

    private val remotePreferencesRepository = mock<RemotePreferencesRepository>()

    @Before
    fun setUp() {
        underTest = GetMeetingTooltipsUseCase(remotePreferencesRepository)
    }

    @Test
    fun `invoke should return meeting tooltip preference`() = runTest {
        val item = MeetingTooltipItem.CREATE
        whenever(remotePreferencesRepository.getMeetingTooltipPreference()).thenReturn(item)

        val result = underTest.invoke()

        assertThat(result).isEqualTo(item)
    }
}