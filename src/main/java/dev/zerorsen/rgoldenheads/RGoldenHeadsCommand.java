package dev.zerorsen.rgoldenheads;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** /rgoldenheads reload | give &lt;player|*&gt; &lt;item&gt; [amount] */
public final class RGoldenHeadsCommand implements BasicCommand {

    private static final String RELOAD_PERMISSION = "rgoldenheads.admin.reload";
    private static final String GIVE_PERMISSION = "rgoldenheads.admin.give";
    private static final List<String> EVERYONE = List.of("*", "@a");
    private static final List<String> AMOUNT_HINTS = List.of("1", "8", "16", "64");

    private final RGoldenHeadsPlugin plugin;
    private final ConsumableRegistry registry;
    private final ItemGiver giver;
    private final Messages messages;

    public RGoldenHeadsCommand(RGoldenHeadsPlugin plugin, ConsumableRegistry registry,
                               ItemGiver giver, Messages messages) {
        this.plugin = plugin;
        this.registry = registry;
        this.giver = giver;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        String subcommand = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);

        switch (subcommand) {
            case "reload" -> reload(sender);
            case "give" -> give(sender, args);
            default -> messages.send(sender, messages.usage("rgoldenheads"));
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            messages.send(sender, messages.noPermission());
            return;
        }
        messages.send(sender, plugin.reloadAll() ? messages.reloadSuccess() : messages.reloadFailed());
    }

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission(GIVE_PERMISSION)) {
            messages.send(sender, messages.noPermission());
            return;
        }
        if (args.length < 3) {
            messages.send(sender, messages.usage("rgoldenheads"));
            return;
        }

        ConsumableSpec spec = registry.byId(args[2].toLowerCase(Locale.ROOT));
        if (spec == null) {
            messages.send(sender, messages.unknownItem(args[2], String.join(", ", registry.ids())));
            return;
        }

        int max = ItemGiver.maxAmount(spec);
        int amount = spec.amount();
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                amount = -1;
            }
            if (amount < 1 || amount > max) {
                messages.send(sender, messages.invalidAmount(max));
                return;
            }
        }

        if (EVERYONE.contains(args[1])) {
            // Copied: the live collection may change while gives are being scheduled
            List<Player> online = List.copyOf(plugin.getServer().getOnlinePlayers());
            if (online.isEmpty()) {
                messages.send(sender, messages.noPlayersOnline());
                return;
            }
            int given = 0;
            for (Player player : online) {
                if (giver.give(player, spec, amount)) {
                    given++;
                }
            }
            messages.send(sender, messages.givenToAll(amount, spec.displayName(), given));
            return;
        }

        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null || !giver.give(target, spec, amount)) {
            messages.send(sender, messages.playerNotFound(args[1]));
            return;
        }
        messages.send(sender, messages.givenToPlayer(amount, spec.displayName(), target.getName()));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);

        if (args.length <= 1) {
            List<String> subcommands = new ArrayList<>();
            if (sender.hasPermission(RELOAD_PERMISSION)) {
                subcommands.add("reload");
            }
            if (sender.hasPermission(GIVE_PERMISSION)) {
                subcommands.add("give");
            }
            return startingWith(subcommands, prefix);
        }
        if (!args[0].equalsIgnoreCase("give") || !sender.hasPermission(GIVE_PERMISSION)) {
            return Collections.emptyList();
        }

        return switch (args.length) {
            case 2 -> {
                List<String> targets = new ArrayList<>(EVERYONE);
                plugin.getServer().getOnlinePlayers().forEach(player -> targets.add(player.getName()));
                yield startingWith(targets, prefix);
            }
            case 3 -> startingWith(registry.ids(), prefix);
            case 4 -> startingWith(AMOUNT_HINTS, prefix);
            default -> Collections.emptyList();
        };
    }

    // Overriding canUse rather than permission(): the root command must be visible to anyone
    // holding either subcommand's node, and each subcommand re-checks its own node above.
    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission(RELOAD_PERMISSION) || sender.hasPermission(GIVE_PERMISSION);
    }

    private static List<String> startingWith(Collection<String> options, String prefix) {
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
