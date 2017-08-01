/*
 *  Customized RecyclerView based on:
 *
 *  https://github.com/chiuki/android-recyclerview/blob/master/app/src/main/java/com/sqisland/android/recyclerview/AutofitRecyclerView.java
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with the License. You may obtain a copy of the License in the LICENSE file, or at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package mega.privacy.android.app.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import mega.privacy.android.app.utils.Util;

public class CustomizedGridRecyclerView extends RecyclerView {

	private CustomizedGridLayoutManager manager;
	private int columnWidth = -1;
	private CustomizedGridRecyclerItemDecoration itemDecoration;

	public CustomizedGridRecyclerView(Context context) {
		super(context);
		init(context, null);
	}

	public CustomizedGridRecyclerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public CustomizedGridRecyclerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		if (attrs != null) {
			int[] attrsArray = {
					android.R.attr.columnWidth
			};
			TypedArray array = context.obtainStyledAttributes(attrs, attrsArray);
			columnWidth = array.getDimensionPixelSize(0, -1);
			array.recycle();
		}

		manager = new CustomizedGridLayoutManager(getContext(), 1);
		setLayoutManager(manager);
	}

	@Override
	protected void onMeasure(int widthSpec, int heightSpec) {
		super.onMeasure(widthSpec, heightSpec);
		if (columnWidth > 0) {
			int spanCount = Math.max(1, getMeasuredWidth() / columnWidth);
			if(itemDecoration != null){
				this.removeItemDecoration(itemDecoration);
				itemDecoration = null;
			}
			itemDecoration = new CustomizedGridRecyclerItemDecoration(spanCount, columnWidth, getMeasuredWidth());
			this.addItemDecoration(itemDecoration);
			manager.setSpanCount(spanCount);
		}
	}

	public int findFirstCompletelyVisibleItemPosition() {
		return getLayoutManager().findFirstCompletelyVisibleItemPosition();
	}

	public int findFirstVisibleItemPosition() {
		return getLayoutManager().findFirstVisibleItemPosition();
	}

	@Override
	public CustomizedGridLayoutManager getLayoutManager() {
		return manager;
	}

	private class CustomizedGridRecyclerItemDecoration extends RecyclerView.ItemDecoration{

		private int spanCount = 0;
		private int columnWidth = 0;
		private int totalWidth = 0;
		private double difference = 0;

		public CustomizedGridRecyclerItemDecoration(int spanCount, int columnWidth, int totalWidth) {
			this.spanCount = spanCount;
			this.columnWidth = columnWidth;
			this.totalWidth = totalWidth;
			log("ItemDecoration: spanCount-> "+spanCount);

			int difference = totalWidth % columnWidth;
			this.difference = difference / ((spanCount + 1) * 2);
		}

		@Override
		public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
			int position = parent.getChildAdapterPosition(view); // item position
			int column = position % spanCount; // item column



			outRect.left = (int) difference;
			outRect.right = (int) difference;

//			if(column == 0){
//				outRect.left = difference;
////			}
//			if(column == spanCount - 1){
//				outRect.right = difference * 2;
//			}


			outRect.top = 0; // item top
			outRect.bottom = 0; // item bottom
		}
	}

	private static void log(String txt){
		Util.log("CustomizedGridRecyclerView", txt);
	}
}