package org.msf.records.sync;

import android.content.ContentResolver;
import android.database.Cursor;

import java.util.ArrayList;

/**
 * A simple helper method to get all observations for a patient in a nice java bean format.
 */
public class LocalizedChartHelper {

    public static final String KNOWN_CHART_UUID = "ea43f213-66fb-4af6-8a49-70fd6b9ce5d4";
    public static final String KNOWN_PATIENT_UUID = "1802f573-6437-11e4-badf-42010af0dc15";
    public static final String ENGLISH_LOCALE = "en";

    public static final String PULSE_UUID = "";

    /**
     * A simple bean class representing an observation. All names and values have been localized.
     */
    public static class LocalizedObservation {
        /**
         * The time of the encounter (hence the observation) in milliseconds since epoch.
         */
        public final long encounterTimeMillis;
        /**
         * The localized name to the group/section the observation should be displayed in.
         */
        public final String groupName;
        /**
         * The UUID of the concept, unique and guaranteed to be stable, so suitable as a map key.
         */
        public final String conceptUuid;
        /**
         * The localized name of the concept that was observed.
         */
        public final String conceptName;
        /**
         * The value that was observed, converted to a String, and localized in the case of
         * Coded (concept) observations.
         */
        public final String localizedValue;

        public LocalizedObservation(long encounterTimeMillis, String groupName, String conceptUuid,
                                    String conceptName,
                                    String localizedValue) {
            this.encounterTimeMillis = encounterTimeMillis;
            this.groupName = groupName;
            this.conceptUuid = conceptUuid;
            this.conceptName = conceptName;
            this.localizedValue = localizedValue;
        }

        @Override
        public String toString() {
            return "time=" + encounterTimeMillis + ",group=" + groupName + ",conceptUuid=" +
                    conceptUuid + ",conceptName=" + conceptName + ",value=" + localizedValue;
        }
    }

    /**
     * Get all observations for a given patient from the local cache, localized to English.
     */
    public static ArrayList<LocalizedObservation> getObservations(ContentResolver contentResolver,
                                                                  String patientUuid) {
        return getObservations(contentResolver, patientUuid, ENGLISH_LOCALE);
    }

    /**
     * Get all observations for a given patient.
     * @param locale the locale to return the results in, to match the server String
     */
    public static ArrayList<LocalizedObservation> getObservations(ContentResolver contentResolver,
                                                           String patientUuid, String locale) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(ChartProviderContract.makeLocalizedChartUri(
                    KNOWN_CHART_UUID, patientUuid, locale), null, null, null, null);

            ArrayList<LocalizedObservation> result = new ArrayList<>();
            while (cursor.moveToNext()) {
                LocalizedObservation obs = new LocalizedObservation(
                        cursor.getInt(cursor.getColumnIndex("encounter_time")) * 1000L,
                        cursor.getString(cursor.getColumnIndex("group_name")),
                        cursor.getString(cursor.getColumnIndex("concept_uuid")),
                        cursor.getString(cursor.getColumnIndex("concept_name")),
                        cursor.getString(cursor.getColumnIndex("localized_value"))
                );
                result.add(obs);
            }
            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Get the most recent observations for each concept for a given patient from the local cache,
     * localized to English. Ordering will be by concept uuid, and there are not groups or other
     * chart based configurations.
     */
    public static ArrayList<LocalizedObservation> getMostRecentObservations(
            ContentResolver contentResolver, String patientUuid) {
        return getMostRecentObservations(contentResolver, patientUuid, ENGLISH_LOCALE);
    }

    /**
     * Get the most recent observations for each concept for a given patient from the local cache,
     * Ordering will be by concept uuid, and there are not groups or other chart based configurations.
     * @param locale the locale to return the results in, to match the server String
     */
    public static ArrayList<LocalizedObservation> getMostRecentObservations(
            ContentResolver contentResolver, String patientUuid, String locale) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(ChartProviderContract.makeMostRecentChartUri(
                    patientUuid, locale), null, null, null, null);

            ArrayList<LocalizedObservation> result = new ArrayList<>();
            while (cursor.moveToNext()) {
                LocalizedObservation obs = new LocalizedObservation(
                        cursor.getInt(cursor.getColumnIndex("encounter_time")) * 1000L,
                        "", /* no group */
                        cursor.getString(cursor.getColumnIndex("concept_uuid")),
                        cursor.getString(cursor.getColumnIndex("concept_name")),
                        cursor.getString(cursor.getColumnIndex("localized_value"))
                );
                result.add(obs);
            }
            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
