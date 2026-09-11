package org.mycompany.model;

/**
 * Write result for one record (success or failure).
 */
public record WriteResult(int rowNumber, String nodePath, String error) {

	public static WriteResult success(int rowNumber, String nodePath) {
		if (nodePath == null || nodePath.isBlank()) {
			throw new IllegalArgumentException("success() needs a nodePath");
		}
		return new WriteResult(rowNumber, nodePath, null);
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
