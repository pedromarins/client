package org.msf.records.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.msf.records.R;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.model.LocationTree;
import org.msf.records.model.LocationTreeFactory;
import org.msf.records.model.Zone;
import org.msf.records.view.SubtitledButtonView;

import java.util.TreeSet;

/**
 * Created by akalachman on 11/30/14.
 */
public class TentListAdapter extends ArrayAdapter<LocationTree> {
    private final Context context;

    public TentListAdapter(Context context) {
        super(context, R.layout.listview_cell_tent_selection, getTents(context));
        this.context = context;
    }

    private static LocationTree[] getTents(Context context) {
        LocationTree tree = new LocationTreeFactory(context).build();

        TreeSet<LocationTree> tents;
        if (tree == null) {
            tents = new TreeSet<LocationTree>();
        } else {
            tents = tree.getLocationsForDepth(2);
        }

        LocationTree[] values = new LocationTree[tents.size()];
        tents.toArray(values);
        return values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(
                R.layout.listview_cell_tent_selection, parent, false);

        LocationTree tent = getItem(position);
        SubtitledButtonView button =
                (SubtitledButtonView)rowView.findViewById(R.id.tent_selection_tent);
        button.setTitle(tent.toString());
        // TODO(akalachman): Make resource string, pluralize.
        button.setSubtitle(tent.getPatientCount() + " patients");
        button.setBackgroundResource(
                Zone.getBackgroundColorResource(tent.getLocation().parent_uuid));
        button.setTextColor(
                Zone.getForegroundColorResource(tent.getLocation().parent_uuid));

        return rowView;
    }
}
