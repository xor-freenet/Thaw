package thaw.gui;

import javax.swing.JProgressBar;

import thaw.core.Logger;
import thaw.fcp.FCPTransferQuery;
import thaw.fcp.FCPClientPut;
import thaw.fcp.FCPClientGet;

import thaw.core.I18n;

public class TransferProgressBar extends JProgressBar {
	private static final long serialVersionUID = -4726613087699822787L;
	private FCPTransferQuery query;
	private boolean statusInProgressBar;
	private boolean withBorder;
	
	private final static String failedStr = I18n.getMessage("thaw.common.failed");
	private final static String finishedStr = I18n.getMessage("thaw.common.finished");
	

	public TransferProgressBar(FCPTransferQuery q) {
		this(q, true);
	}

	public TransferProgressBar(FCPTransferQuery query, boolean statusInProgressBar,
				   				boolean withBorder) {
		super(0, 100);
		this.query = query;
		this.statusInProgressBar = statusInProgressBar;
		this.withBorder = withBorder;
	}

	public TransferProgressBar(FCPTransferQuery query, boolean statusInProgressBar) {
		this(query, statusInProgressBar, false);
	}

	public void setQuery(FCPTransferQuery query) {
		this.query = query;
	}

	public void showStatusInProgressBar(boolean v) {
		this.statusInProgressBar = v;
	}


	public void refresh() {

		final int networkProgress = query.getProgression();
		final int nodeProgress = query.getTransferWithTheNodeProgression();
		int overallProgress;

		setStringPainted(true);
		setBorderPainted(withBorder);

		/* TODO(Jflesch): This way of detecting if we are transfering data with the node is bad
		 * and unreliable
		 */
		if(query instanceof FCPClientPut) {
			/* Network transfer takes priority for Put requests */
			if(networkProgress > 0) {
				overallProgress = networkProgress;
			} else {
				overallProgress = nodeProgress;
			}
		} else if(query instanceof FCPClientGet) {
			/* Node transfer takes priority for Get requests */
			if(nodeProgress > 0) {
				overallProgress = nodeProgress;
			} else {
				overallProgress = networkProgress;
			}
		} else {
			Logger.warning(this, "Unrecognized subclass of FCPTransferQuery");
			return;
		}

		if(overallProgress < 0) {
			overallProgress = 0;
		}

		setValue(overallProgress);

		if(!query.isFinished()) {
			String txt= "";
			if (statusInProgressBar) {
				txt = (query.getStatus() +
					      " [ "+Integer.toString(overallProgress)+"% ]");
			} else {
				txt = (Integer.toString(overallProgress)+"%");
			}
			if (!query.isProgressionReliable()) {
				txt += " [*]";
			}
			setString(txt);
		} else if(query.isSuccessful()) {
			setString(finishedStr);
		} else {
			setString(failedStr);
		}
	}
}
