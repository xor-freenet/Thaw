package thaw.gui;

import thaw.core.Core;
import thaw.core.I18n;
import thaw.core.Logger;
import thaw.gui.ActionHandlers.MainConnect;
import thaw.gui.ActionHandlers.MainDisconnect;
import thaw.gui.ActionHandlers.MainOptions;
import thaw.gui.ActionHandlers.MainQuit;

import javax.swing.*;
import java.util.Collection;


public class ToolBar {
	protected final Core core;
	protected final JToolBar toolBar;
	protected final JButton connectButton;
	protected final JButton disconnectButton;
	protected final JButton settingsButton;
	protected final JButton quitButton;

	protected Object lastToolBarModifier = null;

	public ToolBar(Core core) {
		this.core = core;
		this.toolBar = new JToolBar();

		connectButton = new JButton(IconBox.connectAction);
		connectButton.setBorderPainted(false);
		connectButton.setToolTipText(I18n.getMessage("thaw.toolbar.button.connect"));

		disconnectButton = new JButton(IconBox.disconnectAction);
		disconnectButton.setBorderPainted(false);
		disconnectButton.setToolTipText(I18n.getMessage("thaw.toolbar.button.disconnect"));

		settingsButton = new JButton(IconBox.settings);
		settingsButton.setBorderPainted(false);
		settingsButton.setToolTipText(I18n.getMessage("thaw.toolbar.button.settings"));

		quitButton = new JButton(IconBox.quitAction);
		quitButton.setBorderPainted(false);
		quitButton.setToolTipText(I18n.getMessage("thaw.toolbar.button.quit"));

		connectButton.addActionListener(new MainConnect(core));
		disconnectButton.addActionListener(new MainDisconnect(core));
		settingsButton.addActionListener(new MainOptions(core));
		quitButton.addActionListener(new MainQuit(core));
	}

		/**
	 * @param modifier Correspond to the caller object: it's a security to avoid that a modifier wipe out the buttons from another one
	 * @param newButtons JButton vector : if null, then it means to remove the buttons from the toolbar. Only the object having currently its buttons displayed will be able to remove them, other will simply be ignored.
	 */
	public void changeButtonsInTheToolbar(final Object modifier, final Collection<JButton> newButtons) {
		Logger.debug(this, "changeButtonsInTheToolbar() : Called by "+modifier.getClass().getName());
		Logger.debug(this, newButtons == null ? "-> no button" : Integer.toString(newButtons.size()) + " buttons");

		if ((lastToolBarModifier == null) || (newButtons != null) || (lastToolBarModifier == modifier)) {
			lastToolBarModifier = modifier;
		} else
			/* Only the modifier who added the buttons can remove them */
			return;

		if (newButtons == null)
			lastToolBarModifier = null;

		toolBar.removeAll();
		toolBar.setName(I18n.getMessage("thaw.toolbar.title"));

		toolBar.setBorderPainted(false);
		toolBar.add(connectButton);
		toolBar.add(disconnectButton);
		toolBar.addSeparator();
		toolBar.add(settingsButton);
		toolBar.addSeparator();

		if (newButtons != null) {
			for(final JButton button : newButtons) {
				if (button != null) {
					button.setBorderPainted(false);
					toolBar.add(button);
				} else
					toolBar.addSeparator();
			}
			toolBar.addSeparator();
		}

		toolBar.add(quitButton);
		toolBar.setFloatable(false);

		updateToolBar();
		toolBar.validate();
	}

	public void updateToolBar() {
		if( core.getConnectionManager() != null &&
		   (core.getConnectionManager().isConnected() || core.isReconnecting())) {
			connectButton.setEnabled(false);
			disconnectButton.setEnabled(true);
		} else {
			connectButton.setEnabled(true);
			disconnectButton.setEnabled(false);
		}
	}

	public Object getLastToolbarModifier() {
		return lastToolBarModifier;
	}

	public void resetLastKnowToolBarModifier() {
		lastToolBarModifier = null;
	}

	public JToolBar getToolBar() {
		return toolBar;
	}
}
