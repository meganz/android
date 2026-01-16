package mega.privacy.android.app.appstate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import mega.privacy.android.app.appstate.global.model.GlobalState
import mega.privacy.android.app.appstate.global.model.RootNodeState
import timber.log.Timber

@Composable
fun SessionConnectivityObserver(
    globalState: GlobalState,
    rootNodeState: RootNodeState,
    reconnect: () -> Unit = { },
) {
    var isSessionOffline by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(globalState, rootNodeState) {
        if (globalState is GlobalState.LoggedIn && !rootNodeState.exists) {
            if (globalState.isConnected) {
                if (isSessionOffline) {
                    Timber.d("Session was offline, performing background fast login")
                    reconnect()
                    isSessionOffline = false
                }
            } else {
                Timber.d("Session is empty due to offline")
                isSessionOffline = true
            }
        }
    }
}