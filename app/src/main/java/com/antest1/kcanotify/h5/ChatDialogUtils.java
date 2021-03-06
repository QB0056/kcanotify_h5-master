package com.antest1.kcanotify.h5;


import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class ChatDialogUtils {
    private Dialog dialog;
    private View inflate;
    private OkHttpClient mOkHttpClient;
    private List<ChatMsgObject> msgList;
    private WebSocket mWebSocket;
    private Gson gson = new Gson();
    private ChatAdapter chatAdapter;
    private Context context;
    private Handler mWebSocketHandler;
    private boolean connected;
    private final EditText msgEditText;
    private final Button sendBtn;
    private boolean connecting = false;
    private String showName;

    public ChatDialogUtils(Context context) {
        msgList = new ArrayList<>();
        this.context = context;
        showName = "游客 - 未登录";
        mWebSocketHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                ChatMsgObject chatMsgObject = (ChatMsgObject)msg.obj;
                msgList.add(chatMsgObject);
                if(msgList.size() > 100){
                    msgList.remove(0);
                }
                chatAdapter.notifyDataSetChanged();
                return false;
            }
        });
        inflate = LayoutInflater.from(context).inflate(R.layout.view_dialog_chat, null);
        dialog = new Dialog(context, R.style.DialogLeft);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(inflate);
        webSocketConnect();
        chatAdapter = new ChatAdapter(context, R.layout.view_chat_item, msgList);
        ListView chatMsgListView = dialog.findViewById(R.id.lv_main_msg);
        chatMsgListView.setAdapter(chatAdapter);

        msgEditText = dialog.findViewById(R.id.msg_text_view);
        sendBtn = dialog.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = msgEditText.getText().toString();
                if(msg != null && !msg.equals("") && mWebSocket != null){
                    ChatMsgObject chatMsgObject = new ChatMsgObject(showName, msg);
                    msgList.add(chatMsgObject);
                    if(msgList.size() > 100){
                        msgList.remove(0);
                    }
                    chatAdapter.notifyDataSetChanged();
                    mWebSocket.send(gson.toJson(chatMsgObject));
                    msgEditText.setText("");
                }
            }
        });

        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.LEFT;
        wlp.width = 800;
        wlp.height = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(wlp);
    }

    private class ClientWebSocketListener extends  WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            mWebSocket=webSocket;
            connected = true;
            connecting = false;
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            Log.i("KCV", text);
            ChatMsgObject chatMsgObject = gson.fromJson(text, ChatMsgObject.class);
            Message message=Message.obtain();
            message.obj=chatMsgObject;
            mWebSocketHandler.sendMessage(message);
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            if(null!=mWebSocket){
                mWebSocket.close(1000,"");
                mWebSocket=null;
            }
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            Log.e("KCV", reason);
            connected = false;
            connecting = false;
            webSocketConnect();
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @javax.annotation.Nullable Response response) {
            t.printStackTrace();
            connected = false;
            connecting = false;
            webSocketConnect();
        }
    }

    private void webSocketConnect() {
        if(connecting){
            return;
        }
        connecting = true;
        mOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder()
                .url("ws://www.uyan.pw/websocket")
                .build();
        ClientWebSocketListener listener=new ClientWebSocketListener();
        mOkHttpClient.newWebSocket(request,listener);
    }

    public void showLeftChat(String showName) {
        if(!connected){
            webSocketConnect();
            Toast.makeText(context, "未能连接服务器，请重开聊天窗口！", Toast.LENGTH_LONG).show();
            sendBtn.setBackgroundColor(Color.GRAY);
            sendBtn.setEnabled(false);
        } else {
            sendBtn.setBackgroundColor(Color.parseColor("#52ade9"));
            sendBtn.setEnabled(true);
        }
        if(showName != null) {
            this.showName = showName;
        }
        dialog.show();
    }

    //关闭dialog时调用
    public void close() {
        if (dialog != null) {
            dialog.hide();
        }
    }
}
