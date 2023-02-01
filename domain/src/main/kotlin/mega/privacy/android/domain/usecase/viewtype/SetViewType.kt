package mega.privacy.android.domain.usecase.viewtype

import mega.privacy.android.domain.entity.preference.ViewType

/**
 * Use Case to set the preferred view type
 */
fun interface SetViewType {

    /**
     * Invocation method
     * @param viewType the new [ViewType] to update
     */
    suspend operator fun invoke(viewType: ViewType)
}