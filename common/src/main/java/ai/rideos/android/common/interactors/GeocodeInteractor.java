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
package ai.rideos.android.common.interactors;

import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.reactive.Result;
import io.reactivex.Observable;
import java.util.List;

public interface GeocodeInteractor {
    /**
     * Given a lat/lng coordinate, return a list of possible locations nearest the coordinate.
     * @param latLng - coordinate to search for
     * @param maxResults - maximum locations to return (use 1 if you just want the nearest location)
     * @return list of possible locations
     */
    Observable<List<NamedTaskLocation>> getReverseGeocodeResults(final LatLng latLng, final int maxResults);

    /**
     * Given a lat/lng coordinate, return the best matching location, containing the most displayable information.
     * @param latLng - coordinate to search for
     * @return best matching location, or Result.failure if none found
     */
    Observable<Result<NamedTaskLocation>> getBestReverseGeocodeResult(final LatLng latLng);
}
