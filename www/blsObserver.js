
var bleObserverName = "BleObserver";
                       

var bleObserver = {
  start: function(successCallback, errorCallback, params) {
    cordova.exec(successCallback, errorCallback, bleObserverName, "start", [params]); 
  },
  stop: function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, bleObserverName, "stop", []);
  },
  encodedStringToBytes: function(string) {
    var data = atob(string);
    var bytes = new Uint8Array(data.length);
    for (var i = 0; i < bytes.length; i++)
    {
      bytes[i] = data.charCodeAt(i);
    }
    return bytes;
  },
  bytesToEncodedString: function(bytes) {
    return btoa(String.fromCharCode.apply(null, bytes));
  },
  stringToBytes: function(string) {
    var bytes = new ArrayBuffer(string.length * 2);
    var bytesUint16 = new Uint16Array(bytes);
    for (var i = 0; i < string.length; i++) {
      bytesUint16[i] = string.charCodeAt(i);
    }
    return new Uint8Array(bytesUint16);
  },
  bytesToString: function(bytes) {
    return String.fromCharCode.apply(null, new Uint16Array(bytes));
  }
}

module.exports = bleObserver;