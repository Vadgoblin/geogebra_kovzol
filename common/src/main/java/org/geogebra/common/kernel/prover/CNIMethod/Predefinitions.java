package org.geogebra.common.kernel.prover.CNIMethod;

public class Predefinitions {
	// We try to avoid divisions by X-Y if X or Y are generated points,
	// to not introduce extra degeneracy than required. That is,
	// all formulas assume that arguments are ordered where the free points appear first.
	private static final String[] PREDEFINITIONS = {
			"coll(A_,B_,C_):=(A_-C_)/(A_-B_)",
			"par(A_,B_,C_,D_):=(C_-D_)/(A_-B_)",
			"perppar(A_,B_,C_,D_):=((C_-D_)/(A_-B_))^2",
			"conc(A_,B_,C_,D_):=((C_-D_)/(C_-A_))/((B_-D_)/(B_-A_))",
			// They are not considered yet:
			"eqangle(A_,B_,C_,D_,E_,F_):=((B_-A_)/(B_-C_))/((E_-D_)/(E_-F_))",
			"eqanglemul(A_,B_,C_,D_,E_,F_,n_):=((B_-A_)/(B_-C_))/((E_-D_)/(E_-F_))^n_",
			"anglex(A_,B_,C_,D_,n_):=((C_-D_)/(A_-B_))^n_",
			"isosc(A_,B_,C_):=eqangle(C_,B_,A_,A_,C_,B_)" // |AB|=|AC|
	};

	private static String defs;

	private static void buildDefs(){
		for (String predefinition : PREDEFINITIONS) {
			defs += "[" + predefinition + "],";
		}
	}

	public static int count() {
		return PREDEFINITIONS.length;
	}

	public static String get() {
		if (defs == null){
			buildDefs();
		}
		return defs;
	}
}
