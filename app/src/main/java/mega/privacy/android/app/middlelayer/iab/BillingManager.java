package mega.privacy.android.app.middlelayer.iab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/*
 * general
 * mega.privacy.android.app.middlelayer.iab
 *
 * Created on 19/03/20 4:42 PM.
 */
public interface BillingManager {

    boolean isPurchased(MegaPurchase purchase);

    void initiatePurchaseFlow(@Nullable String oldSku, @Nullable String purchaseToken, @NonNull MegaSku skuDetails);

    void destroy();

    void getInventory(QuerySkuListCallback callback);

    boolean isPayloadValid(String pl);

    String getPayload();
}
