package ca.uqac.mobile.feet_tracker.tools.maps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.SupportMapFragment;

/**
 * Created by MeTaL125 on 2017-11-30.
 */

public class EnhancedSupportMapFragment extends SupportMapFragment {
    private View mapView;

    public EnhancedSupportMapFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mapView = super.onCreateView(layoutInflater, viewGroup, bundle);
        return this.mapView;
    }

    public void setEnabled(boolean enabled) {
        if (this.mapView != null) {
            mapView.setEnabled(enabled);
        }
    }
}
