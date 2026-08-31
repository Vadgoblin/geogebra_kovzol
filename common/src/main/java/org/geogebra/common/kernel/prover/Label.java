package org.geogebra.common.kernel.prover;

import org.geogebra.common.kernel.geos.GeoElement;

public class Label {
	/**
	 * Return a label that is unique and can be inserted in a Giac code.
	 * @param ge the input GeoElement
	 * @return the label as String
	 */
	public static String makeUnique(GeoElement ge) {
		return ge.getLabelSimple().replace("_{","").replace("}", "");
	}
}
