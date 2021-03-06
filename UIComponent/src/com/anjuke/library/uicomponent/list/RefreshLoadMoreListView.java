package com.anjuke.library.uicomponent.list;

import com.anjuke.uicomponent.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * 有下拉刷新,底部加载更多的listview,支持给listview加header
 * <br>修改自好租App
 */
public class RefreshLoadMoreListView extends ListView implements OnScrollListener {

    private final int RELEASE_To_REFRESH = 0;
    private final int PULL_To_REFRESH = 1;
    private final int REFRESHING = 2;
    private final int DONE = 3;
    private final int LOADING = 4;

    // 实际的padding的距离与界面上偏移距离的比例
    private final static int RATIO = 3;

    public LayoutInflater mInflater;

    private LinearLayout mHeadView;

    private TextView mTipsTextview;
    // private TextView lastUpdatedTextView;
    private ImageView mArrowImageView;
    private ProgressBar mProgressBar;

    private RotateAnimation mAnimation;
    private RotateAnimation mReverseAnimation;

    // 用于保证startY的值在一个完整的touch事件中只被记录一次
    public boolean mIsRecored;

    // private int headContentWidth;
    private int mHeadContentHeight;

    public int mStartY;
    public int mFirstItemIndex;

    private int mState;

    private boolean mIsBack;

    private OnRefreshListener mRefreshListener;

    private boolean mIsRefreshable;
    private ProgressBar mFooterLoadingBar;
    private TextView mFooterLoadingTV;
    private boolean mHasMore = true;
    private boolean mIsLoadingMore = false;

    public RefreshLoadMoreListView(Context context) {
        super(context);
        init(context);
    }

    public RefreshLoadMoreListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RefreshLoadMoreListView);
        // set the above and behind views if defined in xml
        int headerView = ta.getResourceId(R.styleable.RefreshLoadMoreListView_headerView, -1);
        if (headerView != -1) {
            addHeaderView(View.inflate(getContext(), headerView, null));
        }
        ta.recycle();
        init(context);
    }

    private void init(Context context) {
        View footerView = View.inflate(getContext(), R.layout.ui_footer_refresh, null);
        mFooterLoadingBar = (ProgressBar) footerView.findViewById(R.id.ui_footer_loading_bar);
        mFooterLoadingTV = (TextView) footerView.findViewById(R.id.ui_footer_loading_tv);
        addFooterView(footerView);

        setCacheColorHint(Color.TRANSPARENT);
        mInflater = LayoutInflater.from(context);

        mHeadView = (LinearLayout) mInflater.inflate(R.layout.ui_header_refresh, null);

        mArrowImageView = (ImageView) mHeadView.findViewById(R.id.ui_header_arrow_iv);
        mArrowImageView.setMinimumWidth(70);
        mArrowImageView.setMinimumHeight(50);
        mProgressBar = (ProgressBar) mHeadView.findViewById(R.id.ui_header_bar);
        mTipsTextview = (TextView) mHeadView.findViewById(R.id.ui_header_tip_tv);
        // lastUpdatedTextView = (TextView) headView
        // .findViewById(R.id.head_lastUpdatedTextView);

        measureView(mHeadView);
        mHeadContentHeight = mHeadView.getMeasuredHeight();
        // headContentWidth = headView.getMeasuredWidth();

        mHeadView.setPadding(0, -1 * mHeadContentHeight, 0, 0);
        mHeadView.invalidate();

        addHeaderView(mHeadView, null, false);
        setOnScrollListener(this);

        mAnimation = new RotateAnimation(0, -180, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mAnimation.setInterpolator(new LinearInterpolator());
        mAnimation.setDuration(250);
        mAnimation.setFillAfter(true);

        mReverseAnimation = new RotateAnimation(-180, 0, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mReverseAnimation.setInterpolator(new LinearInterpolator());
        mReverseAnimation.setDuration(200);
        mReverseAnimation.setFillAfter(true);

        mState = DONE;
        mIsRefreshable = false;

    }

    // public boolean isLoadingMore() {
    // return footerView.getVisibility() == View.VISIBLE;
    // }

    public void setHasMore(boolean hasMore) {
        this.mHasMore = hasMore;
        if (!hasMore) {
            finishLoadingMore();
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        mFirstItemIndex = firstVisibleItem;
        if (totalItemCount > 0 && firstVisibleItem + visibleItemCount == totalItemCount && mHasMore) {
            if (!mIsLoadingMore) {
                startLoadingMore();
                if (mRefreshListener != null)
                    mRefreshListener.onMore();
            }
        } else {
            finishLoadingMore();
        }
    }

    public void startLoadingMore() {
        mIsLoadingMore = true;
        mFooterLoadingBar.setVisibility(View.VISIBLE);
        mFooterLoadingTV.setText(R.string.ui_loading_more);
    }

    public void finishLoadingMore() {
        mIsLoadingMore = false;
        if (!mHasMore) {
            mFooterLoadingBar.setVisibility(View.GONE);
            mFooterLoadingTV.setText(R.string.ui_loading_complete);
        }
        if (getAdapter() == null || getAdapter().getCount() == 0) {
            mFooterLoadingBar.setVisibility(View.GONE);
            mFooterLoadingTV.setText(R.string.ui_loading_none);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    public boolean onTouchEvent(MotionEvent event) {

        if (mIsRefreshable) {
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mFirstItemIndex == 0 && !mIsRecored) {
                    mIsRecored = true;
                    mStartY = (int) event.getY();
                    // Log.v(TAG, "在down时候记录当前位置‘");
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mState != REFRESHING && mState != LOADING) {
                    if (mState == DONE) {
                        // 什么都不做
                    }
                    if (mState == PULL_To_REFRESH) {
                        mState = DONE;
                        changeHeaderViewByState();
                        // Log.v(TAG, "由下拉刷新状态，到done状态");
                    }
                    if (mState == RELEASE_To_REFRESH) {
                        mState = REFRESHING;
                        changeHeaderViewByState();
                        onRefresh();
                        // Log.v(TAG, "由松开刷新状态，到done状态");
                    }
                }

                mIsRecored = false;
                mIsBack = false;

                break;

            case MotionEvent.ACTION_MOVE:
                int tempY = (int) event.getY();

                if (!mIsRecored && mFirstItemIndex == 0) {
                    // Log.v(TAG, "在move时候记录下位置");
                    mIsRecored = true;
                    mStartY = tempY;
                }

                if (mState != REFRESHING && mIsRecored && mState != LOADING) {
                    // 保证在设置padding的过程中，当前的位置一直是在head，否则如果当列表超出屏幕的话，当在上推的时候，列表会同时进行滚动
                    // 可以松手去刷新了
                    if (mState == RELEASE_To_REFRESH) {
                        setSelection(0);
                        // 往上推了，推到了屏幕足够掩盖head的程度，但是还没有推到全部掩盖的地步
                        if (((tempY - mStartY) / RATIO < mHeadContentHeight) && (tempY - mStartY) > 0) {
                            mState = PULL_To_REFRESH;
                            changeHeaderViewByState();
                            // Log.v(TAG, "由松开刷新状态转变到下拉刷新状态");
                        }
                        // 一下子推到顶了
                        else if (tempY - mStartY <= 0) {
                            mState = DONE;
                            changeHeaderViewByState();
                            // Log.v(TAG, "由松开刷新状态转变到done状态");
                        }
                        // 往下拉了，或者还没有上推到屏幕顶部掩盖head的地步
                        else {
                            // 不用进行特别的操作，只用更新paddingTop的值就行了
                        }
                    }
                    // 还没有到达显示松开刷新的时候,DONE或者是PULL_To_REFRESH状态
                    if (mState == PULL_To_REFRESH) {
                        setSelection(0);
                        // 下拉到可以进入RELEASE_TO_REFRESH的状态
                        if ((tempY - mStartY) / RATIO >= mHeadContentHeight) {
                            mState = RELEASE_To_REFRESH;
                            mIsBack = true;
                            changeHeaderViewByState();
                            // Log.v(TAG, "由done或者下拉刷新状态转变到松开刷新");
                        }
                        // 上推到顶了
                        else if (tempY - mStartY <= 0) {
                            mState = DONE;
                            changeHeaderViewByState();
                            // Log.v(TAG, "由DOne或者下拉刷新状态转变到done状态");
                        }
                    }

                    // done状态下
                    if (mState == DONE) {
                        if (tempY - mStartY > 0) {
                            mState = PULL_To_REFRESH;
                            changeHeaderViewByState();
                        }
                    }

                    // 更新headView的size
                    if (mState == PULL_To_REFRESH) {
                        mHeadView.setPadding(0, -1 * mHeadContentHeight + (tempY - mStartY) / RATIO, 0, 0);
                    }

                    // 更新headView的paddingTop
                    if (mState == RELEASE_To_REFRESH) {
                        mHeadView.setPadding(0, (tempY - mStartY) / RATIO - mHeadContentHeight, 0, 0);
                    }
                }
                break;
            }
        }
        return super.onTouchEvent(event);
    }

    // 当状态改变时候，调用该方法，以更新界面
    private void changeHeaderViewByState() {
        switch (mState) {
        case RELEASE_To_REFRESH:
            mArrowImageView.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
            mTipsTextview.setVisibility(View.VISIBLE);
            // lastUpdatedTextView.setVisibility(View.VISIBLE);
            mArrowImageView.clearAnimation();
            mArrowImageView.startAnimation(mAnimation);
            mTipsTextview.setText("松开刷新");
            // Log.v(TAG, "当前状态，松开刷新");
            break;
        case PULL_To_REFRESH:
            mProgressBar.setVisibility(View.GONE);
            mTipsTextview.setVisibility(View.VISIBLE);
            // lastUpdatedTextView.setVisibility(View.VISIBLE);
            mArrowImageView.clearAnimation();
            mArrowImageView.setVisibility(View.VISIBLE);
            // 是由RELEASE_To_REFRESH状态转变来的
            if (mIsBack) {
                mIsBack = false;
                mArrowImageView.clearAnimation();
                mArrowImageView.startAnimation(mReverseAnimation);
                mTipsTextview.setText("下拉刷新");
            } else {
                mTipsTextview.setText("下拉刷新");
            }
            // Log.v(TAG, "当前状态，下拉刷新");
            break;
        case REFRESHING:
            mHeadView.setPadding(0, 0, 0, 0);
            mProgressBar.setVisibility(View.VISIBLE);
            mArrowImageView.clearAnimation();
            mArrowImageView.setVisibility(View.GONE);
            mTipsTextview.setText("正在刷新");
            // lastUpdatedTextView.setVisibility(View.VISIBLE);
            // Log.v(TAG, "当前状态,正在刷新...");
            break;
        case DONE:
            mHeadView.setPadding(0, -1 * mHeadContentHeight, 0, 0);
            mProgressBar.setVisibility(View.GONE);
            mArrowImageView.clearAnimation();
            mArrowImageView.setImageResource(R.drawable.ui_lib_arrow);
            mTipsTextview.setText("下拉刷新");
            // lastUpdatedTextView.setVisibility(View.VISIBLE);
            // Log.v(TAG, "当前状态，done");
            break;
        }
    }

    public void setOnRefreshListener(OnRefreshListener refreshListener) {
        this.mRefreshListener = refreshListener;
        mIsRefreshable = true;
    }

    public interface OnRefreshListener {
        public void onRefresh();

        public void onMore();
    }

    public void onRefreshComplete() {
        mState = DONE;
        // lastUpdatedTextView.setText("最近更新:" + new Date().toLocaleString());
        changeHeaderViewByState();
    }

    public void onRefreshing() {
        mTipsTextview.setText("正在刷新...");
        mState = REFRESHING;
        changeHeaderViewByState();
    }

    public void setStateText(String text) {
        if (mTipsTextview != null) {
            mTipsTextview.setText(text);
        }
    }

    private void onRefresh() {
        mHasMore = true;
        if (mRefreshListener != null) {
            mRefreshListener.onRefresh();
        }
    }

    // 此方法直接照搬自网络上的一个下拉刷新的demo，此处是“估计”headView的width以及height
    private void measureView(View child) {
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }

    public int getState() {
        return mState;
    }

}
