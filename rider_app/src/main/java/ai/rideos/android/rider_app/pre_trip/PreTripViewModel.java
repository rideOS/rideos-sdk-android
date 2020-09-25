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
package ai.rideos.android.rider_app.pre_trip;

import ai.rideos.android.common.reactive.Notification;
import ai.rideos.android.common.viewmodel.BackListener;
import ai.rideos.android.common.viewmodel.ViewModel;
import ai.rideos.android.model.PreTripState;
import ai.rideos.android.rider_app.pre_trip.confirm_location.ConfirmLocationListener;
import ai.rideos.android.rider_app.pre_trip.confirm_trip.ConfirmTripListener;
import ai.rideos.android.rider_app.pre_trip.confirm_vehicle.ConfirmVehicleListener;
import ai.rideos.android.rider_app.pre_trip.requesting_trip.RequestingTripListener;
import ai.rideos.android.rider_app.pre_trip.select_pickup_drop_off.SetPickupDropOffListener;
import io.reactivex.Observable;

/**
 * PreTripViewModel handles the state machine for requesting a trip. It exposes numerous event methods for a view to
 * call to advance the state like setting or confirming pickup/drop-off locations. It also exposes an observable model
 * representing the current state of the system, i.e. what step in the process the rider is in.
 */
public interface PreTripViewModel extends ViewModel, BackListener, SetPickupDropOffListener,
    ConfirmTripListener, ConfirmLocationListener, ConfirmVehicleListener, RequestingTripListener {

    void initialize();

    void cancelTripRequest();

    Observable<PreTripState> getPreTripState();
    
    Observable<Notification> getTripCreationFailures();
}
