package mega.privacy.android.app.presentation.manager.model.mapper

import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.domain.entity.preference.StartScreen

/**
 * Initial screen mapper
 */
fun interface InitialScreenMapper {
    /**
     * Invoke
     *
     * @param startScreen
     * @return mapped drawer item
     */
    operator fun invoke(startScreen: StartScreen): DrawerItem
}