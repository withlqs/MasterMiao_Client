package master.miao.locationlogger;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
