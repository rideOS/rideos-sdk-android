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
package ai.rideos.android.rider_app.on_trip.confirming_edit_pickup;

import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.EmptyArg;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.view.layout.BottomDetailAndButtonView;
import ai.rideos.android.rider_app.R;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;

public class ConfirmingEditPickupFragment extends FragmentViewController<EmptyArg, ConfirmingEditPickupListener> {
    @Override
    public ControllerTypes<EmptyArg, ConfirmingEditPickupListener> getTypes() {
        return new ControllerTypes<>(EmptyArg.class, ConfirmingEditPickupListener.class);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        return BottomDetailAndButtonView.inflateWithNoButton(inflater, container, R.layout.editing_pickup);
    }
}
