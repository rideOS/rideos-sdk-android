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
package ai.rideos.android.rider_app.on_trip.current_trip.driving_to_drop_off;

import ai.rideos.android.common.app.map.MapRelay;
import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.model.VehicleInfo;
import ai.rideos.android.common.view.layout.BottomDetailAndButtonView;
import ai.rideos.android.common.view.resources.AndroidResourceProvider;
import ai.rideos.android.model.TripStateModel;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.on_trip.current_trip.PassengerStateObserver;
import ai.rideos.android.rider_app.on_trip.current_trip.VehicleInfoView;
import ai.rideos.android.rider_app.on_trip.current_trip.driving_to_drop_off.DrivingToDropOffFragment.DrivingToDropOffArgs;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import java.io.Serializable;

public class DrivingToDropOffFragment extends FragmentViewController<DrivingToDropOffArgs, DrivingToDropOffListener> implements PassengerStateObserver {
    public static class DrivingToDropOffArgs implements Serializable {
        private final NamedTaskLocation dropOff;
        @Nullable
        private final VehicleInfo vehicleInfo;

        public DrivingToDropOffArgs(final NamedTaskLocation dropOff,
                                    @Nullable final VehicleInfo vehicleInfo) {
            this.dropOff = dropOff;
            this.vehicleInfo = vehicleInfo;
        }
    }

    private DrivingToDropOffViewModel viewModel;
    private CompositeDisposable compositeDisposable;

    @Override
    public ControllerTypes<DrivingToDropOffArgs, DrivingToDropOffListener> getTypes() {
        return new ControllerTypes<>(DrivingToDropOffArgs.class, DrivingToDropOffListener.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new DefaultDrivingToDropOffViewModel(
            AndroidResourceProvider.forContext(getContext()),
            DateFormat.getTimeFormat(getContext())
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = BottomDetailAndButtonView.inflateWithMenuButton(inflater, container, getActivity(), R.layout.driving_to_drop_off);
        final TextView dropOffText = view.findViewById(R.id.drop_off_address_text);
        dropOffText.setText(getArgs().dropOff.getDisplayName());
        ((VehicleInfoView) view.findViewById(R.id.vehicle_info_container)).setVehicleInfo(getArgs().vehicleInfo);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        final TextView detailText = getView().findViewById(R.id.on_trip_state_title);
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(
            viewModel.getRouteDetailText().observeOn(AndroidSchedulers.mainThread())
                .subscribe(detailText::setText)
        );

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
