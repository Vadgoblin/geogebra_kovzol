package org.geogebra.common.kernel.prover.CNIMethod;

import static org.geogebra.common.kernel.prover.CNIMethod.ProverCNIMethod.getUniqueLabel;
import static org.geogebra.common.kernel.prover.CNIMethod.ProverCNIMethod.removeTail;

import java.util.TreeSet;

import org.geogebra.common.kernel.algos.AlgoAnglePoints;
import org.geogebra.common.kernel.algos.AlgoAngularBisectorPoints;
import org.geogebra.common.kernel.algos.AlgoCircleThreePoints;
import org.geogebra.common.kernel.algos.AlgoCircleTwoPoints;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.algos.AlgoLineBisector;
import org.geogebra.common.kernel.algos.AlgoLineBisectorSegment;
import org.geogebra.common.kernel.algos.AlgoLinePointLine;
import org.geogebra.common.kernel.algos.AlgoOrthoLinePointLine;
import org.geogebra.common.kernel.geos.GeoAngle;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoSegment;

import static org.geogebra.common.kernel.prover.ProverMethod.VARIABLE_CYCLOTOMIC;
import static org.geogebra.common.kernel.prover.ProverMethod.WARNING_EQUALITY_OR_COLLINEAR; // FIXME

public class GiacCommandFactory {
	private final Giac giac;

	GiacCommandFactory(Giac giac){
		this.giac=giac;
	}

	String collinear(GeoElement ge1, GeoElement ge2, GeoElement ge3) {
		TreeSet<GeoElement> collPoints = new TreeSet<>();
		collPoints.add(ge1);
		collPoints.add(ge2);
		collPoints.add(ge3);
		String ret = "coll(";
		for (GeoElement cp : collPoints) {
			ret += getUniqueLabel(cp) + ",";
		}
		ret = removeTail(ret, 1); // FIXME
		ret += ")";
		return ret;
	}

	String concyclic(GeoElement ge1, GeoElement ge2, GeoElement ge3, GeoElement ge4) {
		TreeSet<GeoElement> concPoints = new TreeSet<>();
		concPoints.add(ge1);
		concPoints.add(ge2);
		concPoints.add(ge3);
		concPoints.add(ge4);
		String ret = "conc(";
		for (GeoElement cp : concPoints) {
			ret += getUniqueLabel(cp) + ",";
		}
		ret = removeTail(ret, 1);
		ret += ")";
		return ret;
	}

	String cyclotomicPolynomial(int n) {
		String ctVar = VARIABLE_CYCLOTOMIC + n;
		String minpolyP = "subst(expand(r2e(cyclotomic(" + n + "))),x=" + ctVar + ")";
		return giac.execute(minpolyP);
	}

	String parallel(GeoPoint ge1, GeoPoint ge2, GeoPoint ge3, GeoPoint ge4) {
		String ge1l = getUniqueLabel(ge1);
		String ge2l = getUniqueLabel(ge2);
		String ge3l = getUniqueLabel(ge3);
		String ge4l = getUniqueLabel(ge4);

		int i1 = ge1.getConstructionIndex();
		int i2 = ge2.getConstructionIndex();
		int i3 = ge3.getConstructionIndex();
		int i4 = ge4.getConstructionIndex();

		// In a natural order we return the same ordered quadruple:
		if (i1 < i2 && i2 < i3 && i3 < i4)
			return "par(" + ge1l + "," + ge2l + "," + ge3l + "," + ge4l + ")";

		// In some reversed orders we return the same ordered quadruple:
		if ((i1 > i3 && i2 > i4) || (i1 > i4 && i2 > i3))
			return "par(" + ge3l + "," + ge4l + "," + ge1l + "," + ge2l + ")";

		// Otherwise we return the default order:
		return "par(" + ge1l + "," + ge2l + "," + ge3l + "," + ge4l + ")";
	}

	String parallel(GeoLine g, GeoLine h) {
		/* In general, here we need a much more sophisticated way.
		 * It is possible that g or h is defined with a point and an algo (maybe parallelism or perpendicularity),
		 * but the definition can go arbitrary deeply, so here some recursive way would be more general.
		 */
		GeoPoint gS = g.getStartPoint();
		GeoPoint gE = g.getEndPoint();
		GeoPoint hS = h.getStartPoint();
		GeoPoint hE = h.getEndPoint();
		if (gE != null && hE != null) {
			return parallel(gS, gE, hS, hE);
		}
		if (gE == null && hE != null) {
			AlgoElement gAe = g.getParentAlgorithm();
			if (gAe instanceof AlgoOrthoLinePointLine) {
				AlgoOrthoLinePointLine aolpl = (AlgoOrthoLinePointLine) gAe;
				GeoElement[] input = aolpl.getInput();
				GeoLine l = (GeoLine) input[1];
				gS = l.getStartPoint();
				gE = l.getEndPoint();
				if (gE != null) {
					return perppar(gS, gE, hS, hE);
				}
				// Maybe this is a parallelism (by using double perpendicularity):
				AlgoElement lAe = l.getParentAlgorithm();
				if (lAe instanceof AlgoOrthoLinePointLine) {
					aolpl = (AlgoOrthoLinePointLine) lAe;
					input = aolpl.getInput();
					l = (GeoLine) input[1];
					gS = l.getStartPoint();
					gE = l.getEndPoint();
					if (gE != null) {
						return parallel(gS, gE, hS, hE);
					}
				}
			}
		}
		return null; // Not yet implemented.
	}

	String perppar(GeoPoint ge1, GeoPoint ge2, GeoPoint ge3, GeoPoint ge4) {
		String ge1l = getUniqueLabel(ge1);
		String ge2l = getUniqueLabel(ge2);
		String ge3l = getUniqueLabel(ge3);
		String ge4l = getUniqueLabel(ge4);

		int i1 = ge1.getConstructionIndex();
		int i2 = ge2.getConstructionIndex();
		int i3 = ge3.getConstructionIndex();
		int i4 = ge4.getConstructionIndex();

		// In a natural order we return the same ordered quadruple:
		if (i1 < i2 && i2 < i3 && i3 < i4)
			return "perppar(" + ge1l + "," + ge2l + "," + ge3l + "," + ge4l + ")";

		// In some reversed orders we return the same ordered quadruple:
		if ((i1 > i3 && i2 > i4) || (i1 > i4 && i2 > i3))
			return "perppar(" + ge3l + "," + ge4l + "," + ge1l + "," + ge2l + ")";

		// Otherwise we return the default order:
		return "perppar(" + ge1l + "," + ge2l + "," + ge3l + "," + ge4l + ")";
	}

	String perppar(GeoLine g, GeoLine h) {
		GeoPoint gS = g.getStartPoint();
		GeoPoint gE = g.getEndPoint();
		GeoPoint hS = h.getStartPoint();
		GeoPoint hE = h.getEndPoint();
		return perppar(gS, gE, hS, hE);
	}

	String isosc(GeoPoint ge1, GeoPoint ge2, GeoPoint ge3) {
		String ge1l = getUniqueLabel(ge1);
		String ge2l = getUniqueLabel(ge2);
		String ge3l = getUniqueLabel(ge3);
		return "isosc(" + ge1l + "," + ge2l + "," + ge3l + ")";
	}

	// |AB|=|CD|
	String equal(GeoPoint A, GeoPoint B, GeoPoint C, GeoPoint D) {
		String Al = getUniqueLabel(A);
		String Bl = getUniqueLabel(B);
		String Cl = getUniqueLabel(C);
		String Dl = getUniqueLabel(D);
		return "isosc(" + Dl + "," + Bl + "+" + Dl + "-" + Al + "," + Cl + ")";
	}

	String eqangle(GeoElement ge1, GeoElement ge2, GeoElement ge3, GeoElement ge4,
			GeoElement ge5, GeoElement ge6) {
		String ge1l = getUniqueLabel(ge1);
		String ge2l = getUniqueLabel(ge2);
		String ge3l = getUniqueLabel(ge3);
		String ge4l = getUniqueLabel(ge4);
		String ge5l = getUniqueLabel(ge5);
		String ge6l = getUniqueLabel(ge6);
		return "eqangle(" + ge1l + "," + ge2l + "," + ge3l + "," + ge4l + "," + ge5l + "," + ge6l + ")";
	}

	String eqanglemul(GeoElement ge1, GeoElement ge2, GeoElement ge3, GeoElement ge4,
			GeoElement ge5, GeoElement ge6, int n) {
		String ge1l = getUniqueLabel(ge1);
		String ge2l = getUniqueLabel(ge2);
		String ge3l = getUniqueLabel(ge3);
		String ge4l = getUniqueLabel(ge4);
		String ge5l = getUniqueLabel(ge5);
		String ge6l = getUniqueLabel(ge6);
		return "eqanglemul(" + ge1l + "," + ge2l + "," + ge3l + "," + ge4l + "," + ge5l + "," + ge6l + "," + n + ")";
	}

	String anglex(GeoElement ge1, GeoElement ge2, GeoElement ge3, GeoElement ge4, long n) {
		String ge1l = getUniqueLabel(ge1);
		String ge2l = getUniqueLabel(ge2);
		String ge3l = getUniqueLabel(ge3);
		String ge4l = getUniqueLabel(ge4);
		return "anglex(" + ge1l + "," + ge2l + "," + ge3l + "," + ge4l + "," + n + ")";
	}

	String online(GeoPoint ge, GeoLine g) {
		GeoPoint gS = g.getStartPoint();
		GeoPoint gE = g.getEndPoint();
		if (gS != null && gE != null) {
			return collinear(gS, gE, ge);
		} else {
			if (gS != null) {
				AlgoElement gAe = g.getParentAlgorithm();
				if (gAe instanceof AlgoAngularBisectorPoints) {
					GeoPoint A = ((AlgoAngularBisectorPoints) gAe).getA();
					GeoPoint B = ((AlgoAngularBisectorPoints) gAe).getB();
					GeoPoint C = ((AlgoAngularBisectorPoints) gAe).getC();
					return eqangle(A, B, ge, ge, B, C);
				} else if (gAe instanceof AlgoLineBisector) {
					GeoPoint A = ((AlgoLineBisector) gAe).getA();
					GeoPoint B = ((AlgoLineBisector) gAe).getB();
					return isosc(ge, A, B);
				} else if (gAe instanceof AlgoLineBisectorSegment) {
					GeoSegment f = ((AlgoLineBisectorSegment) gAe).getSegment();
					GeoPoint A = f.getStartPoint();
					GeoPoint B = f.getEndPoint();
					return isosc(ge, A, B);
				} else if (gAe instanceof AlgoLinePointLine) {
					AlgoLinePointLine alpl = (AlgoLinePointLine) gAe;
					GeoElement[] input = alpl.getInput();
					GeoPoint P = (GeoPoint) input[0];
					GeoLine h = (GeoLine) input[1];
					GeoPoint hS = h.getStartPoint();
					GeoPoint hE = h.getEndPoint();
					return parallel(P, (GeoPoint) ge, hS, hE);
				} else if (gAe instanceof AlgoOrthoLinePointLine) {
					AlgoOrthoLinePointLine aolpl = (AlgoOrthoLinePointLine) gAe;
					GeoElement[] input = aolpl.getInput();
					GeoPoint P = (GeoPoint) input[0];
					GeoLine h = (GeoLine) input[1];
					GeoPoint hS = h.getStartPoint();
					GeoPoint hE = h.getEndPoint();
					if (hE != null) {
						return perppar(P, (GeoPoint) ge, hS, hE);
					}
					AlgoElement hAe = h.getParentAlgorithm();
					if (hAe instanceof AlgoOrthoLinePointLine) {
						AlgoOrthoLinePointLine aolplH = (AlgoOrthoLinePointLine) hAe;
						GeoElement[] inputH = aolplH.getInput();
						GeoLine hEl = (GeoLine) inputH[1];
						hS = (GeoPoint) hEl.getStartPoint();
						hE = (GeoPoint) hEl.getEndPoint();
						return parallel(P, (GeoPoint) ge, hS, hE); // check if not null, TODO
					}
				} else {
					// Not yet implemented.
					return null;
				}
			}
		}
		return null; // Unimplemented.
	}

	String oncircle(GeoPoint ge, GeoConic co) {
		AlgoElement coAe = co.getParentAlgorithm();
		if (coAe instanceof AlgoCircleTwoPoints) {
			AlgoCircleTwoPoints actp = (AlgoCircleTwoPoints) coAe;
			GeoPoint ce = (GeoPoint) actp.getInput(0);
			GeoPoint p = (GeoPoint) actp.getInput(1);
			return isosc(ce, p, ge);
		}
		if (coAe instanceof AlgoCircleThreePoints) {
			AlgoCircleThreePoints actp = (AlgoCircleThreePoints) coAe;
			GeoPoint A = (GeoPoint) actp.getA();
			GeoPoint B = (GeoPoint) actp.getB();
			GeoPoint C = (GeoPoint) actp.getC();
			return concyclic(A, B, C, ge);
		}
		return null; // Unimplemented.
	}

	CNIDefinition equal(GeoElement ge1, GeoElement ge2) {
		CNIDefinition c = new CNIDefinition();;
		if (ge1 instanceof GeoPoint && ge2 instanceof GeoPoint) {
			GeoPoint P = (GeoPoint) ge1;
			GeoPoint Q = (GeoPoint) ge2;
			String Pl = getUniqueLabel(P);
			String Ql = getUniqueLabel(Q);
			c.realRelation = Pl + "-" + Ql;
			c.rMustBe0 = true;
			c.specRestriction = 1; // the second free point cannot be fixed
			return c;
		}
		if (ge1 instanceof GeoSegment && ge2 instanceof GeoSegment) {
			GeoSegment s1 = (GeoSegment) ge1;
			GeoSegment s2 = (GeoSegment) ge2;
			GeoPoint A = (GeoPoint) s1.getStartPoint();
			GeoPoint B = (GeoPoint) s1.getEndPoint();
			GeoPoint C = (GeoPoint) s2.getStartPoint();
			GeoPoint D = (GeoPoint) s2.getEndPoint();
			if (A.equals(C)) {
				c.realRelation = isosc(A,B,D);
				c.warning = WARNING_EQUALITY_OR_COLLINEAR;
				return c;
			}
			if (A.equals(D)) {
				c.realRelation = isosc(A,B,C);
				c.warning = WARNING_EQUALITY_OR_COLLINEAR;
				return c;
			}
			if (B.equals(C)) {
				c.realRelation = isosc(B,A,D);
				c.warning = WARNING_EQUALITY_OR_COLLINEAR;
				return c;
			}
			if (B.equals(D)) {
				c.realRelation = isosc(B,A,C);
				c.warning = WARNING_EQUALITY_OR_COLLINEAR;
				return c;
			}
			// General method (but we do not use it in general, to keep readability):
			c.realRelation = equal(A,B,C,D);
			c.warning = WARNING_EQUALITY_OR_COLLINEAR;
			return c;
		}
		if (ge1 instanceof GeoAngle && ge2 instanceof GeoAngle) {
			GeoAngle a1 = (GeoAngle) ge1;
			GeoAngle a2 = (GeoAngle) ge2;
			AlgoElement ae1 = a1.getParentAlgorithm();
			AlgoElement ae2 = a2.getParentAlgorithm();
			if (ae1 instanceof AlgoAnglePoints && ae2 instanceof AlgoAnglePoints) {
				GeoPoint A = (GeoPoint) ((AlgoAnglePoints) ae1).getA();
				GeoPoint B = (GeoPoint) ((AlgoAnglePoints) ae1).getB();
				GeoPoint C = (GeoPoint) ((AlgoAnglePoints) ae1).getC();
				GeoPoint D = (GeoPoint) ((AlgoAnglePoints) ae2).getA();
				GeoPoint E = (GeoPoint) ((AlgoAnglePoints) ae2).getB();
				GeoPoint F = (GeoPoint) ((AlgoAnglePoints) ae2).getC();
				c.realRelation = eqangle(A,B,C,D,E,F);
				return c;
			}
			return null; // Not yet implemented;
		}
		return null; // Missing implementation for equality of other objects.
	}
}
