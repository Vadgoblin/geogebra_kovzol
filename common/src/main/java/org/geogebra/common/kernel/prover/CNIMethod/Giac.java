package org.geogebra.common.kernel.prover.CNIMethod;

import org.geogebra.common.cas.GeoGebraCAS;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.util.debug.Log;

public class Giac {
	private final Kernel kernel;

	public Giac(Kernel kernel){
		this.kernel = kernel;
	}

	public String execute(String command){
		GeoGebraCAS cas = (GeoGebraCAS) kernel.getGeoGebraCAS();
		String APOSTROPHE = "AP__";
		command = command.replace("'", APOSTROPHE);
		try {
			String ret = cas.evaluateRaw(command);
			ret = ret.replace(APOSTROPHE, "'");
			return ret;
		} catch (Throwable e) {
			Log.error("Error in ProverCNIMethod/executeGiac: input=" + command);
			return "ERROR";
		}
	}
}
