package test.mega.privacy.android.app.presentation.settings.reportissue

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.reportissue.ReportIssueViewModel
import mega.privacy.android.app.presentation.settings.reportissue.model.SubmitIssueResult
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.usecase.AreChatLogsEnabled
import mega.privacy.android.domain.usecase.AreSdkLogsEnabled
import mega.privacy.android.domain.usecase.GetSupportEmail
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.SubmitIssue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ReportIssueViewModelTest {
    private lateinit var underTest: ReportIssueViewModel

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val submitIssue = mock<SubmitIssue>()
    private val areSdkLogsEnabled = mock<AreSdkLogsEnabled> {
        on { invoke() }.thenReturn(flowOf(false))
    }
    private val areChatLogsEnabled = mock<AreChatLogsEnabled> {
        on { invoke() }.thenReturn(flowOf(false))
    }

    private val savedStateHandle = SavedStateHandle(mapOf())


    private val monitorConnectivityUseCase =
        mock<MonitorConnectivityUseCase> { on { invoke() }.thenReturn(MutableStateFlow(true)) }

    private val scheduler = TestCoroutineScheduler()

    private val getSupportEmail =
        mock<GetSupportEmail> { onBlocking { invoke() }.thenReturn("Support@Email.address") }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        underTest = ReportIssueViewModel(
            submitIssue = submitIssue,
            areSdkLogsEnabled = areSdkLogsEnabled,
            areChatLogsEnabled = areChatLogsEnabled,
            savedStateHandle = savedStateHandle,
            ioDispatcher = StandardTestDispatcher(),
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            getSupportEmail = getSupportEmail,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.description).isEmpty()
            assertThat(initial.includeLogsVisible).isFalse()
            assertThat(initial.includeLogs).isFalse()
            assertThat(initial.canSubmit).isFalse()
        }
    }

    @Test
    fun `test that saved state values are returned`() = runTest {
        val expectedDescription = "A saved description"
        savedStateHandle.set(underTest.descriptionKey, expectedDescription)
        savedStateHandle.set(underTest.includeLogsVisibleKey, true)
        savedStateHandle.set(underTest.includeLogsKey, true)

        underTest.state.filter {
            it.description == expectedDescription &&
                    it.includeLogsVisible &&
                    it.includeLogs
        }.test(200) {
            val latest = awaitItem()
            assertThat(latest.description).isEqualTo(expectedDescription)
            assertThat(latest.includeLogsVisible).isTrue()
            assertThat(latest.includeLogs).isTrue()
        }
    }

    @Test
    fun `test that when logging is enabled include logs toggle is shown`() = runTest {
        whenever(areSdkLogsEnabled()).thenReturn(flowOf(true))
        whenever(areChatLogsEnabled()).thenReturn(flowOf(true))

        underTest.state.map { it.includeLogsVisible }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isFalse()
                assertThat(awaitItem()).isTrue()
            }
    }

    @Test
    fun `test that include logs is set to true if logging is enabled`() = runTest {
        whenever(areSdkLogsEnabled()).thenReturn(flowOf(true))
        whenever(areChatLogsEnabled()).thenReturn(flowOf(true))

        underTest.state.map { it.includeLogs }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isFalse()
                assertThat(awaitItem()).isTrue()
            }
    }

    @Test
    fun `test that description is updated if new description is provided`() = runTest {
        underTest.state.map { it.description }.distinctUntilChanged()
            .test {
                val newDescription = "New description"
                assertThat(awaitItem()).isEmpty()
                underTest.setDescription(newDescription)
                assertThat(awaitItem()).isEqualTo(newDescription)
            }
    }

    @Test
    fun `test that include logs is updated if new boolean is provided`() = runTest {
        whenever(areSdkLogsEnabled()).thenReturn(flowOf(true))
        whenever(areChatLogsEnabled()).thenReturn(flowOf(true))

        underTest.state.map { it.includeLogs }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isFalse()
                assertThat(awaitItem()).isTrue()
                underTest.setIncludeLogsEnabled(false)
                assertThat(awaitItem()).isFalse()
            }
    }

    @Test
    fun `test that can submit is is false by default`() = runTest {
        underTest.state.test {
            assertThat(awaitItem().canSubmit).isFalse()
        }
    }

    @Test
    fun `test that can submit is true when a description exists`() = runTest {
        underTest.state.map { it.canSubmit }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isFalse()
                underTest.setDescription("Not empty")
                assertThat(awaitItem()).isTrue()
            }
    }

    @Test
    fun `test that after setting a description can submit is true`() = runTest {
        underTest.state.test {
            assertThat(awaitItem().canSubmit).isFalse()
            underTest.setDescription("A Description")
            assertThat(awaitItem().canSubmit).isTrue()
        }
    }

    @Test
    fun `test that can submit becomes false if description is removed`() = runTest {
        underTest.state.test {
            assertThat(awaitItem().canSubmit).isFalse()

            underTest.setDescription("A Description")
            assertThat(awaitItem().canSubmit).isTrue()

            underTest.setDescription("")
            assertThat(awaitItem().canSubmit).isFalse()
        }
    }


    @Test
    fun `test that connection error is returned if attempting to submit and no internet available`() =
        runTest {
            whenever(monitorConnectivityUseCase()).thenReturn(MutableStateFlow(false))

            underTest.state.map { it.error }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isNull()
                    underTest.submit()
                    assertThat(awaitItem()).isEqualTo(R.string.check_internet_connection_error)
                }
        }

    @Test
    fun `test that a success message is returned if submit report completes without an error`() =
        runTest {
            whenever(submitIssue(any())).thenReturn(emptyFlow())

            scheduler.advanceUntilIdle()
            underTest.state.map { it.result }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isNull()
                    underTest.submit()
                    assertThat(awaitItem()).isInstanceOf(SubmitIssueResult.Success::class.java)
                }
        }

    @Test
    fun `test that an error message is returned if submit report completes with an error`() =
        runTest {
            whenever(submitIssue(any())).thenReturn(flow { throw Exception() })
            scheduler.advanceUntilIdle()
            underTest.state.map { it.result }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isNull()
                    underTest.submit()
                    assertThat(awaitItem()).isInstanceOf(SubmitIssueResult.Failure::class.java)
                }
        }

    @Test
    fun `test that description and log setting are passed to submit use case`() = runTest {
        scheduler.advanceUntilIdle()
        val newDescription = "Expected description"
        underTest.setDescription(newDescription)
        underTest.setIncludeLogsEnabled(true)

        scheduler.advanceUntilIdle()
        underTest.submit()
        scheduler.advanceUntilIdle()

        verify(submitIssue).invoke(argThat { description == newDescription && includeLogs })
    }

    @Test
    fun `test that upload progress from 0 to 100 is returned`() = runTest {
        whenever(submitIssue(any())).thenReturn(getProgressFlow())
        scheduler.advanceUntilIdle()
        underTest.state.mapNotNull { it.uploadProgress }.distinctUntilChanged()
            .test {
                underTest.submit()
                (0..100).map { it / 100f }.forEach {
                    assertThat(awaitItem()).isEqualTo(it)
                }
            }
    }

    @Test
    fun `test that no progress is returned after upload is cancelled`() = runTest {
        whenever(submitIssue(any())).thenReturn(
            getProgressFlow().onEach {
                if (it.floatValue > 0.5f) underTest.cancelUpload()
            })
        scheduler.advanceUntilIdle()
        underTest.state.mapNotNull { it.uploadProgress }.distinctUntilChanged()
            .test {
                underTest.submit()
                (0..50).map { it / 100f }.forEach {
                    assertThat(awaitItem()).isEqualTo(it)
                }
            }
    }

    @Test
    fun `test that cancelling an upload does not return an error`() = runTest {
        whenever(submitIssue(any())).thenReturn(
            getProgressFlow()
                .onEach { if (it.floatValue > 0.1f) underTest.cancelUpload() }
        )
        scheduler.advanceUntilIdle()

        underTest.state.map { it.result }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isNull()
                underTest.submit()
                scheduler.advanceUntilIdle()
            }
    }

    @Test
    fun `test that cancelling upload clears progress`() = runTest {
        whenever(submitIssue(any())).thenReturn(
            getProgressFlow()
                .onEach {
                    if (it.floatValue == 0.01f) underTest.cancelUpload()
                }
        )
        scheduler.advanceUntilIdle()
        underTest.state.map { it.uploadProgress == null }.distinctUntilChanged().test {
            assertThat(awaitItem()).isTrue()
            underTest.submit()
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
        }
    }

    private fun getProgressFlow() = (0..100).map { Progress(it / 100f) }.asFlow()
}