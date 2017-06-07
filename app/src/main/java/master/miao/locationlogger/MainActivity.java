package master.miao.locationlogger;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {
    TextView latitude_tv;
    TextView longitude_tv;
    Handler refreshLocation;
    Location nowLocation;

    public void setLocation(Location location) {
        nowLocation = location;
        refreshLocation.sendMessage(new Message());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitude_tv = (TextView) findViewById(R.id.latitude_text_view);
        longitude_tv = (TextView) findViewById(R.id.longitude_text_view);

        refreshLocation = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                latitude_tv.setText(String.valueOf(nowLocation.getLatitude()));
                longitude_tv.setText(String.valueOf(nowLocation.getLongitude()));
            }
        };

        Button button = (Button) findViewById(R.id.start_loop);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocationLooper locationLooper = new LocationLooper(MainActivity.this);
                locationLooper.start();
            }
        });
    }
}
