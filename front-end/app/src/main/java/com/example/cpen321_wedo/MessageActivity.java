package com.example.cpen321_wedo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cpen321_wedo.Adapter.MessageAdapter;
import com.example.cpen321_wedo.Models.Chat;
import com.example.cpen321_wedo.Models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageActivity extends AppCompatActivity {

    CircleImageView profile_image;
    TextView username;
    FirebaseUser firebaseUser;
    DatabaseReference reference;

    ImageButton btn_send;
    EditText text_send;

    MessageAdapter messageAdapter;
    List<Chat> mchat;

    RecyclerView recyclerView;

    Intent intent;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setTitle("");
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//
//        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                finish();
//            }
//        });

        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);
        intent = getIntent();
        final String userid = intent.getStringExtra("userid");
        final boolean isGroupChat = intent.getExtras().getBoolean("isGroupChat");
        btn_send = findViewById(R.id.btn_send);
        text_send = findViewById(R.id.text_send);

        recyclerView = findViewById(R.id.recyclerview_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = text_send.getText().toString();
                if(!msg.equals("")){
                    if(isGroupChat){
                        sendMessageToGroup(firebaseUser.getUid(), userid, msg);
                    }else{
                        sendMessageToUser(firebaseUser.getUid(), userid, msg);
                    }
                }else{
                    Toast.makeText(MessageActivity.this, "You cannot send empty message", Toast.LENGTH_SHORT).show();
                }

                text_send.setText("");
            }
        });


        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(isGroupChat){
                    username.setText("Group Chat");
                    readMessageInGroupChat(userid);
                }else{
                    User user = snapshot.getValue(User.class);
                    username.setText(user.getUsername());
                    if(user.getImageURL().equals("default")){
                        profile_image.setImageResource(R.mipmap.ic_launcher);
                    }else{
                        Glide.with(MessageActivity.this).load(user.getImageURL()).into(profile_image);
                    }
                    readMessageOfPrivateChat(firebaseUser.getUid(), userid, user.getImageURL());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void setSupportActionBar(Toolbar toolbar) {
    }

    private void sendMessageToUser(String sender, String receiver, String message){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);

        reference.child("privateChats").push().setValue(hashMap);
    }

    private void readMessageOfPrivateChat(final String myid, final String userid, final String imageurl){
        mchat = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference("privateChats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshots) {
                mchat.clear();
                for(DataSnapshot snapshot: snapshots.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    if(chat.getReceiver().equals(myid) && chat.getSender().equals(userid) ||
                            chat.getReceiver().equals(userid) && chat.getSender().equals(myid)){
                        mchat.add(chat);
                    }

                    messageAdapter = new MessageAdapter(MessageActivity.this, mchat, imageurl, false);
                    recyclerView.setAdapter(messageAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendMessageToGroup(String sender, String taskListID, String message){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("message", message);

        reference.child("groupChats").child(taskListID).child("messages").push().setValue(hashMap);
    }

    private void readMessageInGroupChat(final String taskListID){
        mchat = new ArrayList<>();
        // TODO: I use "111" for now, don't forget to change it back later.
        reference = FirebaseDatabase.getInstance().getReference("groupChats").child("111").child("messages");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshots) {
                mchat.clear();
                for(DataSnapshot snapshot: snapshots.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    mchat.add(chat);

                    messageAdapter = new MessageAdapter(MessageActivity.this, mchat, "default", true);
                    recyclerView.setAdapter(messageAdapter);

//                    // TODO: for the image url I'll let it be default for a moment
//                    Query query = FirebaseDatabase.getInstance().getReference("Users").equalTo(chat.getSender(), "id");
//                    query.addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(DataSnapshot dataSnapshot) {
//
//                            if (dataSnapshot.exists()) {
//                                for (DataSnapshot issue : dataSnapshot.getChildren()) {
//                                    User user = issue.getValue(User.class);
//                                    messageAdapter = new MessageAdapter(MessageActivity.this, mchat, user.getImageURL());
//                                    recyclerView.setAdapter(messageAdapter);
//                                }
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(DatabaseError databaseError) {
//                            Log.d("test", databaseError.toString());
//                        }
//                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}