package mega.privacy.android.app.appstate.global.initialisation.appcreate

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.VideoCaptureUtils
import mega.privacy.android.navigation.contract.initialisation.SynchronousAppCreateInitialiser
import javax.inject.Inject

/**
 * Hands the application context to the legacy static objects that cannot be Hilt-injected.
 *
 * [AvatarUtil] is a `@JvmStatic` util with many Java/Kotlin callers and [VideoCaptureUtils] is
 * invoked from JNI, so neither can receive dependencies through injection. This unit is the
 * sanctioned interim mechanism for such statics/JNI objects: an explicit, boot-ordered
 * application-context handoff that replaces their `MegaApplication.getInstance()` coupling.
 *
 * Synchronous and set early: it must complete during `Application.onCreate` so the context is in
 * place before any avatar colour is resolved or video capture starts. Neither object is used
 * during boot, so it only needs to run before UI/video, not first.
 */
internal class StaticContextInitialiser @Inject constructor(
    @ApplicationContext private val context: Context,
) : SynchronousAppCreateInitialiser {
    override val name = "StaticContextInitialiser"

    override operator fun invoke() {
        AvatarUtil.applicationContext = context
        CallUtil.applicationContext = context
        VideoCaptureUtils.setApplicationContext(context)
    }
}
