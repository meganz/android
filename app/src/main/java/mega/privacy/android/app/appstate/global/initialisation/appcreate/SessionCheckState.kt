package mega.privacy.android.app.appstate.global.initialisation.appcreate

import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.domain.entity.user.UserCredentials

/**
 * Session facts kept current from app create so session guards can read them synchronously
 * (e.g. from an Activity's onCreate) without blocking the main thread.
 *
 * Values are null until first determined, shortly after app create. Readers must treat null as
 * "no session" so the unknown window fails towards a session refresh rather than towards using
 * a session that is not there.
 */
interface SessionCheckState {

    /**
     * Whether the SDK currently has a root node, null if not yet determined.
     */
    val rootNodeExists: StateFlow<Boolean?>

    /**
     * The credentials of the logged in account, null if not yet determined or not logged in.
     */
    val userCredentials: StateFlow<UserCredentials?>
}
