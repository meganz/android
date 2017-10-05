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

public class LayoutContext {
    public FlowLayoutOptions layoutOptions;
    public int currentLineItemCount;

    public static LayoutContext clone(LayoutContext layoutContext) {
        LayoutContext resultContext = new LayoutContext();
        resultContext.currentLineItemCount = layoutContext.currentLineItemCount;
        resultContext.layoutOptions = FlowLayoutOptions.clone(layoutContext.layoutOptions);
        return resultContext;
    }

    public static LayoutContext fromLayoutOptions(FlowLayoutOptions layoutOptions) {
        LayoutContext layoutContext = new LayoutContext();
        layoutContext.layoutOptions = layoutOptions;
        return layoutContext;
    }
}