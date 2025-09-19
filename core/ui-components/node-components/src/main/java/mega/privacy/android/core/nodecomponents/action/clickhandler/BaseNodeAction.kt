package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction

interface BaseNodeAction {
    fun canHandle(action: MenuAction): Boolean
}
