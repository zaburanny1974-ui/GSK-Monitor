package com.gskmonitor;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.TextView;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int REQ = 10;
    private BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Set<String> seen = new HashSet<>();
    private TextView status, log;
    private boolean scanning;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b); setContentView(R.layout.activity_main);
        status=findViewById(R.id.status); log=findViewById(R.id.log); Button scan=findViewById(R.id.scan);
        adapter=((android.bluetooth.BluetoothManager)getSystemService(BLUETOOTH_SERVICE)).getAdapter();
        scan.setOnClickListener(v -> startScan());
        append("GSK Monitor 0.1.0\nBLE діагностичне ядро. Протокол GSIKE ще не вигаданий.");
    }
    private boolean perms() {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT},REQ); return false;
            }
        } else if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) { requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQ); return false; }
        return true;
    }
    private void startScan() {
        if (!perms()) return;
        if (adapter==null || !adapter.isEnabled()) { status.setText("Увімкни Bluetooth"); return; }
        if (scanning) return; seen.clear(); log.setText(""); scanning=true; status.setText("Сканування 10 с...");
        scanner=adapter.getBluetoothLeScanner(); scanner.startScan(callback);
        handler.postDelayed(this::stopScan,10000);
    }
    private void stopScan() { if (!scanning) return; scanning=false; try { if(scanner!=null) scanner.stopScan(callback); } catch(SecurityException ignored){} status.setText("Сканування завершено"); }
    private final ScanCallback callback=new ScanCallback(){ @Override public void onScanResult(int type, ScanResult r){ BluetoothDevice d=r.getDevice(); String addr=d.getAddress(); if(seen.add(addr)){ String name; try{name=d.getName();}catch(SecurityException e){name="?";} append("\n"+(name==null?"(без назви)":name)+"\n"+addr+"  RSSI "+r.getRssi()); } } };
    private void connect(BluetoothDevice d) { if(!perms()) return; status.setText("Підключення..."); gatt=d.connectGatt(this,false,gattCallback); }
    private final BluetoothGattCallback gattCallback=new BluetoothGattCallback(){
        @Override public void onConnectionStateChange(BluetoothGatt g,int s,int n){ runOnUiThread(()->{ if(n==BluetoothGatt.STATE_CONNECTED){status.setText("Підключено — пошук сервісів..."); if(perms()) g.discoverServices();} else status.setText("З'єднання закрито"); }); }
        @Override public void onServicesDiscovered(BluetoothGatt g,int s){ if(s!=BluetoothGatt.GATT_SUCCESS)return; runOnUiThread(()->{append("\n=== GATT SERVICES ==="); for(BluetoothGattService svc:g.getServices()){ append("Service: "+svc.getUuid()); for(BluetoothGattCharacteristic c:svc.getCharacteristics()){append("  Char: "+c.getUuid()+"  props="+c.getProperties());}} status.setText("GATT готовий");}); }
    };
    private void append(String s){ log.append(s+"\n"); }
    @Override protected void onDestroy(){ super.onDestroy(); stopScan(); if(gatt!=null && perms()) gatt.close(); }
}
