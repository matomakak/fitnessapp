package com.hlavackamartin.fitnessapp.recognition.activity;

import android.app.FragmentManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.support.wearable.activity.WearableActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import com.hlavackamartin.fitnessapp.recognition.R;
import com.hlavackamartin.fitnessapp.recognition.data.MainMenuItem;
import com.hlavackamartin.fitnessapp.recognition.data.MainMenuItem.MenuType;
import com.hlavackamartin.fitnessapp.recognition.fragment.FitnessAppFragment;
import com.hlavackamartin.fitnessapp.recognition.fragment.impl.DetectionFragment;
import com.hlavackamartin.fitnessapp.recognition.fragment.impl.LearningFragment;
import com.hlavackamartin.fitnessapp.recognition.fragment.impl.SyncFragment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Main ACTIVITY class providing functionality for selecting module and showing it's content
 */
public class MainActivity extends WearableActivity implements
    WearableNavigationDrawerView.OnItemSelectedListener,
    MenuItem.OnMenuItemClickListener {

  /**
   * Bundle key for current selected module retrieval
   */
  private static final String FRAGMENT_POSITION_BUNDLE_KEY = "FRAGMENT_POSITION_BUNDLE_KEY";
  /**
   * Available modules represented by Fragment
   */
  private List<FitnessAppFragment> mFragments = new ArrayList<>();
  /**
   * Current selected module
   */
  private int mActiveFragment;

  private NavigationAdapter mNavigationAdapter;
  private WearableNavigationDrawerView mWearableNavigationDrawer;
  private WearableActionDrawerView mWearableActionDrawer;

  /**
   * Providing functionality for saving current selected module
   *
   * @param outState used to store information
   */
  @Override
  public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
    outState.putInt(FRAGMENT_POSITION_BUNDLE_KEY, mActiveFragment);
    super.onSaveInstanceState(outState, outPersistentState);
  }

  /**
   * Initialization of supporting objects and setting application behaviour
   *
   * @param state last selected module holder
   */
  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setAmbientEnabled();

    setContentView(R.layout.activity_main);
    // Top Navigation Drawer
    mWearableNavigationDrawer = findViewById(R.id.top_nav);
    mNavigationAdapter = new NavigationAdapter(this);
    mWearableNavigationDrawer.setAdapter(mNavigationAdapter);
    mWearableNavigationDrawer.addOnItemSelectedListener(this);

    // Bottom Action Drawer
    mWearableActionDrawer = findViewById(R.id.bottom_nav);
    // Peeks action drawer on the bottom.
    mWearableActionDrawer.setOnMenuItemClickListener(this);

    mFragments.add(MainMenuItem.MenuType.RECOGNITION.getPos(), new DetectionFragment());
    mFragments.add(MainMenuItem.MenuType.LEARNING.getPos(), new LearningFragment());
    mFragments.add(MainMenuItem.MenuType.SYNC.getPos(), new SyncFragment());
    mActiveFragment = state != null ?
        state.getInt(FRAGMENT_POSITION_BUNDLE_KEY, MainMenuItem.MenuType.LEARNING.getPos()) :
        MenuType.RECOGNITION.getPos();

    mWearableNavigationDrawer.getController().peekDrawer();
    mWearableActionDrawer.getController().peekDrawer();
  }

  /**
   * Repopulation of current fragment after application regains focus
   */
  @Override
  protected void onResume() {
    super.onResume();
    updateCurrentFragment();
    mWearableNavigationDrawer.setCurrentItem(mActiveFragment, false);
  }

  /**
   * Population of current selected module and its' data
   */
  private void updateCurrentFragment() {
    if (getCurrentFragment().isPresent()) {
      FragmentManager fragmentManager = getFragmentManager();
      FitnessAppFragment fragment = getCurrentFragment().get();
      fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

      mWearableActionDrawer.getMenu().clear();
      int i = 0;
      List<String> items = fragment.getActionMenu(getResources());
      mWearableActionDrawer.setLockedWhenClosed(items == null);
      if (items == null) {
        mWearableActionDrawer.getController().closeDrawer();
      } else {
        for (String itemName : items) {
          MenuItem item = mWearableActionDrawer.getMenu()
              .add(Menu.NONE, i, i, itemName);
          item.setIcon(R.drawable.ic_barbell_white_48dp);
          i++;
        }
        if (!items.isEmpty()) {
          MenuItem separator = mWearableActionDrawer.getMenu().add(Menu.NONE, i, i, "");
          separator.setIcon(R.drawable.ic_expand_more_white_22);
          i++;
        }

        MenuItem reset = mWearableActionDrawer.getMenu()
            .add(Menu.NONE, i, i, getResources().getString(R.string.restart_all));
        reset.setIcon(R.drawable.ic_reset_white_48dp);
        mWearableActionDrawer.getController().peekDrawer();
      }
    }
  }

  private Optional<FitnessAppFragment> getCurrentFragment() {
    return Optional.ofNullable(mFragments.get(mActiveFragment));
  }

  @Override
  public void onItemSelected(int position) {
    mNavigationAdapter.executeAction(position);
  }

  /**
   * Bottom menu execution functionality through fragments' implementation
   *
   * @param menuItem selected item
   * @return event consumed
   */
  @Override
  public boolean onMenuItemClick(MenuItem menuItem) {
    boolean res =
        getCurrentFragment().isPresent() && getCurrentFragment().get().onMenuItemClick(menuItem);
    mWearableActionDrawer.getController().closeDrawer();
    mWearableActionDrawer.getController().peekDrawer();
    return res;
  }

  /**
   * Support for ambient mode in Wear
   */
  @Override
  public void onEnterAmbient(Bundle ambientDetails) {
    super.onEnterAmbient(ambientDetails);
    getCurrentFragment().ifPresent(f -> f.onEnterAmbient(ambientDetails));
    mWearableNavigationDrawer.getController().closeDrawer();
    mWearableActionDrawer.getController().closeDrawer();
  }

  /**
   * Support for ambient mode in Wear
   */
  @Override
  public void onExitAmbient() {
    super.onExitAmbient();
    getCurrentFragment().ifPresent(FitnessAppFragment::onExitAmbient);
    mWearableNavigationDrawer.getController().peekDrawer();
    mWearableActionDrawer.getController().peekDrawer();
  }

  /**
   * Implementation for Top menu functionality
   */
  private final class NavigationAdapter
      extends WearableNavigationDrawerView.WearableNavigationDrawerAdapter {

    private final MainActivity mContext;
    private final ArrayList<MainMenuItem> mMainMenu;

    NavigationAdapter(MainActivity context) {
      mContext = context;
      mMainMenu = new ArrayList<>();

      for (MainMenuItem.MenuType type : MainMenuItem.MenuType.values()) {
        int resourceId =
            mContext.getResources()
                .getIdentifier(type.getName(), "array", getPackageName());
        String[] details = mContext.getResources().getStringArray(resourceId);

        mMainMenu.add(new MainMenuItem(type, details[0], details[1]));
      }
    }

    @Override
    public int getCount() {
      return mMainMenu.size();
    }

    @Override
    public String getItemText(int pos) {
      return mMainMenu.get(pos).getName();
    }

    /**
     * Retrieval of image button via strings resource
     */
    @Override
    public Drawable getItemDrawable(int pos) {
      return mContext.getDrawable(mContext.getResources()
          .getIdentifier(mMainMenu.get(pos).getImage(), "drawable", getPackageName()));
    }

    /**
     * Setting active module and updating content after selection
     *
     * @param pos selected module position in navigation drawer
     */
    void executeAction(int pos) {
      mActiveFragment = mMainMenu.get(pos).getType().getPos();
      updateCurrentFragment();
    }
  }
}
