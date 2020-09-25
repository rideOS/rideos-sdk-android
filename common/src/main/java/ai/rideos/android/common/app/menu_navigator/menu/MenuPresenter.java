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
package ai.rideos.android.common.app.menu_navigator.menu;

import ai.rideos.android.common.R;
import ai.rideos.android.common.app.menu_navigator.LoggedOutListener;
import ai.rideos.android.common.app.menu_navigator.menu.navigation_header.NavigationHeaderPresenter;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.model.MenuOption;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageWriter;
import ai.rideos.android.common.view.Presenter;
import android.content.Context;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import java.util.List;

// TODO we should probably rename this concept of "ViewController". Fragments are going to replace ViewControllers, but
// Fragments create views of their own. We need some concept of a class that handles an already existing view.
public class MenuPresenter implements Presenter<DrawerLayout> {

    private final NavigationHeaderPresenter headerViewController;
    private final MenuViewModel menuViewModel;
    private final List<MenuOption> menuOptions;
    private DrawerLayout drawerLayout;
    private MenuSelectionListener selectionListener = selection -> {};

    public MenuPresenter(final Context context,
                         final List<MenuOption> menuOptions,
                         final LoggedOutListener loggedOutListener) {
        // For now the logic here does not require a view model, but if it becomes more complicated than this, one
        // should be made
        this.menuOptions = menuOptions;
        headerViewController = new NavigationHeaderPresenter(context);
        menuViewModel = new DefaultMenuViewModel(
            User.get(context),
            SharedPreferencesUserStorageWriter.forContext(context),
            loggedOutListener
        );
    }

    /**
     * Slide the menu open. Should be called after the drawer layout is attached.
     */
    public void openMenu() {
        if (drawerLayout != null) {
            setMenuVisibility(true, drawerLayout);
        }
    }

    /**
     * Slide the menu closed. Should be called after the drawer layout is attached.
     */
    public void closeMenu() {
        if (drawerLayout != null) {
            setMenuVisibility(false, drawerLayout);
        }
    }

    public boolean isMenuOpen() {
        return drawerLayout.isDrawerOpen(Gravity.START);
    }

    public void setSelectedOption(final MenuOption optionToSelect) {
        final NavigationView navigationView = drawerLayout.findViewById(R.id.drawer_nav_view);
        navigationView.setCheckedItem(optionToSelect.getId());
        navigationView.getMenu().performIdentifierAction(optionToSelect.getId(), 0);
    }

    public void setSelectionListener(final MenuSelectionListener selectionListener) {
        this.selectionListener = selectionListener;
    }

    @Override
    public void attach(final DrawerLayout drawerLayout) {
        this.drawerLayout = drawerLayout;
        final NavigationView navigationView = drawerLayout.findViewById(R.id.drawer_nav_view);
        final Menu menu = navigationView.getMenu();
        setMenuOptions(menuOptions, menu);

        final Button signOutButton = navigationView.findViewById(R.id.sign_out_button);
        signOutButton.setOnClickListener(click -> menuViewModel.logout());

        final View navigationHeaderView = navigationView.getHeaderView(0);
        headerViewController.attach(navigationHeaderView);
    }

    @Override
    public void detach() {
        headerViewController.detach();
    }

    private void setMenuOptions(final List<MenuOption> menuOptions, final Menu menu) {
        menu.clear();
        for (int i = 0; i < menuOptions.size(); i++) {
            final MenuOption option = menuOptions.get(i);
            final MenuItem viewItem = menu.add(0, option.getId(), i, option.getTitle());
            viewItem.setCheckable(true);
            viewItem
                .setIcon(option.getDrawableIcon())
                .setOnMenuItemClickListener(item -> {
                    selectionListener.selectedMenuOption(option);
                    closeMenu();
                    return true;
                });
        }
    }

    private void setMenuVisibility(final boolean shouldShow, final DrawerLayout drawerLayout) {
        final boolean isDrawerAlreadyOpen = isMenuOpen();
        if (shouldShow && !isDrawerAlreadyOpen) {
            drawerLayout.openDrawer(Gravity.START);
        } else if (!shouldShow && isDrawerAlreadyOpen) {
            drawerLayout.closeDrawers();
        }
    }
}
