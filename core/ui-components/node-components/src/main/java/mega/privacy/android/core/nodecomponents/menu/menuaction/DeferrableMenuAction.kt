package mega.privacy.android.core.nodecomponents.menu.menuaction

import mega.android.core.ui.model.menu.MenuActionWithIcon

/**
 * Marker interface for menu actions that can be intercepted by
 * [mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult]'s
 * `onDeferredAction` before execution (e.g. for rewarded ad gating).
 */
interface DeferrableMenuAction : MenuActionWithIcon
