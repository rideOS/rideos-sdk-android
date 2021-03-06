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
package ai.rideos.android.rider_app.on_trip.current_trip;

import ai.rideos.android.common.model.VehicleInfo;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.deeplink.UriLauncher;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;

public class VehicleInfoView extends ConstraintLayout {
    private TextView infoText;
    private AppCompatImageView contactButton;

    public VehicleInfoView(final Context context) {
        super(context);
        init(context);
    }

    public VehicleInfoView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VehicleInfoView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(final Context context) {
        inflate(context, R.layout.vehicle_info_row, this);
        infoText = findViewById(R.id.vehicle_info_text);
        contactButton = findViewById(R.id.contact_button);
    }

    // TODO this is starting to have a fair bit of logic - does it deserve its own fragment?
    public void setVehicleInfo(@Nullable final VehicleInfo vehicleInfo) {
        if (vehicleInfo == null) {
            contactButton.setVisibility(GONE);
            return;
        }
        infoText.setText(vehicleInfo.getLicensePlate());

        setContactButtonView(vehicleInfo.getContactInfo());
    }

    private void setContactButtonView(final VehicleInfo.ContactInfo vehicleContact) {
        if (!vehicleContact.getPhoneNumber().isEmpty()) {
            contactButton.setImageResource(R.drawable.ic_phone_32dp);
            final UriLauncher launcher = new UriLauncher(getContext(), "tel://" + vehicleContact.getPhoneNumber());

            contactButton.setVisibility(VISIBLE);
            contactButton.setOnClickListener(click -> launcher.launch());
        } else if (!vehicleContact.getUrl().isEmpty()) {
            contactButton.setImageResource(R.drawable.ic_message_32dp);
            final UriLauncher launcher = new UriLauncher(getContext(), vehicleContact.getUrl());
            
            contactButton.setVisibility(VISIBLE);
            contactButton.setOnClickListener(click -> launcher.launch());
        } else {
            contactButton.setVisibility(GONE);
        }
    }
}
