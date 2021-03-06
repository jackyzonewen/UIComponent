package com.anjuke.library.uicomponent.photo;

import java.util.ArrayList;
import java.util.List;

import com.anjuke.library.uicomponent.photo.listener.OnPhotoLoader;
import com.anjuke.uicomponent.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;


/**
 * 图片滑动组件
 * 
 * @author duhu (hudu@anjuke.com)
 * @date 2012-7-23
 * @modified by qitongzhang (qitongzhang@anjuke.com)
 */
public class PhotoGallery extends Gallery {

    private List<View> mViews = new ArrayList<View>();
    private List<String> mPhotoListUrl;
    private int mImageViewListSize = 3;
    private boolean mIsMove = false;
    private OnPhotoLoader mLoader;
    public PhotoGallery(Context context) {
        super(context);
        initGalleryAttr();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            break;
        case MotionEvent.ACTION_MOVE:
            mIsMove = true;
            break;
        case MotionEvent.ACTION_UP:
            mIsMove = false;
            break;

        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!mIsMove) {
            super.onLayout(changed, l, t, r, b);
        }

    }

    public PhotoGallery(Context context, AttributeSet attrs) {
        super(context, attrs);
        initGalleryAttr();
    }

    /** 初始化gallery的状态属性 */
    private void initGalleryAttr() {
        this.setSpacing(5);// 设置图片之间的间隙
        this.setUnselectedAlpha((float) 1.0);// 设置透明度
        this.setHorizontalFadingEdgeEnabled(false);// 去掉左右边界模糊效果
    }

    // 控制gallery的滑动，一张一张的显示
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        int kEvent;
        if (isScrollingLeft(e1, e2)) {
            // Check if scrolling left
            kEvent = KeyEvent.KEYCODE_DPAD_LEFT;
        } else {
            // Otherwise scrolling right
            kEvent = KeyEvent.KEYCODE_DPAD_RIGHT;
        }
        onKeyDown(kEvent, null);
        return true;
    }
    public void setLoader(OnPhotoLoader loader) {
        this.mLoader = loader;
    }
    private boolean isScrollingLeft(MotionEvent e1, MotionEvent e2) {
        return e2.getX() > e1.getX();
    }

    private class GalleryAdapter extends BaseAdapter {

        public GalleryAdapter() {
            init();
        }

        private void init() {
            for (int i = 0; i < mImageViewListSize; i++) {
                mViews.add(null);
            }
        }

        @Override
        public int getCount() {
            if (mPhotoListUrl == null || mPhotoListUrl.size() == 0) {
                return 0;
            }
            if (mPhotoListUrl.size() == 1) {
                return 1;
            }
            return 200;
        }

        @Override
        public Object getItem(int position) {
            if (mPhotoListUrl == null || mPhotoListUrl.size() == 0) {
                return null;
            }
            int pos = (position + mPhotoListUrl.size()) % mPhotoListUrl.size();
            if (mPhotoListUrl.size() <= pos) {
                return null;
            }
            return mPhotoListUrl.get(pos);
        }

        @Override
        public long getItemId(int position) {
            if (mPhotoListUrl == null || mPhotoListUrl.size() == 0) {
                return 0;
            }
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            int nowPosition = position % mViews.size();
            if (mViews.get(nowPosition) == null) {
                view = View.inflate(getContext(), R.layout.ui_item_photo, null);
                mViews.set(nowPosition, view);
            } else {
                view = mViews.get(position % mViews.size());
            }
            view.setLayoutParams(new Gallery.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            String url = mPhotoListUrl.get(position % mPhotoListUrl.size());
            if(mLoader!=null){
                mLoader.loadImage((ImageView)view.findViewById(R.id.ui_photo_iv), url);
            }
            return view;
        }
    }

    /**
     * 设置需加载的图片url,需要传入arraylist.图片会按照从左到右顺序依次下载
     * 
     * @param list
     *            图片url列表
     * @param bakground
     *            默认加载时的背景图
     */
    public void setData(final List<String> list) {

        // 异常判断
        if (list == null || list.size() == 0) {
            return;
        }

        mPhotoListUrl = list;
        this.setAdapter(new GalleryAdapter());
    }
}
