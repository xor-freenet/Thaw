package thaw.fcp;

/**
 *
 */
public interface FCPQuery {

	public boolean start();

	/**
	 * Definitive stop. Transfer is considered as failed.
	 * @return false if really it *cannot* stop the query.
	 */
	public boolean stop();

	/**
	 * Tell if the query is a download query or an upload query.
	 * If >= 1 then *must* be Observable and implements FCPTransferQuery.
	 * @return 0 : Meaningless ; 1 : Download ; 2 : Upload
	 */
	public int getQueryType();

}
