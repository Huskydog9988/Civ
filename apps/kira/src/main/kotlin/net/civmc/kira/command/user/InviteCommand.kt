package net.civmc.kira.command.user

import com.github.maxopoly.kira.KiraMain
import com.github.maxopoly.kira.command.model.top.InputSupplier
import com.github.maxopoly.kira.user.UserManager
import net.civmc.kira.command.Command
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.apache.logging.log4j.Logger

class InviteCommand(logger: Logger, userManager: UserManager): Command(logger, userManager) {

    override val name = "invite"
    override val requireUser = true
    override val requiredPermission = "isauth"

    override fun dispatchCommand(event: SlashCommandEvent, sender: InputSupplier) {
        event.reply(KiraMain.getInstance().jda.getInviteUrl())
    }

    override fun getCommandData() = CommandData("invite", "Get an invite link for Kira")
}