package com.github.maxopoly.Kira.listener;

import org.apache.logging.log4j.Logger;

import com.github.maxopoly.Kira.KiraMain;
import com.github.maxopoly.Kira.command.CommandHandler;
import com.github.maxopoly.Kira.command.DiscordCommandChannelSupplier;
import com.github.maxopoly.Kira.command.DiscordCommandPMSupplier;
import com.github.maxopoly.Kira.command.InputSupplier;
import com.github.maxopoly.Kira.group.GroupChat;
import com.github.maxopoly.Kira.group.GroupChatManager;
import com.github.maxopoly.Kira.user.KiraUser;
import com.github.maxopoly.Kira.user.UserManager;

import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class DiscordMessageListener extends ListenerAdapter {

	private CommandHandler cmdHandler;
	private Logger logger;
	private UserManager userManager;
	private long ownID;
	private final static String channelKeyword = "!kira ";

	public DiscordMessageListener(CommandHandler cmdHandler, Logger logger, UserManager userManager, long ownID) {
		this.cmdHandler = cmdHandler;
		this.logger = logger;
		this.ownID = ownID;
		this.userManager = userManager;
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.getAuthor().isBot() || event.getAuthor().getIdLong() == ownID) {
			return;
		}
		KiraUser user = userManager.getOrCreateUserByDiscordID(event.getAuthor().getIdLong());
		String content = event.getMessage().getContentRaw();
		if (event.isFromType(ChannelType.PRIVATE)) {
			logger.info(String.format("CHAT [PM] %s: %s", event.getAuthor().getName(),
					event.getMessage().getContentDisplay()));
			cmdHandler.handle(content, new DiscordCommandPMSupplier(user));
		} else {
			logger.info(
					String.format("CHAT [%s][%s] %s: %s", event.getGuild().getName(), event.getTextChannel().getName(),
							event.getMember().getEffectiveName(), event.getMessage().getContentDisplay()));
			if (content.startsWith(channelKeyword)) {
				InputSupplier supplier = new DiscordCommandChannelSupplier(user, event.getGuild().getIdLong(),
						event.getChannel().getIdLong());
				cmdHandler.handle(content.substring(channelKeyword.length()), supplier);
				return;
			}
			GroupChatManager chatMan = KiraMain.getInstance().getGroupChatManager();
			GroupChat chat = chatMan.getChatByChannelID(event.getChannel().getIdLong());
			if (chat != null) {
				String message = event.getMessage().getContentDisplay();
				message = sanitize(message);
				if (!message.equals("")) {
					KiraMain.getInstance().getMCRabbitGateway().sendGroupChatMessage(user, chat, message);
				}
				event.getMessage().delete().queue();
			}
		}
	}

	private String sanitize(String input) {
		String result = input.replace("\n", "");
		result = result.replace("\r", "");
		result = result.replace("\t", "");
		result = result.replace("§", "");
		result = result.trim();
		if (result.length() > 255) {
			result = result.substring(0, 255);
		}
		return result;
	}

}
