package mega.privacy.android.app.listeners.global.initialisers

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.appstate.initialisation.initialisers.AppStartInitialiser
import mega.privacy.android.domain.usecase.node.root.MonitorRootNodeRefreshEventUseCase
import timber.log.Timber
import javax.inject.Inject

class ReloadEventInitialiser @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val monitorRootNodeRefreshEventUseCase: MonitorRootNodeRefreshEventUseCase,
) : AppStartInitialiser(
    action = {
        monitorRootNodeRefreshEventUseCase()
            .onEach { Timber.d("Root node refresh event received") }
            .collectLatest {
                appContext.startActivity(Intent(appContext, MegaActivity::class.java).apply {
                    action = it.name
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
    }
)