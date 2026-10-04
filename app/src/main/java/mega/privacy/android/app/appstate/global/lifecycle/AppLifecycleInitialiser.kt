package mega.privacy.android.app.appstate.global.lifecycle

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.navigation.contract.initialisation.SynchronousAppCreateInitialiser
import javax.inject.Inject

/**
 * Registers [AppForegroundLifecycleObserver] with the process lifecycle and the
 * application's component callbacks so it can flush logs at lifecycle boundaries.
 */
internal class AppLifecycleInitialiser @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observer: AppForegroundLifecycleObserver,
) : SynchronousAppCreateInitialiser {
    override val name = "AppLifecycleInitialiser"

    override operator fun invoke() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        context.registerComponentCallbacks(observer)
    }
}
