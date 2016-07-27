package mega.privacy.android.app;

public class MegaAttributes {
	
	String online = "";
	int attemps = 0;
	String askSizeDownload = "true";
	String askNoAppDownload = "true";
	String fileLogger = "false";
	String accountDetailsTimeStamp="";
	String paymentMethodsTimeStamp="";
	String pricingTimeStamp ="";
	String extendedAccountDetailsTimeStamp="";
	
	public MegaAttributes(String online, int attemps, String askSizeDownload, String askNoAppDownload, String fileLogger, String accountDetailsTimeStamp, String paymentMethodsTimeStamp, String pricingTimeStamp, String extendedAccountDetailsTimeStamp) {
		this.online = online;
		this.attemps = attemps;
		this.askNoAppDownload = askNoAppDownload;
		this.askSizeDownload = askSizeDownload;
		this.fileLogger = fileLogger;
		this.accountDetailsTimeStamp = accountDetailsTimeStamp;
		this.paymentMethodsTimeStamp = paymentMethodsTimeStamp;
		this.pricingTimeStamp = pricingTimeStamp;
		this.extendedAccountDetailsTimeStamp = extendedAccountDetailsTimeStamp;
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

	public String getFileLogger(){
		return fileLogger;
	}

	public void setFileLogger(String fileLogger){
		this.fileLogger = fileLogger;
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

}
