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

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.Locations;
import ai.rideos.android.common.utils.Polylines.GMSPolylineDecoder;
import ai.rideos.android.common.utils.Polylines.PolylineDecoder;
import ai.rideos.api.path.v2.PathProto.Leg;
import ai.rideos.api.path.v2.PathProto.PathRequest;
import ai.rideos.api.path.v2.PathProto.PathRequest.GeometryFormat;
import ai.rideos.api.path.v2.PathProto.PathResponse;
import ai.rideos.api.path.v2.PathProto.Waypoint;
import ai.rideos.api.path.v2.PathServiceGrpc;
import ai.rideos.api.path.v2.PathServiceGrpc.PathServiceFutureStub;
import com.google.protobuf.FloatValue;
import io.grpc.ManagedChannel;
import io.reactivex.Observable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RideOsRouteInteractor extends GrpcServerInteractor<PathServiceFutureStub> implements RouteInteractor {
    private final PolylineDecoder polylineDecoder;

    public static class RouteException extends Exception {
        public RouteException(final String message) {
            super(message);
        }
    }

    public RideOsRouteInteractor(final Supplier<ManagedChannel> channelSupplier, final User user) {
        this(channelSupplier, user, new GMSPolylineDecoder());
    }

    public RideOsRouteInteractor(final Supplier<ManagedChannel> channelSupplier,
                                 final User user,
                                 final PolylineDecoder polylineDecoder) {
        super(PathServiceGrpc::newFutureStub, channelSupplier, user, new DefaultSchedulerProvider());

        this.polylineDecoder = polylineDecoder;
    }

    @Override
    public Observable<RouteInfoModel> getRoute(final LatLng origin, final LatLng destination) {
        return fetchAuthorizedStubAndExecute(pathServiceStub -> pathServiceStub.getPath(
            PathRequest.newBuilder()
                .addAllWaypoints(Arrays.asList(
                    Waypoint.newBuilder()
                        .setPosition(Locations.toRideOsPosition(origin))
                        .build(),
                    Waypoint.newBuilder()
                        .setPosition(Locations.toRideOsPosition(destination))
                        .build()
                ))
                .setGeometryFormat(GeometryFormat.POLYLINE)
                .build()
        ))
            .map(this::getRouteFromPathResponse);
    }

    @Override
    public Observable<RouteInfoModel> getRoute(final LocationAndHeading origin, final LocationAndHeading destination) {
        return fetchAuthorizedStubAndExecute(pathServiceStub -> pathServiceStub.getPath(
            PathRequest.newBuilder()
                .addAllWaypoints(Arrays.asList(
                    Waypoint.newBuilder()
                        .setHeading(FloatValue.newBuilder().setValue(origin.getHeading()))
                        .setPosition(Locations.toRideOsPosition(origin.getLatLng()))
                        .build(),
                    Waypoint.newBuilder()
                        .setHeading(FloatValue.newBuilder().setValue(destination.getHeading()))
                        .setPosition(Locations.toRideOsPosition(destination.getLatLng()))
                        .build()
                ))
                .setGeometryFormat(GeometryFormat.POLYLINE)
                .build()
        ))
            .map(this::getRouteFromPathResponse);
    }

    @Override
    public Observable<List<RouteInfoModel>> getRouteForWaypoints(final List<LatLng> waypoints) {
        return fetchAuthorizedStubAndExecute(pathServiceStub -> pathServiceStub.getPath(
            PathRequest.newBuilder()
                .addAllWaypoints(
                    waypoints.stream()
                        .map(latLng -> Waypoint.newBuilder()
                            .setPosition(Locations.toRideOsPosition(latLng))
                            .build()
                        )
                        .collect(Collectors.toList())
                )
                .setGeometryFormat(GeometryFormat.POLYLINE)
                .build()
        ))
            .map(pathResponse -> {
                if (pathResponse.getPathsCount() < 1
                    || pathResponse.getPaths(0).getLegsCount() != waypoints.size() - 1) {
                    throw new RouteException("Route not found");
                }
                return pathResponse.getPaths(0).getLegsList().stream()
                    .map(this::getRouteFromLeg)
                    .collect(Collectors.toList());
            });
    }

    private RouteInfoModel getRouteFromPathResponse(final PathResponse pathResponse) throws RouteException {
        if (pathResponse.getPathsCount() > 0 && pathResponse.getPaths(0).getLegsCount() > 0) {
            final Leg leg = pathResponse.getPaths(0).getLegs(0);
            return getRouteFromLeg(leg);
        }
        throw new RouteException("Route not found");
    }

    private RouteInfoModel getRouteFromLeg(final Leg leg) {
        return new RouteInfoModel(
            polylineDecoder.decode(leg.getPolyline()),
            leg.getTravelTime().getSeconds() * 1000,
            leg.getDistance()
        );
    }
}
