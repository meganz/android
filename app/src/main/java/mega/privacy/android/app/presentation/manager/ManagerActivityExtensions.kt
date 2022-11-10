package mega.privacy.android.app.presentation.manager

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.domain.usecase.BroadcastUploadPauseState

/**
 * Get the ManagerState in ManagerViewModel
 * This function will be used until ManagerActivity is converted to Kotlin
 *
 * @return the ManagerState hold in ManagerViewModel
 */
fun ManagerActivity.state() = viewModel.state.value

/**
 * Get the IncomingSharesState in IncomingSharesStateViewModel
 * This function will be used until ManagerActivity is converted to Kotlin
 *
 * @return the IncomingSharesState hold in IncomingSharesStateViewModel
 */
fun ManagerActivity.incomingSharesState() = incomingSharesViewModel.state.value

/**
 * Get the OutgoingSharesState in OutgoingSharesStateViewModel
 * This function will be used until ManagerActivity is converted to Kotlin
 *
 * @return the OutgoingSharesState hold in OutgoingSharesStateViewModel
 */
fun ManagerActivity.outgoingSharesState() = outgoingSharesViewModel.state.value

/**
 * Get the LinksState in LinksViewModel
 * This function will be used until ManagerActivity is converted to Kotlin
 *
 * @return the LinksState hold in LinksViewModel
 */
fun ManagerActivity.linksState() = linksViewModel.state.value


/**
 * broad cast upload pause state event
 */
fun ManagerActivity.broadCastUploadStatus(
    broadcastUploadPauseState: BroadcastUploadPauseState,
) {
    this.lifecycleScope.launch {
        broadcastUploadPauseState.invoke()
    }
}
