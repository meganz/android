package mega.privacy.android.domain.usecase.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.home.HomeWidgetConfiguration
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub

@RunWith(MockitoJUnitRunner::class)
class MonitorHomeWidgetConfigurationUseCaseTest {
    @Mock
    private lateinit var mockSettingsRepository: SettingsRepository

    private lateinit var underTest: MonitorHomeWidgetConfigurationUseCase

    @Before
    fun setUp() {
        underTest = MonitorHomeWidgetConfigurationUseCase(mockSettingsRepository)
    }

    @Test
    fun `test that an empty list is returned if list is empty`() = runTest {
        val expected = emptyList<HomeWidgetConfiguration>()

        mockSettingsRepository.stub {
            on { monitorHomeScreenWidgetConfiguration() } doReturn flow {
                emit(expected)
                awaitCancellation()
            }
        }

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
        }
    }

    @Test
    fun `test that values are returned if found`() =
        runTest {
            val expected = listOf(
                HomeWidgetConfiguration(
                    widgetIdentifier = "widget1",
                    widgetOrder = 1,
                    enabled = true
                ),
                HomeWidgetConfiguration(
                    widgetIdentifier = "widget2",
                    widgetOrder = 2,
                    enabled = false
                ),
                HomeWidgetConfiguration(
                    widgetIdentifier = "widget3",
                    widgetOrder = 3,
                    enabled = true
                ),
            )
            mockSettingsRepository.stub {
                on { monitorHomeScreenWidgetConfiguration() } doReturn flow {
                    emit(expected)
                    awaitCancellation()
                }
            }

            underTest().test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }
}
