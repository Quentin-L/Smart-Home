package android.intellhome;

import android.intellhome.entity.DeviceHistoryData;
import android.intellhome.services.RequestService;
import android.intellhome.utils.DateUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.util.List;

/**
 * Created by Quentin on 20/10/2016.
 */
public class DeviceHistoryController {

    public static final int METRIC_DAY = 1;
    public static final int METRIC_MONTH = 2;
    public static final int METRIC_YEAR = 3;

    public static final int REQUEST_SUCCESS = 1;
    public static final int REQUEST_FAILURE = 2;

    static String serverSN = "0000000000000107";

    private Handler handler;

    public DeviceHistoryController(Handler handler) {
        this.handler = handler;
    }


    // arg1: days of difference
    // arg2: metric
    // what: request status -> startDate
    // describe contents:  -> request status
    public void requestData(final String startDate, final String endDate) throws IllegalArgumentException {
        if (!checkDate(startDate, endDate))
            throw new IllegalArgumentException("end date is larger than current time");

        new Thread(new Runnable() {
            @Override
            public void run() {
                Message message = new Message();
                List<DeviceHistoryData> data = null;

                int numOfDays = DateUtil.calculateDateDiff(startDate, endDate);
                Bundle bundle = new Bundle();
                try {
                    data = RequestService.getHisDataList(serverSN, startDate, endDate);
                } catch (IOException e) {
                    e.printStackTrace();
                    message.what = REQUEST_FAILURE;
                    handler.sendMessage(message);
                    return;
                }

                if (numOfDays <= 30) {
                    bundle.putInt(DeviceActivity.METRIC, METRIC_DAY);
                    bundle.putInt(DeviceActivity.DAYS_OF_DIFFERENCE, numOfDays);
                }
                else if (numOfDays <= 365) {
                    bundle.putInt(DeviceActivity.METRIC, METRIC_MONTH);
                    bundle.putInt(DeviceActivity.DAYS_OF_DIFFERENCE, computeMonth(numOfDays));
                }

                else{
                    bundle.putInt(DeviceActivity.METRIC, METRIC_YEAR);
                    bundle.putInt(DeviceActivity.DAYS_OF_DIFFERENCE, computeYear(numOfDays));
                }


                message.obj = data;
                message.what = REQUEST_SUCCESS;
                handler.sendMessage(message);
            }
        });

    }

    // TODO: 21/10/2016 add criteria for endDate
    private boolean checkDate(String startDate, String endDate) {
        long endD = DateUtil.stringdate2Unixtime(endDate);
        return !(endD > System.currentTimeMillis());
    }

    private static int computeMonth(int nOfDays) {
        int n = nOfDays / 30;
        if (nOfDays % 30 > 0)
            ++n;
        return n;
    }

    private static int computeYear(int nOfDays) {
        int n = nOfDays / 365;
        if (nOfDays % 365 > 0)
            ++n;
        return n;
    }
}
