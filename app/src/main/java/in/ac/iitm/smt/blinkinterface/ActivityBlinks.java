package in.ac.iitm.smt.blinkinterface;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.DataType.MindDataType;
import com.neurosky.connection.EEGPower;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;

import java.util.ArrayList;
import java.util.List;

public class ActivityBlinks extends AppCompatActivity {
    private static final int MSG_UPDATE_BAD_PACKET = 1001;
    private static final int MSG_UPDATE_STATE = 1002;
    private static final int MSG_CONNECT = 1003;
    public boolean withinTime = false;
    public int blinkCount = 0;
    boolean isJustCrossedThreshold = false;
    int threshold;
    MovingAverage movingAverageSignal;
    int movingAveragePeriod;
    ImageView ivRedSignal;
    CountDownTimer blinkTimer;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean isPressing = false;
    private boolean isReadFilter = false;
    private int badPacketCount = 0;
    private int currentState = 0;
    private boolean isConnected = false;
    private boolean toStop;
    private int blinkInterval;
    private TextView tvBlinkCount;
    private TextView tvFinal;
    private String address = null;
    private TgStreamReader tgStreamReader;
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
                    int temp = movingAverageSignal.getAvg();
                    if (temp > threshold) {
                        if (withinTime) {
                            blinkTimer.cancel();
                            blinkTimer.start();
                            withinTime = true;
                            if (!isJustCrossedThreshold) {
                                blinkCount++;
                                isJustCrossedThreshold = true;
                            }
                        } else {
                            blinkTimer.start();
                            withinTime = true;
                            isJustCrossedThreshold = true;
                            blinkCount = 1;
                        }
                        ivRedSignal.setVisibility(View.VISIBLE);
                    } else {
                        isJustCrossedThreshold = false;
                        ivRedSignal.setVisibility(View.INVISIBLE);
                    }
                    tvBlinkCount.setText(Integer.toString(blinkCount));
                    signalArray.add(temp);


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

                    break;
                case MindDataType.CODE_MEDITATION:

                    break;
                case MindDataType.CODE_ATTENTION:

                    break;
                case MindDataType.CODE_EEGPOWER:
                    EEGPower power = (EEGPower) msg.obj;
                    if (power.isValidate()) {
                        // Validated signal
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
                    isConnected = true;
                    showToast("Connected", Toast.LENGTH_SHORT);
                    break;
                case ConnectionStates.STATE_WORKING:
                    isConnected = true;
                    LinkDetectedHandler.sendEmptyMessageDelayed(1234, 5000);
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
        setContentView(R.layout.activity_blinks);

        movingAveragePeriod = getApplicationContext().getResources().getInteger(R.integer.movingAveragePeriod);

        ivRedSignal = findViewById(R.id.ivRedSignal);
        tvBlinkCount = findViewById(R.id.tvBlinkCount);
        tvFinal = findViewById(R.id.tvFinal);

        movingAverageSignal = new MovingAverage(movingAveragePeriod);
        signalArray = new ArrayList();
        toStop = getApplicationContext().getResources().getBoolean(R.bool.toStop);
        blinkInterval = getApplicationContext().getResources().getInteger(R.integer.blinkInterval);

        blinkTimer = new CountDownTimer(blinkInterval, blinkInterval) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                tvFinal.setText("Final is " + Integer.toString(blinkCount));
                blinkCount = 0;
                withinTime = false;
            }
        };


        threshold = Config.threshold;
        address = Config.address;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice bd = mBluetoothAdapter.getRemoteDevice(address);
        createStreamReader(bd);
        tgStreamReader.connectAndStart();
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
        ActivityBlinks.this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), msg, timeStyle).show();
            }

        });
    }
}

