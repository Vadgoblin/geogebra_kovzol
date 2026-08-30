package org.geogebra.common.kernel.prover.CNIMethod;

public class CNIDefinition {
	// TODO: Consider adding more refinements here, add extra infos related to the Strings.
	String declaration; // declaration in Giac format
	String realRelation; // \n-separated Strings of lhs of real relations in Giac format
	String zeroRelation; // lhs of zero relation in Giac format
	String extraVariable; // an extra variable that is used in the zero relation (and the declaration)
	boolean rMustBe0 = false; // if r is required to be 0
	int warning = 0; // different interpretation than usual?
	int specRestriction = 0; // number of disallowed fixed points
}