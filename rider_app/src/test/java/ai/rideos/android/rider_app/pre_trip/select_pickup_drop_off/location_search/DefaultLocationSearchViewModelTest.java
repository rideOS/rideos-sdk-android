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

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.interactors.LocationAutocompleteInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.LocationAutocompleteResult;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.model.map.LatLngBounds;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.interactors.HistoricalSearchInteractor;
import ai.rideos.android.model.LocationSearchFocusType;
import ai.rideos.android.model.LocationSearchInitialState;
import ai.rideos.android.model.LocationSearchOptionModel;
import ai.rideos.android.model.NamedPickupDropOff;
import ai.rideos.android.rider_app.R;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultLocationSearchViewModelTest {
    private static final String CURRENT_LOCATION_DISPLAY = "current location";
    private static final String SET_ON_MAP_DISPLAY = "set on map";
    private static final LatLng LAT_LNG = new LatLng(0, 0);
    private final LocationAutocompleteResult DEFAULT_RESULT = new LocationAutocompleteResult("place", "id-1");
    private final LocationSearchOptionModel DEFAULT_SEARCH_OPTION
        = LocationSearchOptionModel.autocompleteLocation(DEFAULT_RESULT);
    private final NamedTaskLocation LOCATION_FROM_RESULT = new NamedTaskLocation("place", LAT_LNG);
    private static final String TEST_SEARCH = "test search";

    private LocationAutocompleteInteractor autocompleteInteractor;
    private HistoricalSearchInteractor historyInteractor;
    private DefaultLocationSearchViewModel viewModelUnderTest;
    private LocationSearchListener listener;

    @Before
    public void setUp() {
        setUpWithInitialPickupDropOff(new NamedPickupDropOff(null, null));
    }

    private void setUpWithInitialPickupDropOff(final NamedPickupDropOff pickupDropOff) {
        autocompleteInteractor = Mockito.mock(LocationAutocompleteInteractor.class);
        final ResourceProvider resourceProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(resourceProvider.getString(R.string.current_location_search_option))
            .thenReturn(CURRENT_LOCATION_DISPLAY);
        Mockito.when(resourceProvider.getString(R.string.select_on_map_search_option))
            .thenReturn(SET_ON_MAP_DISPLAY);

        historyInteractor = Mockito.mock(HistoricalSearchInteractor.class);
        Mockito.when(historyInteractor.getHistoricalSearchOptions())
            .thenReturn(Observable.just(Collections.emptyList()));
        Mockito.when(historyInteractor.storeSearchedOption(Mockito.any()))
            .thenReturn(Completable.complete());

        final DeviceLocator deviceLocation = Mockito.mock(DeviceLocator.class);
        Mockito.when(deviceLocation.observeCurrentLocation(Mockito.anyInt()))
            .thenReturn(Observable.just(new LocationAndHeading(LAT_LNG, 0f)));
        listener = Mockito.mock(LocationSearchListener.class);
        viewModelUnderTest = new DefaultLocationSearchViewModel(
            listener,
            autocompleteInteractor,
            historyInteractor,
            deviceLocation,
            resourceProvider,
            new LocationSearchInitialState(
                LocationSearchFocusType.DROP_OFF,
                pickupDropOff.getPickup(),
                pickupDropOff.getDropOff()
            ),
            new TrampolineSchedulerProvider()
        );

        Mockito.when(autocompleteInteractor.getAutocompleteResults(
            Mockito.eq(TEST_SEARCH),
            any(LatLngBounds.class)
        )).thenReturn(Observable.just(Collections.singletonList(DEFAULT_RESULT)));
        Mockito.when(autocompleteInteractor.getLocationFromAutocompleteResult(Mockito.any()))
            .thenReturn(Observable.just(LOCATION_FROM_RESULT));
    }

    @Test
    public void testGetLocationOptionsWhenSearchEmpty() {
        TestObserver<List<LocationSearchOptionModel>> testObserver = viewModelUnderTest
            .getLocationOptions()
            .test();
        testObserver.assertEmpty(); // Nothing sent to view model yet
    }

    @Test
    public void testGetDropOffLocationOptions() {
        TestObserver<List<LocationSearchOptionModel>> testObserver = viewModelUnderTest
            .getLocationOptions()
            .test();

        // Test Drop-off
        viewModelUnderTest.setFocus(LocationSearchFocusType.DROP_OFF);
        assertLatestOptionsContain(
            testObserver,
            1,
            Collections.singletonList(LocationSearchOptionModel.selectOnMap(SET_ON_MAP_DISPLAY))
        );

        viewModelUnderTest.setDropOffInput(TEST_SEARCH);
        assertLatestOptionsContain(
            testObserver,
            2,
            Arrays.asList(
                DEFAULT_SEARCH_OPTION,
                LocationSearchOptionModel.selectOnMap(SET_ON_MAP_DISPLAY)
            )
        );
    }

    @Test
    public void testGetPickupLocationOptions() {
        TestObserver<List<LocationSearchOptionModel>> testObserver = viewModelUnderTest
            .getLocationOptions()
            .test();

        viewModelUnderTest.setFocus(LocationSearchFocusType.PICKUP);
        assertLatestOptionsContain(
            testObserver,
            1,
            Arrays.asList(
                LocationSearchOptionModel.currentLocation(CURRENT_LOCATION_DISPLAY),
                LocationSearchOptionModel.selectOnMap(SET_ON_MAP_DISPLAY)
            )
        );

        viewModelUnderTest.setPickupInput(TEST_SEARCH);
        assertLatestOptionsContain(
            testObserver,
            2,
            Arrays.asList(
                LocationSearchOptionModel.currentLocation(CURRENT_LOCATION_DISPLAY),
                DEFAULT_SEARCH_OPTION,
                LocationSearchOptionModel.selectOnMap(SET_ON_MAP_DISPLAY)
            )
        );
    }

    @Test
    public void testGetPickupLocationsAfterDropOff() {
        TestObserver<List<LocationSearchOptionModel>> testObserver = viewModelUnderTest
            .getLocationOptions()
            .test();
        viewModelUnderTest.setFocus(LocationSearchFocusType.DROP_OFF);
        viewModelUnderTest.setDropOffInput(TEST_SEARCH);
        viewModelUnderTest.setPickupInput(TEST_SEARCH);
        assertLatestOptionsContain(
            testObserver,
            3,
            Arrays.asList(
                DEFAULT_SEARCH_OPTION,
                LocationSearchOptionModel.selectOnMap(SET_ON_MAP_DISPLAY)
            )
        );

        viewModelUnderTest.setFocus(LocationSearchFocusType.PICKUP);
        assertLatestOptionsContain(
            testObserver,
            4,
            Arrays.asList(
                LocationSearchOptionModel.currentLocation(CURRENT_LOCATION_DISPLAY),
                DEFAULT_SEARCH_OPTION,
                LocationSearchOptionModel.selectOnMap(SET_ON_MAP_DISPLAY)
            )
        );
    }

    @Test
    public void testPickupSelectionIsPrePopulated() {
        TestObserver<String> pickupObserver = viewModelUnderTest
            .getSelectedPickup()
            .test();
        pickupObserver.assertValueCount(1);
        pickupObserver.assertValueAt(0, CURRENT_LOCATION_DISPLAY);
    }

    @Test
    public void testChangeFocusDoesNotFireSelectionObservable() {
        TestObserver<String> pickupObserver = viewModelUnderTest
            .getSelectedPickup()
            .test();
        viewModelUnderTest.setFocus(LocationSearchFocusType.PICKUP);
        pickupObserver.assertValueCount(1); // updating focus should not create observed selection
    }

    @Test
    public void testCanSelectPickupAndDropOffAfterChangingFocus() {
        TestObserver<String> pickupObserver = viewModelUnderTest
            .getSelectedPickup()
            .test();
        TestObserver<String> dropOffObserver = viewModelUnderTest
            .getSelectedDropOff()
            .test();

        final LocationSearchOptionModel pickupModel = LocationSearchOptionModel.autocompleteLocation(
            new LocationAutocompleteResult("pickup", "id-1")
        );
        viewModelUnderTest.setFocus(LocationSearchFocusType.PICKUP);

        viewModelUnderTest.makeSelection(pickupModel);
        pickupObserver.assertValueCount(2);
        pickupObserver.assertValueAt(1, pickupModel.getPrimaryName());
        dropOffObserver.assertEmpty();

        viewModelUnderTest.setFocus(LocationSearchFocusType.DROP_OFF);
        pickupObserver.assertValueCount(2);
        dropOffObserver.assertEmpty();

        final LocationSearchOptionModel dropOffModel = LocationSearchOptionModel.autocompleteLocation(
            new LocationAutocompleteResult("drop-off", "id-2")
        );
        viewModelUnderTest.makeSelection(dropOffModel);
        pickupObserver.assertValueCount(2);
        dropOffObserver.assertValueCount(1);
        dropOffObserver.assertValueAt(0, dropOffModel.getPrimaryName());
    }

    @Test
    public void testGetNoOptionsAfterAutocompleteFailure() {
        Mockito.when(autocompleteInteractor.getAutocompleteResults(
            Mockito.eq(TEST_SEARCH),
            any(LatLngBounds.class)
        )).thenReturn(Observable.error(new IOException("oops")));

        TestObserver<List<LocationSearchOptionModel>> testObserver = viewModelUnderTest
            .getLocationOptions()
            .test();

        viewModelUnderTest.setFocus(LocationSearchFocusType.DROP_OFF);
        viewModelUnderTest.setDropOffInput(TEST_SEARCH);
        assertLatestOptionsContain(
            testObserver,
            2,
            Collections.singletonList(LocationSearchOptionModel.selectOnMap(SET_ON_MAP_DISPLAY))
        );
    }

    @Test
    public void testSelectPickupCallsListener() {
        viewModelUnderTest.setFocus(LocationSearchFocusType.PICKUP);

        viewModelUnderTest.makeSelection(DEFAULT_SEARCH_OPTION);
        Mockito.verify(listener).selectPickup(LOCATION_FROM_RESULT);
    }

    @Test
    public void testPickupDropOffArePrePopulatedIfInitialPickupDropOffAreGiven() {
        setUpWithInitialPickupDropOff(new NamedPickupDropOff(
            new NamedTaskLocation("pickup", LAT_LNG),
            new NamedTaskLocation("drop-off", LAT_LNG)
        ));
        viewModelUnderTest.isDoneActionEnabled().test()
            .assertValueAt(0, true);
    }

    @Test
    public void testGetDoneButtonDisabledWhenInitialPickupDropOffNotSet() {
        viewModelUnderTest.isDoneActionEnabled().test()
            .assertValueAt(0, false);
    }

    @Test
    public void testGetDoneButtonEnabledWhenInitialPickupDropOffSet() {
        setUpWithInitialPickupDropOff(new NamedPickupDropOff(
            new NamedTaskLocation("pickup", LAT_LNG),
            new NamedTaskLocation("drop-off", LAT_LNG)
        ));
        viewModelUnderTest.isDoneActionEnabled().test()
            .assertValueAt(0, true);
    }

    @Test
    public void testHistoricalSearchOptionsArePopulated() {
        final LocationAutocompleteResult historicalOption = new LocationAutocompleteResult("history", "id-1");
        Mockito.when(historyInteractor.getHistoricalSearchOptions())
            .thenReturn(Observable.just(Collections.singletonList(historicalOption)));

        final TestObserver<List<LocationSearchOptionModel>> testObserver = viewModelUnderTest
            .getLocationOptions()
            .test();

        viewModelUnderTest.setFocus(LocationSearchFocusType.DROP_OFF);
        assertLatestOptionsContain(
            testObserver,
            1,
            Arrays.asList(
                LocationSearchOptionModel.historicalSearch(historicalOption),
                LocationSearchOptionModel.selectOnMap(SET_ON_MAP_DISPLAY)
            )
        );
    }

    @Test
    public void testSelectingHistoricalSearchOptionPopulatesField() {
        final LocationAutocompleteResult historicalOption = new LocationAutocompleteResult("history", "id-1");
        Mockito.when(historyInteractor.getHistoricalSearchOptions())
            .thenReturn(Observable.just(Collections.singletonList(historicalOption)));
        final TestObserver<String> dropOffObserver = viewModelUnderTest
            .getSelectedDropOff()
            .test();

        viewModelUnderTest.setFocus(LocationSearchFocusType.DROP_OFF);

        viewModelUnderTest.makeSelection(LocationSearchOptionModel.historicalSearch(historicalOption));
        dropOffObserver.assertValueCount(1)
            .assertValueAt(0, historicalOption.getPrimaryName());
    }

    @Test
    public void testCanClearPickupWhenTypingInSearch() {
        final TestObserver<Boolean> canClearObserver = viewModelUnderTest
            .canClearPickup()
            .test();
        viewModelUnderTest.setFocus(LocationSearchFocusType.PICKUP);
        viewModelUnderTest.setPickupInput("foo");
        canClearObserver.assertValueCount(2)
            .assertValueAt(0, false)
            .assertValueAt(1, true);
    }

    @Test
    public void testCanClearDropOffWhenTypingInSearch() {
        final TestObserver<Boolean> canClearObserver = viewModelUnderTest
            .canClearDropOff()
            .test();
        viewModelUnderTest.setFocus(LocationSearchFocusType.DROP_OFF);
        viewModelUnderTest.setDropOffInput("foo");
        canClearObserver.assertValueCount(2)
            .assertValueAt(0, false)
            .assertValueAt(1, true);
    }

    private static void assertLatestOptionsContain(final TestObserver<List<LocationSearchOptionModel>> testObserver,
                                                   final int expectedSize,
                                                   final List<LocationSearchOptionModel> expectedOptions) {
        final List<List<LocationSearchOptionModel>> values = testObserver.values();
        assertEquals(expectedSize, values.size());
        assertEquals(expectedOptions, values.get(expectedSize - 1));
    }
}
