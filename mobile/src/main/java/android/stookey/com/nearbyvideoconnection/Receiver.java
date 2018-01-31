package android.stookey.com.nearbyvideoconnection;

import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class Receiver extends AppCompatActivity {
    private static final String TAG = "Receiver";
    private static final String SERVICE_ID = "Rover";



    //Nearby
    private static int mSurfaceHeight, mSurfaceWidth;
    private ConnectionsClient connectionsClient;
    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String s, DiscoveredEndpointInfo discoveredEndpointInfo) {
            Log.i(TAG, "onEndpointFound(): endpoint found, requesting connection");
            connectionsClient.requestConnection(SERVICE_ID, s, connectionLifecycleCallback);
        }
        @Override
        public void onEndpointLost(String s) {

        }
    };
    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String s, ConnectionInfo connectionInfo) {
            Log.i(TAG, "onConnectionInitiated(): accepting connection");
            connectionsClient.acceptConnection(s, payloadCallback);
        }
        @Override
        public void onConnectionResult(String s, ConnectionResolution connectionResolution) {
            if(connectionResolution.getStatus().isSuccess()){
                Log.i(TAG, "onConnectionResult(): connection successful");
            } else {
                Log.i(TAG, "onConnectionResult(): connection failed");
            }
            switch(connectionResolution.getStatus().getStatusCode()){
                case ConnectionsStatusCodes.STATUS_OK:
                    updateStatus("Connected, able to transfer data");
                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    updateStatus("Connection was rejected by one or both sides");
                    break;
                case ConnectionsStatusCodes.STATUS_ERROR:
                    updateStatus("Connection broke before acceptance");
                    break;

            }

            //Devices are connected. Send Payload object containing the width, height of the Surface
            //to the Advertiser (camera) so the camera can be initialized to the proper size.
            //connectionsClient.sendPayload(SERVICE_ID, Payload.fromBytes())

        }
        @Override
        public void onDisconnected(String s) {
            Log.i(TAG, "onDisconnected(): connection disconnected");
        }
    };
    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String s, Payload payload) {
            Log.i(TAG, "onPayloadReceived(String s, Payload payload): payload received");
            //Do something with the payload
        }

        @Override
        public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {
            //update both the Advertiser and Discoverer on payload progress
        }
    };

    //Video Preview
    private TextureView mTextureView;
    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            updateStatus("onSurfaceTextureAvailable(): the surface texture is available");
            startDiscovery();
            mSurfaceHeight = height;
            mSurfaceWidth = width;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);

        //Assigning variables
        init();

    }



    private void init(){
        updateStatus("init(): starting initialization");
        connectionsClient = Nearby.getConnectionsClient(this);
        mTextureView = findViewById(R.id.tvVideo);

        if(mTextureView.isAvailable()){
            updateStatus("init(): mTextureView is already available");
        }else{
            updateStatus("init(): setting surfaceTextureListener");
            mTextureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    private void startDiscovery(){
        updateStatus("startDiscovery(): starting discovery...");
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, new DiscoveryOptions(Strategy.P2P_CLUSTER)).addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        updateStatus("discovering...");
                    }
                }
        ).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                updateStatus("unable to start discovering");
            }
        });
    }

    private void updateStatus(String string){
        Log.d(TAG, string);
    }

}
