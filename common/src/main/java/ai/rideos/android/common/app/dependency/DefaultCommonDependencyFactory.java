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
package ai.rideos.android.common.app.dependency;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.grpc.ChannelProvider;
import ai.rideos.android.common.interactors.DefaultDeviceRegistryInteractor;
import ai.rideos.android.common.interactors.DefaultFleetInteractor;
import ai.rideos.android.common.interactors.DeviceRegistryInteractor;
import ai.rideos.android.common.interactors.FleetInteractor;
import android.content.Context;

public abstract class DefaultCommonDependencyFactory implements CommonDependencyFactory {
    public FleetInteractor getFleetInteractor(final Context context) {
        return new DefaultFleetInteractor(ChannelProvider.getChannelSupplierForContext(context), User.get(context));
    }

    @Override
    public DeviceRegistryInteractor getDeviceRegistryInteractor(final Context context) {
        return new DefaultDeviceRegistryInteractor(
            ChannelProvider.getChannelSupplierForContext(context),
            User.get(context)
        );
    }
}
