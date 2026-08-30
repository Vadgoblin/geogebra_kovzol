package org.geogebra.common.kernel.prover;

import org.geogebra.common.util.Prover;

public interface ProverMethod {
	static final String PRIME = "\u0027"; // TODO: fix public access
	static final int WARNING_PERPENDICULAR_OR_PARALLEL = 1;
	static final int WARNING_EQUALITY_OR_COLLINEAR = 2;
	static final int WARNING_ANGLE = 3;
	static final String VARIABLE_CYCLOTOMIC = "CT__";
	static final String VARIABLE_R_STRING = "r__"; // This must be a kind of unique string.
	static final String VARIABLE_I_STRING = "I_"; // This must be a kind of unique string.

	Prover.ProofResult execute();
}
