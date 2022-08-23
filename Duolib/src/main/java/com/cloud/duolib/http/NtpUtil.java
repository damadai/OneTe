package com.cloud.duolib.http;

public class NtpUtil {

    private static final String[] ntpServerHost = new String[]{
            "ntp1.aliyun.com",
            "ntp2.aliyun.com",
            "ntp3.aliyun.com"
    };

    private static long getTimeFromNtpServer(String ntpHost) {
        SntpClient client = new SntpClient();
        try {
            boolean isSuccessful = client.requestTime(ntpHost, 3000);
            if (isSuccessful) {
                return client.getNtpTime();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return -1;
    }

    public interface TimeDifResponse {
        void onSuccess(Long data);
    }

    public interface SafeDifResponse {
        void onTimeout();

        void onTimeSafe();
    }

    public void initTimeDif(TimeDifResponse mTimeDif) {
        try {
            for (int i = 0; i < ntpServerHost.length; i++) {
                long time = getTimeFromNtpServer(ntpServerHost[i]);
                if (time != -1) {
                    mTimeDif.onSuccess(time - System.currentTimeMillis());
                    return;
                }
                if (i == ntpServerHost.length - 1) {
                    mTimeDif.onSuccess(null);
                }
            }
            mTimeDif.onSuccess(null);
        } catch (Throwable e) {
            mTimeDif.onSuccess(null);
            e.printStackTrace();
        }
    }
}