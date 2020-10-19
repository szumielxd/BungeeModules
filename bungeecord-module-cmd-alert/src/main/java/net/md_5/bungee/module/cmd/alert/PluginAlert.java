package net.md_5.bungee.module.cmd.alert;

import net.md_5.bungee.api.plugin.Plugin;

public class PluginAlert extends Plugin {

	@Override
	public void onEnable()
	{
		this.getProxy().getPluginManager().registerCommand(this, new CommandAlert());
		this.getProxy().getPluginManager().registerCommand(this, new CommandAlertRaw());
	}
}