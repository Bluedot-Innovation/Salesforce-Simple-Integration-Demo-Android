package bluedotinnovation.com.bdsalesforceintegrationsample;

import android.content.Context;
import au.com.bluedot.point.net.engine.BDError;
import au.com.bluedot.point.net.engine.BluedotServiceReceiver;
import org.jetbrains.annotations.NotNull;

public class BluedotErrorReceiver extends BluedotServiceReceiver{
    @Override
    public void onBluedotServiceError(@NotNull BDError bdError, @NotNull Context context) {
        MainApplication mainApplication = (MainApplication)context.getApplicationContext();
        mainApplication.logInfo(bdError.getReason());
    }
}
