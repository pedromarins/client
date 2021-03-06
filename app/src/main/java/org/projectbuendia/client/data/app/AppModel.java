// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.data.app;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import org.joda.time.DateTime;
import org.projectbuendia.client.data.app.converters.AppTypeConverter;
import org.projectbuendia.client.data.app.converters.AppTypeConverters;
import org.projectbuendia.client.data.app.tasks.AddPatientTask;
import org.projectbuendia.client.data.app.tasks.TaskFactory;
import org.projectbuendia.client.data.app.tasks.AppUpdatePatientTask;
import org.projectbuendia.client.data.app.tasks.FetchItemTask;
import org.projectbuendia.client.events.CleanupSubscriber;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.AppLocationTreeFetchedEvent;
import org.projectbuendia.client.events.data.ItemCreatedEvent;
import org.projectbuendia.client.events.data.ItemFetchedEvent;
import org.projectbuendia.client.events.data.ItemUpdatedEvent;
import org.projectbuendia.client.events.data.TypedCursorFetchedEvent;
import org.projectbuendia.client.events.data.TypedCursorFetchedEventFactory;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.filter.db.patient.UuidFilter;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.sync.providers.Contracts;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import de.greenrobot.event.NoSubscriberEvent;

/**
 * A model that manages all data access within the application.
 *
 * <p>This model's {@code fetch} methods often provide {@link TypedCursor}s as results, which MUST
 * be closed when the consumer is done with them.
 *
 * <p>Updates done through this model are written through to a backing {@link Server}; callers do
 * not need to worry about the implementation details of this.
 */
public class AppModel {
    private static final Logger LOG = Logger.create();
    public static final String ORDER_EXECUTED_CONCEPT_UUID = "buendia-concept-order_executed";

    private final ContentResolver mContentResolver;
    private final AppTypeConverters mConverters;
    private final TaskFactory mTaskFactory;

    AppModel(ContentResolver contentResolver,
            AppTypeConverters converters,
            TaskFactory taskFactory) {
        mContentResolver = contentResolver;
        mConverters = converters;
        mTaskFactory = taskFactory;
    }

    /**
     * Returns true iff the model has previously been fully downloaded from the server--that is, if
     * locations, patients, users, charts, and observations were all downloaded at some point. Note
     * that this data may be out-of-date, but must be present in some form for proper operation of
     * the app.
     */
    public boolean isFullModelAvailable() {
        return getLastFullSyncTime() != null;
    }

    public DateTime getLastFullSyncTime() {
        // The sync process is transactional, but in rare cases, a sync may complete without ever
        // having started--this is the case if user data is cleared mid-sync, for example. To check
        // that a sync actually completed, we look at the FULL_SYNC_START_TIME and
        // FULL_SYNC_END_TIME columns in the Misc table, which are written to as the first and
        // last operations of a complete sync. If both of these fields are present, and the last
        // end time is greater than the last start time, then a full sync must have completed.
        try (Cursor c = mContentResolver.query(
                Contracts.Misc.CONTENT_URI, null, null, null, null)) {
            LOG.d("Sync timing result count: %d", c.getCount());
            if (c.moveToNext()) {
                DateTime fullSyncStart = Utils.getDateTime(c, Contracts.Misc.FULL_SYNC_START_TIME);
                DateTime fullSyncEnd = Utils.getDateTime(c, Contracts.Misc.FULL_SYNC_END_TIME);
                DateTime obsSyncEnd = Utils.getDateTime(c, Contracts.Misc.OBS_SYNC_TIME);
                LOG.i("full_sync_start_time = %s, full_sync_end_time = %s, obs_sync_time = %s",
                        fullSyncStart, fullSyncEnd, obsSyncEnd);
                if (fullSyncStart != null && fullSyncEnd != null && fullSyncEnd.isAfter(fullSyncStart)) {
                    return fullSyncEnd;
                }
            }
            return null;
        }
    }

    /**
     * Asynchronously fetches all locations as a tree, posting an
     * {@link AppLocationTreeFetchedEvent} on the specified event bus when complete.
     */
    public void fetchLocationTree(CrudEventBus bus, String locale) {
        bus.registerCleanupSubscriber(new CrudEventBusCleanupSubscriber(bus));

        FetchLocationTreeAsyncTask task = new FetchLocationTreeAsyncTask(
                mContentResolver,
                locale,
                mConverters.location,
                bus);
        task.execute();
    }

    /**
     * Asynchronously fetches patients, posting a {@link TypedCursorFetchedEvent} with
     * {@link AppPatient}s on the specified event bus when complete.
     */
    public void fetchPatients(CrudEventBus bus, SimpleSelectionFilter filter, String constraint) {
        bus.registerCleanupSubscriber(new CrudEventBusCleanupSubscriber(bus));

        FetchTypedCursorAsyncTask<AppPatient> task = new FetchTypedCursorAsyncTask<>(
                Contracts.Patients.CONTENT_URI,
                null,
                AppPatient.class,
                mContentResolver,
                filter,
                constraint,
                mConverters.patient,
                bus);
        task.execute();
    }

    /**
     * Asynchronously fetches a single patient by UUID, posting a {@link ItemFetchedEvent}
     * with the {@link AppPatient} on the specified event bus when complete.
     */
    public void fetchSinglePatient(CrudEventBus bus, String uuid) {
        FetchItemTask<AppPatient> task = mTaskFactory.newFetchSingleAsyncTask(
                Contracts.Patients.CONTENT_URI,
                null,
                new UuidFilter(),
                uuid,
                mConverters.patient,
                bus);
        task.execute();
    }

    /**
     * Asynchronously fetches patients, posting a {@link TypedCursorFetchedEvent} with
     * {@link AppUser}s on the specified event bus when complete.
     */
    public void fetchUsers(CrudEventBus bus) {
        // Register for error events so that we can close cursors if we need to.
        bus.registerCleanupSubscriber(new CrudEventBusCleanupSubscriber(bus));

        // TODO: Asynchronously fetch users or delete this function.
    }

    /**
     * Asynchronously adds a patient, posting a
     * {@link ItemCreatedEvent} with the newly-added patient on
     * the specified event bus when complete.
     */
    public void addPatient(CrudEventBus bus, AppPatientDelta patientDelta) {
        AddPatientTask task = mTaskFactory.newAddPatientAsyncTask(patientDelta, bus);
        task.execute();
    }

    /**
     * Asynchronously updates a patient, posting a
     * {@link ItemUpdatedEvent} with the updated
     * {@link AppPatient} on the specified event bus when complete.
     */
    public void updatePatient(
            CrudEventBus bus, AppPatient originalPatient, AppPatientDelta patientDelta) {
        AppUpdatePatientTask task =
                mTaskFactory.newUpdatePatientAsyncTask(originalPatient, patientDelta, bus);
        task.execute();
    }

    /**
     * Asynchronously adds an order, posting a
     * {@link ItemCreatedEvent} when complete.
     */
    public void addOrder(CrudEventBus bus, AppOrder order) {
        mTaskFactory.newAddOrderAsyncTask(order, bus).execute();
    }

    /**
     * Asynchronously adds an encounter that records an order as executed, posting a
     * {@link ItemCreatedEvent} when complete.
     */
    public void addOrderExecutedEncounter(CrudEventBus bus, AppPatient patient, String orderUuid) {
        addEncounter(bus, patient, new AppEncounter(
                patient.uuid, null, DateTime.now(), null, new String[] { orderUuid }
        ));
    }

    /**
     * Asynchronously adds an encounter to a patient, posting a
     * {@link ItemCreatedEvent} when complete.
     */
    public void addEncounter(CrudEventBus bus, AppPatient appPatient, AppEncounter appEncounter) {
        mTaskFactory.newAddEncounterAsyncTask(appPatient, appEncounter, bus).execute();
    }

    /** A subscriber that handles error events posted to {@link CrudEventBus}es. */
    private static class CrudEventBusCleanupSubscriber implements CleanupSubscriber {

        private final CrudEventBus mBus;

        public CrudEventBusCleanupSubscriber(CrudEventBus bus) {
            mBus = bus;
        }

        @Override
        @SuppressWarnings("unused") // Called by reflection from event bus.
        public void onEvent(NoSubscriberEvent event) {
            if (event.originalEvent instanceof TypedCursorFetchedEvent<?>) {
                // If no subscribers were registered for a DataFetchedEvent, then the TypedCursor in
                // the event won't be managed by anyone else; therefore, we close it ourselves.
                ((TypedCursorFetchedEvent<?>) event.originalEvent).cursor.close();
            } else if (event.originalEvent instanceof AppLocationTreeFetchedEvent) {
                ((AppLocationTreeFetchedEvent) event.originalEvent).tree.close();
            }

            mBus.unregisterCleanupSubscriber(this);
        }

        @Override
        public void onAllUnregistered() {
            mBus.unregisterCleanupSubscriber(this);
        }
    }

    private static class FetchLocationTreeAsyncTask extends AsyncTask<Void, Void, AppLocationTree> {

        private final ContentResolver mContentResolver;
        private final String mLocale;
        private final AppTypeConverter<AppLocation> mConverter;
        private final CrudEventBus mBus;

        public FetchLocationTreeAsyncTask(
                ContentResolver contentResolver,
                String locale,
                AppTypeConverter<AppLocation> converter,
                CrudEventBus bus) {
            mContentResolver = contentResolver;
            mLocale = locale;
            mConverter = converter;
            mBus = bus;
        }

        @Override
        protected AppLocationTree doInBackground(Void... voids) {
            Cursor cursor = null;
            try {
                // TODO: Ensure this cursor is closed.
                cursor = mContentResolver.query(
                        Contracts.getLocalizedLocationsUri(mLocale),
                        null,
                        null,
                        null,
                        null);

                return AppLocationTree
                        .forTypedCursor(new TypedConvertedCursor<>(mConverter, cursor));
            } catch (Exception e) {
                if (cursor != null) {
                    cursor.close();
                }

                throw e;
            }
        }

        @Override
        protected void onPostExecute(AppLocationTree result) {
            mBus.post(new AppLocationTreeFetchedEvent(result));
        }
    }

    private static class FetchTypedCursorAsyncTask<T extends AppTypeBase>
            extends AsyncTask<Void, Void, TypedCursor<T>> {

        private final Uri mContentUri;
        private final String[] mProjection;
        private final Class<T> mClazz;
        private final ContentResolver mContentResolver;
        private final SimpleSelectionFilter mFilter;
        private final String mConstraint;
        private final AppTypeConverter<T> mConverter;
        private final CrudEventBus mBus;

        public FetchTypedCursorAsyncTask(
                Uri contentUri,
                String[] projection,
                Class<T> clazz,
                ContentResolver contentResolver,
                SimpleSelectionFilter<T> filter,
                String constraint,
                AppTypeConverter<T> converter,
                CrudEventBus bus) {
            mContentUri = contentUri;
            mProjection = projection;
            mClazz = clazz;
            mContentResolver = contentResolver;
            mFilter = filter;
            mConstraint = constraint;
            mConverter = converter;
            mBus = bus;
        }

        @Override
        protected TypedCursor<T> doInBackground(Void... voids) {
            Cursor cursor = null;
            try {
                cursor = mContentResolver.query(
                        mContentUri,
                        mProjection,
                        mFilter.getSelectionString(),
                        mFilter.getSelectionArgs(mConstraint),
                        null);

                return new TypedConvertedCursor<>(mConverter, cursor);
            } catch (Exception e) {
                if (cursor != null) {
                    cursor.close();
                }

                throw e;
            }
        }

        @Override
        protected void onPostExecute(TypedCursor<T> result) {
            mBus.post(TypedCursorFetchedEventFactory.createEvent(mClazz, result));
        }
    }
}
