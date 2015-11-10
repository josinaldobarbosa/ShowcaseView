/*
 * Copyright 2014 Alex Curran
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.amlcurran.showcaseview;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.text.Layout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.github.amlcurran.showcaseview.targets.Target;

import java.util.ArrayList;

import static com.github.amlcurran.showcaseview.AnimationFactory.AnimationEndListener;
import static com.github.amlcurran.showcaseview.AnimationFactory.AnimationStartListener;

/**
 * A view which allows you to showcase areas of your app with an explanation.
 */
public class ShowcaseView extends RelativeLayout
        implements View.OnTouchListener, ShowcaseViewApi {

    private static final int HOLO_BLUE = Color.parseColor("#33B5E5");
    public static final int UNDEFINED = -1;
    public static final int LEFT_OF_SHOWCASE = 0;
    public static final int RIGHT_OF_SHOWCASE = 2;
    public static final int ABOVE_SHOWCASE = 1;
    public static final int BELOW_SHOWCASE = 3;


    //Arrows
    public static final int MARGIN_FROM_TEXT = 16;
    public static final int MARGIN_FROM_SPOT = 100;

    private View mEndView;
    private final TextDrawer textDrawer;
    private ShowcaseDrawer showcaseDrawer;
    private final ShowcaseAreaCalculator showcaseAreaCalculator;
    private final AnimationFactory animationFactory;
    private final ShotStateStore shotStateStore;

    // Showcase metrics
    ArrayList<Point> showcases = new ArrayList<>();
    private float scaleMultiplier = 1f;

    // Text position
    private Point textItemPosition = new Point(-1, -1); // TODO setado como default

    // Touch items
    private boolean hasCustomClickListener = false;
    private boolean blockTouches = true;
    private boolean hideOnTouch = false;
    private OnShowcaseEventListener mEventListener = OnShowcaseEventListener.NONE;

    private boolean hasAlteredText = false;
    private boolean hasNoTarget = false;
    private boolean shouldCentreText;
    private Bitmap bitmapBuffer;

    // Animation items
    private long fadeInMillis;
    private long fadeOutMillis;
    private boolean isShowing;
    private int backgroundColor;
    private int showcaseColor;
    private boolean blockAllTouches;

    protected ShowcaseView(Context context, boolean newStyle) {
        this(context, null, R.styleable.CustomTheme_showcaseViewStyle, newStyle);
    }

    protected ShowcaseView(Context context, AttributeSet attrs, int defStyle, boolean newStyle) {
        super(context, attrs, defStyle);

        ApiUtils apiUtils = new ApiUtils();
        animationFactory = new AnimatorAnimationFactory();
        showcaseAreaCalculator = new ShowcaseAreaCalculator();
        shotStateStore = new ShotStateStore(context);

        apiUtils.setFitsSystemWindowsCompat(this);
        getViewTreeObserver().addOnPreDrawListener(new CalculateTextOnPreDraw());
        getViewTreeObserver().addOnGlobalLayoutListener(new UpdateOnGlobalLayout());

        // Get the attributes for the ShowcaseView
        final TypedArray styled = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.ShowcaseView, R.attr.showcaseViewStyle,
                        R.style.ShowcaseView);

        // Set the default animation times
        fadeInMillis = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        fadeOutMillis = getResources().getInteger(android.R.integer.config_mediumAnimTime);

        mEndView = LayoutInflater.from(context).inflate(R.layout.showcase_button, null);
        if (newStyle) {
            showcaseDrawer = new NewShowcaseDrawer(getResources());
        } else {
            showcaseDrawer = new StandardShowcaseDrawer(getResources());
        }
        textDrawer = new TextDrawer(getResources(), showcaseAreaCalculator, getContext());

        updateStyle(styled, false);

        init();
    }

    private void init() {

        setOnTouchListener(this);

        if (mEndView.getParent() == null) {
            int margin = (int) getResources().getDimension(R.dimen.button_margin);
            RelativeLayout.LayoutParams lps = (LayoutParams) generateDefaultLayoutParams();
            lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            lps.setMargins(margin, margin, margin, margin);
            mEndView.setLayoutParams(lps);
            setTextButtonClose(android.R.string.ok);
            if (!hasCustomClickListener) {
                mEndView.setOnClickListener(hideOnClickListener);
            }
            addView(mEndView);
        }

    }

    private boolean hasShot() {
        return shotStateStore.hasShot();
    }

    void setShowcasePosition(Target target) {
        if (shotStateStore.hasShot()) {
            return;
        }

        showcases.add(target.getPoint());

        //init();
        invalidate();
    }

    public void setTarget(final Target... target) {
        setShowcase(false, target);
    }

    public void setShowcase(final boolean animate, final Target... target) {
        postDelayed(new Runnable() {
            @Override
            public void run() {

                if (!shotStateStore.hasShot()) {

                    updateBitmap();

                    if (target != null) {
                        hasNoTarget = false;

                        for (Target t : target) {
                            if (animate) {
                                animationFactory.animateTargetToPoint(ShowcaseView.this, t.getPoint());
                            } else {
                                setShowcasePosition(t);
                            }
                        }

                    } else {
                        hasNoTarget = true;
                        invalidate();
                    }

                }
            }
        }, 100);
    }

    private void updateBitmap() {
        if (bitmapBuffer == null || haveBoundsChanged()) {
            if (bitmapBuffer != null)
                bitmapBuffer.recycle();
            bitmapBuffer = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        }
    }

    private boolean haveBoundsChanged() {
        return getMeasuredWidth() != bitmapBuffer.getWidth() ||
                getMeasuredHeight() != bitmapBuffer.getHeight();
    }

    public boolean hasShowcaseView() {
        for (Point point : showcases) {
            if ((point.x != 1000000 && point.y != 1000000) && !hasNoTarget) {
                return false;
            }
        }

        return true;
    }

    /**
     * Override the standard button click event
     *
     * @param listener Listener to listen to on click events
     */
    public void overrideButtonClick(OnClickListener listener) {
        if (shotStateStore.hasShot()) {
            return;
        }
        if (mEndView != null) {
            if (listener != null) {
                mEndView.setOnClickListener(listener);
            } else {
                mEndView.setOnClickListener(hideOnClickListener);
            }
        }
        hasCustomClickListener = true;
    }

    public void setOnShowcaseEventListener(OnShowcaseEventListener listener) {
        if (listener != null) {
            mEventListener = listener;
        } else {
            mEventListener = OnShowcaseEventListener.NONE;
        }
    }

    public void setButtonText(CharSequence text) {
        setTextButtonClose(text.toString());
    }

    private void recalculateText() {
        boolean recalculatedCling = showcaseAreaCalculator.calculateShowcaseRect(textItemPosition.x, textItemPosition.y, showcaseDrawer);
        boolean recalculateText = recalculatedCling || hasAlteredText;
        if (recalculateText) {
            textDrawer.calculateTextPosition(getMeasuredWidth(), getMeasuredHeight(), this, shouldCentreText);
        }
        hasAlteredText = false;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    protected void dispatchDraw(Canvas canvas) {

        // Checks if is valid
        for (Point p : showcases) {
            if (p.x < 0 || p.y < 0 || shotStateStore.hasShot() || bitmapBuffer == null) {
                super.dispatchDraw(canvas);
                return;
            }
        }

        // Draw background color
        showcaseDrawer.erase(bitmapBuffer);

        // Draw the showcase drawable
        if (!hasNoTarget) {

            for (Point p : showcases) {
                showcaseDrawer.drawShowcase(bitmapBuffer, p.x, p.y, scaleMultiplier);
            }

            showcaseDrawer.drawToCanvas(canvas, bitmapBuffer);
        }

        // Draw the text on the screen, recalculating its position if necessary
        textDrawer.draw(canvas);


        //Draw arrows

        if (!hasNoTarget) {
            float[] textPosition = this.textDrawer.getBestTextPosition();

            final int INDEX_TEXT_START_X = 0;
            final int INDEX_TEXT_START_Y = 1;
            final int INDEX_TEXT_WIDTH = 2;

            float midx = textDrawer.getCalculateTextPosition(textPosition[INDEX_TEXT_START_X], textDrawer.getCompensationTextPositionWidth());
            float midy = textDrawer.getCalculateTextPosition(textPosition[INDEX_TEXT_START_Y], textDrawer.getCompensationTextPositionHeight());
            float width = textPosition[INDEX_TEXT_WIDTH];

            if (!showcases.isEmpty()) {
                createArrow(canvas, midx - (width / 4), (int) midy, showcases.get(1).x, (int) (showcases.get(1).y + (showcaseDrawer.getBlockedRadius() / 2)), width);
                createArrow(canvas, midx, (int) midy, showcases.get(2).x, (int) (showcases.get(2).y + (showcaseDrawer.getBlockedRadius() / 2)), width);
                createArrow(canvas, midx + (width / 4), (int) midy, showcases.get(3).x, (int) (showcases.get(3).y + (showcaseDrawer.getBlockedRadius() / 2)), width);
            }
        }

        super.dispatchDraw(canvas);

    }


    private void createArrow(Canvas canvas, float x, int y, int xend, int yend, float width) {
        Paint paint = getPaint();

        Point startPoint = new Point((int) (x + (width / 2)), y - MARGIN_FROM_TEXT);
        Point endPoint = new Point(xend, yend + MARGIN_FROM_SPOT);

        Log.d("LINE ANGLE", "" + angleMadeByLine(startPoint, endPoint));
        double angle = angleMadeByLine(startPoint, endPoint);

        Point a = new Point(startPoint.x, startPoint.y);
        Point b = new Point(endPoint.x, endPoint.y);
        Point c = new Point(endPoint.x, endPoint.y);
        Point d = new Point(endPoint.x, endPoint.y);

        applyCompensation(c, -15, 21);
        applyCompensation(d, +15, 21);


        float[] lineRotated1 = {};
        float[] lineRotated2 = {};

        //I need to draw a couple of lines A --> B , B --> C , B --> D

        if (angle < 270) {
            lineRotated1 = getRotatedPoints(angle + 60, b, c);
            lineRotated2 = getRotatedPoints(angle + 60, b, d);
            canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, paint);
            canvas.drawLine(endPoint.x, endPoint.y, lineRotated1[2], lineRotated1[3], paint);
            canvas.drawLine(endPoint.x, endPoint.y, lineRotated2[2], lineRotated2[3], paint);
            c = new Point((int) lineRotated1[2], (int) lineRotated1[3]);
            d = new Point((int) lineRotated2[2], (int) lineRotated2[3]);

            drawPath(canvas, paint, a, b, c, d);
            return;
        }

        if (angle > 270) {
            lineRotated1 = getRotatedPoints(-angle, b, c);
            lineRotated2 = getRotatedPoints(-angle, b, d);

            canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, paint);
            canvas.drawLine(endPoint.x, endPoint.y, lineRotated1[2], lineRotated1[3], paint);
            canvas.drawLine(endPoint.x, endPoint.y, lineRotated2[2], lineRotated2[3], paint);

            c = new Point((int) lineRotated1[2], (int) lineRotated1[3]);
            d = new Point((int) lineRotated2[2], (int) lineRotated2[3]);

            drawPath(canvas, paint, a, b, c, d);

            return;
        }

        drawPath(canvas, paint, a, b, c, d);

        canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, paint);
        canvas.drawLine(endPoint.x, endPoint.y, c.x, c.y, paint);
        canvas.drawLine(endPoint.x, endPoint.y, d.x, d.y, paint);
    }

    private void applyCompensation(Point point, int xvalue, int yvalue) {
        point.x += xvalue;
        point.y += yvalue;
    }

    private Paint getPaint() {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(4);
        paint.setStyle(Paint.Style.STROKE);
        paint.setDither(false);
        paint.setAntiAlias(true);
        float radius = 8.0f;
        CornerPathEffect corEffect = new CornerPathEffect(radius);
        paint.setPathEffect(corEffect);
        return paint;
    }

    private void drawPath(Canvas canvas, Paint paint, final Point... points) {
        if (points != null && points.length > 2) {
            Path path = new Path();
            path.setFillType(Path.FillType.EVEN_ODD);

            path.moveTo(points[0].x, points[0].y);
            path.lineTo(points[1].x, points[1].y);
            path.lineTo(points[2].x, points[2].y);
            path.moveTo(points[1].x, points[1].y);
            path.lineTo(points[3].x, points[3].y);

            canvas.drawPath(path, paint);
        }
    }

    private float[] getRotatedPoints(double angle, Point a, Point b) {
        float[] linePoints = new float[]{a.x, a.y, b.x, b.y};

        //get the center of the line
        float centerX = Math.abs((a.x + b.x) / 2);
        float centerY = Math.abs(a.y + b.y) / 2;

        //create the matrix
        Matrix rotateMat = new Matrix();

        //rotate the matrix around the center
        rotateMat.setRotate((float) angle, centerX, centerY);
        rotateMat.mapPoints(linePoints);

        //draw the line
        return linePoints;
    }

    private double angleMadeByLine(Point a, Point b) {
        double dx = b.x - a.x;
        // Minus to correct for coord re-mapping
        double dy = -(b.y - a.y);

        double inRads = Math.atan2(dy, dx);

        // We need to map to coord system when 0 degree is at 3 O'clock, 270 at 12 O'clock
        if (inRads < 0)
            inRads = Math.abs(inRads);
        else
            inRads = 2 * Math.PI - inRads;

        return Math.toDegrees(inRads);
    }


    @Override
    public void hide() {
        clearBitmap();
        // If the type is set to one-shot, store that it has shot
        shotStateStore.storeShot();
        mEventListener.onShowcaseViewHide(this);
        fadeOutShowcase();
    }

    private void clearBitmap() {
        if (bitmapBuffer != null && !bitmapBuffer.isRecycled()) {
            bitmapBuffer.recycle();
            bitmapBuffer = null;
        }
    }

    private void fadeOutShowcase() {
        animationFactory.fadeOutView(
                this, fadeOutMillis, new AnimationEndListener() {
                    @Override
                    public void onAnimationEnd() {
                        setVisibility(View.GONE);
                        isShowing = false;
                        mEventListener.onShowcaseViewDidHide(ShowcaseView.this);
                    }
                }
        );
    }

    @Override
    public void show() {
        isShowing = true;
        mEventListener.onShowcaseViewShow(this);
        fadeInShowcase();
    }

    private void fadeInShowcase() {
        animationFactory.fadeInView(
                this, fadeInMillis,
                new AnimationStartListener() {
                    @Override
                    public void onAnimationStart() {
                        setVisibility(View.VISIBLE);
                    }
                }
        );
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (blockAllTouches) {
            return true;
        }

        boolean result = false;

        for (Point showcase : showcases) {
            float xDelta = Math.abs(motionEvent.getRawX() - showcase.x);
            float yDelta = Math.abs(motionEvent.getRawY() - showcase.y);
            double distanceFromFocus = Math.sqrt(Math.pow(xDelta, 2) + Math.pow(yDelta, 2));

            if (MotionEvent.ACTION_UP == motionEvent.getAction() &&
                    hideOnTouch && distanceFromFocus > showcaseDrawer.getBlockedRadius()) {
                this.hide();
                return true;
            }

            result = blockTouches && distanceFromFocus > showcaseDrawer.getBlockedRadius();
        }

        return result; // returning the last, is this a problem?
    }

    private static void insertShowcaseView(ShowcaseView showcaseView, ViewGroup parent, int parentIndex) {
        parent.addView(showcaseView, parentIndex);
        if (!showcaseView.hasShot()) {
            showcaseView.show();
        } else {
            showcaseView.hideImmediate();
        }
    }

    private void hideImmediate() {
        isShowing = false;
        setVisibility(GONE);
    }

    @Override
    public void setContentTitle(CharSequence title) {
        textDrawer.setContentTitle(title);
    }

    @Override
    public void setContentText(CharSequence text) {
        textDrawer.setContentText(text);
    }

    private void setScaleMultiplier(float scaleMultiplier) {
        this.scaleMultiplier = scaleMultiplier;
    }

    public void hideButton() {
        mEndView.setVisibility(GONE);
    }

    public void showButton() {
        mEndView.setVisibility(VISIBLE);
    }

    /**
     * Builder class which allows easier creation of {@link ShowcaseView}s.
     * It is recommended that you use this Builder class.
     */
    public static class Builder {

        final ShowcaseView showcaseView;
        private final Activity activity;

        private ViewGroup parent;
        private int parentIndex;

        public Builder(Activity activity) {
            this(activity, false);
        }

        /**
         * @param useNewStyle should use "new style" showcase (see {@link #withNewStyleShowcase()}
         * @deprecated use {@link #withHoloShowcase()}, {@link #withNewStyleShowcase()}, or
         * {@link #setShowcaseDrawer(ShowcaseDrawer)}
         */
        @Deprecated
        public Builder(Activity activity, boolean useNewStyle) {
            this.activity = activity;
            this.showcaseView = new ShowcaseView(activity, useNewStyle);
            this.showcaseView.setTarget(Target.NONE);

            this.parent = ((ViewGroup) activity.getWindow().getDecorView());
            this.parentIndex = -1;
        }

        /**
         * Create the {@link com.github.amlcurran.showcaseview.ShowcaseView} and show it.
         *
         * @return the created ShowcaseView
         */
        public ShowcaseView build() {
            insertShowcaseView(showcaseView, parent, parentIndex);
            return showcaseView;
        }

        /**
         * Draw a holo-style showcase. This is the default.<br/>
         * <img alt="Holo showcase example" src="../../../../../../../../example2.png" />
         */
        public Builder withHoloShowcase() {
            return setShowcaseDrawer(new StandardShowcaseDrawer(activity.getResources()));
        }

        /**
         * Draw a new-style showcase.<br/>
         * <img alt="Holo showcase example" src="../../../../../../../../example.png" />
         */
        public Builder withNewStyleShowcase() {
            return setShowcaseDrawer(new NewShowcaseDrawer(activity.getResources()));
        }

        /**
         * Draw a material style showcase.
         * <img alt="Material showcase" src="../../../../../../../../material.png" />
         */
        public Builder withMaterialShowcase() {
            return setShowcaseDrawer(new MaterialShowcaseDrawer(activity.getResources()));
        }

        /**
         * Set a custom showcase drawer which will be responsible for measuring and drawing the showcase
         */
        public Builder setShowcaseDrawer(ShowcaseDrawer showcaseDrawer) {
            showcaseView.setShowcaseDrawer(showcaseDrawer);
            return this;
        }

        /**
         * Set the title text shown on the ShowcaseView.
         */
        public Builder setContentTitle(int resId) {
            return setContentTitle(activity.getString(resId));
        }

        /**
         * Set the title text shown on the ShowcaseView.
         */
        public Builder setContentTitle(CharSequence title) {
            showcaseView.setContentTitle(title);
            return this;
        }

        /**
         * Set the descriptive text shown on the ShowcaseView.
         */
        public Builder setContentText(int resId) {
            return setContentText(activity.getString(resId));
        }

        /**
         * Set the descriptive text shown on the ShowcaseView.
         */
        public Builder setContentText(CharSequence text) {
            showcaseView.setContentText(text);
            return this;
        }

        /**
         * Set the target of the showcase.
         *
         * @param target a {@link com.github.amlcurran.showcaseview.targets.Target} representing
         *               the item to showcase (e.g., a button, or action item).
         */
        public Builder setTarget(Target... target) {
            showcaseView.setTarget(target);
            return this;
        }

        /**
         * Set the style of the ShowcaseView. See the sample app for example styles.
         */
        public Builder setStyle(int theme) {
            showcaseView.setStyle(theme);
            return this;
        }

        /**
         * Set a listener which will override the button clicks.
         * <p/>
         * Note that you will have to manually hide the ShowcaseView
         */
        public Builder setOnClickListener(OnClickListener onClickListener) {
            showcaseView.overrideButtonClick(onClickListener);
            return this;
        }

        /**
         * Don't make the ShowcaseView block touches on itself. This doesn't
         * block touches in the showcased area.
         * <p/>
         * By default, the ShowcaseView does block touches
         */
        public Builder doNotBlockTouches() {
            showcaseView.setBlocksTouches(false);
            return this;
        }

        /**
         * Make this ShowcaseView hide when the user touches outside the showcased area.
         * This enables {@link #doNotBlockTouches()} as well.
         * <p/>
         * By default, the ShowcaseView doesn't hide on touch.
         */
        public Builder hideOnTouchOutside() {
            showcaseView.setBlocksTouches(true);
            showcaseView.setHideOnTouchOutside(true);
            return this;
        }

        /**
         * Set the ShowcaseView to only ever show once.
         *
         * @param shotId a unique identifier (<em>across the app</em>) to store
         *               whether this ShowcaseView has been shown.
         */
        public Builder singleShot(long shotId) {
            showcaseView.setSingleShot(shotId);
            return this;
        }

        public Builder setShowcaseEventListener(OnShowcaseEventListener showcaseEventListener) {
            showcaseView.setOnShowcaseEventListener(showcaseEventListener);
            return this;
        }

        public Builder setParent(ViewGroup parent, int index) {
            this.parent = parent;
            this.parentIndex = index;
            return this;
        }

        /**
         * Sets the paint that will draw the text as specified by {@link #setContentText(CharSequence)}
         * or {@link #setContentText(int)}
         */
        public Builder setContentTextPaint(TextPaint textPaint) {
            showcaseView.setContentTextPaint(textPaint);
            return this;
        }

        /**
         * Sets the paint that will draw the text as specified by {@link #setContentTitle(CharSequence)}
         * or {@link #setContentTitle(int)}
         */
        public Builder setContentTitlePaint(TextPaint textPaint) {
            showcaseView.setContentTitlePaint(textPaint);
            return this;
        }

        /**
         * Sets the conpensation in position of text
         * 0 = not modify position
         * width positive = move text to right
         * width negative = move text to left
         * heigth positive = move text to down
         * heigth negative = move text to up
         */
        public Builder setCompensationTextPosition(int width, int heigth) {
            float mWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width, activity.getResources().getDisplayMetrics());
            float mHeigth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heigth, activity.getResources().getDisplayMetrics());

            showcaseView.setCompensationTextPosition(mWidth, mHeigth);
            return this;
        }

        /**
         * Replace the end button with the one provided. Note that this resets any OnClickListener provided
         * by {@link #setOnClickListener(OnClickListener)}, so call this method before that one.
         */
        public Builder replaceEndButton(Button button) {
            showcaseView.setEndButton(button);
            return this;
        }

        /**
         * Replace the end view with the one provided. Note that this resets any OnClickListener provided
         * by {@link #setOnClickListener(OnClickListener)}, so call this method before that one.
         */
        public Builder replaceEndButton(View viewEnd) {
            showcaseView.setEndButton(viewEnd);
            return this;
        }

        /**
         * Replace the end button with the one provided. Note that this resets any OnClickListener provided
         * by {@link #setOnClickListener(OnClickListener)}, so call this method before that one.
         */
        public Builder replaceEndButton(int buttonResourceId) {
            View view = LayoutInflater.from(activity).inflate(buttonResourceId, showcaseView, false);
            if (!(view instanceof Button)) {
                throw new IllegalArgumentException("Attempted to replace showcase button with a layout which isn't a button");
            }
            return replaceEndButton((Button) view);
        }

        /**
         * Block any touch made on the ShowcaseView, even inside the showcase
         */
        public Builder blockAllTouches() {
            showcaseView.setBlockAllTouches(true);
            return this;
        }

    }

    private void setEndButton(View button) {
        LayoutParams copyParams = (LayoutParams) mEndView.getLayoutParams();
        mEndView.setOnClickListener(null);
        removeView(mEndView);
        mEndView = button;
        button.setOnClickListener(hideOnClickListener);
        button.setLayoutParams(copyParams);
        addView(button);
    }

    private void setShowcaseDrawer(ShowcaseDrawer showcaseDrawer) {
        this.showcaseDrawer = showcaseDrawer;
        this.showcaseDrawer.setBackgroundColour(backgroundColor);
        this.showcaseDrawer.setShowcaseColour(showcaseColor);
        hasAlteredText = true;
        invalidate();
    }

    private void setContentTitlePaint(TextPaint textPaint) {
        this.textDrawer.setTitlePaint(textPaint);
        hasAlteredText = true;
        invalidate();
    }

    private void setContentTextPaint(TextPaint paint) {
        this.textDrawer.setContentPaint(paint);
        hasAlteredText = true;
        invalidate();
    }

    private void setCompensationTextPosition(float width, float heigth) {
        this.textDrawer.setCompensationTextPosition(width, heigth);
        hasAlteredText = true;
        invalidate();
    }

    /**
     * Set whether the text should be centred in the screen, or left-aligned (which is the default).
     */
    public void setShouldCentreText(boolean shouldCentreText) {
        this.shouldCentreText = shouldCentreText;
        hasAlteredText = true;
        invalidate();
    }

    /**
     * @see com.github.amlcurran.showcaseview.ShowcaseView.Builder#setSingleShot(long)
     */
    private void setSingleShot(long shotId) {
        shotStateStore.setSingleShot(shotId);
    }

    /**
     * Change the position of the ShowcaseView's button from the default bottom-right position.
     *
     * @param layoutParams a {@link android.widget.RelativeLayout.LayoutParams} representing
     *                     the new position of the button
     */
    @Override
    public void setButtonPosition(RelativeLayout.LayoutParams layoutParams) {
        mEndView.setLayoutParams(layoutParams);
    }

    /**
     * Sets the text alignment of the detail text
     */
    public void setDetailTextAlignment(Layout.Alignment textAlignment) {
        textDrawer.setDetailTextAlignment(textAlignment);
        hasAlteredText = true;
        invalidate();
    }

    /**
     * Sets the text alignment of the title text
     */
    public void setTitleTextAlignment(Layout.Alignment textAlignment) {
        textDrawer.setTitleTextAlignment(textAlignment);
        hasAlteredText = true;
        invalidate();
    }

    /**
     * Set the duration of the fading in and fading out of the ShowcaseView
     */
    private void setFadeDurations(long fadeInMillis, long fadeOutMillis) {
        this.fadeInMillis = fadeInMillis;
        this.fadeOutMillis = fadeOutMillis;
    }

    public void forceTextPosition(int textPosition) {
        textDrawer.forceTextPosition(textPosition);
        hasAlteredText = true;
        invalidate();
    }

    /**
     * @see com.github.amlcurran.showcaseview.ShowcaseView.Builder#hideOnTouchOutside()
     */
    @Override
    public void setHideOnTouchOutside(boolean hideOnTouch) {
        this.hideOnTouch = hideOnTouch;
    }

    /**
     * @see com.github.amlcurran.showcaseview.ShowcaseView.Builder#doNotBlockTouches()
     */
    @Override
    public void setBlocksTouches(boolean blockTouches) {
        this.blockTouches = blockTouches;
    }

    private void setBlockAllTouches(boolean blockAllTouches) {
        this.blockAllTouches = blockAllTouches;
    }

    /**
     * @see com.github.amlcurran.showcaseview.ShowcaseView.Builder#setStyle(int)
     */
    @Override
    public void setStyle(int theme) {
        TypedArray array = getContext().obtainStyledAttributes(theme, R.styleable.ShowcaseView);
        updateStyle(array, true);
    }

    @Override
    public boolean isShowing() {
        return isShowing;
    }

    private void updateStyle(TypedArray styled, boolean invalidate) {
        backgroundColor = styled.getColor(R.styleable.ShowcaseView_sv_backgroundColor, Color.argb(128, 80, 80, 80));
        showcaseColor = styled.getColor(R.styleable.ShowcaseView_sv_showcaseColor, HOLO_BLUE);
        String buttonText = styled.getString(R.styleable.ShowcaseView_sv_buttonText);
        if (TextUtils.isEmpty(buttonText)) {
            buttonText = getResources().getString(android.R.string.ok);
        }
        boolean tintButton = styled.getBoolean(R.styleable.ShowcaseView_sv_tintButtonColor, true);

        int titleTextAppearance = styled.getResourceId(R.styleable.ShowcaseView_sv_titleTextAppearance,
                R.style.TextAppearance_ShowcaseView_Title);
        int detailTextAppearance = styled.getResourceId(R.styleable.ShowcaseView_sv_detailTextAppearance,
                R.style.TextAppearance_ShowcaseView_Detail);

        styled.recycle();

        showcaseDrawer.setShowcaseColour(showcaseColor);
        showcaseDrawer.setBackgroundColour(backgroundColor);
        tintButton(showcaseColor, tintButton);
        setTextButtonClose(buttonText);
        textDrawer.setTitleStyling(titleTextAppearance);
        textDrawer.setDetailStyling(detailTextAppearance);
        hasAlteredText = true;

        if (invalidate) {
            invalidate();
        }
    }

    private void tintButton(int showcaseColor, boolean tintButton) {
        if (tintButton) {
            mEndView.getBackground().setColorFilter(showcaseColor, PorterDuff.Mode.MULTIPLY);
        } else {
            mEndView.getBackground().setColorFilter(HOLO_BLUE, PorterDuff.Mode.MULTIPLY);
        }
    }

    private class UpdateOnGlobalLayout implements ViewTreeObserver.OnGlobalLayoutListener {

        @Override
        public void onGlobalLayout() {
            if (!shotStateStore.hasShot()) {
                updateBitmap();
            }
        }
    }

    private class CalculateTextOnPreDraw implements ViewTreeObserver.OnPreDrawListener {

        @Override
        public boolean onPreDraw() {
            recalculateText();
            return true;
        }
    }

    private OnClickListener hideOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            hide();
        }
    };

    private void setTextButtonClose(int text) {
        if (isButtonValid())
            ((Button) mEndView).setText(text);
    }

    private void setTextButtonClose(String text) {
        if (isButtonValid())
            ((Button) mEndView).setText(text);
    }

    private boolean isButtonValid() {
        return (mEndView != null && mEndView instanceof Button);
    }

}
