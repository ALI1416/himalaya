package com.ck.presenter;

import android.content.Context;
import android.content.SharedPreferences;

import com.ck.base.BaseApplication;
import com.ck.interfaces.IPlayerCallback;
import com.ck.interfaces.IPlayerPresenter;
import com.ck.util.L;
import com.ximalaya.ting.android.opensdk.model.PlayableModel;
import com.ximalaya.ting.android.opensdk.model.advertis.Advertis;
import com.ximalaya.ting.android.opensdk.model.advertis.AdvertisList;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;
import com.ximalaya.ting.android.opensdk.player.advertis.IXmAdsStatusListener;
import com.ximalaya.ting.android.opensdk.player.constants.PlayerConstants;
import com.ximalaya.ting.android.opensdk.player.service.IXmPlayerStatusListener;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayerException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerPresenter implements IPlayerPresenter, IXmAdsStatusListener, IXmPlayerStatusListener {

    private List<IPlayerCallback> mCallbacks = new ArrayList<>();

    private final XmPlayerManager mPlayerManger;
    private Track mCurrentTrack;//当前播放节目
    private int mCurrentIndex;//当前播放节目下标
    private final SharedPreferences mPlayerSp;
    private static final String PLAYER_SP_NAME = "player_sp";
    private static final String PLAY_MODE_SP_KEY = "currentPlayMode";
    private static final String PLAY_ORDER_SP_KEY = "currentPlayOrder";
    private final XmPlayListControl.PlayMode mPlayModeDefault = XmPlayListControl.PlayMode.PLAY_MODEL_LIST;
    private XmPlayListControl.PlayMode mPlayModeCurrent = XmPlayListControl.PlayMode.PLAY_MODEL_LIST;
    private final boolean mPlayOrderDefault = false;
    private boolean mPlayOrderCurrent = false;
    private static PlayerPresenter sInstance = null;
    private boolean isPlayerListLoaded = false;

    private PlayerPresenter() {
        mPlayerManger = XmPlayerManager.getInstance(BaseApplication.getAppContext());
        mPlayerManger.addAdsStatusListener(this);
        mPlayerManger.addPlayerStatusListener(this);
        mPlayerSp = BaseApplication.getAppContext().getSharedPreferences(PLAYER_SP_NAME, Context.MODE_PRIVATE);
    }

    public static PlayerPresenter getInstance() {
        if (sInstance == null) {
            synchronized (PlayerPresenter.class) {
                if (sInstance == null) {
                    sInstance = new PlayerPresenter();
                }
            }
        }
        return sInstance;
    }

    /**
     * 设置播放列表
     */
    public void setPlayList(List<Track> list, int playIndex) {
        if (mPlayerManger != null) {
            isPlayerListLoaded = true;
            mPlayerManger.setPlayList(list, playIndex);
            mCurrentTrack = list.get(playIndex);
            mCurrentIndex = playIndex;
        } else {
            L.e("列表为空");
        }
    }

    /**
     * 播放
     */
    @Override
    public void play() {
        if (isPlayerListLoaded) {
            mPlayerManger.play();
        }
    }

    /**
     * 暂停
     */
    @Override
    public void pause() {
        if (isPlayerListLoaded) {
            mPlayerManger.pause();
        }
    }

    /**
     * 停止
     */
    @Override
    public void stop() {
        if (isPlayerListLoaded) {
            mPlayerManger.stop();
        }
    }

    /**
     * 播放上一首
     */
    @Override
    public void playPre() {
        if (mPlayerManger != null) {
            mPlayerManger.playPre();
        }
    }

    /**
     * 播放下一首
     */
    @Override
    public void playNext() {
        if (mPlayerManger != null) {
            mPlayerManger.playNext();
        }
    }


    /**
     * 获取播放列表
     */
    @Override
    public void getPlayList() {
        if (mPlayerManger != null) {
            List<Track> playList = mPlayerManger.getPlayList();
            for (IPlayerCallback callback : mCallbacks) {
                callback.onListLoaded(playList);
            }
        }
    }

    /**
     * 切歌
     */
    @Override
    public void playByIndex(int index) {
        if (mPlayerManger != null) {
            mPlayerManger.play(index);
        }
    }

    /**
     * 更新进度条进度
     */
    @Override
    public void seekTo(int progress) {
        mPlayerManger.seekTo(progress);
    }

    /**
     * 判断是否正在播放
     */
    @Override
    public boolean isPlaying() {
        return mPlayerManger.isPlaying();
    }

    /**
     * 判断是否有播放列表
     */
    @Override
    public boolean hasPlayList() {
        return isPlayerListLoaded;
    }

    /**
     * 切换播放模式
     */
    @Override
    public void switchPlayMode(XmPlayListControl.PlayMode mode) {
        mPlayModeCurrent = mode;
        if (mPlayerManger != null) {
            mPlayerManger.setPlayMode(mode);
            for (IPlayerCallback callback : mCallbacks) {//通知UI更新
                callback.onPlayModeChange(mode);
            }
        }
    }

    /**
     * 切换播放列表顺序
     */
    @Override
    public void switchPlayList(boolean isOrder) {
        mPlayOrderCurrent = isOrder;
        List<Track> tracks = mPlayerManger.getPlayList();
        Collections.reverse(tracks);//反转
        mCurrentIndex = tracks.size() - mCurrentIndex - 1;//反转后当前播放节目的下边
        mPlayerManger.setPlayList(tracks, mCurrentIndex);//重新设置播放列表
        //更新UI
        mCurrentTrack = (Track) mPlayerManger.getCurrSound();
        for (IPlayerCallback callback : mCallbacks) {
            callback.onListLoaded(tracks);
            callback.onTrackUpdate(mCurrentTrack, mCurrentIndex);
            callback.onPlayOrderChange(isOrder);
        }
    }

    @Override
    public void registerViewCallback(IPlayerCallback iPlayerCallback) {
        iPlayerCallback.onTrackUpdate(mCurrentTrack, mCurrentIndex);
        if (!mCallbacks.contains(iPlayerCallback)) {
            mCallbacks.add(iPlayerCallback);
        }
        int mode = mPlayerSp.getInt(PLAY_MODE_SP_KEY, mPlayModeDefault.ordinal());
        boolean isOrder = mPlayerSp.getBoolean(PLAY_ORDER_SP_KEY, mPlayOrderDefault);
        iPlayerCallback.onPlayModeChange(XmPlayListControl.PlayMode.values()[mode]);
        iPlayerCallback.onPlayOrderChange(isOrder);
    }

    @Override
    public void unRegisterViewCallback(IPlayerCallback iPlayerCallback) {
        mCallbacks.remove(iPlayerCallback);
        SharedPreferences.Editor editor = mPlayerSp.edit();
        editor.putInt(PLAY_MODE_SP_KEY, mPlayModeCurrent.ordinal());
        editor.putBoolean(PLAY_ORDER_SP_KEY, mPlayOrderCurrent);
        editor.apply();
    }

    //region 广告回调开始

    @Override
    public void onStartGetAdsInfo() {

    }

    @Override
    public void onGetAdsInfo(AdvertisList advertisList) {

    }

    @Override
    public void onAdsStartBuffering() {

    }

    @Override
    public void onAdsStopBuffering() {

    }

    @Override
    public void onStartPlayAds(Advertis advertis, int i) {

    }

    @Override
    public void onCompletePlayAds() {

    }

    @Override
    public void onError(int i, int i1) {

    }

    //endregion 广告回调结束

    //region 播放器回调开始

    @Override
    public void onPlayStart() {
        for (IPlayerCallback callback : mCallbacks) {
            callback.onPlayerStart();
        }
    }

    @Override
    public void onPlayPause() {
        for (IPlayerCallback callback : mCallbacks) {
            callback.onPlayStop();
        }
    }

    @Override
    public void onPlayStop() {
        for (IPlayerCallback callback : mCallbacks) {
            callback.onPlayStop();
        }
    }

    @Override
    public void onSoundPlayComplete() {

    }

    /**
     * 播放器准备好了，去播放
     */
    @Override
    public void onSoundPrepared() {
        if (mPlayerManger.getPlayerStatus() == PlayerConstants.STATE_PREPARED) {
            mPlayerManger.play();
        }
    }

    /**
     * 切歌
     */
    @Override
    public void onSoundSwitch(PlayableModel lastModel, PlayableModel currentMode) {
        mCurrentIndex = mPlayerManger.getCurrentIndex();
        if (currentMode instanceof Track) {
            Track currentTrack = (Track) currentMode;
            for (IPlayerCallback callback : mCallbacks) {
                callback.onTrackUpdate(currentTrack, mCurrentIndex);
            }
        }
    }

    @Override
    public void onBufferingStart() {

    }

    @Override
    public void onBufferingStop() {

    }

    @Override
    public void onBufferProgress(int i) {

    }

    /**
     * 播放进度 单位：毫秒
     */
    @Override
    public void onPlayProgress(int currentPosition, int duration) {
        for (IPlayerCallback callback : mCallbacks) {
            callback.onProgressChange(currentPosition, duration);
        }
    }

    @Override
    public boolean onError(XmPlayerException e) {
        return false;
    }

    //endregion 播放器回调结束

}
