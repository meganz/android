package mega.privacy.android.app;

import android.os.Parcel;
import android.os.Parcelable;

public class Product implements Parcelable{

	private long handle;
	private int level;
	private int months;
	private int storage;
	private int transfer;
	private int amount;
	private String currency;
	private boolean isBusiness;

	public Product(long handle, int level, int months, int storage, int transfer, int amount, String currency, boolean isBusiness) {
		this.handle = handle;
		this.level = level;
		this.months = months;
		this.storage = storage;
		this.transfer = transfer;
		this.amount = amount;
		this.currency = currency;
		this.isBusiness = isBusiness;
	}

	public long getHandle() {
		return handle;
	}

	public void setHandle(long handle) {
		this.handle = handle;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getMonths() {
		return months;
	}

	public void setMonths(int months) {
		this.months = months;
	}

	public int getStorage() {
		return storage;
	}

	public void setStorage(int storage) {
		this.storage = storage;
	}

	public int getTransfer() {
		return transfer;
	}

	public void setTransfer(int transfer) {
		this.transfer = transfer;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public boolean isBusiness() {
		return isBusiness;
	}

	public void setBusiness(boolean isBusiness) {
		this.isBusiness = isBusiness;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		// TODO Auto-generated method stub
		out.writeLong(handle);
		out.writeInt(level);
		out.writeInt(months);
		out.writeInt(storage);
		out.writeInt(amount);
		out.writeInt(transfer);
		out.writeInt(isBusiness ? 1 : 0);
	}
}
