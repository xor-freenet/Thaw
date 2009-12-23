package thaw.gui.ActionHandlers;

import thaw.core.Core;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class MainDisconnect implements ActionListener {
	final protected Core core;

	public MainDisconnect(Core core)
	{
		this.core = core;
	}


	public void actionPerformed(final ActionEvent e) {
		if(!core.canDisconnect()) {
			if(!core.askDeconnectionConfirmation())
				return;
		}

		core.getPluginManager().stopPlugins();
		core.disconnect();
		core.getPluginManager().loadAndRunPlugins();
	}
}