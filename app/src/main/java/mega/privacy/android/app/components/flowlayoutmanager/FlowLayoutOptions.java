/*
 *
 * https://github.com/xiaofeng-han/AndroidLibs/tree/master/flowlayoutmanager
 *
 * Copyright 2016 Xiaofeng Han
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package mega.privacy.android.app.components.flowlayoutmanager;

public class FlowLayoutOptions {
    public static final int ITEM_PER_LINE_NO_LIMIT = 0;
    public Alignment alignment = Alignment.LEFT;
    public int itemsPerLine = ITEM_PER_LINE_NO_LIMIT;

    public static FlowLayoutOptions clone(FlowLayoutOptions layoutOptions) {
        FlowLayoutOptions result = new FlowLayoutOptions();
        result.alignment = layoutOptions.alignment;
        result.itemsPerLine = layoutOptions.itemsPerLine;
        return result;
    }
}
