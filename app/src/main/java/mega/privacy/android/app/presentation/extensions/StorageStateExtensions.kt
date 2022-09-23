package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent

fun MonitorStorageStateEvent.getState() : StorageState =
    storageState.value.storageState
