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
import android.util.Base64;
import android.content.Intent;
import android.content.IntentFilter;


// Cordova
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;


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
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;


// Debugging 
//import android.util.Log;
import org.apache.cordova.LOG;


@TargetApi(21)

public class BleObserverPlugin extends CordovaPlugin
{
  // General callback variables  
  private CallbackContext mScanCallbackContext;
  private CallbackContext mInitCallbackContext;
  
  // BLE Adapter
  private BluetoothAdapter mBluetoothAdapter;

  private final String mBaseUuidStart = "0000";
  private final String mBaseUuidEnd = "-0000-1000-8000-00805f9b34fb";

  // @Override
  // public void initialize(CordovaInterface cordova, CordovaWebView webView) {
  //   super.initialize(cordova, webView);
  //   // init code here
  // }


  //Actions
  @Override
  public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException
  {

    LOG.i("BleObserverPlugin:execute", action);

    if (mBluetoothAdapter == null) {
      Activity activity = cordova.getActivity();
      BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
      mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    if ("start".equals(action)) { 
    
      startScan(args, callbackContext); 
      return true; 
    
    } else if ("stop".equals(action)) { 
    
      stopScan(callbackContext); 
      return true; 

     } else if ("flush".equals(action)) { 
    
      flushPendingScanResults(callbackContext); 
      return true; 

    } else if ("isEnabled".equals(action)) { 
    
      isEnabled(callbackContext); 
      return true; 

    } else if ("enable".equals(action)) { 
    
      enable(callbackContext); 
      return true; 
    
    } else if ("disable".equals(action)) { 
    
      disable(callbackContext); 
      return true; 

    } else {

      return false;

    }

  }


  // Start Scanner
  private void startScan(final JSONArray args, final CallbackContext callbackContext)
  {
    LOG.i("BleObserverPlugin:startScan", "call");
    if (mScanCallbackContext != null) {
      callbackContext.error("scanning");
      return;
    }

    if (mBluetoothAdapter.isEnabled()) {

      mScanCallbackContext = callbackContext;

      // Run in a thread. I think the need for this goes away with Cordova 4+ !!!
      // cordova.getThreadPool().execute(new Runnable() {
      //   public void run() {

      // BLE Adapter
      BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();

      //Get the service UUIDs from the arguments
      UUID[] serviceUUIDs = null;
      JSONObject obj = getArgsObject(args);
      if (obj != null) {
        serviceUUIDs = getServiceUuids(obj);
      }
      
      List<ScanFilter> filters = new ArrayList<ScanFilter>();
      
      if (serviceUUIDs != null && serviceUUIDs.length > 0) {
        for (UUID serviceUUID : serviceUUIDs) {
          ScanFilter uuidFilter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(serviceUUID)).build();
          filters.add(uuidFilter);
        }
      }

      ScanSettings settings = new ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        //.setReportDelay(0)
        .build();

      LOG.i("BleObserverPlugin:startScan", "startScan");
      scanner.startScan(filters, settings, scanCallback);

      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "scanning started");
      pluginResult.setKeepCallback(true);
      callbackContext.sendPluginResult(pluginResult);

      //   }  
      // });

    } else {

       //Request Bluetooth to be enabled
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      cordova.startActivityForResult(this, enableBtIntent, REQUEST_BT_ENABLE);
    
    }

  }


  // Stop Scanner
  private void stopScan(final CallbackContext callbackContext)
  {
    LOG.i("BleObserverPlugin:stopScan", "called");
    if (mScanCallbackContext == null) {
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
    LOG.i("BleObserverPlugin:stopScan", "stopScan");
    scanner.stopScan(scanCallback);

    callbackContext.success("scanning stopped");

    //   }
    // });
    
  }


  // flush scanner
  private void flushPendingScanResults(final CallbackContext callbackContext)
  {
    LOG.i("BleObserverPlugin:flushPendingScanResults", "called");
    if (mScanCallbackContext == null) {
      callbackContext.error("not scanning");
      return;
    }

    // BLE Adapter
    BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();

    // Stop scan
    LOG.i("BleObserverPlugin:flushPendingScanResults", "flushPendingScanResults");
    scanner.flushPendingScanResults(scanCallback);    

    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "flushing");
    //pluginResult.setKeepCallback(true);
    callbackContext.sendPluginResult(pluginResult);

  }

  // Ble is Enabled
  private void isEnabled(final CallbackContext callbackContext)
  {
    LOG.i("BleObserverPlugin:isEnabled", "called");

    boolean result = (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled());

    JSONObject returnObj = new JSONObject();
    addProperty(returnObj, "enabled", result);

    callbackContext.success(returnObj);
  }

  // Enable BLE
  private void enable(final CallbackContext callbackContext)
  {
    LOG.i("BleObserverPlugin:enable", "called");

    boolean result = (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled());

    JSONObject returnObj = new JSONObject();
    addProperty(returnObj, "enabled", result);

    callbackContext.success(returnObj);
  }

  // Disable BLE
  private void disable(final CallbackContext callbackContext)
  {
    LOG.i("BleObserverPlugin:disable", "called");

    boolean result = (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled());

    JSONObject returnObj = new JSONObject();
    addProperty(returnObj, "enabled", result);

    callbackContext.success(returnObj);
  }


  //Scan Callback
  private ScanCallback scanCallback = new ScanCallback() {
    @Override
    public void onScanResult(final int callbackType, final ScanResult result) {
      
      LOG.i("onScanResult", result.toString());

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
        LOG.i("ScanResult - Results", result.toString());
        
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
      LOG.e("Scan Failed", "Error Code: " + errorCode);
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


  private UUID[] getServiceUuids(JSONObject obj)
  {
    JSONArray array = obj.optJSONArray("serviceUUIDs");

    if (array == null)
    {
      return null;
    }

    //Create temporary array list for building array of UUIDs
    ArrayList<UUID> arrayList = new ArrayList<UUID>();

    //Iterate through the UUID strings
    for (int i = 0; i < array.length(); i++)
    {
      String value = array.optString(i, null);

      if (value == null)
      {
        continue;
      }

      if (value.length() == 4)
      {
        value = mBaseUuidStart + value + mBaseUuidEnd;
      }


      //Try converting string to UUID and add to list
      try
      {
        UUID uuid = UUID.fromString(value);
        arrayList.add(uuid);
      }
      catch (Exception ex)
      {
      }
    }

    //If anything was actually added, convert list to array
    int size = arrayList.size();

    if (size == 0)
    {
      return null;
    }

    UUID[] uuids = new UUID[size];
    uuids = arrayList.toArray(uuids);
    return uuids;
  }


}