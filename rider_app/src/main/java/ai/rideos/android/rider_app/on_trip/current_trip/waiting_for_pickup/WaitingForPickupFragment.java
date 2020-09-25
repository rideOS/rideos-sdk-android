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
package ai.rideos.android.rider_app.on_trip.current_trip.waiting_for_pickup;

import ai.rideos.android.common.app.map.MapRelay;
import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.model.VehicleInfo;
import ai.rideos.android.common.view.layout.BottomDetailAndButtonView;
import ai.rideos.android.common.view.resources.AndroidResourceProvider;
import ai.rideos.android.model.TripStateModel;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.on_trip.current_trip.CancelDialog;
import ai.rideos.android.rider_app.on_trip.current_trip.PassengerStateObserver;
import ai.rideos.android.rider_app.on_trip.current_trip.VehicleInfoView;
import ai.rideos.android.rider_app.on_trip.current_trip.driving_to_pickup.DefaultDrivingToPickupViewModel;
import ai.rideos.android.rider_app.on_trip.current_trip.driving_to_pickup.DrivingToPickupViewModel;
import ai.rideos.android.rider_app.on_trip.current_trip.waiting_for_pickup.WaitingForPickupFragment.WaitingForPickupArgs;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import io.reactivex.disposables.CompositeDisposable;
import java.io.Serializable;

public class WaitingForPickupFragment extends FragmentViewController<WaitingForPickupArgs, WaitingForPickupListener> implements PassengerStateObserver {
    public static class WaitingForPickupArgs implements Serializable {
        private final NamedTaskLocation pickup;
        @Nullable
        private final VehicleInfo vehicleInfo;

        public WaitingForPickupArgs(final NamedTaskLocation pickup,
                                    @Nullable final VehicleInfo vehicleInfo) {
            this.pickup = pickup;
            this.vehicleInfo = vehicleInfo;
        }
    }

    private DrivingToPickupViewModel viewModel;
    private CompositeDisposable compositeDisposable;

    @Override
    public ControllerTypes<WaitingForPickupArgs, WaitingForPickupListener> getTypes() {
        return new ControllerTypes<>(WaitingForPickupArgs.class, WaitingForPickupListener.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new DefaultDrivingToPickupViewModel(AndroidResourceProvider.forContext(getContext()));
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = BottomDetailAndButtonView.inflateWithMenuButton(inflater, container, getActivity(), R.layout.waiting_for_pickup);
        final TextView pickupText = view.findViewById(R.id.pickup_address_text);
        pickupText.setText(getArgs().pickup.getDisplayName());
        ((VehicleInfoView) view.findViewById(R.id.vehicle_info_container)).setVehicleInfo(getArgs().vehicleInfo);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        final View view = getView();
        final WaitingForPickupListener listener = getListener();
        compositeDisposable = new CompositeDisposable();
        final Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(click -> CancelDialog.showDialog(
            getContext(),
            R.string.waiting_for_pickup_cancel_message,
            listener::cancelTrip
        ));

        final AppCompatImageView editButton = view.findViewById(R.id.edit_button);
        editButton.setOnClickListener(click -> listener.changePickup());

        compositeDisposable.add(MapRelay.get().connectToProvider(viewModel));
    }

    @Override
    public void onStop() {
        super.onStop();
        compositeDisposable.dispose();
    }

    @Override
    public void updatePassengerState(final TripStateModel passengerState) {
        if (viewModel != null) {
            viewModel.updatePassengerState(passengerState);
        }
    }
}
