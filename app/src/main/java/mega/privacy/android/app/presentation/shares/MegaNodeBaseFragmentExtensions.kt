package mega.privacy.android.app.presentation.shares

/**
 * Get the ManagerState in ManagerViewModel
 * This function will be used until MegaNodeBaseFragment is converted to Kotlin
 *
 * @return the ManagerState hold in ManagerViewModel
 */
fun MegaNodeBaseFragment.managerState() = managerViewModel.state.value