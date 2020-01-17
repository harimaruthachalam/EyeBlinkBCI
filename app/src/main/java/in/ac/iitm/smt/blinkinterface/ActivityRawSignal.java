    package in.ac.iitm.smt.blinkinterface;

    import android.bluetooth.BluetoothAdapter;
    import android.bluetooth.BluetoothDevice;
    import android.content.Context;
    import android.content.Intent;
    import android.content.res.Configuration;
    import android.graphics.Bitmap;
    import android.graphics.Canvas;
    import android.graphics.Color;
    import android.graphics.Paint;
    import android.graphics.Path;
    import android.graphics.PorterDuff;
    import android.graphics.PorterDuffXfermode;
    import android.os.Bundle;
    import android.os.CountDownTimer;
    import android.os.Handler;
    import android.os.Message;
    import android.support.v7.app.AppCompatActivity;
    import android.util.AttributeSet;
    import android.view.View;
    import android.view.ViewGroup;
    import android.view.Window;
    import android.view.WindowManager;
    import android.widget.LinearLayout;
    import android.widget.Toast;

    import com.neurosky.connection.ConnectionStates;
    import com.neurosky.connection.DataType.MindDataType;
    import com.neurosky.connection.EEGPower;
    import com.neurosky.connection.TgStreamHandler;
    import com.neurosky.connection.TgStreamReader;

    import java.nio.Buffer;
    import java.nio.ByteBuffer;
    import java.nio.IntBuffer;
    import java.util.ArrayList;
    import java.util.List;
    import java.io.*;
    import java.util.Set;

    public class ActivityRawSignal extends AppCompatActivity {
        private static final int MSG_UPDATE_BAD_PACKET = 1001;
        private static final int MSG_UPDATE_STATE = 1002;
        private static final int MSG_CONNECT = 1003;
        public boolean withinTime = false;
        public int blinkCount = 0;
        int threshold;
        MovingAverage movingAverageSignal;
        int movingAveragePeriod;
        CountDownTimer blinkTimer;
        boolean isJustCrossedThreshold = false;
        DrawWaveView waveView = null;
        private boolean isReadFilter = false;
        private String address = null;
        private TgStreamReader tgStreamReader;
        private BluetoothAdapter mBluetoothAdapter;
        private int badPacketCount = 0;
        private boolean isConnected = false;
        private boolean toStop;
        private int blinkInterval;
        private LinearLayout wave_layout;
        private char currentArtifact = '-';
        private List<Integer> blinkSignal = new ArrayList<Integer>();
        private List<Integer> turnSignal = new ArrayList<Integer>();
        private List<Integer> eyeBrowSignal = new ArrayList<Integer>();
        private List<Integer> nodSignal = new ArrayList<Integer>();
        private List<Integer> tempSignal = new ArrayList<Integer>();
        private String tempStr = null;
        private FileOutputStream blinkSignalFile = null;
        private FileOutputStream turnSignalFile = null;
        private Handler LinkDetectedHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {

                // Some business logic
                if (((int)(System.currentTimeMillis()/1000)) % 25 == 0)
                {
                    if (currentArtifact != 'B') {
                        showToast("Blink Start", Toast.LENGTH_SHORT);
                        currentArtifact = 'B';
                    }
                }
                if (((int)(System.currentTimeMillis()/1000)) % 25 == 4)
                {
                    if (currentArtifact != '-') {
                        showToast("Blink End", Toast.LENGTH_SHORT);
                        currentArtifact = '-';
                    }
                }
                if (((int)(System.currentTimeMillis()/1000)) % 25 == 6)
                {
                    if (currentArtifact != 'T') {
                        showToast("Turn Start", Toast.LENGTH_SHORT);
                        currentArtifact = 'T';
                    }
                }
                if (((int)(System.currentTimeMillis()/1000)) % 25 == 10)
                {
                    if (currentArtifact != '-') {
                        showToast("Turn End", Toast.LENGTH_SHORT);
                        currentArtifact = '-';
                    }
                }
                if (((int)(System.currentTimeMillis()/1000)) % 25 == 12)
                {
                    if (currentArtifact != 'N') {
                        showToast("Nod Start", Toast.LENGTH_SHORT);
                        currentArtifact = 'N';
                    }
                }
                if (((int)(System.currentTimeMillis()/1000)) % 25 == 16)
                {
                    if (currentArtifact != '-') {
                        showToast("Nod End", Toast.LENGTH_SHORT);
                        currentArtifact = '-';
                    }
                }
                if (((int)(System.currentTimeMillis()/1000)) % 25 == 18)
                {
                    if (currentArtifact != 'E') {
                        showToast("Eye brow Start", Toast.LENGTH_SHORT);
                        currentArtifact = 'E';
                    }
                }
                if (((int)(System.currentTimeMillis()/1000)) % 25 == 22)
                {
                    if (currentArtifact != '-') {
                        showToast("Eye brow End", Toast.LENGTH_SHORT);
                        currentArtifact = '-';
                    }
                }

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
                        // Code here
                        updateWaveView(msg.arg1);
                        movingAverageSignal.add(msg.arg1);
                        int temp = movingAverageSignal.getAvg();
                        // Business logic for storing the signal data
                        if(currentArtifact == 'B')
                            blinkSignal.add(temp);
                        if(currentArtifact == 'T')
                            turnSignal.add(temp);
                        if(currentArtifact == 'N')
                            nodSignal.add(temp);
                        if(currentArtifact == 'E')
                            eyeBrowSignal.add(temp);
                        if(currentArtifact == '-') {



//                            new Thread(new Runnable() {
//                                public void run() {
//
//
//
//                                    if (!blinkSignal.isEmpty()) {
//                                        tempStr = blinkSignal.toString();
//                                        blinkSignal.removeAll(blinkSignal);
//
//                                        try {
//                                            blinkSignalFile = new FileOutputStream("blink.bin",true);
//                                            BufferedOutputStream bufferedBlinkSignalFile=new BufferedOutputStream(blinkSignalFile);
//                                            bufferedBlinkSignalFile.write(tempStr.getBytes());
//
//                                            bufferedBlinkSignalFile.flush();
//                                            bufferedBlinkSignalFile.close();
//
//                                        } catch (Exception e) {
//                                        }
//
//                                        blinkSignal.removeAll(blinkSignal);
//                                    }
//
//
//                                    if (!turnSignal.isEmpty()) {
//
//                                        tempStr = turnSignal.toString();
//                                        turnSignal.removeAll(turnSignal);
//                                        try {
//                                            turnSignalFile = new FileOutputStream("turn.bin",true);
//                                            BufferedOutputStream bufferedTurnSignalFile=new BufferedOutputStream(turnSignalFile);
//                                            bufferedTurnSignalFile.write(tempStr.getBytes());
//
//                                            bufferedTurnSignalFile.flush();
//                                            bufferedTurnSignalFile.close();
//
//                                        } catch (Exception e) {
//                                        }
//
//                                    }
//
//
//
//                                }
//                            }).start();



//
//                            if (!blinkSignal.isEmpty()) {
//                                try {
//                                    blinkSignalFile = new FileOutputStream("blink.bin",true);
//                                    BufferedOutputStream bufferedBlinkSignalFile=new BufferedOutputStream(blinkSignalFile);
//                                    bufferedBlinkSignalFile.write(blinkSignal.toString().getBytes());
//
//                                    bufferedBlinkSignalFile.flush();
//                                    bufferedBlinkSignalFile.close();
//
//                                } catch (Exception e) {
//                                }
//
//                                blinkSignal.removeAll(blinkSignal);
//                            }
//
//
//                            if (!turnSignal.isEmpty()) {
//
//
//                                try {
//                                    turnSignalFile = new FileOutputStream("turn.bin",true);
//                                    BufferedOutputStream bufferedTurnSignalFile=new BufferedOutputStream(turnSignalFile);
//                                    bufferedTurnSignalFile.write(turnSignal.toString().getBytes());
//
//                                    bufferedTurnSignalFile.flush();
//                                    bufferedTurnSignalFile.close();
//
//                                } catch (Exception e) {
//                                }
//
//                                turnSignal.removeAll(turnSignal);
//                            }

                        }

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
    //            currentState  = connectionStates;
                switch (connectionStates) {
                    case ConnectionStates.STATE_CONNECTED:
                        //sensor.start();
                        isConnected = true;
                        showToast("Connected", Toast.LENGTH_SHORT);
                        break;
                    case ConnectionStates.STATE_WORKING:
                        //byte[] cmd = new byte[1];
                        //cmd[0] = 's';
                        //tgStreamReader.sendCommandtoDevice(cmd);
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
                //Log.i(TAG,"onDataReceived");

                //Log.d(Constants.CUSTOM_LOG_TYPE, "on data received");
            }

        };

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            setContentView(R.layout.activity_raw_signal);
            wave_layout = findViewById(R.id.wave_layout);

            movingAveragePeriod = getApplicationContext().getResources().getInteger(R.integer.movingAveragePeriod);
            movingAverageSignal = new MovingAverage(movingAveragePeriod);
            toStop = getApplicationContext().getResources().getBoolean(R.bool.toStop);
            blinkInterval = getApplicationContext().getResources().getInteger(R.integer.blinkInterval);
            setUpDrawWaveView();
            blinkTimer = new CountDownTimer(blinkInterval, blinkInterval) {
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {

                    if (blinkCount == 6) {
                        tgStreamReader.stop();
                        tgStreamReader.close();
                        Intent intent = new Intent(ActivityRawSignal.this, ActivityMenu.class);
                        startActivity(intent);
                    } else {
                        showToast(Integer.toString(blinkCount) + " Blinks detected", Toast.LENGTH_SHORT);
                    }

                    blinkCount = 0;
                    withinTime = false;
                }
            };





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
            tgStreamReader.connectAndStart();
        }

        @Override
        protected void onStart() {
            super.onStart();

        }

        public void setUpDrawWaveView() {

            waveView = new DrawWaveView(getApplicationContext());
            wave_layout.addView(waveView, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            waveView.setValue(9999, 9999, -9999);

        }

        public void updateWaveView(int data) {
            if (waveView != null) {
                waveView.updateData(data);
            }
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

            //Log.d(Constants.CUSTOM_LOG_TYPE, "create steam reader");
            return tgStreamReader;
        }

        public void showToast(final String msg, final int timeStyle) {
            ActivityRawSignal.this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), msg, timeStyle).show();
                }

            });
        }
    }

    class DrawWaveView extends View {
        public Paint paint = null;
        Bitmap cacheBitmap = null;
        Canvas cacheCanvas = null;
        Paint bmpPaint = new Paint();
        private Path path;
        private int maxPoint = 0;
        private int currentPoint = 0;
        private int maxValue = 0;
        private int minValue = 0;
        private float x = 0;
        private float y = 0;
        private float prex = 0;
        private float prey = 0;

        private int mBottom = 0;
        private int mHeight = 0;
        private int mLeft = 0;
        private int mWidth = 0;

        private float mPixPerHeight = 0;
        private float mPixPerWidth = 0;
        private boolean initFlag = false;

        public DrawWaveView(Context context) {
            super(context);
        }

        public DrawWaveView(Context context, AttributeSet attrs) {
            super(context, attrs);
            // TODO Auto-generated constructor stub


        }

        public void setValue(int maxPoint, int maxValue, int minValue) {
            this.maxPoint = maxPoint;
            this.maxValue = maxValue;
            this.minValue = minValue;
        }

        public void initView() {


            mBottom = this.getBottom();
            mWidth = this.getWidth();
            mLeft = this.getLeft();
            mHeight = this.getHeight();

            mPixPerHeight = (float) mHeight / (maxValue - minValue);
            mPixPerWidth = (float) mWidth / maxPoint;
            cacheBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
            cacheCanvas = new Canvas();
            path = new Path();
            cacheCanvas.setBitmap(cacheBitmap);

            paint = new Paint(Paint.DITHER_FLAG);
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);

            paint.setAntiAlias(true);
            paint.setDither(true);
            currentPoint = 0;
        }

        public void clear() {
            Paint clearPaint = new Paint();
            clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            cacheCanvas.drawPaint(clearPaint);
            clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            currentPoint = 0;
            path.reset();

            invalidate();
        }

        //Modification : PP-> return the xy coordinates back to the caller
        public void updateData(int data) {
            if (!initFlag) {
                //Modification : PP-> return empty arraylist to the caller in case the condition is not met.
                return;
            }
            y = translateData2Y(data);
            x = translatePoint2X(currentPoint);

            if (currentPoint == 0) {
                path.moveTo(x, y);
                currentPoint++;
                prex = x;
                prey = y;
            } else if (currentPoint == maxPoint) {
                cacheCanvas.drawPath(path, paint);
                currentPoint = 0;
            } else {
                path.quadTo(prex, prey, x, y);
                currentPoint++;
                prex = x;
                prey = y;
            }
            invalidate();
            if (currentPoint == 0) {
                clear();
            }
        }

        /**
         * y = top + height - (data -minValue) * height/(2*maxValue)
         *
         * @param data
         * @return
         */
        private float translateData2Y(int data) {
            return (float) mBottom - (data - minValue) * mPixPerHeight;
        }

        /**
         * x = mLeft + mWidth/
         *
         * @param point
         * @return
         */
        private float translatePoint2X(int point) {
            return (float) mLeft + point * mPixPerWidth;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // TODO Auto-generated method stub
            canvas.drawBitmap(cacheBitmap, 0, 0, bmpPaint);
            canvas.drawPath(path, paint);
            //super.onDraw(canvas);

        }

        @Override
        protected void onConfigurationChanged(Configuration newConfig) {
            // TODO Auto-generated method stub
            super.onConfigurationChanged(newConfig);
            initFlag = false;
        }

        // for rotate screen things
        @Override
        protected void onAttachedToWindow() {
            // TODO Auto-generated method stub
            super.onAttachedToWindow();
            initFlag = false;
        }

        // for rotate screen things
        @Override
        protected void onLayout(boolean changed, int left, int top, int right,
                                int bottom) {
            // TODO Auto-generated method stub
            super.onLayout(changed, left, top, right, bottom);
            if (!initFlag) {
                initView();
                initFlag = true;
            }
        }


    }
