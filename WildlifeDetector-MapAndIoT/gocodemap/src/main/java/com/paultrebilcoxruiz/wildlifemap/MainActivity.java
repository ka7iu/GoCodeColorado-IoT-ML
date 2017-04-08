package com.paultrebilcoxruiz.wildlifemap;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
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

public class MainActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener,
        KmlLayer.OnFeatureClickListener {
    private GoogleMap mMap;
    private KmlLayer mKmlLayer;
    private ImageButton mConfirmButton;
    private EditText mEditText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        retrieveFileFromResource();
    }

    private void retrieveFileFromResource() {
        try {
            mKmlLayer = new KmlLayer(mMap, R.raw.canadiangeesewinter, getApplicationContext());

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
            if (address == null) {
                return null;
            }
            Address location=address.get(0);

            p1 = new LatLng(location.getLatitude(), location.getLongitude());

        } catch( IOException e ) {

        }

        return p1;
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
}