package com.example.cruzr;

import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.ubtechinc.cruzr.sdk.ros.RosRobotApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private boolean ledState = false;
    private TextView textView;
    private PreviewView previewView;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textView = findViewById(R.id.dummyText);
        previewView = findViewById(R.id.viewFinder);
        initMediaPlayer();

        findViewById(R.id.showRosVersion).setOnClickListener(v -> textView.setText(RosRobotApi.get().getRosVersion()));

        findViewById(R.id.showRosIP).setOnClickListener(v -> textView.setText(RosRobotApi.get().getRosWifiIp()));

        findViewById(R.id.moveForward).setOnClickListener(v -> {
            int ret = RosRobotApi.get().moveToward(0.1f, 0, 0);
            Log.i("MOVE", Integer.toString(ret));
        });

        findViewById(R.id.stopMove).setOnClickListener(v -> RosRobotApi.get().stopMove());

        findViewById(R.id.toggleLed).setOnClickListener(v -> {
            ledState = !ledState;
            RosRobotApi.get().ledSetOnOff(ledState);
        });

        findViewById(R.id.showCamera).setOnClickListener(v -> {
            boolean hasPermissions = allPermissionsGranted();
            textView.setText(String.valueOf(hasPermissions));
            startCamera();
        });

        findViewById(R.id.playAudio).setOnClickListener(v -> playAudio());
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseMediaPlayer();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        initMediaPlayer();
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this.getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, preview);
            } catch (CancellationException | ExecutionException | InterruptedException e) {
                Log.e("CAMERA", "Camera provider future failed " + e);
            } catch (IllegalStateException | IllegalArgumentException e) {
                Log.e("CAMERA", "Binding failed " + e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void playAudio() {
        if (mediaPlayer == null) initMediaPlayer();
        String audioUrl = "https://www2.cs.uic.edu/~i101/SoundFiles/CantinaBand3.wav";
        try {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.prepareAsync();
        } catch (IllegalArgumentException | IOException e) {
            Log.e("AUDIO", "Could not open audio source " + e);
        }
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public static final String[] REQUIRED_PERMISSIONS;

    static {
        ArrayList<String> permissions = new ArrayList<>(Arrays.asList(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO
        ));
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        REQUIRED_PERMISSIONS = permissions.toArray(new String[0]);
    }
}