package com.imaginea.android.sugarcrm;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.imaginea.android.sugarcrm.CustomActionbar.Action;
import com.imaginea.android.sugarcrm.CustomActionbar.IntentAction;
import com.imaginea.android.sugarcrm.provider.DatabaseHelper;
import com.imaginea.android.sugarcrm.provider.SugarCRMContent.Contacts;
import com.imaginea.android.sugarcrm.ui.BaseMultiPaneActivity;
import com.imaginea.android.sugarcrm.util.Util;
import com.imaginea.android.sugarcrm.util.ViewUtil;

import java.util.Map;
import java.util.Map.Entry;

/**
 * RecentListActivity, lists the view projections for all the Recently accessed records.
 * 
 * 
 * @author Jagadeeshwaran K
 */
public class RecentModuleListFragment extends ListFragment {

    private ListView mListView;

    private View mEmpty;

    private View mListFooterView;

    private TextView mListFooterText;

    private View mListFooterProgress;

    private boolean mBusy = false;

    private String mModuleName;

    private Uri mModuleUri;

    private Uri mIntentUri;

    // we don't make this final as we may want to use the sugarCRM value
    // dynamically, but prevent
    // others from modiying anyway
    // private static int mMaxResults = 20;

    private DatabaseHelper mDbHelper;

    private GenericCursorAdapter mAdapter;

    private String mSelections = ModuleFields.DELETED + "=?";

    private String[] mSelectionArgs = new String[] { Util.EXCLUDE_DELETED_ITEMS };

    private SugarCrmApp app;

    public final static String LOG_TAG = "RecentModuleList";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.common_list, container, false);

    }

    /** {@inheritDoc} */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mDbHelper = new DatabaseHelper(getActivity().getBaseContext());
        app = (SugarCrmApp) getActivity().getApplication();
        Intent intent = getActivity().getIntent();
        // final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
        Bundle extras = intent.getExtras();
        mModuleName = Util.CONTACTS;
        if (extras != null) {
            mModuleName = extras.getString(RestUtilConstants.MODULE_NAME);
        }

        // If the list is a list of related items, hide the filterImage and
        // allItems image
        // if (intent.getData() != null && intent.getData().getPathSegments().size() >= 3) {
        // getActivity().findViewById(R.id.filterImage).setVisibility(View.GONE);
        // getActivity().findViewById(R.id.allItems).setVisibility(View.GONE);
        // }

        // TextView tv = (TextView) getActivity().findViewById(R.id.headerText);
        // tv.setText(mModuleName);
        final CustomActionbar actionBar = (CustomActionbar) getActivity().findViewById(R.id.custom_actionbar);
        actionBar.setTitle(mModuleName);

        final Action homeAction = new IntentAction(RecentModuleListFragment.this.getActivity(), new Intent(RecentModuleListFragment.this.getActivity(), DashboardActivity.class), R.drawable.home);
        actionBar.setHomeAction(homeAction);

        mListView = getListView();

        mIntentUri = intent.getData();
        // mListView.setOnScrollListener(this);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {

                openDetailScreen(position);
            }
        });

        // button code in the layout - 1.6 SDK feature to specify onClick
        mListView.setItemsCanFocus(true);
        mListView.setFocusable(true);
        mEmpty = getActivity().findViewById(R.id.empty);
        mListView.setEmptyView(mEmpty);
        // registerForContextMenu(getListView());

        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
            Log.d(LOG_TAG, "ModuleName:-->" + mModuleName);
        }

        mModuleUri = mDbHelper.getModuleUri(mModuleName);
        if (mIntentUri == null) {
            intent.setData(mModuleUri);
            mIntentUri = mModuleUri;
        }
        // Perform a managed query. The Activity will handle closing and
        // requerying the cursor
        // when needed.
        // TODO - optimize this, if we sync up a dataset, then no need to run
        // detail projection
        // here, just do a list projection
        Cursor cursor = getActivity().managedQuery(intent.getData(), mDbHelper.getModuleProjections(mModuleName), mSelections, mSelectionArgs, getSortOrder());

        // CRMContentObserver observer = new CRMContentObserver()
        // cursor.registerContentObserver(observer);
        String[] moduleSel = mDbHelper.getModuleListSelections(mModuleName);
        if (moduleSel.length >= 2)
            mAdapter = new GenericCursorAdapter(this.getActivity(), R.layout.contact_listitem, cursor, moduleSel, new int[] {
                    android.R.id.text1, android.R.id.text2 });
        else
            mAdapter = new GenericCursorAdapter(this.getActivity(), R.layout.contact_listitem, cursor, moduleSel, new int[] { android.R.id.text1 });
        setListAdapter(mAdapter);
        // make the list filterable using the keyboard
        mListView.setTextFilterEnabled(true);

        TextView tv1 = (TextView) (mEmpty.findViewById(R.id.mainText));

        if (mAdapter.getCount() == 0) {
            mListView.setVisibility(View.GONE);
            mEmpty.findViewById(R.id.progress).setVisibility(View.INVISIBLE);
            tv1.setVisibility(View.VISIBLE);
            if (mIntentUri != null) {
                tv1.setText("No " + mModuleName + " found");
            }
        } else {
            mEmpty.findViewById(R.id.progress).setVisibility(View.VISIBLE);
            tv1.setVisibility(View.GONE);
        }

        mListFooterView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.list_item_footer, mListView, false);
        getListView().addFooterView(mListFooterView);
        mListFooterText = (TextView) getActivity().findViewById(R.id.status);

        mListFooterProgress = mListFooterView.findViewById(R.id.progress);
        if (ViewUtil.isHoneycombTablet(getActivity()) && mAdapter.getCount() != 0)
            openDetailScreen(0);

    }

    /**
     * GenericCursorAdapter
     */
    private final class GenericCursorAdapter extends SimpleCursorAdapter implements Filterable {

        private int realoffset = 0;

        private int limit = 20;

        private ContentResolver mContent;

        public GenericCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to);
            mContent = context.getContentResolver();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View v = super.getView(position, convertView, parent);
            int count = getCursor().getCount();
            Log.d(LOG_TAG, "Get Item" + getItemId(position));
            if (!mBusy && position != 0 && position == count - 1) {
                mBusy = true;
                realoffset += count;
                // Uri uri = getIntent().getData();
                // TODO - fix this, this is no longer used
                Uri newUri = Uri.withAppendedPath(Contacts.CONTENT_URI, realoffset + "/" + limit);
                Log.d(LOG_TAG, "Changing cursor:" + newUri.toString());
                final Cursor cursor = getActivity().managedQuery(newUri, Contacts.LIST_PROJECTION, null, null, Contacts.DEFAULT_SORT_ORDER);
                CRMContentObserver observer = new CRMContentObserver(new Handler() {

                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        Log.d(LOG_TAG, "Changing cursor: in handler");
                        changeCursor(cursor);
                        mListFooterText.setVisibility(View.GONE);
                        mListFooterProgress.setVisibility(View.GONE);
                        mBusy = false;
                    }
                });
                cursor.registerContentObserver(observer);
            }
            if (mBusy) {
                mListFooterProgress.setVisibility(View.VISIBLE);
                mListFooterText.setVisibility(View.VISIBLE);
                mListFooterText.setText("Loading...");
                // Non-null tag means the view still needs to load it's data
                // text.setTag(this);
            }
            return v;
        }

        @Override
        public String convertToString(Cursor cursor) {
            Log.i(LOG_TAG, "convertToString : " + cursor.getString(2));
            return cursor.getString(2);
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            if (getFilterQueryProvider() != null) {
                return getFilterQueryProvider().runQuery(constraint);
            }

            StringBuilder buffer = null;
            String[] args = null;
            if (constraint != null) {
                buffer = new StringBuilder();
                buffer.append("UPPER(");
                buffer.append(mDbHelper.getModuleListSelections(mModuleName)[0]);
                buffer.append(") GLOB ?");
                args = new String[] { constraint.toString().toUpperCase() + "*" };
            }

            return mContent.query(mDbHelper.getModuleUri(mModuleName), mDbHelper.getModuleListProjections(mModuleName), buffer == null ? null
                                            : buffer.toString(), args, mDbHelper.getModuleSortOrder(mModuleName));
        }
    }

    /**
     * opens the Detail Screen
     * 
     * @param position
     */
    void openDetailScreen(int position) {
        Intent detailIntent = new Intent(this.getActivity(), ModuleDetailActivity.class);

        Cursor cursor = (Cursor) getListAdapter().getItem(position);
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }
        // use the details available from cursor to open detailed view
        detailIntent.putExtra(Util.ROW_ID, cursor.getString(1));
        detailIntent.putExtra(RestUtilConstants.BEAN_ID, cursor.getString(2));
        detailIntent.putExtra(RestUtilConstants.MODULE_NAME, cursor.getString(3));
        detailIntent.putExtra("Recent", true);
        Log.d(LOG_TAG, "rowId:" + cursor.getString(1) + "BEAN_ID:" + cursor.getString(2)
                                        + "MODULE_NAME:" + cursor.getString(3));

        if (ViewUtil.isTablet(getActivity())) {
            ((BaseMultiPaneActivity) getActivity()).openActivityOrFragment(detailIntent);
        } else {
            startActivity(detailIntent);
        }

    }

    /** {@inheritDoc} */
    @Override
    public void onPause() {
        super.onPause();
    }

    public void showAssignedItems(View view) {
        // keep this empty as the header is used from list view
    }

    /**
     * <p>
     * showAllItems
     * </p>
     * 
     * @param view
     *            a {@link android.view.View} object.
     */
    public void showAllItems(View view) {
        Cursor cursor = getActivity().managedQuery(getActivity().getIntent().getData(), mDbHelper.getModuleProjections(mModuleName), null, null, getSortOrder());
        mAdapter.changeCursor(cursor);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * <p>
     * showHome
     * </p>
     * 
     * @param view
     *            a {@link android.view.View} object.
     */
    public void showHome(View view) {
        Intent homeIntent = new Intent(this.getActivity(), DashboardActivity.class);
        startActivity(homeIntent);
    }

    private String getSortOrder() {
        String sortOrder = null;
        Map<String, String> sortOrderMap = app.getModuleSortOrder(mModuleName);
        for (Entry<String, String> entry : sortOrderMap.entrySet()) {
            sortOrder = entry.getKey() + " " + entry.getValue();
        }
        return sortOrder;
    }

}
