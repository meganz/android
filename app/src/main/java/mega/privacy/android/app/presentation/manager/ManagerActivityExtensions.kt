package mega.privacy.android.app.presentation.manager

import mega.privacy.android.app.main.ManagerActivity

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