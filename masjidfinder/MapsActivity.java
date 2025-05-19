package com.example.masjidfinder;

import androidx.fragment.app.FragmentActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location currentLocation;
    private static final int LOCATION_REQUEST_CODE = 101;
    private ListView placesList;
    private ArrayAdapter<String> adapter;
    private List<String> placeNames;
    private List<LatLng> placeLocations;
    private PlacesClient placesClient;
    private ImageButton buttonCenterLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        placesList = findViewById(R.id.places_list);

        // Initialize Places API
        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();
        buttonCenterLocation = findViewById(R.id.button_center_location);
        placeNames = new ArrayList<>();
        placeLocations = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, placeNames);
        placesList.setAdapter(adapter);

        // Set up the AutocompleteSupportFragment
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteFragment.setHint("Search for mosques");

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // Handle the selected place
                LatLng selectedLocation = place.getLatLng();
                if (selectedLocation != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
                    mMap.addMarker(new MarkerOptions().position(selectedLocation).title(place.getName()));
                    placeNames.add(place.getName());
                    placeLocations.add(selectedLocation);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(Status status) {
                // Handle the error
                Toast.makeText(MapsActivity.this, "Error: " + status, Toast.LENGTH_SHORT).show();
            }

        });

        placesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LatLng selectedLocation = placeLocations.get(position);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
                getDirections(selectedLocation);
            }
        });

        buttonCenterLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonCenterLocation();
            }
        });

        ImageButton backmaps = findViewById(R.id.back_map);

        backmaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;

                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
                    mapFragment.getMapAsync(MapsActivity.this);
                }
            }
        });
    }

    private void buttonCenterLocation() {
        if (currentLocation != null) {
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
        } else {
            Toast.makeText(MapsActivity.this, "Current location not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (currentLocation != null) {
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12));
            mMap.addMarker(new MarkerOptions().position(currentLatLng).title("You are here"));
            searchNearbyMosques(currentLatLng);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                Toast.makeText(this, "Permission denied, please allow the permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void searchNearbyMosques(LatLng location) {
        String type = "mosque";
        String apiKey = getString(R.string.google_maps_key);
        String locationStr = location.latitude + "," + location.longitude;
        String radius = "7000"; // 7 km radius

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + locationStr + "&radius=" + radius + "&type=" + type + "&key=" + apiKey;

        new Thread(() -> {
            try {
                java.net.URL urlObj = new java.net.URL(url);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) urlObj.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                java.io.InputStream inputStream = connection.getInputStream();
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }

                String response = stringBuilder.toString();
                parsePlacesResponse(response);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void parsePlacesResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray results = jsonObject.getJSONArray("results");

            placeNames.clear();
            placeLocations.clear();

            // Limit the number of places to 5
            int limit = Math.min(results.length(), 5);

            for (int i = 0; i < limit; i++) {
                JSONObject place = results.getJSONObject(i);
                String placeName = place.getString("name");
                JSONObject geometry = place.getJSONObject("geometry");
                JSONObject location = geometry.getJSONObject("location");
                double lat = location.getDouble("lat");
                double lng = location.getDouble("lng");
                LatLng latLng = new LatLng(lat, lng);

                placeNames.add(placeName);
                placeLocations.add(latLng);
                runOnUiThread(() -> {
                    mMap.addMarker(new MarkerOptions().position(latLng).title(placeName));
                    adapter.notifyDataSetChanged();
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getDirections(LatLng destination) {
        if (currentLocation != null) {
            LatLng origin = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            String url = "https://www.google.com/maps/dir/?api=1&origin=" + origin.latitude + "," + origin.longitude + "&destination=" + destination.latitude + "," + destination.longitude + "&travelmode=driving";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "Google Maps not installed", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show();
        }
    }
}
