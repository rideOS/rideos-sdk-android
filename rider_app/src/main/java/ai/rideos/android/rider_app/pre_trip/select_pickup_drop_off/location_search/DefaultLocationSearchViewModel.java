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
package ai.rideos.android.rider_app.pre_trip.select_pickup_drop_off.location_search;

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.interactors.LocationAutocompleteInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.LocationAutocompleteResult;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.model.TaskLocation;
import ai.rideos.android.common.reactive.RetryBehaviors;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.Locations;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.interactors.HistoricalSearchInteractor;
import ai.rideos.android.model.LocationSearchFocusType;
import ai.rideos.android.model.LocationSearchInitialState;
import ai.rideos.android.model.LocationSearchOptionModel;
import ai.rideos.android.model.LocationSearchOptionModel.OptionType;
import ai.rideos.android.rider_app.R;
import androidx.core.util.Pair;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import timber.log.Timber;

public class DefaultLocationSearchViewModel implements LocationSearchViewModel {
    private static final int SEARCH_RADIUS_METERS = 6000;
    private static final int DEFAULT_POLL_INTERVAL_MILLI = 5000;

    private final PublishSubject<String> pickupSubject = PublishSubject.create();
    private final PublishSubject<String> dropOffSubject = PublishSubject.create();
    private final BehaviorSubject<LocationSearchFocusType> focusSubject = BehaviorSubject.create();
    private final PublishSubject<LocationSearchOptionModel> selectionSubject = PublishSubject.create();
    private final Observable<LatLng> currentLocation;

    private final BehaviorSubject<Optional<LocationSearchOptionModel>> selectedPickup;
    private final BehaviorSubject<Optional<LocationSearchOptionModel>> selectedDropOff = BehaviorSubject.create();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final LocationSearchListener listener;

    private final LocationAutocompleteInteractor autocompleteInteractor;
    private final HistoricalSearchInteractor historicalSearchInteractor;
    private final ResourceProvider resourceProvider;
    private final SchedulerProvider schedulerProvider;
    private final LocationSearchInitialState initialState;

    /**
     * Construct a DefaultLocationSearchViewModel using some default values.
     * @param listener - listens to when the pickup and drop off have been set
     * @param autocompleteInteractor - interacts with geocoding service to autocomplete text to place names
     * @param deviceLocator - locates the Android device
     * @param resourceProvider - provides configurable strings and drawables
     * @param initialState - initial state of pickup and drop-off and what to focus on
     */
    public DefaultLocationSearchViewModel(final LocationSearchListener listener,
                                          final LocationAutocompleteInteractor autocompleteInteractor,
                                          final HistoricalSearchInteractor historicalSearchInteractor,
                                          final DeviceLocator deviceLocator,
                                          final ResourceProvider resourceProvider,
                                          final LocationSearchInitialState initialState) {
        this(
            listener,
            autocompleteInteractor,
            historicalSearchInteractor,
            deviceLocator,
            resourceProvider,
            initialState,
            new DefaultSchedulerProvider()
        );
    }

    public DefaultLocationSearchViewModel(final LocationSearchListener listener,
                                          final LocationAutocompleteInteractor autocompleteInteractor,
                                          final HistoricalSearchInteractor historicalSearchInteractor,
                                          final DeviceLocator deviceLocator,
                                          final ResourceProvider resourceProvider,
                                          final LocationSearchInitialState initialState,
                                          final SchedulerProvider schedulerProvider) {
        this.autocompleteInteractor = autocompleteInteractor;
        this.historicalSearchInteractor = historicalSearchInteractor;
        this.currentLocation = deviceLocator.observeCurrentLocation(DEFAULT_POLL_INTERVAL_MILLI)
            .map(LocationAndHeading::getLatLng)
            .observeOn(schedulerProvider.computation());
        this.resourceProvider = resourceProvider;
        this.initialState = initialState;
        this.schedulerProvider = schedulerProvider;
        this.listener = listener;

        // If there is no initial pickup spot, set it to be the current location
        if (!initialState.getInitialPickup().isPresent()) {
            final LocationSearchOptionModel defaultPickup = LocationSearchOptionModel
                .currentLocation(getCurrentLocationDisplayString());
            selectedPickup = BehaviorSubject.createDefault(Optional.of(defaultPickup));
        } else {
            selectedPickup = BehaviorSubject.create();
        }

        compositeDisposable.addAll(
            selectionSubject
                .observeOn(schedulerProvider.computation())
                .subscribe(selectedLocation -> {
                    final LocationSearchFocusType focus = focusSubject.getValue();
                    if (focus == LocationSearchFocusType.PICKUP) {
                        if (selectedLocation.getOptionType() == OptionType.SELECT_ON_MAP) {
                            listener.setPickupOnMap();
                            // Do not select pickup or drop off when "select on map" is clicked.
                            // This way, the text field does not populate
                        } else {
                            selectedPickup.onNext(Optional.of(selectedLocation));
                        }
                    } else {
                        if (selectedLocation.getOptionType() == OptionType.SELECT_ON_MAP) {
                            listener.setDropOffOnMap();
                        } else {
                            selectedDropOff.onNext(Optional.of(selectedLocation));
                        }
                    }
                }),
            // Listen for changes to the selected pickup/drop-off and propagate to listener
            geocodeLocationSelection(selectedPickup)
                .subscribe(listener::selectPickup),
            geocodeLocationSelection(selectedDropOff)
                .subscribe(listener::selectDropOff)
        );
    }

    @Override
    public void setPickupInput(final String input) {
        pickupSubject.onNext(input);
        // Passing Optional.empty() invalidates the current selected pickup
        selectedPickup.onNext(Optional.empty());
    }

    @Override
    public void setDropOffInput(final String input) {
        dropOffSubject.onNext(input);
        // Passing Optional.empty() invalidates the current selected drop-off
        selectedDropOff.onNext(Optional.empty());
    }

    @Override
    public void setFocus(final LocationSearchFocusType inputType) {
        focusSubject.onNext(inputType);
    }

    @Override
    public void makeSelection(final LocationSearchOptionModel selectedLocation) {
        selectionSubject.onNext(selectedLocation);
    }

    @Override
    public void done() {
        listener.doneSearching();
    }

    private Observable<NamedTaskLocation> geocodeLocationSelection(
        final Observable<Optional<LocationSearchOptionModel>> observableSelection
    ) {
        return observableSelection.observeOn(schedulerProvider.computation())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(selection -> {
                if (selection.getOptionType() == OptionType.AUTOCOMPLETE_LOCATION) {
                    return getLocationFromAutocompleteResult(selection.getAutocompleteResult());
                } else {
                    return getCurrentLocation();
                }
            });
    }

    @Override
    public Observable<LocationSearchInitialState> getInitialState() {
        return Observable.just(initialState);
    }

    @Override
    public Observable<Boolean> isDoneActionEnabled() {
        // Enable if the initial values are already filled in
        return Observable.just(initialState.getInitialPickup().isPresent() && initialState.getInitialDropOff().isPresent());
    }

    @Override
    public Observable<List<LocationSearchOptionModel>> getLocationOptions() {
        return Observable.combineLatest(
            getPickupOptions().startWith(Collections.<LocationSearchOptionModel>emptyList()),
            getDropOffOptions().startWith(Collections.<LocationSearchOptionModel>emptyList()),
            focusSubject,
            (pickup, dropOff, focus) -> {
                if (focus == LocationSearchFocusType.PICKUP) {
                    return pickup;
                } else {
                    return dropOff;
                }
            }
        );
    }

    private Observable<List<LocationSearchOptionModel>> getPickupOptions() {
        final Observable<List<LocationSearchOptionModel>> pickupSearchOptions = Observable.combineLatest(
            pickupSubject.startWith(""),
            currentLocation.distinctUntilChanged(),
            Pair::create
        )
            .flatMap(pickupAndLocation -> getAutocompleteOptions(pickupAndLocation.first, pickupAndLocation.second));

        return Observable.combineLatest(
            pickupSearchOptions.startWith(Collections.<LocationSearchOptionModel>emptyList()),
            getHistoricalSearchOptions().startWith(Collections.<LocationSearchOptionModel>emptyList()),
            (predictions, history) -> {
                // Show autocomplete predictions, if any
                final List<LocationSearchOptionModel> predictionsCopy = new ArrayList<>(predictions);
                // Show current location first
                predictionsCopy.add(0, LocationSearchOptionModel.currentLocation(getCurrentLocationDisplayString()));
                // After autocomplete predictions, show select on map
                predictionsCopy.add(LocationSearchOptionModel.selectOnMap(
                    resourceProvider.getString(R.string.select_on_map_search_option)
                ));
                // Lastly, show history
                predictionsCopy.addAll(history);
                return predictionsCopy;
            });
    }

    private Observable<List<LocationSearchOptionModel>> getDropOffOptions() {
        final Observable<List<LocationSearchOptionModel>> dropOffSearchOptions = Observable.combineLatest(
            dropOffSubject.startWith(""),
            currentLocation.distinctUntilChanged(),
            Pair::create
        )
            .flatMap(pickupAndLocation -> getAutocompleteOptions(pickupAndLocation.first, pickupAndLocation.second));

        return Observable.combineLatest(
            dropOffSearchOptions.startWith(Collections.<LocationSearchOptionModel>emptyList()),
            getHistoricalSearchOptions().startWith(Collections.<LocationSearchOptionModel>emptyList()),
            (predictions, history) -> {
                // Show autocomplete predictions, if any
                final List<LocationSearchOptionModel> predictionsCopy = new ArrayList<>(predictions);
                // Then show history
                predictionsCopy.addAll(history);
                // Lastly, show select on map
                predictionsCopy.add(LocationSearchOptionModel.selectOnMap(
                    resourceProvider.getString(R.string.select_on_map_search_option)
                ));
                return predictionsCopy;
            }
        );
    }

    private Observable<List<LocationSearchOptionModel>> getHistoricalSearchOptions() {
        return historicalSearchInteractor.getHistoricalSearchOptions()
            .observeOn(schedulerProvider.computation())
            .map(historicalOptions -> historicalOptions.stream()
                .map(LocationSearchOptionModel::historicalSearch)
                .collect(Collectors.toList())
            );
    }

    private Observable<List<LocationSearchOptionModel>> getAutocompleteOptions(final String input,
                                                                               final LatLng location) {
        if (input.isEmpty()) {
            return Observable.just(Collections.emptyList());
        }
        return autocompleteInteractor.getAutocompleteResults(
            input,
            Locations.getBoundsFromCenterAndRadius(location, SEARCH_RADIUS_METERS)
        )
            .observeOn(schedulerProvider.computation())
            // log errors
            .doOnError(e -> Timber.e(e, "Failed to get autocomplete predictions"))
            .retryWhen(RetryBehaviors.getDefault())
            // Return empty predictions when autocomplete fails
            .onErrorReturnItem(Collections.emptyList())
            .map(predictions -> predictions.stream()
                .map(LocationSearchOptionModel::autocompleteLocation)
                .collect(Collectors.toList())
            );
    }

    @Override
    public Observable<String> getSelectedPickup() {
        return selectedPickup
            .observeOn(schedulerProvider.computation())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(LocationSearchOptionModel::getPrimaryName);
    }

    @Override
    public Observable<String> getSelectedDropOff() {
        return selectedDropOff
            .observeOn(schedulerProvider.computation())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(LocationSearchOptionModel::getPrimaryName);
    }

    @Override
    public Observable<Boolean> canClearPickup() {
        return canClear(pickupSubject, focusSubject, selectedPickup, LocationSearchFocusType.PICKUP);
    }

    @Override
    public Observable<Boolean> canClearDropOff() {
        return canClear(dropOffSubject, focusSubject, selectedDropOff, LocationSearchFocusType.DROP_OFF);
    }

    private Observable<Boolean> canClear(final Observable<String> observableInput,
                                         final Observable<LocationSearchFocusType> observableFocus,
                                         final Observable<Optional<LocationSearchOptionModel>> observableSelection,
                                         final LocationSearchFocusType expectedFocus) {
        return Observable.combineLatest(
            observableInput.startWith(""),
            observableFocus.startWith(LocationSearchFocusType.NONE),
            observableSelection.startWith(Optional.empty()),
            (input, focus, selection) -> focus == expectedFocus
                && !selection.isPresent()
                && input.length() > 0
        )
            .observeOn(schedulerProvider.computation())
            .distinctUntilChanged();
    }

    // Lookup a place by its ID using GMS services and translate into a geocoded model.
    private Observable<NamedTaskLocation> getLocationFromAutocompleteResult(
        final LocationAutocompleteResult autocompleteResult
    ) {
        return autocompleteInteractor.getLocationFromAutocompleteResult(autocompleteResult)
            .observeOn(schedulerProvider.computation())
            // log any errors
            .doOnError(e -> Timber.e(e, "Error getting place from place id"))
            // retry a few times
            .retryWhen(RetryBehaviors.getDefault())
            // on error return empty observable
            .onErrorResumeNext(Observable.empty())
            .flatMap(geocodedLocation -> historicalSearchInteractor.storeSearchedOption(autocompleteResult)
                .observeOn(schedulerProvider.computation())
                .doOnError(e -> Timber.e(e, "Error storing historical search options"))
                .onErrorComplete() // continue on error
                .toSingleDefault(geocodedLocation)
                .toObservable()
            );
    }

    // Return last known location as a geocoded location.
    private Observable<NamedTaskLocation> getCurrentLocation() {
        return currentLocation.firstOrError()
            .toObservable()
            .map(latLng -> new NamedTaskLocation(
                getCurrentLocationDisplayString(),
                new TaskLocation(latLng)
            ));
    }

    private String getCurrentLocationDisplayString() {
        return resourceProvider.getString(R.string.current_location_search_option);
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
    }

    /**
     * SearchState encapsulates all the elements needed to search for a location.
     */
    private static class SearchState {
        private final String pickupSearch;
        private final String dropOffSearch;
        private final LocationSearchFocusType focus;
        private final LatLng currentLocation;

        private SearchState(final String pickupSearch,
                            final String dropOffSearch,
                            final LocationSearchFocusType focus,
                            final LatLng currentLocation) {
            this.pickupSearch = pickupSearch;
            this.dropOffSearch = dropOffSearch;
            this.focus = focus;
            this.currentLocation = currentLocation;
        }
    }
}
