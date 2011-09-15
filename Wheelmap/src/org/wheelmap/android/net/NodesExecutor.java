package org.wheelmap.android.net;

import org.wheelmap.android.model.Wheelmap;
import org.wheelmap.android.service.SyncService;
import org.wheelmap.android.utils.GeocoordinatesMath;
import org.wheelmap.android.utils.ParceableBoundingBox;

import wheelmap.org.BoundingBox;
import wheelmap.org.WheelchairState;
import wheelmap.org.BoundingBox.Wgs84GeoCoordinates;
import wheelmap.org.domain.node.Node;
import wheelmap.org.domain.node.Nodes;
import wheelmap.org.request.AcceptType;
import wheelmap.org.request.NodesRequestBuilder;
import wheelmap.org.request.Paging;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class NodesExecutor extends BaseRetrieveExecutor<Nodes> implements IExecutor {
	public static final String PREF_KEY_WHEELCHAIR_STATE = "wheelchairState";
	
	private BoundingBox mBoundingBox;
	private WheelchairState mWheelchairState;
	private Context mContext;
	

	public NodesExecutor(Context context, ContentResolver resolver, Bundle bundle) {
		super(resolver, bundle, Nodes.class);
		mContext = context;
	}

	@Override
	public void prepareContent() {
		if (getBundle().containsKey( SyncService.EXTRA_BOUNDING_BOX)) {
			ParceableBoundingBox parcBoundingBox = (ParceableBoundingBox) getBundle()
					.getSerializable(SyncService.EXTRA_BOUNDING_BOX);
			mBoundingBox = parcBoundingBox.toBoundingBox();
			Log.d(TAG,
					"retrieving with bounding box: "
							+ parcBoundingBox.toString());
		} else if ( getBundle().containsKey( SyncService.EXTRA_LOCATION)) {
			float distance = getBundle().getFloat( SyncService.EXTRA_DISTANCE_LIMIT);
			Location location = (Location) getBundle()
					.getParcelable( SyncService.EXTRA_LOCATION);
			mBoundingBox = GeocoordinatesMath.calculateBoundingBox(
					new Wgs84GeoCoordinates(location.getLongitude(),
							location.getLatitude()), distance);
			Log.d(TAG,
					"retrieving with current location = ("
							+ location.getLongitude() + ","
							+ location.getLatitude() + ") and distance = "
							+ distance);
		}
		
		mWheelchairState = getWheelchairStateFromPreferences();
		
		deleteRetrievedData();
	}
	
	@Override
	public void execute() throws ExecutorException {
		final long startRemote = System.currentTimeMillis();
		// Retrieve all Pages is terribly slow. Anybody knows why?
		// mRemoteExecutor.retrieveAllPages(bb, wheelState);
		final NodesRequestBuilder requestBuilder = new NodesRequestBuilder( SERVER, API_KEY, AcceptType.JSON );
		// 1. maxi nodes
		requestBuilder.paging(new Paging(DEFAULT_TEST_PAGE_SIZE))
				.boundingBox(mBoundingBox);
		requestBuilder.wheelchairState(mWheelchairState);

		clearTempStore();
		try {
			// retrieveAllPages( requestBuilder );
			retrieveSinglePage(requestBuilder);
		} catch ( Exception e ) {
			throw new ExecutorException( e );
		}
		Log.d(TAG, "remote sync took "
				+ (System.currentTimeMillis() - startRemote) + "ms");
	}

	@Override
	public void prepareDatabase() {
		long insertStart = System.currentTimeMillis();
		for( Nodes nodes: getTempStore() ) {
			bulkInsert(nodes);
		}
		long insertEnd = System.currentTimeMillis();
		Log.d(TAG, "insertTime = " + (insertEnd - insertStart) / 1000f);
		clearTempStore();
	}
	
	private void deleteRetrievedData() {
		String whereClause = "( " + Wheelmap.POIs.UPDATE_TAG + " = ? )";
		String[] whereValues = new String[]{ String.valueOf(Wheelmap.UPDATE_NO) };
		getResolver().delete(Wheelmap.POIs.CONTENT_URI, whereClause, whereValues);
	}
	
	private WheelchairState getWheelchairStateFromPreferences() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		String prefWheelchairState = prefs.getString(PREF_KEY_WHEELCHAIR_STATE,
				String.valueOf( WheelchairState.DEFAULT.getId()));
		WheelchairState ws = WheelchairState.valueOf(Integer
				.valueOf(prefWheelchairState));
		return ws;
	}
	
	private void bulkInsert(Nodes nodes) {
		long makeupTime = System.currentTimeMillis();
		int size = nodes.getMeta().getItemCount().intValue();
		ContentValues[] contentValuesArray = new ContentValues[size];
		for (int i = 0; i < size; i++) {
			ContentValues values = new ContentValues();
			copyNodeToValues(nodes.getNodes().get(i), values);
			
			contentValuesArray[i] = values;
		}
		long bulkInsertTime = System.currentTimeMillis();
		Log.d( TAG, "makeupTime = " + (bulkInsertTime - makeupTime ) / 1000f);
		int count = getResolver().bulkInsert( Wheelmap.POIs.CONTENT_URI, contentValuesArray );
		long bulkInsertDoneTime = System.currentTimeMillis();
		Log.d( TAG, "bulkInsertTime = " + (bulkInsertDoneTime - bulkInsertTime ) / 1000f );
		Log.d( TAG, "Inserted records count = " + count );
	}
	
	private void copyNodeToValues(Node node, ContentValues values) {
		values.clear();
		values.put(Wheelmap.POIs.WM_ID, node.getId().intValue());
		values.put(Wheelmap.POIs.NAME, node.getName());
		values.put(Wheelmap.POIs.COORD_LAT,
				Math.ceil(node.getLat().doubleValue() * 1E6));
		values.put(Wheelmap.POIs.COORD_LON,
				Math.ceil(node.getLon().doubleValue() * 1E6));
		values.put(Wheelmap.POIs.STREET, node.getStreet());
		values.put(Wheelmap.POIs.HOUSE_NUM, node.getHousenumber());
		values.put(Wheelmap.POIs.POSTCODE, node.getPostcode());
		values.put(Wheelmap.POIs.CITY, node.getCity());
		values.put(Wheelmap.POIs.PHONE, node.getPhone());
		values.put(Wheelmap.POIs.WEBSITE, node.getWebsite());
		values.put(Wheelmap.POIs.WHEELCHAIR, WheelchairState.myValueOf( node.getWheelchair()).getId());
		values.put(Wheelmap.POIs.WHEELCHAIR_DESC,
				node.getWheelchairDescription());
		values.put(Wheelmap.POIs.CATEGORY_ID, node.getCategory().getId().intValue());
		values.put(Wheelmap.POIs.CATEGORY_IDENTIFIER, node.getCategory().getIdentifier());
		values.put(Wheelmap.POIs.NODETYPE_ID, node.getNodeType().getId().intValue());
		values.put(Wheelmap.POIs.NODETYPE_IDENTIFIER, node.getNodeType().getIdentifier());		
		values.put(Wheelmap.POIs.UPDATE_TAG, Wheelmap.UPDATE_NO);
	}
}