package nz.mega.android;

public class MegaAttributes {
	
	String online = "";
	int attemps = 0;
	
	public MegaAttributes(String online, int attemps) {
		this.online = online;
		this.attemps = attemps;
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

}
