package net.md_5.bungee.module.cmd.server;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

/**
 * Command to list and switch a player between available servers.
 */
public class CommandServer extends Command implements TabExecutor {

	public CommandServer() {
		super( "server", "bungeecord.command.server" );
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		Map<String, ServerInfo> servers = ProxyServer.getInstance().getServersCopy();
		if(args.length == 0) {
			if(sender instanceof ProxiedPlayer) {
				sender.sendMessage(TextComponent.fromLegacyText(ProxyServer.getInstance().getTranslation("current_server", ((ProxiedPlayer)sender).getServer().getInfo().getName())));
			}

			TextComponent text = new TextComponent(TextComponent.fromLegacyText(ProxyServer.getInstance().getTranslation("server_list")));
			String color = this.getLegacyFormat(ProxyServer.getInstance().getTranslation("server_list"));
			String separator = ProxyServer.getInstance().getTranslation("command_server_separator");
			
			for(ServerInfo server : servers.values().stream().sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName())).toArray(ServerInfo[]::new)) {
				if(server.canAccess(sender)) {
					String format = ProxyServer.getInstance().getTranslation("command_server_list_format", server.getName(), server.getPlayers().size());
					TextComponent extra = this.formatServer(server, color+format);
					text.addExtra(extra);
					color = this.getLegacyFormat(color+format);
					text.addExtra(new TextComponent(TextComponent.fromLegacyText(color+separator)));
					color = this.getLegacyFormat(color+separator);
				}
			}
			sender.sendMessage(text);
		} else {
			if(!(sender instanceof ProxiedPlayer)) {
				return;
			}
			ProxiedPlayer player = (ProxiedPlayer) sender;

			ServerInfo server = ProxyServer.getInstance().getServerInfo(args[0]);
			if (server == null) {
				player.sendMessage(TextComponent.fromLegacyText(ProxyServer.getInstance().getTranslation("no_server")));
			} else if(!server.canAccess(player)) {
				player.sendMessage(TextComponent.fromLegacyText(ProxyServer.getInstance().getTranslation("no_server_permission")));
			} else {
				player.connect(server, ServerConnectEvent.Reason.COMMAND);
			}
		}
	}

	public Iterable<String> onTabComplete(final CommandSender sender, final String[] args) {
		final String match = args.length>0? args[0].toLowerCase(Locale.ROOT) : "";
		return ProxyServer.getInstance().getServersCopy().values().stream().filter((server) -> {
			return (server.getName().toLowerCase(Locale.ROOT).startsWith(match) && server.canAccess(sender));
		}).map(ServerInfo::getName).collect(Collectors.toCollection(ArrayList::new));
	}
	
	public TextComponent formatServer(ServerInfo info) {
		return formatServer(info, info.getName());
	}
	
	@SuppressWarnings("deprecation")
	public TextComponent formatServer(ServerInfo info, String display) {
		TextComponent text = new TextComponent(TextComponent.fromLegacyText(display));
		Text hover = new Text(ProxyServer.getInstance().getTranslation("server_hover", /*0*/info.getName(), /*1*/info.getPlayers().size(),
				/*2*/trimHoverString(String.join("§7, §b", info.getPlayers().stream().map(ProxiedPlayer::getName).toArray(String[]::new)), 30, 15),
				/*3*/info.getPermission(), /*4*/formatBoolean(info.isRestricted()), /*5*/info.getMotd(),
				/*6*/info.getAddress().getAddress().getHostAddress(), /*7*/info.getAddress().getPort()));
		text.setInsertion(info.getName());
		text.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
				ProxyServer.getInstance().getTranslation("server_command_suggestion", /*0*/info.getName())));
		text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
		return text;
	}
	
	public String formatBoolean(boolean val) {
		return val? "§atrue" : "§4false";
	}
	
	public String getLegacyFormat(String text) {
		String[] arr = text.toLowerCase().split("§(?=[a-f0-9k-or])");
		String str = "";
		boolean colored = false;
		boolean[] formats = new boolean[] {false, false, false, false, false, false, false};
		for(int i=arr.length-1; i>0; i--) {
			char ch = arr[i].charAt(0);
			if(ch == 'r') break;
			if(ch < 103) {
				if(!colored) {
					colored = true;
					str = "§"+ch+str;
				} else {
					continue;
				}
			}else {
				if(!formats[ch-107]) {
					str = "§"+ch+str;
					formats[ch-107] = true;
				}
			}
		}
		return str;
	}
	
	public BaseComponent getFormat(String text) {
		BaseComponent[] txt = TextComponent.fromLegacyText(this.getLegacyFormat(text));
		return txt.length>0? txt[txt.length-1] : new TextComponent();
	}
	
	public String trimHoverString(String text, int minLength, int tolerance) {
		return trimHoverString(text, minLength, tolerance, "\n");
	}
	
	public String trimHoverString(String text, int minLength, int tolerance, String separator) {
		if(minLength < 1 || tolerance < 0) return text;
		StringBuilder sb = new StringBuilder();
		int maxLength = minLength + tolerance;
		boolean first = true;
		outer:
		while(text.length() > minLength) {
			int i = minLength;
			for(; i < maxLength; i++) {
				if(i >= text.length()-1) break outer;
				if(text.charAt(i) == ' ') break;
			}
			if(!first) sb.append(separator);
			first = false;
			sb.append(text.substring(0, i));
			text = text.substring(i);
		}
		if(!first) sb.append(separator);
		sb.append(text);
		return sb.toString();
	}
	
}