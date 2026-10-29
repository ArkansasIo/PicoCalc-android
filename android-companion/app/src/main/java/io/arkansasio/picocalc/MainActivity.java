package io.arkansasio.picocalc;

import android.app.Activity;
import android.bluetooth.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.app.AlertDialog;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private LinearLayout content;
    private final int ink=0xFFE6F1FF, accent=0xFF2B83C6;
    private TextView terminalLog, fileList, usbStatus, bluetoothStatus;
    private BluetoothSocket bluetoothSocket;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final int REQUEST_BT_CONNECT = 701;
    private static final java.util.UUID SPP_UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private EditText terminalInput, pythonInput;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF0B1420);
        getWindow().setNavigationBarColor(0xFF0B1420);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(0xFF0B1420);
        TextView title=text("PICOCALC  /  COMPANION",18); title.setTypeface(null,1); title.setPadding(16,18,16,12); root.addView(title);
        HorizontalScrollView scroll=new HorizontalScrollView(this); LinearLayout nav=new LinearLayout(this);
        for(String name:new String[]{"Home","Calculator","Terminal","Files","MicroPython","Emulator","USB","Bluetooth"}) { Button b=new Button(this); b.setText(name); b.setTextSize(11); b.setOnClickListener(v->openTab(name)); nav.addView(b); }
        scroll.addView(nav); root.addView(scroll);
        ScrollView body=new ScrollView(this); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(16,12,16,24); body.addView(content); root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); showHome();
    }
    private TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextColor(ink);t.setTextSize(size);t.setPadding(4,8,4,8);return t;}
    private EditText edit(String hint,boolean multi){EditText e=new EditText(this);e.setTextColor(ink);e.setHintTextColor(0xFF9AAFC3);e.setHint(hint);e.setSingleLine(!multi);e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));return e;}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setOnClickListener(v->action.run());return b;}
    private void heading(String s){content.addView(text(s,20));}
    private void openTab(String tab){content.removeAllViews();switch(tab){case "Calculator":showCalculator();break;case "Terminal":showTerminal();break;case "Files":showFiles();break;case "MicroPython":showPython();break;case "Emulator":showEmulator();break;case "USB":showUsb();break;case "Bluetooth":showBluetooth();break;default:showHome();}}
    private void showHome(){heading("Portable computing, connected.");content.addView(text("Android companion for PicoCalc hardware and development workflows.",16));for(String s:new String[]{"Scientific calculator","Serial terminal interface","Local file manager","MicroPython script editor","PicoCalc-style screen simulator","USB host device discovery","Bluetooth Classic SPP terminal"})content.addView(text("•  "+s,15));content.addView(text("USB serial transfers depend on the device interface and a compatible driver. Firmware flashing is not implemented.",13));}
    private void showCalculator(){heading("Calculator");EditText expression=edit("Expression: (12+3)*4^2",false);content.addView(expression);TextView result=text("Result appears here",18);content.addView(button("Calculate",()->{try{result.setText(String.format(Locale.US,"%.10g",ExpressionEvaluator.evaluate(expression.getText().toString())));}catch(Exception e){result.setText("Error: "+e.getMessage());}}));content.addView(result);content.addView(text("Operators: +  -  *  /  ^  ( )",13));}
    private void showTerminal(){heading("Serial / Bluetooth terminal");content.addView(text("Bluetooth Classic SPP text I/O is supported for paired compatible adapters. USB serial transport is not implemented yet.",13));terminalLog=text("PicoCalc Companion terminal ready.\n",14);terminalLog.setBackgroundColor(0xFF111D2B);content.addView(terminalLog);terminalInput=edit("Enter command or text",false);content.addView(terminalInput);content.addView(button("Send / log locally",()->{String line=terminalInput.getText().toString();terminalLog.append("> "+line+"\n");terminalInput.setText("");if(bluetoothSocket!=null&&bluetoothSocket.isConnected()){try{bluetoothSocket.getOutputStream().write((line+"\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(IOException e){terminalLog.append("Send failed: "+e.getMessage()+"\n");disconnectBluetooth();}}}));content.addView(button("Clear",()->terminalLog.setText("")));content.addView(button("Bluetooth devices",()->openTab("Bluetooth")));}
    private void showFiles(){heading("File manager");content.addView(text("App-private files (sandbox)",13));fileList=text("",14);content.addView(fileList);refreshFiles();content.addView(button("Refresh",this::refreshFiles));EditText filename=edit("Filename, e.g. hello.py",false);content.addView(filename);EditText data=edit("Text file contents",true);data.setMinLines(4);content.addView(data);content.addView(button("Save text file",()->{String n=filename.getText().toString().trim();if(n.isEmpty()||n.contains("/")||n.contains("\\")||n.equals(".")||n.equals("..")){toast("Use a simple filename");return;}try(FileOutputStream out=openFileOutput(n,MODE_PRIVATE)){out.write(data.getText().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));toast("Saved "+n);refreshFiles();}catch(IOException e){toast("Save failed: "+e.getMessage());}}));content.addView(button("Load filename",()->{try{String n=filename.getText().toString().trim();try(InputStream in=openFileInput(n);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int count;while((count=in.read(buf))!=-1)out.write(buf,0,count);data.setText(new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));}}catch(Exception e){toast("Load failed: "+e.getMessage());}}));}
    private void refreshFiles(){if(fileList==null)return;File[] files=getFilesDir().listFiles();StringBuilder b=new StringBuilder();if(files!=null)for(File f:files)b.append(f.getName()).append(f.isDirectory()?"/":"  ("+f.length()+" bytes)").append("\n");fileList.setText(b.length()==0?"No files yet":b.toString());}
    private void showPython(){heading("MicroPython workbench");content.addView(text("Write and save scripts locally. Transfer to a board requires a compatible serial transport.",13));pythonInput=edit("# PicoCalc MicroPython script\nprint('Hello PicoCalc')",true);pythonInput.setMinLines(8);content.addView(pythonInput);content.addView(button("Save as main.py",()->{try(FileOutputStream out=openFileOutput("main.py",MODE_PRIVATE)){out.write(pythonInput.getText().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));toast("Saved main.py");}catch(IOException e){toast(e.getMessage());}}));content.addView(button("Transfer notes",()->new AlertDialog.Builder(this).setTitle("Transfer notes").setMessage("Connect a compatible USB serial adapter, grant Android USB permission, set the target baud rate, and use a compatible serial transport. Automatic script upload is not implemented yet.").setPositiveButton("OK",null).show()));}
    private void showEmulator(){heading("PicoCalc simulator");TextView screen=text("┌────────────────────────┐\n│      PICOCALC           │\n│  > BASIC READY          │\n│                        │\n│  1 Calculator           │\n│  2 Terminal             │\n│  3 MicroPython          │\n└────────────────────────┘",17);screen.setTypeface(android.graphics.Typeface.MONOSPACE);screen.setBackgroundColor(0xFF050A10);screen.setPadding(16,18,16,18);content.addView(screen);content.addView(text("UI prototype only; it does not emulate the RP2040/RP2350 CPU or execute firmware.",13));content.addView(button("1 - Calculator",()->openTab("Calculator")));content.addView(button("2 - Terminal",()->openTab("Terminal")));content.addView(button("3 - MicroPython",()->openTab("MicroPython")));}
    private void showUsb(){heading("USB host");usbStatus=text("Checking connected devices…",14);content.addView(usbStatus);content.addView(button("Refresh devices",this::refreshUsb));refreshUsb();}
    private void refreshUsb(){if(usbStatus==null)return;UsbManager manager=(UsbManager)getSystemService(Context.USB_SERVICE);Map<String,UsbDevice> devices=manager.getDeviceList();StringBuilder b=new StringBuilder("USB host supported: ").append(getPackageManager().hasSystemFeature("android.hardware.usb.host")?"yes":"no").append("\n");if(devices.isEmpty())b.append("No USB devices detected. USB-OTG may be required.");else for(UsbDevice d:devices.values())b.append("\n").append(d.getDeviceName()).append("\nVID: ").append(d.getVendorId()).append(" PID: ").append(d.getProductId()).append("\nInterfaces: ").append(d.getInterfaceCount()).append("\n");usbStatus.setText(b.toString());}

    private boolean hasBtPermission(){return Build.VERSION.SDK_INT < 31 || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;}
    private void showBluetooth(){
        heading("Bluetooth Classic SPP");
        bluetoothStatus=text("Pair your Bluetooth serial adapter in Android Settings, then refresh this screen.",14);
        content.addView(bluetoothStatus);
        content.addView(button("Refresh paired devices",this::refreshBluetooth));
        content.addView(button("Disconnect",this::disconnectBluetooth));
        content.addView(button("Open terminal",()->openTab("Terminal")));
        refreshBluetooth();
    }
    private void refreshBluetooth(){
        if(bluetoothStatus==null)return;
        while(content.getChildCount()>5)content.removeViewAt(5);
        if(Build.VERSION.SDK_INT>=31&&!hasBtPermission()){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},REQUEST_BT_CONNECT);bluetoothStatus.setText("Grant Nearby devices permission, then refresh.");return;}
        BluetoothManager manager=(BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
        if(adapter==null){bluetoothStatus.setText("Bluetooth adapter unavailable.");return;}
        try{
            if(!adapter.isEnabled()){bluetoothStatus.setText("Bluetooth is off. Enable it in Android Settings.");return;}
            java.util.Set<BluetoothDevice> devices=adapter.getBondedDevices();
            StringBuilder list=new StringBuilder("Paired Bluetooth Classic devices:\n");
            if(devices.isEmpty())list.append("No paired devices. Pair an SPP adapter in Android Settings.");
            else for(BluetoothDevice device:devices){
                String name=device.getName();
                list.append(name==null?"Unnamed":name).append(" — ").append(device.getAddress()).append("\n");
                content.addView(button("Connect "+(name==null?device.getAddress():name),()->connectBluetooth(device)));
            }
            bluetoothStatus.setText(list.toString());
        }catch(SecurityException e){bluetoothStatus.setText("Bluetooth permission required: "+e.getMessage());}
    }
    private void connectBluetooth(BluetoothDevice device){
        if(Build.VERSION.SDK_INT>=31&&!hasBtPermission()){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},REQUEST_BT_CONNECT);return;}
        disconnectBluetooth();
        new Thread(()->{
            BluetoothSocket socket=null;
            try{
                socket=device.createRfcommSocketToServiceRecord(SPP_UUID);
                socket.connect();
                BluetoothSocket active=socket;
                mainHandler.post(()->{bluetoothSocket=active;toast("Bluetooth SPP connected");if(terminalLog!=null)terminalLog.append("[Bluetooth connected]\n");});
                InputStream in=active.getInputStream();byte[] buffer=new byte[1024];
                while(!active.isClosed()){
                    int n=in.read(buffer);if(n<0)break;
                    String received=new String(buffer,0,n,java.nio.charset.StandardCharsets.UTF_8);
                    mainHandler.post(()->{if(terminalLog!=null)terminalLog.append(received);});
                }
            }catch(Exception e){
                String message=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();
                mainHandler.post(()->{toast("Bluetooth connection failed: "+message);if(terminalLog!=null)terminalLog.append("[Bluetooth error] "+message+"\n");});
            }finally{if(socket!=null)try{socket.close();}catch(IOException ignored){}}
        },"PicoCalc-Bluetooth-SPP").start();
    }
    private void disconnectBluetooth(){BluetoothSocket old=bluetoothSocket;bluetoothSocket=null;if(old!=null)try{old.close();}catch(IOException ignored){}}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQUEST_BT_CONNECT){if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)refreshBluetooth();else if(bluetoothStatus!=null)bluetoothStatus.setText("Bluetooth permission denied.");}
    }
    @Override protected void onDestroy(){disconnectBluetooth();super.onDestroy();}

    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}