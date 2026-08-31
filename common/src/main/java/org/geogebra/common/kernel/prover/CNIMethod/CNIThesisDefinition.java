package org.geogebra.common.kernel.prover.CNIMethod;

import java.util.ArrayList;
import java.util.TreeSet;

import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.algos.AlgoAnglePoints;
import org.geogebra.common.kernel.algos.AlgoDependentBoolean;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.algos.AlgoIntersectLines;
import org.geogebra.common.kernel.arithmetic.ExpressionNode;
import org.geogebra.common.kernel.arithmetic.MySpecialDouble;
import org.geogebra.common.kernel.geos.GeoAngle;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.prover.AlgoAreCollinear;
import org.geogebra.common.kernel.prover.AlgoAreConcurrent;
import org.geogebra.common.kernel.prover.AlgoAreConcyclic;
import org.geogebra.common.kernel.prover.AlgoAreCongruent;
import org.geogebra.common.kernel.prover.AlgoAreEqual;
import org.geogebra.common.kernel.prover.AlgoAreParallel;
import org.geogebra.common.kernel.prover.AlgoArePerpendicular;
import org.geogebra.common.kernel.prover.Constants;
import org.geogebra.common.kernel.prover.Label;
import org.geogebra.common.plugin.Operation;
import org.geogebra.common.util.DoubleUtil;
import org.geogebra.common.util.debug.Log;

public class CNIThesisDefinition {
	private final GiacCommandFactory commandFactory;

	public CNIThesisDefinition(GiacCommandFactory commandFactory) {
		this.commandFactory = commandFactory;
	}

	/**
	 * Create the CNI definition for a GeoElement (for a thesis).
	 * Compute the rhs of the declaration String, the lhs of the real relation String,
	 * and return them to the caller. This method should cover all algos sooner or later.
	 * Now it is just a prototype that implements the CNI method some frequently used algos.
	 * @param ge the input GeoElement
	 * @return all required information for the CNI definition for the input
	 */
	public CNIDefinition create(GeoElement ge) {
		AlgoElement ae = ge.getParentAlgorithm();

		if (ae instanceof AlgoAreCollinear) {
			return handleAAlgoAreCollinear((AlgoAreCollinear)ae);
		}
		if (ae instanceof AlgoAreConcyclic) {
			return handleAlgoAreConcyclic((AlgoAreConcyclic) ae);
		}
		if (ae instanceof AlgoAreParallel) {
			return handleAlgoAreParallel((AlgoAreParallel) ae);
		}
		if (ae instanceof AlgoArePerpendicular) { // in fact, perpendicular or parallel
			return handleAlgoArePerpendicular((AlgoArePerpendicular)ae);
		}
		if (ae instanceof AlgoAreEqual) {
			return handleAlgoAreEqual((AlgoAreEqual) ae);
		}
		if (ae instanceof AlgoAreCongruent) {
			return handleAlgoAreCongruent((AlgoAreCongruent) ae);
		}
		if (ae instanceof AlgoAreConcurrent) {
			return handleAlgoAreConcurrent((AlgoAreConcurrent) ae);
		}
		if (ae instanceof AlgoDependentBoolean) {
			return handleAlgoDependentBoolean((AlgoDependentBoolean) ae);
		}

		// Unimplemented, but it should be handled...
		return null;
	}

	private CNIDefinition handleAAlgoAreCollinear(AlgoAreCollinear aac){
		CNIDefinition c = new CNIDefinition();

		GeoElement[] input = aac.getInput();
		GeoElement A = input[0];
		GeoElement B = input[1];
		GeoElement C = input[2];
		c.realRelation = commandFactory.collinear(A, B, C);
		return c;
	}

	private CNIDefinition handleAlgoAreConcyclic(AlgoAreConcyclic aac){
		CNIDefinition c = new CNIDefinition();

		GeoElement[] input = aac.getInput();
		GeoElement A = input[0];
		GeoElement B = input[1];
		GeoElement C = input[2];
		GeoElement D = input[3];
		c.realRelation = commandFactory.concyclic(A, B, C, D);
		return c;
	}

	private CNIDefinition handleAlgoAreParallel(AlgoAreParallel aap){
		CNIDefinition c = new CNIDefinition();

		GeoElement[] input = aap.getInput();
		GeoLine g = (GeoLine) input[0];
		GeoLine h = (GeoLine) input[1];
		c.realRelation = commandFactory.parallel(g, h);
		return c;
	}

	private CNIDefinition handleAlgoArePerpendicular(AlgoArePerpendicular aap){
		CNIDefinition c = new CNIDefinition();

		Log.debug("Warning: Testing perpendicularity AND parallelism simultaneously");
		GeoElement[] input = aap.getInput();
		GeoLine g = (GeoLine) input[0];
		GeoLine h = (GeoLine) input[1];
		c.realRelation = commandFactory.perppar(g, h);
		c.warning = Constants.WARNING_PERPENDICULAR_OR_PARALLEL;
		return c;
	}

	private CNIDefinition handleAlgoAreEqual(AlgoAreEqual aae) {
		GeoElement[] input = aae.getInput();
		return commandFactory.equal(input[0], input[1]);
	}

	private CNIDefinition handleAlgoAreCongruent(AlgoAreCongruent aac) {
		GeoElement[] input = aac.getInput();
		return commandFactory.equal(input[0], input[1]);
	}

	private CNIDefinition handleAlgoAreConcurrent(AlgoAreConcurrent aac) {
		CNIDefinition c = new CNIDefinition();

		GeoElement[] input = aac.getInput();
		GeoLine l1 = (GeoLine) input[0];
		GeoLine l2 = (GeoLine) input[1];
		GeoLine l3 = (GeoLine) input[2];

		// Define an extra point X as intersection of l1 and l2, and check if it is on l3,
		// unless there is already an intersection of any of them. In that case, use that intersection.
		// For presenting the proof, it is more elegant to use an existing point than
		// creating an auxiliary one.
		GeoPoint X = null;
		Construction cons = l1.getConstruction();
		ArrayList<AlgoElement> ael = cons.getAlgoList();
		int nrAlgos = ael.size();
		for (int i = 0; i < nrAlgos && X == null; i++) {
			AlgoElement a = ael.get(i);
			if (a instanceof AlgoIntersectLines) {
				GeoElement[] inputs = a.getInput();
				TreeSet<GeoElement> ts = new TreeSet<>();
				ts.add(inputs[0]);
				ts.add(inputs[1]);
				if (ts.contains(l1) && ts.contains(l2) || // allow any permutations :-)
						ts.contains(l1) && ts.contains(l3) ||
						ts.contains(l2) && ts.contains(l3)) {
					X = (GeoPoint) a.getOutput(0);
				}
			}
		}
		if (X == null) { // create X because nothing was not found
			AlgoIntersectLines ail = new AlgoIntersectLines(cons, null, l1, l2);
			X = ail.getPoint();
			X.setLabel("X");
		}

		String h1 = commandFactory.online(X, l1);
		String h2 = commandFactory.online(X, l2);
		String t = commandFactory.online(X, l3);
		c.realRelation = h1 + "\n" + h2 + "\n" + t;
		c.extraVariable = Label.makeUnique(X);
		return c;
	}

	private CNIDefinition handleAlgoDependentBoolean(AlgoDependentBoolean adb) {
		CNIDefinition c = new CNIDefinition();

		ExpressionNode en = ((AlgoDependentBoolean) adb).getExpression();
		if (!en.getLeft().isGeoElement() || !en.getRight().isGeoElement()) {
			// Handle some special cases.
			// 2 alpha == beta
			if (en.getOperation() == Operation.EQUAL_BOOLEAN &&
					en.getLeft() instanceof ExpressionNode &&
					((ExpressionNode) en.getLeft()).getOperation() == Operation.MULTIPLY &&
					((ExpressionNode) en.getLeft()).getLeft() instanceof MySpecialDouble &&
					((ExpressionNode) en.getLeft()).getRight() instanceof GeoAngle &&
					en.getRight().isGeoElement() && en.getRight() instanceof GeoAngle) {
				GeoAngle a1 = (GeoAngle) ((ExpressionNode) en.getLeft()).getRightTree()
						.getSingleGeoElement();
				GeoAngle a2 = (GeoAngle) ((ExpressionNode) en.getRightTree()).getSingleGeoElement();
				AlgoElement ae1 = a1.getParentAlgorithm();
				AlgoElement ae2 = a2.getParentAlgorithm();
				double n = ((ExpressionNode) en.getLeft()).getLeft().evaluateDouble();
				int ni = (int) n; // FIXME. If ni is not an integer, this should be an error.
				double EPSILON = 0.00001;
				if (Math.abs(n - ni) > EPSILON) {
					return null; // Not implemented.
				}
				if (ae1 instanceof AlgoAnglePoints && ae2 instanceof AlgoAnglePoints) {
					GeoPoint A = (GeoPoint) ((AlgoAnglePoints) ae1).getA();
					GeoPoint B = (GeoPoint) ((AlgoAnglePoints) ae1).getB();
					GeoPoint C = (GeoPoint) ((AlgoAnglePoints) ae1).getC();
					GeoPoint D = (GeoPoint) ((AlgoAnglePoints) ae2).getA();
					GeoPoint E = (GeoPoint) ((AlgoAnglePoints) ae2).getB();
					GeoPoint F = (GeoPoint) ((AlgoAnglePoints) ae2).getC();
					c.realRelation = commandFactory.eqanglemul(D, E, F, A, B, C, ni);
					return c;
				}
				return null; // Not implemented.
			}
			// alpha == 30 degrees
			if (en.getOperation() == Operation.EQUAL_BOOLEAN &&
					en.getLeft() instanceof GeoAngle &&
					en.getRight() instanceof ExpressionNode &&
					((ExpressionNode) en.getRight()).getOperation() == Operation.MULTIPLY &&
					((ExpressionNode) en.getRight()).getLeft() instanceof MySpecialDouble &&
					((ExpressionNode) en.getRight()).getRight()
							.toString(StringTemplate.giacTemplate).equals("pi/180")) {

				// This is taken from AlgoRotatePoint (Botana's method)
				double angleDoubleVal =
						((MySpecialDouble) (((ExpressionNode) en.getRight()).getLeft())).getDouble();
				if (!DoubleUtil.isInteger(angleDoubleVal)) {
					// unhandled angle, not an integer degree
					return null; // Unimplemented.
				}
				int angleValDeg = (int) angleDoubleVal;
				// Compute the gcd of the angle and 180 degrees. For 90 degrees, this is 90,
				// for 120, this is 60, for 135, this is 45, for example.
				long gcd = Kernel.gcd(angleValDeg, 180);
				// Which power is required to get a real number?
				long rot = Math.abs(180 / gcd); // This is 2 for 90 degrees, 3 for 120 degrees,
				// 4 for 135 (~45) degrees.
				GeoAngle a = (GeoAngle) en.getLeft();
				AlgoElement gae = a.getParentAlgorithm();
				if (gae instanceof AlgoAnglePoints) {
					GeoPoint A = (GeoPoint) ((AlgoAnglePoints) gae).getA();
					GeoPoint B = (GeoPoint) ((AlgoAnglePoints) gae).getB();
					GeoPoint C = (GeoPoint) ((AlgoAnglePoints) gae).getC();
					c.realRelation = commandFactory.anglex(A, B, B, C, rot);
					c.warning = Constants.WARNING_ANGLE;
					return c;
				}
				return null; // Unimplemented.
			}
			return null; // Unimplemented (maybe a sum).
		}
		GeoElement ge1 = en.getLeftTree().getSingleGeoElement();
		GeoElement ge2 = en.getRightTree().getSingleGeoElement();
		Operation o = en.getOperation();
		if (o == Operation.PARALLEL) {
			c.realRelation = commandFactory.parallel((GeoLine) ge1, (GeoLine) ge2);
			return c;
		} else if (o == Operation.PERPENDICULAR) {
			Log.debug("Warning: Testing perpendicularity AND parallelism simultaneously");
			c.realRelation = commandFactory.perppar((GeoLine) ge1, (GeoLine) ge2);
			c.warning = Constants.WARNING_PERPENDICULAR_OR_PARALLEL;
			return c;
		} else if (o == Operation.IS_ELEMENT_OF) {
			if (ge1 instanceof GeoPoint && ge2 instanceof GeoLine) {
				c.realRelation = commandFactory.online((GeoPoint) ge1, (GeoLine) ge2);
				return c;
			}
			if (ge1 instanceof GeoPoint && ge2 instanceof GeoConic && ((GeoConic) ge2).isCircle()) {
				c.realRelation = commandFactory.oncircle((GeoPoint) ge1, (GeoConic) ge2);
				return c;
			}
			return null; // unimplemented
		} else if (o == Operation.EQUAL_BOOLEAN) {
			return commandFactory.equal(ge1, ge2);
		}

		return null;
	}
}
