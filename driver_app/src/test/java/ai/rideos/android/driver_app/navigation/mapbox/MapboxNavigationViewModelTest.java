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

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.interactors.mapbox.MapboxApiInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MapboxNavigationViewModelTest {
    private static final LatLng CURRENT_LOCATION = new LatLng(0, 1);
    private static final int RETRY_COUNT = 1;

    private MapboxNavigationViewModel viewModelUnderTest;
    private DeviceLocator deviceLocator;
    private MapboxApiInteractor mapboxInteractor;

    @Before
    public void setUp() {
        deviceLocator = Mockito.mock(DeviceLocator.class);
        mapboxInteractor = Mockito.mock(MapboxApiInteractor.class);

        viewModelUnderTest = new MapboxNavigationViewModel(
            deviceLocator,
            mapboxInteractor,
            new TrampolineSchedulerProvider(),
            RETRY_COUNT
        );
    }

    @Test
    public void testMapMatchingEmitsSuccessfulDirections() {
        final LatLng origin = new LatLng(0, 1);
        final LatLng destination = new LatLng(0, 2);
        final List<LatLng> routeToMatch = Arrays.asList(origin, destination);

        final List<Point> expectedCoordinates = Arrays.asList(
            Point.fromLngLat(origin.getLongitude(), origin.getLatitude()),
            Point.fromLngLat(destination.getLongitude(), destination.getLatitude())
        );
        final DirectionsRoute routeToReturn = Mockito.mock(DirectionsRoute.class);
        Mockito.when(mapboxInteractor.matchCoordinatesToDirections(Mockito.any()))
            .thenReturn(Observable.just(routeToReturn));

        viewModelUnderTest.matchDirectionsToRoute(routeToMatch);

        viewModelUnderTest.getDirections().test()
            .assertValueCount(1)
            .assertValueAt(0, Result::isSuccess);
        Mockito.verify(mapboxInteractor).matchCoordinatesToDirections(expectedCoordinates);
    }

    @Test
    public void testMapMatchingEmitsFailureWhenInteractorErrors() {
        final IOException errorToReturn = new IOException();
        Mockito.when(mapboxInteractor.matchCoordinatesToDirections(Mockito.any()))
            .thenReturn(Observable.error(errorToReturn));

        viewModelUnderTest.matchDirectionsToRoute(Collections.emptyList());
        viewModelUnderTest.getDirections().test()
            .assertValueCount(1)
            .assertValueAt(0, result -> result.isFailure() && result.getError().equals(errorToReturn));
    }

    @Test
    public void testRouteToEmitsSuccessfulDirections() {
        final LatLng destination = new LatLng(0, 2);

        Mockito.when(deviceLocator.getLastKnownLocation()).thenReturn(
            Single.just(new LocationAndHeading(CURRENT_LOCATION, 0f))
        );

        final Point expectedOrigin = Point.fromLngLat(CURRENT_LOCATION.getLongitude(), CURRENT_LOCATION.getLatitude());
        final Point expectedDestination = Point.fromLngLat(destination.getLongitude(), destination.getLatitude());
        final DirectionsRoute directionsToReturn = Mockito.mock(DirectionsRoute.class);
        Mockito.when(mapboxInteractor.getDirectionsToDestination(Mockito.any(), Mockito.any()))
            .thenReturn(Observable.just(directionsToReturn));

        viewModelUnderTest.routeTo(destination);
        viewModelUnderTest.getDirections().test()
            .assertValueCount(1)
            .assertValueAt(0, Result::isSuccess);
        Mockito.verify(mapboxInteractor).getDirectionsToDestination(expectedOrigin, expectedDestination);
    }

    @Test
    public void testRouteToEmitsFailureWhenInteractorErrors() {
        Mockito.when(deviceLocator.getLastKnownLocation()).thenReturn(
            Single.just(new LocationAndHeading(CURRENT_LOCATION, 0f))
        );

        final IOException errorToReturn = new IOException();
        Mockito.when(mapboxInteractor.getDirectionsToDestination(Mockito.any(), Mockito.any()))
            .thenReturn(Observable.error(errorToReturn));

        viewModelUnderTest.routeTo(new LatLng(0, 1));
        viewModelUnderTest.getDirections().test()
            .assertValueCount(1)
            .assertValueAt(0, result -> result.isFailure() && result.getError().equals(errorToReturn));
    }

    @Test
    public void testInitialCameraPositionEmitsCurrentLocation() {
        final float heading = 10f;
        Mockito.when(deviceLocator.getLastKnownLocation()).thenReturn(
            Single.just(new LocationAndHeading(CURRENT_LOCATION, heading))
        );

        viewModelUnderTest.getInitialCameraPosition().test()
            .assertValueAt(0, cameraPosition ->
                cameraPosition.target.getLatitude() == CURRENT_LOCATION.getLatitude()
                    && cameraPosition.target.getLongitude() == CURRENT_LOCATION.getLongitude()
                    && cameraPosition.bearing == heading
                    && cameraPosition.tilt > 0
                    && cameraPosition.zoom > 0
            );
    }
}
