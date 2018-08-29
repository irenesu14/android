package com.example.mbl.rtsp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.HashMap;
import java.util.List;

/**
 * Created by mbl on 2018/5/31.
 */

public class RssiMqtt extends Thread {
    static String mqtt_host[] = {"tcp://192.168.1.13:1883", "tcp://192.168.1.89:1883"};
    static String username = "mbl";
    static String password = "mbl12345";
    static String pub_topic = "d1/rssi";
    static String sub_topic = "d1/handover";
    static String clientId="abc";
    MqttAndroidClient client;
    MqttConnectOptions options;
    MqttClient client2;
    MqttConnectOptions options2;

    MqttDeliveryToken token;
    MqttTopic mqttTopic;

    //wifi
    WifiManager wifi;
    WifiScanReceiver wifiReceiver;
    WifiInfo wifiInfo;
    Handler handler;
    WifiConfiguration conf;
    String ssid;
    long startTime;
    String scanreceived;

    private Object mPauseLock;
    private boolean mPaused;
//    @Override
    public RssiMqtt(Context context) {
        scanreceived = "";
        wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiInfo = wifi.getConnectionInfo();
        ssid  = wifiInfo.getSSID();
        mPauseLock = new Object();
        mPaused = false;
        if(ssid.equals("\"n1\""))
        {
            Log.d("MQTT_connect", "n1");
            client =new MqttAndroidClient(context, mqtt_host[0],clientId);
            try {
                client2 = new MqttClient(mqtt_host[1], "abcd", new MemoryPersistence());
            } catch (MqttException e) {
                Log.d("client2", "fail!!QQ");
                e.printStackTrace();
            }
        }
        else
        {
            Log.d("MQTT_connect", "n2");
            client =new MqttAndroidClient(context, mqtt_host[1],clientId);
            try {
                client2 = new MqttClient(mqtt_host[0], "abcd", new MemoryPersistence());
            } catch (MqttException e) {
                Log.d("client2", "fail!!QQ");
                e.printStackTrace();
            }
        }
        options = new MqttConnectOptions();  //set into option
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setCleanSession(true);
        options2 = new MqttConnectOptions();  //set into option
        options2.setUserName(username);
        options2.setPassword(password.toCharArray());
        options2.setCleanSession(true);
        options2.setAutomaticReconnect(true);

        handler = new Handler();
        wifiReceiver = new WifiScanReceiver();


        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.d("lost connection" , "lost connection");
                if(handler!=null){
                    handler.removeCallbacks(runnable);
                }
                client.close();
            }

            @Override
            //recieve msg
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                if(topic.equals("d1/handover"))
                {
                    startTime = System.currentTimeMillis();
                    conf = new WifiConfiguration();
                    if(ssid.equals("\"n1\""))
                    {
                        conf.SSID = "\"" + "n2" + "\"";
                    }
                    else
                    {
                        conf.SSID = "\"" + "n1" + "\"";
                    }
                    conf.wepKeys[0] = "\"" + "1234567890abc" + "\"";
                    conf.wepTxKeyIndex = 0;
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    wifi.addNetwork(conf);
                    List<WifiConfiguration> list = wifi.getConfiguredNetworks();
                    for( WifiConfiguration i : list ) {
                        if(i.SSID != null && ((ssid.equals("\"n1\"") && i.SSID.equals("\"n2\"")) || (ssid.equals("\"n2\"") && i.SSID.equals("\"n1\""))) ) {
                            mqttTopic = client2.getTopic("d1/restart");
                            try {
                                // Publish to the broker
                                token = mqttTopic.publish(new MqttMessage("restart".getBytes()));
                            } catch (MqttException e) {
                                e.printStackTrace();
                            }
                            wifi.disconnect();
                            wifi.enableNetwork(i.networkId, true);
                            Log.d("OMG", "did reconnect");
                            wifi.reconnect();
                            break;
                        }
                    }
                    Log.d("Using Time:" , (System.currentTimeMillis() - startTime) + " ms");
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

//        conn_btn.setOnClickListener(new View.OnClickListener(){ //connect
//            @Override
//            public void onClick(View v) {
//                Log.d("MQTT", "conn_btn");
//                conn();  //connect to MQTT broker
//                conn2();
//            }
//        });
//
//        disconn_btn.setOnClickListener(new View.OnClickListener(){ //disconnect
//            @Override
//            public void onClick(View v) {
//                Log.d("MQTT", "disconn_btn");
//                disconn();  //Disconnect
//            }
//        });
//
//        start.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v) {
//                Log.d("START", "container start");
//                pub("d1/start", "start");
//            }
//        });

    }

    public void conn()
    {

        try {
            //IMqttToken token = client.connect();

            IMqttToken token = client.connect(options); //try connection
//            try {
//                token = client.connect(options);
//            } catch (MqttException e) {
//                Log.d("client2", "fail!!QQ");
//                e.printStackTrace();
//            }
            if( client.isConnected() ){
                Log.d("conn()", "inside");
            }

            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("MQTT", "Conn: onSuccess");
                    sub();  //subscribe
                    handler.postDelayed(runnable, 0);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d("MQTT", "Conn: onFailure");
                    if(handler!=null){
                        handler.removeCallbacks(runnable);
                    }
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
            Log.d("conn()", "fail");
        }
    }

    public void conn2()
    {
        try {
            client2.connect(options2);
        } catch (Exception e1) {
            Log.d("Connection error", "conn2 error");
        }
    }



    public void disconn(final Context context)
    {
        try {
            //IMqttToken token = client.connect();
            IMqttToken token = client.disconnect(); //try connection
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d("MQTT", "Disconn: onSuccess");
                    Toast.makeText(context,"Disconnect Success!",Toast.LENGTH_LONG).show();
                    if(wifiReceiver != null){
                        context.unregisterReceiver(wifiReceiver);
                        wifiReceiver = null;
                    }
                    if(handler!=null){
                        handler.removeCallbacks(runnable);
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d("MQTT", "Disconn: onFailure");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void pub(String msg, String topic)
    {
        try {
            if(msg.getBytes()!=null && client.isConnected())
            {
                Log.d("MQTT", "pub");
                client.publish(topic, msg.getBytes(),0,false);
            }
//            client.publish(topic, msg.getBytes(),0,false);  //???
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void sub()
    {
        Log.d("MQTT", "sub");
        try {
            client.subscribe(sub_topic,0);  //qos=0
            Log.d("MQTT", "sub");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    final Runnable runnable = new Runnable(){
        @Override
        public void run() {
            try {
                /*JSONObject json_obj = new JSONObject();
                List<ScanResult> scanResults=wifi.getScanResults();//搜索到的设备列表
                for (ScanResult scanResult : scanResults) {
                    if(scanResult.SSID != null && scanResult.SSID.charAt(0) == 'n'){
                        json_obj.put(scanResult.SSID, scanResult.level);
                    }
                }
                pub(json_obj.toString(), pub_topic);*/
                //Log.d("runnable", "inside");
//                if( wifi.startScan() ){
//                    Log.d("scan", "succeed");
//                }
//                else{
//                    Log.d("scan", "failed");
//                }
                wifi.startScan();
                handler.postDelayed(this, 0);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    /*
        public static String getAroundWifiDeciceInfo(Context mContext){
            StringBuffer sInfo = new StringBuffer();
            WifiManager mWifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
            //WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
            List<ScanResult> scanResults=mWifiManager.getScanResults();//搜索到的设备列表
            for (ScanResult scanResult : scanResults) {
                sInfo.append("\ndevice name："+scanResult.SSID
                        +" signal strength："+scanResult.level+"/n :"
                        +mWifiManager.calculateSignalLevel(scanResult.level,4));
            }
            return sInfo.toString();
        }
    */
    private class WifiScanReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            /*
            List<ScanResult> wifiScanList = wifi.getScanResults();
            txtWifiInfo.setText("");
            for(int i = 0; i < wifiScanList.size(); i++){
                String info = ((wifiScanList.get(i)).toString());
                txtWifiInfo.append(info+"\n\n");
            }
            */
//            Log.d("wifireceiver", "inside");
            List<ScanResult> scanResults = wifi.getScanResults();
            StringBuilder sb = new StringBuilder();
            sb.append(ssid + "\n");
            for (ScanResult scanResult : scanResults) {
                if(scanResult.SSID != null && (scanResult.SSID.equals("n1") || scanResult.SSID.equals("n2")) ){
                    sb.append(scanResult.SSID );
                    sb.append("\n");
                    sb.append(scanResult.level);
                    sb.append("\n");
                }
            }
            scanreceived = sb.toString();
            pub(scanreceived, pub_topic);
        }
    }
    public void run() {
        Log.d("Thread_run", "run");

        conn();
        conn2();
        synchronized (mPauseLock) {
            while (mPaused) {
                try {
                    mPauseLock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void onPause(Context context) {
        if(wifiReceiver != null){
            context.unregisterReceiver(wifiReceiver);
            wifiReceiver = null;
        }
        synchronized (mPauseLock) {
            mPaused = true;
        }
    }


    public void onResume(Context context) {
        context.registerReceiver(
                wifiReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        );
        synchronized (mPauseLock) {
            mPaused = false;
            mPauseLock.notifyAll();
        }
    }
}
