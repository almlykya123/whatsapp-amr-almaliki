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
        boolean isSystemNotice;

        ChatMessage(String text, boolean sentByMe, boolean isSystemNotice) {
            this.text = text;
            this.sentByMe = sentByMe;
            this.isSystemNotice = isSystemNotice;
        }
    }

    private static class GroupMember {
        String name;
        boolean isAdmin;

        GroupMember(String name, boolean isAdmin) {
            this.name = name;
            this.isAdmin = isAdmin;
        }
    }

    private boolean ghostMode = false;
    private boolean reverseSenders = false;
    private boolean amIAdmin = true;
    private String phoneNumber = "";

    private final List<ChatMessage> messages = new ArrayList<>();
    private final List<GroupMember> members = new ArrayList<>();

    private LinearLayout rootLayout;
    private TextView statusText;
    private Button ghostBtn;
    private Button reverseBtn;
    private Button myAdminBtn;
    private LinearLayout membersBox;
    private LinearLayout chatContainer;
    private ScrollView chatScroll;
    private EditText messageInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initDefaultGroupData();
        showPhoneLoginScreen();
    }

    private void initDefaultGroupData() {
        members.clear();
        members.add(new GroupMember("أنت (عمرو)", true));
        members.add(new GroupMember("أحمد", false));
        members.add(new GroupMember("سالم", false));
        members.add(new GroupMember("خالد", false));
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
        subtitle.setText("أدخل رقم هاتفك للدخول إلى القروب");
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

        // الشريط العلوي وأزرار التحكم
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.VERTICAL);
        topBar.setBackgroundColor(Color.parseColor("#075E54"));
        topBar.setPadding(24, 20, 24, 20);

        TextView appTitle = new TextView(this);
        appTitle.setText("قروب وتساب عمرو المليكي (" + phoneNumber + ")");
        appTitle.setTextColor(Color.WHITE);
        appTitle.setTextSize(16f);

        statusText = new TextView(this);
        statusText.setTextSize(12f);
        statusText.setTextColor(Color.parseColor("#D9FDD3"));
        statusText.setPadding(0, 4, 0, 12);

        LinearLayout controlsRow1 = new LinearLayout(this);
        controlsRow1.setOrientation(LinearLayout.HORIZONTAL);

        ghostBtn = new Button(this);
        ghostBtn.setTextSize(11f);
        ghostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ghostMode = !ghostMode;
                updateStatusAndButtons();
                renderMessages();
            }
        });

        reverseBtn = new Button(this);
        reverseBtn.setTextSize(11f);
        reverseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reverseSenders = !reverseSenders;
                updateStatusAndButtons();
                renderMessages();
            }
        });

        myAdminBtn = new Button(this);
        myAdminBtn.setTextSize(11f);
        myAdminBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                amIAdmin = !amIAdmin;
                if (!members.isEmpty()) {
                    members.get(0).isAdmin = amIAdmin;
                }
                messages.add(new ChatMessage(
                        amIAdmin ? "أنت الآن مشرف في القروب" : "أنت الآن عضو عادي في القروب",
                        false,
                        true
                ));
                updateStatusAndButtons();
                renderMembers();
                renderMessages();
            }
        });

        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p1.setMargins(0, 0, 8, 0);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p2.setMargins(4, 0, 4, 0);
        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p3.setMargins(8, 0, 0, 0);

        controlsRow1.addView(ghostBtn, p1);
        controlsRow1.addView(reverseBtn, p2);
        controlsRow1.addView(myAdminBtn, p3);

        topBar.addView(appTitle);
        topBar.addView(statusText);
        topBar.addView(controlsRow1);

        // قسم إدارة أعضاء ومشرفي القروب
        membersBox = new LinearLayout(this);
        membersBox.setOrientation(LinearLayout.VERTICAL);
        membersBox.setBackgroundColor(Color.parseColor("#FFFDF9"));
        membersBox.setPadding(20, 14, 20, 14);

        // منطقة الرسائل
        chatScroll = new ScrollView(this);
        chatScroll.setPadding(20, 16, 20, 16);

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
        messageInput.setHint("اكتب رسالة في القروب...");
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
                messages.add(new ChatMessage(txt, true, false));
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
        rootLayout.addView(membersBox);
        rootLayout.addView(chatScroll, scrollParams);
        rootLayout.addView(bottomBar);

        setContentView(rootLayout);

        if (messages.isEmpty()) {
            messages.add(new ChatMessage("تم إنشاء القروب — يمكنك التحكم بالمشرفين والإخراج من الأعلى", false, true));
        }

        updateStatusAndButtons();
        renderMembers();
        renderMessages();
    }

    private void updateStatusAndButtons() {
        String roleLabel = amIAdmin ? "مشرف" : "عضو";
        String ghostLabel = ghostMode ? "الشبح: مفعل" : "الشبح: إيقاف";
        statusText.setText("صلاحيتك: " + roleLabel + " | " + ghostLabel);

        if (ghostMode) {
            ghostBtn.setText("الشبح: مفعل");
            ghostBtn.setBackgroundColor(Color.parseColor("#1F2937"));
            ghostBtn.setTextColor(Color.parseColor("#34D399"));
        } else {
            ghostBtn.setText("الشبح: إيقاف");
            ghostBtn.setBackgroundColor(Color.parseColor("#25D366"));
            ghostBtn.setTextColor(Color.BLACK);
        }

        if (reverseSenders) {
            reverseBtn.setText("العكس: مفعل");
            reverseBtn.setBackgroundColor(Color.parseColor("#D97706"));
            reverseBtn.setTextColor(Color.WHITE);
        } else {
            reverseBtn.setText("العكس: عادي");
            reverseBtn.setBackgroundColor(Color.WHITE);
            reverseBtn.setTextColor(Color.BLACK);
        }

        if (amIAdmin) {
            myAdminBtn.setText("أنا: مشرف");
            myAdminBtn.setBackgroundColor(Color.parseColor("#0284C7"));
            myAdminBtn.setTextColor(Color.WHITE);
        } else {
            myAdminBtn.setText("دخول مشرف");
            myAdminBtn.setBackgroundColor(Color.parseColor("#E5E7EB"));
            myAdminBtn.setTextColor(Color.BLACK);
        }
    }

    private void renderMembers() {
        membersBox.removeAllViews();

        TextView header = new TextView(this);
        header.setText(amIAdmin
                ? "أعضاء القروب (أنت مشرف: يمكنك إدخال مشرف أو إخراج عضو)"
                : "أعضاء القروب (فعّل «دخول مشرف» للتحكم بالأعضاء)");
        header.setTextSize(12f);
        header.setTextColor(Color.parseColor("#075E54"));
        header.setPadding(0, 0, 0, 8);
        membersBox.addView(header);

        for (int i = 0; i < members.size(); i++) {
            final int index = i;
            final GroupMember m = members.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 4, 0, 4);

            TextView nameView = new TextView(this);
            nameView.setText(m.name + (m.isAdmin ? " [مشرف]" : " [عضو]"));
            nameView.setTextSize(13f);
            nameView.setTextColor(Color.BLACK);

            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            );
            row.addView(nameView, nameParams);

            if (amIAdmin && index > 0) {
                Button toggleAdminBtn = new Button(this);
                toggleAdminBtn.setText(m.isAdmin ? "إزالة مشرف" : "جعله مشرف");
                toggleAdminBtn.setTextSize(10f);
                toggleAdminBtn.setPadding(14, 4, 14, 4);
                toggleAdminBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        m.isAdmin = !m.isAdmin;
                        messages.add(new ChatMessage(
                                m.isAdmin ? "تم تعيين " + m.name + " كمشرف" : "تمت إزالة إشراف " + m.name,
                                false,
                                true
                        ));
                        renderMembers();
                        renderMessages();
                    }
                });

                Button kickBtn = new Button(this);
                kickBtn.setText("إخراج");
                kickBtn.setTextSize(10f);
                kickBtn.setBackgroundColor(Color.parseColor("#DC2626"));
                kickBtn.setTextColor(Color.WHITE);
                kickBtn.setPadding(14, 4, 14, 4);
                kickBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String removedName = m.name;
                        members.remove(index);
                        messages.add(new ChatMessage("تم إخراج " + removedName + " من القروب", false, true));
                        renderMembers();
                        renderMessages();
                    }
                });

                LinearLayout.LayoutParams smallBtnParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                );
                smallBtnParams.setMargins(8, 0, 0, 0);

                row.addView(toggleAdminBtn, smallBtnParams);
                row.addView(kickBtn, smallBtnParams);
            }

            membersBox.addView(row);
        }
    }

    private void renderMessages() {
        chatContainer.removeAllViews();

        for (ChatMessage m : messages) {
            if (m.isSystemNotice) {
                TextView notice = new TextView(this);
                notice.setText(m.text);
                notice.setTextSize(12f);
                notice.setTextColor(Color.parseColor("#374151"));
                notice.setBackgroundColor(Color.parseColor("#E5E7EB"));
                notice.setPadding(20, 10, 20, 10);

                LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                );
                np.gravity = Gravity.CENTER_HORIZONTAL;
                np.setMargins(0, 0, 0, 12);
                chatContainer.addView(notice, np);
                continue;
            }

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
