/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.app;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.TextView;

import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.app.backend.common.DatabaseHelper;
import de.symeda.sormas.app.component.menu.PageMenuItem;
import de.symeda.sormas.app.core.IUpdateSubHeadingTitle;

public abstract class BaseReportActivity extends BaseActivity implements IUpdateSubHeadingTitle {

    private final static String TAG = BaseReportActivity.class.getSimpleName();

    private View applicationTitleBar = null;
    private BaseReportFragment activeFragment = null;
    private TextView subHeadingActivityTitle;

    @Override
    protected boolean isSubActivitiy() {
        return false;
    }

    public void setSubHeadingTitle(String title) {
        String t = (title == null) ? "" : title;

        if (subHeadingActivityTitle != null) {
            if (!DataHelper.isNullOrEmpty(title)) {
                subHeadingActivityTitle.setText(title);
                subHeadingActivityTitle.setVisibility(View.VISIBLE);
            } else {
                subHeadingActivityTitle.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected boolean openPage(PageMenuItem menuItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateSubHeadingTitle() {
        String subHeadingTitle = "";

        if (activeFragment != null) {
            PageMenuItem activeMenu = getActivePage();
            subHeadingTitle = (activeMenu == null) ? activeFragment.getSubHeadingTitle() : activeMenu.getTitle();
        }

        setSubHeadingTitle(subHeadingTitle);
    }

    @Override
    public void updateSubHeadingTitle(String title) {
        setSubHeadingTitle(title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dashboard_action_menu, menu);

        processActionbarMenu();

        return true;
    }

    @Override
    protected int getRootActivityLayout() {
        return R.layout.activity_root_with_title_layout;
    }

    public BaseReportFragment getActiveFragment() {
        return activeFragment;
    }

    protected void onCreateInner(Bundle savedInstanceState) {
        subHeadingActivityTitle = (TextView) findViewById(R.id.subHeadingActivityTitle);

        if (showTitleBar()) {
            applicationTitleBar = findViewById(R.id.applicationTitleBar);

            if (applicationTitleBar != null)
                applicationTitleBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        replaceFragment(buildReportFragment());
    }

    protected abstract BaseReportFragment buildReportFragment();

    protected boolean showTitleBar() {
        return true;
    }

    private void replaceFragment(BaseReportFragment f) {
        BaseFragment previousFragment = activeFragment;
        activeFragment = f;

        if (activeFragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.fadein, R.anim.fadeout, R.anim.fadein, R.anim.fadeout);
            ft.replace(R.id.fragment_frame, activeFragment);
            ft.commit();
        }

        updateStatusFrame();
    }

    private void processActionbarMenu() {
        if (activeFragment == null)
            return;
    }
}
