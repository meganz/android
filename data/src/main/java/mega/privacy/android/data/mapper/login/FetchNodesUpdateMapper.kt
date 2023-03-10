package mega.privacy.android.data.mapper.login

import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

/**
 * Mapper to get fetch nodes progress and temporary errors and convert them into [FetchNodesUpdate].
 */
internal fun interface FetchNodesUpdateMapper {

    /**
     * Invoke.
     *
     * @param request [MegaRequest]. If null means the request finished.
     * @param error [MegaError]. If null means there is a request update or the request finished.
     */
    operator fun invoke(request: MegaRequest?, error: MegaError?): FetchNodesUpdate
}