package org.geogebra.common.kernel.prover;

import java.util.ArrayList;
import java.util.TreeSet;

import org.geogebra.common.cas.GeoGebraCAS;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.algos.AlgoAnglePoints;
import org.geogebra.common.kernel.algos.AlgoAngularBisectorPoints;
import org.geogebra.common.kernel.algos.AlgoCircleThreePoints;
import org.geogebra.common.kernel.algos.AlgoCircleTwoPoints;
import org.geogebra.common.kernel.algos.AlgoDependentBoolean;
import org.geogebra.common.kernel.algos.AlgoDependentPoint;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.algos.AlgoIntersectConics;
import org.geogebra.common.kernel.algos.AlgoIntersectLineConic;
import org.geogebra.common.kernel.algos.AlgoIntersectLines;
import org.geogebra.common.kernel.algos.AlgoIntersectSingle;
import org.geogebra.common.kernel.algos.AlgoLineBisector;
import org.geogebra.common.kernel.algos.AlgoLineBisectorSegment;
import org.geogebra.common.kernel.algos.AlgoLinePointLine;
import org.geogebra.common.kernel.algos.AlgoMidpoint;
import org.geogebra.common.kernel.algos.AlgoMidpointSegment;
import org.geogebra.common.kernel.algos.AlgoMirror;
import org.geogebra.common.kernel.algos.AlgoOrthoLinePointLine;
import org.geogebra.common.kernel.algos.AlgoPointOnPath;
import org.geogebra.common.kernel.algos.AlgoPolygonRegular;
import org.geogebra.common.kernel.algos.AlgoRotatePoint;
import org.geogebra.common.kernel.algos.AlgoTranslate;
import org.geogebra.common.kernel.arithmetic.ExpressionNode;
import org.geogebra.common.kernel.arithmetic.MySpecialDouble;
import org.geogebra.common.kernel.geos.GeoAngle;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoSegment;
import org.geogebra.common.kernel.geos.GeoVector;
import org.geogebra.common.kernel.scripting.CmdShowProof;
import org.geogebra.common.main.Localization;
import org.geogebra.common.plugin.Operation;
import org.geogebra.common.util.DoubleUtil;
import org.geogebra.common.util.Prover;
import org.geogebra.common.util.Prover.ProofResult;
import org.geogebra.common.util.debug.Log;
import org.geogebra.common.kernel.prover.ProverCNIMethodAsd;

import static org.geogebra.common.cas.giac.CASgiac.ggbGiac;

import com.himamis.retex.editor.share.util.Unicode;

public class ProverCNIMethod {


	public static ProofResult prove(Prover prover) {
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

		context.predefinitions  = predefinitions;
		context.predefs = predefs;

		return new ProverCNIMethodAsd().prove(context);
	}



}
