package mega.privacy.android.app.middlelayer.iab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

import static mega.privacy.android.app.utils.billing.PaymentUtils.*;

public interface BillingManager {

    List<String> IN_APP_SKUS = Arrays.asList(
            SKU_PRO_I_MONTH,
            SKU_PRO_I_YEAR,
            SKU_PRO_II_MONTH,
            SKU_PRO_II_YEAR,
            SKU_PRO_III_MONTH,
            SKU_PRO_III_YEAR,
            SKU_PRO_LITE_MONTH,
            SKU_PRO_LITE_YEAR);

    boolean isPurchased(MegaPurchase purchase);

    void initiatePurchaseFlow(@Nullable String oldSku, @Nullable String purchaseToken, @NonNull MegaSku skuDetails);

    void destroy();

    void getInventory(QuerySkuListCallback callback);

    boolean isPayloadValid(String pl);

    String getPayload();
}
