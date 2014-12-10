package org.msf.records;

import android.app.Application;
import android.content.ContentResolver;
import android.content.res.Resources;

import org.msf.records.data.app.AppModelModule;
import org.msf.records.events.EventsModule;
import org.msf.records.net.NetModule;
import org.msf.records.prefs.PrefsModule;
import org.msf.records.ui.BaseActivity;
import org.msf.records.ui.PatientChartActivity;
import org.msf.records.ui.PatientChartFragment;
import org.msf.records.ui.PatientListActivity;
import org.msf.records.ui.PatientSearchActivity;
import org.msf.records.ui.RoundActivity;
import org.msf.records.ui.TentSelectionActivity;
import org.msf.records.updater.UpdateModule;
import org.msf.records.user.UserModule;
import org.msf.records.utils.UtilsModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A Dagger module that provides the top-level bindings for the app.
 */
@Module(
        includes = {
                AppModelModule.class,
                EventsModule.class,
                NetModule.class,
                PrefsModule.class,
                UpdateModule.class,
                UserModule.class,
                UtilsModule.class
        },
        injects = {
                App.class,

                // TODO(dxchen): Move these into activity-specific modules.
                // Activities
                BaseActivity.class,
                PatientChartActivity.class,
                PatientListActivity.class,
                PatientSearchActivity.class,
                RoundActivity.class,
                TentSelectionActivity.class,

                // TODO(dxchen): Move these into fragment-specific modules.
                PatientChartFragment.class
        }
)
public final class AppModule {

    private final App mApp;

    public AppModule(App app) {
        mApp = app;
    }

    @Provides @Singleton Application provideApplication() {
        return mApp;
    }

    @Provides @Singleton ContentResolver provideContentResolver(Application app) {
        return app.getContentResolver();
    }

    @Provides @Singleton Resources provideResources(Application app) {
        return app.getResources();
    }
}
