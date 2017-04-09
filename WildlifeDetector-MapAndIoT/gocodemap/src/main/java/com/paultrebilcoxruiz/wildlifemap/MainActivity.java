package com.paultrebilcoxruiz.wildlifemap;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.kml.KmlLayer;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener,
        KmlLayer.OnFeatureClickListener,
        MapBottomSheetDialogFragment.MapLayerSelectionListener {

    private GoogleMap mMap;
    private KmlLayer mKmlLayer;
    private ImageButton mConfirmButton;
    private EditText mEditText;

    private BottomSheetDialogFragment bottomSheetDialogFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomSheetDialogFragment = new MapBottomSheetDialogFragment(this);

        mEditText = (EditText) findViewById(R.id.edit_text);
        mConfirmButton = (ImageButton) findViewById(R.id.btn_confirm);

        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( mEditText.getText() == null ) {
                    return;
                }

                LatLng latlng = getLocationFromAddress(mEditText.getText().toString());

                if( latlng == null ) {
                    return;
                }

                placePin(latlng);
            }
        });

    }

    private void placePin(@NonNull LatLng latlng) {
        MarkerOptions options = new MarkerOptions();
        options.title(mEditText.getText().toString());
        options.position(latlng);

        mMap.addMarker(options);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
    }

    public void onMapReady(GoogleMap map) {
        if (mMap != null) {
            return;
        }

        mMap = map;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);

        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);

        retrieveFileFromResource(R.raw.canadiangeesewinter);
    }

    private void retrieveFileFromResource(int raw) {
        try {
            mKmlLayer = new KmlLayer(mMap, raw, getApplicationContext());
            mKmlLayer.addLayerToMap();
            mKmlLayer.setOnFeatureClickListener(this);
            initCamera();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    private void initCamera() {

        CameraPosition position = CameraPosition.builder()
                .target( new LatLng(39.5501, -105.7821) )
                .zoom( 6 )
                .build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), null);

    }

    public LatLng getLocationFromAddress(String strAddress){

        Geocoder coder = new Geocoder(this);
        List<Address> address;
        LatLng p1 = null;

        try {
            address = coder.getFromLocationName(strAddress,5);
            if (address == null || address.isEmpty() ) {
                return null;
            }

            Address location=address.get(0);

            p1 = new LatLng(location.getLatitude(), location.getLongitude());

        } catch( IOException e ) {

        }

        return p1;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_selection:
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_menu, menu);
        return true;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Toast.makeText(this, "No bears around there", Toast.LENGTH_SHORT ).show();
    }

    @Override
    public void onFeatureClick(Feature feature) {
        Toast.makeText(this, "You're in bear territory!", Toast.LENGTH_SHORT ).show();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        placePin(latLng);
    }

    @Override
    public void onMapLayerSelected(String layer) {
        mKmlLayer.removeLayerFromMap();
        if( stringMatches(layer, R.string.wildlife_bear_human_conflict ) ) {
            retrieveFileFromResource(R.raw.bear_human_conflict);
        } else if( stringMatches(layer, R.string.wildlife_mountain_lion_human_conflict ) ) {
            retrieveFileFromResource(R.raw.mountain_lion_human_conflict);
        } else if( stringMatches(layer, R.string.wildlife_black_bear_summer )) {
            retrieveFileFromResource(R.raw.bearssummer);
        } else if( stringMatches(layer, R.string.wildlife_black_bear_fall )) {
            retrieveFileFromResource(R.raw.bearsfall);
        } else if( stringMatches(layer, R.string.wildlife_black_bear_all )) {
            retrieveFileFromResource(R.raw.bear_overall);
        } else if( stringMatches(layer, R.string.wildlife_canadian_goose_winter )) {
            retrieveFileFromResource(R.raw.canadiangeesewinter);
        } else if( stringMatches(layer, R.string.wilfelife_elk_summer )) {
            retrieveFileFromResource(R.raw.elk_summer);
        } else if( stringMatches(layer, R.string.wilfelife_elk_winter )) {
            retrieveFileFromResource(R.raw.elk_winter);
        } else if( stringMatches(layer, R.string.wilfelife_elk_severe_winter )) {
            retrieveFileFromResource(R.raw.elk_severe_winter);
        } else if( stringMatches(layer, R.string.wildlife_mountain_goat_summer )) {
            retrieveFileFromResource(R.raw.mountain_goat_summer);
        } else if( stringMatches(layer, R.string.wildlife_mountain_goat_winter )) {
            retrieveFileFromResource(R.raw.mountain_goat_winter);
        } else if( stringMatches(layer, R.string.wildlife_mountain_lion_all )) {
            retrieveFileFromResource(R.raw.mountain_lion_all);
        } else if( stringMatches(layer, R.string.wildlife_moose_all )) {
            retrieveFileFromResource(R.raw.moose_all);
        }
    }

    public boolean stringMatches(String string, int stringRes) {
        return string.equalsIgnoreCase(getString(stringRes));
    }
}