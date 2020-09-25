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
package ai.rideos.android.interactors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;

import ai.rideos.android.common.model.LocationAutocompleteResult;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.user_storage.UserStorageReader;
import ai.rideos.android.common.user_storage.UserStorageWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class UserStorageHistoricalSearchInteractorTest {
    private static final int MAX_OPTIONS = 3;
    private static final LocationAutocompleteResult OPTION_1 = new LocationAutocompleteResult("place1", "secondary-name", "id1");
    private static final LocationAutocompleteResult OPTION_2 = new LocationAutocompleteResult("place2", "id2");
    private static final LocationAutocompleteResult OPTION_3 = new LocationAutocompleteResult("place3", "id3");
    private static final LocationAutocompleteResult OPTION_4 = new LocationAutocompleteResult("place4", "id4");
    private UserStorageHistoricalSearchInteractor interactorUnderTest;
    private AtomicReference<String> storedString;

    @Before
    public void setUp() {
        final UserStorageReader reader = Mockito.mock(UserStorageReader.class);
        final UserStorageWriter writer = Mockito.mock(UserStorageWriter.class);
        storedString = new AtomicReference<>("");
        Mockito.when(reader.getStringPreference(anyObject()))
            .thenAnswer(invocation -> storedString.get());
        Mockito.doAnswer(invocation -> {
            final String serializedOptions = (String) invocation.getArguments()[1];
            storedString.set(serializedOptions);
            return null;
        }).when(writer).storeStringPreference(anyObject(), anyString());

        interactorUnderTest = new UserStorageHistoricalSearchInteractor(
            reader,
            writer,
            new UserStorageHistoricalSearchInteractor.GsonSerializer(),
            MAX_OPTIONS,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testGetHistoricalOptionsReturnsEmptyListWhenNoOptionsStored() {
        interactorUnderTest.getHistoricalSearchOptions().test()
            .assertValueAt(0, Collections.emptyList());
    }

    @Test
    public void testGetHistoricalOptionsAfterStorage() {
        interactorUnderTest.storeSearchedOption(OPTION_1).blockingAwait();
        interactorUnderTest.getHistoricalSearchOptions().test()
            .assertValueAt(0, Collections.singletonList(OPTION_1));
    }

    @Test
    public void testStoringMultipleOptionsStoresInReverseChronologicalOrder() {
        final List<LocationAutocompleteResult> expectedOptions = Arrays.asList(OPTION_1, OPTION_2);
        interactorUnderTest.storeSearchedOption(OPTION_2).blockingAwait();
        interactorUnderTest.storeSearchedOption(OPTION_1).blockingAwait();
        interactorUnderTest.getHistoricalSearchOptions().test()
            .assertValueAt(0, expectedOptions);
    }

    @Test
    public void testStoringExistingOptionReplacesOldOption() {
        interactorUnderTest.storeSearchedOption(OPTION_2).blockingAwait();
        interactorUnderTest.storeSearchedOption(OPTION_1).blockingAwait();
        interactorUnderTest.storeSearchedOption(OPTION_2).blockingAwait();

        interactorUnderTest.getHistoricalSearchOptions().test()
            .assertValueAt(0, Arrays.asList(OPTION_2, OPTION_1));
    }

    @Test
    public void testStoringMoreOptionsThanMaximumTrimsStoredOptions() {
        interactorUnderTest.storeSearchedOption(OPTION_1).blockingAwait();
        interactorUnderTest.storeSearchedOption(OPTION_2).blockingAwait();
        interactorUnderTest.storeSearchedOption(OPTION_3).blockingAwait();
        interactorUnderTest.storeSearchedOption(OPTION_4).blockingAwait();

        interactorUnderTest.getHistoricalSearchOptions().test()
            .assertValueAt(0, Arrays.asList(OPTION_4, OPTION_3, OPTION_2));
    }

    @Test
    public void testDeserializeErrorResetsStoredSearches() {
        storedString.set("junk value");
        interactorUnderTest.getHistoricalSearchOptions().test()
            .assertNoErrors()
            .assertValueAt(0, Collections.emptyList());
        assertEquals("", storedString.get());
    }
}
