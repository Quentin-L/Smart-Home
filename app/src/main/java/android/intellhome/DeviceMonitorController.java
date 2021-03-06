package android.intellhome;

import android.intellhome.utils.CheckboxManager;
import android.os.Handler;
import android.util.Log;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Created by Quentin on 31/10/2016.
 */
public class DeviceMonitorController {

    static final String TAG = "DeviceMonitorController";

    private CheckboxManager checkboxManager;
    private LineData lineData;
    private Handler mHandler;

    private Map<Integer, QueueManager> dataManager;

    private RequestCurrentData<Integer> request;

    private ChartThread drawingThread;

    private int[] checked;
    private int checkedNum;

    public DeviceMonitorController(Handler handler, CheckboxManager checkboxManager, LineData lineData) {
        this.checkboxManager = checkboxManager;
        this.lineData = lineData;
        this.mHandler = handler;

        request = new RequestService(10);
    }

    public void toggleSwitch(boolean switchOn) {
        if (switchOn) switchTurnOn();
        else switchTurnOff();
    }

    // called when switch is turned off
    private void switchTurnOff() {

    }

    // called when switch is turned on
    private void switchTurnOn() {

    }

    public void startDrawing() {
        // first initialize data
        checked = checkboxManager.getChecked();
        checkedNum = checkboxManager.getCheckedNum();

        dataManager = new HashMap<>();
        LineDataSet[] dataSets = new LineDataSet[checkedNum];
        for (int i = 0; i < checkedNum; i++) {
            dataManager.put(checked[i], new QueueManager());
            dataSets[i] = new LineDataSet(dataManager.get(checked[i]).getEntries(), checkboxManager.getCorrespondingLabel(checked[i]));
        }

        // start to request data from server using the thread, one piece at every second
        drawingThread = new ChartThread();
        drawingThread.start();
    }

    public void stopDrawing() {
        drawingThread.interrupt();
        drawingThread = null;

        dataManager = null;
        checked = null;
        checkedNum = -1;
    }

    public void changeDrawing() {
//        synchronized (drawingThread) {
//            try {
//                drawingThread.wait();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }


//            Log.i(TAG, "changeDrawing: paused drawing thread");

//        drawingThread.onPause();

        synchronized (dataManager) {
            int[] oldChecked = checked;
            int oldNum = checkedNum;
            checked = checkboxManager.getChecked(); // newChecked
            checkedNum = checkboxManager.getCheckedNum(); // newCheckedNum

            int diff = checkboxManager.findDifferenceAndReturn(oldChecked);

            if (oldNum < checkedNum)
                dataManager.put(diff, new QueueManager());
            else
                dataManager.remove(diff);
        }

//        drawingThread.onResume();
//            drawingThread.notify();
//            Log.i(TAG, "changeDrawing: drawing thread changed");
//        }
    }


    private class QueueManager {
        private LinkedList<Entry> entries;

        public QueueManager() {

            entries = new LinkedList<>();
        }

        public List<Entry> getEntries() {
            return entries;
        }

        public void add(Entry entry) {
            if (entries.size() < GlobalConfig.MAX_ITEMS_TO_SHOW) {
                entries.add(entry);
            } else {
                entries.remove();
                entries.add(entry);
            }

            entriesDecrementByOne();
        }

        private void entriesDecrementByOne() {
            for (Entry entry : entries)
                entry.setX(entry.getX() - 1);
        }


    }

    // now adds one pieces of data each second by random
    // later request
    private class ChartThread extends Thread {

        private boolean run;
        private Object pauseLock;
        private boolean paused;

        public ChartThread() {
            pauseLock = new Object();
            paused = false;
            run = true;
        }

        @Override
        public synchronized void start() {
            super.start();
        }

        @Override
        public void run() {
            super.run();
            Log.i(TAG, "start: drawing thread running");

            while (run) {

                synchronized (dataManager) {
                    for (int i = 0; i < checkedNum; i++)
                        lineData.removeDataSet(0);

                    for (int i = 0; i < checkedNum; i++) {
                        int y = request.requestData();
                        dataManager.get(checked[i]).add(new Entry(GlobalConfig.MAX_ITEMS_TO_SHOW, y));
                        LineDataSet dataSet = new LineDataSet(dataManager.get(checked[i]).getEntries(), checkboxManager.getCorrespondingLabel(checked[i]));
                        dataSet.setColor(checkboxManager.getCorrespondingColor(checked[i]));
                        lineData.addDataSet(dataSet);
                    }

                    mHandler.sendEmptyMessage(DeviceMonitorActivity.HANDLER_UPDATE_CHART);
                }

//                 pause the thread when new queue is being added
//                synchronized (pauseLock) {
//                    while (paused) {
//                        try {
//                            pauseLock.wait();
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
            run = false;
        }

        public void onPause() {
            synchronized (pauseLock) {
                paused = true;
            }
        }

        public void onResume() {
            synchronized (pauseLock) {
                paused = false;
                pauseLock.notifyAll();
            }
        }

    }

    // for mocking
    private interface RequestCurrentData<T> {
        T requestData();
    }

    // for mocking also
    private class RequestService implements RequestCurrentData<Integer> {

        private int range;

        private Random random;

        RequestService(int range) {
            this.range = range;

            random = new Random();
        }


        @Override
        public Integer requestData() {
            return random.nextInt(range);
        }

        public void setRange(int range) {
            this.range = range;
        }
    }
}
