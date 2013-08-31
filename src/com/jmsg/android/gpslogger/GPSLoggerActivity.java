package com.jmsg.android.gpslogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jmsg.android.gpslogger.service.GPSLoggerService;

public class GPSLoggerActivity extends Activity {

	private static final String tag = "GPSLoggerActivity";
	
	private String currentTripName = "";
	private Thread th;

	/**
	 * A continuación definimos las variables que cargaremos de
	 * "preferences" para configurar la aplicación.
	 */
	/****************************************************************************/
	private boolean inputPassActive;
	private String inputPass;

	private int altitudeCorrectionMeters; 	// Según Paco Marí (topografo)
											// en Ibiza este valor es la
											// correción
	private int minTimeMillis;
	private int minDistanceMeters;
	private int minAccuracy;
	private boolean showingDebugToast;
	
	private String server;
	private String gprmc;
	private String accountName;
	private String userId;
	/****************************************************************************/
	
	private final DecimalFormat sevenSigDigits = new DecimalFormat("0.#######");

	static final int STATE_READY = 1;
	static final int STATE_START = 2;
	static final int STATE_STOP = 3;
	
	/**
	 * Variables de enlace con el Interface de usuario
	 */
	private TextView mStatus;
	private TextView mDateTime;
	private TextView mLatitude;
	private TextView mLongitude;
	private TextView mAltitude;
	private TextView mSpeed;
	private TextView mDirection;
	private TextView mSatellites;
	private TextView mAccuracy;
	private TextView mTripName;
	private TextView mPassActive;
	private TextView mFrecuency;
	private TextView mDistance;
	private TextView mMinAccuracy;
	private TextView mAltitudeCorrectionMeters;
	private TextView mShowDebug;
	private TextView mServer;
	private TextView mAccountName;
	private TextView mUserID;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_main);
		
		/**
		 * 0. Inicializar las variables de enlace con el interface
		 */
		mStatus = (TextView) findViewById(R.id.textStatus);
		mDateTime = (TextView) findViewById(R.id.txtDateTime);
		mLatitude = (TextView) findViewById(R.id.txtLatitude);
		mLongitude = (TextView) findViewById(R.id.txtLongitude);
		mAltitude = (TextView) findViewById(R.id.txtAltitude);
		mSpeed = (TextView) findViewById(R.id.txtSpeed);
		mDirection = (TextView) findViewById(R.id.txtDirection);
		mSatellites = (TextView) findViewById(R.id.txtSatellites);
		mAccuracy = (TextView) findViewById(R.id.txtAccuracy);
		mTripName = (TextView) findViewById(R.id.txtTripName);
		mPassActive = (TextView) findViewById(R.id.txtPassActive);
		mFrecuency = (TextView) findViewById(R.id.txtFrequency);
		mDistance = (TextView) findViewById(R.id.txtDistance);
		mMinAccuracy = (TextView) findViewById(R.id.txtMinAccuracy);
		mAltitudeCorrectionMeters = (TextView) findViewById(R.id.txtAltitudeCorrectionMeters);
		mShowDebug = (TextView) findViewById(R.id.txtShowDebug);
		mServer = (TextView) findViewById(R.id.txtServer);
		mAccountName = (TextView) findViewById(R.id.txtAccountName);
		mUserID = (TextView) findViewById(R.id.txtUserID);
		
		/**
		 * Final paso 0
		 */
		
		
		/**
		 * 1. Set Action for each Button
		 */
		findViewById(R.id.buttonRegistro).setOnClickListener(mBotonRegistroListener);
//		findViewById(R.id.ButtonStop).setOnClickListener(mStopListener);
//		findViewById(R.id.ButtonUpdate).setOnClickListener(mUpdateListener);

		// Establecer el estado de los botones, según si está corriendo el servicio
		if (GPSLoggerService.isRunningStatus()) {
			setButtonState(STATE_START);

		} else {
			setButtonState(STATE_STOP);
		}

		// Cargar los datos desde preferencias
		cargarPreferencias();
				
		// Auto set TripName to TextView
		initTripName();

		/**
		 * End Step 1
		 */
		

		/**
		 * JMSG 15/08/2013, 13:51:04 Quiero que se inicie el tracking desde el
		 * principio por lo que lo fuerzo con el siguiente código
		 */
		iniciarServicio();
		
//		setButtonState(STATE_START);
//		if (!GPSLoggerService.isRunningStatus()) {
//			setButtonState(STATE_START);
//			startService(new Intent(GPSLoggerActivity.this,
//					GPSLoggerService.class));
//		}
		
		/** JMSG 24/08/2013, 13:26:12
		 * En otro Thread actualizamos el UI cada 1000 ms
		 */
		th = new Thread() {
			@Override
			public void run() {
				while(true){
//					 && !Thread.currentThread().isInterrupted()
					try {
						
						runOnUiThread(new Runnable() {

							public void run() {
								try {
									if (GPSLoggerService.isRunningStatus()) {
										mStatus.setText(GPSLoggerService.currentStatus);
										mDateTime.setText(GPSLoggerService.currentDateTime);
										mLatitude.setText(GPSLoggerService.currentLatitude);
										mLongitude.setText(GPSLoggerService.currentLongitude);
										mSpeed.setText(GPSLoggerService.currentSpeed);
										mAltitude.setText(GPSLoggerService.currentAltitude);
										mAccuracy.setText(GPSLoggerService.currentAccuracy);
										mSatellites.setText(GPSLoggerService.currentSatellites);
										mFrecuency.setText(String.valueOf(GPSLoggerService.getMinTimeMillis()));
										mPassActive.setText(inputPassActive ? "SI" : "NO");
										mDistance.setText(String.valueOf(minDistanceMeters));
										mMinAccuracy.setText(String.valueOf(GPSLoggerService.getMinAccuracyMeters()));
//										mMinAccuracy.setText(String.valueOf(minAccuracy));
										mAltitudeCorrectionMeters.setText(String.valueOf(altitudeCorrectionMeters));
										mShowDebug.setText(showingDebugToast ? "SI" : "NO");
										mServer.setText(server);
										mAccountName.setText(accountName);
										mUserID.setText(userId);
									}
								} catch (Exception e) {
									// TODO: handle exception
									e.printStackTrace();
								}
							}
						});
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}		
		};
		th.start();
		
	}
	@Override
	protected void onResume() {
		super.onResume();
		
		// Cargar los datos desde preferencias
		cargarPreferencias();
	}

	/**
	 * generate tripName and set to TextView
	 */
	private void initTripName() {
		String tripName = "";
		try {
			// Date Format 20101107_093659
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'_'HHmmss");
			tripName = userId + "_" + sdf.format(new Date());
		} catch (Exception e) {
			Log.e(tag, e.toString());
		}

		// Set tripName to TextView
		mTripName.setText(tripName);
		// Asigno a variable global
		currentTripName = tripName;
	}

	/**
	 * Set action while click on Start Button
	 */
	private OnClickListener mBotonRegistroListener = new OnClickListener() {
		public void onClick(View v) {
			
			if (GPSLoggerService.isRunningStatus()) {
				/**
				 * Antes de parar el servicio pedimos el password de administrador
				 * 
				 */				
				// Si el uso de password está activo, lo pedimos para seguir.
				if (inputPassActive) {
					
					// El control del password está activo					
					PromptDialog dlg = new PromptDialog(
							GPSLoggerActivity.this ,
							"Atención",
							"Introduzca el password",
							true) { 
						 @Override
						 public boolean onOkClicked(String input) {
//							 Toast.makeText(getBaseContext(),
//										"inputPass: " + inputPass + "-" + input, Toast.LENGTH_SHORT)
//										.show();
							 
							 // Comparar el valor introducido con el password 
							 if (inputPass.equals(input)) {
								 // Coinciden los passwords
								 pararServicio();
							 }
							 else {
								Toast.makeText(getBaseContext(),
										"ERROR: Password incorrecto.",
										Toast.LENGTH_LONG)
										.show();
							}
							 return true; // true = close dialog
						 }
					};
					dlg.show();
				} else {
					// No está activada la opción de solicitar password, asi que
					// paramos el servicio sin preguntar
					pararServicio();
				}
							
			} else {
				// El servico está parado, entonces iniciarlo y actualizar estado del botón
				iniciarServicio();
			}			
		}
	};

	private void pararServicio(){
		// El servicio esta corriento, entonces detenerlo y actualizar estado del botón
		setButtonState(STATE_STOP);
		doExport();
		doNewTrip();

		stopService(new Intent(GPSLoggerActivity.this,
				GPSLoggerService.class));
		
	}
	
	private void iniciarServicio(){
		// El servico está parado, entonces iniciarlo y actualizar estado del botón
		setButtonState(STATE_START);
		startService(new Intent(GPSLoggerActivity.this,
				GPSLoggerService.class));
	}

	/**
	 * Set Visibility of button m/s *
	 */
	public void setButtonState(int state) {
		Button btnOnOff = (Button) findViewById(R.id.buttonRegistro);

		switch (state) {
		case STATE_STOP:
			btnOnOff.setText("Iniciar Registro");
			break;
		case STATE_START:
			btnOnOff.setText("Detener Registro");
//			stop_button.setVisibility(View.VISIBLE);
//			update_button.setVisibility(View.VISIBLE);
			break;

		default:
			break;
		}
	}

	/**
	 * Set action while click on Stop Button
	 */
	private OnClickListener mStopListener = new OnClickListener() {
		public void onClick(View v) {
			setButtonState(STATE_STOP);
			doExport();
			doNewTrip();

			stopService(new Intent(GPSLoggerActivity.this,
					GPSLoggerService.class));
		}
	};

	/**
	 * Clear Database data and set new tripName to
	 */
	private void doNewTrip() {
		SQLiteDatabase db = null;
		try {
			db = openOrCreateDatabase(GPSLoggerService.DATABASE_NAME,
					SQLiteDatabase.OPEN_READWRITE, null);
			db.execSQL("DELETE FROM " + GPSLoggerService.POINTS_TABLE_NAME);
		} catch (Exception e) {
			Log.e(tag, e.toString());
		} finally {
			initTripName();
			close_db(db);
		}
	}

	/**
	 * Export database contents to a TXT file
	 */
	private void doExport() {
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			// Hard code to set altitudeCorectionMeters
			this.setAltitudeCorrectionMeters(altitudeCorrectionMeters);

			db = openOrCreateDatabase(GPSLoggerService.DATABASE_NAME,
					SQLiteDatabase.OPEN_READWRITE, null);
			cursor = db.rawQuery("SELECT * " + " FROM "
					+ GPSLoggerService.POINTS_TABLE_NAME
					+ " ORDER BY GMTTIMESTAMP ASC", null);
			int gmtTimestampColumnIndex = cursor
					.getColumnIndexOrThrow("GMTTIMESTAMP");
			int latitudeColumnIndex = cursor.getColumnIndexOrThrow("LATITUDE");
			int longitudeColumnIndex = cursor
					.getColumnIndexOrThrow("LONGITUDE");
			int speedColumnIndex = cursor.getColumnIndexOrThrow("SPEED");
			int altitudeColumnIndex = cursor.getColumnIndexOrThrow("ALTITUDE");
			int accuracyColumnIndex = cursor.getColumnIndexOrThrow("ACCURACY");

			if (cursor.moveToFirst()) {
				// fileBuf (Header, coordinates, Footer);
				StringBuffer fileBuf = new StringBuffer();
				String beginTimestamp = null;
				String endTimestamp = null;
				String gmtTimestamp = null;
				// initFileBuf Setting KML
				// (fileBuf) initValuesMap
				initTXTFileBuf(fileBuf, initValuesMap());

				// Write coordinates to file
				do {
					gmtTimestamp = cursor.getString(gmtTimestampColumnIndex);
					if (beginTimestamp == null) {
						beginTimestamp = gmtTimestamp;
					}
					/**
					 * 2. getData from database (cursor);
					 */
					double latitude = cursor.getDouble(latitudeColumnIndex);
					double longitude = cursor.getDouble(longitudeColumnIndex);
					double altitude = cursor.getDouble(altitudeColumnIndex)
							+ this.getAltitudeCorrectionMeters();
					double accuracy = cursor.getDouble(accuracyColumnIndex);
					double speed = cursor.getDouble(speedColumnIndex);

					/**
					 * End step 2.
					 */

					/**
					 * 3. Write data (query from database) to file
					 */

					/**
					 * JMSG 15/08/2013, 14:17:24 Hay que tener en cuenta la
					 * localización, al estar en SPAIN nos pone como separador
					 * de decimales la coma , a nosotros nos interesa el punto,
					 * US porque si no el número sale 1,1234567, en lugar de
					 * 1.1234567
					 */
					Locale locale = Locale.US;
					// fileBuf.append(sevenSigDigits.getNumberInstance(locale).format(longitude)
					// + ","
					// +
					// sevenSigDigits.getNumberInstance(locale).format(latitude)
					// + ","
					// +
					// sevenSigDigits.getNumberInstance(locale).format(altitude)
					// + ","
					// + sevenSigDigits.getNumberInstance(locale).format(speed)
					// + ","
					// +
					// sevenSigDigits.getNumberInstance(locale).format(accuracy)
					// + "\n");

					/**
					 * JMSG 16/08/2013, 21:37:43 PENDIENTE, sumar dos horas para
					 * corregir la hora local
					 */
					fileBuf.append("<coor>" + zuluFormat(gmtTimestamp) + ", "
							+ longitude + ", " + latitude + ", " + altitude
							+ ", " + speed + ", " + accuracy + "</coor>\n");

					/**
					 * End Step 3.
					 */
				} while (cursor.moveToNext());

				endTimestamp = gmtTimestamp;
				// closeFileBuf Setting KML
				// (fileBuf)
				closeTXTFileBuf(fileBuf, beginTimestamp, endTimestamp);

				// File Buffer String
				String fileContents = fileBuf.toString();
				Log.d(tag, fileContents);

				/**
				 * Step 4. Write file to /sdcard
				 */
				
				String dirPath = Environment.getExternalStorageDirectory().toString()
						+ "/GPSLogger" ;
				File sdDir = new File(dirPath);
				Log.d(tag,"new Dir: "+dirPath);
				sdDir.mkdirs();
				Log.d(tag,"mkdirs");
				File file = new File(dirPath+"/" + currentTripName
						+ ".txt");
				Log.d(tag,"new File currentTripNmame:"+dirPath+"/" + currentTripName
						+ ".txt");
				FileWriter sdWriter = new FileWriter(file, false);
				Log.d(tag,"new FileWriter");
				sdWriter.write(fileContents);
				Log.d(tag, "write fileContents");
				sdWriter.close();
				Log.d(tag,"close file");

				/**
				 * End Step 4.
				 */
				// R.string.export_completed Predefined in string.xml
				Toast.makeText(getBaseContext(), R.string.export_completed,
						Toast.LENGTH_LONG).show();
				// cursor.moveToFirst()
				// database
			} else {
				Toast.makeText(
						getBaseContext(),
						"I didn't find any location points in the database, so no KML file was exported.",
						Toast.LENGTH_LONG).show();
			}
		} catch (FileNotFoundException fnfe) {
			Toast.makeText(
					getBaseContext(),
					"Error trying access the SD card.  Make sure your handset is not connected to a computer and the SD card is properly installed",
					Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Toast.makeText(getBaseContext(),
					"Error trying to export: " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
			close_db(db);
		}
	}

	/**
	 * Initial kml setting data
	 * 
	 * @return HashMap<String, String>
	 */
	private HashMap<String, String> initValuesMap() {
		HashMap<String, String> valuesMap = new HashMap<String, String>();

		valuesMap.put("FILENAME", currentTripName);
		// use ground settings for the export
		valuesMap.put("EXTRUDE", "0");
		valuesMap.put("TESSELLATE", "1");
		valuesMap.put("ALTITUDEMODE", "clampToGround");

		return valuesMap;
	}

	private void initTXTFileBuf(StringBuffer fileBuf,
			HashMap<String, String> valuesMap) {
		fileBuf.append("<filename>" + valuesMap.get("FILENAME")
				+ "</filename>\n");
		fileBuf.append("<coordenadas>\n");
	}

	private void closeTXTFileBuf(StringBuffer fileBuf, String beginTimestamp,
			String endTimestamp) {
		fileBuf.append("</coordenadas>\n");
		String formattedBeginTimestamp = zuluFormat(beginTimestamp);
		fileBuf.append("<beginTime>" + formattedBeginTimestamp
				+ "</beginTime>\n");
		String formattedEndTimestamp = zuluFormat(endTimestamp);
		fileBuf.append("<endTime>" + formattedEndTimestamp + "</endTime>\n");
	}

	private void initKMLFileBuf(StringBuffer fileBuf,
			HashMap<String, String> valuesMap) {
		fileBuf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		fileBuf.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
		fileBuf.append("  <Document>\n");
		fileBuf.append("    <name>" + valuesMap.get("FILENAME") + "</name>\n");
		fileBuf.append("    <description>GPSLogger KML export</description>\n");
		fileBuf.append("    <Style id=\"yellowLineGreenPoly\">\n");
		fileBuf.append("      <LineStyle>\n");
		fileBuf.append("        <color>7f00ffff</color>\n");
		fileBuf.append("        <width>4</width>\n");
		fileBuf.append("      </LineStyle>\n");
		fileBuf.append("      <PolyStyle>\n");
		fileBuf.append("        <color>7f00ff00</color>\n");
		fileBuf.append("      </PolyStyle>\n");
		fileBuf.append("    </Style>\n");
		fileBuf.append("    <Placemark>\n");
		fileBuf.append("      <name>Absolute Extruded</name>\n");
		fileBuf.append("      <description>Transparent green wall with yellow points</description>\n");
		fileBuf.append("      <styleUrl>#yellowLineGreenPoly</styleUrl>\n");
		fileBuf.append("      <LineString>\n");
		fileBuf.append("        <extrude>" + valuesMap.get("EXTRUDE")
				+ "</extrude>\n");
		fileBuf.append("        <tessellate>" + valuesMap.get("TESSELLATE")
				+ "</tessellate>\n");
		fileBuf.append("        <altitudeMode>" + valuesMap.get("ALTITUDEMODE")
				+ "</altitudeMode>\n");
		fileBuf.append("        <coordinates>\n");
	}

	private void closeKMLFileBuf(StringBuffer fileBuf, String beginTimestamp,
			String endTimestamp) {
		fileBuf.append("        </coordinates>\n");
		fileBuf.append("     </LineString>\n");
		fileBuf.append("	 <TimeSpan>\n");
		String formattedBeginTimestamp = zuluFormat(beginTimestamp);
		fileBuf.append("		<begin>" + formattedBeginTimestamp + "</begin>\n");
		String formattedEndTimestamp = zuluFormat(endTimestamp);
		fileBuf.append("		<end>" + formattedEndTimestamp + "</end>\n");
		fileBuf.append("	 </TimeSpan>\n");
		fileBuf.append("    </Placemark>\n");
		fileBuf.append("  </Document>\n");
		fileBuf.append("</kml>");
	}

	/**
	 * Format timestamp for human
	 */
	private String zuluFormat(String beginTimestamp) {
		// turn 20081215135500 into 2008-12-15 13:55:00
		StringBuffer buf = new StringBuffer(beginTimestamp);
		buf.insert(4, '-');
		buf.insert(7, '-');
		buf.insert(10, ' ');
		buf.insert(13, ':');
		buf.insert(16, ':');
		// buf.append('Z');
		return buf.toString();
	}

	public void setAltitudeCorrectionMeters(int altitudeCorrectionMeters) {
		this.altitudeCorrectionMeters = altitudeCorrectionMeters;
	}

	public int getAltitudeCorrectionMeters() {
		return altitudeCorrectionMeters;
	}

	public void close_db(SQLiteDatabase db) {
		if (db != null && db.isOpen()) {
			db.close();
		}
	}

	/**
	 * Menú de la app
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_preferencias, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Toast.makeText(GPSLoggerActivity.this, "ID:" + item.getItemId()
		// + " R: " + R.id.menu_preferencias,
		// Toast.LENGTH_LONG).show();

		switch (item.getItemId()) {
		case R.id.menu_preferencias:
			
			/**
			 * Antes de lanzar la actividad de preferencias hay que comprobar si
			 * está activo el uso de password: en caso negativo muestro las
			 * preferencias. en caso positivo pido el password y si coincide muestro
			 * las preferencias.
			 */

			// Obtener las preferencias
			SharedPreferences sharedPrefs = PreferenceManager
	                .getDefaultSharedPreferences(this);
			
			// Si no está activo el password, lanzamos las preferencias
			if (!sharedPrefs.getBoolean("prefAdminPassActive", false)) {
				lanzarPreferencias(null);
			} else {
				// El control del password está activo
				// Guardamos el password introducido
				inputPass = sharedPrefs.getString("prefAdminPassword", "1234");
//				if (inputPass.isEmpty()) {
//					// Si no hay pass en preferencias
//					// Lo ponemos al valor por defecto: 1234
//					inputPass = "1234";
//					
//				}
				
				PromptDialog dlg = new PromptDialog(
						GPSLoggerActivity.this ,
						"Atención",
						"Introduzca el password",
						true) { 
					 @Override
					 public boolean onOkClicked(String input) {
//						 Toast.makeText(getBaseContext(),
//									"inputPass: " + inputPass + "-" + input, Toast.LENGTH_SHORT)
//									.show();
						 
						 // Comparar el valor introducido con el password 
						 if (inputPass.equals(input)) {
							 // Coinciden los passwords
							 lanzarPreferencias(null);
						 }
						 else {
							Toast.makeText(getBaseContext(),
									"ERROR: Password incorrecto.",
									Toast.LENGTH_LONG)
									.show();
						}
						 return true; // true = close dialog
					 }
				};
				dlg.show();
			}
			break;
		}

		return true;
		/** true -> consumimos el item, no se propaga */
	}

	public void lanzarPreferencias(View view) {
		
		Intent i = new Intent(GPSLoggerActivity.this, Preferencias.class);
		startActivity(i);
	}
	
	public void cargarPreferencias() {
		SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
		
		// Cargamos las preferencias en sus respectivass variables
		inputPassActive = sharedPrefs.getBoolean("prefAdminPassActive", false);
		inputPass = sharedPrefs.getString("prefAdminPassword", "1234");
		altitudeCorrectionMeters = Integer.parseInt(sharedPrefs.getString("prefAppAltitudeCorrectionMeters", "-50"));
		minTimeMillis = Integer.parseInt(sharedPrefs.getString("prefAppMinTimeMillis", "20000"));
		minDistanceMeters = Integer.parseInt(sharedPrefs.getString("prefAppMinDistanceMeters", "10"));
		minAccuracy = Integer.parseInt(sharedPrefs.getString("prefAppMinAccuracy", "200"));
		showingDebugToast = sharedPrefs.getBoolean("prefAppShowingDebugToast", false);
		server = sharedPrefs.getString("prefOpenGtsServer", "213.96.91.211:8080");
		gprmc = sharedPrefs.getString("prefOpenGtsGprmc", "/gprmc/Data?");
		accountName = sharedPrefs.getString("prefOpenGtsAccountName", "piscinaspepe");
		userId = sharedPrefs.getString("prefOpenGtsUserId", "t0000");
		
		// Pasamos los datos al GPSLoggerService
		GPSLoggerService.setMinTimeMillis(minTimeMillis);
		GPSLoggerService.setMinAccuracyMeters(minAccuracy);
		GPSLoggerService.setShowingDebugToast(showingDebugToast);
		GPSLoggerService.setAccountName(accountName);
		GPSLoggerService.setDeviceName(userId);
		GPSLoggerService.setServerName(server);
		GPSLoggerService.setGprmcName(gprmc);

	}
}