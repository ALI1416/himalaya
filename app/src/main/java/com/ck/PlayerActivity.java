package com.ck;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.ck.adapter.PlayerViewPagerAdapter;
import com.ck.base.BaseActivity;
import com.ck.interfaces.IPlayerCallback;
import com.ck.presenter.PlayerPresenter;
import com.ck.view.PlayerListPopupWindow;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode.PLAY_MODEL_LIST;
import static com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode.PLAY_MODEL_LIST_LOOP;
import static com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode.PLAY_MODEL_RANDOM;
import static com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode.PLAY_MODEL_SINGLE_LOOP;

public class PlayerActivity extends BaseActivity implements IPlayerCallback {

    private TextView mTitle;
    private ViewPager mViewPager;
    private TextView mSeekNow;
    private TextView mSeekTotal;
    private SeekBar mSeekBar;
    private ImageView mMode;
    private ImageView mList;
    private ImageView mPlay;
    private ImageView mPre;
    private ImageView mNext;
    private PlayerPresenter mPlayerPresenter;
    private SimpleDateFormat mMmssFormat = new SimpleDateFormat("mm:ss", Locale.CHINA);
    private SimpleDateFormat mHhmmssFormat = new SimpleDateFormat("hh:mm:ss", Locale.CHINA);
    private String mTitleText;
    private PlayerViewPagerAdapter mPlayerViewPagerAdapter;
    private int mCurrentProgress = 0;//??????????????????
    private boolean mIsUserTouchSeekBar = false;//???????????????????????????
    private boolean mIsUserSlidPager = false;//????????????????????????
    private static Map<XmPlayListControl.PlayMode, XmPlayListControl.PlayMode> sPlayMode = new HashMap<>();//???????????????????????????
    private XmPlayListControl.PlayMode mCurrentPlayMode = PLAY_MODEL_LIST;//??????????????????
    private boolean mCurrentPlayOrder = false;//??????????????????
    private PlayerListPopupWindow mPlayerListPopupWindow;

    static {
        //PLAY_MODEL_SINGLE ??????
        //PLAY_MODEL_SINGLE_LOOP ????????????
        //PLAY_MODEL_LIST ??????
        //PLAY_MODEL_LIST_LOOP ????????????
        //PLAY_MODEL_RANDOM ??????
        sPlayMode.put(PLAY_MODEL_LIST_LOOP, PLAY_MODEL_RANDOM);
        sPlayMode.put(PLAY_MODEL_RANDOM, PLAY_MODEL_SINGLE_LOOP);
        sPlayMode.put(PLAY_MODEL_SINGLE_LOOP, PLAY_MODEL_LIST);
        sPlayMode.put(PLAY_MODEL_LIST, PLAY_MODEL_LIST_LOOP);
    }

    private ValueAnimator mEnterBgAnimator;
    private ValueAnimator mExitBgAnimator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        initView();
        mPlayerPresenter = PlayerPresenter.getInstance();
        mPlayerPresenter.registerViewCallback(this);
        mPlayerPresenter.getPlayList();
        initEven();
        initBgAnim();
    }

    private void initBgAnim() {
        //??????????????????
        mEnterBgAnimator = ValueAnimator.ofFloat(1.0f, 0.8f);
        mEnterBgAnimator.setDuration(300);
        mEnterBgAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                updateBgAlpha((float) animation.getAnimatedValue());
            }
        });
        //??????????????????
        mExitBgAnimator = ValueAnimator.ofFloat(0.8f, 1.0f);
        mExitBgAnimator.setDuration(300);
        mExitBgAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                updateBgAlpha((float) animation.getAnimatedValue());
            }
        });
    }

    private void initView() {
        mTitle = findViewById(R.id.player_title);
        mViewPager = findViewById(R.id.player_view_pager);
        mSeekNow = findViewById(R.id.play_seek_now);
        mSeekTotal = findViewById(R.id.player_seek_total);
        mSeekBar = findViewById(R.id.play_seek_bar);
        mMode = findViewById(R.id.player_mode);
        mList = findViewById(R.id.player_list);
        mPlay = findViewById(R.id.player_play);
        mPre = findViewById(R.id.player_pre);
        mNext = findViewById(R.id.player_next);
        if (!TextUtils.isEmpty(mTitleText)) {
            mTitle.setText(mTitleText);
        }
        //???????????????
        mPlayerViewPagerAdapter = new PlayerViewPagerAdapter();
        mViewPager.setAdapter(mPlayerViewPagerAdapter);
        //??????
        mPlayerListPopupWindow = new PlayerListPopupWindow();
    }

    //region initEven.start
    @SuppressLint("ClickableViewAccessibility")
    private void initEven() {
        //????????????
        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerPresenter.isPlaying()) {
                    mPlayerPresenter.pause();
                } else {
                    mPlayerPresenter.play();
                }
            }
        });
        //???????????????
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {//??????????????? fromUser??????????????????
                if (fromUser) {
                    mCurrentProgress = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {//????????????
                mIsUserTouchSeekBar = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {//????????????
                mIsUserTouchSeekBar = false;
                mPlayerPresenter.seekTo(mCurrentProgress);//??????????????????
            }
        });
        //?????????
        mPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerPresenter != null) {
                    mPlayerPresenter.playPre();
                }
            }
        });
        //?????????
        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerPresenter != null) {
                    mPlayerPresenter.playNext();
                }
            }
        });
        //????????????????????????
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (mPlayerPresenter != null && mIsUserSlidPager) {
                    mPlayerPresenter.playByIndex(position);
                }
                mIsUserSlidPager = false;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        //??????????????????????????????????????????????????????
        mViewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN: {
                        mIsUserSlidPager = true;
                        break;
                    }
                    default: {

                    }
                }
                return false;
            }
        });
        //??????????????????
        mMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchPlayMode();
            }
        });
        //??????????????????
        mList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayerListPopupWindow.showAtLocation(v, Gravity.BOTTOM, 0, 0);//???????????????
                mEnterBgAnimator.start();//????????????
            }
        });
        //???????????????????????????????????????
        mPlayerListPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                mExitBgAnimator.start();
            }
        });
        //?????????????????????
        mPlayerListPopupWindow.setPlayListItemClickListener(new PlayerListPopupWindow.PlayListItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if (mPlayerPresenter != null) {
                    mPlayerPresenter.playByIndex(position);
                }
            }
        });
        //??????????????????/??????
        mPlayerListPopupWindow.setPlayListActionListener(new PlayerListPopupWindow.PlayListActionListener() {
            @Override
            public void onPlayModeClick() {
                switchPlayMode();
            }

            @Override
            public void onPlayOrderClick() {
                switchPlayOrder();
            }
        });
    }


    //endregion initEven.end

    /**
     * ??????????????????
     */
    private void switchPlayMode() {
        XmPlayListControl.PlayMode playMode = sPlayMode.get(mCurrentPlayMode);//???????????????????????????
        if (mPlayerPresenter != null) {
            mPlayerPresenter.switchPlayMode(playMode);//??????????????????
        }
    }

    /**
     * ??????????????????
     */
    private void switchPlayOrder() {
        if (mPlayerPresenter != null) {
            mPlayerPresenter.switchPlayList(!mCurrentPlayOrder);//????????????????????????
        }
    }

    /**
     * ?????????????????????
     */
    public void updateBgAlpha(float alpha) {
        Window window = getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.alpha = alpha;
        window.setAttributes(attributes);
    }

    /**
     * ????????????????????????
     */
    private void updatePlayModeImg() {
        int resId = R.drawable.selector_player_mode_list;
        switch (mCurrentPlayMode) {
            case PLAY_MODEL_LIST: {
                resId = R.drawable.selector_player_mode_list;
                break;
            }
            case PLAY_MODEL_LIST_LOOP: {
                resId = R.drawable.selector_player_mode_list_loop;
                break;
            }
            case PLAY_MODEL_RANDOM: {
                resId = R.drawable.selector_player_mode_random;
                break;
            }
            case PLAY_MODEL_SINGLE_LOOP: {
                resId = R.drawable.selector_player_mode_single_loop;
                break;
            }
        }
        mMode.setImageResource(resId);
    }

    /**
     * ????????????????????????
     */
    private void updatePlayerOrderImg() {
        int resId = mCurrentPlayOrder ? R.drawable.selector_player_list_asc : R.drawable.selector_player_list_desc;
        mList.setImageResource(resId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayerPresenter != null) {
            mPlayerPresenter.unRegisterViewCallback(this);
            mPlayerPresenter = null;
        }
    }

    //region IPlayerCallback.start

    /**
     * ????????????
     */
    @Override
    public void onPlayerStart() {
        if (mPlay != null) {
            mPlay.setImageResource(R.drawable.selector_player_stop);
        }
    }

    /**
     * ????????????
     */
    @Override
    public void onPlayPause() {
        if (mPlay != null) {
            mPlay.setImageResource(R.drawable.selector_player_play);
        }
    }

    @Override
    public void onPlayStop() {
        if (mPlay != null) {
            mPlay.setImageResource(R.drawable.selector_player_play);
        }
    }

    @Override
    public void onPlayError() {

    }

    @Override
    public void nextPlay(Track track) {

    }

    @Override
    public void prePlay(Track track) {

    }

    @Override
    public void onListLoaded(List<Track> list) {
        if (mPlayerViewPagerAdapter != null) {
            mPlayerViewPagerAdapter.setData(list);
        }
        if (mPlayerListPopupWindow != null) {
            mPlayerListPopupWindow.setListData(list);
        }
    }

    @Override
    public void onPlayModeChange(XmPlayListControl.PlayMode mode) {
        mCurrentPlayMode = mode;
        if (mPlayerListPopupWindow != null) {
            mPlayerListPopupWindow.updatePlayMode(mCurrentPlayMode);
            updatePlayModeImg();
        }
    }

    @Override
    public void onPlayOrderChange(boolean isOrder) {
        mCurrentPlayOrder = isOrder;
        if (mPlayerListPopupWindow != null) {
            mPlayerListPopupWindow.updatePlayOrder(isOrder);
            updatePlayerOrderImg();
        }
    }

    @Override
    public void onProgressChange(int currentProgress, int total) {
        mSeekBar.setMax(total);
        //?????????????????????
        String totalDuration;
        if (total < 1000 * 60 * 60) {
            totalDuration = mMmssFormat.format(total);
        } else {
            totalDuration = mHhmmssFormat.format(total);
        }
        if (mSeekTotal != null) {
            mSeekTotal.setText(totalDuration);
        }
        String currentPosition;
        if (currentProgress < 1000 * 60 * 60) {
            currentPosition = mMmssFormat.format(currentProgress);
        } else {
            currentPosition = mHhmmssFormat.format(currentProgress);
        }
        if (mSeekNow != null) {
            mSeekNow.setText(currentPosition);
        }
        //???????????????
        if (mSeekBar != null && !mIsUserTouchSeekBar) {
            mSeekBar.setProgress(currentProgress);
        }
    }

    @Override
    public void onAdLoading() {

    }

    @Override
    public void onAdFinished() {

    }

    @Override
    public void onTrackUpdate(Track track, int index) {
        mTitleText = track.getTrackTitle();
        if (mTitle != null) {//????????????
            mTitle.setText(mTitleText);
        }
        if (mViewPager != null) {//????????????
            mViewPager.setCurrentItem(index);
        }
        if (mPlayerListPopupWindow != null) {//??????????????????
            mPlayerListPopupWindow.setCurrentPlayPosition(index);
        }
    }

    //endregion IPlayerCallback.end


}
