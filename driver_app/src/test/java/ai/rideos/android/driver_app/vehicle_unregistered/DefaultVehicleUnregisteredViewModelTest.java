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
package ai.rideos.android.driver_app.vehicle_unregistered;

import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.model.VehicleUnregisteredViewState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultVehicleUnregisteredViewModelTest {
    private DefaultVehicleUnregisteredViewModel viewModelUnderTest;

    @Before
    public void setUp() {
        viewModelUnderTest = new DefaultVehicleUnregisteredViewModel(
            Mockito.mock(DoneRegisteringVehicleListener.class),
            new TrampolineSchedulerProvider()
        );
        viewModelUnderTest.initialize();
    }

    @Test
    public void testViewStateInitializesToPreRegistration() {
        viewModelUnderTest.getViewState().test()
            .assertValueCount(1)
            .assertValueAt(0, VehicleUnregisteredViewState.PRE_REGISTRATION);
    }

    @Test
    public void testViewStateTransitionsFromPreRegistrationToRegisterVehicle() {
        viewModelUnderTest.startRegistration();
        viewModelUnderTest.getViewState().test()
            .assertValueCount(1)
            .assertValueAt(0, VehicleUnregisteredViewState.REGISTER_VEHICLE);
    }

    @Test
    public void testViewStateTransitionsFromRegisterVehicleToPreRegistration() {
        viewModelUnderTest.startRegistration();
        viewModelUnderTest.cancelRegistration();
        viewModelUnderTest.getViewState().test()
            .assertValueCount(1)
            .assertValueAt(0, VehicleUnregisteredViewState.PRE_REGISTRATION);
    }
}
