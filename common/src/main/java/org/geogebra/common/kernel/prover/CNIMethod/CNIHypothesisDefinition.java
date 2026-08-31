package org.geogebra.common.kernel.prover.CNIMethod;

import static org.geogebra.common.kernel.prover.CNIMethod.ProverCNIMethod.getUniqueLabel;
import static org.geogebra.common.kernel.prover.ProverMethod.VARIABLE_CYCLOTOMIC;
import static org.geogebra.common.kernel.prover.ProverMethod.WARNING_EQUALITY_OR_COLLINEAR;
import static org.geogebra.common.kernel.prover.ProverMethod.WARNING_PERPENDICULAR_OR_PARALLEL;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.algos.AlgoDependentPoint;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.algos.AlgoIntersectConics;
import org.geogebra.common.kernel.algos.AlgoIntersectLineConic;
import org.geogebra.common.kernel.algos.AlgoIntersectLines;
import org.geogebra.common.kernel.algos.AlgoIntersectSingle;
import org.geogebra.common.kernel.algos.AlgoMidpoint;
import org.geogebra.common.kernel.algos.AlgoMidpointSegment;
import org.geogebra.common.kernel.algos.AlgoMirror;
import org.geogebra.common.kernel.algos.AlgoPointOnPath;
import org.geogebra.common.kernel.algos.AlgoPolygonRegular;
import org.geogebra.common.kernel.algos.AlgoRotatePoint;
import org.geogebra.common.kernel.algos.AlgoTranslate;
import org.geogebra.common.kernel.geos.GeoAngle;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoVector;
import org.geogebra.common.util.DoubleUtil;

public class CNIHypothesisDefinition {
	private final GiacCommandFactory commandFactory;

	public CNIHypothesisDefinition(GiacCommandFactory commandFactory) {
		this.commandFactory = commandFactory;
	}


	/**
	 * Create the CNI definition for a GeoElement (for a hypothesis).
	 * Compute the full declaration String, but only the lhs of the real relation String,
	 * and return them to the caller. This method should cover all algos sooner or later.
	 * Now it is just a prototype that implements the CNI method for some frequently used algos.
	 * @param ge the input GeoElement
	 * @return all required information for the CNI definition for the input
	 */
	public CNIDefinition create(GeoElement ge) {
		AlgoElement ae = ge.getParentAlgorithm();
		String gel = getUniqueLabel(ge);

		// Declarations:
		if (ae instanceof AlgoDependentPoint) {
			return handleDependentPoint(ge, gel);
		}
		if (ae instanceof AlgoMidpoint) {
			return handleAlgoMidpoint((AlgoMidpoint) ae, gel);
		}
		if (ae instanceof AlgoMidpointSegment) {
			return handleAlgoMidpointSegment((AlgoMidpointSegment) ae, gel);
		}

		// Real relations:
		if (ae instanceof AlgoIntersectSingle) {
			ae = ((AlgoIntersectSingle) ae).getAlgo();
		}

		if (ae instanceof AlgoIntersectLines) {
			return handleAlgoIntersectLines((AlgoIntersectLines) ae, ge);
		}
		if (ae instanceof AlgoIntersectLineConic) {
			return handleAlgoIntersectLineConic((AlgoIntersectLineConic) ae, ge);
		}
		if (ae instanceof AlgoIntersectConics) {
			return handleAlgoIntersectConics((AlgoIntersectConics) ae, ge);
		}
		if (ae instanceof AlgoPointOnPath) {
			return handleAlgoPointOnPath((AlgoPointOnPath) ae, ge);
		}
		if (ae instanceof AlgoTranslate) {
			return handleAlgoTranslate((AlgoTranslate) ae, gel);
		}
		if (ae instanceof AlgoRotatePoint) {
			return handleAlgoRotatePoint((AlgoRotatePoint) ae, gel);
		}
		if (ae instanceof AlgoMirror) {
			return handleAlgoMirror((AlgoMirror) ae, gel);
		}
		if (ae instanceof AlgoPolygonRegular) {
			return handleAlgoPolygonRegular(ge, (AlgoPolygonRegular) ae, gel);
		}

		// Unimplemented, but it should be handled...
		return null;
	}

	private CNIDefinition handleDependentPoint(GeoElement ge, String gel) {
		CNIDefinition c = new CNIDefinition();
		String def = ge.getDefinition(StringTemplate.defaultTemplate);
		// TODO: Check if this is polynomial. Now we are optimistic.
		// TODO: The whole expression should be rewritten via getUniqueLabel.
		c.declaration = gel + ":=" + def;
		return c;
	}

	private CNIDefinition handleAlgoMidpoint(AlgoMidpoint am, String gel) {
		CNIDefinition c = new CNIDefinition();

		GeoElement P = am.getP();
		GeoElement Q = am.getQ();
		String Pl = getUniqueLabel(P);
		String Ql = getUniqueLabel(Q);
		c.declaration = gel + ":=(" + Pl + "+" + Ql + ")/2";
		return c;
	}

	private CNIDefinition handleAlgoMidpointSegment(AlgoMidpointSegment ams, String gel) {
		CNIDefinition c = new CNIDefinition();

		GeoElement P = ams.getP();
		GeoElement Q = ams.getQ();
		String Pl = getUniqueLabel(P);
		String Ql = getUniqueLabel(Q);
		c.declaration = gel + ":=(" + Pl + "+" + Ql + ")/2";
		return c;
	}

	private CNIDefinition handleAlgoIntersectLines(AlgoIntersectLines ail, GeoElement ge) {
		CNIDefinition c = new CNIDefinition();

		GeoLine g = ail.getg();
		GeoLine h = ail.geth();
		String rel1 = "", rel2 = "";
		rel1 = commandFactory.online((GeoPoint) ge, g);
		rel2 = commandFactory.online((GeoPoint) ge, h);
		if (rel1 == null || rel2 == null) {
			return null; // Not implemented.
		}
		if (rel1.startsWith("perppar") || rel2.startsWith("perppar")) {
			c.warning = WARNING_PERPENDICULAR_OR_PARALLEL;
		}
		if (rel1.startsWith("isosc") || rel2.startsWith("isosc")) {
			c.warning = WARNING_EQUALITY_OR_COLLINEAR;
		}
		c.realRelation = rel1 + "\n" + rel2;
		return c;
	}

	private CNIDefinition handleAlgoIntersectLineConic(AlgoIntersectLineConic ailc, GeoElement ge) {
		CNIDefinition c = new CNIDefinition();

		GeoLine l = ailc.getLine();
		GeoConic co = ailc.getConic();
		String rel1 = "", rel2 = "";
		rel1 = commandFactory.online((GeoPoint) ge, l);
		rel2 = commandFactory.oncircle((GeoPoint) ge, co);
		if (rel1 == null || rel2 == null) {
			return null; // Not implemented.
		}
		if (rel1.startsWith("perppar")) {
			c.warning = WARNING_PERPENDICULAR_OR_PARALLEL;
		}
		if (rel1.startsWith("isosc")) {
			c.warning = WARNING_EQUALITY_OR_COLLINEAR;
		}
		c.realRelation = rel1 + "\n" + rel2;
		return c;
	}

	private CNIDefinition handleAlgoIntersectConics(AlgoIntersectConics aic, GeoElement ge) {
		CNIDefinition c = new CNIDefinition();

		GeoConic co1 = aic.getA();
		GeoConic co2 = aic.getB();
		String rel1 = "", rel2 = "";
		rel1 = commandFactory.oncircle((GeoPoint) ge, co1);
		rel2 = commandFactory.oncircle((GeoPoint) ge, co2);
		if (rel1 == null || rel2 == null) {
			return null; // Not implemented.
		}
		c.realRelation = rel1 + "\n" + rel2;
		return c;
	}

	private CNIDefinition handleAlgoPointOnPath(AlgoPointOnPath apop, GeoElement ge) {
		CNIDefinition c = new CNIDefinition();

		GeoElement[] input = apop.getInput();
		GeoElement p = input[0];
		if (p instanceof GeoLine) {
			GeoPoint gS = ((GeoLine) p).getStartPoint();
			GeoPoint gE = ((GeoLine) p).getEndPoint();
			c.realRelation = commandFactory.online((GeoPoint) ge, (GeoLine) p);
			if (c.realRelation.startsWith("perppar")) {
				c.warning = WARNING_PERPENDICULAR_OR_PARALLEL;
			}
			if (c.realRelation.startsWith("isosc")) {
				c.warning = WARNING_EQUALITY_OR_COLLINEAR;
			}
			return c;
		}
		if (p instanceof GeoConic) {
			AlgoElement pAe = p.getParentAlgorithm();
			if (((GeoConic) p).isCircle()) {
				c.realRelation = commandFactory.oncircle((GeoPoint) ge, (GeoConic) p);
				return c;
			}
			return null; // Not implemented.
		}
		return null; // Not implemented.
	}

	private CNIDefinition handleAlgoTranslate(AlgoTranslate at, String gel) {
		CNIDefinition c = new CNIDefinition();

		GeoElement P = (GeoElement) at.getInput(0);
		GeoElement v = (GeoElement) at.getInput(1);
		if (P instanceof GeoPoint && v instanceof GeoVector) {
			GeoVector gv = (GeoVector) v;
			AlgoElement gvAe = gv.getParentAlgorithm();
			GeoElement A = (GeoElement) gvAe.getInput(0);
			GeoElement B = (GeoElement) gvAe.getInput(1);
			String Pl = getUniqueLabel(P);
			String Al = getUniqueLabel(A);
			String Bl = getUniqueLabel(B);
			c.declaration = gel + ":=" + Pl + "+" + Bl + "-" + Al;
			return c;
		}
		return null; // Not implemented.
	}

	private CNIDefinition handleAlgoRotatePoint(AlgoRotatePoint arp, String gel) {
		CNIDefinition c = new CNIDefinition();

		GeoElement P = (GeoElement) arp.getInput(0); // rotated
		GeoElement a = (GeoElement) arp.getInput(1); // angle
		GeoElement C = (GeoElement) arp.getInput(2); // center
		if (P instanceof GeoPoint && a instanceof GeoAngle && C instanceof GeoPoint) {
			// This is taken from AlgoRotatePoint (Botana's method)
			double angleDoubleVal = ((GeoAngle) a).getDouble();
			double angleDoubleValDeg = angleDoubleVal / Math.PI * 180;
			int angleValDeg = (int) angleDoubleValDeg;
			if (!DoubleUtil.isInteger(angleDoubleValDeg)) {
				// unhandled angle, not an integer degree
				return null; // Unimplemented.
			}
			// Compute the gcd of the angle and 360 degrees. For 90 degrees, this is 90,
			// for 120, this is 120, for 135, this is 45, for example.
			long gcd = Kernel.gcd(angleValDeg, 360);
			// Which primitive root of unit will be used to describe the rotation?
			long prim = Math.abs(360 / gcd); // This is 4 for 90 degrees, 3 for 120 degrees,
			// 8 for 135 (~45) degrees.
			// Create the minimal polynomial. E.g.: "expand(r2e(cyclotomic(8)))", for 135 degrees.
			String minpoly = commandFactory.cyclotomicPolynomial((int) prim);
			// Now we create the declaration:
			String Pl = getUniqueLabel(P);
			String Cl = getUniqueLabel(C);
			String ctVar = VARIABLE_CYCLOTOMIC + prim;
			c.declaration =
					gel + ":=" + Cl + "+(" + Pl + "-" + Cl + ")*" + ctVar; // complex rotation
			c.zeroRelation = minpoly; // set the minimal polynomial as an extra relation
			c.extraVariable = ctVar; // set the extra variable
			return c;
		}

		return null;
	}

	private CNIDefinition handleAlgoMirror(AlgoMirror am, String gel) {
		CNIDefinition c = new CNIDefinition();
		GeoElement P = (GeoElement) am.getInput(0);
		GeoElement M = (GeoElement) am.getInput(1);
		if (P instanceof GeoPoint && M instanceof GeoPoint) {
			String Pl = getUniqueLabel(P);
			String Ml = getUniqueLabel(M);
			c.declaration = gel + ":=" + Ml + "-(" + Pl + "-" + Ml + ")";
			return c;
		}
		return null; // Not implemented.
	}

	private CNIDefinition handleAlgoPolygonRegular(GeoElement ge, AlgoPolygonRegular ap,
			String gel) {
		CNIDefinition c = new CNIDefinition();

		GeoPoint A = (GeoPoint) ap.getInput(0);
		GeoPoint B = (GeoPoint) ap.getInput(1);
		String Al = getUniqueLabel(A);
		String Bl = getUniqueLabel(B);
		int num = (int) ((GeoNumeric) ap.getInput(2)).getValue(); // number of sides
		// The sum of external angles in a regular polygon is 360 degrees.
		// When computing C from A and B, C=B+(B-A)*CT_num,
		// D=C+(C-B)*CT_num
		// where CT_num is a numth primitive root of the unit.
		// That is, D=B+(B-A)*CT_num+(B+(B-A)*CT_num-B)*CT_num=B+(B-A)*CT_num+((B-A)*CT_num)*CT_num,
		// in general, for the ith vertex (numbered from 0), P_i=B+(B-A)*(CT_num+CT_num^2+CT_num^3+...+CT_num^(i-1))
		// where P_i is the ith vertex.
		GeoElement[] outputObjects = ap.getOutput();
		// The 0th object is the polygon, the 1st, 2nd, ..., nth are the segments of the sides,
		// the (n+1)th object is the 2nd point, the (n+2)th object is the 3rd point, and so on.
		for (int i = num + 1; i < outputObjects.length; i++) {
			if (ge.equals(outputObjects[i])) {
				int whichPoint = i - num + 1;
				String ctVar = VARIABLE_CYCLOTOMIC + num;
				c.declaration = gel + ":=" + Bl + "+(" + Bl + "-" + Al + ")*(";
				for (int j = 1; j < whichPoint; j++) {
					if (j > 1) {
						c.declaration += "+";
					}
					c.declaration += ctVar + "^" + j;
				}
				c.declaration += ")";
				c.zeroRelation = commandFactory.cyclotomicPolynomial(num);
				c.extraVariable = ctVar;
				return c;
			}
		}
		// Unimplemented, but it should be handled...
		return null;
	}
}
