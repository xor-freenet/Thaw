package thaw.gui.ActionHandlers;

import thaw.core.Core;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class MainQuit implements ActionListener {
	final protected Core core;

	public MainQuit(Core core)
	{
		this.core = core;
	}


	public void actionPerformed(final ActionEvent e) {
		core.getMainWindow().endOfTheWorld();
	}
}