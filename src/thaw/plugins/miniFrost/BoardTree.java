package thaw.plugins.miniFrost;

import javax.swing.JPanel;
import java.util.Observable;
import java.util.Observer;
import java.awt.GridLayout;
import java.awt.BorderLayout;

import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import java.util.Vector;
import java.util.Iterator;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;

import java.awt.Color;

import java.awt.Font;

import thaw.gui.JDragTree;
import thaw.gui.IconBox;
import thaw.core.I18n;

import thaw.plugins.miniFrost.interfaces.BoardFactory;
import thaw.plugins.miniFrost.interfaces.Board;

/**
 * It's called BoardTree, but in fact it's just a list.<br/>
 * Notify each time the selection is changed (board is given in argument)
 */
public class BoardTree extends Observable
	implements javax.swing.event.ListSelectionListener {


	private JPanel panel;

	private BoardListModel model;
	private JList list;

	private Vector actions;

	private MiniFrostPanel mainPanel;

	public final static Color SELECTION_COLOR = new Color(190, 190, 190);
	public final static Color LOADING_COLOR = new Color(230, 230, 230);
	public final static Color LOADING_SELECTION_COLOR = new Color(150, 150, 150);



	public BoardTree(MiniFrostPanel mainPanel) {
		this.mainPanel = mainPanel;

		/* label */

		panel = new JPanel(new BorderLayout());

		panel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.boards")),
			  BorderLayout.NORTH);


		/* board list */

		model = new BoardListModel();
		refresh();

		list = new JList(model);
		list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new BoardListRenderer());
		list.addListSelectionListener(this);
		list.setPreferredSize(new java.awt.Dimension(100, 100));

		panel.add(new JScrollPane(list), BorderLayout.CENTER);


		/* buttons */

		JPanel southPanel = new JPanel(new BorderLayout());

		JPanel buttonPanel = new JPanel(new GridLayout(1, 2));

		JButton button;

		actions = new Vector();

		button = new JButton(IconBox.minAdd);
		button.setToolTipText(I18n.getMessage("thaw.common.add"));
		actions.add(new BoardManagementHelper.BoardAdder(mainPanel, button));
		buttonPanel.add(button);

		button = new JButton(IconBox.minDelete);
		button.setToolTipText(I18n.getMessage("thaw.common.remove"));
		actions.add(new BoardManagementHelper.BoardRemover(mainPanel, button));
		buttonPanel.add(button);

		button = new JButton(IconBox.minRefreshAction);
		button.setToolTipText(I18n.getMessage("thaw.plugin.miniFrost.loadNewMessages"));
		actions.add(new BoardManagementHelper.BoardRefresher(mainPanel, button));
		buttonPanel.add(button);

		southPanel.add(new JLabel(""), BorderLayout.CENTER);
		southPanel.add(buttonPanel, BorderLayout.WEST);

		panel.add(southPanel, BorderLayout.SOUTH);
	}


	protected class BoardListRenderer extends DefaultListCellRenderer {
		public BoardListRenderer() {

		}

		public java.awt.Component getListCellRendererComponent(JList list, Object value,
								       int index, boolean isSelected,
								       boolean cellHasFocus) {
			Board board = (Board)value;

			String str = board.toString();

			if (board.getNewMessageNumber() > 0)
				str += " ("+Integer.toString(board.getNewMessageNumber())+")";

			java.awt.Component c = super.getListCellRendererComponent(list, str,
										  index, isSelected,
										  cellHasFocus);

			c.setFont(c.getFont().deriveFont((float)13.5));

			if (board.getNewMessageNumber() > 0)
				c.setFont(c.getFont().deriveFont(Font.BOLD));
			else
				c.setFont(c.getFont().deriveFont(Font.PLAIN));

			if (board.isRefreshing()) {
				if (isSelected) {
					c.setBackground(LOADING_SELECTION_COLOR);
				} else {
					c.setBackground(LOADING_COLOR);
				}
			} else if (isSelected) {
				c.setBackground(SELECTION_COLOR);
			}

			return c;
		}
	}


	protected class BoardListModel extends AbstractListModel {
		private Vector boardList;

		public BoardListModel() {
			super();
			boardList = null;
		}

		public void setBoardList(Vector l) {
			int oldSize = 0;

			if (boardList != null)
				oldSize = boardList.size();

			boardList = l;

			if (boardList.size() < oldSize)
				fireIntervalRemoved(this, boardList.size(), oldSize);

			if (boardList.size() > oldSize)
				fireIntervalAdded(this, oldSize, boardList.size());

			fireContentsChanged(this, 0, boardList.size());
		}

		public void refresh(Board board) {
			refresh(boardList.indexOf(board));
		}

		public void refresh(int row) {
			fireContentsChanged(this, row, row);
		}

		public Object getElementAt(int index) {
			if (boardList == null)
				return null;

			return boardList.get(index);
		}

		public int getSize() {
			if (boardList == null)
				return 0;

			return boardList.size();
		}
	}





	public void refresh() {
		Vector boardList = new Vector();

		BoardFactory[] factories = mainPanel.getPluginCore().getFactories();

		if (factories != null) {
			for (int i = 0 ; i < factories.length; i++) {
				Vector v = factories[i].getBoards();

				if (v != null) {
					for (Iterator it = v.iterator();
					     it.hasNext();) {
						boardList.add(it.next());
					}

				}
			}
		}

		/* TODO : Sort the vector */

		model.setBoardList(boardList);
	}

	public void refresh(Board board) {
		model.refresh(board);
	}

	public void refresh(int row) {
		model.refresh(row);
	}


	public void loadState() {

	}

	public void saveState() {

	}

	public JPanel getPanel() {
		return panel;
	}

	public void valueChanged(javax.swing.event.ListSelectionEvent e) {
		Board b = ((Board)list.getSelectedValue());

		for (Iterator it = actions.iterator();
		     it.hasNext();) {
			((BoardManagementHelper.BoardAction)it.next()).setTarget(b);
		}

		setChanged();
		notifyObservers(b);
	}
}
