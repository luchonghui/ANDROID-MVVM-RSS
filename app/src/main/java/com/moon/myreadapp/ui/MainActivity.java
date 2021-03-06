package com.moon.myreadapp.ui;

import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.moon.appframework.action.RouterAction;
import com.moon.appframework.common.log.XLog;
import com.moon.appframework.core.XDispatcher;
import com.moon.myreadapp.R;
import com.moon.myreadapp.common.components.pulltorefresh.PullToRefreshBase;
import com.moon.myreadapp.common.event.SynchronizeStateEvent;
import com.moon.myreadapp.common.event.UpdateFeedEvent;
import com.moon.myreadapp.common.event.UpdateFeedListEvent;
import com.moon.myreadapp.common.event.UpdateUIEvent;
import com.moon.myreadapp.common.event.UpdateUserEvent;
import com.moon.myreadapp.databinding.ActivityHomeBinding;
import com.moon.myreadapp.databinding.NoticeBinding;
import com.moon.myreadapp.mvvm.viewmodels.DrawerViewModel;
import com.moon.myreadapp.mvvm.viewmodels.MainViewModel;
import com.moon.myreadapp.ui.base.BaseActivity;
import com.moon.myreadapp.util.Conver;
import com.nineoldandroids.animation.ObjectAnimator;

import java.util.Date;

import de.halfbit.tinybus.Subscribe;


public class MainActivity extends BaseActivity {

    private Toolbar toolbar;

    private ActionBarDrawerToggle mDrawerToggle;

    ActivityHomeBinding binding;

    NoticeBinding noticeBinding;

    private DrawerViewModel drawerViewModel;
    private MainViewModel mainViewModel;

    @Override
    protected void onDestroy() {
        if (drawerViewModel != null) {
            drawerViewModel.clear();
            drawerViewModel = null;
        }
        if (mainViewModel != null) {
            mainViewModel.clear();
            mainViewModel = null;
        }
        super.onDestroy();
    }

    @Override
    protected void initToolBar(Toolbar toolbar) {
        super.initToolBar(toolbar);
        toolbar.setNavigationIcon(android.R.drawable.ic_dialog_dialer);
    }

    @Override
    public void setContentViewAndBindVm(Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(LayoutInflater.from(this), getLayoutView(), null, false);
        setContentView(binding.getRoot());

       // noticeBinding = DataBindingUtil.inflate(getLayoutInflater(),R.layout.notice,null,false);

        this.drawerViewModel = new DrawerViewModel(this);
        binding.setDrawerViewModel(drawerViewModel);

        this.mainViewModel = new MainViewModel(this);
        binding.setMainViewModel(mainViewModel);
    }

    private void initBusiness() {
        initDrawer();
        initMainView();
    }

    private void initDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, binding.drawerLayout, 0, 0) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
                //toolbar.setTitle("open");
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
                //toolbar.setTitle("close");
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);

                //切换的效果 当使用整个屏幕宽度来算的话,如果屏幕过宽,可能出现leftdraw和miancontent之间出现空白
                //int dis = (int) (slideOffset * getScreenWidth() / 3.0);
                //所以直接使用leftdraw的宽度来计算
                int dis = (int) (slideOffset * binding.leftDrawer.leftDrawerListview.getWidth() / 3.0);
                binding.mainContent.scrollTo(-dis, 0);
            }
        };
        binding.drawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void initMainView() {
        //必须先设置了adapter,才能进行add head\footer,设置刷新等等操作.
        binding.mainList.setAdapter(mainViewModel.getFeedRecAdapter());
//        binding.mainList.getmAdapter().addFooter(noticeBinding.getRoot());
//        noticeBinding.notice.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                XDispatcher.from(MainActivity.this).dispatch(new RouterAction(AddFeedActivity.class,true));
//            }
//        });
        binding.mainList.setPullLoadEnabled(false);
        //binding.mainList.setScrollLoadEnabled(true);
//        binding.mainList.setPullRefreshEnabled(false);
        binding.mainList.getRefreshableView().addOnItemTouchListener(mainViewModel.getReadItemClickListener());
        binding.mainList.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<RecyclerView>() {
            @Override
            public void onPullDownToRefresh(PullToRefreshBase<RecyclerView> refreshView) {
                binding.mainList.getHeaderLoadingLayout().setLastUpdatedLabel(Conver.ConverToString(new Date(), "HH:mm"));
                //下拉刷新
                //binding.mainList.onPullDownRefreshComplete();
                binding.mainList.post(new Runnable() {
                    @Override
                    public void run() {
                        //加载本地数据
                        mainViewModel.updateFeeds();
                        binding.mainList.onPullDownRefreshComplete();
                        mainViewModel.refreshAll();
                        binding.mainList.setHasMoreData(false);
                    }
                });
            }

            @Override
            public void onPullUpToRefresh(PullToRefreshBase<RecyclerView> refreshView) {
                //TODO feed 页相对比较少,所以就没有做分页了,后面要加上
                binding.mainList.setHasMoreData(false);
                //上拉加载
                binding.mainList.onPullUpRefreshComplete();
            }
        });
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainViewModel.refreshAll();
            }
        });
    }

    @Override
    protected int getLayoutView() {
        return R.layout.activity_home;
    }


    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        initToolBar(toolbar);
        initBusiness();
        mDrawerToggle.syncState();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...
        int id = item.getItemId();
        if (id == R.id.action_addsub) {
            //
            //mainViewModel.refreshAll();
            XDispatcher.from(this).dispatch(new RouterAction(AddFeedActivity.class,true));
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe
    public void onUpdateEvent(UpdateFeedEvent event) {
        //XLog.d("get mes:" + "UpdateFeedEvent");
        mainViewModel.updateFeed(event);
    }

    @Subscribe
    public void onUpdateEvent(UpdateFeedListEvent event) {
        mainViewModel.updateFeeds();
    }

    @Subscribe
    public void onUpdateEvent(UpdateUserEvent event) {
        XLog.d("get mes:" + "UpdateUserEvent");
        drawerViewModel.updateUser(event.getUser());
        drawerViewModel.synchroUserInfo();
    }
    @Subscribe
    @Override
    public void onUpdateEvent(UpdateUIEvent event) {
        super.onUpdateEvent(event);
        if (event.getStatus() == UpdateUIEvent.ARTIVIE_COUNT_CHANGE){
            drawerViewModel.updateTips();
        }
    }

    @Subscribe
    public void onUpdateEvent(SynchronizeStateEvent event) {
        drawerViewModel.setSyncState(event.getSyncState());
    }

    public void btnOnClick(View v) {
        mainViewModel.btnOnClick(v);
    }


    @Override
    protected Toolbar getToolBar() {
        return toolbar;
    }


}
