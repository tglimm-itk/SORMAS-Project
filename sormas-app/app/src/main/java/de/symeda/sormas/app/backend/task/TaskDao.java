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

package de.symeda.sormas.app.backend.task;

import android.location.Location;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.caze.CaseLogic;
import de.symeda.sormas.api.task.TaskStatus;
import de.symeda.sormas.api.task.TaskType;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.ValidationException;
import de.symeda.sormas.app.backend.caze.Case;
import de.symeda.sormas.app.backend.caze.CaseDtoHelper;
import de.symeda.sormas.app.backend.common.AbstractAdoDao;
import de.symeda.sormas.app.backend.common.AbstractDomainObject;
import de.symeda.sormas.app.backend.common.DaoException;
import de.symeda.sormas.app.backend.config.ConfigProvider;
import de.symeda.sormas.app.backend.contact.Contact;
import de.symeda.sormas.app.backend.event.Event;
import de.symeda.sormas.app.util.LocationService;

public class TaskDao extends AbstractAdoDao<Task> {

    public TaskDao(Dao<Task,Long> innerDao) {
        super(innerDao);
    }

    @Override
    protected Class<Task> getAdoClass() {
        return Task.class;
    }

    @Override
    public String getTableName() {
        return Task.TABLE_NAME;
    }

    @Override
    public Task saveAndSnapshot(Task task) throws DaoException {

        if ((task.getTaskStatus() == TaskStatus.NOT_EXECUTABLE || task.getTaskStatus() == TaskStatus.DONE)
            && task.getClosedLat() == null) {
            Location location = LocationService.instance().getLocation();
            if (location != null) {
                task.setClosedLat(location.getLatitude());
                task.setClosedLon(location.getLongitude());
                task.setClosedLatLonAccuracy(location.getAccuracy());
            }
        }

        return super.saveAndSnapshot(task);
    }

    /**
     * query for the following PENDING tasks:
     * 1. suggested start within range
     * 2. due date within range
     * 3. localChangeDate within range, not modified (-> has been updated on server) and suggested start before end of range
     * Ordered by priority, then due date - oldes (most due) first
     * Typically, the range is the interval between two notification polls (e.g. 2 minutes)
     * @return
     */
    public List<Task> queryMyPendingForNotification(Date rangeStart, Date rangeEnd) {

        if (ConfigProvider.getUser() == null) {
            return new ArrayList<Task>();
        }

        try {
            QueryBuilder builder = queryBuilder();
            Where where = builder.where();
            where.and(
                    where.eq(AbstractDomainObject.SNAPSHOT, false),
                    where.eq(Task.ASSIGNEE_USER + "_id", ConfigProvider.getUser()),
                    where.eq(Task.TASK_STATUS, TaskStatus.PENDING),
                    where.or(
                            where.between(Task.SUGGESTED_START, rangeStart, rangeEnd),
                            where.between(Task.DUE_DATE, rangeStart, rangeEnd),
                            where.and(
                                    where.between(Task.LOCAL_CHANGE_DATE, rangeStart, rangeEnd),
                                    where.eq(Task.MODIFIED, false),
                                    where.or(
                                            where.raw(Task.LAST_OPENED_DATE + " < " + Task.LOCAL_CHANGE_DATE),
                                            where.isNull(Task.LAST_OPENED_DATE)
                                    ),
                                    where.le(Task.SUGGESTED_START, rangeEnd)
                            )
                    )
            );

            builder.orderBy(Task.PRIORITY, true).orderBy(Task.DUE_DATE, true);

            builder.prepareStatementString();
            return builder.query();
        } catch (SQLException e) {
            android.util.Log.e(getTableName(), "Could not perform queryMyPendingForNotification on Task");
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets all pending tasks.
     * Ordered by priority, then due date - oldest (most due) first
     * @return
     */
    public List<Task> queryMyPending() {
        try {
            QueryBuilder builder = queryBuilder();
            Where where = builder.where();
            where.and(
                    where.eq(AbstractDomainObject.SNAPSHOT, false),
                    where.eq(Task.ASSIGNEE_USER + "_id", ConfigProvider.getUser()),
                    where.eq(Task.TASK_STATUS, TaskStatus.PENDING)
            );

            return builder
                    .orderBy(Task.PRIORITY, true).orderBy(Task.DUE_DATE, true)
                    .query();
        } catch (SQLException e) {
            android.util.Log.e(getTableName(), "Could not perform queryMyPending on Task");
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets all not executable tasks.
     * Ordered by due date - newest first
     * @return
     */
    public List<Task> queryMyNotExecutable() {
        try {
            QueryBuilder builder = queryBuilder();
            Where where = builder.where();
            where.and(
                    where.eq(AbstractDomainObject.SNAPSHOT, false),
                    where.eq(Task.ASSIGNEE_USER + "_id", ConfigProvider.getUser()),
                    where.eq(Task.TASK_STATUS, TaskStatus.NOT_EXECUTABLE)
            );

            return builder
                    .orderBy(Task.PRIORITY, true).orderBy(Task.DUE_DATE, true)
                    .query();
        } catch (SQLException e) {
            android.util.Log.e(getTableName(), "Could not perform queryMyNotExecutable on Task");
            throw new RuntimeException(e);
        }
    }
    /**
     * Gets all done and discarded tasks.
     * Ordered by due date - newest first
     * @return
     */
    public List<Task> queryMyDoneOrRemoved() {
        try {
            QueryBuilder builder = queryBuilder();
            Where where = builder.where();
            where.and(
                    where.eq(AbstractDomainObject.SNAPSHOT, false),
                    where.eq(Task.ASSIGNEE_USER + "_id", ConfigProvider.getUser()),
                    where.eq(Task.TASK_STATUS, TaskStatus.DONE).or().eq(Task.TASK_STATUS, TaskStatus.REMOVED));

            return builder
                    .orderBy(Task.PRIORITY, true).orderBy(Task.DUE_DATE, true)
                    .query();
        } catch (SQLException e) {
            android.util.Log.e(getTableName(), "Could not perform queryMyDoneOrRemoved on Task");
            throw new RuntimeException(e);
        }
    }

    public List<Task> queryByCase(Case caze) {
        if (caze.isSnapshot()) {
            throw new IllegalArgumentException("Does not support snapshot entities");
        }
        try {
            return queryBuilder()
                    .orderBy(Task.PRIORITY, true).orderBy(Task.DUE_DATE, true)
                    .where().eq(Task.CAZE + "_id", caze)
                    .and().eq(AbstractDomainObject.SNAPSHOT, false)
                    .query();
        } catch (SQLException e) {
            android.util.Log.e(getTableName(), "Could not perform queryByCase on Task");
            throw new RuntimeException(e);
        }
    }

    public List<Task> queryByContact(Contact contact) {
        if (contact.isSnapshot()) {
            throw new IllegalArgumentException("Does not support snapshot entities");
        }
        try {
            return queryBuilder().orderBy(Task.PRIORITY, true)
                    .orderBy(Task.DUE_DATE, true)
                    .where().eq(Task.CONTACT + "_id", contact)
                    .and().eq(AbstractDomainObject.SNAPSHOT, false)
                    .query();
        } catch (SQLException e) {
            android.util.Log.e(getTableName(), "Could not perform queryByContact on Task");
            throw new RuntimeException(e);
        }
    }

    public List<Task> queryByEvent(Event event) {
        if (event.isSnapshot()) {
            throw new IllegalArgumentException("Does not support snapshot entities");
        }
        try {
            return queryBuilder()
                    .orderBy(Task.PRIORITY, true).orderBy(Task.DUE_DATE, true)
                    .where().eq(Task.EVENT + "_id", event)
                    .and().eq(AbstractDomainObject.SNAPSHOT, false)
                    .query();
        } catch (SQLException e) {
            android.util.Log.e(getTableName(), "Could not perform queryByEvent on Task");
            throw new RuntimeException(e);
        }
    }

}
