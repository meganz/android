package mega.privacy.android.app.middlelayer.iab;

import java.util.List;

/**
 * Listener to the updates that happen when purchases list was updated or consumption of the
 * item was finished
 */
public interface BillingUpdatesListener {

    /**
     * Callback when billing client is setup.
     */
    void onBillingClientSetupFinished();

    /**
     * Callback when a purchase completes.
     *
     * @param isFailed If the purchase is failed.
     * @param resultCode Result code returned by platform.
     * @param purchases Current valid purchases.
     */
    void onPurchasesUpdated(boolean isFailed, int resultCode, List<MegaPurchase> purchases);

    /**
     * Callback when query current purchase completes.
     *
     * @param isFailed If the purchase is failed.
     * @param resultCode Result code returned by platform.
     * @param purchases Current valid purchases.
     */
    void onQueryPurchasesFinished(boolean isFailed, int resultCode, List<MegaPurchase> purchases);
}
