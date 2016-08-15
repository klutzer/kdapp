package br.net.twome.cade;

import android.*;
import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

import br.net.twome.cade.service.WebService;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        createServices();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    private void createServices() {
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        if (mLocationRequest == null) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(10000);
            mLocationRequest.setFastestInterval(5000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        super.onPause();
    }

    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 10);
        }else{
            Log.d("MAPS", "LocationUpdates => Tudo ok");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10 && grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("MAPS", "Permissão concedida");
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private boolean first = true;

    @Override
    public void onLocationChanged(final Location location) {
        Log.d("MAPS", "Location changed!! "+location.getLatitude()+"  "+location.getLongitude());
        if (mMap != null) {
            new AsyncTask<Void, Void, List<Usuario>>() {

                @Override
                protected List<Usuario> doInBackground(Void... voids) {
                    SharedPreferences p = getSharedPreferences(LoginActivity.PREFS, 0);
                    Usuario usuario = new Usuario(p.getString(LoginActivity.NICK, ""),
                            p.getString(LoginActivity.PASS, ""));
                    usuario.setLatitude(location.getLatitude());
                    usuario.setLongitude(location.getLongitude());
                    try {
                        return WebService.enviaLocalizacao(usuario);
                    }catch (Exception e) {
                        Log.e("MAPS", e.getMessage(), e);
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(List<Usuario> usuarios) {
                    if (usuarios == null) {
                        Toast.makeText(MapsActivity.this, "Error", Toast.LENGTH_LONG).show();
                        return;
                    }
                    mMap.clear();
                    for (int i=0; i<usuarios.size(); i++) {
                        Usuario usuario = usuarios.get(i);
                        Log.d("MAPS", usuario.getNickname());
                        LatLng position = new LatLng(usuario.getLatitude(), usuario.getLongitude());
                        MarkerOptions marker = new MarkerOptions()
                                .position(position).title(usuario.getNickname());
                        if (i == 0) {
                            if (first) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 19));
                                first = false;
                            }
                            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_me));
                        }else {
                            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_others));
                        }
                        mMap.addMarker(marker);
                    }
                }
            }.execute();
        }
    }

}
