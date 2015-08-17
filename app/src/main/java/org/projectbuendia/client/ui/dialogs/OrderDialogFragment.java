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

package org.projectbuendia.client.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.projectbuendia.client.R;
import org.projectbuendia.client.events.OrderSaveRequestedEvent;
import org.projectbuendia.client.utils.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/** A {@link DialogFragment} for adding a new user. */
public class OrderDialogFragment extends DialogFragment {
    /** Creates a new instance and registers the given UI, if specified. */
    public static OrderDialogFragment newInstance(
            String patientUuid, String previousOrderUuid) {
        Bundle args = new Bundle();
        args.putString("patientUuid", patientUuid);
        args.putString("previousOrderUuid", previousOrderUuid);
        OrderDialogFragment f = new OrderDialogFragment();
        f.setArguments(args);
        return f;
    }

    @InjectView(R.id.order_medication) EditText mMedication;
    @InjectView(R.id.order_dosage) EditText mDosage;
    @InjectView(R.id.order_frequency) EditText mFrequency;
    @InjectView(R.id.order_give_for_days) EditText mGiveForDays;

    private LayoutInflater mInflater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInflater = LayoutInflater.from(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Replace the existing button listener so we can control whether the dialog is dismissed.
        final AlertDialog dialog = (AlertDialog) getDialog();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onSubmit(dialog);
                    }
                }
        );
    }

    public void onSubmit(AlertDialog dialog) {
        String medication = mMedication.getText().toString().trim();
        String dosage = mDosage.getText().toString().trim();
        String frequency = mFrequency.getText().toString().trim();

        String instructions = medication;
        if (!dosage.isEmpty()) {
            instructions += " " + dosage;
        }
        if (!frequency.isEmpty()) {
            instructions += " " + frequency + "x daily";
        }
        String durationStr = mGiveForDays.getText().toString().trim();
        Integer durationDays = durationStr.isEmpty() ? null : Integer.valueOf(durationStr);
        boolean valid = true;
        if (medication.isEmpty()) {
            setError(mMedication, R.string.order_medication_cannot_be_blank);
            valid = false;
        }
        if (durationDays != null && durationDays == 0) {
            setError(mGiveForDays, R.string.order_stop_days_cannot_be_zero);
            valid = false;
        }
        Utils.logUserAction("order_submitted",
                "valid", "" + valid,
                "medication", medication,
                "dosage", dosage,
                "frequency", frequency,
                "instructions", instructions,
                "durationDays", "" + durationDays);

        if (valid) {
            dialog.dismiss();

            // Post an event that triggers the PatientChartController to save the order.
            // TODO: Support revision of previousOrder in addition to creating new orders.
            EventBus.getDefault().post(new OrderSaveRequestedEvent(
                    getArguments().getString("previousOrderUuid"),
                    getArguments().getString("patientUuid"),
                    instructions, durationDays));
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.order_dialog_fragment, null);
        ButterKnife.inject(this, fragment);

        return new AlertDialog.Builder(getActivity())
                .setCancelable(false) // Disable auto-cancel.
                .setTitle(R.string.title_new_order)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setView(fragment)
                .create();
    }

    private void setError(EditText field, int resourceId) {
        field.setError(getResources().getString(resourceId));
        field.invalidate();
        field.requestFocus();
    }
}