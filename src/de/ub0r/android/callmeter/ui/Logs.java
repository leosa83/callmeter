/*
 * Copyright (C) 2009-2010 Felix Bechstein
 * 
 * This file is part of Call Meter 3G.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.callmeter.ui;

import java.util.Calendar;
import java.util.Date;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ResourceCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.LogRunnerService;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.lib.Utils;

/**
 * Callmeter's Log {@link ListActivity}.
 * 
 * @author flx
 */
public final class Logs extends ListActivity implements OnClickListener,
		OnItemLongClickListener {
	/** Tag for output. */
	public static final String TAG = "logs";

	/** Prefs: {@link ToggleButton} state for calls. */
	private static final String PREF_CALL = "_logs_call";
	/** Prefs: {@link ToggleButton} state for sms. */
	private static final String PREF_SMS = "_logs_sms";
	/** Prefs: {@link ToggleButton} state for mms. */
	private static final String PREF_MMS = "_logs_mms";
	/** Prefs: {@link ToggleButton} state for data. */
	private static final String PREF_DATA = "_logs_data";

	/** {@link ToggleButton}s. */
	private ToggleButton tbCall, tbSMS, tbMMS, tbData;
	/** Show hours and days. */
	private static boolean showHours = true;

	/**
	 * Adapter binding logs to View.
	 * 
	 * @author flx
	 */
	public class LogAdapter extends ResourceCursorAdapter {
		/** Currency format. */
		private final String cformat;

		/**
		 * Default Constructor.
		 * 
		 * @param where
		 *            slection
		 * @param context
		 *            {@link Context}
		 */
		public LogAdapter(final Context context, final String where) {
			super(context, R.layout.logs_item, context.getContentResolver()
					.query(DataProvider.Logs.CONTENT_URI,
							DataProvider.Logs.PROJECTION, where, null,
							DataProvider.Logs.DATE + " DESC"), true);
			this.cformat = Preferences.getCurrencyFormat(context);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final void bindView(final View view, final Context context,
				final Cursor cursor) {
			final ContentResolver cr = context.getContentResolver();
			final StringBuilder buf = new StringBuilder();
			final int t = cursor.getInt(DataProvider.Logs.INDEX_TYPE);
			String[] strs = context.getResources().getStringArray(
					R.array.plans_type);
			buf.append(strs[t]);
			final int dir = cursor.getInt(DataProvider.Logs.INDEX_DIRECTION);
			strs = context.getResources().getStringArray(
					R.array.direction_calls);
			buf.append(" (" + strs[dir] + "): ");
			final long date = cursor.getLong(DataProvider.Logs.INDEX_DATE);
			buf
					.append(DateFormat.getDateFormat(context).format(
							new Date(date)));
			buf.append(" ");
			buf
					.append(DateFormat.getTimeFormat(context).format(
							new Date(date)));
			((TextView) view.findViewById(android.R.id.text1)).setText(buf
					.toString());

			((TextView) view.findViewById(R.id.plan))
					.setText(DataProvider.Plans.getName(cr, cursor
							.getLong(DataProvider.Logs.INDEX_PLAN_ID)));

			((TextView) view.findViewById(R.id.rule))
					.setText(DataProvider.Rules.getName(cr, cursor
							.getLong(DataProvider.Logs.INDEX_RULE_ID)));

			String s = cursor.getString(DataProvider.Logs.INDEX_REMOTE);
			if (s == null || s.trim().length() == 0) {
				view.findViewById(R.id.remote).setVisibility(View.GONE);
				view.findViewById(R.id.remote_).setVisibility(View.GONE);
			} else {
				final TextView tw = (TextView) view.findViewById(R.id.remote);
				tw.setVisibility(View.VISIBLE);
				view.findViewById(R.id.remote_).setVisibility(View.VISIBLE);
				tw.setText(s);
			}

			final long amount = cursor.getLong(DataProvider.Logs.INDEX_AMOUNT);
			s = Plans.formatAmount(t, amount, showHours);
			if (s == null || s.trim().length() == 0 || s.equals("1")) {
				view.findViewById(R.id.length).setVisibility(View.GONE);
				view.findViewById(R.id.length_).setVisibility(View.GONE);
			} else {
				final TextView tw = (TextView) view.findViewById(R.id.length);
				tw.setVisibility(View.VISIBLE);
				view.findViewById(R.id.length_).setVisibility(View.VISIBLE);
				tw.setText(s);
			}
			final float ba = cursor
					.getFloat(DataProvider.Logs.INDEX_BILL_AMOUNT);
			TextView tw = (TextView) view.findViewById(R.id.blength);
			if (amount != ba) {
				tw.setText(Plans.formatAmount(t, ba, showHours));
				tw.setVisibility(View.VISIBLE);
				view.findViewById(R.id.blength_).setVisibility(View.VISIBLE);
			} else {
				tw.setVisibility(View.GONE);
				view.findViewById(R.id.blength_).setVisibility(View.GONE);
			}
			final float c = cursor.getFloat(DataProvider.Logs.INDEX_COST);
			tw = (TextView) view.findViewById(R.id.cost);
			if (c > 0f) {
				tw.setText(String.format(this.cformat, c));
				tw.setVisibility(View.VISIBLE);
				view.findViewById(R.id.cost_).setVisibility(View.VISIBLE);
			} else {
				tw.setVisibility(View.GONE);
				view.findViewById(R.id.cost_).setVisibility(View.GONE);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTheme(Preferences.getTheme(this));
		Utils.setLocale(this);
		this.setContentView(R.layout.logs);
		this.setTitle(R.string.logs);
		this.tbCall = (ToggleButton) this.findViewById(R.id.calls);
		this.tbCall.setOnClickListener(this);
		this.tbSMS = (ToggleButton) this.findViewById(R.id.sms);
		this.tbSMS.setOnClickListener(this);
		this.tbMMS = (ToggleButton) this.findViewById(R.id.mms);
		this.tbMMS.setOnClickListener(this);
		this.tbData = (ToggleButton) this.findViewById(R.id.data);
		this.tbData.setOnClickListener(this);
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		this.tbCall.setChecked(p.getBoolean(PREF_CALL, true));
		this.tbSMS.setChecked(p.getBoolean(PREF_SMS, true));
		this.tbMMS.setChecked(p.getBoolean(PREF_MMS, true));
		this.tbData.setChecked(p.getBoolean(PREF_DATA, true));

		this.setAdapter();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Utils.setLocale(this);
		showHours = PreferenceManager.getDefaultSharedPreferences(this)
				.getBoolean(Preferences.PREFS_SHOWHOURS, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStop() {
		super.onStop();
		final Editor e = PreferenceManager.getDefaultSharedPreferences(this)
				.edit();
		e.putBoolean(PREF_CALL, this.tbCall.isChecked());
		e.putBoolean(PREF_SMS, this.tbSMS.isChecked());
		e.putBoolean(PREF_MMS, this.tbMMS.isChecked());
		e.putBoolean(PREF_DATA, this.tbData.isChecked());
		e.commit();
	}

	/** Set Adapter. */
	private void setAdapter() {
		String where = DataProvider.Logs.TYPE + " < 0 ";
		if (this.tbCall.isChecked()) {
			where += " OR " + DataProvider.Logs.TYPE + " = "
					+ DataProvider.TYPE_CALL;
		}
		if (this.tbSMS.isChecked()) {
			where += " OR " + DataProvider.Logs.TYPE + " = "
					+ DataProvider.TYPE_SMS;
		}
		if (this.tbMMS.isChecked()) {
			where += " OR " + DataProvider.Logs.TYPE + " = "
					+ DataProvider.TYPE_MMS;
		}
		if (this.tbData.isChecked()) {
			where += " OR " + DataProvider.Logs.TYPE + " = "
					+ DataProvider.TYPE_DATA;
		}

		this.setListAdapter(new LogAdapter(this, where));
		this.getListView().setOnItemLongClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(final View v) {
		this.setAdapter();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.menu_logs, menu);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_add:
			AlertDialog.Builder b = new AlertDialog.Builder(this);
			b.setCancelable(true);
			b.setTitle(R.string.add_log);
			final View v = LayoutInflater.from(this).inflate(R.layout.logs_add,
					null);
			final Spinner spType = (Spinner) v.findViewById(R.id.type);
			final Spinner spDirection = (Spinner) v
					.findViewById(R.id.direction);
			final EditText etLength = (EditText) v.findViewById(R.id.length);
			final EditText etRemote = (EditText) v.findViewById(R.id.remote);
			final DatePicker dpDate = (DatePicker) v.findViewById(R.id.date);
			final TimePicker tpTime = (TimePicker) v.findViewById(R.id.time);
			final CheckBox cbRoamed = (CheckBox) v.findViewById(R.id.roamed);
			spType.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(final AdapterView<?> parent,
						final View view, final int position, final long id) {
					switch (position) {
					case DataProvider.Rules.WHAT_CALL:
						etLength.setHint(R.string.length_hint_call);
						etLength.setVisibility(View.VISIBLE);
						etRemote.setVisibility(View.VISIBLE);
						break;
					case DataProvider.Rules.WHAT_DATA:
						etLength.setHint(R.string.length_hint_data);
						etLength.setVisibility(View.VISIBLE);
						etRemote.setVisibility(View.GONE);
						break;
					case DataProvider.Rules.WHAT_MMS:
					case DataProvider.Rules.WHAT_SMS:
						etLength.setVisibility(View.GONE);
						etRemote.setVisibility(View.VISIBLE);
						break;
					default:
						break;
					}
				}

				@Override
				public void onNothingSelected(final AdapterView<?> parent) {
					// nothing to do
				}
			});

			b.setView(v);
			b.setNegativeButton(android.R.string.cancel, null);
			b.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							final int t = spType.getSelectedItemPosition();
							final int d = spDirection.getSelectedItemPosition();
							int l = Utils.parseInt(etLength.getText()
									.toString(), 0);
							final String r = etRemote.getText().toString();
							final boolean roamed = cbRoamed.isChecked();
							final Calendar c = Calendar.getInstance();
							c.set(dpDate.getYear(), dpDate.getMonth(), dpDate
									.getDayOfMonth(), // .
									tpTime.getCurrentHour(), tpTime
											.getCurrentMinute());
							final ContentValues cv = new ContentValues();
							switch (t) {
							case DataProvider.Rules.WHAT_CALL:
								cv.put(DataProvider.Logs.TYPE,
										DataProvider.TYPE_CALL);
								break;
							case DataProvider.Rules.WHAT_DATA:
								cv.put(DataProvider.Logs.TYPE,
										DataProvider.TYPE_DATA);
								break;
							case DataProvider.Rules.WHAT_MMS:
								cv.put(DataProvider.Logs.TYPE,
										DataProvider.TYPE_MMS);
								l = 1;
								break;
							case DataProvider.Rules.WHAT_SMS:
								cv.put(DataProvider.Logs.TYPE,
										DataProvider.TYPE_SMS);
								l = 1;
								break;
							default:
								return;
							}
							cv.put(DataProvider.Logs.DIRECTION, d);
							cv.put(DataProvider.Logs.AMOUNT, l);
							cv.put(DataProvider.Logs.PLAN_ID,
									DataProvider.NO_ID);
							cv.put(DataProvider.Logs.RULE_ID,
									DataProvider.NO_ID);
							cv.put(DataProvider.Logs.REMOTE, r);
							cv.put(DataProvider.Logs.DATE, c.getTimeInMillis());
							if (roamed) {
								cv.put(DataProvider.Logs.ROAMED, 1);
							}
							Logs.this.getContentResolver().insert(
									DataProvider.Logs.CONTENT_URI, cv);
							LogRunnerService.update(Logs.this, null);
						}
					});
			b.show();
			return true;
		default:
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onItemLongClick(final AdapterView<?> parent,
			final View view, final int position, final long id) {
		final Builder b = new Builder(this);
		b.setCancelable(true);
		b.setItems(R.array.dialog_delete,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						Logs.this.getContentResolver().delete(
								ContentUris.withAppendedId(
										DataProvider.Logs.CONTENT_URI, id),
								null, null);
					}
				});
		b.setNegativeButton(android.R.string.cancel, null);
		b.show();
		return true;
	}
}
