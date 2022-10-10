# Bluetooth Low Energy (LE) Sample Gatt Client

The code is based on an article series found here:

- 1 Bluetooth LE Gatt Example, step-by-step http://android-er.blogspot.com/2016/06/bluetooth-le-gatt-example-step-by-step.html
- 2 Bluetooth LE Gatt Example, scan BLE devices http://android-er.blogspot.com/2016/06/bluetooth-le-gatt-example-scan-ble.html
- 3 Scan specified BLE devices with ScanFilter http://android-er.blogspot.com/2016/06/scan-specified-ble-devices-with.html
- 4 Bluetooth LE example - connect to Bluetooth LE device and display GATT Services http://android-er.blogspot.com/2016/07/bluetooth-le-example-connect-to.html

It runs on Android 12 (SDK32) and for testing you need a companion device or real BLE device.

For general information see here:
- 1 Bluetooth Low Energy overview https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview 
- 2 Find BLE devices https://developer.android.com/guide/topics/connectivity/bluetooth/find-ble-devices
- 3 Connect to a GATT server https://developer.android.com/guide/topics/connectivity/bluetooth/connect-gatt-server
- 4 Transfer BLE data https://developer.android.com/guide/topics/connectivity/bluetooth/transfer-ble-data

```plaintext
    <!-- Min/target SDK versions (<uses-sdk>) managed by build.gradle -->
    <!-- Request legacy Bluetooth permissions on older devices. -->
    <uses-permission
        android:name="android.permission.BLUETOOTH"
        android:maxSdkVersion="30" />
    <uses-permission
        android:name="android.permission.BLUETOOTH_ADMIN"
        android:maxSdkVersion="30" />
    <!--
 Needed only if your app looks for Bluetooth devices.
         If your app doesn't use Bluetooth scan results to derive physical
         location information, you can strongly assert that your app
         doesn't derive physical location.
    -->
    <uses-permission
        android:name="android.permission.BLUETOOTH_SCAN"
        android:usesPermissionFlags="neverForLocation"
        tools:targetApi="s" />
    <!--
 Needed only if your app makes the device discoverable to Bluetooth
         devices.
    -->
    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
    <!--
 Needed only if your app communicates with already-paired Bluetooth
         devices.
    -->
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" /> <!-- Needed only if your app uses Bluetooth scan results to derive physical location. -->
    <uses-permission
        android:name="android.permission.ACCESS_FINE_LOCATION"
        tools:ignore="CoarseFineLocation" />
```



```plaintext
    /**
     * This block is for requesting permissions up to Android 12+
     *
     */

    private static final int PERMISSIONS_REQUEST_CODE = 191;
    private static final String[] BLE_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };
    @SuppressLint("InlinedApi")
    private static final String[] ANDROID_12_BLE_PERMISSIONS = new String[]{
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    public static void requestBlePermissions(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ActivityCompat.requestPermissions(activity, ANDROID_12_BLE_PERMISSIONS, requestCode);
        else
            ActivityCompat.requestPermissions(activity, BLE_PERMISSIONS, requestCode);
    }
```