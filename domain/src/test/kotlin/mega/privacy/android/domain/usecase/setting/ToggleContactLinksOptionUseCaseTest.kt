package mega.privacy.android.domain.usecase.setting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.SettingNotFoundException
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ToggleContactLinksOptionUseCaseTest {
    private lateinit var underTest: ToggleContactLinksOptionUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @Before
    fun setUp() {
        runBlocking { whenever(settingsRepository.setContactLinksOption(any())).thenAnswer { it.arguments[0] } }
        underTest = ToggleContactLinksOptionUseCase(
            settingsRepository = settingsRepository,
        )
    }


    @Test
    fun `test that if current value is true, it is set to false`() = runTest {
        whenever(settingsRepository.getContactLinksOption()).thenReturn(true)
        assertThat(underTest()).isFalse()

        verify(settingsRepository).setContactLinksOption(false)
    }

    @Test
    fun `test that if current value is false, it is set to true`() = runTest {
        whenever(settingsRepository.getContactLinksOption()).thenReturn(false)
        assertThat(underTest()).isTrue()

        verify(settingsRepository).setContactLinksOption(true)
    }

    @Test
    fun `test that if current value throws a SettingNotFoundException, it is set to true`() =
        runTest {
            whenever(settingsRepository.getContactLinksOption()).thenAnswer {
                throw SettingNotFoundException(-1)
            }
            assertThat(underTest()).isTrue()

            verify(settingsRepository).setContactLinksOption(true)
        }

    @Test
    fun `test that if current value throws an unknownException, it is rethrown`() = runTest {
        whenever(settingsRepository.getContactLinksOption()).thenAnswer { throw Exception() }
        assertThrows(Exception::class.java) {
            runBlocking { underTest() }
        }
    }

}