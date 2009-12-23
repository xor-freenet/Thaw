package thaw.gui;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import java.awt.event.WindowListener;

import thaw.core.*;


/**
 * MainWindow. This class create the main window.
 *
 * Main window is divided in three parts:
 *
 * <pre>
 * ------------------------------------
 * | MenuBar                          |
 * ------------------------------------
 * | ToolBar                          |
 * ------------------------------------
 * | Tab 1 | Tab 2 | Tab 3 |          |
 * |----------------------------------|
 * | Tab content                      |
 * |                                  |
 * |                                  |
 * |                                  |
 * |                                  |
 * ------------------------------------
 * | JLabel (status)                  |
 * ------------------------------------
 * </pre>
 *
 * @author <a href="mailto:jflesch@nerim.net">Jerome Flesch</a>
 */
public class MainWindow implements WindowListener,
				   java.util.Observer {

	public final static int DEFAULT_SIZE_X = 790;
	public final static int DEFAULT_SIZE_Y = 550;

	private final JFrame mainWindow;

	private MenuBar menuBar;
	private ToolBar toolBar;



	private final TabbedPane tabbedPane;
	private final JLabel statusBar;

	private final Core core; /* core is called back when exit() */



	/**
	 * Creates a new <code>MainWindow</code> instance, and so a new Swing window.
	 * @param core a <code>Core</code> value
	 */
	public MainWindow(final Core core) {
		this.core = core;

		mainWindow = new JFrame("Thaw");

		mainWindow.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		mainWindow.setVisible(false);

		try {
			mainWindow.setIconImage(IconBox.blueBunny.getImage());
		} catch(final Throwable e) {
			Logger.notice(this, "No icon");
		}

		// MENUS
		menuBar = new MenuBar(core);

		// TOOLBAR
		toolBar = new ToolBar(core);

		// TABBED PANE

		tabbedPane = new TabbedPane();

		// STATUS BAR

		statusBar = new JLabel();
		setStatus(null, null);
		statusBar.setSize(500, 30);


		mainWindow.getContentPane().setLayout(new BorderLayout(5,5));

		mainWindow.setJMenuBar(menuBar.getMenuBar());

		/* Toolbar adding: */
		mainWindow.getContentPane().add(toolBar.getToolBar(), BorderLayout.NORTH);
		changeButtonsInTheToolbar(this, null);
		mainWindow.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		mainWindow.getContentPane().add(statusBar, BorderLayout.SOUTH);

		mainWindow.setSize(MainWindow.DEFAULT_SIZE_X, MainWindow.DEFAULT_SIZE_Y);

		mainWindow.addWindowListener(this);

		core.getConnectionManager().addObserver(this);

		if (core.getConfig().getValue("mainWindowSizeX") != null
		    && core.getConfig().getValue("mainWindowSizeY") != null) {
			try {
				mainWindow.setSize(Integer.parseInt(core.getConfig().getValue("mainWindowSizeX")),
						   Integer.parseInt(core.getConfig().getValue("mainWindowSizeY")));
			} catch(NumberFormatException e) {
				Logger.warning(this, "Exception while setting the main window size");
			}
		}

		if (core.getConfig().getValue("mainWindowState") != null) {
			mainWindow.setExtendedState(Integer.parseInt(core.getConfig().getValue("mainWindowState")));
		}

		mainWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}


	public void addWindowListener(WindowListener wl) {
		mainWindow.addWindowListener(wl);
	}

	public void removeWindowListener(WindowListener wl) {
		mainWindow.removeWindowListener(wl);
	}


	public void connectionHasChanged() {
		core.getConnectionManager().addObserver(this);
	}



	/**
	 * Make the window visible or not.
	 */
	public void setVisible(final boolean v) {
		if (!v || !core.isStopping()) {
			mainWindow.setVisible(v);
		}

		if (!v && core.isStopping())
			mainWindow.dispose();
	}


	public boolean isVisible() {
		return mainWindow.isVisible();
	}

	public void setIconified() {
		int state = mainWindow.getExtendedState();

		state |= JFrame.ICONIFIED;

		mainWindow.setExtendedState(state);
	}

	public void setNonIconified() {
		int state = mainWindow.getExtendedState();

		state &= ~JFrame.ICONIFIED;

		mainWindow.setExtendedState(state);
	}


	public JFrame getMainFrame() {
		return mainWindow;
	}


	/**
	 * Should not be used.
	 * @see #addTab(String, java.awt.Component)
	 * @return In the future, it's possible that it will sometimes return null.
	 */
	public JTabbedPane getTabbedPane() {
		return tabbedPane;
	}

	public Object getLastToolbarModifier() {
		return toolBar.getLastToolbarModifier();
	}

	/**
	 * @param modifier Correspond to the caller object: it's a security to avoid that a modifier wipe out the buttons from another one
	 * @param newButtons JButton vector : if null, then it means to remove the buttons from the toolbar. Only the object having currently its buttons displayed will be able to remove them, other will simply be ignored.
	 */
	public void changeButtonsInTheToolbar(final Object modifier, final Collection<JButton> newButtons) {
		toolBar.changeButtonsInTheToolbar(modifier,newButtons);
	}

	public void resetLastKnowToolBarModifier() {
		toolBar.resetLastKnowToolBarModifier();
	}


	/**
	 * Used to add a tab in the main window.
	 * In the future, even if the interface change,
	 * this function should remain available.
	 */
	public boolean addTab(final String tabName, final java.awt.Component panel) {
		return addTab(tabName, IconBox.add, panel);
	}

	/**
	 * Used to add a tab in the main window.
	 * In the future, even if the interface change,
	 * this function should remain available
	 * @see #addTab(String, java.awt.Component)
	 */
	public boolean addTab(final String tabName, final Icon icon,
			      final java.awt.Component panel) {
		tabbedPane.addTab(tabName, icon, panel);

		return true;
	}

	public boolean setSelectedTab(java.awt.Component c) {
		tabbedPane.setSelectedComponent(c);
		return true;
	}


	/**
	 * Used to remove a tab from the main window.
	 */
	public boolean removeTab(final java.awt.Component panel) {
		tabbedPane.remove(panel);

		return true;
	}

	/**
	 * Used by plugins to add their own menu.
	 */
	public void insertMenuAt(JMenu menu, int position) {
		menuBar.insertMenuAt(menu, position);
	}

	public void removeMenu(JMenu menu) {
		menuBar.removeMenu(menu);
	}

	/**
	 * Used by plugins to add their own menu / menuItem to the menu 'file'.
	 */
	public void insertInFileMenuAt(JMenuItem newItem, int position) {
		menuBar.insertInFileMenuAt(newItem, position);
	}

	public void removeFromFileMenu(JMenuItem item) {
		menuBar.removeFromFileMenu(item);
	}

	/**
	 * Warns the user by a popup.
	 */
	protected void unableToConnect() {
		new thaw.gui.WarningWindow(core,
					   I18n.getMessage("thaw.warning.unableToConnectTo")+
					   " "+core.getConfig().getValue("nodeAddress")+":"+ core.getConfig().getValue("nodePort"));
	}

	public void update(final java.util.Observable o, final Object arg) {
		updateToolBar();
	}


	public void updateToolBar() {
		toolBar.updateToolBar();
	}

	/**
	 * Called when window is closed or 'quit' is chosen is the menu.
	 */
	public void endOfTheWorld() {
		if (mainWindow != null) {
			java.awt.Dimension size = mainWindow.getSize();

			core.getConfig().setValue("mainWindowSizeX",
						  Integer.toString((new Double(size.getWidth())).intValue()));
			core.getConfig().setValue("mainWindowSizeY",
						  Integer.toString((new Double(size.getHeight())).intValue()));
			core.getConfig().setValue("mainWindowState",
						  Integer.toString(mainWindow.getExtendedState()));
		}

		core.exit();
	}


	public void setStatus(final javax.swing.Icon icon, final String status) {
		setStatus(icon, status, java.awt.Color.BLACK);
	}


	/**
	 * Change text in the status bar.
	 * @param status Null is accepted.
	 */
	public void setStatus(final javax.swing.Icon icon, final String status, java.awt.Color color) {
		if(status != null) {
			statusBar.setText(status);
		} else {
			statusBar.setText(" ");/* not empty else the status bar disappear */
		}

		if (icon != null)
			statusBar.setIcon(icon);

		if (color != null)
			statusBar.setForeground(color);
	}


	public String getStatus() {
		return statusBar.getText();
	}

	/**
	 * @param pos can be BorderLayout.EAST or BorderLayout.WEST
	 */
	public void addComponent(java.awt.Component c, Object pos) {
		mainWindow.getContentPane().add(c, pos);
	}

	protected void setEnabled(boolean value) {
		mainWindow.setEnabled(value);
	}

	/**
	 * @see #addComponent(java.awt.Component, Object)
	 */
	public void removeComponent(java.awt.Component c) {
		mainWindow.getContentPane().remove(c);
	}


	public void windowActivated(final WindowEvent e) {

	}

	public void windowClosing(final WindowEvent e) {
		/* Should be in windowClosed(), but doesn't seem to work */
		if(e.getSource() == mainWindow)
			endOfTheWorld();
	}

	public void windowClosed(final WindowEvent e) {
		// gni
	}

	public void windowDeactivated(final WindowEvent e) {
		// C'est pas comme si on en avait quelque chose a foutre :p
	}

	public void windowDeiconified(final WindowEvent e) {
		// idem
	}

	public void windowIconified(final WindowEvent e) {
		// idem
	}

	public void windowOpened(final WindowEvent e) {
		// idem
	}

}
