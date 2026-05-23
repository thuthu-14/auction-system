package util;

public class DateTimeUtil {

    public static String formatTimestamp(long timestamp) {
        long seconds = timestamp / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    public static long getMinutesRemaining(long endTime) {
        long now = System.currentTimeMillis();
        if (now > endTime) return 0;
        return (endTime - now) / (60 * 1000);
    }

    public static long getSecondsRemaining(long endTime) {
        long now = System.currentTimeMillis();
        if (now > endTime) return 0;
        return (endTime - now) / 1000;
    }

    public static String formatTimeRemaining(long endTime) {
        long seconds = getSecondsRemaining(endTime);

        if (seconds <= 0) {
            return "00:00";
        }

        long minutes = seconds / 60;
        long secs = seconds % 60;

        return String.format("%02d:%02d", minutes, secs);
    }

    public static boolean isExpired(long endTime) {
        return System.currentTimeMillis() > endTime;
    }

    public static int millisecondsToMinutes(long milliseconds) {
        return (int) (milliseconds / (60 * 1000));
    }
}
