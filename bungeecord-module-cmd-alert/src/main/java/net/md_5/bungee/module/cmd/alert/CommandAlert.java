package net.md_5.bungee.module.cmd.alert;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class CommandAlert extends Command {

	public CommandAlert() {
		super("alert", "bungeecord.command.alert");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if ( args.length == 0 ) {
			sender.sendMessage(TextComponent.fromLegacyText(ProxyServer.getInstance().getTranslation("message_needed")));
		} else {
			StringBuilder builder = new StringBuilder();
			if (args[0].startsWith("&h")) {
				// Remove &h
				args[0] = args[0].substring(2, args[0].length());
			} else {
				builder.append(ProxyServer.getInstance().getTranslation("alert"));
			}

			for (String s : args) {
				builder.append(ChatColor.translateAlternateColorCodes('&', s));
				builder.append(" ");
			}

			String message = this.formatText(builder.toString());
			TextComponent text = new TextComponent(TextComponent.fromLegacyText(message));
			TextComponent legacy = new TextComponent(TextComponent.fromLegacyText(this.toLegacyColors(message)));

			ProxyServer.getInstance().getPlayers().forEach(p -> p.sendMessage(p.getPendingConnection().getVersion()>734? text : legacy));
			ProxyServer.getInstance().getConsole().sendMessage(legacy);
		}
	}
	
	public String toLegacyColors(String text) {
		char[] colors = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
		String[] elements = text.split("§x");
		StringBuilder sb = new StringBuilder(elements[0]);
		for(int i = 1; i<elements.length; i++) {
			if(!elements[i].matches("(§[0-9a-fA-F]){6}.*")) {
				sb.append("§x");
				sb.append(elements[i]);
				continue;
			}
			Color origin = getColor("§x"+elements[i].substring(0, 12));
			char finalChar = 'f';
			int min = Integer.MAX_VALUE;
			for(char ch : colors) {
				Color c = ChatColor.getByChar(ch).getColor();
				int length = (int) (Math.pow(origin.getRed() - c.getRed(), 2) + Math.pow(origin.getGreen() - c.getGreen(), 2) + Math.pow(origin.getBlue() - c.getBlue(), 2));
				if(length < min) {
					min = length;
					finalChar = ch;
				}
			}
			sb.append('§');
			sb.append(finalChar);
			sb.append(elements[i].substring(12));
		}
		return sb.toString();
	}
	
	public static int OBFUSCATED = 1;
	public static int BOLD = 2;
	public static int STRIKETHROUGH = 4;
	public static int UNDERLINED = 8;
	public static int ITALIC = 16;
	
	public String gradient(String text, Color start, Color end, int format) {
		double red = start.getRed();
		double green = start.getGreen();
		double blue = start.getBlue();
		boolean obfuscated = format == (format|OBFUSCATED);
		boolean bold = format == (format|BOLD);
		boolean strike = format == (format|STRIKETHROUGH);
		boolean underline = format == (format|UNDERLINED);
		boolean italic = format == (format|ITALIC);
		Color layer = null;
		StringBuilder sb = new StringBuilder(toColor(start.getRed(), start.getGreen(), start.getBlue()));
		int length = text.length();
		Pattern pattern = Pattern.compile("§[\\da-fk-orx]", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(text);
		while (matcher.find()) length -= 2;
		
		if(text.length() > 0) {
			double r = (end.getRed() - red)/(length);
			double g = (end.getGreen() - green)/(length);
			double b = (end.getBlue() - blue)/(length);
			red -= r;
			green -= g;
			blue -= b;
			for(int i=0; i<text.length(); i++) {
				if(text.charAt(i) == '§' && text.length() > i+1) {
					if(pattern.matcher(text.substring(i, i+2)).matches()) {
						char ch = Character.toLowerCase(text.charAt(i+1));
						switch (ch) {
							case 'k': {
								obfuscated = true;
								break;
							}
							case 'l': {
								bold = true;
								break;
							}
							case 'm': {
								strike = true;
								break;
							}
							case 'n': {
								underline = true;
								break;
							}
							case 'o': {
								italic = true;
								break;
							}
							case 'r': {
								obfuscated = false;
								bold = false;
								strike = false;
								underline = false;
								italic = false;
								layer = null;
								break;
							}
							case 'x': {
								if(i+7 < text.length()) {
									try {
										layer = getColor(text.substring(i, i+8));
										i += 6;
									} catch (IllegalArgumentException e) {e.printStackTrace();}
								}
								break;
							}
							default: {
								layer = ChatColor.getByChar(ch).getColor();
								break;
							}
						}
						i++;
						continue;
					}
				}
				if(layer != null) sb.append(toColor((int)Math.round((layer.getRed()+(red+=r))/2), (int)Math.round((layer.getGreen()+(green+=g))/2), (int)Math.round((layer.getBlue()+(blue+=b))/2)));
				else sb.append(toColor((int)Math.round(red+=r), (int)Math.round(green+=g), (int)Math.round(blue+=b)));
				if(obfuscated) sb.append("§k");
				if(bold) sb.append("§l");
				if(strike) sb.append("§m");
				if(underline) sb.append("§n");
				if(italic) sb.append("§o");
				sb.append(text.charAt(i));
			}
		}
		return sb.toString();
	}
	
	public String toColor(int red, int green, int blue) {
		StringBuilder sb = new StringBuilder("§x");
		String r = Integer.toHexString(red);
		String g = Integer.toHexString(green);
		String b = Integer.toHexString(blue);
		sb.append('§');
		sb.append(r.length()==1? '0' : r.charAt(0));
		sb.append('§');
		sb.append(r.charAt(r.length()-1));
		sb.append('§');
		sb.append(g.length()==1? '0' : g.charAt(0));
		sb.append('§');
		sb.append(g.charAt(g.length()-1));
		sb.append('§');
		sb.append(b.length()==1? '0' : b.charAt(0));
		sb.append('§');
		sb.append(b.charAt(b.length()-1));
		return sb.toString();
	}
	
	public String translateAlternateRGBFormats(String text) {
		String[] arr = text.split("(?=#[0-9a-fA-F]{6})");
		if(Pattern.compile("#[0-9a-fA-F]{6}.*").matcher(arr[0]).matches()) {
			String[] array = new String[arr.length+1];
			array[0] = "";
			for (int i = 0; i < arr.length; i++) {
				array[i+1] = arr[i];
			}
			arr = array;
		}
		StringBuilder sb = new StringBuilder(arr[0]);
		for(int i = 1; i < arr.length; i++) {
			if(arr[i-1].isEmpty() || arr[i-1].charAt(arr[i-1].length()-1) != '\\') {
				sb.append("§x");
				for(int j = 1; j <= 6; j++) {
					sb.append('§');
					sb.append(arr[i].charAt(j));
				}
				arr[i] = arr[i].substring(7);
			}
			sb.append(arr[i]);
		}
		return sb.toString();
	}
	
	public String formatText(String text) {
		text = translateAlternateRGBFormats(text);
		String[] arr = text.split("(?=<§x(§[\\da-fA-F]){6}>)");
		if(Pattern.compile("<§x(§[0-9a-fA-F]){6}>.*").matcher(arr[0]).matches()) {
			String[] array = new String[arr.length+1];
			array[0] = "";
			for (int i = 0; i < arr.length; i++) {
				array[i+1] = arr[i];
			}
			arr = array;
		}
		StringBuilder sb = new StringBuilder(arr[0]);
		for(int i = 1; i < arr.length; i++) {
			Matcher match = Pattern.compile("</§x(§[0-9a-fA-F]){6}>.*").matcher(arr[i]);
			if(match.find()) {
				int index = match.start()+2;
				Color start = getColor(arr[i].substring(1, 15));
				Color end = getColor(arr[i].substring(index, index+14));
				String format = this.getLegacyFormat(sb.toString());
				int form = 0;
				if(format.indexOf('k') > -1) form += OBFUSCATED;
				if(format.indexOf('l') > -1) form += BOLD;
				if(format.indexOf('m') > -1) form += STRIKETHROUGH;
				if(format.indexOf('n') > -1) form += UNDERLINED;
				if(format.indexOf('o') > -1) form += ITALIC;
				sb.append(gradient(arr[i].substring(16, index-2), start, end, form));
				sb.append(arr[i].substring(index+15));
			} else {
				sb.append(arr[i]);
			}
		}
		return sb.toString();
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
	
	public Color getColor(String color) {
		if(Pattern.matches("§x(§[a-fA-F0-9]){6}", color)) {
			return new Color(Integer.valueOf(color.replace("§", "").substring(1), 16));
		}
		throw new IllegalArgumentException(color+" not matches regex §x(§[a-fA-F0-9]){6}");
	}
	
}