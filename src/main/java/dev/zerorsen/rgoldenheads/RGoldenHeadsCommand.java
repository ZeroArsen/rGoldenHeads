package dev.zerorsen.rgoldenheads;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class RGoldenHeadsCommand implements BasicCommand {

    private static final String ADMIN_PERMISSION = "rgoldenheads.admin";
    private static final List<String> SUBCOMMANDS = List.of("reload");

    private final RGoldenHeadsPlugin plugin;
    private final Messages messages;

    public RGoldenHeadsCommand(RGoldenHeadsPlugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            messages.send(sender, plugin.reloadAll() ? messages.reloadSuccess() : messages.reloadFailed());
            return;
        }

        messages.send(sender, messages.usage("rgoldenheads"));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(sub -> sub.startsWith(prefix)).toList();
        }
        return Collections.emptyList();
    }

    @Override
    public String permission() {
        return ADMIN_PERMISSION;
    }
}
