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
package ai.rideos.android.rider_app.pre_trip.requesting_trip;

import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.view.layout.BottomDetailAndButtonView;
import ai.rideos.android.common.view.layout.LoadableDividerView;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.pre_trip.requesting_trip.RequestingTripFragment.RequestingTripArgs;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.io.Serializable;

public class RequestingTripFragment extends FragmentViewController<RequestingTripArgs, RequestingTripListener> {
    public static class RequestingTripArgs implements Serializable {
        private final NamedTaskLocation pickup;
        private final NamedTaskLocation dropOff;

        public RequestingTripArgs(final NamedTaskLocation pickup,
                                  final NamedTaskLocation dropOff) {
            this.pickup = pickup;
            this.dropOff = dropOff;
        }
    }

    @Override
    public ControllerTypes<RequestingTripArgs, RequestingTripListener> getTypes() {
        return new ControllerTypes<>(RequestingTripArgs.class, RequestingTripListener.class);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = BottomDetailAndButtonView.inflateWithNoButton(inflater, container, R.layout.waiting_for_assignment);
        final TextView pickupText = view.findViewById(R.id.pickup_address_text);
        final TextView dropOffText = view.findViewById(R.id.drop_off_address_text);
        pickupText.setText(getArgs().pickup.getDisplayName());
        dropOffText.setText(getArgs().dropOff.getDisplayName());
        final LoadableDividerView loadableDivider = view.findViewById(R.id.loadable_divider);
        loadableDivider.startLoading();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        final Button cancelButton = getView().findViewById(R.id.cancel_button);
        final RequestingTripListener listener = getListener();
        cancelButton.setOnClickListener(click -> listener.cancelTripRequest());
    }
}
