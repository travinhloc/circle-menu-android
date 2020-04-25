package com.ramotion.circlemenu;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

public class CircleMenuView extends FrameLayout {

    private static final int DEFAULT_BUTTON_SIZE = 56;
    private static final float DEFAULT_DISTANCE = DEFAULT_BUTTON_SIZE * 2f;
    private static final float DEFAULT_RING_SCALE_RATIO = 1.3f;
    private static final float DEFAULT_CLOSE_ICON_ALPHA = 0.3f;

    private final List<RelativeLayout> menuButtons = new ArrayList<>();
    private final Rect buttonRect = new Rect();

    private ImageView mMenuButton;
    private RingEffectView ringView;

    private boolean mClosedState = true;
    private boolean isAnimating = false;

    private int mIconMenu;
    private int mIconClose;
    private int durationRing;
    private int mLongClickDurationRing;
    private int mDurationOpen;
    private int mDurationClose;
    private int mDesiredSize;
    private int ringRadius;
    private float mDistance;
    private List<Menu> menus;

    private EventListener mListener;



    /**
     * CircleMenu event listener.
     */
    public static class EventListener {
        /**
         * Invoked on menu button click, before animation start.
         * @param view current CircleMenuView instance.
         */
        public void onMenuOpenAnimationStart(@NonNull CircleMenuView view) {}

        /**
         * Invoked on menu button click, after animation end.
         * @param view - current CircleMenuView instance.
         */
        public void onMenuOpenAnimationEnd(@NonNull CircleMenuView view) {}

        /**
         * Invoked on close menu button click, before animation start.
         * @param view - current CircleMenuView instance.
         */
        public void onMenuCloseAnimationStart(@NonNull CircleMenuView view) {}

        /**
         * Invoked on close menu button click, after animation end.
         * @param view - current CircleMenuView instance.
         */
        public void onMenuCloseAnimationEnd(@NonNull CircleMenuView view) {}

        /**
         * Invoked on button click, before animation start.
         * @param view - current CircleMenuView instance.
         * @param buttonIndex - clicked button zero-based index.
         */
        public void onButtonClickAnimationStart(@NonNull CircleMenuView view, int buttonIndex) {}

        /**
         * Invoked on button click, after animation end.
         * @param view - current CircleMenuView instance.
         * @param buttonIndex - clicked button zero-based index.
         */
        public void onButtonClickAnimationEnd(@NonNull CircleMenuView view, int buttonIndex) {}
    }

    private class OnButtonClickListener implements View.OnClickListener {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onClick(final View view) {
            if (isAnimating) {
                return;
            }
            durationRing = 0;
            final Animator click = getButtonClickAnimation((RelativeLayout)view);
            click.setDuration(durationRing);
            click.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (mListener != null) {
                        mListener.onButtonClickAnimationStart(CircleMenuView.this, menuButtons.indexOf(view));
                    }
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    mClosedState = true;
                    if (mListener != null) {
                        mListener.onButtonClickAnimationEnd(CircleMenuView.this, menuButtons.indexOf(view));
                    }
                }
            });
            click.start();
        }
    }

    public CircleMenuView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleMenuView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (attrs == null) {
            throw new IllegalArgumentException("No buttons icons or colors set");
        }

        final int menuButtonColor;
        final List<Integer> icons;
        final List<Integer> colors;
        final List<Integer> labels;

        final TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CircleMenuView, 0, 0);
        try {
            final int iconArrayId = a.getResourceId(R.styleable.CircleMenuView_button_icons, 0);
            final int colorArrayId = a.getResourceId(R.styleable.CircleMenuView_button_colors, 0);
            final CharSequence[] labelsArrayId = a.getTextArray(R.styleable.CircleMenuView_button_labels);

            final TypedArray iconsIds = getResources().obtainTypedArray(iconArrayId);
            try {
                final int[] colorsIds = getResources().getIntArray(colorArrayId);
                final int buttonsCount = Math.min(iconsIds.length(), colorsIds.length);

                icons = new ArrayList<>(buttonsCount);
                colors = new ArrayList<>(buttonsCount);
                menus = new ArrayList<>();

                for (int i = 0; i < buttonsCount; i++) {
                    icons.add(iconsIds.getResourceId(i, -1));
                    colors.add(colorsIds[i]);
                    menus.add(new Menu(iconsIds.getResourceId(i, -1), labelsArrayId[i].toString(), i%2));

                }
            } finally {
                iconsIds.recycle();
            }

            mIconMenu = a.getResourceId(R.styleable.CircleMenuView_icon_menu, R.drawable.ic_menu_black_24dp);
            mIconClose = a.getResourceId(R.styleable.CircleMenuView_icon_close, R.drawable.ic_close_black_24dp);

            durationRing = a.getInteger(R.styleable.CircleMenuView_duration_ring, getResources().getInteger(android.R.integer.config_mediumAnimTime));
            mLongClickDurationRing = a.getInteger(R.styleable.CircleMenuView_long_click_duration_ring, getResources().getInteger(android.R.integer.config_longAnimTime));
            mDurationOpen = a.getInteger(R.styleable.CircleMenuView_duration_open, getResources().getInteger(android.R.integer.config_mediumAnimTime));
            mDurationClose = a.getInteger(R.styleable.CircleMenuView_duration_close, getResources().getInteger(android.R.integer.config_mediumAnimTime));

            final float density = context.getResources().getDisplayMetrics().density;
            final float defaultDistance = DEFAULT_DISTANCE * density;
            mDistance = a.getDimension(R.styleable.CircleMenuView_distance, defaultDistance);

            menuButtonColor = a.getColor(R.styleable.CircleMenuView_icon_color, Color.WHITE);
        } finally {
            a.recycle();
        }

        initLayout(context);
        initMenu(menuButtonColor);
        initButtons(context, icons, colors);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int w = resolveSizeAndState(mDesiredSize, widthMeasureSpec, 0);
        final int h = resolveSizeAndState(mDesiredSize, heightMeasureSpec, 0);

        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (!changed && isAnimating) {
            return;
        }

        ringView.setStrokeWidth(buttonRect.width());
        ringView.setRadius(ringRadius);

        final LayoutParams lp = (LayoutParams) ringView.getLayoutParams();
        lp.width = right - left;
        lp.height = bottom - top;
    }

    private void initLayout(@NonNull Context context) {
        LayoutInflater.from(context).inflate(R.layout.circle_menu, this, true);

        setWillNotDraw(true);
        setClipChildren(false);
        setClipToPadding(false);

        final float density = context.getResources().getDisplayMetrics().density;
        final float buttonSize = DEFAULT_BUTTON_SIZE * density;

        ringRadius = (int) (buttonSize + (mDistance - buttonSize / 2));
        mDesiredSize = (int) (ringRadius * 2 * DEFAULT_RING_SCALE_RATIO);

        ringView = findViewById(R.id.ring_view);
    }

    private void initMenu(int menuButtonColor) {
        final AnimatorListenerAdapter animListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (mListener != null) {
                    if (mClosedState) {
                        mListener.onMenuOpenAnimationStart(CircleMenuView.this);
                    } else {
                        mListener.onMenuCloseAnimationStart(CircleMenuView.this);
                    }
                }
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mListener != null) {
                    if (mClosedState) {
                        mListener.onMenuOpenAnimationEnd(CircleMenuView.this);
                    } else {
                        mListener.onMenuCloseAnimationEnd(CircleMenuView.this);
                    }
                }

                mClosedState = !mClosedState;
            }
        };

        mMenuButton = findViewById(R.id.circle_menu_main_button);
        mMenuButton.setImageResource(mIconMenu);
        mMenuButton.setBackgroundTintList(ColorStateList.valueOf(menuButtonColor));
        mMenuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isAnimating) {
                    return;
                }

                final Animator animation = mClosedState ? getOpenMenuAnimation() : getCloseMenuAnimation();
                animation.setDuration(mClosedState ? mDurationClose : mDurationOpen);
                animation.addListener(animListener);
                animation.start();
            }
        });
    }

    private void initButtons(@NonNull Context context, @NonNull List<Integer> icons, @NonNull List<Integer> colors) {
        final int buttonsCount = Math.min(icons.size(), colors.size());
        for (int i = 0; i < buttonsCount; i++) {
            RelativeLayout  relativeLayout;
            LayoutInflater inflater1 = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            relativeLayout = (RelativeLayout) inflater1.inflate(R.layout.floating_layout, null);
            TextView tvLabel = relativeLayout.findViewById(R.id.tv_menu);
            ImageView ivMenu = relativeLayout.findViewById(R.id.iv_menu);
            TextView tvBadge = relativeLayout.findViewById(R.id.tv_badge);
            Menu menu = menus.get(i);
            tvLabel.setText(menu.title);
            ivMenu.setImageResource(menu.res);
            tvBadge.setText(String.valueOf(menu.badge));
            tvBadge.setVisibility(menu.badge > 0 ? VISIBLE : INVISIBLE);
            tvBadge.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_badge_blue));

            int rect = 210;
            relativeLayout.setLayoutParams(new LayoutParams(rect, rect));
            relativeLayout.setScaleX(0);
            relativeLayout.setScaleY(0);
            relativeLayout.setOnClickListener(new OnButtonClickListener());
            addView(relativeLayout);
            menuButtons.add(relativeLayout);
        }
    }

    public void updateBadge(int[] badges) {
        for (int i = 0; i < badges.length-1; i++) {
            int count = badges[i];
            TextView tvBadge = menuButtons.get(i).findViewById(R.id.tv_badge);
            tvBadge.setText(String.valueOf(count));
            tvBadge.setVisibility(count > 0 ? VISIBLE : INVISIBLE);
            invalidate();
        }

        open(true);
    }
    private void offsetAndScaleButtons(float centerX, float centerY, float angleStep, float offset, float scale) {
        for (int i = 0, cnt = menuButtons.size(); i < cnt; i++) {
            final float angle = angleStep * i - 90;
            final float x = (float) Math.cos(Math.toRadians(angle)) * offset;
            final float y = (float) Math.sin(Math.toRadians(angle)) * offset;

            final View button = menuButtons.get(i);
            button.setX(centerX + x);
            button.setY(centerY + y);
            button.setScaleX(1.0f * scale);
            button.setScaleY(1.0f * scale);
        }
    }

    private Animator getButtonClickAnimation(final @NonNull RelativeLayout button) {
        final int buttonNumber = menuButtons.indexOf(button) + 1;
        final float stepAngle = 360f / menuButtons.size();
        final float rOStartAngle = (270 - stepAngle + stepAngle * buttonNumber);
        final float rStartAngle = rOStartAngle > 360 ? rOStartAngle % 360 : rOStartAngle;

        final float x = (float) Math.cos(Math.toRadians(rStartAngle)) * mDistance;
        final float y = (float) Math.sin(Math.toRadians(rStartAngle)) * mDistance;

        final float pivotX = button.getPivotX();
        final float pivotY = button.getPivotY();
        button.setPivotX(pivotX - x);
        button.setPivotY(pivotY - y);

        final ObjectAnimator rotateButton = ObjectAnimator.ofFloat(button, "rotation", 0f, 360f);
        rotateButton.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                button.setPivotX(pivotX);
                button.setPivotY(pivotY);
            }
        });

        ringView.setVisibility(View.INVISIBLE);
        ringView.setStartAngle(rStartAngle);

        final ColorStateList csl = button.getBackgroundTintList();
        if (csl != null) {
            ringView.setStrokeColor(csl.getDefaultColor());
        }

        final ObjectAnimator ring = ObjectAnimator.ofFloat(ringView, "angle", 360);
        final ObjectAnimator scaleX = ObjectAnimator.ofFloat(ringView, "scaleX", 1f, DEFAULT_RING_SCALE_RATIO);
        final ObjectAnimator scaleY = ObjectAnimator.ofFloat(ringView, "scaleY", 1f, DEFAULT_RING_SCALE_RATIO);
        final ObjectAnimator visible = ObjectAnimator.ofFloat(ringView, "alpha", 1f, 0f);

        final AnimatorSet lastSet = new AnimatorSet();
        lastSet.playTogether(scaleX, scaleY, visible, getCloseMenuAnimation());

        final AnimatorSet firstSet = new AnimatorSet();
        firstSet.playTogether(rotateButton, ring);

        final AnimatorSet result = new AnimatorSet();
        result.play(firstSet).before(lastSet);
        result.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                isAnimating = true;

                for (View b : menuButtons) {
                    if (b != button) {
//                        ((RelativeLayout) b).setCompatElevation(0);
                    }
                }

                ringView.setScaleX(1f);
                ringView.setScaleY(1f);
                ringView.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;
            }
        });

        return result;
    }

    private Animator getOpenMenuAnimation() {
        final ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(mMenuButton, "alpha", DEFAULT_CLOSE_ICON_ALPHA);

        final Keyframe kf0 = Keyframe.ofFloat(0f, 0f);
        final Keyframe kf1 = Keyframe.ofFloat(0.5f, 60f);
        final Keyframe kf2 = Keyframe.ofFloat(1f, 0f);

        final PropertyValuesHolder pvhRotation = PropertyValuesHolder.ofKeyframe("rotation", kf0, kf1, kf2);
        final ObjectAnimator rotateAnimation = ObjectAnimator.ofPropertyValuesHolder(mMenuButton, pvhRotation);
        rotateAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private boolean iconChanged = false;
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                final float fraction = valueAnimator.getAnimatedFraction();
                if (fraction >= 0.5f && !iconChanged) {
                    iconChanged = true;
                    mMenuButton.setImageResource(mIconClose);
                }
            }
        });

        final float centerX = mMenuButton.getX();
        final float centerY = mMenuButton.getY();

        final int buttonsCount = menuButtons.size();
        final float angleStep = 360f / buttonsCount;

        final ValueAnimator buttonsAppear = ValueAnimator.ofFloat(0f, mDistance);
        buttonsAppear.setInterpolator(new OvershootInterpolator());
        buttonsAppear.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                for (View view: menuButtons) {
                    view.setVisibility(View.VISIBLE);
                }
            }
        });
        buttonsAppear.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                final float fraction = valueAnimator.getAnimatedFraction();
                final float value = (float)valueAnimator.getAnimatedValue();
                offsetAndScaleButtons(centerX, centerY, angleStep, value, fraction);
            }
        });

        final AnimatorSet result = new AnimatorSet();
        result.playTogether(alphaAnimation, rotateAnimation, buttonsAppear);
        result.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                isAnimating = true;
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;
            }
        });

        return result;
    }

    private Animator getCloseMenuAnimation() {
        final ObjectAnimator scaleX1 = ObjectAnimator.ofFloat(mMenuButton, "scaleX", 0f);
        final ObjectAnimator scaleY1 = ObjectAnimator.ofFloat(mMenuButton, "scaleY", 0f);
        final ObjectAnimator alpha1 = ObjectAnimator.ofFloat(mMenuButton, "alpha", 0f);
        final AnimatorSet set1 = new AnimatorSet();
        set1.playTogether(scaleX1, scaleY1, alpha1);
        set1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                for (View view: menuButtons) {
                    view.setVisibility(View.INVISIBLE);
                }
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                mMenuButton.setRotation(60f);
                mMenuButton.setImageResource(mIconMenu);
            }
        });

        final ObjectAnimator angle = ObjectAnimator.ofFloat(mMenuButton, "rotation", 0);
        final ObjectAnimator alpha2 = ObjectAnimator.ofFloat(mMenuButton, "alpha", 1f);
        final ObjectAnimator scaleX2 = ObjectAnimator.ofFloat(mMenuButton, "scaleX", 1f);
        final ObjectAnimator scaleY2 = ObjectAnimator.ofFloat(mMenuButton, "scaleY", 1f);
        final AnimatorSet set2 = new AnimatorSet();
        set2.setInterpolator(new OvershootInterpolator());
        set2.playTogether(angle, alpha2, scaleX2, scaleY2);

        final AnimatorSet result = new AnimatorSet();
        result.play(set1).before(set2);
        result.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                isAnimating = true;
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;
            }
        });
        return result;
    }

    public void setIconMenu(@DrawableRes int iconId) {
        mIconMenu = iconId;
    }

    @DrawableRes
    public int getIconMenu() {
        return mIconMenu;
    }

    public void setIconClose(@DrawableRes int iconId) {
        mIconClose = iconId;
    }

    @DrawableRes
    public int getIconClose() {
        return mIconClose;
    }

    /**
     * See {@link R.styleable#CircleMenuView_duration_close}
     * @param duration close animation duration in milliseconds.
     */
    public void setDurationClose(int duration) {
        mDurationClose = duration;
    }

    /**
     * See {@link R.styleable#CircleMenuView_duration_close}
     * @return current close animation duration.
     */
    public int getDurationClose() {
        return mDurationClose;
    }

    /**
     * See {@link R.styleable#CircleMenuView_duration_open}
     * @param duration open animation duration in milliseconds.
     */
    public void setDurationOpen(int duration) {
        mDurationOpen = duration;
    }

    /**
     * See {@link R.styleable#CircleMenuView_duration_open}
     * @return current open animation duration.
     */
    public int getDurationOpen() {
        return mDurationOpen;
    }

    /**
     * See {@link R.styleable#CircleMenuView_duration_ring}
     * @param duration ring animation duration in milliseconds.
     */
    public void setDurationRing(int duration) {
        durationRing = duration;
    }

    /**
     * See {@link R.styleable#CircleMenuView_duration_ring}
     * @return current ring animation duration.
     */
    public int getDurationRing() {
        return durationRing;
    }

    /**
     * See {@link R.styleable#CircleMenuView_long_click_duration_ring}
     * @return current long click ring animation duration.
     */
    public int getLongClickDurationRing() {
        return mLongClickDurationRing;
    }

    /**
     * See {@link R.styleable#CircleMenuView_long_click_duration_ring}
     * @param duration long click ring animation duration in milliseconds.
     */
    public void setLongClickDurationRing(int duration) {
        mLongClickDurationRing = duration;
    }

    /**
     * See {@link R.styleable#CircleMenuView_distance}
     * @param distance in pixels.
     */
    public void setDistance(float distance) {
        mDistance = distance;
        invalidate();
    }

    /**
     * See {@link R.styleable#CircleMenuView_distance}
     * @return current distance in pixels.
     */
    public float getDistance() {
        return mDistance;
    }

    /**
     * See {@link CircleMenuView.EventListener }
     * @param listener new event listener or null.
     */
    public void setEventListener(@Nullable EventListener listener) {
        mListener = listener;
    }

    /**
     * See {@link CircleMenuView.EventListener }
     * @return current event listener or null.
     */
    public EventListener getEventListener() {
        return mListener;
    }

    private void openOrClose(boolean open, boolean animate) {
        if (isAnimating) {
            return;
        }

        if (open && !mClosedState) {
            return;
        }

        if (!open && mClosedState) {
            return;
        }

        if (animate) {
            mMenuButton.performClick();
        } else {
            mClosedState = !open;

            final float centerX = mMenuButton.getX();
            final float centerY = mMenuButton.getY();

            final int buttonsCount = menuButtons.size();
            final float angleStep = 360f / buttonsCount;

            final float offset = open ? mDistance : 0f;
            final float scale = open ? 1f : 0f;

            mMenuButton.setImageResource(open ? mIconClose : mIconMenu);
            mMenuButton.setAlpha(open ? DEFAULT_CLOSE_ICON_ALPHA : 1f);

            final int visibility = open ? View.VISIBLE : View.INVISIBLE;
            for (View view: menuButtons) {
                view.setVisibility(visibility);
            }

            offsetAndScaleButtons(centerX, centerY, angleStep, offset, scale);
        }
    }

    /**
     * Open menu programmatically
     * @param animate open with animation or not
     */
    public void open(boolean animate) {
        openOrClose(true, animate);
    }

    /**
     * Close menu programmatically
     * @param animate close with animation or not
     */
    public void close(boolean animate) {
        openOrClose(false, animate);
    }

}
