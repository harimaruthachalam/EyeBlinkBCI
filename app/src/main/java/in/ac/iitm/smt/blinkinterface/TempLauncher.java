package in.ac.iitm.smt.blinkinterface;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.Locale;
import java.util.Queue;

public class TempLauncher extends AppCompatActivity {

    private static final int MSG_UPDATE_BAD_PACKET = 1001;
    private static final int MSG_UPDATE_STATE = 1002;
    private static final int MSG_CONNECT = 1003;
    public boolean withinTime = false;
    public int blinkCount = 0;
    int threshold;
    MovingAverage movingAverageSignal;
    int movingAveragePeriod;
    ImageView ivRedSignal;
    CountDownTimer blinkTimer;
    CountDownTimer highlightTimer;
    int currentHighlight = 0;
    boolean isJustCrossedThreshold = false;
    TextView autoCompleteTextView;
    EditText accuEditText;
    String strBlankKeyboard;
    ImageView[] iv;
    TextView[] suggTV;
    int currentSuggestion = -1;
    String lines[];
    TextToSpeech tts;
    T9 T9Communicator;
    StringBuilder digitSequence = new StringBuilder("");
    String response = "";
    Button btnBackspace;
    private String address = null;
    private TgStreamReader tgStreamReader;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean isPressing = false;
    private boolean isReadFilter = false;
    private List<Integer> signalArray;
    private int badPacketCount = 0;
    private int currentState = 0;
    private boolean isConnected = false;
    private boolean toStop;
    private int blinkInterval;
    private int highlightInterval;
    private Handler LinkDetectedHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case 1234:
                    tgStreamReader.MWM15_getFilterType();
                    isReadFilter = true;
                    //Log.d(TAG,"MWM15_getFilterType ");

                    break;
                case 1235:
                    tgStreamReader.MWM15_setFilterType(MindDataType.FilterType.FILTER_60HZ);
                    //Log.d(TAG,"MWM15_setFilter  60HZ");
                    LinkDetectedHandler.sendEmptyMessageDelayed(1237, 1000);
                    break;
                case 1236:
                    tgStreamReader.MWM15_setFilterType(MindDataType.FilterType.FILTER_50HZ);
                    //Log.d(TAG,"MWM15_SetFilter 50HZ ");
                    LinkDetectedHandler.sendEmptyMessageDelayed(1237, 1000);
                    break;

                case 1237:
                    tgStreamReader.MWM15_getFilterType();
                    //Log.d(TAG,"MWM15_getFilterType ");

                    break;

                case MindDataType.CODE_FILTER_TYPE:
                    //Log.d(TAG,"CODE_FILTER_TYPE: " + msg.arg1 + "  isReadFilter: " + isReadFilter);
                    if (isReadFilter) {
                        isReadFilter = false;
                        if (msg.arg1 == MindDataType.FilterType.FILTER_50HZ.getValue()) {
                            LinkDetectedHandler.sendEmptyMessageDelayed(1235, 1000);
                        } else if (msg.arg1 == MindDataType.FilterType.FILTER_60HZ.getValue()) {
                            LinkDetectedHandler.sendEmptyMessageDelayed(1236, 1000);
                        } else {
                            // Else code goes here
                        }
                    }

                    break;


                case MindDataType.CODE_RAW:
//                    updateWaveView(msg.arg1);
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
//                    tvBlinkCount.setText("hh");
//                    signalArray.add(temp);


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
                    //Log.d(TAG, "HeadDataType.CODE_MEDITATION " + msg.arg1);

                    break;
                case MindDataType.CODE_ATTENTION:
                    //Log.d(TAG, "CODE_ATTENTION " + msg.arg1);

                    break;
                case MindDataType.CODE_EEGPOWER:
                    EEGPower power = (EEGPower) msg.obj;
                    if (power.isValidate()) {
                        // Validated code goes here
                    }
                    break;
                case MindDataType.CODE_POOR_SIGNAL://
                    int poorSignal = msg.arg1;
//                    showToast("Poor Signal !",Toast.LENGTH_SHORT);
                    //Log.d(TAG, "poorSignal:" + poorSignal);


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

    public void resetSuggestionColor() {
        for (int i = 0; i < 5; i++) {
            suggTV[i].setTextColor(getResources().getColor(R.color.colorAccentMild));
        }
    }

    public void setRemainingSuggestions(int current) {
//        resetSuggestionColor();
        String c = autoCompleteTextView.getText().toString();
        int i = 0;
        boolean isAlreadyShown;
        while (current != 5 && i != lines.length) {

            if (lines[i].startsWith(c)) {
                isAlreadyShown = false;
                for (int iter = 0; iter < current; iter++) {
                    if (suggTV[iter].getText().toString().equals(lines[i]) || autoCompleteTextView.getText().toString().equals(lines[i])) {
                        isAlreadyShown = true;
                        break;
                    }
                }
                if (!isAlreadyShown) {
                    suggTV[current].setText(lines[i]);
                    current++;
                }
            }
            i++;
        }
    }

    public void hideSuggestions() {
        for (int i = 0; i < 5; i++)
            suggTV[i].setText("");
    }

    public void playAccumatedWords() {

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                if (status == TextToSpeech.SUCCESS) {

                    int result = tts.setLanguage(Locale.US);

                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                        Log.e("TTS", "This Language is not supported");
                        showToast("This Language is not supported", Toast.LENGTH_SHORT);
                    } else {
                        tts.speak(accuEditText.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
                        accuEditText.setText("");
                    }

                } else {
//                    Log.e("TTS", "Initilization Failed!");
                    showToast("Initilization Failed!", Toast.LENGTH_SHORT);
                }

            }
        }

        );

    }

    private void setKey(String key) {
        digitSequence.append(key);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_t9_temp);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        iv = new ImageView[8];
        iv[0] = findViewById(R.id.iv2);
        iv[1] = findViewById(R.id.iv3);
        iv[2] = findViewById(R.id.iv4);
        iv[3] = findViewById(R.id.iv5);
        iv[4] = findViewById(R.id.iv6);
        iv[5] = findViewById(R.id.iv7);
        iv[6] = findViewById(R.id.iv8);
        iv[7] = findViewById(R.id.iv9);

        suggTV = new TextView[5];
        suggTV[0] = findViewById(R.id.tvSuggestion1);
        suggTV[1] = findViewById(R.id.tvSuggestion2);
        suggTV[2] = findViewById(R.id.tvSuggestion3);
        suggTV[3] = findViewById(R.id.tvSuggestion4);
        suggTV[4] = findViewById(R.id.tvSuggestion5);
        resetSuggestionColor();
        btnBackspace = findViewById(R.id.btnBackspace);

        T9Communicator = new T9();

        T9.makeDictionary(getResources().getStringArray(R.array.vocabularyMin));

        lines = getResources().getStringArray(R.array.vocabulary);
        autoCompleteTextView = findViewById(R.id.autoCompleteTextView);
        accuEditText = findViewById(R.id.accuEditText);
        ivRedSignal = findViewById(R.id.ivRedSignal);
        blinkInterval = getApplicationContext().getResources().getInteger(R.integer.blinkInterval);
        highlightInterval = getApplicationContext().getResources().getInteger(R.integer.highlightInterval);
        blinkTimer = new CountDownTimer(blinkInterval, blinkInterval) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if (blinkCount == 6) {
                    tgStreamReader.stop();
                    tgStreamReader.close();
                    Intent intent = new Intent(TempLauncher.this, ActivityMenu.class);
                    startActivity(intent);
                }

                switch (currentState) {
                    case 0:
                        // In keyboard
                        switch (blinkCount) {
                            case 2:
if(currentHighlight == 0){
    playAccumatedWords();
    currentState = 0;
    currentSuggestion = 0;

    highlightTimer.cancel();
    highlightTimer.start();
}else {
    setKey(Integer.toString(currentHighlight + 1));
    response = T9.getResponse(digitSequence.toString());
    if (response.isEmpty()) {
        showToast("Vocabulary not Found in Dictionary!", Toast.LENGTH_SHORT);
    } else {
        autoCompleteTextView.setText(response);
        currentState = 1;
        currentHighlight = 0;
        try {
            Queue<Word> words = T9.getWordQueue(digitSequence.toString());
            resetSuggestionColor();
            for (int i = 0; i < 5 && i < words.size(); i++) {

                if (i < words.size() - 1) {
                    suggTV[i].setText(words.toArray()[i + 1].toString());
                } else {
                    setRemainingSuggestions(i);
                    break;
                }
            }
        } catch (Exception e) {
            // Catch code
        }
    }
}

                                highlightTimer.cancel();
                                highlightTimer.start();
                                    break;
                        }
                        break;
                    case 1:
                        // In editView and list
                        switch (blinkCount) {
                            case 2:
                                if(currentHighlight == 0 || currentHighlight == 1){
                                    if (accuEditText.getText().length() == 0) {
                                        accuEditText.append(autoCompleteTextView.getText().toString());
                                    } else {
                                        accuEditText.append(" " + autoCompleteTextView.getText().toString());
                                    }
                                    digitSequence.delete(0, digitSequence.length());
                                    autoCompleteTextView.setText("");
                                    currentSuggestion = 0;
                                    currentState = 3;
                                    hideSuggestions();
                                }
                                else {
                                    if (accuEditText.getText().length() == 0) {
                                        accuEditText.append(suggTV[currentHighlight - 2].getText().toString());
                                    } else {
                                        accuEditText.append(" " + suggTV[currentHighlight - 2].getText().toString());
                                    }
                                    digitSequence.delete(0, digitSequence.length());
                                    autoCompleteTextView.setText("");
                                    currentSuggestion = 0;
                                    currentState = 3;
                                    hideSuggestions();
                                }

                                highlightTimer.cancel();
                                highlightTimer.start();
                                break;
                        }
                        break;
                    case 2:
                        // In backspace
                        switch (blinkCount) {
                            case 2:
                                if (accuEditText.getText().length() == 0) {
                                    accuEditText.append(suggTV[4].getText().toString());
                                } else {
                                    accuEditText.append(" " + suggTV[4].getText().toString());
                                }
                                digitSequence.delete(0, digitSequence.length());
                                autoCompleteTextView.setText("");
                                currentSuggestion = 0;
                                currentState = 3;
                                hideSuggestions();
                                highlightTimer.cancel();
                                highlightTimer.start();

                                break;
                        }
                        break;
                    case 3:
                        // In Accu
                        switch (blinkCount) {
                            case 2:
//                                playAccumatedWords();
//                                currentState = 0;
//                                currentSuggestion = 0;
//
//                                highlightTimer.cancel();
//                                highlightTimer.start();
                                if (autoCompleteTextView.getText().length() != 0) {
                                    autoCompleteTextView.setText(
                                            autoCompleteTextView.getText().toString().
                                                    substring(0, autoCompleteTextView.getText().length() - 1));
                                }
                                digitSequence.delete(digitSequence.length() - 1, digitSequence.length());
                                currentState = 0;
                                currentSuggestion = 0;
                                hideSuggestions();

                                highlightTimer.cancel();
                                highlightTimer.start();
                                break;
                        }
                        break;
                }

                blinkCount = 0;
                withinTime = false;
            }
        };
        highlightTimer = new CountDownTimer(highlightInterval, highlightInterval) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                changeHighlightedItem();
                highlightTimer.start();
            }
        };
        blinkCount = 0;
        blinkTimer.start();
        highlightTimer.start();

        movingAveragePeriod = getApplicationContext().getResources().getInteger(R.integer.movingAveragePeriod);
        movingAverageSignal = new MovingAverage(movingAveragePeriod);

        threshold = Config.threshold;
        address = Config.address;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice bd = mBluetoothAdapter.getRemoteDevice(address);
        createStreamReader(bd);
        tgStreamReader.connectAndStart();
    }

    void changeHighlightedItem(){

        iv[0].setImageResource(R.drawable.b2);
        iv[1].setImageResource(R.drawable.b3);
        iv[2].setImageResource(R.drawable.b4);
        iv[3].setImageResource(R.drawable.b5);
        iv[4].setImageResource(R.drawable.b6);
        iv[5].setImageResource(R.drawable.b7);
        iv[6].setImageResource(R.drawable.b8);
        iv[7].setImageResource(R.drawable.b9);
        autoCompleteTextView.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        btnBackspace.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        accuEditText.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        for(int i = 0; i < 5; i++)
            suggTV[i].setTextColor(getResources().getColor(R.color.colorPrimaryDark));
if (currentState == 0) {
    switch (currentHighlight) {
        case 0:
            iv[0].setImageResource(R.drawable.gr2);
            break;
        case 1:
            iv[1].setImageResource(R.drawable.gr3);
            break;
        case 2:
            iv[2].setImageResource(R.drawable.gr4);
            break;
        case 3:
            iv[3].setImageResource(R.drawable.gr5);
            break;
        case 4:
            iv[4].setImageResource(R.drawable.gr6);
            break;
        case 5:
            iv[5].setImageResource(R.drawable.gr7);
            break;
        case 6:
            iv[6].setImageResource(R.drawable.gr8);
            break;
        case 7:
            iv[7].setImageResource(R.drawable.gr9);
            break;
    }
    // Change the below line according to the length of text
    if(currentHighlight < 8)
        currentHighlight++;
    else {
        iv[0].setImageResource(R.drawable.gr2);
        currentHighlight = 1;
    }
//    currentHighlight = (currentHighlight + 1) % 9;
}else if(currentState == 1 && currentHighlight == 0) {
    autoCompleteTextView.setTextColor(getResources().getColor(R.color.colorHighlight));
    currentHighlight++;
}else if(currentState == 1) {
    suggTV[currentHighlight - 1].setTextColor(getResources().getColor(R.color.colorHighlight));
    if(currentHighlight == 5)
    {
        currentState++;
    }
    else
        currentHighlight++;
}
else if(currentState == 2) {
    btnBackspace.setTextColor(getResources().getColor(R.color.colorHighlight));
    currentState++;
//    currentHighlight = 1;
}
else if(currentState == 3) {
    accuEditText.setTextColor(getResources().getColor(R.color.colorHighlight));
    currentState = 0;
    currentHighlight = 0;
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
        TempLauncher.this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), msg, timeStyle).show();
            }

        });
    }
}

