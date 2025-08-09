package azuo.oplusgms;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

public class MonitorWorker extends Worker {
    public static final String GMS_RESTRICTED = "google_restric_info";

    public MonitorWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        try {
            ContentResolver resolver = context.getContentResolver();
            if (Settings.Secure.getInt(resolver, GMS_RESTRICTED, 0) != 0) {
                boolean reset =
                    context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
                        PackageManager.PERMISSION_GRANTED;
                if (reset)
                    Settings.Secure.putInt(resolver, GMS_RESTRICTED, 0);
                Log.i("OnePlusGMS", GMS_RESTRICTED + " = " + (reset ? 0 : 1));

                NotificationManagerCompat manager = NotificationManagerCompat.from(context);
                if (manager.areNotificationsEnabled()) {
                    NotificationChannelCompat channel =
                        new NotificationChannelCompat.Builder(
                            "oplusgms_monitor", NotificationManagerCompat.IMPORTANCE_DEFAULT
                        ).setName("OnePlus GMS monitor").build();
                    manager.createNotificationChannel(channel);
                    String group = getClass().getName();
                    manager.notify(
                        (int)SystemClock.uptimeMillis(),
                        new NotificationCompat.Builder(context, channel.getId())
                            .setSmallIcon(R.mipmap.ic_launcher)
                            //.setContentTitle(GMS_RESTRICTED)
                            .setContentText(GMS_RESTRICTED + " modified" + (reset ? ", reset." : "."))
                            .setGroup(group)
                            .build()
                    );
                    manager.notify(
                        -1,
                        new NotificationCompat.Builder(context, channel.getId())
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setGroup(group)
                            .setGroupSummary(true)
                            .build()
                    );
                }
            }
        }
        catch (Exception e) {
            Log.e("OnePlusGMS", "Monitor error.", e);
        }
        enqueueTrigger(context);
        return Result.success();
    }

    public static void enqueue(Context context) {
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequest.from(MonitorWorker.class));
    }

    public static void enqueueTrigger(Context context) {
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.enqueueUniqueWork(
            "OnePlusGMSTrigger",
            ExistingWorkPolicy.KEEP,
            new OneTimeWorkRequest.Builder(Trigger.class).setConstraints(
                new Constraints.Builder().addContentUriTrigger(
                    Settings.Secure.getUriFor(GMS_RESTRICTED),
                    false
                ).build()
            ).build()
        );
        workManager.enqueueUniqueWork(
            "OnePlusGMSTimer",
            ExistingWorkPolicy.REPLACE,
            new OneTimeWorkRequest.Builder(Trigger.class)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()
        );
    }

    public static class Trigger extends Worker {
        public Trigger(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            enqueue(getApplicationContext());
            return Result.success();
        }
    }
}
