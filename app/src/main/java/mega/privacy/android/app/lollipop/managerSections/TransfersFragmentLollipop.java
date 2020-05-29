package mega.privacy.android.app.lollipop.managerSections;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.ListIterator;

import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.managerFragments.TransfersBaseFragment;
import mega.privacy.android.app.lollipop.adapters.MegaTransfersLollipopAdapter;
import nz.mega.sdk.MegaTransfer;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;


public class TransfersFragmentLollipop extends TransfersBaseFragment {

	private MegaTransfersLollipopAdapter adapter;

	private ArrayList<MegaTransfer> tL = new ArrayList<>();

	public static TransfersFragmentLollipop newInstance() {
		return new TransfersFragmentLollipop();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		super.onCreateView(inflater, container, savedInstanceState);

		View v = initView(inflater, container);

		emptyImage.setImageResource(isScreenInPortrait(context) ? R.drawable.ic_zero_portrait_transfers : R.drawable.ic_zero_landscape_saved_for_offline);

		String textToShow = context.getString(R.string.transfers_empty_new);
		try {
			textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
			textToShow = textToShow.replace("[/A]", "</font>");
			textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
			textToShow = textToShow.replace("[/B]", "</font>");
		} catch (Exception e) {
			logWarning("Exception formatting string", e);
		}
		emptyText.setText(getSpannedHtmlText(textToShow));

		setTransfers();

		adapter = new MegaTransfersLollipopAdapter(context, tL, listView);

		adapter.setMultipleSelect(false);
		listView.setAdapter(adapter);

		return v;
	}

	private void setTransfers() {
		tL.clear();

		for (int i = 0; i < managerActivity.transfersInProgress.size(); i++) {
			MegaTransfer transfer = megaApi.getTransferByTag(managerActivity.transfersInProgress.get(i));
			if (transfer != null && !transfer.isStreamingTransfer()) {
				tL.add(transfer);
			}
		}

		setEmptyView(tL.size());
	}

	public void transferUpdate(MegaTransfer transfer) {
		try {
			ListIterator li = tL.listIterator();
			int index = 0;
			while (li.hasNext()) {
				MegaTransfer next = (MegaTransfer) li.next();
				if (next != null && next.getTag() == transfer.getTag()) {
					index = li.previousIndex();
					break;
				}
			}
			tL.set(index, transfer);

			adapter.updateProgress(index, transfer);
		} catch (IndexOutOfBoundsException e) {
			logError("EXCEPTION", e);
		}
	}

	public void changeStatusButton(int tag) {
		logDebug("tag: " + tag);

		ListIterator li = tL.listIterator();
		int index = 0;
		while (li.hasNext()) {
			MegaTransfer next = (MegaTransfer) li.next();
			if (next.getTag() == tag) {
				index = li.previousIndex();
				break;
			}
		}
		MegaTransfer transfer = megaApi.getTransferByTag(tag);
		tL.set(index, transfer);
		logDebug("The transfer with index : " + index + "has been paused/resumed, left: " + tL.size());

		adapter.notifyItemChanged(index);
	}

	public void transferFinish(int transferTag) {
		for (int i = 0; i < tL.size(); i++) {
			MegaTransfer transfer = tL.get(i);
			if (transfer != null && transfer.getTag() == transferTag) {
				tL.remove(i);
				adapter.removeItemData(i);
				break;
			}
		}

		setEmptyView(tL.size());
	}

	public void transferStart(MegaTransfer transfer) {
		if (!transfer.isStreamingTransfer()) {
			tL.add(transfer);
		}

		adapter.notifyItemInserted(tL.size() - 1);
		setEmptyView(tL.size());
	}

	public boolean isEmpty() {
		return tL.isEmpty();
	}
}
