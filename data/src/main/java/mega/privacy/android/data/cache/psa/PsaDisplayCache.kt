package mega.privacy.android.data.cache.psa

import mega.privacy.android.data.cache.InMemoryStateFlowCache
import mega.privacy.android.data.cache.StateFlowCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PsaDisplayCache(
    internalCache: StateFlowCache<Int>,
) : StateFlowCache<Int> by internalCache {

    @Inject
    constructor() : this(InMemoryStateFlowCache())
}