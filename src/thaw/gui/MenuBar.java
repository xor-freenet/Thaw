package thaw.gui;

import thaw.core.Core;
import thaw.core.I18n;
import thaw.core.Logger;
import thaw.gui.ActionHandlers.MainMenuAbout;
import thaw.gui.ActionHandlers.MainOptions;
import thaw.gui.ActionHandlers.MainQuit;
import thaw.gui.ActionHandlers.MainMenuReconnection;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Vector;

public class MenuBar implements java.awt.event.ActionListener {
	private final JMenuBar menuBar;
	private JMenu fileMenu;

	private Vector<JMenuItem> fileMenuList = null;

	private final JMenuItem reconnectionFileMenuItem;
	private final JMenuItem optionsFileMenuItem;
	private final JMenuItem quitFileMenuItem;

	private final Vector<JMenu> menuList;
	private final JMenuItem aboutHelpMenuItem;

	public MenuBar(Core core) {
		menuBar = new JMenuBar();
		menuList = new Vector<JMenu>();


		fileMenu = new JMenu(I18n.getMessage("thaw.menu.file"));
		fileMenuList = new Vector<JMenuItem>();

		reconnectionFileMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.reconnect"),
							 IconBox.minReconnectAction);
		optionsFileMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.options"),
						    IconBox.minSettings);
		quitFileMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.quit"),
						 IconBox.minQuitAction);
		
		fileMenuList.add(reconnectionFileMenuItem);
		fileMenuList.add(optionsFileMenuItem);
		fileMenuList.add(quitFileMenuItem);

		reconnectionFileMenuItem.addActionListener(new MainMenuReconnection(core));
		optionsFileMenuItem.addActionListener(new MainOptions(core));
		quitFileMenuItem.addActionListener(new MainQuit(core));

		fileMenu.add(reconnectionFileMenuItem);
		fileMenu.add(optionsFileMenuItem);
		fileMenu.add(quitFileMenuItem);

		menuBar.add(fileMenu);
		menuList.add(fileMenu);

		JMenu helpMenu = new JMenu(I18n.getMessage("thaw.menu.help"));

		aboutHelpMenuItem = new JMenuItem(I18n.getMessage("thaw.menu.item.about"),
						  IconBox.minHelp);
		aboutHelpMenuItem.addActionListener(new MainMenuAbout());

		helpMenu.add(aboutHelpMenuItem);

		//menuBar.add(Box.createHorizontalGlue());
		menuBar.add(helpMenu);
		menuList.add(helpMenu);
	}

	public JMenuBar getMenuBar() {
		return menuBar;
	}


		/**
	 * Used by plugins to add their own menu.
	 */
	public void insertMenuAt(JMenu menu, int position) {
		menuList.add(position, menu);
		refreshMenuBar();
	}

	public void removeMenu(JMenu menu) {
		menuList.remove(menu);
		refreshMenuBar();
	}

	protected void refreshMenuBar() {
		Logger.info(this, "Display "+
			    Integer.toString(menuList.size())+
			    " menus in the main window");

		/* rebuilding menubar */
		menuBar.removeAll();

		for(JMenu menu : menuList) {
			menuBar.add(menu);
		}

		menuBar.validate();
		//mainWindow.validate(); /* no getContentPane() ! else it won't work ! */
	}


	protected void refreshFileMenu() {
		/* rebuilding menubar */
		JMenu m = new JMenu(I18n.getMessage("thaw.menu.file"));

		for(JMenuItem menuItem : fileMenuList) {
			m.add(menuItem);
		}

		menuList.remove(fileMenu);
		fileMenu = m;
		menuList.add(0, fileMenu);

		refreshMenuBar();
	}


	/**
	 * Called when an element from the menu is called.
	 */
	public void actionPerformed(final ActionEvent e) {

	}


	/**
	 * Used by plugins to add their own menu / menuItem to the menu 'file'.
	 */
	public void insertInFileMenuAt(JMenuItem newItem, int position) {
		fileMenuList.add(position, newItem);
		refreshFileMenu();
	}

	public void removeFromFileMenu(JMenuItem item) {
		fileMenuList.remove(item);
		refreshFileMenu();
	}
}
