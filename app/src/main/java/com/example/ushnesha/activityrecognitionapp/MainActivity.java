package com.example.ushnesha.activityrecognitionapp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.google.android.gms.location.ActivityRecognition.ActivityRecognitionApi;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    public static final String TAG=MainActivity.class.getSimpleName();
    protected GoogleApiClient googleApiClient;
    protected ActivityDetectionBroadcastReceiver mBroadcastReceiver;

    @Bind(R.id.detectedActivities)
    TextView detectedActivities;
    @Bind(R.id.remove_activity_updates_button)
    Button RemoveActivityBtn;
    @Bind(R.id.request_activity_updates_button)
    Button RequestActivityBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mBroadcastReceiver=new ActivityDetectionBroadcastReceiver();
        buildGoogleApiClient();

    }

    public synchronized void buildGoogleApiClient(){
        googleApiClient=new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public void requestActivityUpdatesButtonHandler(View view) {
        if(!googleApiClient.isConnected()){
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(googleApiClient, Constants.DETECTION_INTERVAL_IN_MILLISECONDS, getActivityPendingIntent()).setResultCallback(this);
        RequestActivityBtn.setEnabled(false);
        RemoveActivityBtn.setEnabled(true);
    }

    public void removeActivityUpdatesButtonHandler(View view) {
        if(!googleApiClient.isConnected()){
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(googleApiClient, getActivityPendingIntent()).setResultCallback(this);
        RequestActivityBtn.setEnabled(true);
        RemoveActivityBtn.setEnabled(false);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e(TAG, "Connected to GoogleApiClient");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "Connection suspended");
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Connection failed: ConnectionResult.getErrorCode()= "+ connectionResult.getErrorCode());
    }



    @Override
    public void onResult(@NonNull Status status) {
        if(status.isSuccess()){
            Log.e(TAG,"Successfully added activity detection.");
        }
        else{
            Log.e(TAG,"Error adding or removing activity detection.");

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id= item.getItemId();

        if(id == R.id.settings){
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver{
        protected static final String TAG="receiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<DetectedActivity> updatedActivities = intent.getParcelableArrayListExtra(Constants.ACTIVITY_EXTRA);
            String strStatus="";
            for(DetectedActivity activity: updatedActivities){
                strStatus+= getActivityString(activity.getType()) + activity.getConfidence() + "%\n";
            }
            Log.e(TAG, strStatus);
            detectedActivities.setText(strStatus);
        }
    }

    public String getActivityString(int detectedActivityType){
        Resources resources=this.getResources();
        switch(detectedActivityType){
            case DetectedActivity
                    .IN_VEHICLE:
                return resources.getString(R.string.in_vehicle);
            case DetectedActivity
                    .ON_BICYCLE:
                return resources.getString(R.string.on_bicycle);
            case DetectedActivity
                    .ON_FOOT:
                return resources.getString(R.string.on_foot);
            case DetectedActivity
                    .RUNNING:
                return resources.getString(R.string.running);
            case DetectedActivity
                    .STILL:
                return resources.getString(R.string.still);
            case DetectedActivity
                    .TILTING:
                return resources.getString(R.string.tilting);
            case DetectedActivity
                    .UNKNOWN:
                return resources.getString(R.string.unknown);
            case DetectedActivity
                    .WALKING:
                return resources.getString(R.string.walking);
            default:
                return resources.getString(R.string.unidentifiable_activity, detectedActivityType);
        }
    }

    public PendingIntent getActivityPendingIntent(){
        Intent intent=new Intent(this, DetectedActivitiesIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }


    @Override
    protected void onStop() {
        super.onStop();
        if(googleApiClient != null)
        googleApiClient.disconnect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(Constants.BROADCAST_ACTION));
    }
}
