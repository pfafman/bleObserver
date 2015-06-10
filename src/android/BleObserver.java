/*
 *  Ble Observer Plugin for Android SDK >= 21
 *
 */

package com.pfafman.bleObserver;


// Android Base
import android.annotation.TargetApi;
import android.app.Activity;


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

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;


// Java Utils
//import java.util.*;
import java.util.UUID;


@TargetApi(21)

public class BluetoothLePlugin extends CordovaPlugin
{
  //General callback variables  
  private CallbackContext mScanCallbackContext;
  private final BluetoothAdapter mBluetoothAdapter;

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


  private void startScanAction(final JSONArray args, final CallbackContext callbackContext)
  {
    if (scanCallbackContext != null) 
    {
      callbackContext.error("scanning");
      return;
    }
    scanCallbackContext = callbackContext

    // Run in a thread. I think the need for this goes away with Cordova 4+ !!!
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {

        // BLE Adapter
        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();

        UUID[] serviceUUIDs = parseServiceUUIDList(args.getJSONArray(0));

        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        
        if (serviceUuids == null || serviceUuids.length == 0) {
          for (UUID serviceUuid : serviceUuids) {
            ScanFilter uuidFilter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(serviceUuid)).build();
            filters.add(uuidFilter);
          }
        }

        ScanSettings settings = new ScanSettings.Builder()
          .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
          //.setReportDelay(0)
          .build();

        scanner.startScan(filters, settings, scanCallback);

        callbackContext.success("scanning started");
      }  
    });
  }


  private void stopScanAction(final CallbackContext callbackContext)
  {
    if (scanCallbackContext == null) 
    {
      callbackContext.error("not scanning");
      return;
    }
    scanCallbackContext = null;

    // Run in a thread.  I think the need for this goes away with Cordova 4+ !!!
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        // BLE Adapter
        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();

        // Stop scan
        scanner.stopScan(scanCallback);
    
        callbackContext.success("scanning stopped");
      }
    });
    
  }


  //Scan Callback
  private ScanCallback scanCallback = new ScanCallback() {
    @Override
    public void onScanResult(final int callbackType, final ScanResult result) {
      
      //Log.i("onScanResult", result.toString());

      if (scanCallbackContext == null)
      {
        return;
      }
      
      JSONObject returnObj = new JSONObject();
      addDevice(returnObj, result.getDevice());
      addProperty(returnObj, keyRssi, result.getRssi());
      addPropertyBytes(returnObj, keyAdvertisement, result.getScanRecord().getBytes());
      addProperty(returnObj, keyStatus, statusScanResult);
      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
      pluginResult.setKeepCallback(true);
      scanCallbackContext.sendPluginResult(pluginResult);

    }

    @Override
    public void onBatchScanResults(final List<ScanResult> results) {
      if (scanCallbackContext == null)
      {
        return;
      }
      
      for (ScanResult result : results) {
        //Log.i("ScanResult - Results", result.toString());
        
        JSONObject returnObj = new JSONObject();
        addDevice(returnObj, result.getDevice());
        addProperty(returnObj, keyRssi, result.getRssi());
        addPropertyBytes(returnObj, keyAdvertisement, result.getScanRecord().getBytes());
        addProperty(returnObj, keyStatus, statusScanResult);
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
        pluginResult.setKeepCallback(true);
        scanCallbackContext.sendPluginResult(pluginResult);
            
      }

    }

    @Override
    public void onScanFailed(final int errorCode) {
      //Log.e("Scan Failed", "Error Code: " + errorCode);
      if (scanCallbackContext == null)
      {
        return;
      }

      JSONObject returnObj = new JSONObject();
      addProperty(returnObj, keyError, errorStartScan);
      addProperty(returnObj, keyMessage, logScanStartFail);
      scanCallbackContext.error(returnObj);
      scanCallbackContext = null;

    }
  };

  private UUID[] parseServiceUUIDList(JSONArray jsonArray) throws JSONException {
    List<UUID> serviceUUIDs = new ArrayList<UUID>();

    for(int i = 0; i < jsonArray.length(); i++){
      String uuidString = jsonArray.getString(i);
      serviceUUIDs.add(uuidFromString(uuidString));
    }

    return serviceUUIDs.toArray(new UUID[jsonArray.length()]);
  }
}