package org.geogebra.common.kernel.prover;

import java.util.TreeSet;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.main.Localization;
import org.geogebra.common.util.Prover;

public class ProverContext {
	public Prover prover;
	public TreeSet<GeoPoint> allPredecessorPoints;
	public GeoElement statement;
	public Kernel kernel;
	public Localization loc;

	public String[] predefinitions;
	public String predefs = "";

	public TreeSet<String> primeLabels;
}
