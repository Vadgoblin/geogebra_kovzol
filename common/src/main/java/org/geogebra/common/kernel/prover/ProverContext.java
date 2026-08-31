package org.geogebra.common.kernel.prover;

import java.util.TreeSet;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.main.Localization;
import org.geogebra.common.util.Prover;

public class ProverContext {
	public ProverContext(Prover prover, TreeSet<GeoPoint> allPredecessorPoints, TreeSet<String> primeLabels){
		this.prover = prover;
		this.statement = prover.getStatement();
		this.kernel = prover.getStatement().getKernel();
		this.loc = prover.getStatement().getKernel().getLocalization();

		this.allPredecessorPoints = allPredecessorPoints;
		this.primeLabels = primeLabels;
	}

	public Prover prover;
	public TreeSet<GeoPoint> allPredecessorPoints;
	public GeoElement statement;
	public Kernel kernel;
	public Localization loc;

	public TreeSet<String> primeLabels;
}
