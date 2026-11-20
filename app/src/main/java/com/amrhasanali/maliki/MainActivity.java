package com.amrhasanali.maliki;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static class ChatMessage {
        String text;
        boolean sentByMe;

        ChatMessage(String text, boolean sentByMe) {
            this.text = text;
            this.sentByMe = sentByMe;
        }
    }

    private boolean ghostMode = false;
    private boolean reverseSenders = false;
    private String phoneNumber = "";
    private final List<ChatMessage> messages = new ArrayList<>();

    private LinearLayout rootLayout;
    private TextView statusText;
    private Button ghostBtn;
    private Button reverseBtn;
    private LinearLayout chatContainer;
    private ScrollView chatScroll;
    private EditText messageInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showPhoneLoginScreen();
    }

    private void showPhoneLoginScreen() {
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#ECE5DD"));
        rootLayout.setGravity(Gravity.CENTER);
        rootLayout.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText("وتساب عمرو المليكي");
        title.setTextSize(24f);
        title.setTextColor(Color.parseColor("#075E54"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 16);

        TextView subtitle = new TextView(this);
        subtitle.setText("أدخل رقم هاتفك لبدء الدردشة");
        subtitle.setTextSize(15f);
        subtitle.setTextColor(Color.parseColor("#444444"));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 32);

        EditText phoneInput = new EditText(this);
        phoneInput.setHint("+967 77xxxxxxxx");
        phoneInput.setText("+967 ");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneInput.setTextColor(Color.BLACK);
        phoneInput.setBackgroundColor(Color.WHITE);
        phoneInput.setPadding(32, 28, 32, 28);

        Button loginBtn = new Button(this);
        loginBtn.setText("دخول");
        loginBtn.setBackgroundColor(Color.parseColor("#128C7E"));
        loginBtn.setTextColor(Color.WHITE);
        loginBtn.setPadding(24, 28, 24, 28);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String entered = phoneInput.getText().toString().trim();
                if (entered.length() < 6) {
                    Toast.makeText(MainActivity.this, "اكتب رقم هاتف صحيح", Toast.LENGTH_SHORT).show();
                    return;
                }
                phoneNumber = entered;
                showChatScreen();
            }
        });

        rootLayout.addView(title);
        rootLayout.addView(subtitle);
        rootLayout.addView(phoneInput, params);
        rootLayout.addView(loginBtn, params);

        setContentView(rootLayout);
    }

    private void showChatScreen() {
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#EFEAE2"));

        // شريط علوي بلون واتساب
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.VERTICAL);
        topBar.setBackgroundColor(Color.parseColor("#075E54"));
        topBar.setPadding(28, 24, 28, 24);

        TextView appTitle = new TextView(this);
        appTitle.setText("وتساب عمرو المليكي (" + phoneNumber + ")");
        appTitle.setTextColor(Color.WHITE);
        appTitle.setTextSize(17f);

        statusText = new TextView(this);
        statusText.setTextSize(13f);
        statusText.setTextColor(Color.parseColor("#D9FDD3"));
        statusText.setPadding(0, 6, 0, 14);

        LinearLayout controlsRow = new LinearLayout(this);
        controlsRow.setOrientation(LinearLayout.HORIZONTAL);

        ghostBtn = new Button(this);
        ghostBtn.setTextSize(12f);
        ghostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ghostMode = !ghostMode;
                updateStatusAndButtons();
                renderMessages();
            }
        });

        reverseBtn = new Button(this);
        reverseBtn.setTextSize(12f);
        reverseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reverseSenders = !reverseSenders;
                updateStatusAndButtons();
                renderMessages();
            }
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        btnParams.setMargins(0, 0, 12, 0);

        LinearLayout.LayoutParams btnParamsRight = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        btnParamsRight.setMargins(12, 0, 0, 0);

        controlsRow.addView(ghostBtn, btnParams);
        controlsRow.addView(reverseBtn, btnParamsRight);

        topBar.addView(appTitle);
        topBar.addView(statusText);
        topBar.addView(controlsRow);

        // منطقة الرسائل
        chatScroll = new ScrollView(this);
        chatScroll.setPadding(20, 20, 20, 20);

        chatContainer = new LinearLayout(this);
        chatContainer.setOrientation(LinearLayout.VERTICAL);
        chatScroll.addView(chatContainer);

        // شريط الإرسال السفلي
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setBackgroundColor(Color.WHITE);
        bottomBar.setPadding(16, 12, 16, 12);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);

        messageInput = new EditText(this);
        messageInput.setHint("اكتب رسالة...");
        messageInput.setTextColor(Color.BLACK);
        messageInput.setBackgroundColor(Color.parseColor("#F5F5F5"));
        messageInput.setPadding(24, 20, 24, 20);

        Button sendBtn = new Button(this);
        sendBtn.setText("إرسال");
        sendBtn.setBackgroundColor(Color.parseColor("#008069"));
        sendBtn.setTextColor(Color.WHITE);
        sendBtn.setPadding(24, 18, 24, 18);

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        inputParams.setMargins(0, 0, 12, 0);

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt = messageInput.getText().toString().trim();
                if (txt.isEmpty()) return;
                messages.add(new ChatMessage(txt, true));
                messageInput.setText("");
                renderMessages();
            }
        });

        bottomBar.addView(messageInput, inputParams);
        bottomBar.addView(sendBtn);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        );

        rootLayout.addView(topBar);
        rootLayout.addView(chatScroll, scrollParams);
        rootLayout.addView(bottomBar);

        setContentView(rootLayout);

        if (messages.isEmpty()) {
            messages.add(new ChatMessage("أهلاً بك في وتساب عمرو المليكي", false));
        }

        updateStatusAndButtons();
        renderMessages();
    }

    private void updateStatusAndButtons() {
        if (ghostMode) {
            statusText.setText("وضع الشبح مفعّل: الظهور مخفي والصح الأزرق متوقف");
            ghostBtn.setText("وضع الشبح: مفعل");
            ghostBtn.setBackgroundColor(Color.parseColor("#1F2937"));
            ghostBtn.setTextColor(Color.parseColor("#34D399"));
        } else {
            statusText.setText("متصل الآن");
            ghostBtn.setText("وضع الشبح: إيقاف");
            ghostBtn.setBackgroundColor(Color.parseColor("#25D366"));
            ghostBtn.setTextColor(Color.BLACK);
        }

        if (reverseSenders) {
            reverseBtn.setText("العكس: مفعل (من الطرف الآخر)");
            reverseBtn.setBackgroundColor(Color.parseColor("#D97706"));
            reverseBtn.setTextColor(Color.WHITE);
        } else {
            reverseBtn.setText("زر العكس: عادي");
            reverseBtn.setBackgroundColor(Color.WHITE);
            reverseBtn.setTextColor(Color.BLACK);
        }
    }

    private void renderMessages() {
        chatContainer.removeAllViews();

        for (ChatMessage m : messages) {
            boolean displayAsOutgoing = m.sentByMe;
            if (reverseSenders) {
                displayAsOutgoing = !displayAsOutgoing;
            }

            TextView bubble = new TextView(this);
            String receipt = ghostMode ? "" : (displayAsOutgoing ? " ✓✓" : "");
            bubble.setText(m.text + receipt);
            bubble.setTextSize(15f);
            bubble.setTextColor(Color.BLACK);
            bubble.setPadding(28, 18, 28, 18);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(24f);
            if (displayAsOutgoing) {
                bg.setColor(Color.parseColor("#D9FDD3"));
            } else {
                bg.setColor(Color.WHITE);
            }
            bubble.setBackground(bg);

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            p.setMargins(0, 0, 0, 14);
            p.gravity = displayAsOutgoing ? Gravity.END : Gravity.START;

            chatContainer.addView(bubble, p);
        }

        chatScroll.post(new Runnable() {
            @Override
            public void run() {
                chatScroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
}
