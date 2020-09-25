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
package ai.rideos.android.driver_app.vehicle_settings;

import ai.rideos.android.common.app.menu_navigator.OpenMenuListener;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.device.InputMethodManagerKeyboardManager;
import ai.rideos.android.common.device.KeyboardManager;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.driver_app.dependency.DriverDependencyRegistry;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import java.util.function.Consumer;

public class VehicleSettingsFragment extends Fragment {
    private CompositeDisposable compositeDisposable;

    private VehicleSettingsViewModel viewModel;
    private OpenMenuListener openMenuListener;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new DefaultVehicleSettingsViewModel(
            User.get(getContext()),
            DriverDependencyRegistry.driverDependencyFactory().getDriverVehicleInteractor(getContext())
        );
        openMenuListener = (OpenMenuListener) getActivity();
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.vehicle_settings, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        compositeDisposable = new CompositeDisposable();

        final View view = getView();
        final KeyboardManager keyboardManager = new InputMethodManagerKeyboardManager(getContext(), view);

        final EditText licenseText = view.findViewById(R.id.license_edit_text);
        listenToEditText(licenseText, viewModel::editLicensePlate);

        final Toolbar toolbar = view.findViewById(R.id.title_bar);
        toolbar.setNavigationOnClickListener(click -> {
            keyboardManager.hideKeyboard();
            openMenuListener.openMenu();
        });

        final Button saveButton = view.findViewById(R.id.save_button);
        saveButton.setOnClickListener(click -> {
            viewModel.save();
            licenseText.clearFocus();
            keyboardManager.hideKeyboard();
        });

        compositeDisposable.addAll(
            viewModel.getLicensePlate().observeOn(AndroidSchedulers.mainThread()).subscribe(licenseText::setText),
            viewModel.isSavingEnabled().observeOn(AndroidSchedulers.mainThread()).subscribe(enabled -> {
                if (enabled) {
                    saveButton.setVisibility(View.VISIBLE);
                } else {
                    saveButton.setVisibility(View.GONE);
                }
            })
        );
    }

    private static void listenToEditText(final EditText editText, final Consumer<String> onChange) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                onChange.accept(s.toString());
            }

            @Override
            public void afterTextChanged(final Editable s) {

            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        compositeDisposable.dispose();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }
}
