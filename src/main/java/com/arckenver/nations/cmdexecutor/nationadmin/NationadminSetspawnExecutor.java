package com.arckenver.nations.cmdexecutor.nationadmin;

import com.arckenver.nations.object.NationSpawn;
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
import com.arckenver.nations.cmdelement.NationNameElement;
import com.arckenver.nations.object.Nation;

public class NationadminSetspawnExecutor implements CommandExecutor
{
	public static void create(CommandSpec.Builder cmd) {
		cmd.child(CommandSpec.builder()
				.description(Text.of(""))
				.permission("nations.command.nationadmin.setspawn")
				.arguments(
						GenericArguments.optional(new NationNameElement(Text.of("nation"))),
						GenericArguments.optional(GenericArguments.string(Text.of("name"))))
				.executor(new NationadminSetspawnExecutor())
				.build(), "setspawn");
	}

	public CommandResult execute(CommandSource src, CommandContext ctx) throws CommandException
	{
		if (src instanceof Player)
		{
			Player player = (Player) src;
			if (!ctx.<String>getOne("nation").isPresent() || !ctx.<String>getOne("name").isPresent())
			{
				src.sendMessage(Text.of(TextColors.YELLOW, "/na setspawn <nation> <name>"));
				return CommandResult.success();
			}
			String nationName = ctx.<String>getOne("nation").get();
			Nation nation = DataHandler.getNation(nationName);
			if (nation == null)
			{
				src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_BADNATIONNAME));
				return CommandResult.success();
			}
			String spawnName = ctx.<String>getOne("name").get();
			
			Location<World> newSpawn = player.getLocation();

			if (nation.getRegion().isEmpty())
			{
				src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_NOAREACLAIMED));
				return CommandResult.success();
			}
			if (!nation.getRegion().isInside(newSpawn))
			{
				src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_BADSPAWNLOCATION));
				return CommandResult.success();
			}
			if (nation.getNumSpawns() + 1 > nation.getMaxSpawns() && !nation.getSpawns().stream().map(NationSpawn::getName).anyMatch(spawnName::equals))
			{
				src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_MAXSPAWNREACH
						.replaceAll("\\{MAX\\}", String.valueOf(nation.getMaxSpawns()))));
				return CommandResult.success();
			}
			if (!spawnName.matches("[\\p{Alnum}\\p{IsIdeographic}\\p{IsLetter}]{1,30}"))
			{
				src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_ALPHASPAWN
						.replaceAll("\\{MIN\\}", ConfigHandler.getNode("others", "minZoneNameLength").getString())
						.replaceAll("\\{MAX\\}", ConfigHandler.getNode("others", "maxZoneNameLength").getString())));
				return CommandResult.success();
			}
			nation.addSpawn(new NationSpawn(spawnName, newSpawn));
			DataHandler.saveNation(nation.getUUID());
			src.sendMessage(Text.of(TextColors.AQUA, LanguageHandler.SUCCESS_CHANGESPAWN));
		}
		else
		{
			src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_NOPLAYER));
		}
		return CommandResult.success();
	}
}
