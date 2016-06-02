package com.example.xyzreader.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailActivity";
    private final int offset = 20;
    private final int delay = 500;
    private Cursor mCursor;
    private long mStartId;
    private long mSelectedItemId;
    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private int animFactor = 2;
    private ValueAnimator animator = new ValueAnimator();
    private boolean hasRunBefore = false;

    private void animateViewPager() {
//        hasRunBefore = true;
        if (!animator.isRunning()) {
            animator.removeAllUpdateListeners();
            animator.removeAllListeners();
            //Set animation
            animator.setIntValues(0, -offset);
            animator.setDuration(delay);
            animator.setRepeatCount(1);
            animator.setRepeatMode(ValueAnimator.RESTART);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    Integer value = animFactor * (Integer) animation.getAnimatedValue();
                    if (!mPager.isFakeDragging()) {
                        mPager.beginFakeDrag();
                    }
                    mPager.fakeDragBy(value);
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationStart(Animator animation) {
                    animFactor = 1;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mPager.endFakeDrag();
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                    animFactor = -1;
                }
            });
            animator.start();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
                Log.e(TAG, "onCreate: select" + mSelectedItemId);
            }
        }


        setContentView(R.layout.activity_article_detail);

        getLoaderManager().initLoader(0, null, this);
        View mSwipe = (View) findViewById(R.id.swipe_hint_panel);
        mSwipe.bringToFront();
        mSwipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Log.d(TAG, "onClick: ");
                    animateViewPager();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
//            @Override
//            public void onPageScrollStateChanged(int state) {
//                super.onPageScrollStateChanged(state);
//                mUpButton.animate()
//                        .alpha((state == ViewPager.SCROLL_STATE_IDLE) ? 1f : 0f)
//                        .setDuration(300);
//            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
            }
        });



    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
        try {
            if (mStartId > 0) animateViewPager();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        Log.e(TAG, "onLoaderReset: ");
        mPagerAdapter.notifyDataSetChanged();
    }


    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }


        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}


