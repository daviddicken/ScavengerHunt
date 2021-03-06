package com.cinnamontoast.scavengerhunt.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import com.amplifyframework.api.graphql.model.ModelQuery;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.datastore.generated.model.Contact;
import com.amplifyframework.datastore.generated.model.Quest;
import com.cinnamontoast.scavengerhunt.R;
import com.cinnamontoast.scavengerhunt.adapters.ContactAdapter;
import com.cinnamontoast.scavengerhunt.adapters.QuestAdapter;

import java.util.ArrayList;

public class ParentProfileActivity extends AppCompatActivity implements QuestAdapter.QuestListFormatter, ContactAdapter.ContactListFormatter {

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;

    // TODO: figure out how to pass it an array of phone numbers
    // TODO: research how to pass the data for the recycler view. maybe as an intent? w/ putExtra

    String phoneNo = "+19496836174";
    String questId = "0";
    String smsEndpoint = "https://scavengerhuntstart.page.link/start?questID=";
    String message = smsEndpoint + questId;
    String[] numbers = {"12062519102","18649928355","15556842648","19815426325"};
    Button sendBtn;
    View lastView;


    // Quest ID will come from highlighting the Quest in the recycler view
    // contacts will come from highlighting the Contacts on the right side of the recycler
    // TODO: Where is the list data for the task items coming from?
//     - send the Quest ID --> quest ID Querys the data from AWS
    // TODO: How exactly to transport it

    Quest selectedQuest;
    RecyclerView questRecycler;
    RecyclerView playerRecycler;
    ArrayList<Quest> questOptions;
    ArrayList<Contact> playerOptions;
    ArrayList<Contact> selectedPlayers = new ArrayList<>();
    Handler questHandler;
    Handler playerHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_profile);

        setupButtons();

        sendBtn = (Button) findViewById(R.id.startquestBtn);

        // request & set SMS permissions for the app
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS},
                MY_PERMISSIONS_REQUEST_SEND_SMS);

        sendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                if (selectedQuest == null){
                    Toast.makeText(getApplicationContext(), "Please Select A Quest", Toast.LENGTH_LONG).show();
                } else {
                    // grab Quest Id
                    String getId = selectedQuest.getId();
                    setQuestId(getId); // TODO: pull this from the recycler view

                    if (checkPermission(Manifest.permission.SEND_SMS)){
                        sendSMSMessage();
                    }
                }
                Toast.makeText(getApplicationContext(), "SMS sent.", Toast.LENGTH_LONG).show();
            }
        });

        // Set up and fill list of available quests
        setupQuestRecycler();
        questHandler = createRecyclerHandler(questRecycler);
        queryUserQuests();

        // Set up and fill list of available players
        setupPlayerRecycler();
        playerHandler = createRecyclerHandler(playerRecycler);
        queryUserPlayers();
    }

    public void setupButtons (){
        Button createquest = findViewById(R.id.createquest);
        createquest.setOnClickListener(view -> this.startActivity(new Intent(this, CreateQuestActivity.class)));

        Button addplayer = findViewById(R.id.addplayer);
        addplayer.setOnClickListener(view -> this.startActivity(new Intent(this, ContactActivity.class)));

        ImageButton logoutButton = findViewById(R.id.logoutBtn);
        logoutButton.setOnClickListener(view -> {
            Amplify.Auth.signOut(
                    () -> Log.i("Amplify.logout", "successful logout"),
                    error -> Log.e("Amplify.logout", "logout unsuccessful")
            );
            this.startActivity(new Intent(this, MainActivity.class));
        });

        Switch addAll = findViewById(R.id.switch1);
        addAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                for(Contact contact : playerOptions){
                    selectedPlayers.add(contact);
                }
                findViewById(R.id.profilePlayerRecycler).setBackgroundColor(Color.LTGRAY);
            } else {
                selectedPlayers.clear();
                findViewById(R.id.profilePlayerRecycler).setBackgroundColor(Color.TRANSPARENT);
            }
        });
    }


//    ----- SEND SMS METHODS -----
    protected void sendSMSMessage() {

        if(selectedPlayers.size() > 0) {
            SmsManager sms = SmsManager.getDefault();
            //for (String number : numbers){
            for (Contact number : selectedPlayers) {
                sms.sendTextMessage(number.getPhoneNumber(), null, message, null, null);
                Log.i("----- DEEP LINK SMS ----", "DEEP LINK SENT to " + number + " message of " + message);
            }
        }else {
            Toast.makeText(getApplicationContext(), "No players have been selected.", Toast.LENGTH_LONG).show();
        }
    }

    public void setQuestId(String questId){
        this.questId = questId;
        this.message = smsEndpoint + questId;
    }

    public void setupQuestRecycler() {
        questOptions = new ArrayList<>();
        questRecycler = findViewById(R.id.profileQuestRecycler);
        questRecycler.setLayoutManager(new LinearLayoutManager(this));
        questRecycler.setAdapter(new QuestAdapter(questOptions, this));
    }

    public void setupPlayerRecycler() {
        playerOptions = new ArrayList<>();
        playerRecycler = findViewById(R.id.profilePlayerRecycler);
        playerRecycler.setLayoutManager(new LinearLayoutManager(this));
        playerRecycler.setAdapter(new ContactAdapter(playerOptions, this));
    }

    public Handler createRecyclerHandler(RecyclerView rView) {
        Handler handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                rView.getAdapter().notifyDataSetChanged();
                return false;
            }
        });
        return handler;
    }

    public void queryUserQuests() {
        Amplify.API.query(ModelQuery.list(Quest.class),
                response -> {
                    for (Quest quest : response.getData()) {
                        // TODO: only display Auth user's quests
                        if (quest.getUserId().equals(Amplify.Auth.getCurrentUser().getUserId())) {
                            questOptions.add(quest);
                        }
                    }
                    questHandler.sendEmptyMessage(1);
                },
                error -> {
                    Log.e("MyAmplify", "uh oh", error);
                });
    }

    public boolean checkPermission(String permission){
        int check = ContextCompat.checkSelfPermission(this,permission);
        return (check == PackageManager.PERMISSION_GRANTED);
    }

    public void queryUserPlayers() {
        Amplify.API.query(ModelQuery.list(Contact.class),
                response -> {
                    for (Contact contact : response.getData()) {
                        // TODO: only display Auth user's contacts
                        if (contact.getUserId().equals(Amplify.Auth.getCurrentUser().getUserId())) {
                            playerOptions.add(contact);
                        }
                    }
                    playerHandler.sendEmptyMessage(1);
                },
                error -> {
                    Log.e("MyAmplify", "uh oh", error);
                });
    }

    @Override
    public void questFormatter(Quest quest) {
        // on click
        // TODO: highlight selected quest fragment

        // save to global quest
        selectedQuest = quest;
        Log.i("MyAmplify.selectedQuest", selectedQuest.toString());
    }

    @Override
    public void questHighlighter(View questView, Quest quest) {
        if(lastView != null){
            lastView.setBackgroundColor(Color.TRANSPARENT);
        }
        lastView = questView;
        questView.setBackgroundColor(Color.LTGRAY);
    }

    @Override
    public void contactFormatter(Contact contact) {
        if (selectedPlayers.contains(contact)) {
            selectedPlayers.remove(contact);
        } else {
            selectedPlayers.add(contact);
        }
        Log.i("MyAmplify.playerOptions", selectedPlayers.toString());
    }

    @Override
    public void contactHighlighter(View contactView, Contact contact) {
        if(selectedPlayers.contains(contact)){
            contactView.setBackgroundColor(Color.TRANSPARENT);
        } else {
            contactView.setBackgroundColor(Color.LTGRAY);
        }
    }
}