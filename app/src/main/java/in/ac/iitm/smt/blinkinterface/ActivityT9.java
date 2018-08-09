package in.ac.iitm.smt.blinkinterface;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public class ActivityT9 extends AppCompatActivity {

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
    boolean isJustCrossedThreshold = false;
    TextView autoCompleteTextView;
    EditText accuEditText;
    ArrayList<String> arrayAvailableKeys;
    ImageView[] imageViews;
    TextView[] suggTV;
    String strBlankKeyboard;
    int currentSuggestion = -1;
    String lines[];
    TextToSpeech tts;
    T9 T9Communicator;
    StringBuilder digitSequence = new StringBuilder("");
    String currentString = "";
    String response = "";
    TextView tvRaw;
    //    ArrayList<String> temp;
//    ArrayList<String> leftAL = new ArrayList<String> (arrayList.subList(0,mid));
//    ArrayList<String> rightAL = new ArrayList<String> (arrayList.subList(mid+1,arrayList.size()));
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
                            // Else part goes here
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
                    signalArray.add(temp);
                    tvRaw.setText(Integer.toString(temp));


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
                        // Validated code goes here...
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_t9);
        imageViews = new ImageView[8];
        imageViews[0] = findViewById(R.id.iv2);
        imageViews[1] = findViewById(R.id.iv3);
        imageViews[2] = findViewById(R.id.iv4);
        imageViews[3] = findViewById(R.id.iv5);
        imageViews[4] = findViewById(R.id.iv6);
        imageViews[5] = findViewById(R.id.iv7);
        imageViews[6] = findViewById(R.id.iv8);
        imageViews[7] = findViewById(R.id.iv9);
        arrayAvailableKeys = new ArrayList();
        resetKeyboard();

        suggTV = new TextView[5];
        suggTV[0] = findViewById(R.id.tvSuggestion1);
        suggTV[1] = findViewById(R.id.tvSuggestion2);
        suggTV[2] = findViewById(R.id.tvSuggestion3);
        suggTV[3] = findViewById(R.id.tvSuggestion4);
        suggTV[4] = findViewById(R.id.tvSuggestion5);
        resetSuggestionColor();

        lines = getResources().getStringArray(R.array.vocabulary);
        autoCompleteTextView = findViewById(R.id.autoCompleteTextView);
        accuEditText = findViewById(R.id.accuEditText);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        strBlankKeyboard = getApplicationContext().getResources().getString(R.string.strBlankKeyboard);

        movingAveragePeriod = getApplicationContext().getResources().getInteger(R.integer.movingAveragePeriod);

        ivRedSignal = findViewById(R.id.ivRedSignal);
        tvRaw = findViewById(R.id.tvRaw);

        movingAverageSignal = new MovingAverage(movingAveragePeriod);
        signalArray = new ArrayList();
        toStop = getApplicationContext().getResources().getBoolean(R.bool.toStop);
        blinkInterval = getApplicationContext().getResources().getInteger(R.integer.blinkInterval);

        T9Communicator = new T9();

        T9.makeDictionary(getResources().getStringArray(R.array.vocabularyMin));

        blinkTimer = new CountDownTimer(blinkInterval, blinkInterval) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if (blinkCount == 6) {
                    tgStreamReader.stop();
                    tgStreamReader.close();
                    Intent intent = new Intent(ActivityT9.this, ActivityMenu.class);
                    startActivity(intent);
                }

                switch (currentState) {
                    case 0:
                        // In keyboard
                        int mid = arrayAvailableKeys.size() / 2;
                        ArrayList<String> leftAK = new ArrayList(arrayAvailableKeys.subList(0, mid));
                        ArrayList<String> rightAK = new ArrayList(arrayAvailableKeys.subList(mid, arrayAvailableKeys.size()));
                        switch (blinkCount) {
                            case 2:
                                // Choose top or left
                                if (leftAK.size() == 1) {
//                                    currentState = 1;
                                    // Chosen key
                                    setKey(leftAK.get(0));
                                    response = T9.getResponse(digitSequence.toString());
                                    if (response.isEmpty()) {
                                        showToast("Vocabulary not Found in Dictionary!", Toast.LENGTH_SHORT);
                                    } else {
                                        autoCompleteTextView.setText(response);
                                        currentState = 1;

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
                                            //                                    showToast(words.peek().toString(),Toast.LENGTH_SHORT);
                                        } catch (Exception e) {
                                            // Catch code
                                        }
                                    }
                                    resetKeyboard();
                                } else {
                                    setInvalidKeys(rightAK);
                                    setValidKeys(leftAK);
                                }
                                break;
                            case 3:
                                // Choose bottom or right
                                if (rightAK.size() == 1) {
//                                    currentState = 1;
                                    // Chosen key
                                    setKey(rightAK.get(0));
                                    response = T9.getResponse(digitSequence.toString());
                                    if (response.isEmpty()) {
                                        showToast("Vocabulary not Found in Dictionary!", Toast.LENGTH_SHORT);
                                    } else {
                                        autoCompleteTextView.setText(response);
                                        currentState = 1;

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
                                            //                                    showToast(words.peek().toString(),Toast.LENGTH_SHORT);
                                        } catch (Exception e) {
                                            // Catch code
                                        }
                                    }
                                    resetKeyboard();
                                } else {
                                    setInvalidKeys(leftAK);
                                    setValidKeys(rightAK);
                                }
                                break;
                            case 4:
//                                showSuggestions();

//                                try {
//                                    Queue<Word> words = T9.getWordQueue(digitSequence.toString());
//                                    for (int i = 0; i < 5 && i < words.size(); i++){
//
//                                        if(!words.isEmpty())
//                                        {
//                                            suggTV[i].setText(T9.notThisWord(digitSequence.toString(), i+2));
//                                        }
//                                    }
////                                    showToast(words.peek().toString(),Toast.LENGTH_SHORT);
//                                }
//                                catch (Exception e){
//
//                                }
                                break;
                            case 5:
                                // Speak the accumalated
                                // reset the keyboard
                                playAccumatedWords();
                                break;
                        }
                        break;
                    case 1:
                        // In editView
                        switch (blinkCount) {
                            case 2:
                                // Go down in the list
                                // state = 2
                                currentState = 2;
                                currentSuggestion++;
                                resetSuggestionColor();
                                suggTV[currentSuggestion].setTextColor(getResources().getColor(R.color.colorHighlight));
                                break;
                            case 3:
                                // Continue the char accumalation
                                // state = 0
                                // reset the keyboard
                                currentState = 0;
                                resetKeyboard();
                                hideSuggestions();
                                currentSuggestion = -1;
                                break;
                            case 4:
                                // Remove the character
                                // state = 0
                                // reset the keyboard
                                if (autoCompleteTextView.getText().length() != 0) {
                                    autoCompleteTextView.setText(
                                            autoCompleteTextView.getText().toString().
                                                    substring(0, autoCompleteTextView.getText().length() - 1));
                                }
                                digitSequence.delete(0, digitSequence.length());
                                currentState = 0;
                                currentSuggestion = -1;
                                hideSuggestions();
                                resetKeyboard();
                                break;
                            case 5:
                                // Append the current word to the Accumalted words
                                // state = 0
                                // reset the keyboard
                                if (accuEditText.getText().length() == 0) {
                                    accuEditText.append(autoCompleteTextView.getText().toString());
                                } else {
                                    accuEditText.append(" " + autoCompleteTextView.getText().toString());
                                }
                                digitSequence.delete(0, digitSequence.length());
                                autoCompleteTextView.setText("");
                                currentSuggestion = -1;
                                currentState = 0;
                                hideSuggestions();
                                resetKeyboard();
                                break;
                        }
                        break;
                    case 2:
                        // In list
                        switch (blinkCount) {
                            case 2:
                                // Go down in the list
                                // If its last, focus on editView
                                // state = 1
                                currentSuggestion++;
                                resetSuggestionColor();
                                if (currentSuggestion != 5) {
                                    suggTV[currentSuggestion].setTextColor(getResources().getColor(R.color.colorHighlight));
                                } else {
                                    currentState = 1;
//                                    resetKeyboard();
                                    currentSuggestion = -1;
                                }
                                break;
                            case 3:
                                // Go up in the list
                                // If its first, focus on editView
                                // state = 1
                                currentSuggestion--;
                                resetSuggestionColor();
                                if (currentSuggestion != -1) {
                                    suggTV[currentSuggestion].setTextColor(getResources().getColor(R.color.colorHighlight));
                                } else {
                                    currentState = 1;
//                                    resetKeyboard();
                                    currentSuggestion = -1;
                                }
                                break;
                            case 4:
                                // Append the current word to the Accumalted words
                                // state = 0
                                // reset the keyboard
                                if (accuEditText.getText().length() == 0) {
                                    accuEditText.append(suggTV[currentSuggestion].getText().toString());
                                } else {
                                    accuEditText.append(" " + suggTV[currentSuggestion].getText().toString());
                                }
                                digitSequence.delete(0, digitSequence.length());
                                autoCompleteTextView.setText("");
                                currentSuggestion = -1;
                                currentState = 0;
                                hideSuggestions();
                                resetKeyboard();
                                break;
                            case 5:
                                // Speak the accumalated
                                // state = 0
                                // reset the keyboard
                                playAccumatedWords();
                                break;
                        }
                        break;
                }

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

    private void resetKeyboard() {
        imageViews[0].setImageResource(R.drawable.b2);
        imageViews[1].setImageResource(R.drawable.b3);
        imageViews[2].setImageResource(R.drawable.b4);
        imageViews[3].setImageResource(R.drawable.b5);
        imageViews[4].setImageResource(R.drawable.gr6);
        imageViews[5].setImageResource(R.drawable.gr7);
        imageViews[6].setImageResource(R.drawable.gr8);
        imageViews[7].setImageResource(R.drawable.gr9);
        arrayAvailableKeys.removeAll(arrayAvailableKeys);
        for (int i = 0; i < 8; i++) {
            arrayAvailableKeys.add(Integer.toString(i + 2));
        }
    }

    private void setInvalidKeys(ArrayList<String> arrayList) {
        if (arrayList.contains(Integer.toString(2)))
            imageViews[0].setImageResource(R.drawable.gy2);
        if (arrayList.contains(Integer.toString(3)))
            imageViews[1].setImageResource(R.drawable.gy3);
        if (arrayList.contains(Integer.toString(4)))
            imageViews[2].setImageResource(R.drawable.gy4);
        if (arrayList.contains(Integer.toString(5)))
            imageViews[3].setImageResource(R.drawable.gy5);
        if (arrayList.contains(Integer.toString(6)))
            imageViews[4].setImageResource(R.drawable.gy6);
        if (arrayList.contains(Integer.toString(7)))
            imageViews[5].setImageResource(R.drawable.gy7);
        if (arrayList.contains(Integer.toString(8)))
            imageViews[6].setImageResource(R.drawable.gy8);
        if (arrayList.contains(Integer.toString(9)))
            imageViews[7].setImageResource(R.drawable.gy9);
    }

    private void setValidKeys(ArrayList<String> arrayList) {
        arrayAvailableKeys = arrayList;
        int mid = arrayList.size() / 2;
        ArrayList<String> leftAK = new ArrayList(arrayList.subList(0, mid));
        ArrayList<String> rightAK = new ArrayList(arrayList.subList(mid, arrayList.size()));
        setLeftValidKeys(leftAK);
        setRightValidKeys(rightAK);
    }

    private void setLeftValidKeys(ArrayList<String> arrayList) {
        if (arrayList.contains(Integer.toString(2)))
            imageViews[0].setImageResource(R.drawable.b2);
        if (arrayList.contains(Integer.toString(3)))
            imageViews[1].setImageResource(R.drawable.b3);
        if (arrayList.contains(Integer.toString(4)))
            imageViews[2].setImageResource(R.drawable.b4);
        if (arrayList.contains(Integer.toString(5)))
            imageViews[3].setImageResource(R.drawable.b5);
        if (arrayList.contains(Integer.toString(6)))
            imageViews[4].setImageResource(R.drawable.b6);
        if (arrayList.contains(Integer.toString(7)))
            imageViews[5].setImageResource(R.drawable.b7);
        if (arrayList.contains(Integer.toString(8)))
            imageViews[6].setImageResource(R.drawable.b8);
        if (arrayList.contains(Integer.toString(9)))
            imageViews[7].setImageResource(R.drawable.b9);
    }

    private void setRightValidKeys(ArrayList<String> arrayList) {
        if (arrayList.contains(Integer.toString(2)))
            imageViews[0].setImageResource(R.drawable.gr2);
        if (arrayList.contains(Integer.toString(3)))
            imageViews[1].setImageResource(R.drawable.gr3);
        if (arrayList.contains(Integer.toString(4)))
            imageViews[2].setImageResource(R.drawable.gr4);
        if (arrayList.contains(Integer.toString(5)))
            imageViews[3].setImageResource(R.drawable.gr5);
        if (arrayList.contains(Integer.toString(6)))
            imageViews[4].setImageResource(R.drawable.gr6);
        if (arrayList.contains(Integer.toString(7)))
            imageViews[5].setImageResource(R.drawable.gr7);
        if (arrayList.contains(Integer.toString(8)))
            imageViews[6].setImageResource(R.drawable.gr8);
        if (arrayList.contains(Integer.toString(9)))
            imageViews[7].setImageResource(R.drawable.gr9);
    }

    private void setKey(String key) {
        digitSequence.append(key);
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

        //Log.d(Constants.CUSTOM_LOG_TYPE, "create steam reader");
        return tgStreamReader;
    }

    public void showToast(final String msg, final int timeStyle) {
        ActivityT9.this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), msg, timeStyle).show();
            }

        });
    }
}


class SequenceNotFoundException extends Exception {
    public String msg;

    public SequenceNotFoundException(String sequence) {
        msg = sequence + ": corresponding word not found in Dictionary";
    }

    @Override
    public String getMessage() {
        return msg;
    }
}


class Word {
    String words;
    int frequency;

    public Word(String word) {
        this.words = word;
        this.frequency = 1;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object) Overrided equals returns
     * true if the word matches false otherwise (frequency is ignored)
     */
    @Override
    public boolean equals(Object word) {
//		System.out.println("My compare function : "+((Word) this).words + " " + ((Word) word).words);
        return this.words.equals(((Word) word).words);
    }

    @Override
    public String toString() {
//		return this.words + " " + this.frequency;
        return this.words;
    }

}


class T9 {

    /**
     * T9 dictionary is the hash table having the digit seq mapped to words the
     * digit sequence is the key and the word tree is the value
     */
    public static HashMap<String, Queue<Word>> T9dictionary = new LinkedHashMap();

    /**
     * T9dicitionary is constructed.
     */
    public T9() {
//        makeDictionary();
//        System.out.println(T9dictionary);
    }

    /**
     * @param queries
     * @return response for the query
     * this method is only used when this code is run.
     */
    public static String respondQuery(String[] queries) {
        StringBuilder response = new StringBuilder();

        for (String eachQuery : queries) {
            response.append(getResponse(eachQuery) + " ");
        }
        return response.toString();
    }

    /**
     * @param query
     * @return
     * @throws SequenceNotFoundException converts the digit sequence input by the user into words and
     *                                   returns it
     */
    public static String getResponse(String query) {
        Queue<Word> WordQueue;
        try {
            WordQueue = getWordQueue(query);
        } catch (SequenceNotFoundException e) {
            System.out.println(e.getMessage());
            return "";
        }
        return (WordQueue.peek().toString().substring(0, query.length()));
    }

    /**
     * @param query
     * @return
     * @throws SequenceNotFoundException returns the wordQueue corresponding to the given Sequence
     */
    public static Queue<Word> getWordQueue(String query) throws SequenceNotFoundException {
        if (!T9dictionary.containsKey(query)) {
            Set<String> Keys = T9dictionary.keySet();
            for (String eachKey : Keys) {
                if (eachKey.startsWith(query)) {
                    System.out.println("in loop: " + eachKey);
                    return (T9dictionary.get(eachKey));
                }
            }
            throw (new SequenceNotFoundException(query));
        }
        return T9dictionary.get(query);
    }

    /**
     * @param eachQuery
     * @param mayBeThis
     * @throws SequenceNotFoundException
     * @returns Word In case the Word at the head of the tree is not the
     * expected one, on pressing * (T9GUI) this method gets Invoked and
     * based on the number of times * is pressed the tree returns a
     * different Word everytime
     */
    public static String notThisWord(String eachQuery, int mayBeThis) throws SequenceNotFoundException {
        Queue<Word> WordQueue = getWordQueue(eachQuery);
        List<Word> temporaryList = new ArrayList();

        int numberOfWords = WordQueue.size();
        int notThisWord = 1;
        System.out.println(numberOfWords + " " + mayBeThis + " " + (mayBeThis % numberOfWords));
        // till the tree is not empty and a different word is not encountered

        while ((!WordQueue.isEmpty())
                && (notThisWord != (mayBeThis % numberOfWords)) && (mayBeThis % numberOfWords != 0)) {
            temporaryList.add(WordQueue.remove());
            System.out.println("dict " + WordQueue);

            notThisWord = (notThisWord + 1) % numberOfWords;
        }

        String mayBeThisWord = WordQueue.peek().toString();
        WordQueue.addAll(temporaryList);
        return mayBeThisWord.substring(0, eachQuery.length());

    }

    /**
     * reads a dicitionary word by word adding the sequence to the T9 dictionary
     */
    public static void makeDictionary(String lines[]) {

        try {
//			FileInputStream fstream = new FileInputStream(
//					"../T9/Dictionary.txt");
            String sequence;
            for (String line : lines) {
                sequence = generateSeq(line.toLowerCase());
                insert(sequence, line);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * @param word
     * @returns an equivalent digit sequence to the word
     */
    public static String generateSeq(String word) {
        StringBuilder sequence = new StringBuilder();
        for (char c : word.toCharArray())
            sequence.append(getDigit(c));
        return sequence.toString();
    }

    /**
     * @param sequence
     * @param word     inserts the sequence into the T9dictionary if it is not there
     *                 already, updates the wordQueue with the new word or increments
     *                 the wodfrequencey if it already exists
     */
    public static void insert(String sequence, String word) {
        if (T9dictionary.containsKey(sequence)) {

            Queue<Word> wordQueue = T9dictionary.get(sequence);
            // System.out.println(wordQueue.contains(new Word(word)));
            if (wordQueue.contains(new Word(word))) {
                // System.out.println("Before adding: "+ wordQueue + " "+word);
                Word toUpdate = removeWord(wordQueue, new Word(word));
                // System.out.println("Removed Word: "+toUpdate +
                // " from : "+wordQueue);
                toUpdate.setFrequency(toUpdate.getFrequency() + 1);
                wordQueue.add(toUpdate);
                // System.out.println("After adding: "+ wordQueue + " "+word);
            } else {
                // System.out.println("Before adding: "+ wordQueue + " "+word);
                Word ne = new Word(word);
                wordQueue.add(ne);
                // System.out.println("After adding: "+ wordQueue);

            }
        } else {
            Queue<Word> wordQueue = new PriorityQueue(1,
                    new Comparator<Word>() {

                        public int compare(Word o1, Word o) {
                            if (o1.words.equals(o.words)) {
                                return 0;
                            } else if (o1.frequency < o.frequency) {
                                return 1;
                            } else {
                                return -1;
                            }
                        }

                    });
            wordQueue.add(new Word(word));
            T9dictionary.put(sequence, wordQueue);
        }
    }

    /**
     * @param wordQueue
     * @param word
     * @return Word removes the Word from the PriorityQueue and returns the
     * Word.
     */
    private static Word removeWord(Queue<Word> wordQueue, Word word) {
        for (Word eachWordIn : wordQueue) {
            if (eachWordIn.equals(word)) {
                wordQueue.remove(eachWordIn);
                return eachWordIn;
            }
        }
        // since the method is called only if the Word is present return null
        // statement never gets executed
        return null;
    }

    /**
     * @param alphabet
     * @return digit mapped to the corresponding character
     */
    public static char getDigit(char alphabet) {
        if (alphabet >= '0' && alphabet <= '9') {
            return alphabet;
        }
        switch (alphabet) {
            case 'a':
            case 'b':
            case 'c':
                return '2';
            case 'd':
            case 'e':
            case 'f':
                return '3';
            case 'g':
            case 'h':
            case 'i':
                return '4';
            case 'j':
            case 'k':
            case 'l':
                return '5';
            case 'm':
            case 'n':
            case 'o':
                return '6';
            case 'p':
            case 'q':
            case 'r':
            case 's':
                return '7';
            case 't':
            case 'u':
            case 'v':
                return '8';
            case 'w':
            case 'x':
            case 'y':
            case 'z':
                return '9';
            default:
                return '1';
        }
    }
}
