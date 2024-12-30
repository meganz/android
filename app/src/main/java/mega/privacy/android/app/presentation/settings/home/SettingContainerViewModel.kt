package mega.privacy.android.app.presentation.settings.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.home.mapper.SectionHeaderMapper
import mega.privacy.android.app.presentation.settings.home.mapper.SettingItemFlowMapper
import mega.privacy.android.app.presentation.settings.home.mapper.SettingItemMapper
import mega.privacy.android.app.presentation.settings.home.model.SettingModelItem
import mega.privacy.android.app.presentation.settings.home.model.SettingSection
import mega.privacy.android.app.presentation.settings.home.model.SettingsUiState
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.navigation.settings.FeatureSettings
import timber.log.Timber
import javax.inject.Inject

/**
 * Setting container view model
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class SettingContainerViewModel @Inject constructor(
    private val featureSettings: Set<FeatureSettings>,
    private val settingItemMapper: SettingItemMapper,
    private val settingItemFlowMapper: SettingItemFlowMapper,
    private val sectionHeaderMapper: SectionHeaderMapper,
    @ApplicationScope private val functionScope: CoroutineScope,
) : ViewModel() {
    private val _state: MutableStateFlow<SettingsUiState> =
        MutableStateFlow(SettingsUiState.Loading)
    val state = _state.asStateFlow()

    init {

        viewModelScope.launch {
            val settingList: List<SettingModelItem> = featureSettings.toSettingList()

            val updateFlows =
                featureSettings.toUpdateFlowArray() + flowOf({ it })

            runCatching {
                combine(
                    flowOf(settingList).filterNot { it.isEmpty() },
                    merge(*updateFlows)
                ) { list, updateFunction ->
                    updateFunction(list)
                }.mapLatest { list ->
                    list.groupBy { it.section }
                        .map { (section, items) ->
                            SettingSection(
                                sectionHeader = sectionHeaderMapper(section),
                                sectionItems = items
                            )
                        }
                }.catch {
                    Timber.e(it)
                }.collectLatest {
                    _state.value = SettingsUiState.Data(it)
                }
            }.onFailure {
                Timber.e(it)
            }
        }

    }

    private fun Set<FeatureSettings>.toSettingList() = map { it.entryPoints }.flatten()
        .map { entry ->
            entry.items.map {
                settingItemMapper(
                    section = entry.section,
                    item = it,
                    suspendHandler = getSuspendHandler()
                )
            }
        }.flatten()

    private fun getSuspendHandler(): (suspend () -> Unit) -> Unit = {
        functionScope.launch {
            runCatching { it() }
                .onFailure { e ->
                    Timber.e(e)
                }
        }
    }

    private fun Set<FeatureSettings>.toUpdateFlowArray() =
        mapNotNull { entry ->
            entry.entryPoints.map {
                it.items
            }.flatten()
                .map { item ->
                    settingItemFlowMapper(item) ?: return@mapNotNull null
                }
        }.flatten()
            .toTypedArray()

}
