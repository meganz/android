package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mega.privacy.android.app.appstate.global.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.app.presentation.psa.InfoPsaBottomSheet
import mega.privacy.android.app.presentation.psa.StandardPsaBottomSheet
import mega.privacy.android.app.presentation.psa.WebPsaScreen
import mega.privacy.android.app.presentation.psa.mapper.PsaStateMapper
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.psa.MonitorDisplayedPsaUseCase
import mega.privacy.android.domain.usecase.psa.MonitorPsaUseCase
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import timber.log.Timber
import javax.inject.Inject

class PsaInitialiser(
    private val coroutineScope: CoroutineScope,
    private val monitorPsaUseCase: MonitorPsaUseCase,
    private val monitorDisplayedPsaUseCase: MonitorDisplayedPsaUseCase,
    private val psaStateMapper: PsaStateMapper,
    private val currentTimeProvider: () -> Long,
    private val navigationEventQueue: NavigationEventQueue,
) : PostLoginInitialiser(
    action = { _, _ ->
        coroutineScope.launch {
            combine(
                monitorPsaUseCase(currentTimeProvider)
                    .map { psaStateMapper(it) }
                    .onEach { Timber.d("PSA State: $it") }
                    .catch { Timber.e(it, "Error in monitoring psa") },
                monitorDisplayedPsaUseCase()
                    .onEach { Timber.d("Displayed psa: $it") }
            ) { psaState, displayedPsa ->
                if (displayedPsa == null || displayedPsa != psaState.id) {
                    psaState
                } else {
                    null
                }
            }.filterNotNull()
                .filterNot { it is PsaState.NoPsa }
                .distinctUntilChanged()
                .collect {
                    when (it) {
                        is PsaState.InfoPsa -> {
                            navigationEventQueue.emit(InfoPsaBottomSheet(it))
                        }

                        is PsaState.StandardPsa -> {
                            navigationEventQueue.emit(StandardPsaBottomSheet(it))
                        }

                        is PsaState.WebPsa -> {
                            navigationEventQueue.emit(WebPsaScreen(it))
                        }

                        else -> { //no-op }
                        }
                    }
                }
        }
    }
) {
    @Inject
    constructor(
        @ApplicationScope coroutineScope: CoroutineScope,
        monitorPsaUseCase: MonitorPsaUseCase,
        monitorDisplayedPsaUseCase: MonitorDisplayedPsaUseCase,
        psaStateMapper: PsaStateMapper,
        navigationEventQueue: NavigationEventQueue,
    ) : this(
        coroutineScope = coroutineScope,
        monitorPsaUseCase = monitorPsaUseCase,
        monitorDisplayedPsaUseCase = monitorDisplayedPsaUseCase,
        psaStateMapper = psaStateMapper,
        currentTimeProvider = System::currentTimeMillis,
        navigationEventQueue = navigationEventQueue,
    )
}