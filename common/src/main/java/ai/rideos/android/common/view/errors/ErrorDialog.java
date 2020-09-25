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
package ai.rideos.android.common.view.errors;

import ai.rideos.android.common.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public class ErrorDialog {
    private final Context context;
    private final AlertDialog alertDialog;

    public ErrorDialog(@NonNull final Context context) {
        alertDialog = new AlertDialog.Builder(context, R.style.DefaultAlertDialogTheme)
            .setPositiveButton(R.string.error_dialog_confirmation, (d, i) -> {})
            .create();
        this.context = context;
    }

    public void show(final String errorMessage) {
        if (alertDialog.isShowing()) {
            return;
        }
        final View dialogView = LayoutInflater.from(context).inflate(R.layout.default_error_alert, null);
        final TextView message = dialogView.findViewById(R.id.error_message);
        message.setText(errorMessage);
        alertDialog.setView(dialogView);
        alertDialog.show();
    }
}
