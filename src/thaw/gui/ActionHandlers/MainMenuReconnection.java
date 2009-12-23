package thaw.gui.ActionHandlers;

import thaw.core.Core;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class MainMenuReconnection implements ActionListener {
	final protected Core core;

	public MainMenuReconnection(Core core)
	{
		this.core = core;
	}


	public void actionPerformed(final ActionEvent e) {
		if(!core.canDisconnect()) {
			if(!core.askDeconnectionConfirmation())
				return;
		}

		core.reconnect(false);
	}
}
