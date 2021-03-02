package bluedotinnovation.com.bdsalesforceintegrationsample;

import android.content.Context;
import au.com.bluedot.point.net.engine.GeoTriggeringEventReceiver;
import au.com.bluedot.point.net.engine.ZoneEntryEvent;
import au.com.bluedot.point.net.engine.ZoneExitEvent;
import au.com.bluedot.point.net.engine.ZoneInfo;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class AppGeoTriggerReceiver extends GeoTriggeringEventReceiver {

    @Override public void onZoneInfoUpdate(@NotNull List<ZoneInfo> list, @NotNull Context context) {
        MainApplication mainApplication = (MainApplication)context.getApplicationContext();
        mainApplication.logInfo("Received onZoneInfoUpdate: size "+list.size());
    }

    @Override
    public void onZoneEntryEvent(@NotNull ZoneEntryEvent zoneEntryEvent, @NotNull Context context) {
        MainApplication mainApplication = (MainApplication)context.getApplicationContext();
        String title = "Entered zone "+zoneEntryEvent.getZoneInfo().getZoneName()+" via fence "+zoneEntryEvent.getFenceInfo().getName();
        String content = "";
        if (zoneEntryEvent.getZoneInfo().getCustomData() != null && !zoneEntryEvent.getZoneInfo().getCustomData().isEmpty())
           content = "Data: "+zoneEntryEvent.getZoneInfo().getCustomData();
        mainApplication.logInfo(title + content);
    }

    @Override
    public void onZoneExitEvent(@NotNull ZoneExitEvent zoneExitEvent, @NotNull Context context) {
        MainApplication mainApplication = (MainApplication)context.getApplicationContext();
        String title = "Exited zone "+zoneExitEvent.getZoneInfo().getZoneName();
        String content = "Dwell time: "+zoneExitEvent.getDwellTime()+" minutes";
        mainApplication.logInfo(title + content);
    }
}
