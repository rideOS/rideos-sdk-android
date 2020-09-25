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
package ai.rideos.android.common.view.adapters;

import ai.rideos.android.common.model.SingleSelectOptions;
import ai.rideos.android.common.model.SingleSelectOptions.Option;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

/**
 * SingleSelectArrayAdapter is an adapter for an Android Spinner that displays the SingleSelectOptions model.
 * @param <T> - Type of value stored
 */
public class SingleSelectArrayAdapter<T> extends ArrayAdapter<Option<T>> {

    private final Context context;
    private final List<Option<T>> options;

    public SingleSelectArrayAdapter(final Context context,
                                    final SingleSelectOptions<T> singleSelectOptions) {
        super(context, 0, singleSelectOptions.getOptions());
        this.context = context;
        options = singleSelectOptions.getOptions();
    }

    public Option<T> getOptionAtPosition(final int position) {
        return options.get(position);
    }

    @NonNull
    @Override
    public View getView(final int position, final @Nullable View maybeView, final @NonNull ViewGroup parent) {
        return getViewForLayout(position, maybeView, parent, android.R.layout.simple_spinner_item);
    }

    @Override
    public View getDropDownView(final int position, final @Nullable View maybeView, final @NonNull ViewGroup parent) {
        return getViewForLayout(position, maybeView, parent, android.R.layout.simple_spinner_dropdown_item);
    }

    private View getViewForLayout(final int position,
                                  final @Nullable View maybeView,
                                  final @NonNull ViewGroup parent,
                                  final int layoutToInflate) {
        final TextView listItem;
        if (maybeView == null) {
            listItem = (TextView) LayoutInflater.from(context).inflate(layoutToInflate, parent, false);
        } else {
            listItem = (TextView) maybeView;
        }

        listItem.setText(getOptionAtPosition(position).getDisplayText());
        return listItem;
    }
}
