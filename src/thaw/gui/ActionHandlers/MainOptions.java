package thaw.gui.ActionHandlers;

import thaw.core.Core;
import thaw.gui.ConfigWindow;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class MainOptions implements ActionListener {
	final protected Core core;

	public MainOptions(Core core)
	{
		this.core = core;
	}


	public void actionPerformed(final ActionEvent e) {
		core.getConfigWindow().setVisible(true);
	}
}