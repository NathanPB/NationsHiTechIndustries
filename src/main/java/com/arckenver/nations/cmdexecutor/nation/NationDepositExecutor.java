package com.arckenver.nations.cmdexecutor.nation;

import java.math.BigDecimal;
import java.util.Optional;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Text.Builder;
import org.spongepowered.api.text.format.TextColors;

import com.arckenver.nations.DataHandler;
import com.arckenver.nations.LanguageHandler;
import com.arckenver.nations.NationsPlugin;
import com.arckenver.nations.Utils;
import com.arckenver.nations.object.Nation;

public class NationDepositExecutor implements CommandExecutor
{
	public static void create(CommandSpec.Builder cmd) {
		cmd.child(CommandSpec.builder()
				.description(Text.of(""))
				.permission("nations.command.nation.deposit")
				.arguments(GenericArguments.optional(GenericArguments.doubleNum(Text.of("amount"))))
				.executor(new NationDepositExecutor())
				.build(), "deposit", "give");
	}

	public CommandResult execute(CommandSource src, CommandContext ctx) throws CommandException
	{
		if (src instanceof Player)
		{
			if (!ctx.<Double>getOne("amount").isPresent())
			{
				src.sendMessage(Text.of(TextColors.YELLOW, "/n deposit <amount>\n/n withdraw <amount>"));
				return CommandResult.success();
			}
			
			Player player = (Player) src;
			Nation nation = DataHandler.getNationOfPlayer(player.getUniqueId());
			if (nation == null)
			{
				src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_NONATION));
				return CommandResult.success();
			}
			
			if (NationsPlugin.getEcoService() == null)
			{
				src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_NOECO));
				return CommandResult.success();
			}
			Optional<UniqueAccount> optAccount = NationsPlugin.getEcoService().getOrCreateAccount(player.getUniqueId());
			if (!optAccount.isPresent())
			{
				src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_ECONOACCOUNT));
				return CommandResult.success();
			}
			Optional<Account> optNationAccount = NationsPlugin.getEcoService().getOrCreateAccount(nation.getUUID().toString());
			if (!optNationAccount.isPresent())
			{
				src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_ECONONATION));
				return CommandResult.success();
			}
			BigDecimal amount = BigDecimal.valueOf(ctx.<Double>getOne("amount").get());
			TransactionResult result = optAccount.get().transfer(optNationAccount.get(), NationsPlugin.getEcoService().getDefaultCurrency(), amount, NationsPlugin.getCause());
			if (result.getResult() == ResultType.ACCOUNT_NO_FUNDS)
			{
				src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_NOENOUGHMONEY));
				return CommandResult.success();
			}
			else if (result.getResult() != ResultType.SUCCESS)
			{
				src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_ECOTRANSACTION));
				return CommandResult.success();
			}
			
			String[] s1 = LanguageHandler.SUCCESS_DEPOSIT.split("\\{AMOUNT\\}");
			Builder builder = Text.builder();
			if (s1[0].indexOf("{BALANCE}") >= 0)
			{
				String[] splited0 = s1[0].split("\\{BALANCE\\}");
				builder
				.append(Text.of(TextColors.GREEN, (splited0.length > 0) ? splited0[0] : ""))
				.append(Utils.formatPrice(TextColors.GREEN, optNationAccount.get().getBalance(NationsPlugin.getEcoService().getDefaultCurrency())))
				.append(Text.of(TextColors.GREEN, (splited0.length > 1) ? splited0[1] : ""));
			}
			else
			{
				builder.append(Text.of(TextColors.GREEN, s1[0]));
			}
			builder.append(Utils.formatPrice(TextColors.GREEN, amount));
			if (s1[1].indexOf("{BALANCE}") >= 0)
			{
				String[] splited1 = s1[1].split("\\{BALANCE\\}");
				builder
				.append(Text.of(TextColors.GREEN, (splited1.length > 0) ? splited1[0] : ""))
				.append(Utils.formatPrice(TextColors.GREEN, optNationAccount.get().getBalance(NationsPlugin.getEcoService().getDefaultCurrency())))
				.append(Text.of(TextColors.GREEN, (splited1.length > 1) ? splited1[1] : ""));
			}
			else
			{
				builder.append(Text.of(TextColors.GREEN, s1[1]));
			}
			src.sendMessage(builder.build());
		}
		else
		{
			src.sendMessage(Text.of(TextColors.RED, LanguageHandler.ERROR_NOPLAYER));
		}
		return CommandResult.success();
	}
}
