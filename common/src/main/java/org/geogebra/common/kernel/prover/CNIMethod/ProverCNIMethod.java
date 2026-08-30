package org.geogebra.common.kernel.prover.CNIMethod;


import static org.geogebra.common.cas.giac.CASgiac.ggbGiac;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.geogebra.common.kernel.Construction;
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
import org.geogebra.common.kernel.prover.ProverContext;
import org.geogebra.common.kernel.prover.ProverMethod;
import org.geogebra.common.kernel.scripting.CmdShowProof;
import org.geogebra.common.plugin.Operation;
import org.geogebra.common.util.DoubleUtil;
import org.geogebra.common.util.Prover;
import org.geogebra.common.util.debug.Log;
import org.geogebra.common.util.Prover.ProofResult;



public class ProverCNIMethod implements ProverMethod {
	private final ProverContext context;
	private final Giac giac;
	private final GiacCommandFactory idk;
	
	
	List<String> declarations = new ArrayList<>();
	List<String> realRelations = new ArrayList<>();

	boolean declarative = true;
	boolean rMustBeZero = false;
	int maxSpecRestriction = 0;

	// to avoid that explanations are displayed multiple times
	boolean primedNotationExplained = false;
	boolean algebraicRelationExplained = false;

	public ProverCNIMethod(ProverContext context){
		this.context=context;
		this.giac = new Giac(context.kernel);
		this.idk = new GiacCommandFactory(giac);
	}

	@Override
	public ProofResult execute(){
		// collect r_k definitions to print them in CAS later
		ArrayList<String> toEliminateLhsPrimed = null;
		ArrayList<String> toEliminateRhsVars = null;
		if (context.prover.getShowproof() && context.prover.getShowEliminate()) {
			toEliminateLhsPrimed = new ArrayList<>();  // e.g., ((A'-C')/(A'-O'))/...
			toEliminateRhsVars = new ArrayList<>();  // e.g., r__1, r__2, r__
		}

		// Free points. We need them to eliminate the variables according to them.
		TreeSet<GeoPoint> freePoints = new TreeSet<>();
		// Real-relational points. We need them to eliminate the variables according to them too.
		TreeSet<GeoElement> realRelationalPoints = new TreeSet<>();

		String extraVariables = "";

		String thesisDefinitionPrimed = null;

		if (context.prover.getShowproof()) {
			context.prover.addProofLine(context.loc.getMenuDefault("TheHypotheses", "The hypotheses:"));
		}
		for (GeoPoint ge : context.allPredecessorPoints) {
			if (ge.getParentAlgorithm() == null) {
				freePoints.add(ge);
			} else {
				// We also collect declarative and real-relational definitions.
				CNIDefinition def = null;
				try {
					def = new CNIHypothesisDefinition(idk).create(ge);
				} catch (Exception ex) {
					Log.debug("The CNI method does not yet fully implement " + ge.getParentAlgorithm().toString()
							+ " which is required for " + ge.getLabelSimple());
					return Prover.ProofResult.UNKNOWN;
				}
				if (def == null) {
					Log.debug("The CNI method does not yet implement " + ge.getParentAlgorithm().toString()
							+ " which is required for " + ge.getLabelSimple());
					return Prover.ProofResult.UNKNOWN;
				}

				if (context.prover.getShowproof()) {
					context.prover.addProofLine(context.loc.getPlain("ConsideringDefinitionA",
							ge.getLabelSimple() + " = "
									+ ge.getDefinition(
									StringTemplate.defaultTemplate)));
				}

				if (def.declaration != null) {
					declarations .add(def.declaration);

					if (context.prover.getShowproof()) {
						context.prover.addProofLine(CmdShowProof.TEXT_EQUATION, def.declaration);

						explainPrimedNotation(ge);

						context.prover.addProofLine(CmdShowProof.EQUATION, addPrimesToLabels(def.declaration, context.primeLabels));
					}
				}
				if (def.zeroRelation != null) {
					realRelations.add(def.zeroRelation);

					if (context.prover.getShowproof()) {
						context.prover.addProofLine(CmdShowProof.TEXT_EQUATION, def.zeroRelation + "=0");
					}
				}
				if (def.extraVariable != null) {
					extraVariables = def.extraVariable + ",";
				}
				if (def.realRelation != null) {
					String[] CASrealRelations = def.realRelation.split("\n");
					for (String CASrealRelation : CASrealRelations) {
						String expression = CASrealRelation + "=" + VARIABLE_R_STRING + realRelations.size();
						realRelations.add(expression);
						if (context.prover.getShowproof()) {
							String rewriteProgram = "[" + context.predefs + expression + "][" + context.predefinitions.length + "]";
							String expression2 = giac.execute(rewriteProgram);
							context.prover.addProofLine(CmdShowProof.TEXT_EQUATION, lhs(expression) + "=" + expression2
									+ com.himamis.retex.editor.share.util.Unicode.IS_ELEMENT_OF + "\u211D");

							explainAlgebricNotation();

							String rk = VARIABLE_R_STRING + realRelations.size(); // e.g., r__1
							String lhsProgram = giac.execute("lhs(" + expression2 + ")");
							String lhs2 = addPrimesToLabels(lhsProgram, context.primeLabels);

							if (toEliminateLhsPrimed != null) {
								toEliminateLhsPrimed.add(lhs2);
								toEliminateRhsVars.add(rk);
							}

							context.prover.addProofLine(CmdShowProof.EQUATION, rk + PRIME + ":=" + lhs2);
						}
					}
					if (def.warning == WARNING_PERPENDICULAR_OR_PARALLEL) {
						context.prover.addProofLine(CmdShowProof.PROBLEM, context.loc.getMenuDefault("PerpendicularityParallelism",
								"Perpendicularity means perpendicularity or parallelism simultaneously."));
					}
					if (def.warning == WARNING_EQUALITY_OR_COLLINEAR) {
						context.prover.addProofLine(CmdShowProof.PROBLEM, context.loc.getMenuDefault("EqualityCollinearity",
								"Equality of lengths means equality or collinearity simultaneously."));
					}
					realRelationalPoints.add(ge);
					declarative = false;
				}
				if (def.declaration == null && def.realRelation == null && def.zeroRelation == null) {
					Log.debug("The CNI method does not yet implement " + ge.getParentAlgorithm().toString()
							+ " which is required for " + ge.getLabelSimple());
					return Prover.ProofResult.UNKNOWN;
				}
			}
		}

		// Adding the thesis. This is very similar to the code above:
		CNIDefinition def = null;
		try {
			def = getCNIThesisDefinition(context.statement);
		} catch (Exception e) {
			Log.debug("The CNI method does not yet fully implement " + context.statement.getParentAlgorithm().toString());
			return Prover.ProofResult.UNKNOWN;
		}
		if (def == null) {
			Log.debug("The CNI method does not yet implement " + context.statement.getParentAlgorithm().toString());
			return Prover.ProofResult.UNKNOWN;
		}
		if (context.prover.getShowproof()) {
			context.prover.addProofLine(context.loc.getMenuDefault("TheThesis", "The thesis:"));
			context.prover.addProofLine(context.statement.getParentAlgorithm().getDefinition(StringTemplate.defaultTemplate));
		}
		if (def.declaration != null) {
			declarations.add(def.declaration);
			if (context.prover.getShowproof()) {
				context.prover.addProofLine(CmdShowProof.TEXT_EQUATION, def.declaration);
				context.prover.addProofLine(CmdShowProof.EQUATION, addPrimesToLabels(def.declaration, context.primeLabels));
			}
		}

		if (def.extraVariable != null) {
			extraVariables = def.extraVariable + ",";
		}

		if (def.realRelation != null) {

			String[] CASrealRelations = def.realRelation.split("\n");
			int nrRels = CASrealRelations.length;

			// It's possible that there are multiple relations. In this case we append the
			// first ones to the hypotheses and keep only the last one for the thesis.
			// A typical application is AreConcurrent.
			for (int i = 0; i < nrRels - 1; i++) {
				String CASrealRelation = CASrealRelations[i];
				String expression = CASrealRelation + "=" + VARIABLE_R_STRING + realRelations.size();
				realRelations.add(expression);
				if (context.prover.getShowproof()) {
					String rewriteProgram = "[" + context.predefs + expression + "][" + context.predefinitions.length + "]";
					String expression2 = giac.execute(rewriteProgram);

					context.prover.addProofLine(CmdShowProof.TEXT_EQUATION, lhs(expression) + "=" + expression2
							+ com.himamis.retex.editor.share.util.Unicode.IS_ELEMENT_OF + "\u211D");

					String rk = VARIABLE_R_STRING + realRelations.size(); // e.g., r__1
					String lhsProgram = giac.execute("lhs(" + expression2 + ")");
					String lhs2 = addPrimesToLabels(lhsProgram, context.primeLabels);

					if (toEliminateLhsPrimed != null) {
						toEliminateLhsPrimed.add(lhs2);
						toEliminateRhsVars.add(rk);
					}

					context.prover.addProofLine(CmdShowProof.EQUATION, rk + PRIME + ":=" + lhs2);
				}
			}
			String thesis = CASrealRelations[nrRels - 1] + "=" + VARIABLE_R_STRING;
			realRelations.add(thesis);
			if (context.prover.getShowproof()) {
				String rewriteProgram = "[" + context.predefs + thesis + "][" + context.predefinitions.length + "]";
				String thesis2 = giac.execute(rewriteProgram);
				context.prover.addProofLine(CmdShowProof.TEXT_EQUATION, lhs(thesis) + "=" + thesis2);

				String lhsProgram = giac.execute("lhs(" + thesis2 + ")");
				String lhs2 = addPrimesToLabels(lhsProgram, context.primeLabels);

				if (toEliminateLhsPrimed != null) {
					toEliminateLhsPrimed.add(lhs2);
					toEliminateRhsVars.add(VARIABLE_R_STRING);
				}

				thesisDefinitionPrimed = lhs2;

				context.prover.addProofLine(context.loc.getPlainDefault(
						"CNIThesisAlgebraicForm",
						"We now turn the thesis into an algebraic expression. The symbol %0 stands for this expression:",
						VARIABLE_R_STRING + PRIME));
				context.prover.addProofLine(CmdShowProof.EQUATION, VARIABLE_R_STRING + PRIME + ":=" + lhs2);

				if (def.warning == WARNING_PERPENDICULAR_OR_PARALLEL) {
					context.prover.addProofLine(CmdShowProof.PROBLEM, context.loc.getMenuDefault("PerpendicularityParallelism",
							"Perpendicularity means perpendicularity or parallelism simultaneously"));
				}
				if (def.warning == WARNING_EQUALITY_OR_COLLINEAR) {
					context.prover.addProofLine(CmdShowProof.PROBLEM,
							context.loc.getMenuDefault("EqualityCollinearity",
									"Equality of lengths means equality or collinearity simultaneously."));
				}
				if (def.warning == WARNING_ANGLE) {
					context.prover.addProofLine(CmdShowProof.PROBLEM,
							context.loc.getMenuDefault("AngleAmbiguity",
									"Angle equality means equality or equality to another specific angle simultaneously."));
				}
				if (def.specRestriction > 0 && def.specRestriction > maxSpecRestriction) {
					maxSpecRestriction = def.specRestriction;
				}
			}
		}
		if (def.declaration == null && def.realRelation == null) {
			Log.debug("The CNI method does not yet fully implement " + context.statement.getParentAlgorithm().toString());
			return Prover.ProofResult.UNKNOWN;
		}
		if (def.rMustBe0) {
			rMustBeZero = true;
		}

		// Specialization.
		if (context.prover.getShowproof()) {
			context.prover.addProofLine(CmdShowProof.SPECIALIZATION, context.loc.getMenuDefault("WlogCoordinates",
					"Without loss of generality, some coordinates can be fixed:"));
		}
		// Put the first two points into 0 and 1:
		int i = 0;
		TreeSet<GeoElement> specialized = new TreeSet<>();
		List<String> specCode = new ArrayList<>();
		ArrayList<String> specEqList = new ArrayList<>();
		for (GeoElement ge : freePoints) {
			if (i == 0 && maxSpecRestriction < 2) {
				String spec1 = getUniqueLabel(ge) + ":=0";
				if (context.prover.getShowproof() && context.prover.getShowEliminate()) {
					specEqList.add(getUniqueLabel(ge) + PRIME + "=0");
				}
				specCode.add(spec1);
				if (context.prover.getShowproof()) {
					context.prover.addProofLine(CmdShowProof.TEXT_EQUATION, spec1);
				}
				specialized.add(ge);
			}
			if (i == 1 && maxSpecRestriction < 1) {
				String spec2 = getUniqueLabel(ge) + ":=1";
				if (context.prover.getShowproof() && context.prover.getShowEliminate()) {
					specEqList.add(getUniqueLabel(ge) + PRIME + "=1");
				}
				specCode.add(spec2);
				if (context.prover.getShowproof()) {
					context.prover.addProofLine(CmdShowProof.TEXT_EQUATION, spec2);
				}
				specialized.add(ge);
			}
			i++;
		}
		declarations.addAll(0, specCode) ;// Prepend specializations before declarations.
		freePoints.removeAll(specialized);
		// These will be no longer free points.

		// Putting the code together...
		String program = "";
		program = "[";
		program += context.predefs;
		for (String declaration : declarations) {
			program += "[" + declaration + "],";
		}
		program += "[" + VARIABLE_I_STRING + ":=eliminate([" + String.join(",", realRelations);
		String program1 = program; // first program stored, later it may be required with an edit
		String rest = "";
		rest += "],";
		String toEliminate = "";
		for (GeoElement ge : freePoints) {
			toEliminate += getUniqueLabel(ge) + ",";
		}
		for (GeoElement ge : realRelationalPoints) {
			toEliminate += getUniqueLabel(ge) + ",";
		}
		toEliminate += extraVariables;
		toEliminate = removeTail(toEliminate, 1);
		rest += "[" + toEliminate + "]";

		// Add third parameter for the eliminate command.
		// It specifies the variable ordering for the remaining variables (that are not eliminated).
		// Since August 2026. This is available only in the newer Giac version (since February 2026).
		String remVars = ",revlist([";
		if (toEliminateRhsVars != null) {
			boolean first = true;
			for (String v : toEliminateRhsVars) {
				if (!first) {
					remVars += ",";
				} else {
					first = false;
				}
				remVars += v;
			}
		}
		remVars += "])";
		rest += remVars;

		rest += ")]";
		int codeLengthLines = context.predefinitions.length + declarations.size() + 1;
		rest += "][" + (codeLengthLines - 1) + "]";
		program += rest;
		String elimIdeal = giac.execute(program);
		// This is in form {{4*r_1*r_2*r_-4*r_1*r_2-4*r_1*r_-4*r_2*r_+3*r_1+3*r_2+3*r_}}
		// or there may be multiple polynomials in the form {{...,...,...}}

		if (context.prover.getShowproof()) {
			context.prover.addProofLine(context.loc.getMenuDefault("EliminateAllFreeVariables",
					"We eliminate all variables that correspond to free points."));

			if (context.prover.getShowEliminate()) {

				context.prover.addProofLine(context.loc.getMenuDefault("CNIEliminateCommandInfo",
						"The next command does this elimination. It removes the free-point coordinates and keeps only the relation between the hypotheses and the thesis:"));

				String ggbEliminateCommand = buildGeoGebraEliminateCommand(
						toEliminateLhsPrimed,
						toEliminateRhsVars,
						context.primeLabels,
						extraVariables,
						null,
						specEqList
				);

				context.prover.addProofLine(CmdShowProof.EQUATION, ggbEliminateCommand);
			}

		}


		if (elimIdeal.equals("{{}}")) {
			// There is no direct correspondence between r1, r2, ..., and r.
			// The statement is quite probably false, but we cannot explicitly state this.
			if (context.prover.getShowproof()) {
				context.prover.addProofLine(CmdShowProof.PROBLEM,
						context.loc.getMenuDefault("NoCorrespondenceBetweenHypothesesThesis",
								"There is no correspondence between the hypotheses and the thesis."));
			}
			Log.debug("The elimination ideal is <0>, no conclusion.");
			return Prover.ProofResult.UNKNOWN;
		}

		// There is a direct correspondence.
		String elimIdealL = removeHeadTail(elimIdeal, 1).
				replace("{", "[").replace("}", "]"); // remove { and }
		// Now we choose the minimal degree polynomial (in r) of this list.
		program = "[[" + VARIABLE_I_STRING + ":= " + elimIdealL + "],[deg:=inf],[degi:=0],"
				+ "[for (k:=0;k<size(" + VARIABLE_I_STRING + ");k++) { d:=degree(" + VARIABLE_I_STRING
				+ "[k]," + VARIABLE_R_STRING + ");"
				+ "if (d>0 && d<deg) { deg:=d; degi:=k; } }],"
				+ "[deg," + VARIABLE_I_STRING + "[degi]]][4]";
		program = ggbGiac(program);
		String minDegree = giac.execute(program);
		// The result is in form: {1,4*r_1*r_2*r_-4*r_1*r_2-4*r_1*r_-4*r_2*r_+3*r_1+3*r_2+3*r_}

		String minDegreeC = removeHeadTail(minDegree,1); // remove { and }
		String[] minDegreeA = minDegreeC.split(","); // Separate items
		if (minDegreeA[0].equals("+infinity")) {
			// r cannot be expressed, the statement is probably false...
			if (context.prover.getShowproof()) {
				context.prover.addProofLine(CmdShowProof.PROBLEM,
						context.loc.getMenuDefault("ThesisCannotBeExpressed",
								"The thesis cannot be expressed with the hypotheses."));
			}
			Log.debug("The elimination ideal does not contain r_.");
			return Prover.ProofResult.UNKNOWN;
		}
		int minDegreeI = Integer.valueOf(minDegreeA[0]);
		if (minDegreeI == 1) {
			// r can be expressed by using r1, r2, ..., here r is linear.
			if (context.prover.getShowproof()) {
				context.prover.addProofLine(context.loc.getPlainDefault("ThesisACanBeExpressedAsRationalBecauseALinear",
						"The thesis (%0) can be expressed as a rational expression of the hypotheses, because %0 is"
								+ " linear in the following polynomial equation:", VARIABLE_R_STRING));
				context.prover.addProofLine(minDegreeA[1] + "=0");

				// r is linear: a * r + b = 0
				// rExpr = -b/a, with coeff(poly,r)[0] = a und coeff(poly,r)[1] = b
				String poly = minDegreeA[1];

				String rExpr = giac.execute(
						"-(coeff(" + poly + "," + VARIABLE_R_STRING + ")[1])"
								+ "/(coeff(" + poly + "," + VARIABLE_R_STRING + ")[0])");

				// prime the r expression
				String rExprPrimed = addPrimesToRVariables(rExpr, VARIABLE_R_STRING);

				String simplifiedRExpr = giac.execute("simplify(" + rExprPrimed + ")");
				String simplifiedThesis = null;
				if (thesisDefinitionPrimed != null) {
					simplifiedThesis = giac.execute("simplify(" + thesisDefinitionPrimed + ")");
				}

				context.prover.addProofLine(context.loc.getMenuDefault("CNISimplifyBoth",
						"We now simplify both expressions. This makes them easier to compare:"));

				context.prover.addProofLine(CmdShowProof.EQUATION,
						VARIABLE_R_STRING + PRIME + PRIME + ":=" + rExprPrimed);
				context.prover.addProofLine(CmdShowProof.EQUATION, "Simplify(" + rExprPrimed + ")");

				if (thesisDefinitionPrimed != null) {
					context.prover.addProofLine(CmdShowProof.EQUATION,
							"Simplify(" + thesisDefinitionPrimed + ")");
				}

				// if it simplifies to a number => inform user that this is not a mistake
				if (isNumericConstant(simplifiedRExpr)) {
					if (simplifiedThesis != null && simplifiedRExpr.equals(simplifiedThesis)) {
						context.prover.addProofLine(context.loc.getMenuDefault("CNISimplifiedSameNumber",
								"Both simplified expressions are the same number. So the result matches the thesis."));
					} else {
						context.prover.addProofLine(context.loc.getMenuDefault("CNISimplifiedToNumber",
								"The expression simplifies to a fixed number. This means the hypotheses already determine its value."));
					}
				} else if (thesisDefinitionPrimed != null) {
					context.prover.addProofLine(context.loc.getMenuDefault("CNISimplifiedEqualThesis",
							"If the simplified expressions are the same, then the result matches the thesis."));
				}

			}
			Log.debug("The elimination ideal contains " + minDegreeA[1] + ", it is linear in r_.");
			// Check if r can be expressed without a division:
			// lvar(coeff(2*r_+1,r_)[0])
			program = "lvar(coeff(" + minDegreeA[1] + "," + VARIABLE_R_STRING + ")[0])";
			String divVars = giac.execute(program);
			if (divVars.equals("{}")) {
				if (rMustBeZero) {
					if (minDegreeA[1].equals(VARIABLE_R_STRING) ||
							minDegreeA[1].equals("-" + VARIABLE_R_STRING)) {
						if (context.prover.getShowproof()) {
							context.prover.addProofLine(CmdShowProof.CONCLUSION,
									context.loc.getMenuDefault("ThesisZeroStatementTrue",
											"Since the thesis is zero, the statement is true."));
						}
						Log.debug("r_ is zero.");
						return Prover.ProofResult.TRUE;
					}
					if (context.prover.getShowproof()) {
						context.prover.addProofLine(CmdShowProof.PROBLEM,
								context.loc.getMenuDefault("ThesisShouldBeZero",
										"Since the thesis is not zero, the statement cannot be proven."));
					}
					Log.debug("r_ should be zero.");
					return Prover.ProofResult.UNKNOWN; // maybe here we can result FALSE?
				}
				if (context.prover.getShowproof()) {
					context.prover.addProofLine(context.loc.getMenuDefault("ThesisCanBeExpressedPolynomial",
							"The thesis can be expressed as a polynomial expression of the hypotheses."));
					context.prover.addProofLine(CmdShowProof.CONCLUSION,
							context.loc.getMenuDefault("HypothesesRealThesisReal",
									"Since all hypotheses are real expressions, the thesis must also be real."));
				}
				return Prover.ProofResult.TRUE;
			}
			// Read off the divisor when expressing r:
			program = "coeff(" + minDegreeA[1] + "," + VARIABLE_R_STRING + ")[0])";
			String divisor = giac.execute(program);
			if (context.prover.getShowproof()) {
				context.prover.addProofLine(
						context.loc.getPlainDefault("SolvingForARequiresDivByB",
								"Solving for %0 requires a division by %1.",
								new String[]{VARIABLE_R_STRING, divisor}));
				context.prover.addProofLine(context.loc.getMenuDefault("AssumeDivisorZero",
						"Let us assume that this divisor is 0 and restart the elimination."));
			}
			// Insert the divisor in the first program and check what happens:
			program = program1 + "," + divisor + rest;
			String elimIdeal2 = giac.execute(program);

			if(context.prover.getShowproof() && context.prover.getShowEliminate()) {
				context.prover.addProofLine(context.loc.getMenuDefault("CNIEliminateCommandInfoDivisor",
						"The next command repeats the elimination with the extra assumption divisor = 0. It checks whether this case is possible:"));

				String ggbEliminateCommand = buildGeoGebraEliminateCommand(
						toEliminateLhsPrimed,
						toEliminateRhsVars,
						context.primeLabels,
						extraVariables,
						divisor,
						specEqList
				);

				context.prover.addProofLine(CmdShowProof.EQUATION, ggbEliminateCommand);

			}

			if (elimIdeal2.equals("{{1}}")) {
				// The case divisor == 0 is contradictory. This means that division by zero
				// is not a relevant issue, so we can be sure that the statement is true.
				if (context.prover.getShowproof()) {
					context.prover.addProofLine(context.loc.getMenuDefault("DivisorCannotBeZero",
							"The elimination verifies that this divisor cannot be zero."));
				}
				Log.debug("Division by zero is irrelevant.");
				if (rMustBeZero) {
					if (context.prover.getShowproof()) {
						context.prover.addProofLine(CmdShowProof.PROBLEM,
								context.loc.getMenuDefault("ThesisShouldBeZero",
										"Since the thesis is not zero, the statement cannot be proven."));
					}
					Log.debug("r_ should be zero.");
					return Prover.ProofResult.UNKNOWN; // maybe here we can result FALSE?
				}
				if (context.prover.getShowproof()) {
					context.prover.addProofLine(CmdShowProof.CONCLUSION,
							context.loc.getMenuDefault("HypothesesRealThesisReal",
									"Since all hypotheses are real expressions, the thesis must also be real."));
				}
				return Prover.ProofResult.TRUE;
			}

			// There is direct correspondence between r1, r2, ..., and r.
			String elimIdeal2L = removeHeadTail(elimIdeal2, 1).
					replace("{", "[").replace("}", "]"); // remove { and }
			// Now we choose the minimal degree polynomial (in r) of this list.
			program = "[[" + VARIABLE_I_STRING + ":= " + elimIdeal2L + "],[deg:=inf],[degi:=0],"
					+ "[for (k:=0;k<size(" + VARIABLE_I_STRING + ");k++) { d:=degree(" + VARIABLE_I_STRING
					+ "[k]," + VARIABLE_R_STRING + ");"
					+ "if (d>0 && d<deg) { deg:=d; degi:=k; } }],"
					+ "[deg," + VARIABLE_I_STRING + "[degi]]][4]";
			program = ggbGiac(program);
			String minDegree2 = giac.execute(program);
			// The result is in form: {1,4*r_1*r_2*r_-4*r_1*r_2-4*r_1*r_-4*r_2*r_+3*r_1+3*r_2+3*r_}

			String minDegree2C = removeHeadTail(minDegree2,1); // remove { and }
			String[] minDegree2A = minDegree2C.split(","); // Separate items
			if (minDegree2A[0].equals("+infinity")) {
				// r cannot be expressed, the statement is probably false...
				if (context.prover.getShowproof()) {
					context.prover.addProofLine(CmdShowProof.PROBLEM,
							context.loc.getMenuDefault("AssumingZeroThesisCannotBeExpressed",
									"Assuming that this is zero, the thesis cannot be expressed with the hypotheses."));
				}
				Log.debug("The second elimination ideal does not contain r_.");
				return Prover.ProofResult.UNKNOWN;
			}
			int minDegree2I = Integer.valueOf(minDegree2A[0]);
			if (minDegree2I == 1) {
				// The secondly computed ideal is linear.
				if (context.prover.getShowproof()) {
					context.prover.addProofLine(context.loc.getPlainDefault("ThesisACanBeExpressedNowAsRationalBecauseALinear",
							"The thesis (%0) can now be expressed as a rational expression of the hypotheses, because %0 is"
									+ " linear in the following polynomial equation:", VARIABLE_R_STRING));
					context.prover.addProofLine(minDegree2A[1] + "=0");

					String poly2 = minDegree2A[1];
					String rExpr2 = giac.execute(
							"-(coeff(" + poly2 + "," + VARIABLE_R_STRING + ")[1])"
									+ "/(coeff(" + poly2 + "," + VARIABLE_R_STRING + ")[0])");

					String rExpr2Primed = addPrimesToRVariables(rExpr2, VARIABLE_R_STRING);

					String simplifiedRExpr2 = giac.execute("simplify(" + rExpr2Primed + ")");
					String simplifiedThesis2 = null;
					if (thesisDefinitionPrimed != null) {
						simplifiedThesis2 = giac.execute("simplify(" + thesisDefinitionPrimed + ")");
					}

					context.prover.addProofLine(context.loc.getMenuDefault("CNISimplifyBoth",
							"We now simplify both expressions. This makes them easier to compare:"));

					context.prover.addProofLine(CmdShowProof.EQUATION, VARIABLE_R_STRING + PRIME + PRIME + ":=" + rExpr2Primed);
					context.prover.addProofLine(CmdShowProof.EQUATION, "Simplify(" + rExpr2Primed + ")");

					if (thesisDefinitionPrimed != null) {
						context.prover.addProofLine(CmdShowProof.EQUATION, "Simplify(" + thesisDefinitionPrimed + ")");
					}

					if (isNumericConstant(simplifiedRExpr2)) {
						if (simplifiedThesis2 != null && simplifiedRExpr2.equals(simplifiedThesis2)) {
							context.prover.addProofLine(context.loc.getMenuDefault("CNISimplifiedSameNumber",
									"Both simplified expressions are the same number. So the result matches the thesis."));
						} else {
							context.prover.addProofLine(context.loc.getMenuDefault("CNISimplifiedToNumber",
									"The expression simplifies to a fixed number. This means the hypotheses already determine its value."));
						}
					} else if (thesisDefinitionPrimed != null) {
						context.prover.addProofLine(context.loc.getMenuDefault("CNISimplifiedEqualThesis",
								"If the simplified expressions are the same, then the result matches the thesis."));
					}

				}
				Log.debug("The second elimination ideal contains " + minDegree2A[1] + ", it is linear in r_.");
				// Check if r can be expressed without a division:
				// lvar(coeff(2*r_+1,r_)[0])
				program = "lvar(coeff(" + minDegree2A[1] + ","+ VARIABLE_R_STRING + ")[0])";
				String divVars2 = giac.execute(program);
				if (divVars2.equals("{}")) {
					if (rMustBeZero) {
						if (minDegree2A[1].equals(VARIABLE_R_STRING) || minDegree2A[1].equals("-" + VARIABLE_R_STRING)) {
							if (context.prover.getShowproof()) {
								context.prover.addProofLine(CmdShowProof.CONCLUSION,
										context.loc.getMenuDefault("ThesisZeroStatementTrue",
												"Since the thesis is zero, the statement is true."));
							}
							Log.debug("r_ is zero.");
							return Prover.ProofResult.TRUE;
						}
						if (context.prover.getShowproof()) {
							context.prover.addProofLine(CmdShowProof.PROBLEM,
									context.loc.getMenuDefault("ThesisShouldBeZeroNow",
											"Since the thesis is not zero now, the statement cannot be proven."));
						}
						Log.debug("r_ should be zero.");
						return Prover.ProofResult.UNKNOWN; // maybe here we can result FALSE?
					}
					if (context.prover.getShowproof()) {
						context.prover.addProofLine(context.loc.getMenuDefault("NowThesisCanBeExpressedPolynomial",
								"Now the thesis can be expressed as a polynomial expression of the hypotheses."));
						context.prover.addProofLine(CmdShowProof.CONCLUSION,
								context.loc.getMenuDefault("HypothesesRealThesisReal",
										"Since all hypotheses are real expressions, the thesis must also be real."));
					}
					return Prover.ProofResult.TRUE;
				}
				// Cannot decide, maybe we need another round? TODO
				if (context.prover.getShowproof()) {
					context.prover.addProofLine(CmdShowProof.PROBLEM,
							context.loc.getMenuDefault("ThesisStillContainsDivision",
									"The thesis still contains a division, no conclusion can be found."));
				}
				Log.debug("Another division occurred, a third elimination is needed.");
				return Prover.ProofResult.UNKNOWN;
			}
			// The division does not result in an unambiguous case.
			if (context.prover.getShowproof()) {
				context.prover.addProofLine(CmdShowProof.PROBLEM,
						context.loc.getMenuDefault("ThesisCannotBeExpressedDivision",
								"The thesis cannot be expressed as a division.")); // +now?
			}
			Log.debug("The division does not result in an unambiguous case.");
			return Prover.ProofResult.UNKNOWN;
		}
		// The case is not linear.
		if (context.prover.getShowproof()) {
			context.prover.addProofLine(CmdShowProof.PROBLEM,
					context.loc.getMenuDefault("ThesisCannotBeExpressedDivision",
							"The thesis cannot be expressed as a division."));
		}
		Log.debug("r_ is not linear, further check is needed.");

		// Maybe the case is quadratic.
		if (minDegreeI == 2) {
			Log.debug("r_ is quadratic.");
			program = "[[D:=discriminant(" + minDegreeA[1] + "," + VARIABLE_R_STRING + ")],[total_degree(D,lvar(D))]][1]";
			String discDegreeL = giac.execute(program);
			String discDegreeS = removeHeadTail(discDegreeL, 1);
			int discDegree = Integer.parseInt(discDegreeS);
			Log.debug("The degree of the discriminant is " + discDegree);
			if (discDegree > 2) {
				Log.debug("No method can be directly applied to detect positivity.");
			} else {
				Log.debug("There is hope to detect positivity.");
			}
		}

		return Prover.ProofResult.UNKNOWN;
	}

	private void explainPrimedNotation(GeoPoint ge){
		if (!primedNotationExplained) {
			String exampleLabel = getUniqueLabel(ge);
			context.prover.addProofLine(context.loc.getPlainDefault("CNIPrimedSymbols",
					"Denote point %0 by %1 in a symbolic manner.",
					exampleLabel, exampleLabel + PRIME));
			primedNotationExplained = true;
		}
	}

	private void explainAlgebricNotation(){
		if (!algebraicRelationExplained) {
			context.prover.addProofLine(context.loc.getPlainDefault("CNIAlgebraicRelations",
					"We now turn geometric relations into algebraic expressions. The symbols %0, %1, ... stand for these expressions:",
					VARIABLE_R_STRING + "1'",
					VARIABLE_R_STRING + "2'"));
			algebraicRelationExplained = true;
		}
	}




	/** Create the CNI definition for a GeoElement (for a thesis).
	 * Compute the rhs of the declaration String, the lhs of the real relation String,
	 * and return them to the caller. This method should cover all algos sooner or later.
	 * Now it is just a prototype that implements the CNI method some frequently used algos.
	 *
	 * @param ge the input GeoElement
	 * @return all required information for the CNI definition for the input
	 */
	CNIDefinition getCNIThesisDefinition(GeoElement ge) {
		CNIDefinition c = new CNIDefinition();
		AlgoElement ae = ge.getParentAlgorithm();
		if (ae instanceof AlgoAreCollinear) {
			AlgoAreCollinear aac = (AlgoAreCollinear) ae;
			GeoElement[] input = aac.getInput();
			GeoElement A = input[0];
			GeoElement B = input[1];
			GeoElement C = input[2];
			c.realRelation = idk.collinear(A, B, C);
			return c;
		}
		if (ae instanceof AlgoAreConcyclic) {
			AlgoAreConcyclic aac = (AlgoAreConcyclic) ae;
			GeoElement[] input = aac.getInput();
			GeoElement A = input[0];
			GeoElement B = input[1];
			GeoElement C = input[2];
			GeoElement D = input[3];
			c.realRelation = idk.concyclic(A, B, C, D);
			return c;
		}
		if (ae instanceof AlgoAreParallel) {
			AlgoAreParallel aap = (AlgoAreParallel) ae;
			GeoElement[] input = aap.getInput();
			GeoLine g = (GeoLine) input[0];
			GeoLine h = (GeoLine) input[1];
			c.realRelation = idk.parallel(g, h);
			return c;
		}
		if (ae instanceof AlgoArePerpendicular) { // in fact, perpendicular or parallel
			Log.debug("Warning: Testing perpendicularity AND parallelism simultaneously");
			AlgoArePerpendicular aap = (AlgoArePerpendicular) ae;
			GeoElement[] input = aap.getInput();
			GeoLine g = (GeoLine) input[0];
			GeoLine h = (GeoLine) input[1];
			c.realRelation = idk.perppar(g, h);
			c.warning = WARNING_PERPENDICULAR_OR_PARALLEL;
			return c;
		}
		if (ae instanceof AlgoAreEqual) {
			AlgoAreEqual aae = (AlgoAreEqual) ae;
			GeoElement[] input = aae.getInput();
			return idk.equal(input[0], input[1]);
		}
		if (ae instanceof AlgoAreCongruent) {
			AlgoAreCongruent aac = (AlgoAreCongruent) ae;
			GeoElement[] input = aac.getInput();
			return idk.equal(input[0], input[1]);
		}
		if (ae instanceof AlgoAreConcurrent) {
			AlgoAreConcurrent aac = (AlgoAreConcurrent) ae;
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

			String h1 = idk.online(X, l1);
			String h2 = idk.online(X, l2);
			String t = idk.online(X, l3);
			c.realRelation = h1 + "\n" + h2 + "\n" + t;
			c.extraVariable = getUniqueLabel(X);
			return c;
		}
		if (ae instanceof AlgoDependentBoolean) {
			ExpressionNode en = ((AlgoDependentBoolean) ae).getExpression();
			if (!en.getLeft().isGeoElement() || !en.getRight().isGeoElement()) {
				// Handle some special cases.
				// 2 alpha == beta
				if (en.getOperation() == Operation.EQUAL_BOOLEAN &&
						en.getLeft() instanceof ExpressionNode &&
						((ExpressionNode) en.getLeft()).getOperation() == Operation.MULTIPLY &&
						((ExpressionNode) en.getLeft()).getLeft() instanceof MySpecialDouble &&
						((ExpressionNode) en.getLeft()).getRight() instanceof GeoAngle &&
						en.getRight().isGeoElement() && en.getRight() instanceof GeoAngle) {
					GeoAngle a1 = (GeoAngle) ((ExpressionNode) en.getLeft()).getRightTree().getSingleGeoElement();
					GeoAngle a2 = (GeoAngle) ((ExpressionNode) en.getRightTree()).getSingleGeoElement();
					AlgoElement ae1 = a1.getParentAlgorithm();
					AlgoElement ae2 = a2.getParentAlgorithm();
					double n = ((ExpressionNode) en.getLeft()).getLeft().evaluateDouble();
					int ni = (int) n; // FIXME. If ni is not an integer, this should be an error.
					double EPSILON = 0.00001;
					if (Math.abs(n-ni) > EPSILON) {
						return null; // Not implemented.
					}
					if (ae1 instanceof AlgoAnglePoints && ae2 instanceof AlgoAnglePoints) {
						GeoPoint A = (GeoPoint) ((AlgoAnglePoints) ae1).getA();
						GeoPoint B = (GeoPoint) ((AlgoAnglePoints) ae1).getB();
						GeoPoint C = (GeoPoint) ((AlgoAnglePoints) ae1).getC();
						GeoPoint D = (GeoPoint) ((AlgoAnglePoints) ae2).getA();
						GeoPoint E = (GeoPoint) ((AlgoAnglePoints) ae2).getB();
						GeoPoint F = (GeoPoint) ((AlgoAnglePoints) ae2).getC();
						c.realRelation = idk.eqanglemul(D,E,F,A,B,C,ni);
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
						((ExpressionNode) en.getRight()).getRight().toString(StringTemplate.giacTemplate).equals("pi/180")) {

					// This is taken from AlgoRotatePoint (Botana's method)
					double angleDoubleVal = ((MySpecialDouble) (((ExpressionNode) en.getRight()).getLeft())).getDouble();
					if (!DoubleUtil.isInteger(angleDoubleVal)) {
						// unhandled angle, not an integer degree
						return null; // Unimplemented.
					}
					int angleValDeg = (int) angleDoubleVal;
					// Compute the gcd of the angle and 180 degrees. For 90 degrees, this is 90,
					// for 120, this is 60, for 135, this is 45, for example.
					long gcd = context.kernel.gcd(angleValDeg, 180);
					// Which power is required to get a real number?
					long rot = Math.abs(180 / gcd); // This is 2 for 90 degrees, 3 for 120 degrees,
					// 4 for 135 (~45) degrees.
					GeoAngle a = (GeoAngle) en.getLeft();
					AlgoElement gae = a.getParentAlgorithm();
					if (gae instanceof AlgoAnglePoints) {
						GeoPoint A = (GeoPoint) ((AlgoAnglePoints) gae).getA();
						GeoPoint B = (GeoPoint) ((AlgoAnglePoints) gae).getB();
						GeoPoint C = (GeoPoint) ((AlgoAnglePoints) gae).getC();
						c.realRelation = idk.anglex(A, B, B, C, rot);
						c.warning = WARNING_ANGLE;
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
				c.realRelation = idk.parallel((GeoLine) ge1, (GeoLine) ge2);
				return c;
			} else if (o == Operation.PERPENDICULAR) {
				Log.debug("Warning: Testing perpendicularity AND parallelism simultaneously");
				c.realRelation = idk.perppar((GeoLine) ge1, (GeoLine) ge2);
				c.warning = WARNING_PERPENDICULAR_OR_PARALLEL;
				return c;
			} else if (o == Operation.IS_ELEMENT_OF) {
				if (ge1 instanceof GeoPoint && ge2 instanceof GeoLine) {
					c.realRelation = idk.online((GeoPoint) ge1, (GeoLine) ge2);
					return c;
				}
				if (ge1 instanceof GeoPoint && ge2 instanceof GeoConic && ((GeoConic) ge2).isCircle()) {
					c.realRelation = idk.oncircle((GeoPoint) ge1, (GeoConic) ge2);
					return c;
				}
				return null; // unimplemented
			} else if (o == Operation.EQUAL_BOOLEAN) {
				return idk.equal(ge1, ge2);
			}
		}
		// Unimplemented, but it should be handled...
		return null;
	}

	/**
	 * Return a label that is unique and can be inserted in a Giac code.
	 * @param ge the input GeoElement
	 * @return the label as String
	 */
	static String getUniqueLabel(GeoElement ge) { // FIXME: move to an appropriate location
		return ge.getLabelSimple().replace("_{","").replace("}", "");
	}

	static String removeTail(String input, int length) {
		if (input.length() >= length) {
			return input.substring(0, input.length() - length);
		}
		return input;
	}

	// This is already present in the class Compute. TODO: Unify the code.
	private static String removeHeadTail(String input, int length) {
		if (input.length() >= 2 * length) {
			return input.substring(length, input.length() - length);
		}
		return input;
	}


	private static String lhs(String eq) {
		int eqIndex = eq.indexOf("=");
		return eq.substring(0, eqIndex);
	}

	private static String addPrimesToLabels(String s, TreeSet<String> labels) {
		if (s == null) return null;
		String out = s;
		for (String lab : labels) {
			if (lab == null || lab.isEmpty()) continue;
			StringBuilder sb = new StringBuilder();
			int i = 0;
			// find all occurences of label
			while (i < out.length()) {
				int idx = out.indexOf(lab, i);
				// nothing more to find, leave loop
				if (idx == -1) {
					sb.append(out.substring(i));
					break;
				}
				// Check word boundary: do not replace if label is part of a longer name
				boolean beforeOk = idx == 0 || !Character.isLetterOrDigit(out.charAt(idx - 1));
				boolean afterOk = idx + lab.length() == out.length()
						|| !Character.isLetterOrDigit(out.charAt(idx + lab.length()));
				sb.append(out, i, idx);
				if (beforeOk && afterOk) {
					sb.append(lab).append(PRIME);
				} else {
					sb.append(lab);
				}
				i = idx + lab.length();
			}
			out = sb.toString();
		}
		return out;
	}

	private static String addPrimesToRVariables(String s, String variableR) {
		if (s == null) return null;
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while (i < s.length()) {
			int idx = s.indexOf(variableR, i);
			if (idx == -1) {
				sb.append(s.substring(i));
				break;
			}
			// ensure r is not part of a longer variable name
			boolean beforeOk = idx == 0 || !Character.isLetterOrDigit(s.charAt(idx - 1));
			if (!beforeOk) {
				sb.append(s, i, idx + 1);
				i = idx + 1;
				continue;
			}
			// consume optional numbers after variableR
			int end = idx + variableR.length();
			while (end < s.length() && Character.isDigit(s.charAt(end))) {
				end++;
			}
			// negative lookahead
			boolean alreadyPrimed = end < s.length() && s.charAt(end) == '\'';
			boolean afterOk = end == s.length() || !Character.isLetterOrDigit(s.charAt(end));
			sb.append(s, i, idx);
			sb.append(s, idx, end);
			if (!alreadyPrimed && afterOk) {
				sb.append(PRIME);
			}
			i = end;
		}
		return sb.toString();
	}

	private static String buildGeoGebraEliminateCommand(
			ArrayList<String> lhsList,
			ArrayList<String> rhsVars,
			TreeSet<String> pointLabels,
			String extraVariables,
			String extraEq0,
			ArrayList<String> specEqList
	) {

		ArrayList<String> eqs = new ArrayList<>();

		//build polynomial in format: ((A'-C')/(A'-O'))/... - r__k
		for (int i = 0; i < lhsList.size(); i++) {
			String lhs = lhsList.get(i);
			String rVar = rhsVars.get(i);
			eqs.add(lhs + "-" + rVar);
		}

		// prime all variables to avoid issues in CAS with defined points
		StringBuilder vars = new StringBuilder();
		for (String lab : pointLabels) {
			if (vars.length() > 0) {
				vars.append(",");
			}
			vars.append(lab).append(PRIME);   // A',B',C',O',...
		}

		// handle divisor (divisor = 0)
		if (extraEq0 != null) {
			String d = extraEq0.trim();
			if (!d.isEmpty() && !"1".equals(d) && !"-1".equals(d) && !"0".equals(d)) {
				eqs.add(d + " = 0");
			}
		}
		// add specializations
		if (specEqList != null) {
			eqs.addAll(specEqList);
		}

		// handle extraVariables
		if (extraVariables != null && !extraVariables.trim().isEmpty()) {
			String ev = extraVariables.trim();
			if (ev.endsWith(",")) {
				ev = ev.substring(0, ev.length() - 1);
			}
			if (!ev.isEmpty()) {
				vars.append(",").append(ev);
			}
		}

		return "Eliminate({" + String.join(",", eqs) + "},{" + vars + "})";
	}

	// simple helpers
	private static boolean isNumericConstant(String expr) {
		if (expr == null) return false;
		String s = expr.trim();
		int slash = s.indexOf('/');
		if (slash == -1) {
			return isDecimal(s);
		}
		// treat string as fraction
		return isDecimal(s.substring(0, slash)) && isDecimal(s.substring(slash + 1));
	}

	private static boolean isDecimal(String s) {
		if (s.isEmpty()) return false;
		int start = 0;
		if (s.charAt(0) == '+' || s.charAt(0) == '-') start = 1;
		if (start == s.length()) return false; // not a valid number
		boolean dotSeen = false;
		for (int i = start; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '.') {
				if (dotSeen) return false; // second dot found -> not a valid number
				dotSeen = true;
			} else if (!Character.isDigit(c)) {
				return false;
			}
		}
		return true;
	}


}
