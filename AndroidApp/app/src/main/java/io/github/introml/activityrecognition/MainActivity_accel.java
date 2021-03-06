package io.github.introml.activityrecognition;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements SensorEventListener, TextToSpeech.OnInitListener {

    List<Float> values = new ArrayList<Float>();

    private TextView walkForwardTextView;
    private TextView walkLeftTextView;
    private TextView walkRightTextView;
    private TextView walkUpTextView;
    private TextView walkDownTextView;
    private TextView runForwardTextView;
    private TextView jumpUpTextView;
    private TextView sitTextView;
    private TextView standTextView;
    private TextView sleepTextView;
    private TextView elevatorUpTextView;
    private TextView elevatorDownTextView;
    private TextToSpeech textToSpeech;
    private float[] results;
    private TensorFlowClassifier classifier;

    float[] gravity = new float[3];
    private Float[] sample = {null, null, null};
    private static List<Float> x;
    private static List<Float> y;
    private static List<Float> z;
    private float[] linear_acceleration = new float[3];


    private static final int N_FEATURES = 3;
    private static final int N_STEPS = 90;

    private String[] labels = {"WalkForward", "WalkLeft", "WalkRight", "WalkUp", "WalkDown", "RunForward", "JumpUp", "Sit", "Stand", "Sleep", "ElevatorUp", "ElevatorDown"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        values = new ArrayList<>();
        x = new ArrayList<>();
        y = new ArrayList<>();
        z = new ArrayList<>();


        walkForwardTextView = (TextView) findViewById(R.id.walkforward_prob);
        walkLeftTextView = (TextView) findViewById(R.id.walkleft_prob);
        walkRightTextView = (TextView) findViewById(R.id.walkright_prob);
        walkUpTextView = (TextView) findViewById(R.id.walkup_prob);
        walkDownTextView = (TextView) findViewById(R.id.walkdown_prob);
        runForwardTextView = (TextView) findViewById(R.id.runforward_prob);
        jumpUpTextView = (TextView) findViewById(R.id.jump_prob);
        sitTextView = (TextView) findViewById(R.id.sit_prob);
        standTextView = (TextView) findViewById(R.id.stand_prob);
        sleepTextView = (TextView) findViewById(R.id.sleep_prob);
        elevatorUpTextView = (TextView) findViewById(R.id.elevatorup_prob);
        elevatorDownTextView = (TextView) findViewById(R.id.elevatordown_prob);

        classifier = new TensorFlowClassifier(getApplicationContext());

        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setLanguage(Locale.US);
    }

    @Override
    public void onInit(int status) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (results == null || results.length == 0) {
                    return;
                }
                float max = -1;
                int idx = -1;
                for (int i = 0; i < results.length; i++) {
                    if (results[i] > max) {
                        idx = i;
                        max = results[i];
                    }
                }

                textToSpeech.speak(labels[idx], TextToSpeech.QUEUE_ADD, null, Integer.toString(new Random().nextInt()));
            }
        }, 3000, 5000);
    }

    protected void onPause() {
        getSensorManager().unregisterListener(this);
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 10000, 10000);

    }

    private boolean isSampleComplete() {
        for (int i=0; i<sample.length; i++) {
            if (sample[i] == null) {
                return false;
            }
        }

        return true;
    }

    private float[] convertAcceleration(float[] acc) {
        final float alpha = 0.8f;

        gravity[0] = alpha * gravity[0] + (1 - alpha) * acc[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * acc[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * acc[2];

        float[] result = {acc[0] - gravity[0], acc[1] - gravity[1], acc[2] - gravity[2]};
        return result;


    @Override
    public void onSensorChanged(SensorEvent event) {


        final float alpha = (float) 0.8;

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

        values.add(linear_acceleration[0]);
        values.add(linear_acceleration[1]);
        values.add(linear_acceleration[2]);


        if (values.size() == N_FEATURES * N_STEPS) {

            float[] arrVals = new float[N_FEATURES * N_STEPS];
            for (int i = 0; i < arrVals.length; i++) {
                arrVals[i] = values.get(i);
            }

            Log.v("arrVals", Arrays.toString(arrVals));

            results = classifier.predictProbabilities(arrVals);


            walkForwardTextView.setText(Float.toString(results[0]));
            walkLeftTextView.setText(Float.toString(results[1]));
            walkRightTextView.setText(Float.toString(results[2]));
            walkUpTextView.setText(Float.toString(results[3]));
            walkDownTextView.setText(Float.toString(results[4]));
            runForwardTextView.setText(Float.toString(results[5]));
            jumpUpTextView.setText(Float.toString(results[6]));
            sitTextView.setText(Float.toString(results[7]));
            standTextView.setText(Float.toString(results[8]));
            sleepTextView.setText(Float.toString(results[9]));
            elevatorUpTextView.setText(Float.toString(results[10]));
            elevatorDownTextView.setText(Float.toString(results[11]));

            Log.v("data float array", Arrays.toString(arrVals));

            Log.v("results", Arrays.toString(results));

            values.clear();


        x.clear();
        y.clear();
        z.clear();
    }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    private SensorManager getSensorManager() {
        return (SensorManager) getSystemService(SENSOR_SERVICE);
    }

}
