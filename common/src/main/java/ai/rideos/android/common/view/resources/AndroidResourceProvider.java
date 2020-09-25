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
package ai.rideos.android.common.view.resources;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import androidx.core.content.res.ResourcesCompat;
import java.util.Locale;

public class AndroidResourceProvider implements ResourceProvider {
    private final Context context;
    private final Resources resources;

    private AndroidResourceProvider(final Context context) {
        this.context = context;
        resources = context.getResources();
    }

    public static AndroidResourceProvider forContext(final Context context) {
        return new AndroidResourceProvider(context);
    }

    @Override
    public int getColor(final int colorAttributeId) {
        final TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(colorAttributeId, typedValue, true);
        if (typedValue.type == TypedValue.TYPE_REFERENCE) {
            // It's a reference, get the associated color
            return ResourcesCompat.getColor(resources, typedValue.resourceId, null);
        } else {
            // it's a color
            return typedValue.data;
        }
    }

    @Override
    public Locale getLocale() {
        return resources.getConfiguration().getLocales().get(0); // guaranteed to be at least 1 locale
    }

    @Override
    public String getString(final int stringResourceId, final Object... args) {
        return resources.getString(stringResourceId, args);
    }

    @Override
    public String getQuantityString(final int pluralResourceId, final int quantity, final Object... args) {
        return resources.getQuantityString(pluralResourceId, quantity, args);
    }

    @Override
    public int getDrawableId(final int attributeId) {
        final TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attributeId, typedValue, true);
        return typedValue.resourceId;
    }
}
