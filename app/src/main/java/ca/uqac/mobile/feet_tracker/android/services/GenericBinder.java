package ca.uqac.mobile.feet_tracker.android.services;

import android.app.Service;
import android.os.Binder;

/**
 * Created by MeTaL125 on 2017-11-21.
 */

public class GenericBinder<T extends Service> extends Binder {
    private T mService;

    public GenericBinder(T service) {
        this.mService = service;
    }

    public T getService() {
        return mService;
    }
}
