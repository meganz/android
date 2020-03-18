package mega.privacy.android.app.middlelayer.iab;


import androidx.annotation.Nullable;

import com.android.billingclient.api.SkuDetails;

import java.util.ArrayList;
import java.util.List;

public class MegaSku {

    private String sku;

    private String type;

    public String getSku() {
        return sku;
    }

    public String getType() {
        return type;
    }

    private MegaSku() {
        // hide constructor, use converter instead
    }

    public static MegaSku convert(SkuDetails sku) {
        MegaSku megaSku = new MegaSku();
        megaSku.sku = sku.getSku();
        megaSku.type = sku.getType();
        return megaSku;
    }

    public static List<MegaSku> convert(@Nullable List<SkuDetails> skus) {
        if(skus == null) {
            return null;
        }
        List<MegaSku> result = new ArrayList<>(skus.size());
        for(SkuDetails sku : skus) {
            result.add(convert(sku));
        }
        return result;
    }
}
