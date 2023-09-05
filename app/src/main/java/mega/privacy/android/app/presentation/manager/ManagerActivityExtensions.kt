package mega.privacy.android.app.presentation.manager

import mega.privacy.android.app.main.ManagerActivity

/**
 * Get the ManagerState in ManagerViewModel
 *
 * @return the ManagerState hold in ManagerViewModel
 */
fun ManagerActivity.state() = viewModel.state.value

/**
 * Get the FileBrowserState in FileBrowserViewModel
 *
 * @return the FileBrowserState hold in FileBrowserViewModel
 */
fun ManagerActivity.fileBrowserState() = fileBrowserViewModel.state.value

/**
 * Get the IncomingSharesState in IncomingSharesStateViewModel
 *
 * @return the IncomingSharesState hold in IncomingSharesStateViewModel
 */
fun ManagerActivity.incomingSharesState() = incomingSharesViewModel.state.value

/**
 * Get the OutgoingSharesState in OutgoingSharesStateViewModel
 *
 * @return the OutgoingSharesState hold in OutgoingSharesStateViewModel
 */
fun ManagerActivity.outgoingSharesState() = outgoingSharesViewModel.state.value

/**
 * Get the BackupsState in BackupsViewModel
 *
 * @return the BackupsState in BackupsViewModel
 */
fun ManagerActivity.backupsState() = backupsViewModel.state.value

/**
 * Get the LinksState in LinksViewModel
 *
 * @return the LinksState hold in LinksViewModel
 */
fun ManagerActivity.linksState() = legacyLinksViewModel.state.value

/**
 * Get the RubbishBinState in RubbishBinViewModel
 *
 * @return the RubbishBinState hold in RubbishBinViewModel
 */
fun ManagerActivity.rubbishBinState() = rubbishBinViewModel.state.value
