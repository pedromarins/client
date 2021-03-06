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

package org.projectbuendia.client.net;

import android.support.annotation.Nullable;

import com.android.volley.Response;

import org.projectbuendia.client.data.app.AppEncounter;
import org.projectbuendia.client.data.app.AppOrder;
import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.data.app.AppPatientDelta;
import org.projectbuendia.client.net.model.Encounter;
import org.projectbuendia.client.net.model.Form;
import org.projectbuendia.client.net.model.Location;
import org.projectbuendia.client.net.model.NewUser;
import org.projectbuendia.client.net.model.Order;
import org.projectbuendia.client.net.model.Patient;
import org.projectbuendia.client.net.model.User;

import java.util.List;

/** An interface abstracting the idea of an RPC to a server. */
public interface Server {
    public static final String PATIENT_ID_KEY = "id";
    public static final String PATIENT_UUID_KEY = "uuid";
    public static final String PATIENT_GIVEN_NAME_KEY = "given_name";
    public static final String PATIENT_FAMILY_NAME_KEY = "family_name";
    public static final String PATIENT_BIRTHDATE_KEY = "birthdate";
    public static final String PATIENT_GENDER_KEY = "gender";
    public static final String PATIENT_ASSIGNED_LOCATION = "assigned_location";
    public static final String ENCOUNTER_OBSERVATIONS_KEY = "observations";
    public static final String ENCOUNTER_TIMESTAMP = "timestamp";
    public static final String ENCOUNTER_ORDER_UUIDS = "order_uuids";
    public static final String OBSERVATION_QUESTION_UUID = "question_uuid";
    public static final String OBSERVATION_ANSWER_DATE = "answer_date";
    public static final String OBSERVATION_ANSWER_UUID = "answer_uuid";

    /**
     * Logs an event by sending a dummy request to the server.  (The server logs
     * can then be scanned later to produce analytics for the client app.)
     * @param pairs An even number of arguments providing key-value pairs of
     *              arbitrary data to record with the event.
     */
    void logToServer(List<String> pairs);

    /** Adds a patient. */
    void addPatient(
            AppPatientDelta patientDelta,
            Response.Listener<Patient> patientListener,
            Response.ErrorListener errorListener);

    /** Updates a patient. */
    public void updatePatient(
            String patientId,
            AppPatientDelta patientDelta,
            Response.Listener<Patient> patientListener,
            Response.ErrorListener errorListener);

    /**
     * Creates a new user.
     *
     * @param user the NewUser to add
     */
    public void addUser(
            NewUser user,
            Response.Listener<User> userListener,
            Response.ErrorListener errorListener);

    /**
     * Creates a new encounter for a given patient.
     *
     * @param patient the patient being observed
     * @param encounter the encounter to add
     */
    void addEncounter(
            AppPatient patient,
            AppEncounter encounter,
            Response.Listener<Encounter> encounterListener,
            Response.ErrorListener errorListener);

    /**
     * Get the patient record for an existing patient. Currently we are just using a String-String
     * map for parameters, but this is a bit close in implementation details to the old Buendia UI
     * so it will probably need to be generalized in future.
     *
     * @param patientId the unique patient id representing the patients
     */
    public void getPatient(
            String patientId,
            Response.Listener<Patient> patientListener,
            Response.ErrorListener errorListener);

    /**
     * Updates the location of a patient.
     *
     * @param patientId the id of the patient to update
     * @param newLocationId the id of the new location that the patient is assigned to
     */
    public void updatePatientLocation(String patientId, String newLocationId);

    /** Lists all existing patients. */
    public void listPatients(@Nullable String filterState,
                             @Nullable String filterLocation,
                             @Nullable String filterQueryTerm,
                             Response.Listener<List<Patient>> patientListener,
                             Response.ErrorListener errorListener);

    /** Lists all existing users. */
    public void listUsers(@Nullable String filterQueryTerm,
                          Response.Listener<List<User>> userListener,
                          Response.ErrorListener errorListener);

    /** Lists all published forms. */
    void listForms(Response.Listener<List<Form>> userListener,
                   Response.ErrorListener errorListener);

    /**
     * Adds a new location to the server.
     *
     * @param location uuid must not be set, parent_uuid must be set, and the names map must have a
     *                 name for at least one locale.
     * @param locationListener the listener to be informed of the newly added location
     * @param errorListener listener to be informed of any errors
     */
    public void addLocation(Location location,
                            final Response.Listener<Location> locationListener,
                            final Response.ErrorListener errorListener);

    /**
     * Updates the names for a location on the server.
     *
     * @param location the location, only uuid and new locale names for the location will be used,
     *                 but ideally the other arguments should be correct
     * @param locationListener the listener to be informed of the newly added location
     * @param errorListener listener to be informed of any errors
     */
    public void updateLocation(Location location,
                               final Response.Listener<Location> locationListener,
                               final Response.ErrorListener errorListener);

    /**
     * Deletes a given location from the server. The location should not be the EMC location or
     * one of the zones - just a client added location, tent or bed.
     */
    public void deleteLocation(String locationUuid,
                               final Response.ErrorListener errorListener);

    /** Lists all locations. */
    public void listLocations(Response.Listener<List<Location>> locationListener,
                              Response.ErrorListener errorListener);

    /** Lists all existing orders. */
    public void listOrders(Response.Listener<List<Order>> successListener,
                           Response.ErrorListener errorListener);

    /** Adds an order for a patient. */
    void addOrder(AppOrder order,
                  Response.Listener<Order> successListener,
                  Response.ErrorListener errorListener);

    /** Cancels all pending requests. */
    public void cancelPendingRequests();
}
