package org.msf.records.filter;

import org.msf.records.sync.PatientProviderContract;

/**
 * Returns only the patient with the given UUID.
 */
public final class UuidFilter implements SimpleSelectionFilter {

    @Override
    public String getSelectionString() {
        return PatientProviderContract.PatientColumns.COLUMN_NAME_UUID + "=?";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return new String[] { constraint.toString() };
    }
}
