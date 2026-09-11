package org.mycompany.persistence;

import org.mycompany.model.MappedRecord;
import org.mycompany.model.WriteResult;

/**
 * Persistence port (originally CMS-agnostic, now SQLite is used).
 * No exceptions on write — failures are returned as WriteResult.
 * Used by update strategies for folder operations and existence checks.
 */
public interface ImportRepository {

	/**
	 * Creates or updates a target table record (originally a cms document).
	 */
    WriteResult write(MappedRecord record);

	/**
	 * Clears all target table values - originally: documents in target folder (replace mode).
	 */
    void clearTargetFolder();

	/**
	 * Checks if a matching table (originally: document) already exists.
	 */
    boolean exists(MappedRecord record);
}
