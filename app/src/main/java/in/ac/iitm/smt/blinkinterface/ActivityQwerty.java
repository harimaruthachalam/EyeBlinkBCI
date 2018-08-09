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
import java.util.List;
import java.util.Locale;

public class ActivityQwerty extends AppCompatActivity {

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
    TextView[] leftTV;
    TextView[] rightTV;
    TextView centerTV;
    TextView[] suggTV;
    String strBlankKeyboard;
    int currentSuggestion = -1;
    String lines[];
    TextToSpeech tts;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qwerty);
        leftTV = new TextView[18];
        leftTV[0] = findViewById(R.id.textViewL1);
        leftTV[1] = findViewById(R.id.textViewL2);
        leftTV[2] = findViewById(R.id.textViewL3);
        leftTV[3] = findViewById(R.id.textViewL4);
        leftTV[4] = findViewById(R.id.textViewL5);
        leftTV[5] = findViewById(R.id.textViewL6);
        leftTV[6] = findViewById(R.id.textViewL7);
        leftTV[7] = findViewById(R.id.textViewL8);
        leftTV[8] = findViewById(R.id.textViewL9);
        leftTV[9] = findViewById(R.id.textViewL10);
        leftTV[10] = findViewById(R.id.textViewL11);
        leftTV[11] = findViewById(R.id.textViewL12);
        leftTV[12] = findViewById(R.id.textViewL13);
        leftTV[13] = findViewById(R.id.textViewL14);
        leftTV[14] = findViewById(R.id.textViewL15);
        leftTV[15] = findViewById(R.id.textViewL16);
        leftTV[16] = findViewById(R.id.textViewL17);
        leftTV[17] = findViewById(R.id.textViewL18);

        rightTV = new TextView[18];
        rightTV[0] = findViewById(R.id.textViewR1);
        rightTV[1] = findViewById(R.id.textViewR2);
        rightTV[2] = findViewById(R.id.textViewR3);
        rightTV[3] = findViewById(R.id.textViewR4);
        rightTV[4] = findViewById(R.id.textViewR5);
        rightTV[5] = findViewById(R.id.textViewR6);
        rightTV[6] = findViewById(R.id.textViewR7);
        rightTV[7] = findViewById(R.id.textViewR8);
        rightTV[8] = findViewById(R.id.textViewR9);
        rightTV[9] = findViewById(R.id.textViewR10);
        rightTV[10] = findViewById(R.id.textViewR11);
        rightTV[11] = findViewById(R.id.textViewR12);
        rightTV[12] = findViewById(R.id.textViewR13);
        rightTV[13] = findViewById(R.id.textViewR14);
        rightTV[14] = findViewById(R.id.textViewR15);
        rightTV[15] = findViewById(R.id.textViewR16);
        rightTV[16] = findViewById(R.id.textViewR17);

        centerTV = findViewById(R.id.textViewCenter);

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

        movingAverageSignal = new MovingAverage(movingAveragePeriod);
        signalArray = new ArrayList();
        toStop = getApplicationContext().getResources().getBoolean(R.bool.toStop);
        blinkInterval = getApplicationContext().getResources().getInteger(R.integer.blinkInterval);

        blinkTimer = new CountDownTimer(blinkInterval, blinkInterval) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if (blinkCount == 6) {
                    tgStreamReader.stop();
                    tgStreamReader.close();
                    Intent intent = new Intent(ActivityQwerty.this, ActivityMenu.class);
                    startActivity(intent);
                }

                switch (currentState) {
                    case 0:
                        // In keyboard
                        ArrayList<ArrayList<String>> characters = getCharacters();
                        ArrayList<String> temp;
                        switch (blinkCount) {
                            case 2:
                                // Choose left
                                temp = characters.get(0);
                                if (temp.size() == 1) {
                                    autoCompleteTextView.append(temp.get(0));
                                    showSuggestions();
                                    currentState = 1;
                                } else if (temp.size() != 0) {
                                    setCharacters(temp);
                                }
                                break;
                            case 3:
                                // Choose right
                                temp = characters.get(2);
                                if (temp.size() == 1) {
                                    autoCompleteTextView.append(temp.get(0));
                                    showSuggestions();
                                    currentState = 1;
                                } else if (temp.size() != 0) {
                                    setCharacters(temp);
                                }
                                break;
                            case 4:
                                // Fix mid
                                autoCompleteTextView.append(characters.get(1).get(0));
                                showSuggestions();
                                currentState = 1;
//                                resetKeyboard();
                                // state = 1
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

    public ArrayList<ArrayList<String>> getCharacters() {

        ArrayList<ArrayList<String>> arrayList = new ArrayList<>();
        ArrayList<String> leftAL = new ArrayList<>();
        ArrayList<String> rightAL = new ArrayList<>();
        ArrayList<String> center = new ArrayList<>();

        int i = 0;
        // For the 18 Left Characters
        while (i < 18 && !leftTV[i].getText().toString().equals(strBlankKeyboard)) {
            leftAL.add(leftTV[i].getText().toString());
            i++;
        }

        //For the single Character at the center
        if (!centerTV.getText().toString().equals(strBlankKeyboard)) {
            center.add(centerTV.getText().toString());
        }

        i = 0;
        // For the 17 Right Characters
        while (i < 17 && !rightTV[i].getText().toString().equals(strBlankKeyboard)) {
            rightAL.add(rightTV[i].getText().toString());
            i++;
        }

        arrayList.add(leftAL);
        arrayList.add(center);
        arrayList.add(rightAL);

        return (arrayList);
    }

    public void setCharacters(ArrayList<String> arrayList) {

        int len = arrayList.size();
        int mid = len / 2;
        int count = 0;
        ArrayList<String> leftAL = new ArrayList(arrayList.subList(0, mid));
        ArrayList<String> rightAL = new ArrayList(arrayList.subList(mid + 1, arrayList.size()));
        int i;
        // For the 18 Left Characters
        for (i = 0; i < 18; i++) {
            if (leftAL.size() != 0 && count < leftAL.size()) {
                leftTV[i].setText(leftAL.get(count));
                count++;
            } else {
                leftTV[i].setText(strBlankKeyboard);
            }
        }

        centerTV.setText(arrayList.get(mid));
        count = 0;
        // For the 17 right Characters
        for (i = 0; i < 17; i++) {
            if (rightAL.size() != 0 && count < rightAL.size()) {
                rightTV[i].setText(rightAL.get(count));
                count++;
            } else {
                rightTV[i].setText(strBlankKeyboard);
            }
        }

    }

    public void resetKeyboard() {

        leftTV[0].setText(R.string.tvL1);
        leftTV[1].setText(R.string.tvL2);
        leftTV[2].setText(R.string.tvL3);
        leftTV[3].setText(R.string.tvL4);
        leftTV[4].setText(R.string.tvL5);
        leftTV[5].setText(R.string.tvL6);
        leftTV[6].setText(R.string.tvL7);
        leftTV[7].setText(R.string.tvL8);
        leftTV[8].setText(R.string.tvL9);
        leftTV[9].setText(R.string.tvL10);
        leftTV[10].setText(R.string.tvL11);
        leftTV[11].setText(R.string.tvL12);
        leftTV[12].setText(R.string.tvL13);
        leftTV[13].setText(R.string.tvL14);
        leftTV[14].setText(R.string.tvL15);
        leftTV[15].setText(R.string.tvL16);
        leftTV[16].setText(R.string.tvL17);
        leftTV[17].setText(R.string.tvL18);

        rightTV[0].setText(R.string.tvR1);
        rightTV[1].setText(R.string.tvR2);
        rightTV[2].setText(R.string.tvR3);
        rightTV[3].setText(R.string.tvR4);
        rightTV[4].setText(R.string.tvR5);
        rightTV[5].setText(R.string.tvR6);
        rightTV[6].setText(R.string.tvR7);
        rightTV[7].setText(R.string.tvR8);
        rightTV[8].setText(R.string.tvR9);
        rightTV[9].setText(R.string.tvR10);
        rightTV[10].setText(R.string.tvR11);
        rightTV[11].setText(R.string.tvR12);
        rightTV[12].setText(R.string.tvR13);
        rightTV[13].setText(R.string.tvR14);
        rightTV[14].setText(R.string.tvR15);
        rightTV[15].setText(R.string.tvR16);
        rightTV[16].setText(R.string.tvR17);

        centerTV.setText(R.string.tvC);
    }

    public void resetSuggestionColor() {
        for (int i = 0; i < 5; i++) {
            suggTV[i].setTextColor(getResources().getColor(R.color.colorAccentMild));
        }
    }

    public void showSuggestions() {
        resetSuggestionColor();
        String c = autoCompleteTextView.getText().toString();
        int count = 0;
        int i = 0;
        while (count != 5 && i != lines.length) {

            if (lines[i].startsWith(c)) {
                suggTV[count].setText(lines[i]);

                count++;
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
        ActivityQwerty.this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), msg, timeStyle).show();
            }

        });
    }
}