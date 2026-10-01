package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem
import mega.privacy.android.domain.repository.RemotePreferencesRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class SetNextMeetingTooltipUseCaseTest {
    private lateinit var underTest: SetNextMeetingTooltipUseCase

    private val remotePreferencesRepository = mock<RemotePreferencesRepository>()

    @Before
    fun setUp() {
        underTest = SetNextMeetingTooltipUseCase(remotePreferencesRepository)
    }

    @Test
    fun `test that invoke sets next meeting tooltip preference`() =
        runTest {
            val item = MeetingTooltipItem.RECURRING

            underTest.invoke(item)

            verify(remotePreferencesRepository).setMeetingTooltipPreference(item)
        }
}
