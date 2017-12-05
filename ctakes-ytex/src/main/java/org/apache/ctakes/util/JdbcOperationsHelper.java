package org.apache.ctakes.util;

import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Refactors helper functions like dropTableIfExists or other DB operations
 *
 * // TODO: consider renaming it with something more suitable
 */
public abstract class JdbcOperationsHelper {

	static private final Logger LOGGER = Logger.getLogger(JdbcOperationsHelper.class);

	/**
	 * Helper function to drop a 'table' from a DB, using SQL syntax
	 *
	 * @param jdbc
	 * @param dbEngineType
	 * @param sqlTableName
	 */
	protected final void dropTableIfExist(JdbcOperations jdbc, final String dbEngineType, final String sqlTableName) {
		// TODO: consider refactor using JOOQ
		String sqlStatement = "";
		switch (dbEngineType.toLowerCase()) {
			case "hsql":
			case "mysql":
				sqlStatement = String.format("DROP TABLE IF EXISTS %s", sqlTableName);
				break;
			case "mssql":
				sqlStatement = String.format("IF EXISTS(SELECT * FROM sys.objects WHERE object_id = object_id('%s')) DROP TABLE %s", sqlTableName);
				break;
			case "orcl":
				sqlStatement = String.format("DROP TABLE %s", sqlTableName);
				break;
			default:
				LOGGER.warn(String.format("unsupported DB engine type: %s", dbEngineType));
				break;
		}
		if (!sqlStatement.isEmpty()) {
			try {
				jdbc.execute(sqlStatement);
			} catch (DataAccessException e) {
				LOGGER.warn("couldn't drop table test_concepts. Maybe it doesn't even exists", e);
			}
		}
	}
}
