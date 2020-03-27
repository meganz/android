package mega.privacy.android.app.middlelayer.iab;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

import static mega.privacy.android.app.utils.billing.PaymentUtils.*;

public interface BillingManager {

    //for HMS only, the value should be OrderStatusCode.ORDER_STATE_SUCCESS
    int ORDER_STATE_SUCCESS = 0;

    List<String> IN_APP_SKUS = Arrays.asList(
            SKU_PRO_I_MONTH,
            SKU_PRO_I_YEAR,
            SKU_PRO_II_MONTH,
            SKU_PRO_II_YEAR,
            SKU_PRO_III_MONTH,
            SKU_PRO_III_YEAR,
            SKU_PRO_LITE_MONTH,
            SKU_PRO_LITE_YEAR);

    /**
     * For HMS resolution
     */
    interface RequestCode {

        /**
         * requestCode for pull up the pmsPay page
         */
        int REQ_CODE_BUY = 4002;

        /**
         * requestCode for pull up the login page or agreement page for createPurchaseIntentWithPrice interface
         */
        int REQ_CODE_BUYWITHPRICE_CONTINUE = 4005;

        /**
         * requestCode for pull up the login page for isEnvReady interface
         */
        int REQ_CODE_LOGIN = 2001;
    }

    boolean isPurchased(MegaPurchase purchase);

    void initiatePurchaseFlow(@Nullable String oldSku, @Nullable String purchaseToken, @NonNull MegaSku skuDetails);

    void destroy();

    void getInventory(QuerySkuListCallback callback);

    boolean isPayloadValid(String pl);

    String getPayload();

    void queryPurchases();

    void updatePurchase();

    boolean verifyValidSignature(String signedData, String signature);

    /**
     * for HMS only
     *
     * @param data
     * @return
     */
    int getPurchaseResult(Intent data);
}
