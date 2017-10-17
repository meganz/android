package mega.privacy.android.app;

public class MegaAttributes {
	
	String online = "";
	int attemps = 0;
	String askSizeDownload = "true";
	String askNoAppDownload = "true";
	String fileLoggerSDK = "false";
	String accountDetailsTimeStamp="";
	String paymentMethodsTimeStamp="";
	String pricingTimeStamp ="";
	String extendedAccountDetailsTimeStamp="";
	String invalidateSdkCache = "false";
	String fileLoggerKarere = "false";
	String useHttpsOnly = "false";
	
	public MegaAttributes(String online, int attemps, String askSizeDownload, String askNoAppDownload, String fileLogger, String accountDetailsTimeStamp, String paymentMethodsTimeStamp, String pricingTimeStamp, String extendedAccountDetailsTimeStamp, String invalidateSdkCache, String fileLoggerKarere) {
		this.online = online;
		this.attemps = attemps;
		this.askNoAppDownload = askNoAppDownload;
		this.askSizeDownload = askSizeDownload;
		this.fileLoggerSDK = fileLogger;
		this.accountDetailsTimeStamp = accountDetailsTimeStamp;
		this.paymentMethodsTimeStamp = paymentMethodsTimeStamp;
		this.pricingTimeStamp = pricingTimeStamp;
		this.extendedAccountDetailsTimeStamp = extendedAccountDetailsTimeStamp;
		this.invalidateSdkCache = invalidateSdkCache;
		this.fileLoggerKarere = fileLoggerKarere;
		this.useHttpsOnly = "false";
	}

	public MegaAttributes(String online, int attemps, String askSizeDownload, String askNoAppDownload, String fileLogger, String accountDetailsTimeStamp, String paymentMethodsTimeStamp, String pricingTimeStamp, String extendedAccountDetailsTimeStamp, String invalidateSdkCache, String fileLoggerKarere, String useHttpsOnly) {
		this.online = online;
		this.attemps = attemps;
		this.askNoAppDownload = askNoAppDownload;
		this.askSizeDownload = askSizeDownload;
		this.fileLoggerSDK = fileLogger;
		this.accountDetailsTimeStamp = accountDetailsTimeStamp;
		this.paymentMethodsTimeStamp = paymentMethodsTimeStamp;
		this.pricingTimeStamp = pricingTimeStamp;
		this.extendedAccountDetailsTimeStamp = extendedAccountDetailsTimeStamp;
		this.invalidateSdkCache = invalidateSdkCache;
		this.fileLoggerKarere = fileLoggerKarere;
		this.useHttpsOnly = useHttpsOnly;
	}
	
	public String getOnline(){
		return online;
	}
	
	public void setOnline (String online){
		this.online = online;
	}

	public int getAttemps() {
		return attemps;
	}

	public void setAttemps(int attemps) {
		this.attemps = attemps;
	}

	public String getAskSizeDownload() {
		return askSizeDownload;
	}

	public void setAskSizeDownload(String askSizeDownload) {
		this.askSizeDownload = askSizeDownload;
	}

	public String getAskNoAppDownload() {
		return askNoAppDownload;
	}

	public void setAskNoAppDownload(String askNoAppDownload) {
		this.askNoAppDownload = askNoAppDownload;
	}

	public String getFileLoggerSDK(){
		return fileLoggerSDK;
	}

	public void setFileLoggerSDK(String fileLoggerSDK){
		this.fileLoggerSDK = fileLoggerSDK;
	}

	public String getFileLoggerKarere(){
		return this.fileLoggerKarere;
	}

	public void setFileLoggerKarere(String fileLoggerKarere){
		this.fileLoggerKarere = fileLoggerKarere;
	}

	public String getAccountDetailsTimeStamp() {
		return accountDetailsTimeStamp;
	}

	public void setAccountDetailsTimeStamp(String accountDetailsTimeStamp) {
		this.accountDetailsTimeStamp = accountDetailsTimeStamp;
	}

	public String getPricingTimeStamp() {
		return pricingTimeStamp;
	}

	public void setPricingTimeStamp(String pricingTimeStamp) {
		this.pricingTimeStamp = pricingTimeStamp;
	}

	public String getPaymentMethodsTimeStamp() {
		return paymentMethodsTimeStamp;
	}

	public void setPaymentMethodsTimeStamp(String paymentMethodsTimeStamp) {
		this.paymentMethodsTimeStamp = paymentMethodsTimeStamp;
	}

	public String getExtendedAccountDetailsTimeStamp() {
		return extendedAccountDetailsTimeStamp;
	}

	public void setExtendedAccountDetailsTimeStamp(String extendedAccountDetailsTimeStamp) {
		this.extendedAccountDetailsTimeStamp = extendedAccountDetailsTimeStamp;
	}

	public String getInvalidateSdkCache(){
		return invalidateSdkCache;
	}

	public void setInvalidateSdkCache(String invalidateSdkCache){
		this.invalidateSdkCache = invalidateSdkCache;
	}

	public String getUseHttpsOnly() {
		return useHttpsOnly;
	}

	public void setUseHttpsOnly(String useHttpsOnly) {
		this.useHttpsOnly = useHttpsOnly;
	}
}
