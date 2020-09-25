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
package ai.rideos.android.driver_app.online.driving.drive_pending;

import ai.rideos.android.common.app.map.MapRelay;
import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.view.resources.AndroidResourceProvider;
import ai.rideos.android.device.PotentiallySimulatedDeviceLocator;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.driver_app.dependency.DriverDependencyRegistry;
import ai.rideos.android.driver_app.online.driving.drive_pending.DrivePendingFragment.DrivePendingArgs;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import ai.rideos.android.view.HeaderAndBottomDetailView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import java.io.Serializable;

public class DrivePendingFragment extends FragmentViewController<DrivePendingArgs, DrivePendingListener> {
    public static class DrivePendingArgs implements Serializable {
        private final int passengerDetailTemplate;
        private final int drawableDestinationPinAttr;
        private final Waypoint nextWaypoint;

        public DrivePendingArgs(@StringRes final int passengerDetailTemplate,
                                @AttrRes final int drawableDestinationPinAttr,
                                final Waypoint nextWaypoint) {
            this.passengerDetailTemplate = passengerDetailTemplate;
            this.drawableDestinationPinAttr = drawableDestinationPinAttr;
            this.nextWaypoint = nextWaypoint;
        }
    }

    private CompositeDisposable compositeDisposable;
    private DrivePendingViewModel drivingViewModel;

    @Override
    public ControllerTypes<DrivePendingArgs, DrivePendingListener> getTypes() {
        return new ControllerTypes<>(DrivePendingArgs.class, DrivePendingListener.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        drivingViewModel = new DefaultDrivePendingViewModel(
            new PotentiallySimulatedDeviceLocator(getContext()),
            DriverDependencyRegistry.driverDependencyFactory().getRouteInteractor(getContext()),
            DriverDependencyRegistry.mapDependencyFactory().getGeocodeInteractor(getContext()),
            AndroidResourceProvider.forContext(getContext()),
            getArgs().nextWaypoint,
            AndroidResourceProvider.forContext(getContext()).getDrawableId(getArgs().drawableDestinationPinAttr),
            getArgs().passengerDetailTemplate
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = HeaderAndBottomDetailView.inflate(
            inflater,
            container,
            getActivity(),
            R.layout.on_trip_header,
            R.layout.on_trip_bottom_detail
        );

        final Button startNavButton = view.findViewById(R.id.on_trip_action_button);
        startNavButton.setText(R.string.start_navigation_button_text);
        return view;

    }

    @Override
    public void onStart() {
        super.onStart();
        final View view = getView();

        final Button startNavButton = view.findViewById(R.id.on_trip_action_button);
        startNavButton.setOnClickListener(click -> getListener().startNavigation());

        final TextView passengerDetail = view.findViewById(R.id.on_trip_passenger_detail);
        passengerDetail.setText(drivingViewModel.getPassengerDetailText());

        final View expandDetailsButton = view.findViewById(R.id.passenger_detail_row);
        expandDetailsButton.setOnClickListener(click -> getListener().openTripDetails());

        final TextView onTripAddress = view.findViewById(R.id.on_trip_address);

        final Disposable mapSubscription = MapRelay.get().connectToProvider(drivingViewModel);

        final Disposable detailSubscription = drivingViewModel.getDestinationAddress()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(onTripAddress::setText);

        compositeDisposable = new CompositeDisposable();
        compositeDisposable.addAll(mapSubscription, detailSubscription);
    }

    @Override
    public void onStop() {
        super.onStop();
        compositeDisposable.dispose();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        drivingViewModel.destroy();
    }
}
