package in.clientside.chatbot.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import in.clientside.chatbot.R;
import in.clientside.chatbot.adapters.ChatAdapter;
import in.clientside.chatbot.helpers.Chat;

public class MainActivity extends AppCompatActivity implements AIListener, TextToSpeech.OnInitListener{
    private TextView txtSpeechInput;
    private ImageButton btnSpeak;
    List<Chat> chatList = new ArrayList<>();
    ArrayList<String> actionList = new ArrayList<>();
    String name;
    boolean flag;
    Result result;
    Button listenButton;
    private EditText editTextMessage;
    private RecyclerView recyclerView;
    private TextView resultTextView, messageText, queryText;
    private AIService aiService;
    private AIRequest aiRequest;
    private ChatAdapter mAdapter;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private String customerID;
    private TextToSpeech textToSpeech;
    RecyclerView.LayoutManager mLayoutManager;


    private static String TAG = "PermissionDemo";
    private static final int RECORD_REQUEST_CODE = 101;


    protected void makeRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                RECORD_REQUEST_CODE);
    }
    private TextWatcher textWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) {
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!editTextMessage.getText().toString().equals("")){
                listenButton.setText("Send");
            }
            else{
                listenButton.setText("Listen");
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.getSupportActionBar().hide();
        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setLanguage(Locale.ENGLISH);

        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            //Log.i(TAG, "Permission to record denied");
            makeRequest();
        }
        flag = false;

        listenButton = findViewById(R.id.listenButton);
        editTextMessage = findViewById(R.id.editText);
        recyclerView = findViewById(R.id.recycler_view);
        initializeActionList();
        editTextMessage.addTextChangedListener(textWatcher);

        mAdapter = new ChatAdapter(chatList);
        mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        final AIConfiguration config = new AIConfiguration("c1a8566f0e81483aaf5c51e9d8eeede4",
                AIConfiguration.SupportedLanguages.DEFAULT,
                AIConfiguration.RecognitionEngine.System);
        aiService = AIService.getService(this, config);
        aiService.setListener(this);
        aiRequest = new AIRequest();




        listenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listenButton.getText().equals("Listen")){
                    aiService.startListening();
                }
                else{
                    aiRequest.setQuery(editTextMessage.getText().toString());
                    Chat chat = new Chat(editTextMessage.getText().toString(), "me");
                    editTextMessage.setText("");
                    chatList.add(chat);
                    changeRecyclerView();
                    SendTextRequest sendTextRequest = new SendTextRequest();
                    sendTextRequest.execute(aiRequest);
                }
            }
        });
    }

    private class SendTextRequest extends AsyncTask<AIRequest, Void, AIResponse>{

        @Override
        protected AIResponse doInBackground(AIRequest... aiRequests) {
            final AIRequest aiRequest = aiRequests[0];
            AIResponse response;
            try {
                response = aiService.textRequest(aiRequest);
                return response;
            } catch (AIServiceException e) {
                System.out.println(""+e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(AIResponse aiResponse) {
            super.onPostExecute(aiResponse);
            MainActivity.this.callback(aiResponse);
        }
    }
    public void callback(AIResponse aiResponse){
        Toast.makeText(this, "here", Toast.LENGTH_SHORT).show();
        if (aiResponse!=null){
            String receivedMessage = aiResponse.getResult().getFulfillment().getSpeech();
            Chat chat = new Chat(receivedMessage, "him");
            chatList.add(chat);
            textToSpeech.speak(receivedMessage, TextToSpeech.QUEUE_FLUSH, null);
            changeRecyclerView();
        }
        else{
            Toast.makeText(this, "empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeActionList() {
        actionList.add("query.name");
        actionList.add("query.phone");
    }
    public void listenButtonOnClick() {
        aiService.startListening();
    }


    @Override
    public void onResult(AIResponse response) {

        result = response.getResult();
        // Get parameters
        String parameterString = "";
        if (result.getParameters() != null && !result.getParameters().isEmpty()) {
            for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {
                parameterString += "(" + entry.getKey() + ", " + entry.getValue() + ") ";
            }
        }
        String receivedMessage = result.getFulfillment().getSpeech();
        String sentMessage = result.getResolvedQuery();
        String action = result.getAction();
        Chat chat = new Chat(sentMessage, "me");
        chatList.add(chat);
        changeRecyclerView();
        textToSpeech.speak(receivedMessage, TextToSpeech.QUEUE_FLUSH, null);
        //resolveQuery(action);
        if (!actionList.contains(action)){
            chat = new Chat(receivedMessage, "him");
            chatList.add(chat);
            changeRecyclerView();
        }
    }


    private void changeRecyclerView(){
        mAdapter.notifyDataSetChanged();


        RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(this) {
            @Override protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_END;
            }
        };

        smoothScroller.setTargetPosition(mAdapter.getItemCount() - 1);
        mLayoutManager.startSmoothScroll(smoothScroller);




        /*recyclerView.post(new Runnable() {
            @Override
            public void run() {
                // Call smooth scroll
                recyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
            }
        });*/
    }


    @Override
    public void onInit(int status) {

    }











    @Override
    public void onError(AIError error) {
        //resultTextView.setText(error.toString());
        Toast.makeText(this, "Error" +error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    txtSpeechInput.setText(result.get(0));
                }
                break;
            }

        }
    }
    @Override
    protected void onDestroy() {


        //Close the Text to Speech Library
        if(textToSpeech != null) {

            textToSpeech.stop();
            textToSpeech.shutdown();
            Log.d(TAG, "TTS Destroyed");
        }
        super.onDestroy();
    }
}
