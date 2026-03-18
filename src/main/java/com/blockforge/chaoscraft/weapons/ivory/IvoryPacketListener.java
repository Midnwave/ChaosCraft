package com.blockforge.chaoscraft.weapons.ivory;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;

public class IvoryPacketListener extends PacketListenerAbstract {

    private final ChaosCraftPlugin plugin;
    private final IvoryConfig config;
    private final IvoryStateManager stateManager;

    public IvoryPacketListener(ChaosCraftPlugin plugin, IvoryConfig config, IvoryStateManager stateManager) {
        super(PacketListenerPriority.HIGH);
        this.plugin = plugin;
        this.config = config;
        this.stateManager = stateManager;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Reserved for future packet-level swing prevention
    }
}
