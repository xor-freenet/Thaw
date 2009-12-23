package thaw.gui.ActionHandlers;

import thaw.core.Core;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class MainConnect implements ActionListener {
	final protected Core core;

	public MainConnect(Core core)
	{
		this.core = core;
	}


	public void actionPerformed(final ActionEvent e) {
		core.reconnect(false);
	}
}
