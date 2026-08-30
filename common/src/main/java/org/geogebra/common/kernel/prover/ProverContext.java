package org.geogebra.common.kernel.prover;

import java.util.TreeSet;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.main.Localization;
import org.geogebra.common.util.Prover;

public class ProverContext {
	Prover prover;
	TreeSet<GeoPoint> allPredecessorPoints;
	GeoElement statement;
	Kernel kernel;
	Localization loc;

	String[] predefinitions;
	String predefs = "";

	TreeSet<String> primeLabels;
}
