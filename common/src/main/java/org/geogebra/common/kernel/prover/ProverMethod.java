package org.geogebra.common.kernel.prover;

import org.geogebra.common.util.Prover;

public interface ProverMethod {
	public static final String  VARIABLE_R_STRING = "r__"; // This must be a kind of unique string.
	public static final String  VARIABLE_I_STRING = "I_"; // This must be a kind of unique string.
	public static final String PRIME = "\u0027";

	public static final int WARNING_PERPENDICULAR_OR_PARALLEL = 1;
	public static final int WARNING_EQUALITY_OR_COLLINEAR = 2;
	public static final int WARNING_ANGLE = 3;
	public static final String VARIABLE_CYCLOTOMIC = "CT__";

	public Prover.ProofResult execute();
}
