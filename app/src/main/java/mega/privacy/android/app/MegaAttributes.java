package mega.privacy.android.app;

public class MegaAttributes {
	
	String online = "";
	int attemps = 0;
	String askSizeDownload = "true";
	String askNoAppDownload = "true";
	String fileLogger = "false";
	String accountDetailsTimeStamp="";
	String paymentMethodsTimeStamp="";
	String creditCardTimeStamp="";
	
	public MegaAttributes(String online, int attemps, String askSizeDownload, String askNoAppDownload, String fileLogger, String accountDetailsTimeStamp, String paymentMethodsTimeStamp, String creditCardTimeStamp) {
		this.online = online;
		this.attemps = attemps;
		this.askNoAppDownload = askNoAppDownload;
		this.askSizeDownload = askSizeDownload;
		this.fileLogger = fileLogger;
		this.accountDetailsTimeStamp = accountDetailsTimeStamp;
		this.paymentMethodsTimeStamp = paymentMethodsTimeStamp;
		this.creditCardTimeStamp = creditCardTimeStamp;
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

	public String getCreditCardTimeStamp() {
		return creditCardTimeStamp;
	}

	public void setCreditCardTimeStamp(String creditCardTimeStamp) {
		this.creditCardTimeStamp = creditCardTimeStamp;
	}

	public String getPaymentMethodsTimeStamp() {
		return paymentMethodsTimeStamp;
	}

	public void setPaymentMethodsTimeStamp(String paymentMethodsTimeStamp) {
		this.paymentMethodsTimeStamp = paymentMethodsTimeStamp;
	}

}
