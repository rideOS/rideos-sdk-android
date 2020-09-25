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
package ai.rideos.android.common.view.strings;

import ai.rideos.android.common.R;
import ai.rideos.android.common.location.Distance;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.common.view.strings.Units.UnitType;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RouteFormatter {
    private final ResourceProvider resourceProvider;

    public RouteFormatter(final ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    public String getDisplayStringForRouteResult(final Result<RouteInfoModel> routeInfoResult) {
        if (routeInfoResult.isFailure()) {
            return resourceProvider.getString(R.string.unknown_route_display);
        }
        final String distanceDisplay = getTravelDistanceDisplayString(routeInfoResult.get());
        final String timeDisplay = getTravelTimeDisplayString(routeInfoResult.get());
        return resourceProvider.getString(R.string.distance_time_display, distanceDisplay, timeDisplay);
    }

    public String getTravelDistanceDisplayString(final RouteInfoModel routeInfoModel) {
        final double distanceMeters = routeInfoModel.getTravelDistanceMeters();
        final Locale userLocale = resourceProvider.getLocale();
        return Units.getPreferredUnitsFromLocale(userLocale) == UnitType.IMPERIAL
            ? getImperialDistanceString(distanceMeters) : getMetricDistanceString(distanceMeters);
    }

    public String getTravelTimeDisplayString(final RouteInfoModel routeInfoModel) {
        final int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(routeInfoModel.getTravelTimeMillis());
        return resourceProvider.getString(R.string.time_display_minutes, minutes);
    }

    private String getImperialDistanceString(final double meters) {
        final double miles = Distance.metersToMiles(meters);
        return resourceProvider.getString(R.string.distance_display_miles, miles);
    }

    private String getMetricDistanceString(final double meters) {
        return resourceProvider.getString(R.string.distance_display_kilometers, meters / 1000);
    }
}
