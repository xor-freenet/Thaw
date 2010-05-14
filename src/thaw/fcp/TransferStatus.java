package thaw.fcp;

public enum TransferStatus {
	NOT_RUNNING(false,false,false),
	RUNNING(true,false,false),
	FAILED(false,true,false),
	SUCCESSFUL(false,true,true);

	private boolean running;
	private boolean finished;
	private boolean successful;

	TransferStatus(boolean running, boolean finished, boolean successful) {
		this.running = running;
		this.finished = finished;
		this.successful = successful;
	}

	public boolean isRunning() {
		return running;
	}

	public boolean isFinished() {
		return finished;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public static TransferStatus getTransferStatus(boolean running, boolean finished, boolean successful) {
		if(running) {
			return RUNNING;
		} else {
			if(finished) {
				if(successful) {
					return SUCCESSFUL;
				} else {
					return FAILED;
				}
			} else {
				return NOT_RUNNING;
			}
		}
	}
}
