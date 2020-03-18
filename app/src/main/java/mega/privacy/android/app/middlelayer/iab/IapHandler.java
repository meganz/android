package mega.privacy.android.app.middlelayer.iab;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.utils.billing.BillingManager;
import mega.privacy.android.app.middlelayer.iab.MegaSku;

import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.billing.PaymentUtils.SKU_PRO_III_MONTH;
import static mega.privacy.android.app.utils.billing.PaymentUtils.SKU_PRO_III_YEAR;
import static mega.privacy.android.app.utils.billing.PaymentUtils.SKU_PRO_II_MONTH;
import static mega.privacy.android.app.utils.billing.PaymentUtils.SKU_PRO_II_YEAR;
import static mega.privacy.android.app.utils.billing.PaymentUtils.SKU_PRO_I_MONTH;
import static mega.privacy.android.app.utils.billing.PaymentUtils.SKU_PRO_I_YEAR;
import static mega.privacy.android.app.utils.billing.PaymentUtils.SKU_PRO_LITE_MONTH;
import static mega.privacy.android.app.utils.billing.PaymentUtils.SKU_PRO_LITE_YEAR;

public class IapHandler {

    private MegaApplication app;

    private List<SkuDetails> mSkuDetailsList;

    private BillingManager mBillingManager;

    public IapHandler() {

    }

    private void getInventory() {
        SkuDetailsResponseListener listener = new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    logWarning("Failed to get SkuDetails, error code is " + billingResult.getResponseCode());
                }
                if (skuDetailsList != null && skuDetailsList.size() > 0) {
                    mSkuDetailsList = skuDetailsList;
//                    app.getMyAccountInfo().setAvailableSkus(MegaSku.convert(skuDetailsList));
                }
            }
        };

        List<String> inAppSkus = new ArrayList<>();
        inAppSkus.add(SKU_PRO_I_MONTH);
        inAppSkus.add(SKU_PRO_I_YEAR);
        inAppSkus.add(SKU_PRO_II_MONTH);
        inAppSkus.add(SKU_PRO_II_YEAR);
        inAppSkus.add(SKU_PRO_III_MONTH);
        inAppSkus.add(SKU_PRO_III_YEAR);
        inAppSkus.add(SKU_PRO_LITE_MONTH);
        inAppSkus.add(SKU_PRO_LITE_YEAR);

        //we only support subscription for google pay
        mBillingManager.querySkuDetailsAsync(BillingClient.SkuType.SUBS, inAppSkus, listener);
    }
}
