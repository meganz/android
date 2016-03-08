package mega.privacy.android.app;

public class MegaAttributes {
	
	String online = "";
	int attemps = 0;
	String askSizeDownload = "true";
	String askNoAppDownload = "true";
	
	public MegaAttributes(String online, int attemps, String askSizeDownload, String askNoAppDownload) {
		this.online = online;
		this.attemps = attemps;
		this.askNoAppDownload = askNoAppDownload;
		this.askSizeDownload = askSizeDownload;
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

}
