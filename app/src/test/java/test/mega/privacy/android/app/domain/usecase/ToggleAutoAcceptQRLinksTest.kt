package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.exception.ApiError
import mega.privacy.android.app.domain.exception.SettingNotFoundException
import mega.privacy.android.app.domain.repository.SettingsRepository
import mega.privacy.android.app.domain.usecase.DefaultToggleAutoAcceptQRLinks
import mega.privacy.android.app.domain.usecase.FetchAutoAcceptQRLinks
import mega.privacy.android.app.domain.usecase.ToggleAutoAcceptQRLinks
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFailsWith

class ToggleAutoAcceptQRLinksTest {

    private lateinit var underTest: ToggleAutoAcceptQRLinks

    private val fetchAutoAcceptQRLinks = mock<FetchAutoAcceptQRLinks>()
    private val settingsRepository = mock<SettingsRepository>()

    @Before
    fun setUp() {
        underTest = DefaultToggleAutoAcceptQRLinks(
            fetchAutoAcceptQRLinks = fetchAutoAcceptQRLinks,
            settingsRepository = settingsRepository
        )
    }

    @Test
    fun `test that auto accept is set to true if currently false`() = runTest {
        whenever(fetchAutoAcceptQRLinks()).thenReturn(true)
        val actual = underTest()
        verify(settingsRepository, times(1)).setAutoAcceptQR(false)
        assertThat(actual).isFalse()
    }

    @Test
    fun `test that auto accept is set to false if not present`() = runTest {
        whenever(fetchAutoAcceptQRLinks()).thenAnswer { throw SettingNotFoundException() }
        val actual = underTest()
        verify(settingsRepository, times(1)).setAutoAcceptQR(false)
        assertThat(actual).isFalse()
    }

    @Test
    fun `test that errors are propagated`() = runTest {
        whenever(fetchAutoAcceptQRLinks()).thenAnswer { throw ApiError() }
        assertFailsWith<ApiError> {
            underTest()
        }
    }
}