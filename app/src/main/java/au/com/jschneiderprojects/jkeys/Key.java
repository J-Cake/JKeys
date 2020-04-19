package au.com.jschneiderprojects.jkeys;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;

public class Key extends LinearLayout {

    String keyName;
    String keyValue;
    int drawable;
    int background;

    View keyContent;

    Thread pressHandler;

    LayoutInflater inflater;

    LinearLayout keyContainer;

    Drawable defaultBackground;

    boolean isUpperCase;

    boolean repeats;

    int repeatCount;

    boolean isKeyDown;

    public Key(Context context, AttributeSet attrs) {
        super(context, attrs);

        isUpperCase = false;
        isKeyDown = false;
        repeatCount = 0;
        this.inflater = LayoutInflater.from(context);

        this.init(attrs);

        this.defaultBackground = context.getDrawable(R.drawable.key);

        if (this.drawable > -1 && context.getDrawable(this.drawable) != null) {
            this.keyContainer = (LinearLayout) inflate(context, R.layout.drawable_key, this);

            AppCompatImageView drawableContainer = findViewById(R.id.drawable);

            drawableContainer.setImageResource(this.drawable);

            this.keyContent = findViewById(R.id.keyText);
        } else {
            this.keyContainer = (LinearLayout) inflate(context, R.layout.text_key, this);

            this.keyContent = findViewById(R.id.keyText);

            ((TextView) this.keyContent).setText(this.keyName);
        }

        if (this.background > -1 && context.getDrawable(this.background) != null)
            findViewById(R.id.keyText).setBackground(context.getDrawable(this.background));

    }

    private void init(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.Key);

        String keyValue = typedArray.getString(R.styleable.Key_symbol);
        String keyName = typedArray.getString(R.styleable.Key_display);
        int drawable = typedArray.getResourceId(R.styleable.Key_drawable, -1);
        int background = typedArray.getResourceId(R.styleable.Key_background, -1);
        boolean repeating = typedArray.getBoolean(R.styleable.Key_repeat, false);

        this.keyValue = keyValue;
        this.keyName = keyName;
        this.drawable = drawable;
        this.background = background;
        this.repeats = repeating;

        typedArray.recycle();
    }

    String getSymbol() {
        if (!this.isUpperCase) {
            if (this.keyValue == null)
                return this.keyName.toLowerCase();
            else
                return this.keyValue.toLowerCase();
        } else {
            if (this.keyValue == null)
                return this.keyName.toUpperCase();
            else
                return this.keyValue.toUpperCase();
        }
    }

    void keyDown() {
        this.isKeyDown = true;
        keyContent.setBackground(getContext().getDrawable(R.drawable.key_pressed));
    }

    void keyUp() {
        this.isKeyDown = false;
        keyContent.setBackground(this.defaultBackground);
        this.repeatCount = 0;
    }

    public void setShift(boolean isUpperCase) {
        this.isUpperCase = isUpperCase;

        if (this.keyContent instanceof TextView)
            if (isUpperCase)
                ((TextView) this.keyContent).setText(this.keyName.toUpperCase());
            else
                ((TextView) this.keyContent).setText(this.keyName.toLowerCase());
    }
}
