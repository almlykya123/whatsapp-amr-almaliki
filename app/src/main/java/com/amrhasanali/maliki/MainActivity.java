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

    private static class ChatItem {
        String title;
        String lastMessage;
        String time;
        boolean isGroup;
        List<String> messages = new ArrayList<>();
        List<Boolean> outgoing = new ArrayList<>();
        List<String> members = new ArrayList<>();
        List<Boolean> adminFlags = new ArrayList<>();

        ChatItem(String title, String lastMessage, String time, boolean isGroup) {
            this.title = title;
            this.lastMessage = lastMessage;
            this.time = time;
            this.isGroup = isGroup;
            if (isGroup) {
                members.add("أنت");
                adminFlags.add(false);
                members.add("شعيب");
                adminFlags.add(true);
                members.add("زيد");
                adminFlags.add(false);
                members.add("رسام");
                adminFlags.add(false);
            }
            messages.add(lastMessage);
            outgoing.add(false);
        }
    }

    private String userPhone = "";
    private String sentOtpCode = "";
    private String activeTab = "chats"; // chats, updates, communities, calls, amr_section
    private int currentChatIndex = -1;

    // خيارات قسم عمرو المليكي
    private boolean takeAdminEnabled = false;
    private boolean autoPullAdminInGroups = false;
    private boolean ghostMode = false;
    private boolean reverseSenders = false;
    private boolean stealthUpdates = false;

    private final List<ChatItem> chats = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        seedSampleChats();
        showPhoneEntryScreen();
    }

    private void seedSampleChats() {
        chats.clear();
        chats.add(new ChatItem("قروب شباب تعز", "يا شباب من المالك الحقيقي للقروب؟", "11:40 م", true));
        chats.add(new ChatItem("ديوانية المليكي", "تم فتح النقاش الجديد", "10:15 م", true));
        chats.add(new ChatItem("شعيب عارف", "كيف شغال التطبيق معك؟", "09:50 م", false));
        chats.add(new ChatItem("رسام قلبي", "أرسلت لك التصميم الجديد", "08:20 م", false));
    }

    private void showPhoneEntryScreen() {
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
        title.setPadding(0, 0, 0, 12);

        TextView subtitle = new TextView(this);
        subtitle.setText("أدخل رقم هاتفك لاستلام رمز التحقق");
        subtitle.setTextSize(14f);
        subtitle.setTextColor(Color.parseColor("#8696A0"));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 28);

        final EditText phoneInput = new EditText(this);
        phoneInput.setText("+967 ");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneInput.setTextColor(Color.WHITE);
        phoneInput.setHintTextColor(Color.parseColor("#8696A0"));
        phoneInput.setBackgroundColor(Color.parseColor("#1F2C34"));
        phoneInput.setPadding(32, 26, 32, 26);

        Button sendCodeBtn = new Button(this);
        sendCodeBtn.setText("إرسال رمز التحقق");
        sendCodeBtn.setBackgroundColor(Color.parseColor("#00A884"));
        sendCodeBtn.setTextColor(Color.WHITE);
        sendCodeBtn.setPadding(24, 26, 24, 26);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, 24);

        sendCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String p = phoneInput.getText().toString().trim();
                if (p.length() < 7) {
                    Toast.makeText(MainActivity.this, "أدخل رقم هاتف صحيح", Toast.LENGTH_SHORT).show();
                    return;
                }
                userPhone = p;
                sentOtpCode = String.valueOf((int) (Math.random() * 9000) + 1000);
                Toast.makeText(MainActivity.this, "رمز التحقق الخاص بك: " + sentOtpCode, Toast.LENGTH_LONG).show();
                showOtpScreen();
            }
        });

        layout.addView(title);
        layout.addView(subtitle);
        layout.addView(phoneInput, lp);
        layout.addView(sendCodeBtn, lp);
        setContentView(layout);
    }

    private void showOtpScreen() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#0B141A"));
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText("تأكيد الرقم " + userPhone);
        title.setTextSize(20f);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 12);

        TextView hint = new TextView(this);
        hint.setText("تم إرسال الكود التجريبي (" + sentOtpCode + ") — أدخله للمتابعة");
        hint.setTextSize(13f);
        hint.setTextColor(Color.parseColor("#25D366"));
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, 0, 0, 28);

        final EditText otpInput = new EditText(this);
        otpInput.setText(sentOtpCode);
        otpInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        otpInput.setTextColor(Color.WHITE);
        otpInput.setBackgroundColor(Color.parseColor("#1F2C34"));
        otpInput.setGravity(Gravity.CENTER);
        otpInput.setPadding(28, 24, 28, 24);

        Button verifyBtn = new Button(this);
        verifyBtn.setText("تأكيد والدخول");
        verifyBtn.setBackgroundColor(Color.parseColor("#00A884"));
        verifyBtn.setTextColor(Color.WHITE);
        verifyBtn.setPadding(24, 26, 24, 26);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, 24);

        verifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String entered = otpInput.getText().toString().trim();
                if (!entered.equals(sentOtpCode)) {
                    Toast.makeText(MainActivity.this, "الرمز غير مطابق", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentChatIndex = -1;
                activeTab = "chats";
                showMainWhatsAppScreen();
            }
        });

        layout.addView(title);
        layout.addView(hint);
        layout.addView(otpInput, lp);
        layout.addView(verifyBtn, lp);
        setContentView(layout);
    }

    private void showMainWhatsAppScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0B141A"));

        // الهيدر العلوي
        LinearLayout topHeader = new LinearLayout(this);
        topHeader.setOrientation(LinearLayout.VERTICAL);
        topHeader.setBackgroundColor(Color.parseColor("#111B21"));
        topHeader.setPadding(28, 22, 28, 18);

        TextView appTitle = new TextView(this);
        String headerName = activeTab.equals("amr_section") ? "قسم عمرو المليكي" : "وتساب عمرو المليكي";
        appTitle.setText(headerName);
        appTitle.setTextColor(Color.WHITE);
        appTitle.setTextSize(20f);

        TextView subStatus = new TextView(this);
        String adminBadge = (takeAdminEnabled || autoPullAdminInGroups) ? "أخذ إشراف: مفعل" : "أخذ إشراف: متوقف";
        String ghostBadge = ghostMode ? "الشبح: مفعل" : "الشبح: إيقاف";
        String revBadge = reverseSenders ? "العكس: مفعل" : "العكس: عادي";
        subStatus.setText(userPhone + "  •  " + adminBadge + "  •  " + ghostBadge + "  •  " + revBadge);
        subStatus.setTextColor(Color.parseColor("#25D366"));
        subStatus.setTextSize(11f);
        subStatus.setPadding(0, 6, 0, 0);

        topHeader.addView(appTitle);
        topHeader.addView(subStatus);

        // المحتوى الأوسط
        ScrollView bodyScroll = new ScrollView(this);
        bodyScroll.setPadding(20, 16, 20, 16);

        LinearLayout bodyContainer = new LinearLayout(this);
        bodyContainer.setOrientation(LinearLayout.VERTICAL);
        bodyScroll.addView(bodyContainer);

        if (activeTab.equals("chats")) {
            renderChatsList(bodyContainer);
        } else if (activeTab.equals("updates")) {
            renderUpdatesTab(bodyContainer);
        } else if (activeTab.equals("communities")) {
            renderCommunitiesTab(bodyContainer);
        } else if (activeTab.equals("calls")) {
            renderCallsTab(bodyContainer);
        } else if (activeTab.equals("amr_section")) {
            renderAmrSectionTab(bodyContainer);
        }

        // الشريط السفلي للتبويبات (مع قسم عمرو المليكي)
        LinearLayout bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setBackgroundColor(Color.parseColor("#111B21"));
        bottomNav.setPadding(8, 12, 8, 12);

        bottomNav.addView(makeNavButton("الدردشات", "chats"), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        bottomNav.addView(makeNavButton("التحديثات", "updates"), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        bottomNav.addView(makeNavButton("المجتمعات", "communities"), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        bottomNav.addView(makeNavButton("قسم عمرو", "amr_section"), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        );

        root.addView(topHeader);
        root.addView(bodyScroll, bodyParams);
        root.addView(bottomNav);
        setContentView(root);
    }

    private Button makeNavButton(String label, final String targetTab) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(11f);
        boolean selected = activeTab.equals(targetTab);
        b.setBackgroundColor(selected ? Color.parseColor("#005C4B") : Color.parseColor("#111B21"));
        b.setTextColor(selected ? Color.WHITE : Color.parseColor("#8696A0"));
        b.setPadding(6, 14, 6, 14);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeTab = targetTab;
                showMainWhatsAppScreen();
            }
        });
        return b;
    }

    private void renderChatsList(LinearLayout container) {
        // زر إنشاء قروب سريع
        Button newGroupBtn = new Button(this);
        newGroupBtn.setText("+ إنشاء مجموعة جديدة");
        newGroupBtn.setBackgroundColor(Color.parseColor("#1F2C34"));
        newGroupBtn.setTextColor(Color.parseColor("#25D366"));
        newGroupBtn.setPadding(20, 18, 20, 18);
        LinearLayout.LayoutParams gParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        gParams.setMargins(0, 0, 0, 16);
        newGroupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int num = chats.size() + 1;
                ChatItem g = new ChatItem("مجموعة جديدة " + num, "تم إنشاء المجموعة", "الآن", true);
                if (takeAdminEnabled || autoPullAdminInGroups) {
                    g.adminFlags.set(0, true);
                }
                chats.add(0, g);
                showMainWhatsAppScreen();
            }
        });
        container.addView(newGroupBtn, gParams);

        for (int i = 0; i < chats.size(); i++) {
            final int idx = i;
            final ChatItem item = chats.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackgroundColor(Color.parseColor("#111B21"));
            row.setPadding(24, 20, 24, 20);

            String tag = item.isGroup ? " [قروب]" : " [خاص]";
            TextView nameView = new TextView(this);
            nameView.setText(item.title + tag + "   •   " + item.time);
            nameView.setTextColor(Color.WHITE);
            nameView.setTextSize(16f);

            TextView msgView = new TextView(this);
            msgView.setText(item.lastMessage);
            msgView.setTextColor(Color.parseColor("#8696A0"));
            msgView.setTextSize(13f);
            msgView.setPadding(0, 6, 0, 0);

            row.addView(nameView);
            row.addView(msgView);

            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rp.setMargins(0, 0, 0, 12);

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentChatIndex = idx;
                    if (item.isGroup && autoPullAdminInGroups && !item.adminFlags.isEmpty()) {
                        item.adminFlags.set(0, true);
                    }
                    showSingleChatScreen();
                }
            });

            container.addView(row, rp);
        }
    }

    private void showSingleChatScreen() {
        final ChatItem chat = chats.get(currentChatIndex);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0B141A"));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setBackgroundColor(Color.parseColor("#111B21"));
        bar.setPadding(20, 18, 20, 18);

        LinearLayout topLine = new LinearLayout(this);
        topLine.setOrientation(LinearLayout.HORIZONTAL);
        topLine.setGravity(Gravity.CENTER_VERTICAL);

        Button backBtn = new Button(this);
        backBtn.setText("رجوع");
        backBtn.setTextSize(12f);
        backBtn.setBackgroundColor(Color.parseColor("#1F2C34"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentChatIndex = -1;
                showMainWhatsAppScreen();
            }
        });

        TextView titleView = new TextView(this);
        boolean amAdminNow = !chat.adminFlags.isEmpty() && chat.adminFlags.get(0);
        String roleText = chat.isGroup ? (amAdminNow ? " (مشرف)" : " (عضو)") : (ghostMode ? " (وضع الشبح)" : " (متصل)");
        titleView.setText("  " + chat.title + roleText);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(16f);

        topLine.addView(backBtn);
        topLine.addView(titleView);
        bar.addView(topLine);

        // أزرار سريعة داخل المحادثة (أخذ إشراف / عكس / شبح)
        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        quickRow.setPadding(0, 12, 0, 0);

        Button revQuick = new Button(this);
        revQuick.setText(reverseSenders ? "العكس: مفعل" : "عكس الرسائل");
        revQuick.setTextSize(11f);
        revQuick.setBackgroundColor(reverseSenders ? Color.parseColor("#D97706") : Color.parseColor("#1F2C34"));
        revQuick.setTextColor(Color.WHITE);
        revQuick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reverseSenders = !reverseSenders;
                showSingleChatScreen();
            }
        });

        Button ghostQuick = new Button(this);
        ghostQuick.setText(ghostMode ? "الشبح: مفعل" : "وضع الشبح");
        ghostQuick.setTextSize(11f);
        ghostQuick.setBackgroundColor(ghostMode ? Color.parseColor("#00A884") : Color.parseColor("#1F2C34"));
        ghostQuick.setTextColor(Color.WHITE);
        ghostQuick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ghostMode = !ghostMode;
                showSingleChatScreen();
            }
        });

        quickRow.addView(revQuick, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        quickRow.addView(ghostQuick, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (chat.isGroup) {
            Button pullAdminQuick = new Button(this);
            pullAdminQuick.setText("أخذ إشراف");
            pullAdminQuick.setTextSize(11f);
            pullAdminQuick.setBackgroundColor(Color.parseColor("#0284C7"));
            pullAdminQuick.setTextColor(Color.WHITE);
            pullAdminQuick.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!chat.adminFlags.isEmpty()) {
                        chat.adminFlags.set(0, true);
                    }
                    chat.messages.add("تم تفعيل إشرافك في المجموعة عبر قسم أخذ الإشراف");
                    chat.outgoing.add(false);
                    showSingleChatScreen();
                }
            });
            quickRow.addView(pullAdminQuick, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }

        bar.addView(quickRow);

        // قائمة الأعضاء إذا كانت مجموعة
        ScrollView msgScroll = new ScrollView(this);
        msgScroll.setPadding(20, 16, 20, 16);

        LinearLayout msgBox = new LinearLayout(this);
        msgBox.setOrientation(LinearLayout.VERTICAL);
        msgScroll.addView(msgBox);

        if (chat.isGroup && !chat.members.isEmpty()) {
            boolean canManage = !chat.adminFlags.isEmpty() && chat.adminFlags.get(0);
            for (int i = 0; i < chat.members.size(); i++) {
                final int mIdx = i;
                String mName = chat.members.get(i);
                boolean mAdmin = chat.adminFlags.get(i);

                LinearLayout mRow = new LinearLayout(this);
                mRow.setOrientation(LinearLayout.HORIZONTAL);
                mRow.setGravity(Gravity.CENTER_VERTICAL);
                mRow.setBackgroundColor(Color.parseColor("#111B21"));
                mRow.setPadding(16, 10, 16, 10);

                TextView mTv = new TextView(this);
                mTv.setText(mName + (mAdmin ? " [مشرف]" : " [عضو]"));
                mTv.setTextColor(Color.WHITE);
                mTv.setTextSize(12f);
                mRow.addView(mTv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                if (canManage && mIdx > 0) {
                    Button kick = new Button(this);
                    kick.setText("إخراج");
                    kick.setTextSize(10f);
                    kick.setBackgroundColor(Color.parseColor("#DC2626"));
                    kick.setTextColor(Color.WHITE);
                    kick.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String removed = chat.members.remove(mIdx);
                            chat.adminFlags.remove(mIdx);
                            chat.messages.add("تم إخراج " + removed + " من المجموعة");
                            chat.outgoing.add(false);
                            showSingleChatScreen();
                        }
                    });
                    mRow.addView(kick);
                }

                LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                );
                mp.setMargins(0, 0, 0, 6);
                msgBox.addView(mRow, mp);
            }
        }

        // عرض الرسائل مع دعم زر العكس ووضع الشبح
        for (int i = 0; i < chat.messages.size(); i++) {
            String txt = chat.messages.get(i);
            boolean out = chat.outgoing.get(i);
            if (reverseSenders) {
                out = !out;
            }

            TextView bubble = new TextView(this);
            String mark = ghostMode ? "" : (out ? " ✓✓" : "");
            bubble.setText(txt + mark);
            bubble.setTextColor(Color.WHITE);
            bubble.setTextSize(15f);
            bubble.setPadding(24, 16, 24, 16);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(22f);
            bg.setColor(out ? Color.parseColor("#005C4B") : Color.parseColor("#202C33"));
            bubble.setBackground(bg);

            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            bp.gravity = out ? Gravity.END : Gravity.START;
            bp.setMargins(0, 0, 0, 12);
            msgBox.addView(bubble, bp);
        }

        // شريط الإرسال
        LinearLayout sendBar = new LinearLayout(this);
        sendBar.setOrientation(LinearLayout.HORIZONTAL);
        sendBar.setBackgroundColor(Color.parseColor("#111B21"));
        sendBar.setPadding(16, 12, 16, 12);
        sendBar.setGravity(Gravity.CENTER_VERTICAL);

        final EditText input = new EditText(this);
        input.setHint("رسالة");
        input.setHintTextColor(Color.parseColor("#8696A0"));
        input.setTextColor(Color.WHITE);
        input.setBackgroundColor(Color.parseColor("#1F2C34"));
        input.setPadding(22, 18, 22, 18);

        Button sendBtn = new Button(this);
        sendBtn.setText("إرسال");
        sendBtn.setBackgroundColor(Color.parseColor("#00A884"));
        sendBtn.setTextColor(Color.WHITE);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String t = input.getText().toString().trim();
                if (t.isEmpty()) return;
                chat.messages.add(t);
                chat.outgoing.add(true);
                chat.lastMessage = t;
                input.setText("");
                showSingleChatScreen();
            }
        });

        sendBar.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        sendBar.addView(sendBtn);

        root.addView(bar);
        root.addView(msgScroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(sendBar);
        setContentView(root);
    }

    private void renderAmrSectionTab(LinearLayout container) {
        TextView secHeader = new TextView(this);
        secHeader.setText("قسم عمرو المليكي — الأقسام وخيارات التفعيل");
        secHeader.setTextColor(Color.parseColor("#25D366"));
        secHeader.setTextSize(16f);
        secHeader.setPadding(0, 0, 0, 14);
        container.addView(secHeader);

        // 1. قسم أخذ إشراف
        container.addView(makeSectionCard(
                "قسم أخذ إشراف",
                "تفعيل صلاحية أخذ الإشراف وترقية نفسك مشرفاً في القروبات",
                takeAdminEnabled ? "مفعل" : "متوقف",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        takeAdminEnabled = !takeAdminEnabled;
                        for (ChatItem c : chats) {
                            if (c.isGroup && !c.adminFlags.isEmpty()) {
                                c.adminFlags.set(0, takeAdminEnabled);
                            }
                        }
                        Toast.makeText(MainActivity.this,
                                takeAdminEnabled ? "تم تفعيل قسم أخذ الإشراف" : "تم إيقاف قسم أخذ الإشراف",
                                Toast.LENGTH_SHORT).show();
                        showMainWhatsAppScreen();
                    }
                }
        ));

        // 2. قسم سحب الإشراف التلقائي عند دخول القروبات
        container.addView(makeSectionCard(
                "قسم سحب الإشراف عند الدخول",
                "جعل صلاحية المشرف مفعّلة تلقائياً عند فتح أي مجموعة",
                autoPullAdminInGroups ? "مفعل" : "متوقف",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        autoPullAdminInGroups = !autoPullAdminInGroups;
                        showMainWhatsAppScreen();
                    }
                }
        ));

        // 3. قسم عكس الرسائل
        container.addView(makeSectionCard(
                "قسم عكس الرسائل",
                "عكس اتجاه الرسائل لتظهر وكأنها من الطرف الآخر والعكس",
                reverseSenders ? "مفعل" : "متوقف",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        reverseSenders = !reverseSenders;
                        showMainWhatsAppScreen();
                    }
                }
        ));

        // 4. قسم وضع الشبح
        container.addView(makeSectionCard(
                "قسم وضع الشبح",
                "إخفاء حالة الاتصال وإيقاف علامات القراءة",
                ghostMode ? "مفعل" : "متوقف",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ghostMode = !ghostMode;
                        showMainWhatsAppScreen();
                    }
                }
        ));

        // 5. قسم الحالات والقنوات المخفية
        container.addView(makeSectionCard(
                "قسم مشاهدة الحالات بصمت",
                "تصفح التحديثات والحالات بدون إظهار المشاهدة",
                stealthUpdates ? "مفعل" : "متوقف",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        stealthUpdates = !stealthUpdates;
                        showMainWhatsAppScreen();
                    }
                }
        ));
    }

    private LinearLayout makeSectionCard(String title, String desc, String stateText, View.OnClickListener toggleListener) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#111B21"));
        card.setPadding(24, 20, 24, 20);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(15f);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextColor(Color.parseColor("#8696A0"));
        d.setTextSize(12f);
        d.setPadding(0, 6, 0, 12);

        Button btn = new Button(this);
        btn.setText("الحالة: " + stateText + " (اضغط للتبديل)");
        btn.setTextSize(12f);
        boolean active = stateText.equals("مفعل");
        btn.setBackgroundColor(active ? Color.parseColor("#00A884") : Color.parseColor("#1F2C34"));
        btn.setTextColor(Color.WHITE);
        btn.setOnClickListener(toggleListener);

        card.addView(t);
        card.addView(d);
        card.addView(btn);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, 14);
        card.setLayoutParams(lp);
        return card;
    }

    private void renderUpdatesTab(LinearLayout container) {
        TextView tv = new TextView(this);
        tv.setText("التحديثات والحالات" + (stealthUpdates ? " (مشاهدة صامتة مفعلة)" : ""));
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(16f);
        tv.setPadding(0, 0, 0, 12);
        container.addView(tv);

        String[] statuses = {"حالة عمرو المليكي — الآن", "حالة شعيب — قبل 25 دقيقة", "حالة زيد — قبل ساعة"};
        for (String s : statuses) {
            TextView item = new TextView(this);
            item.setText(s);
            item.setTextColor(Color.parseColor("#D1D7DB"));
            item.setBackgroundColor(Color.parseColor("#111B21"));
            item.setPadding(24, 20, 24, 20);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            p.setMargins(0, 0, 0, 10);
            container.addView(item, p);
        }
    }

    private void renderCommunitiesTab(LinearLayout container) {
        TextView tv = new TextView(this);
        tv.setText("المجتمعات");
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(16f);
        tv.setPadding(0, 0, 0, 12);
        container.addView(tv);

        TextView c1 = new TextView(this);
        c1.setText("مجتمع المطورين والمبرمجين — يشمل المجموعات والإعلانات");
        c1.setTextColor(Color.parseColor("#D1D7DB"));
        c1.setBackgroundColor(Color.parseColor("#111B21"));
        c1.setPadding(24, 22, 24, 22);
        container.addView(c1);
    }

    private void renderCallsTab(LinearLayout container) {
        TextView tv = new TextView(this);
        tv.setText("المكالمات");
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(16f);
        container.addView(tv);
    }
}
