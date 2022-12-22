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
 * Get the FileBrowserState in FileBrowserViewModel
 * This function will be used until ManagerActivity is converted to Kotlin
 *
 * @return the FileBrowserState hold in FileBrowserViewModel
 */
fun ManagerActivity.fileBrowserState() = fileBrowserViewModel.state.value

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
 * Get the InboxState in InboxViewModel
 * This function will be used until ManagerActivity is converted to Kotlin
 *
 * @return the InboxState hold in InboxViewModel
 */
fun ManagerActivity.inboxState() = inboxViewModel.state.value

/**
 * Get the LinksState in LinksViewModel
 * This function will be used until ManagerActivity is converted to Kotlin
 *
 * @return the LinksState hold in LinksViewModel
 */
fun ManagerActivity.linksState() = linksViewModel.state.value
