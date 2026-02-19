package com.crystalpvp.data;

import com.crystalpvp.enums.EventState;
import lombok.Data;
import org.bukkit.Location;

@Data
public class EventData {
    private EventState state = EventState.CLOSED;
    private Location pos1;
    private Location pos2;
    private Location center;
    private Location spawn;
    private Location lobby;
    private double borderRadius;
    private double currentBorderSize;
}