package dylandesrosier.glossa;

import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;
import android.media.AudioFormat;
import android.os.Environment;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.TextView;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import androidx.annotation.NonNull;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.loopj.android.http.*;

import org.json.JSONObject;
import org.json.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import cz.msebera.android.httpclient.Header;
import omrecorder.AudioChunk;
import omrecorder.AudioRecordConfig;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.PullableSource;
import omrecorder.Recorder;
import omrecorder.WriteAction;

public class Pronunciation extends AppCompatActivity {
    private Integer score;
    private String letter;
    private String pronunciation;
    private Boolean isPlaying = false;

    // Recording Audio

    private static String fileName = null;
    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private boolean permissionToStoreAccepted = false;
    private boolean permissionToReadAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    private ImageButton recordButton = null;
    private ImageButton playButton = null;
    private MediaPlayer player = null;

    private omrecorder.Recorder wavRecorder = null;

    // Request
    private String url = "https://phoneme-recognition.herokuapp.com/getScore";
//    private String url = "http://d40a219f.ngrok.io/getScore";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pronunciation);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION );
        }

        letter = getIntent().getStringExtra("letter");
        pronunciation = getIntent().getStringExtra("pronunciation");

        TextView character = findViewById(R.id.character_text);
        character.setText(letter);
        character.setTextColor(getColor(R.color.DarkGrey));
        TextView pronun = findViewById(R.id.pronunciation);
        pronun.setText(pronunciation);

        fileName = "recording.wav";

        recordButton = findViewById(R.id.recordButton);
        recordButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    try {
                        onRecord(true);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    try {
                        onRecord(false);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                return true;
            }
        });

        playButton = findViewById(R.id.play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPlaying) {
                    return;
                } else {
                    isPlaying = true;
                    player = new MediaPlayer();
                    try {
                        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mediaPlayer) {
                                isPlaying = false;
                            }
                        });
                        player.setDataSource(String.format("%s/%s",Environment.getExternalStorageDirectory().getAbsolutePath(), fileName));
                        player.prepare();
                        player.start();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "prepare() failed");
                        isPlaying = false;
                    }
                }
            }
        });

        wavRecorder = OmRecorder.wav(
                new PullTransport.Noise(mic(),
                        new PullTransport.OnAudioChunkPulledListener() {
                            @Override public void onAudioChunkPulled(AudioChunk audioChunk) {
                                animateVoice((float) (audioChunk.maxAmplitude() / 200.0));
                            }
                        },
                        new WriteAction.Default(),
                        new Recorder.OnSilenceListener() {
                            @Override public void onSilence(long silenceTime) {
                                Log.e("silenceTime", String.valueOf(silenceTime));
                            }
                        }, 200
                ), file()
        );
    }

    @NonNull private File file() {
        return new File(Environment.getExternalStorageDirectory(), fileName);
    }

    private void animateVoice(final float maxPeak) {
        recordButton.animate().scaleX(1 + maxPeak).scaleY(1 + maxPeak).setDuration(10).start();
    }

    private PullableSource mic() {
        return new PullableSource.NoiseSuppressor(
                new PullableSource.Default(
                        new AudioRecordConfig.Default(
                                MediaRecorder.AudioSource.MIC, AudioFormat.ENCODING_PCM_16BIT,
                                AudioFormat.CHANNEL_IN_MONO, 44100
                        )
                )
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                permissionToStoreAccepted  = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                permissionToReadAccepted  = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted || !permissionToStoreAccepted || !permissionToReadAccepted) finish();
    }

    private void onRecord(boolean start) throws IOException {
        if (start) {
            // Create new instance w a blank file
            wavRecorder = OmRecorder.wav(
                    new PullTransport.Noise(mic(),
                            new PullTransport.OnAudioChunkPulledListener() {
                                @Override public void onAudioChunkPulled(AudioChunk audioChunk) {
                                    animateVoice((float) (audioChunk.maxAmplitude() / 200.0));
                                }
                            },
                            new WriteAction.Default(),
                            new Recorder.OnSilenceListener() {
                                @Override public void onSilence(long silenceTime) {
                                    Log.e("silenceTime", String.valueOf(silenceTime));
                                }
                            }, 200
                    ), file()
            );
            wavRecorder.startRecording();
        } else {
            // Stop recording and send data to server
            wavRecorder.stopRecording();
            getScore();
        }
    }

    private void getScore() {
        // Build request

        File myFile = new File(Environment.getExternalStorageDirectory(), fileName);
        RequestParams params = new RequestParams();

        try {
            params.put("audio", myFile);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        params.add("pronunciation", pronunciation);
        params.add("letter", letter);

        // send request
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    JSONObject r = new JSONObject(new String(response));
                    Log.d(LOG_TAG, r.getString("score"));
                    TextView score = findViewById(R.id.score);
                    score.setText(String.format("%s", r.getString("score")));
                    score.setVisibility(View.VISIBLE);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
                TextView score = findViewById(R.id.score);
                score.setText("There was an issue scoring your audio");
                score.setVisibility(View.VISIBLE);
                Log.d(LOG_TAG, "failed");
            }
        });
    }
}
