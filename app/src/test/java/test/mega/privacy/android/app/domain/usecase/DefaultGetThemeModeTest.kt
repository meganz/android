package test.mega.privacy.android.app.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.entity.ThemeMode
import mega.privacy.android.app.domain.repository.SettingsRepository
import mega.privacy.android.app.domain.usecase.DefaultGetThemeMode
import mega.privacy.android.app.domain.usecase.GetThemeMode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetThemeModeTest {
    private lateinit var underTest: GetThemeMode

    private val settingsRepository = mock<SettingsRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetThemeMode(settingsRepository = settingsRepository)
    }

    @Test
    fun `test that default value is returned if settings returns null`() = runTest {
        whenever(settingsRepository.monitorStringPreference(any(), any())).thenReturn(flowOf(null))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(ThemeMode.DEFAULT)
            awaitComplete()
        }
    }

    @Test
    fun `test that default value is returned if unknown value is returned`() = runTest {
        whenever(settingsRepository.monitorStringPreference(any(),
            any())).thenReturn(flowOf("Not a valid value"))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(ThemeMode.DEFAULT)
            awaitComplete()
        }
    }

    @Test
    fun `test that theme values are returned if the names match exactly`() = runTest {
        whenever(settingsRepository.monitorStringPreference(any(), any())).thenReturn(ThemeMode.values()
            .map { it.name }.asFlow())

        underTest().test {
            ThemeMode.values()
                .forEach {
                    assertThat(awaitItem()).isEqualTo(it)
                }
            awaitComplete()
        }
    }

    @Test
    fun `test that theme values are returned if the names match ignoring case`() = runTest {
        whenever(settingsRepository.monitorStringPreference(any(), any())).thenReturn(ThemeMode.values()
            .map { it.name.uppercase(Locale.getDefault()) }.asFlow())

        underTest().test {
            ThemeMode.values()
                .forEach {
                    assertThat(awaitItem()).isEqualTo(it)
                }
            awaitComplete()
        }
    }

}