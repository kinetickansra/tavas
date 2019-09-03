package com.celestini.distressdetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.common.modeldownload.FirebaseRemoteModel;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;
import com.google.firebase.ml.common.FirebaseMLException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.core.app.ActivityCompat;

import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int ERROR_DIALOG_REQUEST = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(isServicesOK()){
            init();
        }
        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS,Manifest.permission.ACCESS_COARSE_LOCATION},1234);
        }
        FirebaseModelDownloadConditions.Builder conditionsBuilder =
                new FirebaseModelDownloadConditions.Builder().requireWifi();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Enable advanced conditions on Android Nougat and newer.
            conditionsBuilder = conditionsBuilder
                    .requireCharging()
                    .requireDeviceIdle();
        }
        FirebaseModelDownloadConditions conditions = conditionsBuilder.build();

// Build a remote model source object by specifying the name you assigned the model
// when you uploaded it in the Firebase console.
        FirebaseRemoteModel cloudSource = new FirebaseRemoteModel.Builder("distress")
                .enableModelUpdates(true)
                .setInitialDownloadConditions(conditions)
                .setUpdatesDownloadConditions(conditions)
                .build();
        FirebaseModelManager.getInstance().registerRemoteModel(cloudSource);

        FirebaseLocalModel localSource =
                new FirebaseLocalModel.Builder("distress")  // Assign a name to this model
                        .setAssetFilePath("distress.tflite")
                        .build();
        FirebaseModelManager.getInstance().registerLocalModel(localSource);

        FirebaseModelOptions options = new FirebaseModelOptions.Builder()
                .setRemoteModelName("distress")
                .setLocalModelName("distress")
                .build();
        try {
            FirebaseModelInterpreter firebaseInterpreter =
                    FirebaseModelInterpreter.getInstance(options);
            FirebaseModelInputOutputOptions inputOutputOptions =
                    new FirebaseModelInputOutputOptions.Builder()
                            .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 40})
                            .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 3})
                            .build();
            //float[][] input = {{-355.8044573138089f,133.67714257728628f,2.6359922097336552f,14.227297647771413f,1.2618401939717734f,6.0131387606205235f,19.01456078059234f,8.40240297028787f,3.3119543500219413f,13.8405165580398f,8.227373598935632f,7.068606086499635f,-2.426122421115979f,-1.9679724172915187f,5.696924712756267f,5.994543346431556f,1.7964566835687776f,5.080294983009213f,-0.8287599101487046f,-1.458000320844907f,2.2408959693543355f,-0.029741714672487988f,-3.226075925319936f,1.5459167769119817f,-0.09980602336619124f,-0.4250155380220617f,0.9386166468612873f,3.0049660708435355f,3.367233271503158f,2.2524869930862153f,-0.7628666422849145f,1.5139255565242242f,0.7496743831699035f,-1.1959439785916326f,-1.888201306338271f,-0.8958427937871515f,-1.39654808053547f,0.8166615436324204f,-0.5790579094659607f,-0.9137407685252883f},{0}};
            float[][] input = {{-355.8044573138089f,133.67714257728628f,2.6359922097336552f,14.227297647771413f,1.2618401939717734f,6.0131387606205235f,19.01456078059234f,8.40240297028787f,3.3119543500219413f,13.8405165580398f,8.227373598935632f,7.068606086499635f,-2.426122421115979f,-1.9679724172915187f,5.696924712756267f,5.994543346431556f,1.7964566835687776f,5.080294983009213f,-0.8287599101487046f,-1.458000320844907f,2.2408959693543355f,-0.029741714672487988f,-3.226075925319936f,1.5459167769119817f,-0.09980602336619124f,-0.4250155380220617f,0.9386166468612873f,3.0049660708435355f,3.367233271503158f,2.2524869930862153f,-0.7628666422849145f,1.5139255565242242f,0.7496743831699035f,-1.1959439785916326f,-1.888201306338271f,-0.8958427937871515f,-1.39654808053547f,0.8166615436324204f,-0.5790579094659607f,-0.9137407685252883f}};
            final String[] outputMap = {"Scream", "Sobbing and Crying", "Not Distress"};
            FirebaseModelInputs inputs = new FirebaseModelInputs.Builder()
                    .add(input)  // add() as many input arrays as your model requires
                    .build();
            firebaseInterpreter.run(inputs, inputOutputOptions)
                    .addOnSuccessListener(
                            new OnSuccessListener<FirebaseModelOutputs>() {
                                @Override
                                public void onSuccess(FirebaseModelOutputs result) {
                                    float[][] output = result.getOutput(0);
                                    float[] probabilities = output[0];
                                    int maxIndex = 0;
                                    for (int i = 1; i< probabilities.length; i++) {
                                        if (probabilities[i] > probabilities[maxIndex]) {
                                            maxIndex = i;
                                        }
                                    }
                                    TextView outputView = findViewById(R.id.output);
                                    outputView.setText("Distress: Message sent after 15 seconds ");
                                    if(maxIndex == 2){
                                        try {
                                            Log.e("my app","sms sent");
                                            sendMessage();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                    }
                                    Log.e("my app", outputMap[maxIndex]);

                                    // ...
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Task failed with an exception
                                    // ...
                                    Log.e("my app", e.getMessage());
                                }
                            });


        } catch (FirebaseMLException e) {
            e.printStackTrace();
        }




    }

    @Override
    protected void onStart() {
        super.onStart();

    }
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private List<Address> getLocation() throws IOException {
        Geocoder geocoder;
        List<Address> addresses=new ArrayList<>();
        geocoder = new Geocoder(this, Locale.getDefault());
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        final Task location = mFusedLocationProviderClient.getLastLocation();
        Location currentLocation = null;
        location.addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if(task.isSuccessful()){
                    Log.d(TAG, "onComplete: found location!");
                    Location currentLocation = (Location) task.getResult();


                }else{
                    Log.d(TAG, "onComplete: current location is null");
                    Toast.makeText(MainActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                }
            }
        });

            if(currentLocation!=null)
            addresses = geocoder.getFromLocation(currentLocation.getLatitude(), currentLocation.getLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5

//        String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
//        String city = addresses.get(0).getLocality();
//        String state = addresses.get(0).getAdminArea();
//        String country = addresses.get(0).getCountryName();
//        String postalCode = addresses.get(0).getPostalCode();
//        String knownName = addresses.get(0).getFeatureName();
        return addresses;
    }
    private void sendMessage() throws IOException {
        SmsManager smsManager = SmsManager.getDefault();

//        List<Address> addresses=getLocation();
//        String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
//        String city = addresses.get(0).getLocality();
//        String state = addresses.get(0).getAdminArea();
//        String country = addresses.get(0).getCountryName();
//        String postalCode = addresses.get(0).getPostalCode();
//        String knownName = addresses.get(0).getFeatureName();

//        String text="Please help me!! I am in danger my location is " + address+" "+city+" "+state+" "+ country+" "+postalCode+" "+knownName ;
        smsManager.sendTextMessage("+918585965780",null,"PLease help me I am in danger",null,null);
    }

    private void init(){
        Button btnMap = (Button) findViewById(R.id.btnMap);
        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                startActivity(intent);
            }
        });
    }
    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if(available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

}
