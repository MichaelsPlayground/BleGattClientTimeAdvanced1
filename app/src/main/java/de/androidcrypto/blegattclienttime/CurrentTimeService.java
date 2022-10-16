package de.androidcrypto.blegattclienttime;

import android.annotation.SuppressLint;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class CurrentTimeService {

    /**
     * The following code shows how to convert a CurrentTimeService value to
     * the timestamp
     */

    public long getTimestamp() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return timestamp.getTime();
    }

    // E6 07 0A 10 0D 2F 34 07 03 00
    // 16.10.2022 13:48

    @SuppressLint("SimpleDateFormat")
    public static String getTimestampFromService (byte[] data) {
        byte year1 = data[0];
        byte year2 = data[1];
        byte month = data[2];
        byte day = data[3];
        byte hours = data[4];
        byte minutes = data[5];
        byte seconds = data[6];
        // byte getDayOfWeekCode = data[7];
        // byte Fractions256 = data[7];
        byte adjustReason = data[9];
        Timestamp timestamp = makeTimestamp((year2 * 256) + Byte.toUnsignedInt(year1), month, day, hours, minutes, seconds, 0);
        final SimpleDateFormat sdf1 = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        return sdf1.format(timestamp);
    }

    public static Timestamp makeTimestamp(int year, int month, int day, int hour, int minute,
                                          int second, int millisecond) {
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DATE, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, millisecond);

        // now convert GregorianCalendar object to Timestamp object
        return new Timestamp(cal.getTimeInMillis());
    }

    /**
     * The following code shows how to convert a timestamp to the
     * CurrentTimeService value
     * see: BleGattServerTime
     */

    public static byte[] getExactTime(long timestamp, byte adjustReason) {
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(timestamp);

        byte[] field = new byte[10];

        // Year
        int year = time.get(Calendar.YEAR);
        field[0] = (byte) (year & 0xFF);
        field[1] = (byte) ((year >> 8) & 0xFF);
        // Month
        field[2] = (byte) (time.get(Calendar.MONTH) + 1);
        // Day
        field[3] = (byte) time.get(Calendar.DATE);
        // Hours
        field[4] = (byte) time.get(Calendar.HOUR_OF_DAY);
        // Minutes
        field[5] = (byte) time.get(Calendar.MINUTE);
        // Seconds
        field[6] = (byte) time.get(Calendar.SECOND);
        // Day of Week (1-7)
        field[7] = getDayOfWeekCode(time.get(Calendar.DAY_OF_WEEK));
        // Fractions256
        field[8] = (byte) (time.get(Calendar.MILLISECOND) / 256);

        field[9] = adjustReason;

        return field;
    }

    /* Bluetooth Weekday Codes */
    private static final byte DAY_UNKNOWN = 0;
    private static final byte DAY_MONDAY = 1;
    private static final byte DAY_TUESDAY = 2;
    private static final byte DAY_WEDNESDAY = 3;
    private static final byte DAY_THURSDAY = 4;
    private static final byte DAY_FRIDAY = 5;
    private static final byte DAY_SATURDAY = 6;
    private static final byte DAY_SUNDAY = 7;

    /**
     * Convert a {@link Calendar} weekday value to the corresponding
     * Bluetooth weekday code.
     */
    private static byte getDayOfWeekCode(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                return DAY_MONDAY;
            case Calendar.TUESDAY:
                return DAY_TUESDAY;
            case Calendar.WEDNESDAY:
                return DAY_WEDNESDAY;
            case Calendar.THURSDAY:
                return DAY_THURSDAY;
            case Calendar.FRIDAY:
                return DAY_FRIDAY;
            case Calendar.SATURDAY:
                return DAY_SATURDAY;
            case Calendar.SUNDAY:
                return DAY_SUNDAY;
            default:
                return DAY_UNKNOWN;
        }
    }
}
