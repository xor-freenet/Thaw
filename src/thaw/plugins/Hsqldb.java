package thaw.plugins;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import thaw.core.Core;
import thaw.core.I18n;
import thaw.core.LibraryPlugin;
import thaw.core.Logger;

public class Hsqldb extends LibraryPlugin {
	private Core core;

	public final Object dbLock;
	private Connection connection;

	public Hsqldb() {
		dbLock = new Object();
	}

	public boolean run(final Core core) {
		this.core = core;

		try {
			Class.forName("org.hsqldb.jdbcDriver");
		} catch (final Exception e) {
			Logger.error(this, "ERROR: failed to load HSQLDB JDBC driver.");
			e.printStackTrace();
			System.exit(1);
			return false;
		}

		return true;
	}



	public void realStart() {
		Logger.info(this, "Connecting to the database ...");

		if(core.getConfig().getValue("hsqldb.url") == null)
			core.getConfig().setValue("hsqldb.url", "jdbc:hsqldb:file:thaw.db;shutdown=true");

		try {
			connect();
		} catch (final java.sql.SQLException e) {
			Logger.error(this, "SQLException while connecting to the database '"+core.getConfig().getValue("hsqldb.url")+"'");
			e.printStackTrace();
		}
	}


	public void connect() throws java.sql.SQLException {
		if(core.getConfig().getValue("hsqldb.url") == null)
			core.getConfig().setValue("hsqldb.url", "jdbc:hsqldb:file:thaw.db");

		if(connection != null)
			disconnect();
		
		connection = DriverManager.getConnection(core.getConfig().getValue("hsqldb.url"),
							 "sa", "");

		try {
		executeQuery("SET LOGSIZE 50;");
		} catch (final java.sql.SQLException e) {
			/* Newer versions of HSQLDB have an alternate log size property */
			executeQuery("SET FILES LOG SIZE 50;");
		}

		try {
			executeQuery("SET CHECKPOINT DEFRAG 50;");
		} catch (final java.sql.SQLException e) {
			/* Newer versions of HSQLDB use a different property */
			executeQuery("SET FILES DEFRAG 50;");
		}
		
		executeQuery("SET PROPERTY \"hsqldb.nio_data_file\" FALSE");
	}

	public void disconnect() throws java.sql.SQLException {
		synchronized(dbLock) {
			connection.commit();
			executeQuery("SHUTDOWN");
			connection.close();
		}
	}


	public void stop() {
		/* \_o< */
	}

	public void realStop() {
		Logger.info(this, "Disconnecting from the database ...");

		try {
			disconnect();
		} catch(final java.sql.SQLException e) {
			Logger.error(this, "SQLException while closing connection !");
			e.printStackTrace();
		}

		Logger.info(this, "Done.");
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.hsqldb.database");
	}



	public Connection getConnection() {
		return connection;
	}


	public void executeQuery(final String query) throws java.sql.SQLException {
		synchronized(dbLock) {
			final Statement stmt = connection.createStatement();

			stmt.execute(query);

			stmt.close();
		}
	}

	public javax.swing.ImageIcon getIcon() {
		return thaw.gui.IconBox.database;
	}


	/**
	 * Determines if the table exists in the database.
	 * @param tableName Name of the table to test.
	 * @return True if the table exists, else false.
	 */
	public boolean tableExists(final String tableName) {
		try {
			executeQuery("SELECT COUNT(1) FROM "+tableName);
			/* The table exists */
			return true;
		} catch(final SQLException e) {
			return false;
		}
	}


	/**
	 * Given a "CREATE TABLE" query, extracts the table name.
	 *
	 * TODO: Where should this go?  It doesn't use this class...
	 *
	 * @param query Create table query
	 * @return Table name contained in the create table query.
	 */
	public String getTableNameFromCreateTable(final String query) {
		try {
			Pattern findTablePattern = Pattern.compile("(?i)\\A\\s*CREATE\\s(?:MEMORY|CACHED|GLOBAL|TEMPORARY|TEMP|TEXT|\\s)+\\sTABLE\\s([\\w]+).*", Pattern.MULTILINE);
			Matcher findTableMatcher = findTablePattern.matcher(query);
			if (findTableMatcher.find()) {
				return findTableMatcher.group(1);
			} else {
				Logger.warning(this, "No table name found in query: "+query);
				return null;
			}
		} catch (PatternSyntaxException ex) {
			// Syntax error in the regular expression
			Logger.error(this, "PatternSyntaxException: "+ex.getMessage());
			return null;
		}
	}
}
