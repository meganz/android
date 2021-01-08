package mega.privacy.android.app.lollipop.managerSections;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.ListIterator;

import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.managerFragments.TransfersBaseFragment;
import mega.privacy.android.app.lollipop.adapters.MegaTransfersLollipopAdapter;
import mega.privacy.android.app.utils.ColorUtils;
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

		emptyImage.setImageResource(isScreenInPortrait(context) ? R.drawable.empty_transfer_portrait : R.drawable.empty_transfer_landscape);

		String textToShow = context.getString(R.string.transfers_empty_new);
		try {
			textToShow = textToShow.replace("[A]", "<font color=\'"
					+ ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
					+ "\'>");
			textToShow = textToShow.replace("[/A]", "</font>");
			textToShow = textToShow.replace("[B]", "<font color=\'"
					+ ColorUtils.getColorHexString(context, R.color.grey_300_grey_600)
					+ "\'>");
			textToShow = textToShow.replace("[/B]", "</font>");
		} catch (Exception e) {
			logWarning("Exception formatting string", e);
		}
		emptyText.setText(HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY));

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

	/**
	 * Updates the state of a transfer.
	 *
	 * @param transfer	transfer to update
	 */
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

	/**
	 * Changes the status (play/pause) of the button of a transfer.
	 *
	 * @param tag	identifier of the transfer to change the status of the button
	 */
	public void changeStatusButton(int tag) {
		logDebug("tag: " + tag);

		ListIterator li = tL.listIterator();
		int index = 0;
		while (li.hasNext()) {
			MegaTransfer next = (MegaTransfer) li.next();
			if (next == null) continue;

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

	/**
	 * Removes a transfer when finishes.
	 *
	 * @param transferTag	identifier of the transfer to remove
	 */
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

		if (tL.isEmpty()) {
			managerActivity.supportInvalidateOptionsMenu();
		}
	}

	/**
	 * Adds a transfer when starts.
	 *
	 * @param transfer	transfer to add
	 */
	public void transferStart(MegaTransfer transfer) {
		if (transfer.isStreamingTransfer()) {
			return;
		}

		if (tL.isEmpty()) {
			managerActivity.supportInvalidateOptionsMenu();
		}

		tL.add(transfer);
		adapter.notifyItemInserted(tL.size() - 1);
		setEmptyView(tL.size());
	}

	/**
	 * Checks if there is any transfer in progress.
	 *
	 * @return True if there is not any transfer, false otherwise.
	 */
	public boolean isEmpty() {
		return tL.isEmpty();
	}
}
