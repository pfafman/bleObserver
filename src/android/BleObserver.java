/*
 *  Ble Observer Plugin for Android SDK >= 21
 *
 */

package com.pfafman.bleObserver;


// Android Base
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.ParcelUuid;

// Cordova
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.LOG;


// JSON
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


// Android BLE
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;


// Java Utils
//import java.util.*;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;


@TargetApi(21)

public class BleObserver extends CordovaPlugin
{
  //General callback variables  
  private CallbackContext mScanCallbackContext;
  private BluetoothAdapter mBluetoothAdapter;

  // @Override
  // public void initialize(CordovaInterface cordova, CordovaWebView webView) {
  //   super.initialize(cordova, webView);
  //   // init code here
  // }


  //Actions
  @Override
  public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException
  {

    if (mBluetoothAdapter == null) {
      Activity activity = cordova.getActivity();
      BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
      mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    if ("start".equals(action)) { 
    
      startScan(args, callbackContext); 
      return true; 
    
    } else if ("stopScan".equals(action)) { 
    
      stopScan(args, callbackContext); 
      return true; 
    
    } else {

      return false;

    }

  }


  private void startScan(final JSONArray args, final CallbackContext callbackContext)
  {
    if (mScanCallbackContext != null) 
    {
      callbackContext.error("scanning");
      return;
    }
    mScanCallbackContext = callbackContext;

    // Run in a thread. I think the need for this goes away with Cordova 4+ !!!
    // cordova.getThreadPool().execute(new Runnable() {
    //   public void run() {

    // BLE Adapter
    BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();

    UUID[] serviceUUIDs = parseServiceUUIDList(args.getJSONArray(0));

    List<ScanFilter> filters = new ArrayList<ScanFilter>();
    
    if (serviceUUIDs == null || serviceUUIDs.length == 0) {
      for (UUID serviceUUID : serviceUUIDs) {
        ScanFilter uuidFilter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(serviceUUID)).build();
        filters.add(uuidFilter);
      }
    }

    ScanSettings settings = new ScanSettings.Builder()
      .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
      //.setReportDelay(0)
      .build();

    scanner.startScan(filters, settings, scanCallback);

    callbackContext.success("scanning started");

    //   }  
    // });

  }


  private void stopScan(final CallbackContext callbackContext)
  {
    if (mScanCallbackContext == null) 
    {
      callbackContext.error("not scanning");
      return;
    }
    mScanCallbackContext = null;

    // Run in a thread.  I think the need for this goes away with Cordova 4+ !!!
    // cordova.getThreadPool().execute(new Runnable() {
    //   public void run() {
        
    // BLE Adapter
    BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();

    // Stop scan
    scanner.stopScan(scanCallback);

    callbackContext.success("scanning stopped");

    //   }
    // });
    
  }


  //Scan Callback
  private ScanCallback scanCallback = new ScanCallback() {
    @Override
    public void onScanResult(final int callbackType, final ScanResult result) {
      
      //Log.i("onScanResult", result.toString());

      if (mScanCallbackContext == null)
      {
        return;
      }
      
      JSONObject returnObj = new JSONObject();
      addDevice(returnObj, result.getDevice());
      addProperty(returnObj, "rssi", result.getRssi());
      addPropertyBytes(returnObj, "advertisement", result.getScanRecord().getBytes());
      addProperty(returnObj, "status", "scanResult");
      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
      pluginResult.setKeepCallback(true);
      mScanCallbackContext.sendPluginResult(pluginResult);

    }

    @Override
    public void onBatchScanResults(final List<ScanResult> results) {
      if (mScanCallbackContext == null)
      {
        return;
      }
      
      for (ScanResult result : results) {
        //Log.i("ScanResult - Results", result.toString());
        
        JSONObject returnObj = new JSONObject();
        addDevice(returnObj, result.getDevice());
        addProperty(returnObj, "rssi", result.getRssi());
        addPropertyBytes(returnObj, "advertisement", result.getScanRecord().getBytes());
        addProperty(returnObj, "status", "scanResult");
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
        pluginResult.setKeepCallback(true);
        mScanCallbackContext.sendPluginResult(pluginResult);
            
      }

    }

    @Override
    public void onScanFailed(final int errorCode) {
      //Log.e("Scan Failed", "Error Code: " + errorCode);
      if (mScanCallbackContext == null)
      {
        return;
      }

      JSONObject returnObj = new JSONObject();
      addProperty(returnObj, "error", "startScan");
      addProperty(returnObj, "message", "Scan failed to start");
      mScanCallbackContext.error(returnObj);
      mScanCallbackContext = null;

    }
  };


  // General Helpers
  
  private void addProperty(JSONObject obj, String key, Object value)
  {
    //Believe exception only occurs when adding duplicate keys, so just ignore it
    try
    {
      obj.put(key, value);
    }
    catch (JSONException e)
    {

    }
  }


  private void addPropertyBytes(JSONObject obj, String key, byte[] bytes)
  {
    String string = Base64.encodeToString(bytes, Base64.NO_WRAP);

    addProperty(obj, key, string);
  }


  private JSONObject getArgsObject(JSONArray args)
  {
    if (args.length() == 1)
    {
      try
      {
        return args.getJSONObject(0);
      }
      catch (JSONException ex)
      {
      }
    }

    return null;
  }

  
  private void addDevice(JSONObject returnObj, BluetoothDevice device) {
    addProperty(returnObj, "address", device.getAddress());
    addProperty(returnObj, "name", device.getName());
  }


  private UUID[] parseServiceUUIDList(JSONArray jsonArray) throws JSONException {
    List<UUID> serviceUUIDs = new ArrayList<UUID>();

    for(int i = 0; i < jsonArray.length(); i++){
      String uuidString = jsonArray.getString(i);
      serviceUUIDs.add(uuidFromString(uuidString));
    }

    return serviceUUIDs.toArray(new UUID[jsonArray.length()]);
  }

}