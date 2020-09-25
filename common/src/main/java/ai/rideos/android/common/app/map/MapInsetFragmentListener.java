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
package ai.rideos.android.common.app.map;

import ai.rideos.android.common.view.ViewMarginProvider;
import ai.rideos.android.common.view.ViewMargins;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks;

public class MapInsetFragmentListener extends FragmentLifecycleCallbacks {
    private final MapStateReceiver mapStateReceiver;

    public MapInsetFragmentListener() {
        mapStateReceiver = MapRelay.get();
    }

    @Override
    public void onFragmentViewCreated(@NonNull final FragmentManager fragmentManager,
                                      @NonNull final Fragment fragment,
                                      @NonNull final View view,
                                      @Nullable final Bundle savedInstanceState) {
        super.onFragmentViewCreated(fragmentManager, fragment, view, savedInstanceState);
        if (view instanceof ViewMarginProvider) {
            ((ViewMarginProvider) view).calculateViewMargins(mapStateReceiver::setMapMargins);
        } else {
            mapStateReceiver.setMapMargins(ViewMargins.newBuilder().build());
        }
    }
}
