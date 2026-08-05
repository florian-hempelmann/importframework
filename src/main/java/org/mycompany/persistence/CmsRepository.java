package org.mycompany.persistence;

import org.mycompany.model.MappedRecord;
import org.mycompany.model.WriteResult;

/**
 * Persistence port (CMS-agnostic).
 * Implementations map to the concrete CMS (e.g. JCR adapter).
 * No exceptions on write — failures are returned as WriteResult.
 * Used by update strategies for folder operations and existence checks.
 */
public interface CmsRepository {

	/**
	 * Creates or updates a cms document.
	 */
    WriteResult write(MappedRecord record);

	/**
	 * Clears all documents in target folder (replace mode).
	 */
    void clearTargetFolder();

	/**
	 * Checks if a matching document already exists.
	 */
    boolean exists(MappedRecord record);
}
