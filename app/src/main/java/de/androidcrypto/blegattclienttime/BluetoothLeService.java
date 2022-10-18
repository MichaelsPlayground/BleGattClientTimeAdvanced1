package de.androidcrypto.blegattclienttime;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private static final UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CURRENT_TIME           = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb");

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "android-er.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "android-er.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "android-er.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "android-er.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "android-er.EXTRA_DATA";
    public final static String ACTION_DATA_WRITE =
            "android-er.ACTION_DATA_WRITE";
    public final static String ACTION_SET_NOTIFICATION =
            "android-er.ACTION_SET_NOTIFICATION";
    public final static String ACTION_UNSET_NOTIFICATION =
            "android-er.ACTION_UNSET_NOTIFICATION";

    /**
     * this are the specific UUIDs for the Battery Service / Battery Level notifications
     */

    public static String String_TIMESERVER_BATTERY_LEVEL =
            "00002a19-0000-1000-8000-00805f9b34fb";
    public final static UUID UUID_TIMESERVER_BATTERY_LEVEL =
            UUID.fromString(String_TIMESERVER_BATTERY_LEVEL);
    public static String String_TIMESERVER_BATTERY_LEVEL_DESCRIPTOR =
            "00002902-0000-1000-8000-00805f9b34fb";
    public final static UUID UUID_TIMESERVER_BATTERY_LEVEL_DESCRIPTOR =
            UUID.fromString(String_TIMESERVER_BATTERY_LEVEL_DESCRIPTOR);

    // todo remove old stuff
    public static String String_GENUINO101_ledService =
            "19B10000-E8F2-537E-4F6C-D104768A1214";
    public final static ParcelUuid ParcelUuid_GENUINO101_ledService =
            ParcelUuid.fromString(String_GENUINO101_ledService);
    public final static UUID UUID_GENUINO101_ledService =
            UUID.fromString(String_GENUINO101_ledService);

    public static String String_GENUINO101_switchChar =
            "19B10001-E8F2-537E-4F6C-D104768A1214";
    public final static UUID UUID_GENUINO101_switchChare =
            UUID.fromString(String_GENUINO101_switchChar);

    /**
     * some utility methods to check for characteristics options
     */

    /**
     * Check if a characteristic supports write permissions
     * @return Returns <b>true</b> if property is writable
     */
    public static boolean isCharacteristicWritable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }

    /**
     * Check if a characteristic supports write wuthout response permissions
     * @return Returns <b>true</b> if property is writable
     */
    public static boolean isCharacteristicWritableWithoutResponse(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0;
    }

    /**
     * Check if a characteristic supports write with permissions
     * @return Returns <b>true</b> if property is writable
     */
    public static boolean isCharacteristicWritableWithResponse(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
    }

    /**
     * Check if a characteristic supports Notifications
     *
     * @return Returns <b>true</b> if property is supports notification
     */
    public static boolean isCharacteristicNotifiable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }

    /**
     * Check if a Descriptor can be read
     *
     * @param descriptor a descriptor to check
     * @return Returns <b>true</b> if descriptor is readable
     */
    public static boolean isDescriptorReadable(BluetoothGattDescriptor descriptor) {
        return (descriptor.getPermissions() & BluetoothGattCharacteristic.PERMISSION_WRITE) != 0;
    }

    /**
     * Check if a Descriptor can be written
     *
     * @param descriptor a descriptor to check
     * @return Returns <b>true</b> if descriptor is writeable
     */
    public static boolean isDescriptorWriteable(BluetoothGattDescriptor descriptor) {
        return (descriptor.getPermissions() & BluetoothGattCharacteristic.PERMISSION_WRITE) != 0;
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        /**
         * Characteristic was written successfully.  update the UI
         *
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.v(TAG, "characteristic written");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_WRITE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG,"onDescriptorWrite");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_SET_NOTIFICATION, descriptor);
            }
        }

    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattDescriptor descriptor) {
        Log.i(TAG, "broadcastUpdate descriptor: " + descriptor.getUuid());
        final Intent intent = new Intent(action);

        if (UUID_TIMESERVER_BATTERY_LEVEL_DESCRIPTOR.equals(descriptor.getUuid())) {
            Log.i(TAG, "BATTERY_LEVEL_DESCRIPTOR found");
            intent.putExtra(EXTRA_DATA,"BATTERY_LEVEL");
        }
        sendBroadcast(intent);
    }


    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx
        // ?u=org.bluetooth.characteristic.heart_rate_measurement.xml

        System.out.println("*** characteristic.getUuid: " + characteristic.getUuid());

        // for testing purposes
        for (BluetoothGattDescriptor descriptor:characteristic.getDescriptors()){
            Log.e(TAG, "BluetoothGattDescriptor: "+descriptor.getUuid().toString());
        }

        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            System.out.println("* UUID_HEART_RATE_MEASUREMENT found *");
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else if (UUID_CURRENT_TIME.equals(characteristic.getUuid())) {
            System.out.println("* UUID_UUID_CURRENT_TIME found *");
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                String receivedTime = CurrentTimeService.getTimestampFromService(data);
                Log.d(TAG, String.format("Received heart rate: %s", receivedTime));
                intent.putExtra(EXTRA_DATA,receivedTime);
            }

        } else {
            System.out.println("* unknown UUID found *");
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }

/*
        //remove special handling for time being
        Log.w(TAG, "broadcastUpdate()");

        final byte[] data = characteristic.getValue();

        Log.v(TAG, "data.length: " + data.length);

        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data) {
                stringBuilder.append(String.format("%02X ", byteChar));

                Log.v(TAG, String.format("%02X ", byteChar));
            }
            intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
        }
*/
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(
     * android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    @SuppressLint("MissingPermission")
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(
     * android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    @SuppressLint("MissingPermission")
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(
     * android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    @SuppressLint("MissingPermission")
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    @SuppressLint("MissingPermission")
    public void changeCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        Log.i(TAG, "setCharacteristicNotification for characteristic: "
                + characteristic.getUuid() + " to " + enabled);

        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // this is specific to TimeServer BatteryService - Battery Level
        if (UUID_TIMESERVER_BATTERY_LEVEL.equals(characteristic.getUuid())) {
            Log.i(TAG, "UUID_TIMESERVER_BATTERY_LEVEL.equals(characteristic.getUuid");
            Log.i(TAG, "Characteristic UUID: " + characteristic.getUuid());

            List<BluetoothGattDescriptor> listDescriptors = characteristic.getDescriptors();
            Log.i(TAG, "listDescriptors size: " + listDescriptors.size());
            Log.i(TAG, "list 0 UUID: " + listDescriptors.get(0).getUuid());

            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID_TIMESERVER_BATTERY_LEVEL_DESCRIPTOR);
            Log.i(TAG, "Descriptor UUID: " + descriptor.getUuid());
            Log.i(TAG, "BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE: "
                    + BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
            Log.i(TAG, "mBluetoothGatt.writeCharacteristic done for " + UUID_TIMESERVER_BATTERY_LEVEL);
            Log.i(TAG, "mBluetoothGatt.writeDescriptor done for " + descriptor.getUuid());
        }

        // This is specific to Genuino 101 ledService.
        if (UUID_GENUINO101_ledService.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID_GENUINO101_switchChare);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Request a write to a given {@code BluetoothGattCharacteristic}.
     *
     * @param characteristic The characteristic to write to.
     */
    @SuppressLint("MissingPermission")
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, int data) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.i(TAG, "writeCharacteristic called, fired to mBluetoothGatt.writeCharacteristic");
        characteristic.setValue(data, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        mBluetoothGatt.writeCharacteristic(characteristic);
    }


    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

}
