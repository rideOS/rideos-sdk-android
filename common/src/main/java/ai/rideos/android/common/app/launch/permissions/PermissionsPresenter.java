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
package ai.rideos.android.common.app.launch.permissions;

import ai.rideos.android.common.R;
import ai.rideos.android.common.app.launch.LaunchStep.LaunchStepListener;
import ai.rideos.android.common.view.Presenter;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.ViewGroup;
import androidx.core.app.ActivityCompat;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import java.util.List;

public class PermissionsPresenter implements Presenter<ViewGroup>, PermissionRequestor {
    private static final int PERMISSION_REQUEST_ID = 58417;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final Context context;
    private final PermissionsViewModel permissionsViewModel;

    public PermissionsPresenter(final Context context, final LaunchStepListener listener) {
        this.context = context;
        permissionsViewModel = new DefaultPermissionsViewModel(listener);
    }

    @Override
    public void attach(final ViewGroup view) {
        compositeDisposable.add(
            permissionsViewModel.getPermissionsToCheck().observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::checkPermissions)
        );
    }

    private void checkPermissions(final List<String> permissionsToCheck) {
        final boolean permissionsEnabled = permissionsToCheck.stream()
            .allMatch(permission ->
                ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            );
        if (permissionsEnabled) {
            permissionsViewModel.permissionsEnabled(true);
            return;
        }

        // TODO Change design from modal to view
        new Builder(context)
            .setTitle(context.getString(R.string.location_permission_explanation_title))
            .setMessage(context.getString(R.string.location_permission_explanation_details))
            .setPositiveButton(context.getString(R.string.location_permission_confirmation), (dialog, i) ->
                // When this completes, onRequestPermissionsResult is called
                ActivityCompat.requestPermissions(
                    (Activity) context,
                    permissionsToCheck.toArray(new String[0]),
                    PERMISSION_REQUEST_ID
                )
            )
            .create()
            .show();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String permissions[], final int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionsViewModel.permissionsEnabled(true);
            } else {
                permissionsViewModel.permissionsEnabled(false);
            }
        }
    }

    @Override
    public void detach() {
        compositeDisposable.dispose();
    }
}
