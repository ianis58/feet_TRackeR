package ca.uqac.mobile.feet_tracker.android.activities.router;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by MeTaL125 on 2017-11-27.
 */

public class EnhancedSupportPlaceAutocompleteFragment extends SupportPlaceAutocompleteFragment {
    public interface OnClearListener {
        void onClear();
    }

    private View clearButton;
    private EditText searchInput;

    private OnClearListener onClearListener;

    public EnhancedSupportPlaceAutocompleteFragment() {
        super();
    }

    public void setOnClearListener(OnClearListener listener) {
        this.onClearListener = listener;
    }

    public void setLatLng(LatLng latLng) {
        searchInput.setText(String.format("%.6f, %.6f", latLng.latitude, latLng.longitude));
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        final View view = super.onCreateView(layoutInflater, viewGroup, bundle);

        clearButton = view.findViewById(com.google.android.gms.R.id.place_autocomplete_clear_button);
        searchInput = (EditText) view.findViewById(com.google.android.gms.R.id.place_autocomplete_search_input);

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchInput.setText("");
                if (onClearListener != null) {
                    onClearListener.onClear();
                }
            }
        });

        return view;
    }
}
