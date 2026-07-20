package mega.privacy.android.app.initializer

import android.content.Context
import mega.privacy.android.app.MegaApplication

/**
 * True when app startup initializers and manifest-registered receivers may resolve Hilt entry
 * points.
 *
 * Under an instrumented Hilt test the application is a Hilt test application whose component
 * does not exist yet when androidx.startup runs its initializers or when the system delivers
 * early broadcasts, so entry-point-based initializers and receivers must no-op there; tests
 * configure replacements through Hilt test modules instead.
 */
internal fun Context.canResolveHiltEntryPoints(): Boolean = applicationContext is MegaApplication
