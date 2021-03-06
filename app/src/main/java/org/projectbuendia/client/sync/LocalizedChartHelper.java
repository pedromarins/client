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

package org.projectbuendia.client.sync;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.util.Pair;

import org.projectbuendia.client.data.app.AppForm;
import org.projectbuendia.client.data.app.AppModel;
import org.projectbuendia.client.model.Concepts;
import org.projectbuendia.client.net.model.ConceptType;
import org.projectbuendia.client.sync.providers.Contracts;
import org.projectbuendia.client.utils.Utils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/** A simple helper class for retrieving and localizing data from patient charts. */
public class LocalizedChartHelper {

    public static final String KNOWN_CHART_UUID = "ea43f213-66fb-4af6-8a49-70fd6b9ce5d4";
    public static final String ENGLISH_LOCALE = "en";

    /** UUIDs for concepts that mean everything is normal; there is no worrying symptom. */
    public static final ImmutableSet<String> NO_SYMPTOM_VALUES = ImmutableSet.of(
            Concepts.NO_UUID, // NO
            Concepts.SOLID_FOOD_UUID, // Solid food
            Concepts.NORMAL_UUID, // NORMAL
            Concepts.NONE_UUID); // None

    private final ContentResolver mContentResolver;

    public LocalizedChartHelper(ContentResolver contentResolver) {
        mContentResolver = checkNotNull(contentResolver);
    }

    public List<Order> getOrders(String patientUuid) {
        Cursor c = mContentResolver.query(
                Contracts.Orders.CONTENT_URI,
                null, "patient_uuid = ?", new String[] {patientUuid}, "start_time");
        List<Order> orders = new ArrayList<>();
        while (c.moveToNext()) {
            orders.add(new Order(
                    Utils.getString(c, "uuid", ""),
                    Utils.getString(c, "instructions", ""),
                    Utils.getLong(c, "start_time", null),
                    Utils.getLong(c, "stop_time", null)));
        }
        c.close();
        return orders;
    }

    /** Gets all observations for a given patient from the local cache, localized to English. */
    public List<LocalizedObs> getObservations(String patientUuid) {
        return getObservations(patientUuid, ENGLISH_LOCALE);
    }

    /** Gets all observations for a given patient, localized for a given locale. */
    public List<LocalizedObs> getObservations(String patientUuid, String locale) {
        Cursor cursor = null;
        try {
            List<LocalizedObs> results = new ArrayList<>();

            // Get all the regular observations with localized names.
            cursor = mContentResolver.query(
                    Contracts.getLocalizedChartUri(
                            KNOWN_CHART_UUID, patientUuid, locale),
                    null, null, null, null);
            while (cursor.moveToNext()) {
                results.add(new LocalizedObs(
                        cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)),
                        cursor.getLong(cursor.getColumnIndex("encounter_time")) * 1000L,
                        cursor.getString(cursor.getColumnIndex("group_name")),
                        cursor.getString(cursor.getColumnIndex("concept_uuid")),
                        cursor.getString(cursor.getColumnIndex("concept_name")),
                        cursor.getString(cursor.getColumnIndex("concept_type")),
                        cursor.getString(cursor.getColumnIndex("value")),
                        cursor.getString(cursor.getColumnIndex("localized_value"))
                ));
            }
            cursor.close();

            // Also get observations representing executed orders.
            cursor = mContentResolver.query(
                    Contracts.Observations.CONTENT_URI, null,
                    "concept_uuid = ? and patient_uuid = ?",
                    new String[] {AppModel.ORDER_EXECUTED_CONCEPT_UUID, patientUuid}, null);
            while (cursor.moveToNext()) {
                results.add(new LocalizedObs(
                        cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)),
                        cursor.getLong(cursor.getColumnIndex("encounter_time")) * 1000L,
                        "",
                        cursor.getString(cursor.getColumnIndex("concept_uuid")),
                        "",
                        ConceptType.NONE.name(),
                        cursor.getString(cursor.getColumnIndex("value")),
                        ""
                ));
            }

            return results;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Gets the most recent observations for each concept for a given patient from the local cache,
     * localized to English. Ordering will be by concept uuid, and there are not groups or other
     * chart based configurations.
     */
    public Map<String, LocalizedObs> getMostRecentObservations(String patientUuid) {
        return getMostRecentObservations(patientUuid, ENGLISH_LOCALE);
    }

    /**
     * Gets the most recent observations for each concept for a given patient from the local cache,
     * Ordering will be by concept uuid, and there are not groups or other chart-based
     * configurations.
     */
    public Map<String, LocalizedObs> getMostRecentObservations(String patientUuid, String locale) {
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(
                    Contracts.getMostRecentChartUri(patientUuid, locale),
                    null, null, null, null);

            Map<String, LocalizedObs> result = Maps.newLinkedHashMap();
            while (cursor.moveToNext()) {
                String conceptUuid = cursor.getString(cursor.getColumnIndex("concept_uuid"));

                LocalizedObs obs = new LocalizedObs(
                        cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)),
                        cursor.getLong(cursor.getColumnIndex("encounter_time")) * 1000L,
                        "", /* no group */
                        conceptUuid,
                        cursor.getString(cursor.getColumnIndex("concept_name")),
                        cursor.getString(cursor.getColumnIndex("concept_type")),
                        cursor.getString(cursor.getColumnIndex("value")),
                        cursor.getString(cursor.getColumnIndex("localized_value"))
                );
                result.put(conceptUuid, obs);
            }
            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Gets the most recent observations for all concepts for a set of patients from the local
     * cache. Ordering will be by concept uuid, and there are not groups or other chart-based
     * configurations.
     */
    public Map<String, Map<String, LocalizedObs>>
            getMostRecentObservationsBatch(String[] patientUuids, String locale) {
        Map<String, Map<String, LocalizedObs>> observations = new HashMap<String, Map<String, LocalizedObs>>();
        for (String patientUuid : patientUuids) {
            observations.put(patientUuid, getMostRecentObservations(patientUuid, locale));
        }
        return observations;
    }

    /** Gets a list of the concept UUIDs and names to show in the rows of the chart grid. */
    public List<Pair<String, String>> getGridRows() {
        Map<String, String> conceptNames = new HashMap<>();
        Cursor cursor = mContentResolver.query(Contracts.ConceptNames.CONTENT_URI, null,
                "locale = ?",  new String[] {ENGLISH_LOCALE}, null);
        try {
            while (cursor.moveToNext()) {
                conceptNames.put(Utils.getString(cursor, "concept_uuid"),
                        Utils.getString(cursor, "name"));
            }
        } finally {
            cursor.close();
        }
        List<Pair<String, String>> conceptUuidsAndNames = new ArrayList<>();
        cursor = mContentResolver.query(Contracts.Charts.CONTENT_URI, null,
                "chart_uuid = ?", new String[] {KNOWN_CHART_UUID}, "chart_row");
        try {
            while (cursor.moveToNext()) {
                String uuid = Utils.getString(cursor, "concept_uuid");
                conceptUuidsAndNames.add(new Pair<>(uuid, conceptNames.get(uuid)));
            }
        } finally {
            cursor.close();
        }
        return conceptUuidsAndNames;
    }

    public List<AppForm> getForms() {
        Cursor cursor = mContentResolver.query(
                Contracts.Forms.CONTENT_URI, null, null, null, null);
        SortedSet<AppForm> forms = new TreeSet<>();
        try {
            while (cursor.moveToNext()) {
                forms.add(new AppForm(
                        Utils.getString(cursor, Contracts.Forms._ID),
                        Utils.getString(cursor, Contracts.Forms.UUID),
                        Utils.getString(cursor, Contracts.Forms.NAME),
                        Utils.getString(cursor, Contracts.Forms.VERSION)));
            }
        } finally {
            cursor.close();
        }
        List<AppForm> sortedForms = new ArrayList<>();
        sortedForms.addAll(forms);
        return sortedForms;
    }
}
