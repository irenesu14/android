package com.example.mbl.rtsp;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.VideoView;

public class MainActivity extends AppCompatActivity {

    EditText rtspUrl;
    Button playButton, stopButton;
    VideoView videoView;
    //RssiMqtt rssimqtt;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //rssimqtt = new RssiMqtt(this.getApplicationContext());

        rtspUrl = (EditText) findViewById(R.id.editText);
        rtspUrl.setText("rtsp://10.42.0.1:8001/live.sdp");
        videoView = (VideoView)findViewById(R.id.rtspVideo);
        playButton = (Button)findViewById(R.id.playButton);
        stopButton = (Button)findViewById(R.id.stopButton);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                RtspStream(rtspUrl.getEditableText().toString());
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoView.stopPlayback();
            }
        });
        //rssimqtt.start();
    }

    private void RtspStream(String rtspUrl){
        videoView.setVideoURI(Uri.parse(rtspUrl));
        videoView.requestFocus();
        videoView.start();
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        //rssimqtt.onResume(this.getApplicationContext());
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        //rssimqtt.onPause(this.getApplicationContext());
    }

}
