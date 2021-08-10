package mega.privacy.android.app.getLink

import androidx.hilt.lifecycle.ViewModelInject
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode

class GetSeveralLinksViewModel @ViewModelInject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) : BaseRxViewModel() {

    private val nodeList = ArrayList<MegaNode>()

    fun initNodes(handlesList: LongArray) {
        for (handle in handlesList) {
            val node = megaApi.getNodeByHandle(handle)
            if (node != null) {
                nodeList.add(node)
            }
        }
    }
}