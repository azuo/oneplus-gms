package azuo.oplusgms;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
               PackageManager.PERMISSION_GRANTED) {
            onClick(findViewById(R.id.refresh));
            MonitorWorker.enqueue(this);
        }
        else {
            findViewById(R.id.refresh).setVisibility(View.GONE);
            this.<TextView>findViewById(R.id.textView).setText(
                "1. Run the following command:\n\n" +
                "adb shell pm grant " + getPackageName() +
                " android.permission.WRITE_SECURE_SETTINGS\n\n" +
                "2. Grant notification permission;\n" +
                "3. Enable auto run;\n" +
                "4. Stop and restart the app."
            );
        }
    }

    public void onClick(View button) {
        int v = Settings.Secure.getInt(getContentResolver(), MonitorWorker.GMS_RESTRICTED, -1);
        this.<TextView>findViewById(R.id.textView).setText(MonitorWorker.GMS_RESTRICTED + " = " + v);
    }
}
