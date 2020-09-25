/**
 * Copyright 2018-2019 rideOS, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.rideos.android.common.view;

import ai.rideos.android.common.viewmodel.BackListener;
import android.view.View;
import androidx.annotation.CallSuper;

/**
 * ParentPresenter defines a Presenter that controls other Presenter in a hierarchy. The parent
 * may hold one child at a time.
 *
 * TODO remove and replace with a coordinator
 * @param <P> Parent view
 */
public abstract class ParentPresenter<P extends View> implements Presenter<P> {
    private Presenter<? extends View> child;

    /**
     * A back signal can originate from one of 2 places: the phone's back button, or some UI button in the view. When
     * the signal originates from the phone, it has to be propagated from the top-level activity. To do this, the back
     * signal is sent from parent to child until the last parent. Then, it is sent to the corresponding view model back
     * listener.
     */
    public void propagateBackSignalToChild(final BackListener onBackNotHandled) {
        if (child instanceof BackPropagator) {
            ((BackPropagator) child).propagateBackSignal();
        } else {
            onBackNotHandled.back();
        }
    }

    // TODO consider breaking this up into 2 replaceChild and attach methods for readability.
    protected <C extends View> void replaceChildAndAttach(final Presenter<C> childController, final C childView) {
        detachChild();
        child = childController;
        childController.attach(childView);
    }

    protected void detachChild() {
        if (child != null) {
            child.detach();
            child = null;
        }
    }

    protected Presenter<? extends View> getChild() {
        return child;
    }

    @Override
    @CallSuper
    public void detach() {
        detachChild();
    }
}
