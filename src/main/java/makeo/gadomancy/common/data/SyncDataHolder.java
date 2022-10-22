package makeo.gadomancy.common.data;

import makeo.gadomancy.common.network.PacketHandler;
import makeo.gadomancy.common.network.packets.PacketSyncData;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is part of the Gadomancy Mod
 * Gadomancy is Open Source and distributed under the
 * GNU LESSER GENERAL PUBLIC LICENSE
 * for more read the LICENSE file
 *
 * Created by HellFirePvP @ 13.12.2015 15:53
 */
public class SyncDataHolder {

    private static Map<String, AbstractData> serverData = new HashMap<String, AbstractData>();
    private static Map<String, AbstractData> clientData = new HashMap<String, AbstractData>();

    private static List<String> dirtyData = new ArrayList<String>();
    private static byte providerCounter = 0;

    public static void register(AbstractData.AbstractDataProvider<? extends AbstractData> provider) {
        AbstractData.Registry.register(provider);
        AbstractData ad = provider.provideNewInstance();
        ad.setProviderId(provider.getProviderId());
        serverData.put(provider.getKey(), ad);
        ad = provider.provideNewInstance();
        ad.setProviderId(provider.getProviderId());
        clientData.put(provider.getKey(), ad);
    }

    public static byte allocateNewId() {
        byte pId = providerCounter;
        providerCounter++;
        return pId;
    }

    public static <T extends AbstractData> T getDataServer(String key) {
        return (T) serverData.get(key);
    }

    public static <T extends AbstractData> T getDataClient(String key) {
        return (T) clientData.get(key);
    }

    public static void markForUpdate(String key) {
        if(!dirtyData.contains(key)) {
            dirtyData.add(key);
        }
    }

    public static void syncAllDataTo(EntityPlayer player) {
        PacketSyncData dataSync = new PacketSyncData(serverData, true);
        PacketHandler.INSTANCE.sendTo(dataSync, (net.minecraft.entity.player.EntityPlayerMP) player);
    }

    public static void receiveServerPacket(Map<String, AbstractData> data) {
        for(String key : data.keySet()) {
            AbstractData dat = clientData.get(key);
            if(dat != null) {
                dat.handleIncomingData(data.get(key));
            }
        }
    }

    public static void doNecessaryUpdates() {
        if(dirtyData.isEmpty()) return;
        Map<String, AbstractData> pktData = new HashMap<String, AbstractData>();
        for(String s : dirtyData) {
            AbstractData d = getDataServer(s);
            if(d.needsUpdate()) {
                pktData.put(s, d);
            }
        }
        dirtyData.clear();
        PacketSyncData dataSync = new PacketSyncData(pktData, false);
        PacketHandler.INSTANCE.sendToAll(dataSync);
    }

    public static void initialize() {
        register(new DataFamiliar.Provider("FamiliarData"));
        register(new DataAchromatic.Provider("AchromaticData"));
    }

}
