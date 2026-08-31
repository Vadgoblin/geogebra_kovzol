package org.geogebra.common.kernel.prover;

import java.util.TreeSet;import java.util.function.Function;

import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.scripting.CmdShowProof;
import org.geogebra.common.util.Prover.ProofResult;

import static org.geogebra.common.kernel.prover.CNIMethod.ProverCNIMethod.PRIME;


public class Prover {

	public static <T extends ProverMethod> ProofResult prove(org.geogebra.common.util.Prover prover, Function<ProverContext, T> methodFactory) {
		// All predecessors:
		TreeSet<GeoElement> allPredecessors = prover.getStatement().getAllPredecessors();
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

		ProverContext context = new ProverContext(prover, allPredecessorPoints,primeLabels );

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
