package com.creativemd.cmdcam.client;

import java.util.List;

import com.creativemd.cmdcam.client.interpolation.CamInterpolation;
import com.creativemd.cmdcam.common.packet.GetPathPacket;
import com.creativemd.cmdcam.common.packet.SetPathPacket;
import com.creativemd.cmdcam.common.utils.CamPath;
import com.creativemd.cmdcam.common.utils.CamPoint;
import com.creativemd.cmdcam.common.utils.CamTarget;
import com.creativemd.cmdcam.server.CamCommandServer;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.mojang.realmsclient.gui.ChatFormatting;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.EntityNotFoundException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CamCommandClient extends CommandBase {

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    public static Minecraft mc = Minecraft.getMinecraft();

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender
                .addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam add [number] " + ChatFormatting.RED + "register a point at the current position"));
            sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam clear " + ChatFormatting.RED + "delete all registered points"));
            sender
                .addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam start [time|ms|s|m|h|d] [loops (-1 -> endless)] " + ChatFormatting.RED + "starts the animation"));
            sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam stop " + ChatFormatting.RED + "stops the animation"));
            sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam goto <index> " + ChatFormatting.RED + "tp to the given point"));
            sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam set <index> " + ChatFormatting.RED + "updates point to current location"));
            sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam remove <index> " + ChatFormatting.RED + "removes the given point"));
            sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam target <none:self> " + ChatFormatting.RED + "set the camera target"));
            sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam mode <default:outside> " + ChatFormatting.RED + "set current mode"));
            sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam interpolation <" + String
                .join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "set the camera interpolation"));
            sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam follow-speed <number> " + ChatFormatting.RED + "default is 1.0"));
            sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam show <all:" + String
                .join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "shows the path using the given interpolation"));
            sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam hide <all:" + String
                .join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "hides the path using the given interpolation"));
            sender
                .addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam save <name> " + ChatFormatting.RED + "saves the current path (including settings) with the given name"));
            sender
                .addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam load <name> " + ChatFormatting.RED + "tries to load the saved path with the given name"));
            sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam list " + ChatFormatting.RED + "lists all saved paths"));
        } else {
            String subCommand = args[0];
            if (subCommand.equals("clear")) {
                sender.addChatMessage(new TextComponentString("Cleared all registered points!"));
                CMDCamClient.points.clear();
            } else if (subCommand.equals("add")) {
                if (args.length == 1) {
                    CMDCamClient.points.add(new CamPoint());
                    sender.addChatMessage(new TextComponentString("Registered " + CMDCamClient.points.size() + ". Point!"));
                } else if (args.length == 2) {
                    try {
                        Integer index = Integer.parseInt(args[1]) - 1;
                        if (index >= 0 && index < CMDCamClient.points.size()) {
                            CMDCamClient.points.add(index, new CamPoint());
                            sender.addChatMessage(new TextComponentString("Inserted " + index + ". Point!"));
                        } else
                            sender.addChatMessage(new TextComponentString("The given index '" + args[1] + "' is too high/low!"));
                    } catch (Exception e) {
                        sender.addChatMessage(new TextComponentString("Invalid index '" + args[1] + "'!"));
                    }

                } else
                    sender
                        .addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam add [number] " + ChatFormatting.RED + "register a point at the current position."));
            } else if (subCommand.equals("start")) {
                if (args.length >= 2) {
                    long duration = CamCommandServer.StringToDuration(args[1]);
                    if (duration > 0)
                        CMDCamClient.lastDuration = duration;
                    else {
                        sender.addChatMessage(new TextComponentString("Invalid time '" + args[1] + "'!"));
                        return;
                    }

                    if (args.length >= 3) {
                        CMDCamClient.lastLoop = Integer.parseInt(args[2]);
                    }
                }
                try {
                    CMDCamClient.startPath(CMDCamClient.createPathFromCurrentConfiguration());
                } catch (PathParseException e) {
                    sender.addChatMessage(new TextComponentString(e.getMessage()));
                }
            } else if (subCommand.equals("stop")) {
                CMDCamClient.stopPath();
            } else if (subCommand.equals("remove")) {
                if (args.length >= 2) {
                    try {
                        Integer index = Integer.parseInt(args[1]) - 1;
                        if (index >= 0 && index < CMDCamClient.points.size()) {
                            CMDCamClient.points.remove((int) index);
                            sender.addChatMessage(new TextComponentString("Removed " + (index + 1) + ". point!"));
                        } else
                            sender.addChatMessage(new TextComponentString("The given index '" + args[1] + "' is too high/low!"));
                    } catch (Exception e) {
                        sender.addChatMessage(new TextComponentString("Invalid index '" + args[1] + "'!"));
                    }
                } else
                    sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam remove <index> " + ChatFormatting.RED + "removes the given point"));
            } else if (subCommand.equals("set")) {
                if (args.length >= 2) {
                    try {
                        Integer index = Integer.parseInt(args[1]) - 1;
                        if (index >= 0 && index < CMDCamClient.points.size()) {
                            CMDCamClient.points.set(index, new CamPoint());
                            sender.addChatMessage(new TextComponentString("Updated " + (index + 1) + ". point!"));
                        } else
                            sender.addChatMessage(new TextComponentString("The given index '" + args[1] + "' is too high/low!"));
                    } catch (Exception e) {
                        sender.addChatMessage(new TextComponentString("Invalid index '" + args[1] + "'!"));
                    }
                } else
                    sender
                        .addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam set <index> " + ChatFormatting.RED + "updates the giveng point to the current location"));
            } else if (subCommand.equals("goto")) {
                if (args.length >= 2) {
                    try {
                        Integer index = Integer.parseInt(args[1]) - 1;
                        if (index >= 0 && index < CMDCamClient.points.size()) {
                            CamPoint point = CMDCamClient.points.get(index);
                            mc.thePlayer.capabilities.isFlying = true;

                            CamEventHandlerClient.roll = (float) point.roll;
                            mc.gameSettings.fovSetting = (float) point.zoom;
                            mc.thePlayer.setPositionAndRotation(point.x, point.y, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
                            mc.thePlayer.setLocationAndAngles(point.x, point.y/*-mc.thePlayer.getEyeHeight()*/, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
                        } else
                            sender.addChatMessage(new TextComponentString("The given index '" + args[1] + "' is too high/low!"));
                    } catch (Exception e) {
                        sender.addChatMessage(new TextComponentString("Invalid index '" + args[1] + "'!"));
                    }
                } else {
                    sender.addChatMessage(new TextComponentString("Missing point!"));
                    sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam goto <index> " + ChatFormatting.RED + "tp to the given point"));
                }
            } else if (subCommand.equals("mode")) {
                if (args.length >= 2) {
                    if (args[1].equals("default") || args[1].equals("outside")) {
                        CMDCamClient.lastMode = args[1];
                        sender.addChatMessage(new TextComponentString("Changed to " + args[1] + " path!"));
                    } else
                        sender.addChatMessage(new TextComponentString("Path mode '" + args[1] + "' does not exit!"));
                } else
                    sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam mode <default:outside> " + ChatFormatting.RED + "set current mode"));
            } else if (subCommand.equals("target")) {
                if (args.length == 2) {
                    String target = args[1];
                    if (target.equals("self")) {
                        CMDCamClient.target = new CamTarget.SelfTarget();
                        sender.addChatMessage(new TextComponentString("The camera will point towards you!"));
                    } else if (target.equals("none")) {
                        CMDCamClient.target = null;
                        sender.addChatMessage(new TextComponentString("Removed target!"));
                    } else
                        sender.addChatMessage(new TextComponentString("Target '" + target + "' not found!"));
                } else
                    CamEventHandlerClient.startSelectingTarget(null);

            } else if (subCommand.equals("interpolation")) {
                if (args.length == 2) {
                    String target = args[1];
                    CamInterpolation move = CamInterpolation.getInterpolation(target);
                    if (move != null) {
                        CMDCamClient.lastInterpolation = target;
                        sender.addChatMessage(new TextComponentString("Interpolation is set to '" + target + "'!"));
                    } else
                        sender.addChatMessage(new TextComponentString("Interpolation '" + target + "' not found!"));
                } else
                    sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam interpolation <" + String
                        .join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "set the camera interpolation"));
            } else if (subCommand.equals("follow-speed")) {
                if (args.length == 2) {
                    try {
                        double followspeed = Double.parseDouble(args[1]);
                        CMDCamClient.cameraFollowSpeed = followspeed;
                        sender.addChatMessage(new TextComponentString("Camera follow speed is set to  '" + followspeed + "'. Default is 1.0!"));
                    } catch (NumberFormatException e) {
                        sender.addChatMessage(new TextComponentString("'" + args[1] + "' is an invalid number!"));
                    }
                } else
                    sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam follow-speed <number> " + ChatFormatting.RED + "default is 1.0"));
            } else if (subCommand.equals("show")) {
                if (args.length == 2) {
                    String target = args[1];
                    CamInterpolation move = CamInterpolation.getInterpolation(target);
                    if (move != null) {
                        move.isRenderingEnabled = true;
                        sender.addChatMessage(new TextComponentString("Showing '" + target + "' interpolation path!"));
                    } else if (target.equals("all")) {
                        for (CamInterpolation movement : CamInterpolation.interpolationTypes.values()) {
                            movement.isRenderingEnabled = true;
                        }
                        sender.addChatMessage(new TextComponentString("Showing all interpolation paths!"));
                    } else
                        sender.addChatMessage(new TextComponentString("Interpolation '" + target + "' not found!"));
                } else
                    sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam show <all:" + String
                        .join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "shows the path using the given interpolation"));
            } else if (subCommand.equals("hide")) {
                if (args.length == 2) {
                    String target = args[1];
                    CamInterpolation move = CamInterpolation.getInterpolation(target);
                    if (move != null) {
                        move.isRenderingEnabled = false;
                        sender.addChatMessage(new TextComponentString("Hiding '" + target + "' interpolation path!"));
                    } else if (target.equals("all")) {
                        for (CamInterpolation movement : CamInterpolation.interpolationTypes.values()) {
                            movement.isRenderingEnabled = false;
                        }
                        sender.addChatMessage(new TextComponentString("Hiding all interpolation paths!"));
                    } else
                        sender.addChatMessage(new TextComponentString("Interpolation '" + target + "' not found!"));
                } else
                    sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam hide <all:" + String
                        .join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "hides the path using the given interpolation"));
            } else if (subCommand.equals("save")) {
                if (args.length == 2) {
                    try {
                        CamPath path = CMDCamClient.createPathFromCurrentConfiguration();

                        if (CMDCamClient.isInstalledOnSever) {
                            PacketHandler.sendPacketToServer(new SetPathPacket(args[1], path));
                        } else if (!sender.getServer().isSinglePlayer()) {
                            CMDCamClient.savedPaths.put(args[1], path);
                            sender.addChatMessage(new TextComponentString("Path will not be saved permanently. It's not installed on server side."));
                        } else {
                            CMDCamClient.savedPaths.put(args[1], path);
                            sender.addChatMessage(new TextComponentString("Saved path '" + args[1] + "' successfully!"));
                        }
                    } catch (PathParseException e) {
                        sender.addChatMessage(new TextComponentString(e.getMessage()));
                    }

                } else
                    sender
                        .addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam save <name> " + ChatFormatting.RED + "saves the current path (including settings) with the given name"));
            } else if (subCommand.equals("load")) {
                if (args.length == 2) {
                    if (CMDCamClient.isInstalledOnSever) {
                        PacketHandler.sendPacketToServer(new GetPathPacket(args[1]));
                    } else {
                        CamPath path = CMDCamClient.savedPaths.get(args[1]);
                        if (path != null) {
                            path.overwriteClientConfig();
                            sender.addChatMessage(new TextComponentString("Loaded path '" + args[1] + "' successfully!"));
                        } else
                            sender.addChatMessage(new TextComponentString("Could not find path '" + args[1] + "'!"));
                    }
                } else
                    sender
                        .addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam load <name> " + ChatFormatting.RED + "tries to load the saved path with the given name"));
            } else if (subCommand.equals("list")) {
                if (CMDCamClient.isInstalledOnSever) {
                    sender.addChatMessage(new TextComponentString("Use /cam-server list instead!"));
                    return;
                }
                String output = "There are " + CMDCamClient.savedPaths.size() + " path(s) in total. ";
                for (String key : CMDCamClient.savedPaths.keySet()) {
                    output += key + ", ";
                }
                sender.addChatMessage(new TextComponentString(output));
            }
        }
    }

    @Override
    public String getCommandName() {
        return "cam";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "used to control the camera";
    }

    private static List<EntityPlayerMP> getPlayers(MinecraftServer server, ICommandSender sender, String target) {
    	List<Entity> entityList = null;
    	List<EntityPlayerMP> playerList = null;

    	try {
        	entityList = getEntityList(server, sender, target);
    	} catch (EntityNotFoundException e) {
    		return null;
    	} catch (CommandException e) {
    		return null;
    	}

    	for (Entity e : entityList) {
    		if (e instanceof EntityPlayerMP) {
    			playerList.add((EntityPlayerMP) e);
    		}
    	}

    	return playerList;
    }
}
