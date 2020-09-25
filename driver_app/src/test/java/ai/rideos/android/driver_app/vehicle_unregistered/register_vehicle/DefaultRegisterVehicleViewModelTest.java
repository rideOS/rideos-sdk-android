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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.reactive.CompletionResult;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import ai.rideos.android.model.VehicleRegistration;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultRegisterVehicleViewModelTest {
    private static final String VEHICLE_ID = "vehicle-1";
    private static final String FLEET_ID = "fleet-1";

    private DefaultRegisterVehicleViewModel viewModelUnderTest;
    private DriverVehicleInteractor vehicleInteractor;
    private RegisterVehicleListener listener;

    @Before
    public void setUp() {
        listener = mock(RegisterVehicleListener.class);
        vehicleInteractor = mock(DriverVehicleInteractor.class);

        final User user = mock(User.class);
        when(user.getId()).thenReturn(VEHICLE_ID);

        viewModelUnderTest = new DefaultRegisterVehicleViewModel(
            vehicleInteractor,
            user,
            Observable.just(new FleetInfo(FLEET_ID)),
            listener,
            number -> number.length() > 0,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testSavingIsDisabledByDefault() {
        viewModelUnderTest.isSavingEnabled().test()
            .assertValueCount(1)
            .assertValueAt(0, false);
    }

    @Test
    public void testSavingIsEnabledWhenAllFieldsAreValidated() {
        viewModelUnderTest.setPreferredName("Driver");
        viewModelUnderTest.setPhoneNumber("1234567890");
        viewModelUnderTest.setLicensePlate("abc123");
        viewModelUnderTest.setRiderCapacity(4);
        viewModelUnderTest.isSavingEnabled().test()
            .assertValueAt(0, true);
    }

    @Test
    public void testSavingIsDisabledAfterBeingEnabled() {
        viewModelUnderTest.setPreferredName("Driver");
        viewModelUnderTest.setPhoneNumber("1234567890");
        viewModelUnderTest.setLicensePlate("abc123");
        viewModelUnderTest.setRiderCapacity(4);

        final TestObserver<Boolean> enabledObserver = viewModelUnderTest.isSavingEnabled().test();
        enabledObserver.assertValueAt(0, true);

        viewModelUnderTest.setLicensePlate("");
        enabledObserver.assertValueAt(1, false);
    }

    @Test
    public void testSavingRegistrationFailsIfFieldsAreInvalid() {
        viewModelUnderTest.save();
        Mockito.verifyNoMoreInteractions(listener, vehicleInteractor);
    }

    @Test
    public void testSavingRegistrationSucceedsIfAllFieldsAreValid() {
        when(vehicleInteractor.createVehicle(anyString(), anyString(), any()))
            .thenReturn(Completable.complete());
        viewModelUnderTest.setPreferredName("Driver");
        viewModelUnderTest.setPhoneNumber("1234567890");
        viewModelUnderTest.setLicensePlate("abc123");
        viewModelUnderTest.setRiderCapacity(4);

        AtomicReference<CompletionResult<Integer>> value = new AtomicReference();
        viewModelUnderTest.save().subscribe(result -> value.set(result));
        verify(vehicleInteractor).createVehicle(VEHICLE_ID, FLEET_ID, new VehicleRegistration(
            "Driver",
            "1234567890",
            "abc123",
            4
        ));
        verify(listener).doneRegistering();
        assertTrue(value.get().isSuccess());
    }

    @Test
    public void testSavingRegistrationFailure_backendErrorFleetNotRegistered() {
        when(vehicleInteractor.createVehicle(anyString(), anyString(), any()))
                .thenReturn(Completable.error(newFleetNotFoundError()));
        viewModelUnderTest.setPreferredName("Driver");
        viewModelUnderTest.setPhoneNumber("1234567890");
        viewModelUnderTest.setLicensePlate("abc123");
        viewModelUnderTest.setRiderCapacity(4);

        final AtomicReference<CompletionResult<Integer>> value = new AtomicReference();
        viewModelUnderTest.save().subscribe(result -> value.set(result));
        verify(vehicleInteractor)
                .createVehicle(VEHICLE_ID, FLEET_ID, new VehicleRegistration(
                        "Driver",
                        "1234567890",
                        "abc123",
                        4
                ));
        verify(listener, never()).doneRegistering();
        assertTrue(value.get().isFailure());
        assertEquals(R.string.error_msg_vehicle_reg_invalid_fleet, (int) value.get().getFailureInfo());
    }

    @Test
    public void testConvertErrorToErrMsgId_invalidFleetErrorCase() {
        assertEquals(
                R.string.error_msg_vehicle_reg_invalid_fleet,
                viewModelUnderTest.convertErrorToErrMsgId(newFleetNotFoundError()));
    }

    @Test
    public void testConvertErrorToErrMsgId_networkErrorCase() {
        assertEquals(
                R.string.error_msg_no_network,
                viewModelUnderTest.convertErrorToErrMsgId(newNetworkError()));
    }

    @Test
    public void testConvertErrorToErrMsgId_defaultErrorCase() {
        assertEquals(
                R.string.error_msg_unknown,
                viewModelUnderTest.convertErrorToErrMsgId(new RuntimeException("kdjf")));
    }

    private Throwable newFleetNotFoundError() {
        // Actual exception looks like:
        // Caused by: io.grpc.StatusRuntimeException: FAILED_PRECONDITION: FAILED_PRECONDITION: Fleet ID fleet-1 does not exist for this partner
        String msg = String.format("Fleet ID %s does not exist for this partner", FLEET_ID);
        return createGrpcException(Status.FAILED_PRECONDITION, msg);
    }

    private Throwable newInvalidIdError() {
        // Actual exception looks like:
        // java.util.concurrent.ExecutionException: io.grpc.StatusRuntimeException: INVALID_ARGUMENT: INVALID_ARGUMENT: Field(s) should not be empty: [CreateVehicleRequest.id]
        String msg = "Field(s) should not be empty: [CreateVehicleRequest.id]";
        return createGrpcException(Status.INVALID_ARGUMENT, msg);
    }

    private Throwable newNetworkError() {
        // Actual exception looks like:
        // java.util.concurrent.ExecutionException: io.grpc.StatusRuntimeException: UNAVAILABLE: Unable to resolve host gapi.rideos.ai
        String msg = "Unable to resolve host gapi.rideos.ai";
        return createGrpcException(Status.UNAVAILABLE, msg);
    }

    private Throwable createGrpcException(Status status, String msg) {
        return new ExecutionException(new StatusRuntimeException(status.withDescription(msg)));
    }
}
