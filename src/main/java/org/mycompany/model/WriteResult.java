package org.mycompany.model;

/**
 * Write result for one record (success or failure).
 */
public record WriteResult(int rowNumber, String recordReference, String error) {

	public static WriteResult success(int rowNumber, String recordReference) {
		if (recordReference == null || recordReference.isBlank()) {
			throw new IllegalArgumentException("success() needs a recordReference");
		}
		return new WriteResult(rowNumber, recordReference, null);
	}
	public static WriteResult failure(int rowNumber, String error) {
		if (error == null || error.isBlank()) {
			throw new IllegalArgumentException("failure() needs an error description");
		}
		return new WriteResult(rowNumber, null, error);
	}

	public boolean isSuccess() {
		return error == null;
	}
}
