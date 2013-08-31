package com.jmsg.android.gpslogger.service;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.jmsg.android.gpslogger.R;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class GPSLoggerService extends Service {

	public static final String DATABASE_NAME = "GPSLOGGERDB";
	public static final String POINTS_TABLE_NAME = "LOCATION_POINTS";
	public static final String TRIPS_TABLE_NAME = "TRIPS";

	private final DecimalFormat sevenSigDigits = new DecimalFormat("0.#######");
	private final DateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");

	/**
	 * Step 4.5 Declare LocationManager, LocationListener, SQLiteDatebase
	 */
	private LocationManager lm;
	private LocationListener locationListener;
	private SQLiteDatabase db;
	/**
	 * End Step 4.5
	 */
	
	public static String currentStatus="GPS, intentando obtener localización...";
	public static String currentDateTime;
	public static String currentLatitude;
	public static String currentLongitude;
	public static String currentSpeed;
	public static String currentAltitude;
	public static String currentAccuracy;
	public static String currentSatellites;

	// Valores por defecto
	private static long minTimeMillis = 2000;
	private static long minDistanceMeters = 10;
	private static float minAccuracyMeters = 200;

	private int lastGpsStatus = -99999;
	private static boolean showingDebugToast = false;

	private static final String tag = "GPSLoggerService";
	private static boolean running = false;
	
	private static String accountName="";
	private static String deviceName="";
	private static String serverName="";
	private static String gprmcName="";
	

	/** Called when the activity is first created. */
	private void startLoggerService() {
		setRunningStatus(true);
		// ---use the LocationManager class to obtain GPS locations---
		/**
		 * Step 5. Setup LocationManager and Listener
		 */
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationListener = new MyLocationListener();
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMillis,
				minDistanceMeters, locationListener);
		/**
		 * End Step 5.
		 */
		initDatabase();
	}

	/**
	 * Open database if does not exist CREATE!
	 */
	private void initDatabase() {
		/**
		 * Step 5.5 Create Database
		 */
		db = this.openOrCreateDatabase(DATABASE_NAME,
				SQLiteDatabase.OPEN_READWRITE, null);
		db.execSQL("CREATE TABLE IF NOT EXISTS " + POINTS_TABLE_NAME
				+ " (GMTTIMESTAMP VARCHAR, LATITUDE REAL, LONGITUDE REAL,"
				+ "ALTITUDE REAL, ACCURACY REAL, SPEED REAL, BEARING REAL);");
		db.close();
		Log.i(tag, "Database opened ok");
		/**
		 * End Step 5.5
		 */
	}

	/**
	 * Shutdown GPSLogger Service
	 */
	private void shutdownLoggerService() {
		/**
		 * Step 8. removeUpdates
		 */
		GPSLoggerService.setRunningStatus(false);
		lm.removeUpdates(locationListener);

		/**
		 * End Step 8.
		 */
	}

	/**
	 * Implement LocationListener
	 * 
	 * @author NAzT
	 * 
	 */
	public class MyLocationListener implements LocationListener {
		public void onLocationChanged(Location loc) {
			
			// Para poder obtener la hora
			GregorianCalendar greg = new GregorianCalendar(TimeZone.getTimeZone("Europe/Madrid"));
			String fechaHora = timestampFormat.format(greg.getTime());

			if (loc != null) {
				boolean pointIsRecorded = false;
				
				try {					
					if (loc.hasAccuracy()
							&& loc.getAccuracy() <= minAccuracyMeters) {
						pointIsRecorded = true;

						// database
						
						/** JMSG 31/08/2013, 10:30:44
						 * Step 6. Enviar las coordenadas a nuestra base de
						 * datos remota
						 */
						String url = "http://" +  
								getServerName() + 
								getGprmcName() +
								"acct=" + getAccountName() + "&" +
								"dev=" + getDeviceName() + "&" +
								"gprmc=" + GPRMCEncode(loc);

						Log.i(tag,"URL: "+url);

						
						AsyncHttpClient client = new AsyncHttpClient();						
						client.get(url, new AsyncHttpResponseHandler(){
							@Override
							public void onStart() {
								Log.i(tag, "onStart");
							}
							@Override
							public void onSuccess(String response) {
								Log.i(tag,"SUCCESS "+response);
							}
							@Override
						    public void onFailure(Throwable e, String response) {
								Log.i(tag,"FAILURE "+response);
							}
							
							@Override
							public void onFinish() {
								Log.i(tag,"onFinish");
							}
						});
						
//						 RequestParams params = new RequestParams();
//						 params.put("acct", getAccountName());
//						 params.put("dev", getDeviceName());
//						 params.put("gprmc", URLEncoder.encode(GPRMCEncode(loc),"UTF-8"));
//						 Log.i(tag,"Params: "+params.toString());
//						 
//						AsyncHttpClient client = new AsyncHttpClient();
//						client.post(serverURL, params, new AsyncHttpResponseHandler() {
//						    @Override
//						    public void onSuccess(String response) {
//						        Log.i(tag,"SUCCESS: Http response: "+response);
//						        Toast.makeText(
//										getBaseContext(),
//										"SUCCESS: Http response: "+response,
//										Toast.LENGTH_SHORT).show();
//						    }
//						    @Override
//						     public void onFailure(Throwable e, String response) {
//						         // Response failed :(
//						    	Log.i(tag,"FAILURE: Http response: "+response);
////						    	e.printStackTrace();
//						    	Toast.makeText(
//										getBaseContext(),
//										"FAILURE: Http response: "+response,
//										Toast.LENGTH_SHORT).show();
//						     }
//
////						     @Override
////						     public void onFinish() {
////						         // Completed the request (either success or failure)
////						    	 Log.i(tag,"Http: FINISH");
////						    	 Toast.makeText(
////											getBaseContext(),
////											"Http: FINISH",
////											Toast.LENGTH_SHORT).show();
////						     }
//						});
						/**
						 * 
						 * Step 6.1 Insert location data to database
						 */
						StringBuffer queryBuf = new StringBuffer();
						queryBuf.append("INSERT INTO "
								+ POINTS_TABLE_NAME
								+ " (GMTTIMESTAMP,LATITUDE,LONGITUDE,ALTITUDE,ACCURACY,SPEED,BEARING) VALUES ("
								+ "'"
								+ fechaHora
								+ "',"
								+ loc.getLatitude()
								+ ","
								+ loc.getLongitude()
								+ ","
								+ (loc.hasAltitude() ? loc.getAltitude()
										: "NULL")
								+ ","
								+ (loc.hasAccuracy() ? loc.getAccuracy()
										: "NULL")
								+ ","
								+ (loc.hasSpeed() ? loc.getSpeed() : "NULL")
								+ ","
								+ (loc.hasBearing() ? loc.getBearing() : "NULL")
								+ ");");
						Log.i(tag, queryBuf.toString());
						db = openOrCreateDatabase(DATABASE_NAME,
								SQLiteDatabase.OPEN_READWRITE, null);
						db.execSQL(queryBuf.toString());

						/**
						 * End Step 6.
						 */
					}
				} catch (Exception e) {
					Log.e(tag, e.toString());
				} finally {
					if (db.isOpen())
						db.close();
				}

				// Toast
				if (pointIsRecorded) {
					if (showingDebugToast)
						Toast.makeText(
								getBaseContext(),
								"Location stored: \nLat: "
										+ sevenSigDigits.format(loc
												.getLatitude())
										+ " \nLon: "
										+ sevenSigDigits.format(loc
												.getLongitude())
										+ " \nAlt: "
										+ (loc.hasAltitude() ? loc
												.getAltitude() + "m" : "?")
										+ "\nVel: "
										+ (loc.hasSpeed() ? loc
												.getSpeed() + "m" : "?")
										+ " \nAcc: "
										+ (loc.hasAccuracy() ? loc
												.getAccuracy() + "m" : "?"),
								Toast.LENGTH_LONG).show();
				} else {
					if (showingDebugToast)
						Toast.makeText(
								getBaseContext(),
								"Location not accurate enough: \nLat: "
										+ sevenSigDigits.format(loc
												.getLatitude())
										+ " \nLon: "
										+ sevenSigDigits.format(loc
												.getLongitude())
										+ " \nAlt: "
										+ (loc.hasAltitude() ? loc
												.getAltitude() + "m" : "?")
										+ "\nVel: "
										+ (loc.hasSpeed() ? loc
												.getSpeed() + "m" : "?")
										+ " \nAcc: "
										+ (loc.hasAccuracy() ? loc
												.getAccuracy() + "m" : "?"),
								Toast.LENGTH_LONG).show();
				}
			}
			
			// Poner la fecha en YYMMDD HH:MM:SS
			StringBuffer buf = new StringBuffer(timestampFormat.format(greg.getTime()));
			buf.insert(4, '-');
			buf.insert(7, '-');
			buf.insert(10, ' ');
			buf.insert(13, ':');
			buf.insert(16, ':');
			// buf.append('Z');
			currentDateTime = buf.toString();
			currentLatitude = String.valueOf(loc.getLatitude());
			currentLongitude = String.valueOf(loc.getLongitude());
			currentAccuracy = (loc.hasAccuracy() ? loc.getAccuracy() + "m" : "?");
			currentAltitude = (loc.hasAltitude()? loc.getAltitude() + "m" : "?");
			currentSpeed = (loc.hasSpeed() ? loc.getSpeed() + "m/s" : "?");
			currentSpeed =  currentSpeed + " " + (loc.hasSpeed() ? loc.getSpeed()*3.6 + "km/s" : "?");
//			if (loc.getExtras()!=null) {
//				currentSatellites = loc.getExtras().get("satellites").toString();
//            } else
//            	currentSatellites = "0";
			
			Bundle extras = loc.getExtras();			
			if (extras!=null){
				currentSatellites = extras.containsKey("satellites") ? extras.get("satellites").toString() : "0";
				Log.d(tag,"sat: "+currentSatellites);
			} else
					Log.d(tag,"extras es null");
			
		}

		public void onProviderDisabled(String provider) {
			if (showingDebugToast)
				Toast.makeText(getBaseContext(),
						"onProviderDisabled: " + provider, Toast.LENGTH_SHORT)
						.show();

		}

		public void onProviderEnabled(String provider) {
			if (showingDebugToast)
				Toast.makeText(getBaseContext(),
						"onProviderEnabled: " + provider, Toast.LENGTH_SHORT)
						.show();

		}

		public void onStatusChanged(String provider, 
					int status, Bundle extras) {
			String showStatus = "GPS estado desconocido";
			/**
			 * Step 7. Set GPS Status message
			 */
			if (status == LocationProvider.AVAILABLE)
				  showStatus = "GPS Disponible";
			if (status == LocationProvider.TEMPORARILY_UNAVAILABLE)
				  showStatus = "GPS Inaccesible temporalmente";
			if (status == LocationProvider.OUT_OF_SERVICE)
				  showStatus = "GPS fuera de servicio";
			if (status != lastGpsStatus && showingDebugToast) {
				  Toast.makeText(getBaseContext(), "Provider: "+provider+" GPS: " + showStatus, Toast.LENGTH_SHORT).show();
			}
			
			if (status != lastGpsStatus)
				currentStatus = showStatus;
			
			/**
			 * End Step 7.
			 */
			lastGpsStatus = status;
			
			if (extras!=null){
				currentSatellites = extras.containsKey("satellites") ? extras.get("satellites").toString() : "0";
				Log.d(tag,"sat: "+currentSatellites);
			} else
					Log.d(tag,"extras es null");
		}
                
	}

	// Below is the service framework methods

	private NotificationManager mNM;

	@Override
	public void onCreate() {
		super.onCreate();
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		startLoggerService();

		// Display a notification about us starting. We put an icon in the
		// status bar.
		showNotification();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		shutdownLoggerService();
		// Cancel the persistent notification.
		mNM.cancel(R.string.local_service_started);

		// Tell the user we stopped.
		Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT)
				.show();
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.local_service_started);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.gpslogger16,
				text, System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, GPSLoggerService.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.service_name),
				text, contentIntent);

		// Send the notification.
		// We use a layout id because it is a unique number. We use it later to
		// cancel.
		mNM.notify(R.string.local_service_started, notification);
	}

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public static void setMinTimeMillis(long _minTimeMillis) {
		minTimeMillis = _minTimeMillis;
	}

	public static long getMinTimeMillis() {
		return minTimeMillis;
	}

	public static void setMinDistanceMeters(long _minDistanceMeters) {
		minDistanceMeters = _minDistanceMeters;
	}

	public static long getMinDistanceMeters() {
		return minDistanceMeters;
	}

	public static float getMinAccuracyMeters() {
		return minAccuracyMeters;
	}

	public static void setMinAccuracyMeters(float minAccuracyMeters) {
		GPSLoggerService.minAccuracyMeters = minAccuracyMeters;
	}

	public static void setShowingDebugToast(boolean showingDebugToast) {
		GPSLoggerService.showingDebugToast = showingDebugToast;
	}
	
	public static String getAccountName() {
		return accountName;
	}

	public static void setAccountName(String accountName) {
		GPSLoggerService.accountName = accountName;
	}

	public static String getDeviceName() {
		return deviceName;
	}

	public static void setDeviceName(String deviceName) {
		GPSLoggerService.deviceName = deviceName;
	}

	public static String getServerName() {
		return serverName;
	}

	public static void setServerName(String serverName) {
		GPSLoggerService.serverName = serverName;
	}

	public static String getGprmcName() {
		return gprmcName;
	}

	public static void setGprmcName(String gprmcName) {
		GPSLoggerService.gprmcName = gprmcName;
	}

	public static boolean isShowingDebugToast() {
		return showingDebugToast;
	}

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		GPSLoggerService getService() {
			return GPSLoggerService.this;
		}
	}

	public static void setRunningStatus(boolean runningStatus) {
		GPSLoggerService.running = runningStatus;
	}

	public static boolean isRunningStatus() {
		return running;
	}
	
	/**
	 * JMSG 31/08/2013, 10:29:45
	 * Funciones para crear la cadena de datos GPRMC
	 */
	/**
     * Encode a location as GPRMC string data.
     * <p/>
     * For details check org.opengts.util.Nmea0183#_parse_GPRMC(String)
     * (OpenGTS source)
     *
     * @param loc location
     * @return GPRMC data
     */
    public static String GPRMCEncode(Location loc)
    {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
        DecimalFormat f = new DecimalFormat("0.000000", dfs);

        String gprmc = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,,",
                "$GPRMC",
                NMEAGPRMCTime(new Date(loc.getTime())),
                "A",
                NMEAGPRMCCoord(Math.abs(loc.getLatitude())),
                (loc.getLatitude() >= 0) ? "N" : "S",
                NMEAGPRMCCoord(Math.abs(loc.getLongitude())),
                (loc.getLongitude() >= 0) ? "E" : "W",
                f.format(MetersPerSecondToKnots(loc.getSpeed())),
                f.format(loc.getBearing()),
                NMEAGPRMCDate(new Date(loc.getTime()))
        );

        gprmc += "*" + NMEACheckSum(gprmc);

        return gprmc;
    }

    public static String NMEAGPRMCTime(Date dateToFormat)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(dateToFormat);
    }

    public static String NMEAGPRMCDate(Date dateToFormat)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(dateToFormat);
    }

    public static String NMEAGPRMCCoord(double coord)
    {
        int degrees = (int) coord;
        double minutes = (coord - degrees) * 60;

        DecimalFormat df = new DecimalFormat("00.00000", new DecimalFormatSymbols(Locale.US));
        StringBuilder rCoord = new StringBuilder();
        rCoord.append(degrees);
        rCoord.append(df.format(minutes));

        return rCoord.toString();
    }


    public static String NMEACheckSum(String msg)
    {
        int chk = 0;
        for (int i = 1; i < msg.length(); i++)
        {
            chk ^= msg.charAt(i);
        }
        String chk_s = Integer.toHexString(chk).toUpperCase();
        while (chk_s.length() < 2)
        {
            chk_s = "0" + chk_s;
        }
        return chk_s;
    }

    /**
     * Converts given meters/second to nautical mile/hour.
     *
     * @param mps meters per second
     * @return knots
     */
    public static double MetersPerSecondToKnots(double mps)
    {
        // Google "meters per second to knots"
        return mps * 1.94384449;
    }

}

