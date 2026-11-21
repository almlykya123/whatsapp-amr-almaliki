package com.amrhasanali.maliki;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String DB_URL = "https://whatsapp-amr-hasan-default-rtdb.firebaseio.com";
    private static final String PREFS_NAME = "maliki_whatsapp_prefs";

    private static class MsgItem {
        String sender;
        String text;
        long ts;

        MsgItem(String sender, String text, long ts) {
            this.sender = sender;
            this.text = text;
            this.ts = ts;
        }
    }

    private static class ChatEntry {
        String id;
        String title;
        boolean isGroup;
        String lastMsg;

        ChatEntry(String id, String title, boolean isGroup, String lastMsg) {
            this.id = id;
            this.title = title;
            this.isGroup = isGroup;
            this.lastMsg = lastMsg;
        }
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler pollHandler = new Handler(Looper.getMainLooper());

    private String myPhone = "";
    private String sentOtp = "";
    private String activeTab = "chats"; // chats, groups, amr_section

    private boolean isInChatScreen = false;
    private String currentChatId = "";
    private String currentChatTitle = "";
    private boolean currentIsGroup = false;

    // خيارات قسم عمرو المليكي
    private boolean takeAdminEnabled = false;
    private boolean autoPullAdminInGroups = false;
    private boolean ghostMode = false;
    private boolean reverseSenders = false;
    private boolean amAdminInCurrentGroup = false;

    private final List<ChatEntry> chatList = new ArrayList<>();
    private final List<MsgItem> currentMessages = new ArrayList<>();
    private final List<String> groupMembers = new ArrayList<>();

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (myPhone != null && !myPhone.isEmpty()) {
                if (isInChatScreen) {
                    loadMessages(currentChatId, currentIsGroup, false);
                } else if ("chats".equals(activeTab) || "groups".equals(activeTab)) {
                    refreshHomeData();
                }
            }
            pollHandler.postDelayed(this, 3500L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        myPhone = prefs.getString("saved_phone", "");
        takeAdminEnabled = prefs.getBoolean("take_admin", false);
        autoPullAdminInGroups = prefs.getBoolean("auto_pull_admin", false);
        ghostMode = prefs.getBoolean("ghost_mode", false);
        reverseSenders = prefs.getBoolean("reverse_senders", false);

        if (myPhone != null && !myPhone.isEmpty()) {
            registerUserOnCloud(myPhone);
            ensureDefaultGroupsExist();
            showMainScreen();
        } else {
            showPhoneScreen();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.postDelayed(pollRunnable, 2500L);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void savePrefs() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString("saved_phone", myPhone)
                .putBoolean("take_admin", takeAdminEnabled)
                .putBoolean("auto_pull_admin", autoPullAdminInGroups)
                .putBoolean("ghost_mode", ghostMode)
                .putBoolean("reverse_senders", reverseSenders)
                .apply();
    }

    private String sanitizeKey(String raw) {
        if (raw == null) return "unknown";
        return raw.trim().replaceAll("[^0-9A-Za-z_]", "_");
    }

    private String makeDmId(String p1, String p2) {
        String a = sanitizeKey(p1);
        String b = sanitizeKey(p2);
        if (a.compareTo(b) <= 0) {
            return "dm_" + a + "__" + b;
        } else {
            return "dm_" + b + "__" + a;
        }
    }

    private void showPhoneScreen() {
        isInChatScreen = false;
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#0B141A"));
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText("وتساب عمرو المليكي");
        title.setTextSize(24f);
        title.setTextColor(Color.parseColor("#25D366"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 10);

        TextView sub = new TextView(this);
        sub.setText("سجل رقمك للدردشة الحية والقروبات");
        sub.setTextSize(14f);
        sub.setTextColor(Color.parseColor("#8696A0"));
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 0, 0, 28);

        final EditText phoneInput = new EditText(this);
        phoneInput.setText("+967 ");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneInput.setTextColor(Color.WHITE);
        phoneInput.setBackgroundColor(Color.parseColor("#1F2C34"));
        phoneInput.setPadding(30, 24, 30, 24);

        Button sendBtn = new Button(this);
        sendBtn.setText("إرسال الكود");
        sendBtn.setBackgroundColor(Color.parseColor("#00A884"));
        sendBtn.setTextColor(Color.WHITE);
        sendBtn.setPadding(24, 24, 24, 24);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, 22);

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String p = phoneInput.getText().toString().trim();
                if (p.replaceAll("[^0-9]", "").length() < 6) {
                    Toast.makeText(MainActivity.this, "أدخل رقم هاتف صحيح", Toast.LENGTH_SHORT).show();
                    return;
                }
                myPhone = p;
                sentOtp = String.valueOf((int) (Math.random() * 9000) + 1000);
                Toast.makeText(MainActivity.this, "كود التحقق: " + sentOtp, Toast.LENGTH_LONG).show();
                showOtpScreen();
            }
        });

        layout.addView(title);
        layout.addView(sub);
        layout.addView(phoneInput, lp);
        layout.addView(sendBtn, lp);
        setContentView(layout);
    }

    private void showOtpScreen() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#0B141A"));
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText("تأكيد الرقم " + myPhone);
        title.setTextSize(20f);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 12);

        TextView info = new TextView(this);
        info.setText("أدخل رمز التحقق: " + sentOtp);
        info.setTextSize(14f);
        info.setTextColor(Color.parseColor("#25D366"));
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 0, 0, 26);

        final EditText otpInput = new EditText(this);
        otpInput.setText(sentOtp);
        otpInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        otpInput.setTextColor(Color.WHITE);
        otpInput.setBackgroundColor(Color.parseColor("#1F2C34"));
        otpInput.setGravity(Gravity.CENTER);
        otpInput.setPadding(24, 22, 24, 22);

        Button okBtn = new Button(this);
        okBtn.setText("دخول");
        okBtn.setBackgroundColor(Color.parseColor("#00A884"));
        okBtn.setTextColor(Color.WHITE);
        okBtn.setPadding(24, 24, 24, 24);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, 22);

        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!otpInput.getText().toString().trim().equals(sentOtp)) {
                    Toast.makeText(MainActivity.this, "الرمز غير صحيح", Toast.LENGTH_SHORT).show();
                    return;
                }
                savePrefs();
                registerUserOnCloud(myPhone);
                ensureDefaultGroupsExist();
                activeTab = "chats";
                showMainScreen();
            }
        });

        layout.addView(title);
        layout.addView(info);
        layout.addView(otpInput, lp);
        layout.addView(okBtn, lp);
        setContentView(layout);
    }

    private void showMainScreen() {
        isInChatScreen = false;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0B141A"));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(Color.parseColor("#111B21"));
        header.setPadding(24, 20, 24, 16);

        TextView hTitle = new TextView(this);
        String t = "وتساب عمرو المليكي";
        if ("groups".equals(activeTab)) t = "القروبات";
        else if ("amr_section".equals(activeTab)) t = "قسم عمرو المليكي";
        hTitle.setText(t);
        hTitle.setTextColor(Color.WHITE);
        hTitle.setTextSize(20f);

        TextView hSub = new TextView(this);
        String st = myPhone
                + " • " + ((takeAdminEnabled || autoPullAdminInGroups) ? "أخذ إشراف: مفعل" : "أخذ إشراف: متوقف")
                + " • " + (ghostMode ? "الشبح: مفعل" : "الشبح: إيقاف")
                + " • " + (reverseSenders ? "العكس: مفعل" : "العكس: عادي");
        hSub.setText(st);
        hSub.setTextColor(Color.parseColor("#25D366"));
        hSub.setTextSize(11f);
        hSub.setPadding(0, 6, 0, 0);

        header.addView(hTitle);
        header.addView(hSub);

        ScrollView scroll = new ScrollView(this);
        scroll.setPadding(18, 14, 18, 14);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);

        if ("chats".equals(activeTab)) {
            renderDirectChatsTab(body);
        } else if ("groups".equals(activeTab)) {
            renderGroupsTab(body);
        } else {
            renderAmrSectionTab(body);
        }

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Color.parseColor("#111B21"));
        nav.setPadding(8, 10, 8, 10);

        nav.addView(makeTabBtn("الدردشات", "chats"), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        nav.addView(makeTabBtn("القروبات", "groups"), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        nav.addView(makeTabBtn("قسم عمرو", "amr_section"), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(header);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(nav);
        setContentView(root);

        if ("chats".equals(activeTab) || "groups".equals(activeTab)) {
            refreshHomeData();
        }
    }

    private Button makeTabBtn(String label, final String tabId) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12f);
        boolean sel = activeTab.equals(tabId);
        b.setBackgroundColor(sel ? Color.parseColor("#005C4B") : Color.parseColor("#111B21"));
        b.setTextColor(sel ? Color.WHITE : Color.parseColor("#8696A0"));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeTab = tabId;
                showMainScreen();
            }
        });
        return b;
    }

    private void renderDirectChatsTab(LinearLayout container) {
        TextView lbl = new TextView(this);
        lbl.setText("مراسلة أي رقم مسجل أو بدء دردشة جديدة:");
        lbl.setTextColor(Color.parseColor("#D1D7DB"));
        lbl.setTextSize(13f);
        lbl.setPadding(0, 0, 0, 8);
        container.addView(lbl);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        final EditText targetPhoneInput = new EditText(this);
        targetPhoneInput.setHint("رقم الطرف الآخر مثل +9677...");
        targetPhoneInput.setHintTextColor(Color.parseColor("#8696A0"));
        targetPhoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        targetPhoneInput.setTextColor(Color.WHITE);
        targetPhoneInput.setBackgroundColor(Color.parseColor("#1F2C34"));
        targetPhoneInput.setPadding(18, 16, 18, 16);

        Button openChatBtn = new Button(this);
        openChatBtn.setText("فتح الدردشة");
        openChatBtn.setBackgroundColor(Color.parseColor("#00A884"));
        openChatBtn.setTextColor(Color.WHITE);
        openChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String other = targetPhoneInput.getText().toString().trim();
                if (other.replaceAll("[^0-9]", "").length() < 6) {
                    Toast.makeText(MainActivity.this, "أدخل رقم صحيح للمراسلة", Toast.LENGTH_SHORT).show();
                    return;
                }
                openConversation(makeDmId(myPhone, other), other, false);
            }
        });

        row.addView(targetPhoneInput, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(openChatBtn);

        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rlp.setMargins(0, 0, 0, 16);
        container.addView(row, rlp);

        TextView usersHeader = new TextView(this);
        usersHeader.setText("المستخدمون والمحادثات المتاحة:");
        usersHeader.setTextColor(Color.parseColor("#25D366"));
        usersHeader.setTextSize(14f);
        usersHeader.setPadding(0, 0, 0, 10);
        container.addView(usersHeader);

        if (chatList.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("جاري تحميل المستخدمين من السحابة... إذا لم يظهر أحد بعد، أدخل رقمه في الأعلى وابدأ الدردشة.");
            empty.setTextColor(Color.parseColor("#8696A0"));
            empty.setTextSize(13f);
            container.addView(empty);
        } else {
            for (final ChatEntry entry : chatList) {
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackgroundColor(Color.parseColor("#111B21"));
                card.setPadding(22, 18, 22, 18);

                TextView t = new TextView(this);
                t.setText(entry.title);
                t.setTextColor(Color.WHITE);
                t.setTextSize(16f);

                TextView s = new TextView(this);
                s.setText(entry.lastMsg.isEmpty() ? "اضغط لبدء المراسلة" : entry.lastMsg);
                s.setTextColor(Color.parseColor("#8696A0"));
                s.setTextSize(12f);
                s.setPadding(0, 4, 0, 0);

                card.addView(t);
                card.addView(s);

                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                );
                clp.setMargins(0, 0, 0, 10);
                card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openConversation(entry.id, entry.title, false);
                    }
                });
                container.addView(card, clp);
            }
        }
    }

    private void renderGroupsTab(LinearLayout container) {
        LinearLayout createRow = new LinearLayout(this);
        createRow.setOrientation(LinearLayout.HORIZONTAL);
        createRow.setGravity(Gravity.CENTER_VERTICAL);

        final EditText groupNameInput = new EditText(this);
        groupNameInput.setHint("اسم قروب جديد...");
        groupNameInput.setHintTextColor(Color.parseColor("#8696A0"));
        groupNameInput.setTextColor(Color.WHITE);
        groupNameInput.setBackgroundColor(Color.parseColor("#1F2C34"));
        groupNameInput.setPadding(18, 16, 18, 16);

        Button createGroupBtn = new Button(this);
        createGroupBtn.setText("+ إنشاء قروب");
        createGroupBtn.setBackgroundColor(Color.parseColor("#00A884"));
        createGroupBtn.setTextColor(Color.WHITE);
        createGroupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String gName = groupNameInput.getText().toString().trim();
                if (gName.isEmpty()) gName = "قروب عمرو المليكي";
                createGroupOnCloud(gName);
                groupNameInput.setText("");
            }
        });

        createRow.addView(groupNameInput, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        createRow.addView(createGroupBtn);

        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        glp.setMargins(0, 0, 0, 16);
        container.addView(createRow, glp);

        TextView gHeader = new TextView(this);
        gHeader.setText("القروبات النشطة (الدردشة جماعية ومباشرة):");
        gHeader.setTextColor(Color.parseColor("#25D366"));
        gHeader.setTextSize(14f);
        gHeader.setPadding(0, 0, 0, 10);
        container.addView(gHeader);

        if (chatList.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("جاري تحميل القروبات...");
            empty.setTextColor(Color.parseColor("#8696A0"));
            container.addView(empty);
        } else {
            for (final ChatEntry g : chatList) {
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackgroundColor(Color.parseColor("#111B21"));
                card.setPadding(22, 18, 22, 18);

                TextView t = new TextView(this);
                t.setText(g.title + " [قروب]");
                t.setTextColor(Color.WHITE);
                t.setTextSize(16f);

                TextView s = new TextView(this);
                s.setText(g.lastMsg.isEmpty() ? "اضغط للدخول ومشاهدة رسائل القروب" : g.lastMsg);
                s.setTextColor(Color.parseColor("#8696A0"));
                s.setTextSize(12f);
                s.setPadding(0, 4, 0, 0);

                card.addView(t);
                card.addView(s);

                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                );
                clp.setMargins(0, 0, 0, 10);
                card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openConversation(g.id, g.title, true);
                    }
                });
                container.addView(card, clp);
            }
        }
    }

    private void renderAmrSectionTab(LinearLayout container) {
        TextView head = new TextView(this);
        head.setText("قسم عمرو المليكي — التحكم والأقسام");
        head.setTextColor(Color.parseColor("#25D366"));
        head.setTextSize(16f);
        head.setPadding(0, 0, 0, 14);
        container.addView(head);

        container.addView(makeSwitchCard(
                "قسم أخذ إشراف",
                "تفعيل صلاحية أخذ الإشراف وترقيتك مشرفاً في القروبات",
                takeAdminEnabled,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        takeAdminEnabled = !takeAdminEnabled;
                        savePrefs();
                        showMainScreen();
                    }
                }
        ));

        container.addView(makeSwitchCard(
                "قسم أخذ إشراف تلقائي عند دخول القروب",
                "تفعيل إشرافك تلقائياً بمجرد فتح أي قروب",
                autoPullAdminInGroups,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        autoPullAdminInGroups = !autoPullAdminInGroups;
                        savePrefs();
                        showMainScreen();
                    }
                }
        ));

        container.addView(makeSwitchCard(
                "قسم عكس الرسائل",
                "عكس اتجاه عرض الرسائل داخل المحادثات",
                reverseSenders,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        reverseSenders = !reverseSenders;
                        savePrefs();
                        showMainScreen();
                    }
                }
        ));

        container.addView(makeSwitchCard(
                "قسم وضع الشبح",
                "إخفاء حالة الاتصال وعلامات القراءة",
                ghostMode,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ghostMode = !ghostMode;
                        savePrefs();
                        showMainScreen();
                    }
                }
        ));

        Button logoutBtn = new Button(this);
        logoutBtn.setText("تبديل الرقم / تسجيل خروج");
        logoutBtn.setBackgroundColor(Color.parseColor("#7F1D1D"));
        logoutBtn.setTextColor(Color.WHITE);
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myPhone = "";
                savePrefs();
                showPhoneScreen();
            }
        });
        container.addView(logoutBtn);
    }

    private LinearLayout makeSwitchCard(String title, String desc, boolean enabled, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#111B21"));
        card.setPadding(22, 18, 22, 18);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(15f);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextColor(Color.parseColor("#8696A0"));
        d.setTextSize(12f);
        d.setPadding(0, 4, 0, 12);

        Button btn = new Button(this);
        btn.setText("الحالة: " + (enabled ? "مفعل" : "متوقف") + " (اضغط للتبديل)");
        btn.setTextSize(12f);
        btn.setBackgroundColor(enabled ? Color.parseColor("#00A884") : Color.parseColor("#1F2C34"));
        btn.setTextColor(Color.WHITE);
        btn.setOnClickListener(listener);

        card.addView(t);
        card.addView(d);
        card.addView(btn);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, 12);
        card.setLayoutParams(lp);
        return card;
    }

    private void openConversation(String chatId, String title, boolean isGroup) {
        isInChatScreen = true;
        currentChatId = chatId;
        currentChatTitle = title;
        currentIsGroup = isGroup;
        amAdminInCurrentGroup = isGroup && (takeAdminEnabled || autoPullAdminInGroups);
        renderConversationScreen();
        loadMessages(currentChatId, currentIsGroup, true);
    }

    private void renderConversationScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0B141A"));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setBackgroundColor(Color.parseColor("#111B21"));
        top.setPadding(18, 16, 18, 14);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);

        Button backBtn = new Button(this);
        backBtn.setText("رجوع");
        backBtn.setTextSize(12f);
        backBtn.setBackgroundColor(Color.parseColor("#1F2C34"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isInChatScreen = false;
                showMainScreen();
            }
        });

        TextView titleTv = new TextView(this);
        String roleTag = currentIsGroup ? (amAdminInCurrentGroup ? " [مشرف]" : " [عضو]") : "";
        titleTv.setText("  " + currentChatTitle + roleTag);
        titleTv.setTextColor(Color.WHITE);
        titleTv.setTextSize(15f);

        row1.addView(backBtn);
        row1.addView(titleTv);
        top.addView(row1);

        LinearLayout toolsRow = new LinearLayout(this);
        toolsRow.setOrientation(LinearLayout.HORIZONTAL);
        toolsRow.setPadding(0, 10, 0, 0);

        Button revBtn = new Button(this);
        revBtn.setText(reverseSenders ? "العكس: مفعل" : "عكس");
        revBtn.setTextSize(11f);
        revBtn.setBackgroundColor(reverseSenders ? Color.parseColor("#D97706") : Color.parseColor("#1F2C34"));
        revBtn.setTextColor(Color.WHITE);
        revBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reverseSenders = !reverseSenders;
                savePrefs();
                renderConversationScreen();
            }
        });

        Button ghostBtn = new Button(this);
        ghostBtn.setText(ghostMode ? "الشبح: مفعل" : "الشبح");
        ghostBtn.setTextSize(11f);
        ghostBtn.setBackgroundColor(ghostMode ? Color.parseColor("#00A884") : Color.parseColor("#1F2C34"));
        ghostBtn.setTextColor(Color.WHITE);
        ghostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ghostMode = !ghostMode;
                savePrefs();
                renderConversationScreen();
            }
        });

        toolsRow.addView(revBtn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        toolsRow.addView(ghostBtn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (currentIsGroup) {
            Button adminBtn = new Button(this);
            adminBtn.setText("أخذ إشراف");
            adminBtn.setTextSize(11f);
            adminBtn.setBackgroundColor(Color.parseColor("#0284C7"));
            adminBtn.setTextColor(Color.WHITE);
            adminBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    amAdminInCurrentGroup = true;
                    takeAdminEnabled = true;
                    savePrefs();
                    Toast.makeText(MainActivity.this, "تم أخذ الإشراف في القروب", Toast.LENGTH_SHORT).show();
                    renderConversationScreen();
                }
            });
            toolsRow.addView(adminBtn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }

        top.addView(toolsRow);

        ScrollView msgScroll = new ScrollView(this);
        msgScroll.setPadding(18, 14, 18, 14);
        LinearLayout msgBox = new LinearLayout(this);
        msgBox.setOrientation(LinearLayout.VERTICAL);
        msgScroll.addView(msgBox);

        if (currentIsGroup && !groupMembers.isEmpty()) {
            TextView mHeader = new TextView(this);
            mHeader.setText("أعضاء القروب (" + groupMembers.size() + "):");
            mHeader.setTextColor(Color.parseColor("#25D366"));
            mHeader.setTextSize(12f);
            mHeader.setPadding(0, 0, 0, 6);
            msgBox.addView(mHeader);

            for (int i = 0; i < groupMembers.size(); i++) {
                final String member = groupMembers.get(i);
                LinearLayout mRow = new LinearLayout(this);
                mRow.setOrientation(LinearLayout.HORIZONTAL);
                mRow.setGravity(Gravity.CENTER_VERTICAL);
                mRow.setBackgroundColor(Color.parseColor("#111B21"));
                mRow.setPadding(14, 8, 14, 8);

                TextView mTv = new TextView(this);
                mTv.setText(member + (member.equals(myPhone) && amAdminInCurrentGroup ? " [مشرف]" : ""));
                mTv.setTextColor(Color.WHITE);
                mTv.setTextSize(12f);
                mRow.addView(mTv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                if (amAdminInCurrentGroup && !member.equals(myPhone)) {
                    Button kickBtn = new Button(this);
                    kickBtn.setText("إخراج");
                    kickBtn.setTextSize(10f);
                    kickBtn.setBackgroundColor(Color.parseColor("#DC2626"));
                    kickBtn.setTextColor(Color.WHITE);
                    kickBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            groupMembers.remove(member);
                            sendMessageToCloud(currentChatId, true, "تم إخراج " + member + " من القروب");
                        }
                    });
                    mRow.addView(kickBtn);
                }

                LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                );
                mlp.setMargins(0, 0, 0, 6);
                msgBox.addView(mRow, mlp);
            }
        }

        if (currentMessages.isEmpty()) {
            TextView noMsg = new TextView(this);
            noMsg.setText("لا توجد رسائل بعد. ابدأ المراسلة الآن!");
            noMsg.setTextColor(Color.parseColor("#8696A0"));
            noMsg.setGravity(Gravity.CENTER);
            noMsg.setPadding(0, 32, 0, 0);
            msgBox.addView(noMsg);
        } else {
            for (MsgItem m : currentMessages) {
                boolean mine = m.sender.equals(myPhone);
                if (reverseSenders) {
                    mine = !mine;
                }

                TextView bubble = new TextView(this);
                String prefix = currentIsGroup ? (m.sender + ":\n") : "";
                String suffix = ghostMode ? "" : (mine ? "  ✓✓" : "");
                bubble.setText(prefix + m.text + suffix);
                bubble.setTextColor(Color.WHITE);
                bubble.setTextSize(14f);
                bubble.setPadding(22, 14, 22, 14);

                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(20f);
                bg.setColor(mine ? Color.parseColor("#005C4B") : Color.parseColor("#202C33"));
                bubble.setBackground(bg);

                LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                );
                blp.gravity = mine ? Gravity.END : Gravity.START;
                blp.setMargins(0, 0, 0, 10);
                msgBox.addView(bubble, blp);
            }
        }

        LinearLayout bottomInput = new LinearLayout(this);
        bottomInput.setOrientation(LinearLayout.HORIZONTAL);
        bottomInput.setBackgroundColor(Color.parseColor("#111B21"));
        bottomInput.setPadding(14, 10, 14, 10);
        bottomInput.setGravity(Gravity.CENTER_VERTICAL);

        final EditText msgField = new EditText(this);
        msgField.setHint("اكتب رسالة...");
        msgField.setHintTextColor(Color.parseColor("#8696A0"));
        msgField.setTextColor(Color.WHITE);
        msgField.setBackgroundColor(Color.parseColor("#1F2C34"));
        msgField.setPadding(20, 16, 20, 16);

        Button sendBtn = new Button(this);
        sendBtn.setText("إرسال");
        sendBtn.setBackgroundColor(Color.parseColor("#00A884"));
        sendBtn.setTextColor(Color.WHITE);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt = msgField.getText().toString().trim();
                if (txt.isEmpty()) return;
                msgField.setText("");
                sendMessageToCloud(currentChatId, currentIsGroup, txt);
            }
        });

        bottomInput.addView(msgField, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        bottomInput.addView(sendBtn);

        root.addView(top);
        root.addView(msgScroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(bottomInput);
        setContentView(root);
    }

    private void registerUserOnCloud(final String phone) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String key = sanitizeKey(phone);
                    JSONObject obj = new JSONObject();
                    obj.put("phone", phone);
                    obj.put("lastSeen", ghostMode ? "شبح" : "متصل");
                    obj.put("updatedAt", System.currentTimeMillis());
                    httpPut(DB_URL + "/users/" + key + ".json", obj.toString());
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void ensureDefaultGroupsExist() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String existing = httpGet(DB_URL + "/groups/general_maliki.json");
                    if (existing == null || existing.equals("null") || existing.trim().isEmpty()) {
                        JSONObject g = new JSONObject();
                        g.put("name", "قروب عمرو المليكي العام");
                        g.put("lastMsg", "مرحباً بكم في قروب عمرو المليكي");
                        g.put("updatedAt", System.currentTimeMillis());
                        httpPut(DB_URL + "/groups/general_maliki.json", g.toString());
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void createGroupOnCloud(final String groupName) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String id = "grp_" + System.currentTimeMillis();
                    JSONObject g = new JSONObject();
                    g.put("name", groupName);
                    g.put("creator", myPhone);
                    g.put("lastMsg", "تم إنشاء القروب بواسطة " + myPhone);
                    g.put("updatedAt", System.currentTimeMillis());
                    httpPut(DB_URL + "/groups/" + id + ".json", g.toString());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "تم إنشاء القروب: " + groupName, Toast.LENGTH_SHORT).show();
                            refreshHomeData();
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void refreshHomeData() {
        final String mode = activeTab;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final List<ChatEntry> fetched = new ArrayList<>();
                try {
                    if ("chats".equals(mode)) {
                        String rawUsers = httpGet(DB_URL + "/users.json");
                        if (rawUsers != null && !rawUsers.equals("null")) {
                            JSONObject usersObj = new JSONObject(rawUsers);
                            Iterator<String> keys = usersObj.keys();
                            while (keys.hasNext()) {
                                String k = keys.next();
                                JSONObject u = usersObj.optJSONObject(k);
                                if (u != null) {
                                    String p = u.optString("phone", k);
                                    if (!p.equals(myPhone)) {
                                        fetched.add(new ChatEntry(makeDmId(myPhone, p), p, false, "اضغط للمراسلة المباشرة"));
                                    }
                                }
                            }
                        }
                    } else if ("groups".equals(mode)) {
                        String rawGroups = httpGet(DB_URL + "/groups.json");
                        if (rawGroups != null && !rawGroups.equals("null")) {
                            JSONObject gObj = new JSONObject(rawGroups);
                            Iterator<String> keys = gObj.keys();
                            while (keys.hasNext()) {
                                String k = keys.next();
                                JSONObject item = gObj.optJSONObject(k);
                                if (item != null) {
                                    String name = item.optString("name", k);
                                    String last = item.optString("lastMsg", "");
                                    fetched.add(new ChatEntry(k, name, true, last));
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!isInChatScreen && activeTab.equals(mode)) {
                            chatList.clear();
                            chatList.addAll(fetched);
                            showMainScreen();
                        }
                    }
                });
            }
        });
    }

    private void loadMessages(final String chatId, final boolean isGroup, final boolean forceRedraw) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final List<MsgItem> loaded = new ArrayList<>();
                final List<String> members = new ArrayList<>();
                try {
                    String path = isGroup ? ("/groups/" + chatId + "/messages.json") : ("/chats/" + chatId + "/messages.json");
                    String raw = httpGet(DB_URL + path);
                    if (raw != null && !raw.equals("null")) {
                        JSONObject map = new JSONObject(raw);
                        Iterator<String> keys = map.keys();
                        while (keys.hasNext()) {
                            JSONObject m = map.optJSONObject(keys.next());
                            if (m != null) {
                                String sender = m.optString("sender", "مستخدم");
                                String text = m.optString("text", "");
                                long ts = m.optLong("ts", 0L);
                                loaded.add(new MsgItem(sender, text, ts));
                                if (!members.contains(sender)) {
                                    members.add(sender);
                                }
                            }
                        }
                    }
                    if (!members.contains(myPhone)) {
                        members.add(0, myPhone);
                    }
                } catch (Exception ignored) {
                }

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isInChatScreen && currentChatId.equals(chatId)) {
                            boolean changed = forceRedraw || (loaded.size() != currentMessages.size());
                            currentMessages.clear();
                            currentMessages.addAll(loaded);
                            groupMembers.clear();
                            groupMembers.addAll(members);
                            if (changed) {
                                renderConversationScreen();
                            }
                        }
                    }
                });
            }
        });
    }

    private void sendMessageToCloud(final String chatId, final boolean isGroup, final String text) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    long now = System.currentTimeMillis();
                    JSONObject msg = new JSONObject();
                    msg.put("sender", myPhone);
                    msg.put("text", text);
                    msg.put("ts", now);

                    String msgPath = isGroup
                            ? (DB_URL + "/groups/" + chatId + "/messages.json")
                            : (DB_URL + "/chats/" + chatId + "/messages.json");
                    httpPost(msgPath, msg.toString());

                    if (isGroup) {
                        JSONObject meta = new JSONObject();
                        meta.put("lastMsg", myPhone + ": " + text);
                        meta.put("updatedAt", now);
                        httpPatch(DB_URL + "/groups/" + chatId + ".json", meta.toString());
                    }

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            loadMessages(chatId, isGroup, true);
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        });
    }

    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    private void httpPut(String urlStr, String body) throws Exception {
        sendJson(urlStr, "PUT", body);
    }

    private void httpPost(String urlStr, String body) throws Exception {
        sendJson(urlStr, "POST", body);
    }

    private void httpPatch(String urlStr, String body) throws Exception {
        sendJson(urlStr, "PATCH", body);
    }

    private void sendJson(String urlStr, String method, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        conn.getInputStream().close();
    }
}
