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

Find the specifications for BLE services here:

https://www.bluetooth.com/specifications/specs/

Time Server: Current Time Service 1.1: https://www.bluetooth.com/specifications/specs/current-time-service-1-1/

A simulator for Android BLE server: https://play.google.com/store/apps/details?id=io.github.webbluetoothcg.bletestperipheral
( simulates a Battery service, a Heart Rate Monitor or a Health Thermometer)

https://play.google.com/store/apps/details?id=com.vance.cwartist.cwsimulation
(simulates Battery, Heart Rate and chatting, english & chinese)

https://play.google.com/store/apps/details?id=com.zhctwh.ble_tester
(simulates )

Explorer: https://play.google.com/store/apps/details?id=com.punchthrough.lightblueexplorer&hl=de&gl=US


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

Note on Queueable transactions: Android is not been able to run a lot of /write) requests 
in a row so you need to wait until one transaction is done and the start the next one.

Here is an example for a TxQueue

Source taken from: https://gist.github.com/SoulAuctioneer/ee4cb9bc0b3785bbdd51

```plaintext
        // we got response regarding our request to fetch characteristic value
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status)
        {
            // Ready for next transmission
            processTxQueue();

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Succeeded, so we can get the value
                getCharacteristicValue(characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            String deviceName = gatt.getDevice().getName();
            String serviceName = BleNamesResolver.resolveServiceName(characteristic.getService().getUuid().toString().toLowerCase(Locale.getDefault()));
            String charName = BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString().toLowerCase(Locale.getDefault()));
            String description = "Device: " + deviceName + " Service: " + serviceName + " Characteristic: " + charName;

            // Ready for next transmission
            processTxQueue();

            // we got response regarding our request to write new value to the characteristic
            // let see if it failed or not
            if(status == BluetoothGatt.GATT_SUCCESS) {
                mUiCallback.uiSuccessfulWrite(mBluetoothGatt, mBluetoothDevice, mBluetoothSelectedService, characteristic, description);
            }
            else {
                mUiCallback.uiFailedWrite(mBluetoothGatt, mBluetoothDevice, mBluetoothSelectedService, characteristic, description + " STATUS = " + status);
            }
        };

        /**
         * Callback indicating the result of a descriptor write operation.
         *
         * @param gatt GATT client invoked {@link BluetoothGatt#writeDescriptor}
         * @param descriptor Descriptor that was written to the associated
         *                   remote device.
         * @param status The result of the write operation
         *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
            Log.d("-----", "Wrote a descriptor");

            // Ready for next transmission
            processTxQueue();
        }
    };

    /* An enqueueable write operation - notification subscription or characteristic write */
    private class TxQueueItem
    {
        BluetoothGattCharacteristic characteristic;
        byte[] dataToWrite; // Only used for characteristic write
        boolean enabled; // Only used for characteristic notification subscription
        public TxQueueItemType type;
    }

    /**
     * The queue of pending transmissions
     */
    private Queue<TxQueueItem> txQueue = new LinkedList<TxQueueItem>();

    private boolean txQueueProcessing = false;

    private enum TxQueueItemType {
        SubscribeCharacteristic,
        ReadCharacteristic,
        WriteCharacteristic
    }

    /* queues enables/disables notification for characteristic */
    public void queueSetNotificationForCharacteristic(BluetoothGattCharacteristic ch, boolean enabled)
    {
        // Add to queue because shitty Android GATT stuff is only synchronous
        TxQueueItem txQueueItem = new TxQueueItem();
        txQueueItem.characteristic = ch;
        txQueueItem.enabled = enabled;
        txQueueItem.type = TxQueueItemType.SubscribeCharacteristic;
        addToTxQueue(txQueueItem);
    }

    /* queues enables/disables notification for characteristic */
    public void queueWriteDataToCharacteristic(final BluetoothGattCharacteristic ch, final byte[] dataToWrite)
    {
        // Add to queue because shitty Android GATT stuff is only synchronous
        TxQueueItem txQueueItem = new TxQueueItem();
        txQueueItem.characteristic = ch;
        txQueueItem.dataToWrite = dataToWrite;
        txQueueItem.type = TxQueueItemType.WriteCharacteristic;
        addToTxQueue(txQueueItem);
    }

    /* request to fetch newest value stored on the remote device for particular characteristic */
    public void queueRequestCharacteristicValue(BluetoothGattCharacteristic ch) {

        // Add to queue because shitty Android GATT stuff is only synchronous
        TxQueueItem txQueueItem = new TxQueueItem();
        txQueueItem.characteristic = ch;
        txQueueItem.type = TxQueueItemType.ReadCharacteristic;
        addToTxQueue(txQueueItem);
    }

    /**
     * Add a transaction item to transaction queue
     * @param txQueueItem
     */
    private void addToTxQueue(TxQueueItem txQueueItem) {

        txQueue.add(txQueueItem);

        // If there is no other transmission processing, go do this one!
        if (!txQueueProcessing) {
            processTxQueue();
        }
    }

    /**
     * Call when a transaction has been completed.
     * Will process next transaction if queued
     */
    private void processTxQueue()
    {
        if (txQueue.size() <= 0)  {
            txQueueProcessing = false;
            return;
        }

        txQueueProcessing = true;
        TxQueueItem txQueueItem = txQueue.remove();
        switch (txQueueItem.type) {
            case WriteCharacteristic:
                writeDataToCharacteristic(txQueueItem.characteristic, txQueueItem.dataToWrite);
                break;
            case SubscribeCharacteristic:
                setNotificationForCharacteristic(txQueueItem.characteristic, txQueueItem.enabled);
                break;
            case ReadCharacteristic:
                requestCharacteristicValue(txQueueItem.characteristic);
        }
    }

```


