package in.ac.iitm.smt.blinkinterface;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.DataType.MindDataType;
import com.neurosky.connection.EEGPower;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ActivityMain extends AppCompatActivity {
    private static final int MSG_UPDATE_BAD_PACKET = 1001;
    private static final int MSG_UPDATE_STATE = 1002;
    private static final int MSG_CONNECT = 1003;
    Handler myHandler;
    MovingAverage movingAverageSignal;
    int movingAveragePeriod;
    int threshold;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean toStop;
    private String address = null;
    private TgStreamReader tgStreamReader;
    private int currentState = 0;
    private TextView tvRawData;
    private boolean isPressing = false;
    private boolean isReadFilter = false;
    private int badPacketCount = 0;
    private boolean isConnected = false;
    private List<Integer> signalArray;
    private Handler LinkDetectedHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case 1234:
                    tgStreamReader.MWM15_getFilterType();
                    isReadFilter = true;

                    break;
                case 1235:
                    tgStreamReader.MWM15_setFilterType(MindDataType.FilterType.FILTER_60HZ);
                    LinkDetectedHandler.sendEmptyMessageDelayed(1237, 1000);
                    break;
                case 1236:
                    tgStreamReader.MWM15_setFilterType(MindDataType.FilterType.FILTER_50HZ);
                    LinkDetectedHandler.sendEmptyMessageDelayed(1237, 1000);
                    break;

                case 1237:
                    tgStreamReader.MWM15_getFilterType();
                    break;

                case MindDataType.CODE_FILTER_TYPE:
                    if (isReadFilter) {
                        isReadFilter = false;
                        if (msg.arg1 == MindDataType.FilterType.FILTER_50HZ.getValue()) {
                            LinkDetectedHandler.sendEmptyMessageDelayed(1235, 1000);
                        } else if (msg.arg1 == MindDataType.FilterType.FILTER_60HZ.getValue()) {
                            LinkDetectedHandler.sendEmptyMessageDelayed(1236, 1000);
                        } else {
                        }
                    }

                    break;


                case MindDataType.CODE_RAW:
                    movingAverageSignal.add(msg.arg1);
                    signalArray.add(movingAverageSignal.getAvg());


//                    File folder = new File("/sdcard/demo");
//                    folder.mkdirs();
//                    File file = new File("/sdcard/demo/demotext.txt");
//                    try {
//                        FileOutputStream fileinput = new FileOutputStream(file, true);
//                        PrintStream printstream = new PrintStream(fileinput);
//                        printstream.print(Integer.toString(msg.arg1) + "\n");
//                        fileinput.close();
//                    }
//                    catch (Exception e)
//                    {
//                        showToast("Text Could not be added",Toast.LENGTH_SHORT);
//                    }
                    tvRawData.setText(Integer.toString(msg.arg1));

                    break;
                case MindDataType.CODE_MEDITATION:

                    break;
                case MindDataType.CODE_ATTENTION:

                    break;
                case MindDataType.CODE_EEGPOWER:
                    EEGPower power = (EEGPower) msg.obj;
                    if (power.isValidate()) {

                    }
                    break;
                case MindDataType.CODE_POOR_SIGNAL://
                    int poorSignal = msg.arg1;


                    break;
                case MSG_UPDATE_BAD_PACKET:


                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };
    private TgStreamHandler callback = new TgStreamHandler() {

        @Override
        public void onStatesChanged(int connectionStates) {
            // TODO Auto-generated method stub
            currentState = connectionStates;
            switch (connectionStates) {
                case ConnectionStates.STATE_CONNECTED:
                    showToast("Connected", Toast.LENGTH_SHORT);
                    isConnected = true;
                    break;
                case ConnectionStates.STATE_WORKING:
                    isConnected = true;
                    LinkDetectedHandler.sendEmptyMessageDelayed(1234, 1000);
                    break;
                case ConnectionStates.STATE_GET_DATA_TIME_OUT:
                    //get data time out
                    isConnected = false;
                    break;
                case ConnectionStates.STATE_COMPLETE:
                    //read file complete
                    break;
                case ConnectionStates.STATE_STOPPED:
                    isConnected = false;
                    break;
                case ConnectionStates.STATE_DISCONNECTED:
                    isConnected = false;
                    break;
                case ConnectionStates.STATE_ERROR:
                    isConnected = false;
                    showToast("Connect error, Please try again!", Toast.LENGTH_LONG);
                    if (toStop) {
                        finish();
                        return;
                    }
                    break;
                case ConnectionStates.STATE_FAILED:
                    isConnected = false;
                    showToast("Connection failed, Please try again!", Toast.LENGTH_LONG);
                    if (toStop) {
                        finish();
                        return;
                    }
                    break;
            }
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = MSG_UPDATE_STATE;
            msg.arg1 = connectionStates;
            LinkDetectedHandler.sendMessage(msg);
        }

        @Override
        public void onRecordFail(int a) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onChecksumFail(byte[] payload, int length, int checksum) {
            // TODO Auto-generated method stub

            badPacketCount++;
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = MSG_UPDATE_BAD_PACKET;
            msg.arg1 = badPacketCount;
            LinkDetectedHandler.sendMessage(msg);

        }

        @Override
        public void onDataReceived(int datatype, int data, Object obj) {
            // TODO Auto-generated method stub
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = datatype;
            msg.arg1 = data;
            msg.obj = obj;
            LinkDetectedHandler.sendMessage(msg);
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        movingAveragePeriod = getApplicationContext().getResources().getInteger(R.integer.movingAveragePeriod);

        movingAverageSignal = new MovingAverage(movingAveragePeriod);

        tvRawData = findViewById(R.id.tvRawData);
        signalArray = new ArrayList();

        myHandler = new Handler();

        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                showToast(
                        "Please enable your Bluetooth and re-start this app !",
                        Toast.LENGTH_LONG);
                toStop = getApplicationContext().getResources().getBoolean(R.bool.toStop);
                if (toStop) {
                    finish();
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        showToast("Initiated", Toast.LENGTH_SHORT);

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals("MindWave Mobile")) {
                address = device.getAddress();
                showToast(
                        "'MindWave Mobile' is in paired devices !",
                        Toast.LENGTH_SHORT);
            }
        }
        if (address == null) {
            showToast(
                    "Pair with the 'MindWave Mobile' and try again !",
                    Toast.LENGTH_LONG);
            if (toStop) {
                finish();
                return;
            }
        }

        BluetoothDevice bd = mBluetoothAdapter.getRemoteDevice(address);
        createStreamReader(bd);

        showToast("Streaming Initiation !", Toast.LENGTH_SHORT);

        tgStreamReader.connectAndStart();

        while (!isConnected) {

        }

        showToast(this.getResources().getString(R.string.tvSingleblinkLbl), Toast.LENGTH_SHORT);

        showToast(this.getResources().getString(R.string.tvDoubleblinkLbl), Toast.LENGTH_LONG);

        showToast(this.getResources().getString(R.string.tvTripleblinkLbl), Toast.LENGTH_LONG);

        showToast(this.getResources().getString(R.string.tvTripleblinkLbl), Toast.LENGTH_SHORT);

        myHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

//                Intent intent = new Intent(ActivityMain.this, ActivityQwerty.class);
//                Intent intent = new Intent(ActivityMain.this, ActivityT9.class);
//                Intent intent = new Intent(ActivityMain.this, ActivityRawSignal.class);
                Intent intent = new Intent(ActivityMain.this, TempLauncher.class);
//                Intent intent = new Intent(ActivityMain.this, ActivityMenu.class);


                threshold = Statistics.getMean(signalArray) + Statistics.getStdDev(signalArray) * 2;

//                String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
//
//                String path = "/sdcard/demo/MA_" + timeStamp + ".txt";
//
//                File folder = new File("/sdcard/demo");
//                folder.mkdirs();
//                File file = new File(path);
//                try {
//                    FileOutputStream fileinput = new FileOutputStream(file, true);
//                    PrintStream printstream = new PrintStream(fileinput);
//                    for (int i : signalArray) {
//                        printstream.print(Integer.toString(i) + "\n");
//                    }
//                    fileinput.close();
//                }
//                catch (Exception e)
//                {
//                    showToast("Text Could not be added",Toast.LENGTH_SHORT);
//                }
                String msg = "Estimated Threshold is " + threshold;
                showToast(msg, Toast.LENGTH_SHORT);
                if (threshold > 0) {
                    Config.threshold = threshold;
                    Config.address = address;
                }
                tgStreamReader.stop();
                tgStreamReader.close();
                startActivity(intent);

            }
        }, 20000);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tgStreamReader.stop();
        tgStreamReader.close();
    }

    public TgStreamReader createStreamReader(BluetoothDevice bd) {

        if (tgStreamReader == null) {
            // Example of constructor public TgStreamReader(BluetoothDevice mBluetoothDevice,TgStreamHandler tgStreamHandler)
            tgStreamReader = new TgStreamReader(bd, callback);
            tgStreamReader.startLog();
        } else {
            // (1) Demo of changeBluetoothDevice
            tgStreamReader.changeBluetoothDevice(bd);

            // (4) Demo of setTgStreamHandler, you can change the data handler by this function
            tgStreamReader.setTgStreamHandler(callback);
        }

        return tgStreamReader;
    }

    public void showToast(final String msg, final int timeStyle) {
        ActivityMain.this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), msg, timeStyle).show();
            }

        });
    }
}
