package mega.privacy.android.app;

import nz.mega.sdk.MegaApiJava;

import static mega.privacy.android.app.utils.LogUtil.*;

public class MegaAttributes {
	
	private String online = "";
	private int attemps = 0;
	private String askSizeDownload = "true";
	private String askNoAppDownload = "true";
	private String fileLoggerSDK = "false";
	private String accountDetailsTimeStamp="";
	private String paymentMethodsTimeStamp="";
	private String pricingTimeStamp ="";
	private String extendedAccountDetailsTimeStamp="";
	private String invalidateSdkCache = "false";
	private String fileLoggerKarere = "false";
	private String useHttpsOnly = "false";
	private String showCopyright = "true";
	private String showNotifOff = "true";
	private String staging = "false";
	private String lastPublicHandle = "";
	private String lastPublicHandleTimeStamp = "";
	private int lastPublicHandleType = MegaApiJava.AFFILIATE_TYPE_INVALID;
	private int storageState = MegaApiJava.STORAGE_STATE_UNKNOWN;
	private String myChatFilesFolderHandle = "";
	private String transferQueueStatus;

	public MegaAttributes(String online, int attemps, String askSizeDownload, String askNoAppDownload, String fileLogger,
						  String accountDetailsTimeStamp, String paymentMethodsTimeStamp, String pricingTimeStamp, String extendedAccountDetailsTimeStamp,
						  String invalidateSdkCache, String fileLoggerKarere, String showCopyright, String showNotifOff, String staging,
						  String lastPublicHandle, String lastPublicHandleTimeStamp, int lastPublicHandleType, int storageState, String myChatFilesFolderHandle,
						  String transferQueueStatus) {
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
		this.showCopyright = showCopyright;
		this.showNotifOff = showNotifOff;
        this.staging = staging;
		this.lastPublicHandle = lastPublicHandle;
		this.lastPublicHandleTimeStamp = lastPublicHandleTimeStamp;
		this.lastPublicHandleType = lastPublicHandleType;
		this.storageState = storageState;
		this.myChatFilesFolderHandle = myChatFilesFolderHandle;
		this.transferQueueStatus = transferQueueStatus;
	}

	public MegaAttributes(String online, int attemps, String askSizeDownload, String askNoAppDownload, String fileLogger,
						  String accountDetailsTimeStamp, String paymentMethodsTimeStamp, String pricingTimeStamp, String extendedAccountDetailsTimeStamp,
						  String invalidateSdkCache, String fileLoggerKarere, String useHttpsOnly, String showCopyright, String showNotifOff, String staging,
						  String lastPublicHandle, String lastPublicHandleTimeStamp, int lastPublicHandleType, int storageState, String myChatFilesFolderHandle,
						  String transferQueueStatus) {
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
		this.showCopyright = showCopyright;
		this.showNotifOff = showNotifOff;
        this.staging = staging;
		this.lastPublicHandle = lastPublicHandle;
		this.lastPublicHandleTimeStamp = lastPublicHandleTimeStamp;
		this.lastPublicHandleType = lastPublicHandleType;
		this.storageState = storageState;
		this.myChatFilesFolderHandle = myChatFilesFolderHandle;
		this.transferQueueStatus = transferQueueStatus;
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

	public String getShowCopyright() {
		return showCopyright;
	}

	public void setShowCopyright(String showCopyright) {
		this.showCopyright = showCopyright;
	}

	public String getShowNotifOff() {
		return showNotifOff;
	}

	public void setShowNotifOff(String showNotifOff) {
		this.showNotifOff = showNotifOff;
	}

    public String getStaging(){
        return staging;
    }

    public void setStaging(String staging) {
        this.staging = staging;
    }

	public long getLastPublicHandle() {
		return getLongValueFromStringAttribute(lastPublicHandle, "Last public handle", -1);
	}

	public void setLastPublicHandle(long lastPublicHandle){
		this.lastPublicHandle = Long.toString(lastPublicHandle);
	}

	public long getLastPublicHandleTimeStamp(){
		return getLongValueFromStringAttribute(lastPublicHandleTimeStamp, "Last public handle time stamp", -1);
	}

	public void setLastPublicHandleTimeStamp(long lastPublicHandleTimeStamp){
		this.lastPublicHandleTimeStamp = Long.toString(lastPublicHandleTimeStamp);
	}

	public int getLastPublicHandleType(){
		return lastPublicHandleType;
	}

	public void setLastPublicHandleType(int lastPublicHandleType){
		this.lastPublicHandleType = lastPublicHandleType;
	}

	public int getStorageState(){
		return storageState;
	}

	public void setStorageState(int storageState){
		this.storageState = storageState;
	}

	public long getMyChatFilesFolderHandle() {
		return getLongValueFromStringAttribute(myChatFilesFolderHandle, "My chat files folder handle", MegaApiJava.INVALID_HANDLE);
	}

	public void setMyChatFilesFolderHandle(long myChatFilesFolderHandle) {
		this.myChatFilesFolderHandle = Long.toString(myChatFilesFolderHandle);
	}

	public String getTransferQueueStatus() {
		return transferQueueStatus;
	}

	public void setTransferQueueStatus(String transferQueueStatus) {
		this.transferQueueStatus = transferQueueStatus;
	}

	/**
	 * Get an attribute stored as String as a long value.
	 *
	 * @param attribute    Attribute to get value.
	 * @param attrName     Descriptive name of the attribute.
	 * @param defaultValue Default value to get in case of error.
	 * @return Long value of the attribute.
	 */
	private long getLongValueFromStringAttribute(String attribute, String attrName, long defaultValue) {
		long value = defaultValue;

		if (attribute != null) {
			try {
				value = Long.parseLong(attribute);
			} catch (Exception e) {
				logWarning("Exception getting " + attrName + ".", e);
				value = defaultValue;
			}
		}

		logDebug(attrName + ": " + value);
		return value;
	}
}
