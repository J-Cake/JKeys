package au.com.jschneiderprojects.jkeys;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class KeyboardService extends InputMethodService implements View.OnTouchListener {

    InputConnection input;

    ArrayList<Key> keys = new ArrayList<>();

    Vibrator vibration;

    int[] specialKeys = {R.id.backspace, R.id.space, R.id.done, R.id.shift, R.id.more, R.id.less, R.id.cycle_page};

    boolean shift;
    boolean caps;

    static long repeatDelay = 600L;
    static long repeatInterval = 1000L / 25L;

    LinearLayout[] panels;
    LinearLayout keyboard;

    int symbolIndex = 0;
    LinearLayout[] symbols;

    @Override
    public View onCreateInputView() {
        initPanels();

        this.input = getCurrentInputConnection();

//        setCandidatesViewShown(true);

        this.stepIntoChild(keyboard);

        this.vibration = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        this.shift = false;
        this.caps = false;

        return keyboard;
    }

    @Override
    public View onCreateCandidatesView() {

        return getLayoutInflater().inflate(R.layout.candidates, null);
    }

    private void initPanels() {

        LinearLayout alphanumeric = (LinearLayout) getLayoutInflater().inflate(R.layout.alphanumeric_keyboard, null);

        LinearLayout symbolic1 = (LinearLayout) getLayoutInflater().inflate(R.layout.symbol_page_1_keyboard, null);
        LinearLayout symbolic2 = (LinearLayout) getLayoutInflater().inflate(R.layout.symbol_page_2_keyboard, null);

        symbols = new LinearLayout[]{
                symbolic1,
                symbolic2
        };

        panels = new LinearLayout[]{
                alphanumeric,
                symbolic1,
                symbolic2
        };

        for (LinearLayout panel : panels)
            stepIntoChild(panel);

        this.keyboard = alphanumeric;
        this.symbolIndex = -1;
    }

    void vibrate() {
        int duration = 15;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.vibration.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            this.vibration.vibrate(duration);
        }
    }

    void sendKey(String key) {
        vibrate();

        this.input.commitText(key, 1);
    }

    private void stepIntoChild(View child) {
        if (child instanceof ViewGroup) {
            ViewGroup container = (ViewGroup) child;
            int children = container.getChildCount();

            for (int i = 0; i < children; i++) {
                View childElement = container.getChildAt(i);

                if (childElement instanceof Key) {
                    Key textKey = (Key) childElement;

                    keys.add(textKey);

                    addTouch(textKey);
                } else
                    stepIntoChild(childElement);
            }
        }
    }

    private boolean includes(int[] ids, int id) {
        for (int i : ids)
            if (i == id)
                return true;

        return false;
    }

    public void enableShift() {
        this.shift = true;

        for (Key key : keys)
            key.setShift(true);
    }

    public void disableShift(boolean overrideCaps) {
        this.shift = false;

        if (!this.caps || overrideCaps) {
            this.caps = false;

            for (Key key : keys)
                key.setShift(false);
        }
    }

    void switchPanels(Key k, LinearLayout panel) {
        if (!k.repeats) // due to threading issues, a repeating key can't change panels.
            this.setInputView(panel);
    }

    void keyPressed(Key key) {
        key.repeatCount++;

        if (this.includes(this.specialKeys, key.getId())) {

            Log.d("Special Key", key.getId() + "");
            this.vibrate();

            switch (key.getId()) {
                case R.id.backspace:
                    this.input.deleteSurroundingText(1, 0);
                    return;
                case R.id.done:
                    this.input.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                    return;
                case R.id.space:
                    this.input.commitText(" ", 1);
                    return;
                case R.id.shift:
                    if (this.shift)
                        this.disableShift(false);
                    else
                        this.enableShift();
                    return;
                case R.id.more:
                    symbolIndex = 0;
                    switchPanels(key, panels[1]);
                    return;
                case R.id.less:
                    switchPanels(key, panels[0]);
                    symbolIndex = -1;
                    return;
                case R.id.cycle_page:
                    if (symbolIndex >= 0) {
                        symbolIndex = (symbolIndex + 1) % symbols.length;
                        keyboard = symbols[symbolIndex];
                    } else
                        keyboard = panels[0];

                    switchPanels(key, keyboard);
                    return;

            }

        } else
            this.sendKey(key.getSymbol());

        if (this.shift)
            this.disableShift(false);
    }

    void handleTouch(Key key, MotionEvent event) {
        this.input = getCurrentInputConnection();

        if (event.getAction() == MotionEvent.ACTION_POINTER_DOWN || event.getAction() == MotionEvent.ACTION_DOWN) {
            this.keyPressed(key);

            key.pressHandler = new Thread(() -> {
                if (key.repeats) {
                    try {
                        Thread.sleep(KeyboardService.repeatDelay);

                        while (key.isKeyDown) {
                            this.keyPressed(key);
                            Thread.sleep(KeyboardService.repeatInterval);
                        }
                    } catch (Exception ignored) {
                    }
                }
            });

            key.pressHandler.start();
        }
    }

    private void addTouch(Key key) {
        final KeyboardService keyboard = this;

        key.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.performClick();

                Key k = (Key) v;

                if (event.getAction() == MotionEvent.ACTION_DOWN)
                    k.keyDown();
                else if (event.getAction() == MotionEvent.ACTION_UP)
                    k.keyUp();

                keyboard.handleTouch(k, event);

                return true;
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        // Handle Gesture typing here

        v.performClick();

        return false;
    }

    private void cleanup() {
        this.caps = false;
        this.shift = false;
        this.input = null;

        for (Key key : keys)
            key.setShift(false);
    }

    @Override
    public void onFinishInput() {
        cleanup();
    }

    @Override
    public void onFinishInputView(boolean finishingView) {
        this.cleanup();
    }
}