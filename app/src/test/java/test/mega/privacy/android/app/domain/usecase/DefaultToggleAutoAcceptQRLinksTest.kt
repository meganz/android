package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.exception.SettingNotFoundException
import mega.privacy.android.app.domain.repository.SettingsRepository
import mega.privacy.android.app.domain.usecase.DefaultToggleAutoAcceptQRLinks
import mega.privacy.android.app.domain.usecase.FetchAutoAcceptQRLinks
import mega.privacy.android.app.domain.usecase.ToggleAutoAcceptQRLinks
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

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
    fun `test that if current value is true, it is set to false`() = runTest{
        whenever(fetchAutoAcceptQRLinks()).thenReturn(true)
        assertThat(underTest()).isFalse()

        verify(settingsRepository).setAutoAcceptQR(false)
    }

    @Test
    fun `test that if current value is false, it is set to true`() = runTest{
        whenever(fetchAutoAcceptQRLinks()).thenReturn(false)
        assertThat(underTest()).isTrue()

        verify(settingsRepository).setAutoAcceptQR(true)
    }

    @Test
    fun `test that if current value throws a SettingNotFoundException, it is set to true`() = runTest{
        whenever(fetchAutoAcceptQRLinks()).thenAnswer { throw SettingNotFoundException() }
        assertThat(underTest()).isTrue()

        verify(settingsRepository).setAutoAcceptQR(true)
    }

    @Test
    fun `test that if current value throws an unknownException, it is rethrown`() = runTest{
        whenever(fetchAutoAcceptQRLinks()).thenAnswer { throw Exception() }
        assertThrows(Exception::class.java){
            runBlocking{ underTest() }
        }

        verifyNoInteractions(settingsRepository)
    }

}