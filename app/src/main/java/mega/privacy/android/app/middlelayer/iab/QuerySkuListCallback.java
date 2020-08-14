package mega.privacy.android.app.middlelayer.iab;

import java.util.List;

/**
 * Get currently available subscriptions' callback.
 */
public interface QuerySkuListCallback {

    /**
     * Execute when get subscriptions succefully.
     *
     * @param skuList Available subscriptions. Need to covert platform depends subscription object to generic MegaSku object.
     */
    void onSuccess(List<MegaSku> skuList);
}
