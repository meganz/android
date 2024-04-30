package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.meeting.UsersCallLimitReminders
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [SetUsersCallLimitRemindersUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetUsersCallLimitRemindersUseCaseTest {

    private lateinit var underTest: SetUsersCallLimitRemindersUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetUsersCallLimitRemindersUseCase(
            settingsRepository = settingsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository)
    }

    @Test
    fun `test that the enabled reminder is set when invoked`() = runTest {
        val usersCallLimitReminder = UsersCallLimitReminders.Enabled
        underTest(usersCallLimitReminder)

        verify(settingsRepository).setUsersCallLimitReminders(usersCallLimitReminder)
    }

    @Test
    fun `test that the disabled reminder is set when invoked`() = runTest {
        val usersCallLimitReminder = UsersCallLimitReminders.Disabled
        underTest(usersCallLimitReminder)

        verify(settingsRepository).setUsersCallLimitReminders(usersCallLimitReminder)
    }
}