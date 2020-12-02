package mega.privacy.android.app.middlelayer.iab;


/**
 * Generic SKU object, used to unify corresponding platform dependent purchase object.
 *
 * In HMS, it's ProductInfo.
 * In GMS, it's SkuDetails.
 */
public class MegaSku {

    /**
     * SKU of the product.
     */
    private String sku;

    /**
     * Price of the sku in corresponding platform.
     */
    private long priceAmountMicros;

    /**
     * Currency code, used to format price.
     */
    private String priceCurrencyCode;

    public MegaSku(String sku, long priceAmountMicros, String priceCurrencyCode) {
        this.sku = sku;
        this.priceAmountMicros = priceAmountMicros;
        this.priceCurrencyCode = priceCurrencyCode;
    }

    public String getSku() {
        return sku;
    }

    public long getPriceAmountMicros() {
        return priceAmountMicros;
    }

    public String getPriceCurrencyCode() {
        return priceCurrencyCode;
    }
}
