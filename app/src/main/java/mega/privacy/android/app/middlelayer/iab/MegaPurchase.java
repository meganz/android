package mega.privacy.android.app.middlelayer.iab;


import androidx.annotation.Nullable;

import com.android.billingclient.api.Purchase;

import java.util.ArrayList;
import java.util.List;

public class MegaPurchase {

    private String sku;

    private String receipt;

    private String userHandle;

    private int state;

    private String token;

    public String getSku() {
        return sku;
    }

    public String getReceipt() {
        return receipt;
    }

    public String getUserHandle() {
        return userHandle;
    }

    public int getState() {
        return state;
    }

    public String getToken() {
        return token;
    }

    private MegaPurchase() {
        // hide constructor, use converter instead
    }

    public static MegaPurchase convert(Purchase purchase) {
        MegaPurchase p = new MegaPurchase();
        p.sku = purchase.getSku();
        p.receipt = purchase.getOriginalJson();
        p.state = purchase.getPurchaseState();
        p.token = purchase.getPurchaseToken();
        p.userHandle = purchase.getDeveloperPayload();
        return p;
    }

    public static List<MegaPurchase> convert(@Nullable List<Purchase> purchases) {
        if (purchases == null) {
            return null;
        }
        List<MegaPurchase> result = new ArrayList<>(purchases.size());
        for (Purchase purchase : purchases) {
            result.add(convert(purchase));
        }
        return result;
    }
}
