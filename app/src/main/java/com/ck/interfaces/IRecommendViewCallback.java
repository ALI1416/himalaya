package com.ck.interfaces;

import com.ximalaya.ting.android.opensdk.model.album.Album;

import java.util.List;

/**
 * Recommend界面回调接口
 */
public interface IRecommendViewCallback {
    /**
     * 获取到了推荐内容
     */
    void onRecommendListLoaded(List<Album> albums);

    /**
     * 网络异常
     */
    void onNetworkError(int i, String s);

    /**
     * 数据为空
     */
    void onEmpty();

    /**
     * 正在加载
     */
    void onLoading();
}
