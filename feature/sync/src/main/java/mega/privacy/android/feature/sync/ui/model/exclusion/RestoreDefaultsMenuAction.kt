package mega.privacy.android.feature.sync.ui.model.exclusion

import mega.android.core.ui.model.menu.MenuActionWithoutIcon
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Puts a sync's exclusions back to the set MEGA defines.
 *
 * Deliberately carries no icon: that is what makes the design system draw it inside the top bar's
 * overflow menu rather than beside the title, which is where the design puts it.
 */
internal object RestoreDefaultsMenuAction : MenuActionWithoutIcon(
    descriptionRes = sharedR.string.sync_exclusions_manage_restore_defaults,
    testTag = TEST_TAG_RESTORE_DEFAULTS_ACTION,
)

internal const val TEST_TAG_RESTORE_DEFAULTS_ACTION = "manage_sync_exclusions:restore_defaults"
