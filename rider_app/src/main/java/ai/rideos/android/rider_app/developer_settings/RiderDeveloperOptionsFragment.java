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
package ai.rideos.android.rider_app.developer_settings;

import ai.rideos.android.common.app.menu_navigator.developer_options.DeveloperOptionsFragment;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageReader;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageWriter;
import ai.rideos.android.rider_app.R;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import androidx.annotation.NonNull;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class RiderDeveloperOptionsFragment extends DeveloperOptionsFragment {
    private CompositeDisposable compositeDisposable;

    private RiderDeveloperOptionsViewModel viewModel;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.viewModel = new DefaultRiderDeveloperOptionsViewModel(
            SharedPreferencesUserStorageReader.forContext(getContext()),
            SharedPreferencesUserStorageWriter.forContext(getContext())
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        final ViewGroup extraSettingsContainer = view.findViewById(R.id.extra_settings_container);
        final View riderSettings = inflater.inflate(R.layout.rider_developer_settings, extraSettingsContainer, false);
        extraSettingsContainer.addView(riderSettings);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        final View view = getView();

        final Switch vehicleSelectionSwitch = view.findViewById(R.id.vehicle_select_switch);
        vehicleSelectionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setManualVehicleSelection(isChecked);
        });

        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(
            viewModel.isManualVehicleSelectionEnabled().observeOn(AndroidSchedulers.mainThread())
                .subscribe(selectionEnabled -> {
                    vehicleSelectionSwitch.setEnabled(true);
                    vehicleSelectionSwitch.setChecked(selectionEnabled);
                })
        );
    }

    public void onStop() {
        super.onStop();
        compositeDisposable.dispose();
    }
}
