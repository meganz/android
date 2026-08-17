package mega.privacy.android.app.appstate.content.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.appstate.content.navigation.model.StorageQuotaWarning
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.StorageQuotaWarningTrigger
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.account.SetStorageQuotaWarningShownUseCase
import mega.privacy.android.domain.usecase.account.ShouldShowStorageQuotaWarningUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.MonitorSuccessfulUploadsUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@ExtendWith(CoroutineMainDispatcherExtension::class)
class StorageStatusViewModelTest {

    private val monitorStorageStateUseCase = mock<MonitorStorageStateUseCase>()
    private val monitorSuccessfulUploadsUseCase = mock<MonitorSuccessfulUploadsUseCase>()
    private val shouldShowStorageQuotaWarningUseCase =
        mock<ShouldShowStorageQuotaWarningUseCase>()
    private val setStorageQuotaWarningShownUseCase = mock<SetStorageQuotaWarningShownUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    @AfterEach
    fun tearDown() {
        reset(
            monitorStorageStateUseCase,
            monitorSuccessfulUploadsUseCase,
            shouldShowStorageQuotaWarningUseCase,
            setStorageQuotaWarningShownUseCase,
            getFeatureFlagValueUseCase,
        )
    }

    @Test
    fun `test that quotaWarnings emits the warning for the current storage state`() = runTest {
        stubDependencies(storageStates = openFlowOf(StorageState.Red))

        buildUnderTest().quotaWarnings.test {
            assertThat(awaitItem()).isEqualTo(
                StorageQuotaWarning(
                    storageState = StorageState.Red,
                    trigger = StorageQuotaWarningTrigger.LoginOrReload,
                    isUpsellEnabled = true,
                )
            )
        }
    }

    @Test
    fun `test that quotaWarnings does not emit again when a collector resubscribes`() = runTest {
        stubDependencies(storageStates = openFlowOf(StorageState.Red))
        val underTest = buildUnderTest()

        underTest.quotaWarnings.test { awaitItem() }

        underTest.quotaWarnings.test { expectNoEvents() }
    }

    @Test
    fun `test that quotaWarnings emits again when the storage state changes`() = runTest {
        val storageStates = MutableStateFlow(StorageState.Orange)
        stubDependencies(storageStates = storageStates)

        buildUnderTest().quotaWarnings.test {
            assertThat(awaitItem().storageState).isEqualTo(StorageState.Orange)

            storageStates.value = StorageState.Red

            assertThat(awaitItem().storageState).isEqualTo(StorageState.Red)
        }
    }

    @Test
    fun `test that quotaWarnings does not emit when the warning should not be shown`() = runTest {
        stubDependencies(storageStates = openFlowOf(StorageState.Green), shouldShow = false)

        buildUnderTest().quotaWarnings.test { expectNoEvents() }
    }

    @Test
    fun `test that onQuotaWarningShown spends the allowance for the shown warning`() = runTest {
        stubDependencies(storageStates = openFlowOf(StorageState.Orange))
        val warning = StorageQuotaWarning(
            storageState = StorageState.Orange,
            trigger = StorageQuotaWarningTrigger.UploadSuccess,
            isUpsellEnabled = true,
        )

        buildUnderTest().onQuotaWarningShown(warning)

        verify(setStorageQuotaWarningShownUseCase).invoke(
            StorageState.Orange,
            StorageQuotaWarningTrigger.UploadSuccess,
        )
    }

    private fun buildUnderTest() = StorageStatusViewModel(
        monitorStorageStateUseCase = monitorStorageStateUseCase,
        monitorSuccessfulUploadsUseCase = monitorSuccessfulUploadsUseCase,
        shouldShowStorageQuotaWarningUseCase = shouldShowStorageQuotaWarningUseCase,
        setStorageQuotaWarningShownUseCase = setStorageQuotaWarningShownUseCase,
        getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
    )

    /**
     * A state the monitor re-reads on every subscription, held open the way the real flow is, so a
     * resubscription would raise the warning again were the sharing not keeping the upstream alive.
     */
    private fun openFlowOf(storageState: StorageState): Flow<StorageState> = flow {
        emit(storageState)
        awaitCancellation()
    }

    private fun stubDependencies(
        storageStates: Flow<StorageState>,
        shouldShow: Boolean = true,
        isUpsellEnabled: Boolean = true,
    ) {
        monitorStorageStateUseCase.stub { on { invoke() }.thenReturn(storageStates) }
        monitorSuccessfulUploadsUseCase.stub { on { invoke() }.thenReturn(emptyFlow<Transfer>()) }
        shouldShowStorageQuotaWarningUseCase.stub {
            onBlocking { invoke(any(), any()) }.thenReturn(shouldShow)
        }
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(ApiFeatures.QuotaWarningUpsellScreen) }.thenReturn(isUpsellEnabled)
        }
    }
}
