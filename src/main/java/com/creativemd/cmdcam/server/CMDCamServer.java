package com.creativemd.cmdcam.server;

import java.util.ArrayList;
import java.util.Collection;

import com.creativemd.cmdcam.CMDCamProxy;
import com.creativemd.cmdcam.common.utils.CamPath;

import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public class CMDCamServer extends CMDCamProxy {
    
    @Override
    public void init(FMLInitializationEvent event) {
        
    }
    
    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        
    }
    
    public static CamPath getPath(World world, String name) {
        CamSaveData data = (CamSaveData) world.getPerWorldStorage().getOrLoadData(CamSaveData.class, CamSaveData.DATA_NAME);
        if (data != null)
            return data.get(name);
        return null;
    }
    
    public static void setPath(World world, String name, CamPath path) {
        CamSaveData data = (CamSaveData) world.getPerWorldStorage().getOrLoadData(CamSaveData.class, CamSaveData.DATA_NAME);
        if (data == null) {
            data = new CamSaveData();
            world.getPerWorldStorage().setData(CamSaveData.DATA_NAME, data);
        }
        data.set(name, path);
    }
    
    public static boolean removePath(World world, String name) {
        CamSaveData data = (CamSaveData) world.getPerWorldStorage().getOrLoadData(CamSaveData.class, CamSaveData.DATA_NAME);
        if (data != null)
            return data.remove(name);
        return false;
    }
    
    public static Collection<String> getSavedPaths(World world) {
        CamSaveData data = (CamSaveData) world.getPerWorldStorage().getOrLoadData(CamSaveData.class, CamSaveData.DATA_NAME);
        if (data != null)
            return data.names();
        return new ArrayList<>();
    }
    
    public static void clearPaths(World world) {
        CamSaveData data = (CamSaveData) world.getPerWorldStorage().getOrLoadData(CamSaveData.class, CamSaveData.DATA_NAME);
        if (data != null)
            data.clear();
    }
    
}
