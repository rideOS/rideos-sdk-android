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
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.viewmodel.ViewModel;
import androidx.core.util.Pair;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MapboxNavigationViewModel is used only for MapboxNavigationFragment and only has one implementation. There is no need
 * to generalize this view model since it should only be used directly with mapbox objects.
 */
public class MapboxNavigationViewModel implements ViewModel {
    private static final int INITIAL_TILT_DEGREES = 45; // Degrees from straight down;
    private static final int INITIAL_ZOOM_LEVEL = 15;
    private static final int DEFAULT_RETRY_COUNT = 2;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final BehaviorSubject<Result<DirectionsRoute>> directionsToDisplay = BehaviorSubject.create();
    private final PublishSubject<LatLng> destinationsToRoute = PublishSubject.create();
    private final PublishSubject<List<LatLng>> routesToMatch = PublishSubject.create();
    private final DeviceLocator deviceLocator;

    private final SchedulerProvider schedulerProvider;

    public MapboxNavigationViewModel(final DeviceLocator deviceLocator,
                                     final MapboxApiInteractor mapboxInteractor) {
        this(deviceLocator, mapboxInteractor, new DefaultSchedulerProvider(), DEFAULT_RETRY_COUNT);
    }

    public MapboxNavigationViewModel(final DeviceLocator deviceLocator,
                                     final MapboxApiInteractor mapboxInteractor,
                                     final SchedulerProvider schedulerProvider,
                                     final int retryCount) {
        this.schedulerProvider = schedulerProvider;
        this.deviceLocator = deviceLocator;

        // listen for last known location
        compositeDisposable.addAll(
            subscribeToMapMatching(mapboxInteractor, retryCount),
            subscribeToDirections(mapboxInteractor, retryCount)
        );
    }

    /**
     * Call into Mapbox's map matching API to match a list of coordinates to a route
     * TODO: check that the route is < 100 points long, and simplify the route if so
     * @param route - list of coordinates representing the requested route
     */
    public void matchDirectionsToRoute(final List<LatLng> route) {
        routesToMatch.onNext(route);
    }

    /**
     * Directly route to a destination using Mapbox's directions API
     * @param destination - destination of navigation
     */
    public void routeTo(final LatLng destination) {
        destinationsToRoute.onNext(destination);
    }

    /**
     * Get the initial camera position to zoom in on. This alleviates issues of the camera being too far zoomed out
     *
     * @return
     */
    public Single<CameraPosition> getInitialCameraPosition() {
        return deviceLocator.getLastKnownLocation()
            .observeOn(schedulerProvider.computation())
            .map(locationAndHeading -> new CameraPosition.Builder()
                .target(new com.mapbox.mapboxsdk.geometry.LatLng(
                    locationAndHeading.getLatLng().getLatitude(),
                    locationAndHeading.getLatLng().getLongitude()
                ))
                .bearing(locationAndHeading.getHeading())
                .tilt(INITIAL_TILT_DEGREES)
                .zoom(INITIAL_ZOOM_LEVEL)
                .build()
            );
    }

    /**
     * Get the result of either map matching or routing to display in the navigation view.
     * @return - successful result of directions if API calls were successful.
     */
    public Observable<Result<DirectionsRoute>> getDirections() {
        return directionsToDisplay;
    }

    private Disposable subscribeToMapMatching(final MapboxApiInteractor mapboxInteractor,
                                              final int retryCount) {
        return routesToMatch.observeOn(schedulerProvider.computation())
            .map(route -> route.stream()
                .map(latLng -> Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude()))
                .collect(Collectors.toList())
            )
            .flatMap(coordinates -> mapboxInteractor.matchCoordinatesToDirections(coordinates)
                .retry(retryCount)
                .map(Result::success)
                .onErrorReturn(Result::failure)
            )
            .subscribe(directionsToDisplay::onNext);
    }

    private Disposable subscribeToDirections(final MapboxApiInteractor mapboxInteractor,
                                             final int retryCount) {
        return destinationsToRoute
            .observeOn(schedulerProvider.computation())
            .flatMap(destination -> deviceLocator.getLastKnownLocation()
                .toObservable()
                .map(origin -> Pair.create(origin.getLatLng(), destination))
            )
            .flatMap(pudo ->
                mapboxInteractor.getDirectionsToDestination(
                    Point.fromLngLat(pudo.first.getLongitude(), pudo.first.getLatitude()),
                    Point.fromLngLat(pudo.second.getLongitude(), pudo.second.getLatitude())
                )
                    .retry(retryCount)
                    .map(Result::success)
                    .onErrorReturn(Result::failure)
            )
            .subscribe(directionsToDisplay::onNext);
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
    }
}
