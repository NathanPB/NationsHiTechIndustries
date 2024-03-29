package com.arckenver.nations.cmdexecutor.nationadmin;

import com.arckenver.nations.object.*;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.arckenver.nations.ConfigHandler;
import com.arckenver.nations.DataHandler;
import com.arckenver.nations.LanguageHandler;
import com.flowpowered.math.vector.Vector2i;

import java.util.stream.Collectors;

public class NationadminUnclaimExecutor implements CommandExecutor
{
	public static void create(CommandSpec.Builder cmd) {
		cmd.child(CommandSpec.builder()
				.description(Text.of(""))
				.permission("nations.command.nationadmin.unclaim")
				.arguments(GenericArguments.optional(GenericArguments.string(Text.of("nation"))))
				.executor(new NationadminUnclaimExecutor())
				.build(), "unclaim");
	}

	public CommandResult execute(CommandSource src, CommandContext ctx) throws CommandException
	{
		if (src instanceof Player)
		{
			if (!ctx.<String>getOne("nation").isPresent())
			{
				src.sendMessage(Text.of(TextColors.YELLOW, "/na unclaim <nation>"));
				return CommandResult.success();
			}
			Player player = (Player) src;
			String nationName = ctx.<String>getOne("nation").get();
			Nation nation = DataHandler.getNation(nationName);
			if (nation == null)
			{
				src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_BADNATIONNAME));
				return CommandResult.success();
			}
			Point a = DataHandler.getFirstPoint(player.getUniqueId());
			Point b = DataHandler.getSecondPoint(player.getUniqueId());
			if (a == null || b == null)
			{
				src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_NEEDAXESELECT));
				return CommandResult.success();
			}
			if (!ConfigHandler.getNode("worlds").getNode(a.getWorld().getName()).getNode("enabled").getBoolean())
			{
				src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_PLUGINDISABLEDINWORLD));
				return CommandResult.success();
			}
			Rect rect = new Rect(a, b);
			if (!nation.getRegion().intersects(rect))
			{
				src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_NEEDINTERSECT));
				return CommandResult.success();
			}
			for (Location<World> spawn : nation.getSpawns().stream().map(NationSpawn::getLocation).collect(Collectors.toList()))
			{
				if (rect.isInside(new Vector2i(spawn.getBlockX(), spawn.getBlockZ())))
				{
					src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_AREACONTAINSPAWN));
					return CommandResult.success();
				}
			}
			for (Zone zone : nation.getZones().values())
			{
				if (zone.getRect().intersects(rect))
				{
					src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_SELECTIONCONTAINZONE));
					return CommandResult.success();
				}
			}
			Region claimed = nation.getRegion().copy();
			claimed.removeRect(rect);

			nation.setRegion(claimed);
			DataHandler.addToWorldChunks(nation);
			DataHandler.saveNation(nation.getUUID());
			src.sendMessage(Text.of(TextColors.AQUA, LanguageHandler.SUCCESS_GENERAL));
		}
		else
		{
			src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_NOPLAYER));
		}
		return CommandResult.success();
	}
}
