package org.geogebra.common.kernel.prover;

import java.util.TreeSet;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.main.Localization;
import org.geogebra.common.util.Prover;

public class CommonData {
	public static final String PRIME = "\u0027";

	public static int WARNING_PERPENDICULAR_OR_PARALLEL = 1;
	public static int WARNING_EQUALITY_OR_COLLINEAR = 2;
	public static int WARNING_ANGLE = 3;
	public static String VARIABLE_CYCLOTOMIC = "CT__";

	Prover prover;
	TreeSet<GeoPoint> allPredecessorPoints;
	GeoElement statement;
	Kernel kernel;
	Localization loc;

	String[] predefinitions;
	String predefs = "";

	TreeSet<String> primeLabels;
}
