package mega.privacy.android.app.di

import android.content.Context

/**
 * Boot-provided application [Context] for the handful of context-less static helpers that resolve
 * Hilt entry points (e.g. [getDbHandler]) and therefore need an application [Context] but cannot
 * receive one through injection.
 *
 * Set once during `Application.onCreate` by `StaticContextInitialiser`. This is the same sanctioned
 * boot-handoff mechanism used for the legacy static utils; it deliberately exposes only a [Context]
 * rather than the whole `MegaApplication`, replacing the removed `MegaApplication.getInstance()`
 * service locator.
 */
object LegacyStaticAppContext {
    lateinit var context: Context
}
