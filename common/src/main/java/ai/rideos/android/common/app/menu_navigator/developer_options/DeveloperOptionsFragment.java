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
package ai.rideos.android.common.app.menu_navigator.developer_options;

import ai.rideos.android.common.R;
import ai.rideos.android.common.app.dependency.CommonDependencyRegistry;
import ai.rideos.android.common.app.menu_navigator.OpenMenuListener;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.fleets.ResolvedFleet;
import ai.rideos.android.common.model.SingleSelectOptions;
import ai.rideos.android.common.model.SingleSelectOptions.Option;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageReader;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageWriter;
import ai.rideos.android.common.view.adapters.SingleSelectArrayAdapter;
import ai.rideos.android.common.view.resources.AndroidResourceProvider;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import java.util.function.Consumer;
import timber.log.Timber;

public class DeveloperOptionsFragment extends Fragment {
    private CompositeDisposable compositeDisposable;

    private OpenMenuListener openMenuListener;
    private DeveloperOptionsViewModel viewModel;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final User user = User.get(getContext());
        viewModel = new DefaultDeveloperOptionsViewModel(
            CommonDependencyRegistry.commonDependencyFactory().getFleetInteractor(getContext()),
            SharedPreferencesUserStorageReader.forContext(getContext()), SharedPreferencesUserStorageWriter.forContext(getContext()),
            AndroidResourceProvider.forContext(getContext()),
            user,
            ResolvedFleet.get()
        );
        openMenuListener = (OpenMenuListener) getActivity();
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.developer_options, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        compositeDisposable = new CompositeDisposable();
        final View optionsView = getView();

        final Toolbar toolbar = optionsView.findViewById(R.id.title_bar);
        toolbar.setNavigationOnClickListener(click -> openMenuListener.openMenu());

        final TextView fleetIdField = optionsView.findViewById(R.id.fleet_id_field);
        final TextView dispatchIdField = optionsView.findViewById(R.id.dispatch_id_field);
        final Spinner fleetSpinner = optionsView.findViewById(R.id.fleet_id_spinner);
        final Spinner envSpinner = optionsView.findViewById(R.id.rideos_env_spinner);

        compositeDisposable.addAll(
            viewModel.getFleetOptions()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(options -> setSpinnerOptions(options, viewModel::selectFleetId, fleetSpinner)),
            viewModel.getEnvironmentOptions()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(options -> setSpinnerOptions(options, viewModel::selectEnvironment, envSpinner)),
            viewModel.getResolvedFleetId()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fleetIdField::setText),
            viewModel.getUserId()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(dispatchIdField::setText)
        );

        setVersionName(optionsView);
    }

    private void setVersionName(final View view) {
        final TextView versionText = view.findViewById(R.id.version_name);
        final PackageManager manager = getContext().getPackageManager();
        final PackageInfo info;
        try {
            info = manager.getPackageInfo(
                getContext().getPackageName(), 0);
        } catch (final NameNotFoundException e) {
            Timber.e(e, "Could not get package name");
            return;
        }
        versionText.setText(getString(R.string.version_name_display, info.versionName));
    }

    private <T> void setSpinnerOptions(final SingleSelectOptions<T> options,
                                       final Consumer<Option<T>> onSelect,
                                       final Spinner spinner) {
        final SingleSelectArrayAdapter<T> adapter = new SingleSelectArrayAdapter<>(getContext(), options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
                onSelect.accept(adapter.getOptionAtPosition(position));
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
            }
        });

        if (options.getSelectionIndex().isPresent()) {
            spinner.setSelection(options.getSelectionIndex().get());
        }
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
