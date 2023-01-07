package com.creativemd.cmdcam.client.mode;

import com.creativemd.cmdcam.common.utils.CamPath;
import com.creativemd.cmdcam.common.utils.CamPoint;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class OutsideMode extends CamMode {

    public EntityLivingBase camPlayer;

    public OutsideMode(CamPath path) {
        super(path);
        if (path != null)
            this.camPlayer = new EntityZombie(Minecraft.getMinecraft().theWorld);
    }

    @Override
    public CamMode createMode(CamPath path) {
        return new OutsideMode(path);
    }

    @Override
    public String getDescription() {
        return "the player isn't the camera, you can control him at every time";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onPathFinish() {
        super.onPathFinish();
        Minecraft mc = Minecraft.getMinecraft();
        mc.setRenderViewEntity(mc.thePlayer);
    }

    @Override
    public EntityLivingBase getCamera() {
        return this.camPlayer;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void processPoint(CamPoint point) {
        super.processPoint(point);

        Minecraft mc = Minecraft.getMinecraft();
        mc.setRenderViewEntity(this.camPlayer);
        if (this.camPlayer instanceof EntityPlayer)
            ((EntityPlayer) this.camPlayer).capabilities.isFlying = true;
        this.camPlayer.setPositionAndRotation(point.x, point.y, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
        this.camPlayer.setLocationAndAngles(point.x, point.y - this.camPlayer.getEyeHeight() + mc.thePlayer.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
        this.camPlayer.setRotationYawHead(0);
    }

}
