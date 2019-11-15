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
package ai.rideos.android.driver_app.vehicle_unregistered.register_vehicle;

import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.EmptyArg;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.device.InputMethodManagerKeyboardManager;
import ai.rideos.android.common.device.KeyboardManager;
import ai.rideos.android.common.fleets.ResolvedFleet;
import ai.rideos.android.common.view.errors.ErrorDialog;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.driver_app.dependency.DriverDependencyRegistry;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Slide;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toolbar;
import androidx.annotation.NonNull;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

import java.util.function.Consumer;

public class RegisterVehicleFragment extends FragmentViewController<EmptyArg, RegisterVehicleListener> {
    private CompositeDisposable compositeDisposable;

    private User user;
    private RegisterVehicleViewModel viewModel;

    @Override
    public ControllerTypes<EmptyArg, RegisterVehicleListener> getTypes() {
        return new ControllerTypes<>(EmptyArg.class, RegisterVehicleListener.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        user = User.get(getContext());
        viewModel = new DefaultRegisterVehicleViewModel(
                DriverDependencyRegistry.driverDependencyFactory().getDriverVehicleInteractor(getContext()),
                user,
                ResolvedFleet.get().observeFleetInfo(),
                getListener()
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final TransitionSet transitionSet = new TransitionSet()
                .addTransition(new Slide(Gravity.BOTTOM)
                        .addTarget("vehicle_registration")
                );
        setSharedElementEnterTransition(transitionSet);
        setExitTransition(transitionSet);
        return inflater.inflate(R.layout.register_vehicle, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        compositeDisposable = new CompositeDisposable();
        final View view = getView();
        final KeyboardManager keyboardManager = new InputMethodManagerKeyboardManager(getContext(), view);

        final EditText nameInput = view.findViewById(R.id.registration_preferred_name_input);
        listenToEditText(nameInput, viewModel::setPreferredName);

        final EditText phoneInput = view.findViewById(R.id.registration_phone_number_input);
        listenToEditText(phoneInput, viewModel::setPhoneNumber);

        final EditText licenseInput = view.findViewById(R.id.registration_license_plate_input);
        listenToEditText(licenseInput, viewModel::setLicensePlate);

        final EditText capacityInput = view.findViewById(R.id.registration_rider_capacity_input);
        listenToEditText(capacityInput, input -> viewModel.setRiderCapacity(input.isEmpty() ? 0 : Integer.parseInt(input)));

        final Toolbar toolbar = view.findViewById(R.id.title_bar);
        toolbar.setNavigationOnClickListener(click -> {
            keyboardManager.hideKeyboard();
            getListener().cancelRegistration();
        });

        final Button saveButton = view.findViewById(R.id.save_button);
        saveButton.setOnClickListener(click -> {
            compositeDisposable.add(
                    viewModel.save()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(result -> {
                                if (result.isFailure()) {
                                    String errorMessage = getContext().getString(result.getFailureInfo());
                                    ErrorDialog errorDialog = new ErrorDialog(getContext());
                                    errorDialog.show(errorMessage);
                                }
                            })
            );
            keyboardManager.hideKeyboard();
        });

        compositeDisposable.add(
                viewModel.isSavingEnabled()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(enabled -> {
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
        viewModel.destroy();
    }
}
