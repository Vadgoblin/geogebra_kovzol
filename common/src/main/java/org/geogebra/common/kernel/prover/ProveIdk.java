package org.geogebra.common.kernel.prover;

import java.util.TreeSet;import java.util.function.Function;

import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.scripting.CmdShowProof;
import org.geogebra.common.util.Prover;import org.geogebra.common.util.Prover.ProofResult;

import static org.geogebra.common.kernel.prover.ProverCNIMethod.PRIME;


public class ProveIdk {


	public static <T extends ProverMethod> ProofResult prove(Prover prover, Function<ProverContext, T> methodFactory) {
		ProverContext context = new ProverContext();

		context.prover = prover;
		context.statement = prover.getStatement();
		context.kernel = context.statement.getKernel();
		context.loc = context.kernel.getLocalization();

		// We try to avoid divisions by X-Y if X or Y are generated points,
		// to not introduce extra degeneracy than required. That is,
		// all formulas assume that arguments are ordered where the free points appear first.
		String[] predefinitions = {"coll(A_,B_,C_):=(A_-C_)/(A_-B_)",
				"par(A_,B_,C_,D_):=(C_-D_)/(A_-B_)",
				"perppar(A_,B_,C_,D_):=((C_-D_)/(A_-B_))^2",
				"conc(A_,B_,C_,D_):=((C_-D_)/(C_-A_))/((B_-D_)/(B_-A_))",
				// They are not considered yet:
				"eqangle(A_,B_,C_,D_,E_,F_):=((B_-A_)/(B_-C_))/((E_-D_)/(E_-F_))",
				"eqanglemul(A_,B_,C_,D_,E_,F_,n_):=((B_-A_)/(B_-C_))/((E_-D_)/(E_-F_))^n_",
				"anglex(A_,B_,C_,D_,n_):=((C_-D_)/(A_-B_))^n_",
				"isosc(A_,B_,C_):=eqangle(C_,B_,A_,A_,C_,B_)" // |AB|=|AC|
		};
		String predefs = "";
		for (String predefinition : predefinitions) {
			predefs += "[" + predefinition + "],";
		}

		context.predefinitions = predefinitions;
		context.predefs = predefs;


		// All predecessors:
		TreeSet<GeoElement> allPredecessors = context.statement.getAllPredecessors();
		// prime labels
		TreeSet<String> primeLabels = new TreeSet<>();


		// Keep only points:
		TreeSet<GeoPoint> allPredecessorPoints = new TreeSet<>();
		for (GeoElement p : allPredecessors) {
			if (p instanceof GeoPoint) {
				allPredecessorPoints.add((GeoPoint) p);
				primeLabels.add(getUniqueLabel(p));
			}
		}

		context.allPredecessorPoints = allPredecessorPoints;
		context.primeLabels = primeLabels;

		// inform user that variables sucha as A' will cause issues when the proof is displayed
		if (context.prover.getShowproof() && containsPrimedPointLabel(primeLabels)) {
			context.prover.addProofLine(CmdShowProof.PROBLEM,
					context.loc.getMenuDefault("CNIPrimedLabelsWarning",
							"Warning: Labels that already contain a prime symbol can cause display problems in later proof steps."));
		}

		ProverMethod method = methodFactory.apply(context);
		return method.execute();
	}

	/**
	 * Return a label that is unique and can be inserted in a Giac code.
	 * @param ge the input GeoElement
	 * @return the label as String
	 */
	static String getUniqueLabel(GeoElement ge) {
		return ge.getLabelSimple().replace("_{", "").replace("}", "");
	}

	private static boolean containsPrimedPointLabel(TreeSet<String> labels) {
		for (String lab : labels) {
			if (lab != null && lab.contains(PRIME)) {
				return true;
			}
		}
		return false;
	}

}
