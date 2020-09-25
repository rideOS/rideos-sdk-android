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
package ai.rideos.android.driver_app.vehicle_unregistered.register_vehicle;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.reactive.CompletionResult;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import ai.rideos.android.model.VehicleRegistration;
import android.telephony.PhoneNumberUtils;
import androidx.annotation.VisibleForTesting;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import java.util.function.Function;
import timber.log.Timber;

public class DefaultRegisterVehicleViewModel implements RegisterVehicleViewModel {

    private final BehaviorSubject<String> nameSubject = BehaviorSubject.createDefault("");
    private final BehaviorSubject<String> phoneSubject = BehaviorSubject.createDefault("");
    private final BehaviorSubject<String> licenseSubject = BehaviorSubject.createDefault("");
    private final BehaviorSubject<Integer> capacitySubject = BehaviorSubject.createDefault(0);

    private final DriverVehicleInteractor vehicleInteractor;
    private final User user;
    private final Observable<FleetInfo> observableFleet;
    private final RegisterVehicleListener registerVehicleListener;
    // Phone number validator is injected because the static isGlobalPhoneNumber method must be mocked in testing
    private final Function<String, Boolean> phoneNumberValidator;
    private final SchedulerProvider schedulerProvider;

    public DefaultRegisterVehicleViewModel(final DriverVehicleInteractor vehicleInteractor,
                                           final User user,
                                           final Observable<FleetInfo> observableFleet,
                                           final RegisterVehicleListener registerVehicleListener) {
        this(
            vehicleInteractor,
            user,
            observableFleet,
            registerVehicleListener,
            PhoneNumberUtils::isGlobalPhoneNumber,
            new DefaultSchedulerProvider()
        );
    }

    public DefaultRegisterVehicleViewModel(final DriverVehicleInteractor vehicleInteractor,
                                           final User user,
                                           final Observable<FleetInfo> observableFleet,
                                           final RegisterVehicleListener registerVehicleListener,
                                           final Function<String, Boolean> phoneNumberValidator,
                                           final SchedulerProvider schedulerProvider) {
        this.vehicleInteractor = vehicleInteractor;
        this.user = user;
        this.observableFleet = observableFleet;
        this.registerVehicleListener = registerVehicleListener;
        this.phoneNumberValidator = phoneNumberValidator;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public Observable<Boolean> isSavingEnabled() {
        return Observable.combineLatest(
            nameSubject.observeOn(schedulerProvider.io()),
            phoneSubject.observeOn(schedulerProvider.io()),
            licenseSubject.observeOn(schedulerProvider.io()),
            capacitySubject.observeOn(schedulerProvider.io()),
            VehicleRegistration::new
        )
            .observeOn(schedulerProvider.computation())
            .map(this::isVehicleRegistrationValid);
    }

    @Override
    public Single<CompletionResult<Integer>> save() {
        return observableFleet.firstOrError()
                .flatMapCompletable(fleetInfo -> {
                    final VehicleRegistration registration = new VehicleRegistration(
                        nameSubject.getValue(),
                        phoneSubject.getValue(),
                        licenseSubject.getValue(),
                        capacitySubject.getValue()
                    );
                    // If fields become invalid between when saving is enabled and save is clicked, log
                    // an error and move on. The saving button will disappear.
                    if (!isVehicleRegistrationValid(registration)) {
                        return Completable.error(new IllegalArgumentException("Invalid registration"));
                    }
                    return vehicleInteractor
                            .createVehicle(user.getId(), fleetInfo.getId(), registration);
                })
                .doOnComplete(registerVehicleListener::doneRegistering)
                .toSingleDefault(CompletionResult.<Integer>success())
                .onErrorReturn(e -> {
                    Timber.e(e, "Failed to create vehicle %s", user);
                    return CompletionResult.failure(e, convertErrorToErrMsgId(e));
                });
    }

    @Override
    public void setPreferredName(final String preferredName) {
        nameSubject.onNext(preferredName);
    }

    @Override
    public void setPhoneNumber(final String phoneNumber) {
        phoneSubject.onNext(phoneNumber);
    }

    @Override
    public void setLicensePlate(final String licensePlate) {
        licenseSubject.onNext(licensePlate);
    }

    @Override
    public void setRiderCapacity(final int riderCapacity) {
        capacitySubject.onNext(riderCapacity);
    }

    @Override
    public void destroy() { }

    private boolean isVehicleRegistrationValid(final VehicleRegistration registration) {
        return registration.getPreferredName().length() > 0
            && phoneNumberValidator.apply(registration.getPhoneNumber())
            && registration.getLicensePlate().length() > 0
            && registration.getRiderCapacity() > 0;
    }

    @VisibleForTesting
    int convertErrorToErrMsgId(Throwable e) {

        String networkErrorRegex = ".*io.grpc.StatusRuntimeException: UNAVAILABLE.*";
        String fleetDoesNotExistRegex = ".*Fleet ID .* does not exist for this partner.*";

        String errMsg = e.getMessage();
        if (errMsg.matches(networkErrorRegex)) {
            return R.string.error_msg_no_network;
        } else if (errMsg.matches(fleetDoesNotExistRegex)) {
            return R.string.error_msg_vehicle_reg_invalid_fleet;
        }

        return R.string.error_msg_unknown;
    }
}
