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

Article series (Java) on medium regarding BLE from Martijn van Welie:

-Making Android BLE work part 1 (Scanning): https://medium.com/@martijn.van.welie/making-android-ble-work-part-1-a736dcd53b02
-Making Android BLE work part 2 (connecting, disconnecting and discovering services): https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07
-Making Android BLE work part 3 (reading+ writing characteristics, turning notifications on and off): https://medium.com/@martijn.van.welie/making-android-ble-work-part-3-117d3a8aee23
-Making Android BLE work part 4 (bonding): https://medium.com/@martijn.van.welie/making-android-ble-work-part-4-72a0b85cb442

Library from the same author: https://github.com/weliem/blessed-android 

Another example for a queue: https://medium.com/@martijn.van.welie/making-android-ble-work-part-3-117d3a8aee23 
```plaintext
Using a queue
Since it is very annoying that you can only do one read/write at the time, any serious application will have to manage this. The solution to this issue is to implement a command queue. All of the BLE libraries I mentioned earlier implement a queue! This is really a best practice!
The idea is that every command you want to do will first be added to the queue. A command is then taken from the queue, executed and when the result comes in the command is marked ‘completed’ and taken off the queue. Then the next command in the queue can be executed. That way you can issue read/write commands whenever you want and they will be executed in the order you enqueued them. This make BLE programming a LOT easier. On iOS, a similar queuing mechanism is implemented inside of CoreBluetooth. Believe me, this is really what you want!
The queue we need to make has to be a queue per BluetoothGatt object. Luckily, Android will handle the queuing of commands from multiple BluetoothGatt objects, so you don’t need to worry about that. There are many ways to create a queue and for this article I’ll show how to make a simple queue by using a Runnable for every command. We first declare the queue using a Queue object and also declare a ‘lock’ variable to keep track whether an operation is in progress or not:
private Queue<Runnable> commandQueue;
private boolean commandQueueBusy;
We then add a new Runnable to the queue when we do a command. Here is an example for the readCharacteristic command:
public boolean readCharacteristic(final BluetoothGattCharacteristic characteristic) {
    if(bluetoothGatt == null) {
        Log.e(TAG, "ERROR: Gatt is 'null', ignoring read request");
        return false;
    }

    // Check if characteristic is valid
    if(characteristic == null) {
        Log.e(TAG, "ERROR: Characteristic is 'null', ignoring read request");
        return false;
    }

    // Check if this characteristic actually has READ property
    if((characteristic.getProperties() & PROPERTY_READ) == 0 ) {
        Log.e(TAG, "ERROR: Characteristic cannot be read");
        return false;
    }

    // Enqueue the read command now that all checks have been passed
    boolean result = commandQueue.add(new Runnable() {
        @Override
        public void run() {
            if(!bluetoothGatt.readCharacteristic(characteristic)) {
                Log.e(TAG, String.format("ERROR: readCharacteristic failed for characteristic: %s", characteristic.getUuid()));
                completedCommand();
            } else {
                Log.d(TAG, String.format("reading characteristic <%s>", characteristic.getUuid()));
                nrTries++;
            }
        }
    });

    if(result) {
        nextCommand();
    } else {
        Log.e(TAG, "ERROR: Could not enqueue read characteristic command");
    }
    return result;
}
In these method we first check if all fields are valid since we don't want to add commands to the queue if we know they will fail. This will also help debugging your application as you will see messages in the log if you trying to do read or write operations on characteristics that don't support those operations. Inside the Runnable we actually make the call to readCharacteristic() which will issue the read command to the device. We also keep track of how many times we tried to executed this command because maybe we’ll have to retry it later. If it returns false we log an error and ‘complete’ the command so that the next command in the queue can be started. Finally, we call nextCommand() to nudge the queue to start executing:
private void nextCommand() {
    // If there is still a command being executed then bail out
    if(commandQueueBusy) {
        return;
    }

    // Check if we still have a valid gatt object
    if (bluetoothGatt == null) {
        Log.e(TAG, String.format("ERROR: GATT is 'null' for peripheral '%s', clearing command queue", getAddress()));
        commandQueue.clear();
        commandQueueBusy = false;
        return;
    }

    // Execute the next command in the queue
    if (commandQueue.size() > 0) {
        final Runnable bluetoothCommand = commandQueue.peek();
        commandQueueBusy = true;
        nrTries = 0;

        bleHandler.post(new Runnable() {
            @Override
            public void run() {
                    try {
                        bluetoothCommand.run();
                    } catch (Exception ex) {
                        Log.e(TAG, String.format("ERROR: Command exception for device '%s'", getName()), ex);
                    }
            }
        });
    }
}
Note that we use a peek() to obtain the Runnable from the queue. That leaves the Runnable on the queue so we can retry it later if we have to.
After the read is complete the result will come in on your callback:
@Override
public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
    // Perform some checks on the status field
    if (status != GATT_SUCCESS) {
        Log.e(TAG, String.format(Locale.ENGLISH,"ERROR: Read failed for characteristic: %s, status %d", characteristic.getUuid(), status));
        completedCommand();
        return;
    }

    // Characteristic has been read so processes it   
    ...
    // We done, complete the command
    completedCommand();
}
So if there was an error we log it. Otherwise you process the value of the characteristic. Note that we call completedCommand() only after you are done processing the new value! This will make sure we there is no other command running while you process the value and helps to avoid race conditions.
Then we complete this command, take the Runnable off the queue by calling poll() and start the next command in the queue:
private void completedCommand() {
    commandQueueBusy = false;
    isRetrying = false;
    commandQueue.poll();
    nextCommand();
}
In some cases you may have to retry a command. We can do that easily since the Runnable is still on the queue. In order to make sure we don’t endlessly retry commands we also check if we have reached the retry limit:
private void retryCommand() {
    commandQueueBusy = false;
    Runnable currentCommand = commandQueue.peek();
    if(currentCommand != null) {
        if (nrTries >= MAX_TRIES) {
            // Max retries reached, give up on this one and proceed
            Log.v(TAG, "Max number of tries reached");
            commandQueue.poll();
        } else {
            isRetrying = true;
        }
    }
    nextCommand();
}

```

