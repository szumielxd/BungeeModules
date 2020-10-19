package net.md_5.bungee.module.cmd.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ComponentBuilder.FormatRetention;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

/**
 * Command to list all players connected to the proxy.
 */
public class CommandList extends Command implements TabExecutor {

	public CommandList() {
		super("glist", "bungeecord.command.list");
	}
	
	@Override
	public Iterable<String> onTabComplete(final CommandSender sender, String[] args) {
		ArrayList<String> list = new ArrayList<>();
		if(args.length == 1) {
			String prefix = args[0].substring(0, args[0].lastIndexOf(',')+1);
			args[0] = args[0].toLowerCase();
			list = ProxyServer.getInstance().getServersCopy().values().stream().filter(srv -> srv.canAccess(sender))
					.map(srv -> prefix+srv.getName()).filter(srv -> srv.toLowerCase().startsWith(args[0]))
					.collect(Collectors.toCollection(ArrayList::new));
			if("all".startsWith(args[0])) list.add("all");
		}
		return list;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		String str = ProxyServer.getInstance().getTranslation("total_players", ProxyServer.getInstance().getOnlineCount());
		sender.sendMessage(TextComponent.fromLegacyText(str));
		if(args.length == 0) {
			TextComponent text = new TextComponent();
			String separator = ProxyServer.getInstance().getTranslation("command_list_player_separator");
			String color = this.getLegacyFormat(separator);
			
			for(ServerInfo server : ProxyServer.getInstance().getServersCopy().values().stream()
					.sorted((a, b) -> -Integer.compare(a.getPlayers().size(), b.getPlayers().size()))
					.toArray(ServerInfo[]::new)) {
				if(!server.canAccess(sender)) continue;
				String format = ProxyServer.getInstance().getTranslation("command_list_server_format", server.getName(), server.getPlayers().size());
				TextComponent extra = this.formatServerList(server, color+format);
				text.addExtra(extra);
				color = this.getLegacyFormat(color+format);
				text.addExtra(new TextComponent(TextComponent.fromLegacyText(color+separator)));
				color = this.getLegacyFormat(color+separator);
			}
			text.getExtra().remove(text.getExtra().size()-1);
			sender.sendMessage(text);
		} else {
			ArrayList<ServerInfo> servers = new ArrayList<>();
			if(args[0].equalsIgnoreCase("all")) {
				servers = new ArrayList<>(ProxyServer.getInstance().getServersCopy().values());
			} else {
				for(String srv : args[0].split(",")) {
					ServerInfo info = ProxyServer.getInstance().getServerInfo(srv);
					if(info != null && info.canAccess(sender) && !servers.contains(info)) servers.add(info);
				}
			}
			servers.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName()));
			if(!servers.isEmpty()) for(ServerInfo server : servers) {
				this.sendServerInfo(sender, server);
			}
		}
	}
	
	private void sendServerInfo(CommandSender sender, ServerInfo server) {
		Collection<ProxiedPlayer> players = server.getPlayers().stream()
				.sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName()))
				.collect(Collectors.toCollection(ArrayList::new));
		String separator = ProxyServer.getInstance().getTranslation("command_list_player_separator");
		String color = this.getLegacyFormat(separator);
		TextComponent list = new TextComponent();
		if(players.size() > 0) {
			for(ProxiedPlayer p : players) {
				TextComponent extra = this.formatPlayer(p);
				extra.copyFormatting(this.getFormat(color), FormatRetention.FORMATTING, false);
				list.addExtra(extra);
				list.addExtra(new TextComponent(TextComponent.fromLegacyText(separator)));
			}
			list.getExtra().remove(list.getExtra().size()-1);
		}
		String[] arr = ProxyServer.getInstance().getTranslation("command_list").replace("{1}", ""+players.size()).split("(?=\\{\\d+\\})");
		TextComponent text = new TextComponent(TextComponent.fromLegacyText(arr[0]));
		color = this.getLegacyFormat(arr[0]);
		for(int i=1; i<arr.length; i++) {
			TextComponent extra = null;
			if(arr[i].startsWith("{0}")) extra = this.formatServer(server);
			if(arr[i].startsWith("{2}")) extra = list.duplicate();
			if(extra != null) {
				extra.copyFormatting(this.getFormat(color), FormatRetention.FORMATTING, false);
				text.addExtra(extra);
			}
			TextComponent txt = new TextComponent(TextComponent.fromLegacyText(color+arr[i].substring(3)));
			color = this.getLegacyFormat(color+arr[i]);
			text.addExtra(txt);
		}
		sender.sendMessage(text);
		
	}
	
	@SuppressWarnings("deprecation")
	public TextComponent formatPlayer(ProxiedPlayer p) {
		TextComponent text = new TextComponent(p.getDisplayName());
		Text hover = new Text(ProxyServer.getInstance().getTranslation("user_hover", /*0*/p.getName(), /*1*/p.getDisplayName(), /*2*/p.getUniqueId(),
				/*3*/p.getAddress().getAddress().getHostAddress(), /*4*/p.getServer().getInfo().getName(),
				/*5*/p.getReconnectServer()==null? "default" : p.getReconnectServer(),
				/*6*/String.join("§7, §b", p.getGroups()), /*7*/String.join("§7, §b", p.getModList().keySet()),
				/*8*/this.getVersion(p.getPendingConnection().getVersion()), /*9*/p.getViewDistance(),
				/*10*/p.getLocale(), /*11*/p.getChatMode()));
		text.setInsertion(p.getName());
		text.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
				ProxyServer.getInstance().getTranslation("user_command_suggestion", /*0*/p.getName(), /*1*/p.getDisplayName(), /*2*/p.getServer().getInfo().getName())));
		text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
		return text;
	}
	
	@SuppressWarnings("deprecation")
	public TextComponent formatServer(ServerInfo info) {
		TextComponent text = new TextComponent(info.getName());
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
	
	public TextComponent formatServerList(ServerInfo info) {
		return this.formatServerList(info, info.getName());
	}
	
	@SuppressWarnings("deprecation")
	public TextComponent formatServerList(ServerInfo info, String display) {
		TextComponent text = new TextComponent(TextComponent.fromLegacyText(display));
		List<String> players = info.getPlayers().stream().map(ProxiedPlayer::getName)
				.sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toCollection(ArrayList::new));
		Text hover = new Text(ProxyServer.getInstance().getTranslation("server_info_hover", /*0*/info.getName(), /*1*/info.getPlayers().size(),
				/*2*/trimHoverString(String.join("§7, §b", players), 30, 15),
				/*3*/info.getPermission(), /*4*/formatBoolean(info.isRestricted()), /*5*/info.getMotd(),
				/*6*/info.getAddress().getAddress().getHostAddress(), /*7*/info.getAddress().getPort()));
		text.setInsertion(info.getName());
		text.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
				ProxyServer.getInstance().getTranslation("server_info_command_suggestion", /*0*/info.getName())));
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
	
	public String getVersion(int protocol) {
		switch (protocol) {
			case 753: return "1.16.3";
			case 751: return "1.16.2";
			case 736: return "1.16.1";
			case 735: return "1.16.0";
			case 578: return "1.15.2";
			case 575: return "1.15.1";
			case 573: return "1.15.0";
			case 554: return "1.14.4";
			case 490: return "1.14.3";
			case 485: return "1.14.2";
			case 480: return "1.14.1";
			case 477: return "1.14.0";
			case 404: return "1.13.2";
			case 401: return "1.13.1";
			case 393: return "1.13.0";
			case 340: return "1.12.2";
			case 338: return "1.12.1";
			case 335: return "1.12.0";
			case 316: return "1.11.1-1.11.2";
			case 315: return "1.11.0";
			case 210: return "1.10.x";
			case 110: return "1.9.3-1.9.4";
			case 109: return "1.9.2";
			case 108: return "1.9.1";
			case 107: return "1.9.0";
			case 47: return "1.8.x";
			default: return "UNKNOWN";
		}
	}
	
}