//package eu.smartsantander.androidExperimentation.tabs;
//
//import android.app.ListActivity;
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.hardware.Sensor;
//import android.hardware.SensorEvent;
//import android.hardware.SensorEventListener;
//import android.hardware.SensorManager;
//import android.location.Location;
//import android.media.MediaRecorder;
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.ArrayAdapter;
//import android.widget.CompoundButton;
//import android.widget.Switch;
//
//import org.ambientdynamix.contextplugins.ExperimentPlugin.PluginInfo;
//import org.ambientdynamix.core.DynamixService;
//import org.ambientdynamix.core.HomeActivity;
//import org.ambientdynamix.core.R;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Timer;
//import java.util.TimerTask;
//
//import eu.smartsantander.androidExperimentation.jsonEntities.Reading;
//import eu.smartsantander.androidExperimentation.jsonEntities.Report;
//import eu.smartsantander.androidExperimentation.util.DefaultSensor;
//import eu.smartsantander.androidExperimentation.util.DefaultSensorArrayAdapter;
//
///**
// * Default sensing activity that runs in the background, and gathers data for the city.
// */
//public class DefaultSensingActivity extends ListActivity implements SensorEventListener {
//    private static final String TAG = "DefaultSensing";
//    ArrayAdapter<DefaultSensor> mAdapter;
//    Context context;
//    private SensorManager mSensorManager;
//    private DefaultSensingActivity activity;
//    private long lastTemperature = 0;
//    private long lastHumidity = 0;
//    private long lastPressure;
//    private Timer publishTimer;
//    private Timer noiseTimer;
//    private TimerTask noiseMeasureTask;
//    private Map<Integer, Reading> sensorReadings;
//    private List<Sensor> sensorList;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        context = this.getApplicationContext();
//        activity = this;
//        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
//        sensorReadings = new HashMap<>();
//        publishTimer = new Timer(true);
//        publishTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                if (!sensorReadings.isEmpty()) {
//                    Location location = HomeActivity.location;
//                    if (location != null) {
//
//                        final Report rObject = new Report("0");
//                        rObject.setDeviceId(DynamixService.getPhoneProfiler().getPhoneId());
//                        final List<String> mlist = new ArrayList<>();
//                        rObject.setResults(mlist);
//                        final JSONObject obj = new JSONObject();
//                        try {
//                            obj.put("org.ambientdynamix.contextplugins.Latitude", location.getLatitude());
//                            obj.put("org.ambientdynamix.contextplugins.Longitude", location.getLongitude());
//                            sensorReadings.put(-1, new Reading(Reading.Datatype.Float, obj.toString(), "DefaultPlugin"));
//                            Log.d(TAG, location.toString());
//
//
//                            final List<Reading> readings = new ArrayList<>();
//                            final PluginInfo info = new PluginInfo();
//                            info.setState("ACTIVE");
//                            for (final Reading r : sensorReadings.values()) {
//                                mlist.add(new Reading(Reading.Datatype.String,
//                                        r.toJson(), "DefaultExperiment").getValue()
//                                );
//                            }
//                            info.setPayload(readings);
//                            final String message = rObject.toJson();
//                            Log.d(TAG, "ResultMessage:message " + message);
//
//
//                            DynamixService.publishMessage(message);
//
//
//                        } catch (JSONException e) {
//                        }
//
//                    }
//                    sensorReadings.clear();
//                }
//            }
//        }, 60000, 30000);
//
//        noiseTimer = new Timer(true);
//        noiseMeasureTask = new TimerTask() {
//            public double REFERENCE = 0.00002;
//
//            @Override
//            public void run() {
//                final double db = captureNoiseLevel();
//                final JSONObject obj = new JSONObject();
//                try {
//                    obj.put("urn:oc:attributeType:soundPressureLevel:ambient", db);
//                    sensorReadings.put(-2,
//                            new Reading(Reading.Datatype.Float, obj.toString(), "DefaultPlugin"));
//                } catch (JSONException e) {
//                }
//            }
//
//            private double captureNoiseLevel() {
//                try {
//                    final MediaRecorder mRecorder = new MediaRecorder();
//                    mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//                    mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//                    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//                    mRecorder.setOutputFile("/dev/null");
//                    mRecorder.prepare();
//                    mRecorder.start();
//                    double sum = 0;
//                    double ma = mRecorder.getMaxAmplitude();
//                    double value;
//                    for (int i = 1; i <= 10; i++) {
//                        Thread.sleep(100);
//                        sum += mRecorder.getMaxAmplitude();
//                        ma = sum / i;
//
//                    }
//                    value = (ma / 51805.5336);
//                    double db = 20 * Math.log10(value / REFERENCE);
//                    mRecorder.stop();
//                    mRecorder.reset();
//                    mRecorder.release();
//                    return db;
//                } catch (Exception e) {
//                    Log.w("NoiseLevel Plugin Error", e.toString());
//                    return -1;
//                }
//            }
//        };
//
//        final SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
//
//        List<DefaultSensor> sensors = new ArrayList<>();
//        if (hasSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)) {
//            final boolean state = sharedPref.getBoolean(getString(R.string.sensor_temperature), false);
//            final CompoundButton.OnCheckedChangeListener listener = new Switch.OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    final Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
//
//                    final SharedPreferences.Editor editor = sharedPref.edit();
//                    editor.putBoolean(getString(R.string.sensor_temperature), isChecked);
//                    editor.apply();
//
//                    if (isChecked) {
//                        mSensorManager.registerListener(activity, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
//                    } else {
//                        mSensorManager.unregisterListener(activity, mSensor);
//                    }
//
//                }
//            };
//            listener.onCheckedChanged(null, state);
//            sensors.add(new DefaultSensor(getString(R.string.sensor_temperature), "desc", state, listener));
//        }
//        if (hasSensor(Sensor.TYPE_RELATIVE_HUMIDITY)) {
//            final boolean state = sharedPref.getBoolean(getString(R.string.sensor_relative_humidity), false);
//            final CompoundButton.OnCheckedChangeListener listener = new Switch.OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    final Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
//
//                    final SharedPreferences.Editor editor = sharedPref.edit();
//                    editor.putBoolean(getString(R.string.sensor_relative_humidity), isChecked);
//                    editor.apply();
//
//                    if (isChecked) {
//                        mSensorManager.registerListener(activity, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
//                    } else {
//                        mSensorManager.unregisterListener(activity, mSensor);
//                    }
//                }
//            };
//            listener.onCheckedChanged(null, state);
//            sensors.add(new DefaultSensor(getString(R.string.sensor_relative_humidity), "desc", state, listener));
//        }
//        if (hasSensor(Sensor.TYPE_PRESSURE)) {
//            boolean state = sharedPref.getBoolean(getString(R.string.sensor_pressure), false);
//            final CompoundButton.OnCheckedChangeListener listener = new Switch.OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    final Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
//
//                    final SharedPreferences.Editor editor = sharedPref.edit();
//                    editor.putBoolean(getString(R.string.sensor_pressure), isChecked);
//                    editor.apply();
//
//                    if (isChecked) {
//                        mSensorManager.registerListener(activity, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
//                    } else {
//                        mSensorManager.unregisterListener(activity, mSensor);
//                    }
//                }
//            };
//            listener.onCheckedChanged(null, state);
//            sensors.add(new DefaultSensor(getString(R.string.sensor_pressure), "desc", state, listener));
//
//        }
//        boolean state = sharedPref.getBoolean(getString(R.string.sensor_noise), false);
//        final CompoundButton.OnCheckedChangeListener listener = new Switch.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                final SharedPreferences.Editor editor = sharedPref.edit();
//                editor.putBoolean(getString(R.string.sensor_noise), isChecked);
//                editor.apply();
//
//                if (isChecked) {
//                    noiseTimer.schedule(noiseMeasureTask, 0, 30000);
//                } else {
//                    noiseTimer.cancel();
//                }
//            }
//        };
//        listener.onCheckedChanged(null, state);
//        sensors.add(new DefaultSensor(getString(R.string.sensor_noise), "desc", state, listener));
//        // Create an empty adapter we will use to display the loaded data.
//        // We pass null for the cursor, then update it in onLoadFinished()
//        mAdapter = new DefaultSensorArrayAdapter(this, R.layout.default_sensing_tab_list_item, sensors);
//        setListAdapter(mAdapter);
//    }
//
//
//    @Override
//    public void onSensorChanged(SensorEvent event) {
//        switch (event.sensor.getType()) {
//            case Sensor.TYPE_AMBIENT_TEMPERATURE:
//                if (System.currentTimeMillis() - lastTemperature > 30000) {
//                    lastTemperature = System.currentTimeMillis();
//                    Log.d(TAG, event.sensor.getName() + " " + event.values[0]);
//                    final JSONObject obj = new JSONObject();
//                    try {
//                        obj.put("urn:oc:attributeType:temperature:ambient", event.values[0]);
//                        sensorReadings.put(Sensor.TYPE_AMBIENT_TEMPERATURE,
//                                new Reading(Reading.Datatype.Float, obj.toString(), "DefaultPlugin"));
//                    } catch (JSONException e) {
//                    }
//                }
//                break;
//            case Sensor.TYPE_RELATIVE_HUMIDITY:
//                if (System.currentTimeMillis() - lastHumidity > 30000) {
//                    lastHumidity = System.currentTimeMillis();
//                    Log.d(TAG, event.sensor.getName() + " " + event.values[0]);
//                    final JSONObject obj = new JSONObject();
//                    try {
//                        obj.put("urn:oc:attributeType:relativeHumidity", event.values[0]);
//                        sensorReadings.put(Sensor.TYPE_RELATIVE_HUMIDITY,
//                                new Reading(Reading.Datatype.Float, obj.toString(), "DefaultPlugin"));
//                    } catch (JSONException e) {
//                    }
//                }
//                break;
//            case Sensor.TYPE_PRESSURE:
//                if (System.currentTimeMillis() - lastPressure > 30000) {
//                    lastPressure = System.currentTimeMillis();
//                    Log.d(TAG, event.sensor.getName() + " " + event.values[0]);
//                    final JSONObject obj = new JSONObject();
//                    try {
//                        obj.put("urn:oc:atributeType:atmosphericPressure", event.values[0]);
//                        sensorReadings.put(Sensor.TYPE_PRESSURE,
//                                new Reading(Reading.Datatype.Float, obj.toString(), "DefaultPlugin"));
//                    } catch (JSONException e) {
//                    }
//                }
//                break;
//
//        }
//    }
//
//    @Override
//    public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
//        //ignore
//    }
//
//    private boolean hasSensor(int sensorType) {
//        for (final Sensor sensor : sensorList) {
//            if (sensor.getType() == sensorType) {
//                return true;
//            }
//        }
//        return false;
//    }
//}
