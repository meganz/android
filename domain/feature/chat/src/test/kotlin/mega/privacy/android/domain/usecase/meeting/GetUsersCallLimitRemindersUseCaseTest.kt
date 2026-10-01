package mega.privacy.android.domain.usecase.meeting

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.meeting.UsersCallLimitReminders
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Test class for [GetUsersCallLimitRemindersUseCaseTest]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetUsersCallLimitRemindersUseCaseTest {

    private lateinit var underTest: GetUsersCallLimitRemindersUseCase
    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetUsersCallLimitRemindersUseCase(
            settingsRepository = settingsRepository,
        )
    }

    @Test
    fun `test that it emits value when invoked `() = runTest {
        val reminders = UsersCallLimitReminders.entries.toTypedArray()
        whenever(settingsRepository.getUsersCallLimitReminders()).thenReturn(reminders.asFlow())


        underTest().test {
            reminders.forEach {
                Truth.assertThat(awaitItem()).isEqualTo(it)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }
}