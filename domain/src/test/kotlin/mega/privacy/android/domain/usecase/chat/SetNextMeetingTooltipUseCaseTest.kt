package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem
import mega.privacy.android.domain.repository.RemotePreferencesRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@OptIn(ExperimentalCoroutinesApi::class)
class SetNextMeetingTooltipUseCaseTest {
    private lateinit var underTest: SetNextMeetingTooltipUseCase

    private val remotePreferencesRepository = mock<RemotePreferencesRepository>()

    @Before
    fun setUp() {
        underTest = SetNextMeetingTooltipUseCase(remotePreferencesRepository)
    }

    @Test
    fun `invoke should set next meeting tooltip preference when current item is not the last one`() =
        runTest {
            val currentItem = MeetingTooltipItem.CREATE
            val nextItem =
                MeetingTooltipItem.values()[MeetingTooltipItem.values().indexOf(currentItem) + 1]

            underTest.invoke(currentItem)

            verify(remotePreferencesRepository).setMeetingTooltipPreference(nextItem)
            verifyNoMoreInteractions(remotePreferencesRepository)
        }

    @Test
    fun `invoke should set current meeting tooltip preference when current item is the last one`() =
        runTest {
            val currentItem = MeetingTooltipItem.values().last()

            underTest.invoke(currentItem)

            verify(remotePreferencesRepository).setMeetingTooltipPreference(currentItem)
            verifyNoMoreInteractions(remotePreferencesRepository)
        }
}
