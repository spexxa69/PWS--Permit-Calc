package com.billtobottles.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A fully native, offline bottle/case counter. No WebView or internet access is used. */
public class MainActivity extends Activity {
    private static final String PREFS = "bill_to_bottles_local";
    private static final String DATA_KEY = "entries_v1";

    private static final int BG = Color.rgb(238, 240, 236);
    private static final int CARD = Color.WHITE;
    private static final int INK = Color.rgb(29, 42, 36);
    private static final int MUTED = Color.rgb(102, 114, 107);
    private static final int LINE = Color.rgb(213, 218, 212);
    private static final int IMFL = Color.rgb(31, 92, 70);
    private static final int BEER = Color.rgb(168, 112, 26);
    private static final int DANGER = Color.rgb(179, 53, 43);

    private final List<SizeItem> sizes = Arrays.asList(
            new SizeItem(750, 12, "imfl"),
            new SizeItem(375, 24, "imfl"),
            new SizeItem(180, 48, "imfl"),
            new SizeItem(650, 12, "beer"),
            new SizeItem(500, 24, "beer"),
            new SizeItem(330, 24, "beer"),
            new SizeItem(275, 24, "beer")
    );

    private final Map<String, ArrayList<Integer>> entries = new LinkedHashMap<>();
    private final ArrayList<String> undoKeys = new ArrayList<>();
    private final Map<String, ProductViews> productViews = new LinkedHashMap<>();

    private LinearLayout imflContainer;
    private LinearLayout beerContainer;
    private TextView grandValue;
    private TextView grandSplit;
    private TextView[] summaryValues;
    private ScrollView scrollView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        loadEntries();
        buildScreen();
        renderAll();
    }

    private void loadEntries() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        try {
            JSONObject saved = new JSONObject(prefs.getString(DATA_KEY, "{}"));
            for (SizeItem size : sizes) {
                String key = size.key();
                entries.put(key, readArray(saved.optJSONArray(key)));
                entries.put(looseKey(key), readArray(saved.optJSONArray(looseKey(key))));
            }
        } catch (Exception ignored) {
            for (SizeItem size : sizes) {
                entries.put(size.key(), new ArrayList<Integer>());
                entries.put(looseKey(size.key()), new ArrayList<Integer>());
            }
        }
        for (SizeItem size : sizes) {
            if (!entries.containsKey(size.key())) entries.put(size.key(), new ArrayList<Integer>());
            if (!entries.containsKey(looseKey(size.key()))) entries.put(looseKey(size.key()), new ArrayList<Integer>());
        }
    }

    private ArrayList<Integer> readArray(JSONArray array) {
        ArrayList<Integer> result = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                int n = array.optInt(i, 0);
                if (n > 0) result.add(n);
            }
        }
        return result;
    }

    private void saveEntries() {
        try {
            JSONObject saved = new JSONObject();
            for (Map.Entry<String, ArrayList<Integer>> entry : entries.entrySet()) {
                JSONArray values = new JSONArray();
                for (Integer value : entry.getValue()) values.put(value);
                saved.put(entry.getKey(), values);
            }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(DATA_KEY, saved.toString()).apply();
        } catch (Exception ignored) {
            Toast.makeText(this, "Could not save this change", Toast.LENGTH_SHORT).show();
        }
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(8), dp(12), dp(16));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("Bill to Bottles", 24, INK, true);
        content.addView(title, matchWrap());
        TextView subtitle = text("Enter cases in the top box and loose bottles under ADDITION. Your entries save on this phone.", 13, MUTED, false);
        LinearLayout.LayoutParams subLp = matchWrap();
        subLp.topMargin = dp(3);
        subLp.bottomMargin = dp(10);
        content.addView(subtitle, subLp);

        addSectionHeading(content, "IMFL");
        imflContainer = new LinearLayout(this);
        imflContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(imflContainer, matchWrap());

        addSectionHeading(content, "Beer");
        beerContainer = new LinearLayout(this);
        beerContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(beerContainer, matchWrap());

        addSectionHeading(content, "Grand sum");
        content.addView(buildSummaryCard(), matchWrap());

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setPadding(dp(12), dp(10), dp(12), dp(10));
        bottom.setBackground(roundRect(CARD, INK, 2, 0));
        LinearLayout.LayoutParams bottomLp = matchWrap();
        root.addView(bottom, bottomLp);

        LinearLayout totalBlock = new LinearLayout(this);
        totalBlock.setOrientation(LinearLayout.VERTICAL);
        totalBlock.setMinimumWidth(dp(80));
        bottom.addView(totalBlock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        totalBlock.addView(text("TOTAL BOTTLES", 11, MUTED, true), matchWrap());
        grandValue = text("0", 30, INK, true);
        totalBlock.addView(grandValue, matchWrap());
        grandSplit = text("", 11, MUTED, false);
        totalBlock.addView(grandSplit, matchWrap());

        Button undo = button("Undo", CARD, INK, LINE);
        LinearLayout.LayoutParams undoLp = new LinearLayout.LayoutParams(dp(72), dp(44));
        undoLp.leftMargin = dp(6);
        bottom.addView(undo, undoLp);
        undo.setOnClickListener(v -> undoLast());

        Button clear = button("Clear", CARD, DANGER, LINE);
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(dp(72), dp(44));
        clearLp.leftMargin = dp(6);
        bottom.addView(clear, clearLp);
        clear.setOnClickListener(v -> confirmClear());
    }

    private void addSectionHeading(LinearLayout parent, String label) {
        TextView heading = text(label, 14, MUTED, true);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(12);
        lp.bottomMargin = dp(7);
        parent.addView(heading, lp);
    }

    private View buildProductCard(SizeItem size) {
        String key = size.key();
        int accent = size.type.equals("imfl") ? IMFL : BEER;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(11), dp(12), dp(12));
        card.setBackground(roundRect(CARD, LINE, 1, 12));
        LinearLayout.LayoutParams cardLp = matchWrap();
        cardLp.bottomMargin = dp(9);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(top, matchWrap());

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        top.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        left.addView(text(size.ml + " ml", 20, INK, true), matchWrap());
        left.addView(text(size.perCase + " bottles per case", 12, MUTED, false), matchWrap());

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.RIGHT);
        top.addView(right, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView bottles = text("0", 24, accent, true);
        bottles.setGravity(Gravity.RIGHT);
        right.addView(bottles, matchWrap());
        TextView breakdown = text("0 cases + 0 loose", 11, MUTED, false);
        breakdown.setGravity(Gravity.RIGHT);
        right.addView(breakdown, matchWrap());

        FlowLayout caseChips = new FlowLayout(this);
        LinearLayout.LayoutParams chipsLp = matchWrap();
        chipsLp.topMargin = dp(7);
        card.addView(caseChips, chipsLp);

        LinearLayout caseRow = inputRow("Cases", "Add");
        LinearLayout.LayoutParams rowLp = matchWrap();
        rowLp.topMargin = dp(8);
        card.addView(caseRow, rowLp);
        EditText caseInput = (EditText) caseRow.getChildAt(0);
        Button caseAdd = (Button) caseRow.getChildAt(1);
        caseAdd.setOnClickListener(v -> addFromInput(key, caseInput));
        caseInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { addFromInput(key, caseInput); return true; }
            return false;
        });

        Button looseToggle = button("ADDITION +", CARD, INK, INK);
        looseToggle.setTextSize(12);
        looseToggle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        looseToggle.setLetterSpacing(0.04f);
        LinearLayout.LayoutParams toggleLp = matchWrap();
        toggleLp.topMargin = dp(9);
        card.addView(looseToggle, toggleLp);

        LinearLayout loosePanel = new LinearLayout(this);
        loosePanel.setOrientation(LinearLayout.VERTICAL);
        loosePanel.setVisibility(View.GONE);
        card.addView(loosePanel, matchWrap());
        TextView looseLabel = text("Loose bottles", 12, MUTED, false);
        LinearLayout.LayoutParams looseLabelLp = matchWrap();
        looseLabelLp.topMargin = dp(9);
        loosePanel.addView(looseLabel, looseLabelLp);
        FlowLayout looseChips = new FlowLayout(this);
        LinearLayout.LayoutParams looseChipsLp = matchWrap();
        looseChipsLp.topMargin = dp(5);
        loosePanel.addView(looseChips, looseChipsLp);
        LinearLayout looseRow = inputRow("Loose bottles", "Add");
        LinearLayout.LayoutParams looseRowLp = matchWrap();
        looseRowLp.topMargin = dp(7);
        loosePanel.addView(looseRow, looseRowLp);
        EditText looseInput = (EditText) looseRow.getChildAt(0);
        Button looseAdd = (Button) looseRow.getChildAt(1);
        String looseKey = looseKey(key);
        looseAdd.setOnClickListener(v -> addFromInput(looseKey, looseInput));
        looseInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { addFromInput(looseKey, looseInput); return true; }
            return false;
        });

        looseToggle.setOnClickListener(v -> {
            boolean open = loosePanel.getVisibility() != View.VISIBLE;
            loosePanel.setVisibility(open ? View.VISIBLE : View.GONE);
            looseToggle.setText(open ? "ADDITION −" : "ADDITION +");
            if (open) looseInput.requestFocus();
        });

        productViews.put(key, new ProductViews(size, bottles, breakdown, caseChips, looseChips,
                loosePanel, looseToggle, caseInput, looseInput));
        return card;
    }

    private LinearLayout inputRow(String hint, String buttonLabel) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(17);
        input.setTextColor(INK);
        input.setHintTextColor(MUTED);
        input.setHint(hint);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(10), dp(5), dp(10), dp(5));
        input.setBackground(roundRect(CARD, LINE, 2, 9));
        row.addView(input, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button add = button(buttonLabel, INK, Color.WHITE, INK);
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(dp(72), dp(48));
        addLp.leftMargin = dp(8);
        row.addView(add, addLp);
        return row;
    }

    private View buildSummaryCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(8), dp(6), dp(8), dp(6));
        card.setBackground(roundRect(CARD, LINE, 1, 12));
        String[] labels = {"", "Cases", "Loose", "Bottles"};
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < labels.length; i++) {
            TextView cell = text(labels[i], 11, MUTED, true);
            cell.setGravity(i == 0 ? Gravity.LEFT : Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            header.addView(cell, new LinearLayout.LayoutParams(i == 0 ? 0 : 0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, i == 0 ? 1.25f : 1f));
        }
        card.addView(header, matchWrap());

        String[] rowNames = {"IMFL", "Beer", "Total"};
        summaryValues = new TextView[9];
        for (int row = 0; row < 3; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setOrientation(LinearLayout.HORIZONTAL);
            TextView name = text(rowNames[row], row == 2 ? 15 : 14, INK, true);
            line.addView(name, new LinearLayout.LayoutParams(0, dp(39), 1.25f));
            for (int col = 0; col < 3; col++) {
                TextView value = text("0", row == 2 ? 17 : 15, INK, row == 2);
                value.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                line.addView(value, new LinearLayout.LayoutParams(0, dp(39), 1f));
                summaryValues[row * 3 + col] = value;
            }
            if (row == 2) {
                line.setBackground(roundRect(BG, Color.TRANSPARENT, 0, 6));
            }
            card.addView(line, matchWrap());
        }
        return card;
    }

    private void renderAll() {
        if (imflContainer == null || beerContainer == null) return;
        imflContainer.removeAllViews();
        beerContainer.removeAllViews();
        productViews.clear();
        for (SizeItem size : sizes) {
            View card = buildProductCard(size);
            if (size.type.equals("imfl")) imflContainer.addView(card, matchWrap());
            else beerContainer.addView(card, matchWrap());
        }
        updateNumbers();
    }

    private void updateNumbers() {
        long grand = 0, imflBottles = 0, beerBottles = 0;
        long imflCases = 0, beerCases = 0, imflLoose = 0, beerLoose = 0;
        for (SizeItem size : sizes) {
            String key = size.key();
            long cases = sum(entries.get(key));
            long loose = sum(entries.get(looseKey(key)));
            long bottles = safeAdd(safeMultiply(cases, size.perCase), loose);
            ProductViews views = productViews.get(key);
            if (views != null) {
                views.bottleCount.setText(String.valueOf(bottles));
                views.breakdown.setText(cases + (cases == 1 ? " case" : " cases") + " + " + loose + " loose");
                populateChips(views.caseChips, entries.get(key), key, false);
                populateChips(views.looseChips, entries.get(looseKey(key)), looseKey(key), true);
                boolean hasLoose = !entries.get(looseKey(key)).isEmpty();
                if (hasLoose && views.loosePanel.getVisibility() != View.VISIBLE) {
                    views.loosePanel.setVisibility(View.VISIBLE);
                    views.looseToggle.setText("ADDITION −");
                }
            }
            grand = safeAdd(grand, bottles);
            if (size.type.equals("imfl")) {
                imflBottles = safeAdd(imflBottles, bottles);
                imflCases = safeAdd(imflCases, cases);
                imflLoose = safeAdd(imflLoose, loose);
            } else {
                beerBottles = safeAdd(beerBottles, bottles);
                beerCases = safeAdd(beerCases, cases);
                beerLoose = safeAdd(beerLoose, loose);
            }
        }
        long totalCases = safeAdd(imflCases, beerCases);
        long totalLoose = safeAdd(imflLoose, beerLoose);
        long[] values = {imflCases, imflLoose, imflBottles, beerCases, beerLoose, beerBottles,
                totalCases, totalLoose, grand};
        if (summaryValues != null) {
            for (int i = 0; i < values.length; i++) summaryValues[i].setText(String.valueOf(values[i]));
        }
        if (grandValue != null) grandValue.setText(String.valueOf(grand));
        if (grandSplit != null) {
            grandSplit.setText(grand == 0 ? "" : "IMFL " + imflBottles + "  ·  Beer " + beerBottles + "  ·  Loose " + totalLoose);
        }
    }

    private void populateChips(FlowLayout flow, ArrayList<Integer> values, String key, boolean loose) {
        flow.removeAllViews();
        if (values == null) return;
        for (int i = 0; i < values.size(); i++) {
            final int index = i;
            int accent = key.toLowerCase().contains("imfl") ? IMFL : BEER;
            TextView chip = text(values.get(i) + "   ×", 13, accent, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(11), dp(6), dp(11), dp(6));
            chip.setBackground(roundRect(BG, LINE, 1, 18));
            chip.setContentDescription("Remove entry " + values.get(i));
            chip.setOnClickListener(v -> {
                ArrayList<Integer> current = entries.get(key);
                if (current != null && index < current.size()) {
                    current.remove(index);
                    saveEntries();
                    updateNumbers();
                }
            });
            flow.addView(chip, new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private void addFromInput(String key, EditText input) {
        String raw = input.getText().toString().trim();
        if (raw.isEmpty()) {
            input.requestFocus();
            Toast.makeText(this, "Enter a quantity first", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            long parsed = Long.parseLong(raw);
            if (parsed < 1 || parsed > 1000000) {
                Toast.makeText(this, "Enter a number from 1 to 1,000,000", Toast.LENGTH_SHORT).show();
                return;
            }
            entries.get(key).add((int) parsed);
            undoKeys.add(key);
            input.setText("");
            saveEntries();
            updateNumbers();
            input.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Enter a whole number", Toast.LENGTH_SHORT).show();
        }
    }

    private void undoLast() {
        while (!undoKeys.isEmpty()) {
            String key = undoKeys.remove(undoKeys.size() - 1);
            ArrayList<Integer> current = entries.get(key);
            if (current != null && !current.isEmpty()) {
                current.remove(current.size() - 1);
                saveEntries();
                updateNumbers();
                return;
            }
        }
        Toast.makeText(this, "Nothing to undo in this session", Toast.LENGTH_SHORT).show();
    }

    private void confirmClear() {
        if (!hasEntries()) {
            Toast.makeText(this, "The bill is already empty", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Clear this bill?")
                .setMessage("All case and loose-bottle entries on this screen will be removed from this phone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear", (dialog, which) -> {
                    for (SizeItem size : sizes) {
                        entries.get(size.key()).clear();
                        entries.get(looseKey(size.key())).clear();
                    }
                    undoKeys.clear();
                    saveEntries();
                    renderAll();
                })
                .show();
    }

    private boolean hasEntries() {
        for (ArrayList<Integer> values : entries.values()) if (!values.isEmpty()) return true;
        return false;
    }

    private long sum(ArrayList<Integer> values) {
        long result = 0;
        if (values != null) for (Integer value : values) result = safeAdd(result, value);
        return result;
    }

    private long safeMultiply(long a, long b) {
        if (a != 0 && b > Long.MAX_VALUE / a) return Long.MAX_VALUE;
        return a * b;
    }

    private long safeAdd(long a, long b) {
        if (b > 0 && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }

    private String looseKey(String key) { return "L" + key; }

    private TextView text(String label, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String label, int background, int foreground, int stroke) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setTextColor(foreground);
        button.setAllCaps(false);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(roundRect(background, stroke, 1, 10));
        button.setMinHeight(dp(44));
        button.setMinimumHeight(dp(44));
        return button;
    }

    private GradientDrawable roundRect(int color, int strokeColor, int strokeWidth, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (strokeWidth > 0) drawable.setStroke(dp(strokeWidth), strokeColor);
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class SizeItem {
        final int ml;
        final int perCase;
        final String type;
        SizeItem(int ml, int perCase, String type) { this.ml = ml; this.perCase = perCase; this.type = type; }
        String key() { return type + ml; }
    }

    private static final class ProductViews {
        final SizeItem size;
        final TextView bottleCount;
        final TextView breakdown;
        final FlowLayout caseChips;
        final FlowLayout looseChips;
        final LinearLayout loosePanel;
        final Button looseToggle;
        final EditText caseInput;
        final EditText looseInput;
        ProductViews(SizeItem size, TextView bottleCount, TextView breakdown, FlowLayout caseChips,
                     FlowLayout looseChips, LinearLayout loosePanel, Button looseToggle,
                     EditText caseInput, EditText looseInput) {
            this.size = size; this.bottleCount = bottleCount; this.breakdown = breakdown;
            this.caseChips = caseChips; this.looseChips = looseChips; this.loosePanel = loosePanel;
            this.looseToggle = looseToggle; this.caseInput = caseInput; this.looseInput = looseInput;
        }
    }

    /** Small native wrapping layout for entry chips; avoids an external UI dependency. */
    private static final class FlowLayout extends ViewGroup {
        private final int hGap;
        private final int vGap;
        FlowLayout(Context context) { super(context); hGap = dpStatic(context, 6); vGap = dpStatic(context, 5); }

        @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int available = Math.max(0, width - getPaddingLeft() - getPaddingRight());
            int x = 0, rowHeight = 0, totalHeight = getPaddingTop() + getPaddingBottom();
            int maxX = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) continue;
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                int childW = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
                int childH = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
                if (x > 0 && x + hGap + childW > available) {
                    totalHeight += rowHeight + vGap;
                    x = 0; rowHeight = 0;
                }
                if (x > 0) x += hGap;
                x += childW;
                maxX = Math.max(maxX, x);
                rowHeight = Math.max(rowHeight, childH);
            }
            totalHeight += rowHeight;
            int desiredW = maxX + getPaddingLeft() + getPaddingRight();
            setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(totalHeight, heightMeasureSpec));
        }

        @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int available = r - l - getPaddingLeft() - getPaddingRight();
            int x = getPaddingLeft(), y = getPaddingTop(), rowHeight = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) continue;
                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                int cw = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
                int ch = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
                if (x > getPaddingLeft() && x - getPaddingLeft() + hGap + cw > available) {
                    x = getPaddingLeft();
                    y += rowHeight + vGap;
                    rowHeight = 0;
                }
                if (x > getPaddingLeft()) x += hGap;
                int left = x + lp.leftMargin;
                int top = y + lp.topMargin;
                child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
                x += cw;
                rowHeight = Math.max(rowHeight, ch);
            }
        }

        @Override protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
            return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }
        @Override public ViewGroup.LayoutParams generateLayoutParams(android.util.AttributeSet attrs) {
            return new MarginLayoutParams(getContext(), attrs);
        }
        @Override protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
            return new MarginLayoutParams(p);
        }
        @Override protected boolean checkLayoutParams(ViewGroup.LayoutParams p) { return p instanceof MarginLayoutParams; }
        private static int dpStatic(Context context, float value) {
            return Math.round(value * context.getResources().getDisplayMetrics().density);
        }
    }
}
