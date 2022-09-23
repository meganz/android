package mega.privacy.android.domain.usecase

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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultToggleAutoAcceptQRLinksTest {
    private lateinit var underTest: ToggleAutoAcceptQRLinks

    private val fetchAutoAcceptQRLinks = mock<FetchAutoAcceptQRLinks>()
    private val settingsRepository = mock<SettingsRepository>()

    @Before
    fun setUp() {
        runBlocking { whenever(settingsRepository.setAutoAcceptQR(any())).thenAnswer { it.arguments[0] } }
        underTest = DefaultToggleAutoAcceptQRLinks(
            fetchAutoAcceptQRLinks = fetchAutoAcceptQRLinks,
            settingsRepository = settingsRepository,
        )
    }


    @Test
    fun `test that if current value is true, it is set to false`() = runTest {
        whenever(fetchAutoAcceptQRLinks()).thenReturn(true)
        assertThat(underTest()).isFalse()

        verify(settingsRepository).setAutoAcceptQR(false)
    }

    @Test
    fun `test that if current value is false, it is set to true`() = runTest {
        whenever(fetchAutoAcceptQRLinks()).thenReturn(false)
        assertThat(underTest()).isTrue()

        verify(settingsRepository).setAutoAcceptQR(true)
    }

    @Test
    fun `test that if current value throws a SettingNotFoundException, it is set to true`() =
        runTest {
            whenever(fetchAutoAcceptQRLinks()).thenAnswer { throw SettingNotFoundException(-1) }
            assertThat(underTest()).isTrue()

            verify(settingsRepository).setAutoAcceptQR(true)
        }

    @Test
    fun `test that if current value throws an unknownException, it is rethrown`() = runTest {
        whenever(fetchAutoAcceptQRLinks()).thenAnswer { throw Exception() }
        assertThrows(Exception::class.java) {
            runBlocking { underTest() }
        }

        verifyNoInteractions(settingsRepository)
    }

}