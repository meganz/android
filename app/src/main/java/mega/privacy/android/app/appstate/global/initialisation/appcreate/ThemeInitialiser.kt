package mega.privacy.android.app.appstate.global.initialisation.appcreate

import mega.privacy.android.app.presentation.theme.ThemeModeState
import mega.privacy.android.navigation.contract.initialisation.SynchronousAppCreateInitialiser
import javax.inject.Inject

/**
 * Starts the theme mode collection that keeps `AppCompatDelegate`'s night mode in sync with the
 * user's preference.
 *
 * Critical: the body only launches the collection (non-blocking), and running it synchronously
 * preserves today's guarantee that the collection is started before the first Activity can
 * resolve its theme.
 */
internal class ThemeInitialiser @Inject constructor(
    private val themeModeState: ThemeModeState,
) : SynchronousAppCreateInitialiser {
    override val name = "ThemeInitialiser"

    override operator fun invoke() {
        themeModeState.initialise()
    }
}
