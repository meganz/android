package mega.privacy.android.feature.myaccount.presentation.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.GetUserFirstNameUseCase
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.feature.myaccount.presentation.mapper.AccountTypeNameMapper
import mega.privacy.android.feature.myaccount.presentation.mapper.QuotaLevelMapper
import mega.privacy.android.feature.myaccount.presentation.model.MyAccountWidgetUiState
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for MyAccount widget
 */
@HiltViewModel
class MyAccountWidgetViewModel @Inject constructor(
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorStorageStateUseCase: MonitorStorageStateUseCase,
    private val getUserFirstNameUseCase: GetUserFirstNameUseCase,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val monitorMyAvatarFile: MonitorMyAvatarFile,
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase,
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase,
    private val accountTypeNameMapper: AccountTypeNameMapper,
    private val quotaLevelMapper: QuotaLevelMapper,
) : ViewModel() {

    internal val uiState: StateFlow<MyAccountWidgetUiState> by lazy {
        combine(
            monitorAccountDetailUseCase().catch {
                Timber.e(
                    it,
                    "Error monitoring account details"
                )
            },
            monitorStorageStateUseCase().catch { Timber.e(it, "Error monitoring storage state") },
            monitorUserUpdates()
                .onStart { emit(UserChanges.Firstname) }
                .filter { it == UserChanges.Firstname || it == UserChanges.Lastname }
                .catch { Timber.w("Exception monitoring user updates: $it") },
            monitorMyAvatarFile()
                .onStart {
                    emit(getMyAvatarFileUseCase(isForceRefresh = false))
                    emit(getMyAvatarFileUseCase(isForceRefresh = true))
                }
                .catch { Timber.e(it, "Error loading avatar data") }
        ) { accountDetail, storageState, _, avatarFile ->
            val storageDetail = accountDetail.storageDetail
            val usedPercentage = storageDetail?.usedPercentage ?: 0
            val userName =
                runCatching { getUserFirstNameUseCase(forceRefresh = false) }.getOrNull() ?: ""
            val avatarColor = runCatching { getMyAvatarColorUseCase() }.getOrNull()

            MyAccountWidgetUiState(
                name = userName,
                usedStorage = storageDetail?.usedStorage ?: 0L,
                totalStorage = storageDetail?.totalStorage ?: 0L,
                usedStoragePercentage = usedPercentage,
                storageState = storageState,
                storageQuotaLevel = quotaLevelMapper(usedPercentage, storageState),
                accountTypeNameResource = accountTypeNameMapper(accountDetail.levelDetail?.accountType),
                avatarFile = avatarFile,
                avatarColor = avatarColor,
                isLoading = false
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(
                viewModelScope,
                MyAccountWidgetUiState(accountTypeNameResource = accountTypeNameMapper(null))
            )
    }
}
