package thaw.core;

import thaw.gui.ConfigWindow;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.JScrollPane;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * PluginConfigPanel. Creates and manages the panel containing all the things to configure
 *  the list of plugins.
 */
public class PluginConfigPanel implements Observer, ActionListener {
	private Core core;

	private JPanel mainPanel;
	private JPanel pluginConfigPanel;

	private Vector<JCheckBox> pluginCheckBoxes = null;

	public PluginConfigPanel(final ConfigWindow configWindow, final Core core) {
		this.core = core;

		pluginConfigPanel = new JPanel();
		pluginConfigPanel.setLayout(new GridLayout(PluginManager.getKnownPlugins().length, 1));
		configWindow.addObserver(this);
		//refreshList();

		mainPanel = new JPanel(new GridLayout(1, 1));
		
		JScrollPane scroll = new JScrollPane(pluginConfigPanel);
		scroll.getVerticalScrollBar().setUnitIncrement(10);
		mainPanel.add(scroll);
	}


	public JPanel getPanel() {
		return mainPanel;
	}

	/**
	 * In fact, it's not used here, because config is immediatly updated when
	 * user change something.
	 */
	public void update(final Observable o, final Object arg) {
		if(arg == null) // Warns us window is now visible
			refreshList();
	}


	/**
	 * We regenerate each time all the checkboxes<br/>
	 * Okay, it's dirty, but it should work
	 */
	public void refreshList() {
		if (pluginCheckBoxes != null) {
			for(JCheckBox checkBox : pluginCheckBoxes) {
				pluginConfigPanel.remove(checkBox);
			}
		}

		PluginManager pluginManager = core.getPluginManager();

		pluginCheckBoxes = new Vector<JCheckBox>();

		String[] knownPlugins = PluginManager.getKnownPlugins();

		for(String knownPlugin : knownPlugins) {
			JCheckBox c = new JCheckBox(knownPlugin.replaceFirst("thaw.plugins.", ""));
			c.addActionListener(this);
			c.setSelected(false);
			pluginCheckBoxes.add(c);
			pluginConfigPanel.add(c);
		}

		LinkedHashMap<String,Plugin> loadedPlugins = pluginManager.getPlugins();
		Collection<Plugin> loadedPluginsValues = loadedPlugins.values();

		for(Plugin plugin : loadedPluginsValues) {
			for(JCheckBox c : pluginCheckBoxes) {
				if (c.getText().equals(plugin.getClass().getName().replaceFirst("thaw.plugins.", ""))) {
					c.setSelected(true);
					c.setText(c.getText()+" ("+plugin.getNameForUser()+")");
				}
			}
		}
	}


	/**
	 * Return the class name contained in an option name from the list.
	 */
	protected String getClassName(final JCheckBox checkBox) {
		final String[] part = checkBox.getText().split(" ");
		return "thaw.plugins."+part[0];
	}

	public void actionPerformed(final ActionEvent e) {

		if (e.getSource() instanceof JCheckBox) {
			boolean load;
			JCheckBox c = (JCheckBox)e.getSource();

			load = c.isSelected();

			if (load) {
				if (core.getPluginManager().loadPlugin(getClassName(c)) == null
				    || !core.getPluginManager().runPlugin(getClassName(c)))
					load = false;
				else
					core.getConfig().addPlugin(getClassName(c));
			}

			if (!load) {
				core.getPluginManager().stopPlugin(getClassName(c));
				core.getPluginManager().unloadPlugin(getClassName(c));
				core.getConfig().removePlugin(getClassName(c));
			}

			refreshList();
		}
	}
}
