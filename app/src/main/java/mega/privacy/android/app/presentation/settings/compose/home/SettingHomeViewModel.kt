package mega.privacy.android.app.presentation.settings.compose.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.compose.home.mapper.MyAccountSettingStateMapper
import mega.privacy.android.app.presentation.settings.compose.home.model.SettingsHomeState
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.navigation.settings.FeatureSettingEntryPoint
import mega.privacy.android.navigation.settings.MoreSettingEntryPoint
import javax.inject.Inject

/**
 * Setting container view model
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class SettingHomeViewModel @Inject constructor(
    featureEntryPoints: Set<@JvmSuppressWildcards FeatureSettingEntryPoint>,
    moreEntryPoints: Set<@JvmSuppressWildcards MoreSettingEntryPoint>,
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
    private val myAccountSettingStateMapper: MyAccountSettingStateMapper,
) : ViewModel() {

    val state: StateFlow<SettingsHomeState>
        field: MutableStateFlow<SettingsHomeState> = MutableStateFlow(
            SettingsHomeState.Loading(
                featureEntryPoints = featureEntryPoints
                    .sortedBy { it.preferredOrdinal }
                    .toImmutableList(),
                moreEntryPoints = moreEntryPoints
                    .sortedBy { it.preferredOrdinal }
                    .toImmutableList(),
            )
        )

    init {
        runCatching {
            viewModelScope.launch {
                val accountDetails: UserAccount = getAccountDetailsUseCase(false)
                state.update { currentState ->
                    myAccountSettingStateMapper(accountDetails)?.let { myAccount ->
                        SettingsHomeState.Data(
                            myAccountState = myAccount,
                            featureEntryPoints = currentState.featureEntryPoints,
                            moreEntryPoints = currentState.moreEntryPoints,
                        )
                    } ?: currentState
                }
            }
        }
    }
}
