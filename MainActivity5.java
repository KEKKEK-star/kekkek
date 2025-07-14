package com.example.study.utile;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.study.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity5 extends AppCompatActivity {
    private static final String API_KEY = "API在实验报告图片里，wrk的安卓";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private EditText cityEditText;
    private Button getWeatherButton;
    private Button getLocationButton;
    private TextView weatherInfoTextView;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main5);

        
        cityEditText = findViewById(R.id.cityEditText);
        getWeatherButton = findViewById(R.id.getWeatherButton);
        getLocationButton = findViewById(R.id.getLocationButton);
        weatherInfoTextView = findViewById(R.id.weatherInfoTextView);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        getWeatherButton.setOnClickListener(v -> {
            String city = cityEditText.getText().toString().trim();
            if (!city.isEmpty()) {
                getWeatherByCity(city);
            } else {
                Toast.makeText(MainActivity5.this, "请输入城市名称", Toast.LENGTH_SHORT).show();
            }
        });

        getLocationButton.setOnClickListener(v -> checkLocationPermission());

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    getWeatherByLocation(location.getLatitude(), location.getLongitude());
                }
            }
        };
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "位置权限被拒绝，无法获取当前位置天气", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        new Handler().postDelayed(() -> fusedLocationClient.removeLocationUpdates(locationCallback), 10000);
    }

    private void getWeatherByCity(String city) {
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city +
                "&appid=" + API_KEY + "&units=metric";
        fetchWeatherData(url);
    }

    private void getWeatherByLocation(double lat, double lon) {
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat +
                "&lon=" + lon + "&appid=" + API_KEY + "&units=metric";
        fetchWeatherData(url);
    }

    private void fetchWeatherData(String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity5.this, "获取天气数据失败", Toast.LENGTH_SHORT).show();
                    Log.e("WeatherApp", "Error: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String responseData = response.body().string();
                    runOnUiThread(() -> {
                        try {
                            parseWeatherData(responseData);
                        } catch (JSONException e) {
                            Toast.makeText(MainActivity5.this, "解析天气数据失败", Toast.LENGTH_SHORT).show();
                            Log.e("WeatherApp", "JSON Error: " + e.getMessage());
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity5.this, "服务器响应错误", Toast.LENGTH_SHORT).show();
                        Log.e("WeatherApp", "Response code: " + response.code());
                    });
                }
            }
        });
    }

    private void parseWeatherData(String jsonData) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonData);
        String cityName = jsonObject.getString("name");
        JSONObject main = jsonObject.getJSONObject("main");
        double temperature = main.getDouble("temp");
        int humidity = main.getInt("humidity");
        JSONObject weather = jsonObject.getJSONArray("weather").getJSONObject(0);
        String description = weather.getString("description");
        double windSpeed = jsonObject.getJSONObject("wind").getDouble("speed");

        String weatherInfo = "城市: " + cityName + "\n" +
                "温度: " + temperature + "°C\n" +
                "天气: " + description + "\n" +
                "湿度: " + humidity + "%\n" +
                "风速: " + windSpeed + " m/s";
        weatherInfoTextView.setText(weatherInfo);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}