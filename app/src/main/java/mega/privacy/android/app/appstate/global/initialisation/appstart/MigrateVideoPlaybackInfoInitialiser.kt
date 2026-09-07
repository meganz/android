package mega.privacy.android.app.appstate.global.initialisation.appstart

import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.MigrateVideoPlaybackInfosUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiserAction
import timber.log.Timber
import javax.inject.Inject

/**
 * App start initialiser that carries the legacy video playback positions over to the database.
 *
 * App-start initialisers are launched concurrently and nothing waits for them, so a player opened
 * immediately after launch can still beat this copy. That costs one resume: the position is
 * written regardless and the next open finds it. A no-op on every start after the first.
 */
class MigrateVideoPlaybackInfoInitialiser @Inject constructor(
    private val migrateVideoPlaybackInfosUseCase: MigrateVideoPlaybackInfosUseCase,
) : AppStartInitialiserAction(action = {
    runCatching {
        migrateVideoPlaybackInfosUseCase()
    }.onFailure {
        Timber.e(it, "Error migrating the legacy video playback positions")
    }
})
