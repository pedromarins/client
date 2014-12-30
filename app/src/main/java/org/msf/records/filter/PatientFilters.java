package org.msf.records.filter;

import org.msf.records.events.location.LocationsLoadedEvent;
import org.msf.records.filter.FilterGroup.FilterType;
import org.msf.records.location.LocationTree;
import org.msf.records.location.LocationTree.LocationSubtree;

import de.greenrobot.event.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * All available patient filters available to the user, categorized by filter type.
 */
public final class PatientFilters {

    // TODO(rjlothian): Remove this. Static mutable state is a common source of bugs.
    private static LocationTree sRoot = null;

    // Id and name filters should always be applied.
    private static final FilterGroup BASE_FILTERS =
            new FilterGroup(
                    FilterType.OR,
                    new IdFilter(),
                    new NameFilter()).setName("All Patients");

    private static final SimpleSelectionFilter[] OTHER_FILTERS = new SimpleSelectionFilter[] {
//        new FilterGroup(BASE_FILTERS, new ConceptFilter(Concept.PREGNANCY_UUID, Concept.YES_UUID)),
        new FilterGroup(BASE_FILTERS, new AgeFilter(5)).setName("Children Under 5"),
        new FilterGroup(BASE_FILTERS, new AgeFilter(2)).setName("Children Under 2")
    };

    public static SimpleSelectionFilter getDefaultFilter() {
        return BASE_FILTERS;
    }

    public static SimpleSelectionFilter[] getZoneFilters() {
        List<SimpleSelectionFilter> filters = new ArrayList<>();
        if (sRoot != null) {
            for (LocationSubtree zone : sRoot.getLocationsForDepth(1)) {
                filters.add(new FilterGroup(
                        BASE_FILTERS,
                        new LocationUuidFilter(zone)).setName(zone.toString()));
            }
        }
        SimpleSelectionFilter[] filterArray = new SimpleSelectionFilter[filters.size()];
        filters.toArray(filterArray);
        return filterArray;
    }

    public static SimpleSelectionFilter[] getOtherFilters() {
        return OTHER_FILTERS;
    }

    public static SimpleSelectionFilter[] getFiltersForDisplay() {
        List<SimpleSelectionFilter> allFilters = new ArrayList<>();
        allFilters.add(getDefaultFilter());
        Collections.addAll(allFilters, getZoneFilters());
        allFilters.add(null); // Section break
        Collections.addAll(allFilters, getOtherFilters());

        SimpleSelectionFilter[] filterArray = new SimpleSelectionFilter[allFilters.size()];
        allFilters.toArray(filterArray);
        return filterArray;
    }

    @SuppressWarnings("unused") // Called by reflection from event bus.
    private static class LocationSyncSubscriber {
        public synchronized void onEvent(LocationsLoadedEvent event) {
            sRoot = event.locationTree;
        }
    }

    // TODO(rjlothian): This is likely to cause problems for testability. Remove it.
    static {
        EventBus.getDefault().register(new LocationSyncSubscriber());
    }
}
