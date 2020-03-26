package team.creative.cmdcam;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.realmsclient.gui.ChatFormatting;

import net.minecraft.command.Commands;
import net.minecraft.command.arguments.ArgumentSerializer;
import net.minecraft.command.arguments.ArgumentTypes;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import team.creative.cmdcam.client.CMDCamClient;
import team.creative.cmdcam.common.command.argument.CamModeArgument;
import team.creative.cmdcam.common.command.argument.DurationArgument;
import team.creative.cmdcam.common.command.argument.InterpolationArgument;
import team.creative.cmdcam.common.command.argument.InterpolationArgument.AllInterpolationArgument;
import team.creative.cmdcam.common.command.argument.TargetArgument;
import team.creative.cmdcam.common.packet.ConnectPacket;
import team.creative.cmdcam.common.packet.GetPathPacket;
import team.creative.cmdcam.common.packet.SetPathPacket;
import team.creative.cmdcam.common.packet.StartPathPacket;
import team.creative.cmdcam.common.packet.StopPathPacket;
import team.creative.cmdcam.common.util.CamPath;
import team.creative.cmdcam.server.CMDCamServer;
import team.creative.creativecore.common.network.CreativeNetwork;
import team.creative.creativecore.common.network.CreativePacket;

@Mod(value = CMDCam.MODID)
public class CMDCam {
	
	public static final String MODID = "cmdcam";
	
	private static final Logger LOGGER = LogManager.getLogger(CMDCam.MODID);
	public static CreativeNetwork NETWORK;
	
	public CMDCam() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::client);
		MinecraftForge.EVENT_BUS.addListener(this::serverStarting);
	}
	
	private void client(final FMLClientSetupEvent event) {
		CMDCamClient.init(event);
	}
	
	private void init(final FMLCommonSetupEvent event) {
		NETWORK = new CreativeNetwork("1.0", LOGGER, new ResourceLocation(CMDCam.MODID, "main"));
		NETWORK.registerType(ConnectPacket.class);
		NETWORK.registerType(GetPathPacket.class);
		NETWORK.registerType(SetPathPacket.class);
		NETWORK.registerType(StartPathPacket.class);
		NETWORK.registerType(StopPathPacket.class);
		
		ArgumentTypes.register("duration", DurationArgument.class, new ArgumentSerializer<>(() -> DurationArgument.duration()));
		ArgumentTypes.register("cameramode", CamModeArgument.class, new ArgumentSerializer<>(() -> CamModeArgument.mode()));
		ArgumentTypes.register("cameratarget", TargetArgument.class, new ArgumentSerializer<>(() -> TargetArgument.target()));
		ArgumentTypes.register("interpolation", InterpolationArgument.class, new ArgumentSerializer<>(() -> InterpolationArgument.interpolation()));
		ArgumentTypes.register("allinterpolation", AllInterpolationArgument.class, new ArgumentSerializer<>(() -> InterpolationArgument.interpolationAll()));
	}
	
	private void serverStarting(final FMLServerStartingEvent event) {
		event.getCommandDispatcher().register(Commands.literal("cam-server").executes((x) -> {
			x.getSource().sendFeedback(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server start <player> <path> [time|ms|s|m|h|d] [loops (-1 -> endless)] " + ChatFormatting.RED + "starts the animation"), false);
			x.getSource().sendFeedback(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server stop <player> " + ChatFormatting.RED + "stops the animation"), false);
			x.getSource().sendFeedback(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server list " + ChatFormatting.RED + "lists all saved paths"), false);
			x.getSource().sendFeedback(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server remove <name> " + ChatFormatting.RED + "removes the given path"), false);
			x.getSource().sendFeedback(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server clear " + ChatFormatting.RED + "clears all saved paths"), false);
			return 0;
		}).then(Commands.literal("start").then(Commands.argument("players", EntityArgument.players()).then(Commands.argument("name", StringArgumentType.string()).executes((x) -> {
			Collection<ServerPlayerEntity> players = EntityArgument.getPlayers(x, "players");
			if (players.isEmpty())
				return 0;
			String pathName = StringArgumentType.getString(x, "name");
			CamPath path = CMDCamServer.getPath(x.getSource().getWorld(), pathName);
			if (path != null) {
				CreativePacket packet = new StartPathPacket(path);
				for (ServerPlayerEntity player : players)
					CMDCam.NETWORK.sendToClient(packet, player);
			} else
				x.getSource().sendFeedback(new StringTextComponent("Path '" + pathName + "' could not be found!"), true);
			return 0;
		}).then(Commands.argument("duration", DurationArgument.duration()).executes((x) -> {
			Collection<ServerPlayerEntity> players = EntityArgument.getPlayers(x, "players");
			if (players.isEmpty())
				return 0;
			String pathName = StringArgumentType.getString(x, "name");
			CamPath path = CMDCamServer.getPath(x.getSource().getWorld(), pathName);
			if (path != null) {
				path = path.copy();
				long duration = DurationArgument.getDuration(x, "duration");
				if (duration > 0)
					path.duration = duration;
				CreativePacket packet = new StartPathPacket(path);
				for (ServerPlayerEntity player : players)
					CMDCam.NETWORK.sendToClient(packet, player);
			} else
				x.getSource().sendFeedback(new StringTextComponent("Path '" + pathName + "' could not be found!"), true);
			return 0;
		}).then(Commands.argument("loop", IntegerArgumentType.integer(-1)).executes((x) -> {
			Collection<ServerPlayerEntity> players = EntityArgument.getPlayers(x, "players");
			if (players.isEmpty())
				return 0;
			String pathName = StringArgumentType.getString(x, "name");
			CamPath path = CMDCamServer.getPath(x.getSource().getWorld(), pathName);
			if (path != null) {
				path = path.copy();
				long duration = DurationArgument.getDuration(x, "duration");
				if (duration > 0)
					path.duration = duration;
				
				path.currentLoop = IntegerArgumentType.getInteger(x, "loop");
				CreativePacket packet = new StartPathPacket(path);
				for (ServerPlayerEntity player : players)
					CMDCam.NETWORK.sendToClient(packet, player);
			} else
				x.getSource().sendFeedback(new StringTextComponent("Path '" + pathName + "' could not be found!"), true);
			return 0;
		})))))).then(Commands.literal("stop").then(Commands.argument("players", EntityArgument.players()).executes((x) -> {
			CreativePacket packet = new StopPathPacket();
			for (ServerPlayerEntity player : EntityArgument.getPlayers(x, "players"))
				CMDCam.NETWORK.sendToClient(packet, player);
			return 0;
		}))).then(Commands.literal("list").executes((x) -> {
			Collection<String> names = CMDCamServer.getSavedPaths(x.getSource().getWorld());
			String output = "There are " + names.size() + " path(s) in total. ";
			for (String key : names) {
				output += key + ", ";
			}
			x.getSource().sendFeedback(new StringTextComponent(output), true);
			return 0;
		})).then(Commands.literal("clear").executes((x) -> {
			CMDCamServer.clearPaths(x.getSource().getWorld());
			x.getSource().sendFeedback(new StringTextComponent("Removed all existing paths (in this world)!"), true);
			return 0;
		})).then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.string()).executes((x) -> {
			String path = StringArgumentType.getString(x, "name");
			if (CMDCamServer.removePath(x.getSource().getWorld(), path))
				x.getSource().sendFeedback(new StringTextComponent("Path '" + path + "' has been removed!"), true);
			else
				x.getSource().sendFeedback(new StringTextComponent("Path '" + path + "' could not be found!"), true);
			return 0;
		}))));
	}
}