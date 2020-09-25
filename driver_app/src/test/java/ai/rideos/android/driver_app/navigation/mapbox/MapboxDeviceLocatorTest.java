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
package ai.rideos.android.driver_app.navigation.mapbox;

import static org.mockito.Matchers.any;

import ai.rideos.android.common.device.PermissionsChecker;
import ai.rideos.android.common.device.PermissionsNotGrantedException;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import android.location.Location;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineResult;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class MapboxDeviceLocatorTest {
    private static final int POLL_INTERVAL_MILLIS = 100;

    private LocationEngine locationEngine;
    private MapboxDeviceLocator locatorUnderTest;
    private PermissionsChecker permissionsChecker;

    @Before
    public void setUp() {
        locationEngine = Mockito.mock(LocationEngine.class);
        permissionsChecker = Mockito.mock(PermissionsChecker.class);
        locatorUnderTest = new MapboxDeviceLocator(
            locationEngine,
            permissionsChecker,
            new TrampolineSchedulerProvider()
        );
        Mockito.when(permissionsChecker.areLocationPermissionsGranted()).thenReturn(true);
    }

    @Test
    public void testObserveErrorWhenPermissionsNotGranted() {
        Mockito.when(permissionsChecker.areLocationPermissionsGranted()).thenReturn(false);
        locatorUnderTest.observeCurrentLocation(POLL_INTERVAL_MILLIS).test()
            .assertValueCount(0)
            .assertError(error -> error instanceof PermissionsNotGrantedException);
    }

    @Test
    public void testLocatorRequestsLocationUpdatesWhenObserverSubscribes() {
        final Observable<LocationAndHeading> observableLocation = locatorUnderTest.observeCurrentLocation(POLL_INTERVAL_MILLIS);
        Mockito.verifyNoMoreInteractions(locationEngine);
        observableLocation.subscribeOn(Schedulers.trampoline()).subscribe();
        Mockito.verify(locationEngine).requestLocationUpdates(any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCanReceiveLocationUpdatesAfterSubscription() {
        Class<LocationEngineCallback<LocationEngineResult>> callbackClass = (Class) LocationEngineCallback.class;
        final ArgumentCaptor<LocationEngineCallback<LocationEngineResult>> callbackCaptor = ArgumentCaptor.forClass(callbackClass);

        final TestObserver<LocationAndHeading> testObserver = locatorUnderTest.observeCurrentLocation(POLL_INTERVAL_MILLIS)
            .subscribeOn(Schedulers.trampoline())
            .test();

        Mockito.verify(locationEngine).requestLocationUpdates(any(), callbackCaptor.capture(), any());
        final LocationEngineCallback<LocationEngineResult> callback = callbackCaptor.getValue();

        final LocationAndHeading expectedLocation = new LocationAndHeading(new LatLng(0, 1), 10.0f);
        final Location mockLocation = Mockito.mock(Location.class);
        Mockito.when(mockLocation.getLatitude()).thenReturn(expectedLocation.getLatLng().getLatitude());
        Mockito.when(mockLocation.getLongitude()).thenReturn(expectedLocation.getLatLng().getLongitude());
        Mockito.when(mockLocation.getBearing()).thenReturn(expectedLocation.getHeading());
        callback.onSuccess(LocationEngineResult.create(Collections.singletonList(mockLocation)));

        testObserver.assertValueCount(1)
            .assertValueAt(0, expectedLocation);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLocatorStopsLocationUpdatesWhenSubscriptionIsDisposed() {
        final Observable<LocationAndHeading> observableLocation = locatorUnderTest.observeCurrentLocation(POLL_INTERVAL_MILLIS);
        final Disposable disposable = observableLocation.subscribeOn(Schedulers.trampoline()).subscribe();
        disposable.dispose();

        Class<LocationEngineCallback<LocationEngineResult>> callbackClass = (Class) LocationEngineCallback.class;
        Mockito.verify(locationEngine).removeLocationUpdates(any(callbackClass));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLastKnownLocationWhenLocationExists() {
        Class<LocationEngineCallback<LocationEngineResult>> callbackClass = (Class) LocationEngineCallback.class;
        final ArgumentCaptor<LocationEngineCallback<LocationEngineResult>> engineCallbackCaptor
            = ArgumentCaptor.forClass(callbackClass);

        final Location mockLocation = mockLocation(new LatLng(0, 1), 10.0f);

        final TestObserver<LocationAndHeading> testObserver = locatorUnderTest.getLastKnownLocation().test();

        Mockito.verify(locationEngine).getLastLocation(engineCallbackCaptor.capture());
        engineCallbackCaptor.getValue().onSuccess(LocationEngineResult.create(Collections.singletonList(mockLocation)));

        testObserver.assertValueCount(1)
            .assertValueAt(0, new LocationAndHeading(new LatLng(0, 1), 10.0f));
        Mockito.verify(locationEngine, Mockito.never()).requestLocationUpdates(any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLastKnownLocationFallsBackToLocationUpdatesWhenLastLocationUnknown() {
        Class<LocationEngineCallback<LocationEngineResult>> callbackClass = (Class) LocationEngineCallback.class;
        final ArgumentCaptor<LocationEngineCallback<LocationEngineResult>> engineCallbackCaptor
            = ArgumentCaptor.forClass(callbackClass);

        final Location mockLocation = mockLocation(new LatLng(0, 1), 10.0f);

        // Try to get last known location
        final TestObserver<LocationAndHeading> testObserver = locatorUnderTest.getLastKnownLocation().test();

        // Send empty result
        Mockito.verify(locationEngine).getLastLocation(engineCallbackCaptor.capture());
        engineCallbackCaptor.getValue().onSuccess(LocationEngineResult.create(Collections.emptyList()));
        // Assert that no value should appear in the observer
        testObserver.assertValueCount(0);

        final ArgumentCaptor<LocationEngineCallback<LocationEngineResult>> updateCallbackCaptor
            = ArgumentCaptor.forClass(callbackClass);

        // Assert that location updates have started
        Mockito.verify(locationEngine).requestLocationUpdates(any(), updateCallbackCaptor.capture(), any());
        // Get the location update callback and send a location
        updateCallbackCaptor.getValue().onSuccess(LocationEngineResult.create(Collections.singletonList(mockLocation)));

        // Assert that this location is received
        testObserver.assertValueCount(1)
            .assertValueAt(0, new LocationAndHeading(new LatLng(0, 1), 10.0f));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLastKnownLocationFallsBackToLocationUpdatesWhenLastLocationErrors() {
        Class<LocationEngineCallback<LocationEngineResult>> callbackClass = (Class) LocationEngineCallback.class;
        final ArgumentCaptor<LocationEngineCallback<LocationEngineResult>> engineCallbackCaptor
            = ArgumentCaptor.forClass(callbackClass);

        final Location mockLocation = mockLocation(new LatLng(0, 1), 10.0f);

        // Try to get last known location
        final TestObserver<LocationAndHeading> testObserver = locatorUnderTest.getLastKnownLocation().test();

        // Send empty result
        Mockito.verify(locationEngine).getLastLocation(engineCallbackCaptor.capture());
        engineCallbackCaptor.getValue().onFailure(new IOException());
        // Assert that no value should appear in the observer
        testObserver.assertValueCount(0);

        final ArgumentCaptor<LocationEngineCallback<LocationEngineResult>> updateCallbackCaptor
            = ArgumentCaptor.forClass(callbackClass);

        // Assert that location updates have started
        Mockito.verify(locationEngine).requestLocationUpdates(any(), updateCallbackCaptor.capture(), any());
        // Get the location update callback and send a location
        updateCallbackCaptor.getValue().onSuccess(LocationEngineResult.create(Collections.singletonList(mockLocation)));

        // Assert that this location is received
        testObserver.assertValueCount(1)
            .assertValueAt(0, new LocationAndHeading(new LatLng(0, 1), 10.0f));
    }

    private static Location mockLocation(final LatLng latLng, final float heading) {
        final Location mockLocation = Mockito.mock(Location.class);
        Mockito.when(mockLocation.getLatitude()).thenReturn(latLng.getLatitude());
        Mockito.when(mockLocation.getLongitude()).thenReturn(latLng.getLongitude());
        Mockito.when(mockLocation.getBearing()).thenReturn(heading);
        Mockito.when(mockLocation.hasBearing()).thenReturn(true);
        return mockLocation;
    }

}
