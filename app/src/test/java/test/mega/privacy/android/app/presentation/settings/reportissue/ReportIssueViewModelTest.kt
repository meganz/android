package test.mega.privacy.android.app.presentation.settings.reportissue

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.reportissue.ReportIssueViewModel
import mega.privacy.android.app.presentation.settings.reportissue.model.SubmitIssueResult
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.usecase.GetSupportEmailUseCase
import mega.privacy.android.domain.usecase.SubmitIssueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.InstantExecutorExtension
import test.mega.privacy.android.app.extensions.asHotFlow
import test.mega.privacy.android.app.extensions.withCoroutineExceptions

@ExperimentalCoroutinesApi
@ExtendWith(InstantExecutorExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportIssueViewModelTest {
    private lateinit var underTest: ReportIssueViewModel

    private val submitIssueUseCase = mock<SubmitIssueUseCase>()

    private var savedStateHandle = SavedStateHandle(mapOf())

    private val monitorConnectivityUseCase =
        mock<MonitorConnectivityUseCase>()

    private val supportEmail = "Support@Email.address"
    private val getSupportEmail = mock<GetSupportEmailUseCase>()

    @BeforeEach
    fun setUp() {
        monitorConnectivityUseCase.stub {
            on { invoke() } doReturn true.asHotFlow()
        }
    }


    private fun initViewModel() {
        underTest = ReportIssueViewModel(
            submitIssueUseCase = submitIssueUseCase,
            savedStateHandle = savedStateHandle,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            getSupportEmailUseCase = getSupportEmail,
        )
    }

    @AfterEach
    fun resetTests() {
        savedStateHandle = SavedStateHandle(mapOf())
        reset(
            submitIssueUseCase,
            monitorConnectivityUseCase,
            getSupportEmail,
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        initViewModel()
        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.description).isEmpty()
            assertThat(initial.includeLogsVisible).isTrue()
            assertThat(initial.includeLogs).isFalse()
            assertThat(initial.canSubmit).isFalse()
        }
    }

    @Test
    fun `test that saved state values are returned`() = runTest {
        val expectedDescription = "A saved description"
        savedStateHandle[ReportIssueViewModel.DESCRIPTION_KEY] = expectedDescription
        savedStateHandle[ReportIssueViewModel.INCLUDE_LOGS_KEY] = true
        initViewModel()

        underTest.uiState.filter {
            it.description == expectedDescription &&
                    it.includeLogs
        }.test {
            val latest = awaitItem()
            assertThat(latest.description).isEqualTo(expectedDescription)
            assertThat(latest.includeLogs).isTrue()
        }
    }

    @Test
    fun `test that description is updated if new description is provided`() = runTest {
        initViewModel()
        underTest.uiState.map { it.description }.distinctUntilChanged()
            .test {
                val newDescription = "New description"
                assertThat(awaitItem()).isEmpty()
                underTest.setDescription(newDescription)
                assertThat(awaitItem()).isEqualTo(newDescription)
            }
    }

    @Test
    fun `test that can submit is is false by default`() = runTest {
        initViewModel()
        underTest.uiState.test {
            assertThat(awaitItem().canSubmit).isFalse()
        }
    }

    @Test
    fun `test that can submit is true when a description exists`() = runTest {
        initViewModel()
        underTest.uiState.map { it.canSubmit }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isFalse()
                underTest.setDescription("Not blank")
                assertThat(awaitItem()).isTrue()
            }
    }

    @Test
    fun `test that after setting a description can submit is true`() = runTest {
        initViewModel()
        underTest.uiState.distinctUntilChangedBy { it.canSubmit }.test {
            assertThat(awaitItem().canSubmit).isFalse()
            underTest.setDescription("A Description")
            assertThat(awaitItem().canSubmit).isTrue()
        }
    }

    @Test
    fun `test that can submit becomes false if description is removed`() = runTest {
        initViewModel()
        underTest.uiState.distinctUntilChangedBy { it.canSubmit }.test {
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
            whenever(monitorConnectivityUseCase()).thenReturn(false.asHotFlow())

            initViewModel()

            underTest.uiState.map { it.error }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isNull()
                    underTest.submit()
                    assertThat(awaitItem()).isEqualTo(R.string.check_internet_connection_error)
                }
        }

    @Test
    fun `test that a success message is returned if submit report completes without an error`() =
        runTest {
            whenever(submitIssueUseCase(any())).thenReturn(emptyFlow())
            initViewModel()
            underTest.setDescription("A long enough description")
            scheduler.advanceUntilIdle()
            underTest.uiState.map { it.result }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isNull()
                    underTest.submit()
                    assertThat(awaitItem()).isInstanceOf(SubmitIssueResult.Success::class.java)
                }
        }

    @Test
    fun `test that an error message is returned if submit report completes with an error`() =
        withCoroutineExceptions {
            runTest {
                submitIssueUseCase.stub {
                    onBlocking { invoke(any()) }.thenAnswer { throw Exception() }
                }
                getSupportEmail.stub {
                    onBlocking { invoke() } doReturn supportEmail
                }

                initViewModel()
                underTest.setDescription("A long enough description")
                scheduler.advanceUntilIdle()
                underTest.uiState.map { it.result }.distinctUntilChanged()
                    .test {
                        assertThat(awaitItem()).isNull()
                        underTest.submit()
                        assertThat(awaitItem()).isInstanceOf(SubmitIssueResult.Failure::class.java)
                    }
            }
        }

    @Test
    fun `test that description and log setting are passed to submit use case`() = runTest {
        initViewModel()
        scheduler.advanceUntilIdle()
        val newDescription = "Expected description"
        underTest.setDescription(newDescription)
        underTest.setIncludeLogsEnabled(true)

        scheduler.advanceUntilIdle()
        underTest.submit()
        scheduler.advanceUntilIdle()

        verify(submitIssueUseCase).invoke(argThat { description == newDescription && includeLogs })
    }

    @Test
    fun `test that upload progress from 0 to 100 is returned`() = runTest {
        whenever(submitIssueUseCase(any())).thenReturn(getProgressFlow())
        initViewModel()
        underTest.setDescription("A long enough description")
        scheduler.advanceUntilIdle()
        underTest.uiState.mapNotNull { it.uploadProgress }.distinctUntilChanged()
            .test {
                underTest.submit()
                (0..100).map { it / 100f }.forEach {
                    assertThat(awaitItem()).isEqualTo(it)
                }
            }
    }

    @Test
    fun `test that no progress is returned after upload is cancelled`() = runTest {
        whenever(submitIssueUseCase(any())).thenReturn(
            getProgressFlow().onEach {
                if (it.floatValue > 0.5f) underTest.cancelUpload()
            })

        initViewModel()
        underTest.setDescription("A long enough description")
        scheduler.advanceUntilIdle()
        underTest.uiState.mapNotNull { it.uploadProgress }.distinctUntilChanged()
            .test {
                underTest.submit()
                (0..50).map { it / 100f }.forEach {
                    assertThat(awaitItem()).isEqualTo(it)
                }
            }
    }

    @Test
    fun `test that cancelling an upload does not return an error`() = runTest {
        whenever(submitIssueUseCase(any())).thenReturn(
            getProgressFlow()
                .onEach { if (it.floatValue > 0.1f) underTest.cancelUpload() }
        )

        initViewModel()
        underTest.setDescription("A long enough description")
        scheduler.advanceUntilIdle()

        underTest.uiState.map { it.result }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isNull()
                underTest.submit()
                scheduler.advanceUntilIdle()
            }
    }

    @Test
    fun `test that cancelling upload clears progress`() = runTest {
        whenever(submitIssueUseCase(any())).thenReturn(
            getProgressFlow()
                .onEach {
                    if (it.floatValue == 0.01f) underTest.cancelUpload()
                }
        )

        initViewModel()
        underTest.setDescription("A long enough description")
        scheduler.advanceUntilIdle()
        underTest.uiState.map { it.uploadProgress == null }.distinctUntilChanged().test {
            assertThat(awaitItem()).isTrue()
            underTest.submit()
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
        }
    }

    private fun getProgressFlow() = (0..100).map { Progress(it / 100f) }.asFlow()

    companion object {
        private val scheduler = TestCoroutineScheduler()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher(scheduler))
    }
}