package mega.privacy.android.app.middlelayer.iab;

import java.util.List;

/**
 * Listener to the updates that happen when purchases list was updated or consumption of the
 * item was finished
 */
public interface BillingUpdatesListener {


    void onBillingClientSetupFinished();

    void onPurchasesUpdated(boolean isFailed, int resultCode, List<MegaPurchase> purchases);

    void onQueryPurchasesFinished(boolean isFailed, int resultCode, List<MegaPurchase> purchases);
}
