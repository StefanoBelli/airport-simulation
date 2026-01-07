package mbpmcsn.center;

import mbpmcsn.center.abstracts.ServiceProcess;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.desbook.Rvms;

public final class NormalTruncatedServiceProcess implements ServiceProcess {
	private final double mean;
	private final double devstd;
	private final double lb;
	private final double ub;

	private final int streamIdx;
	private Rngs rngs;
	
	public NormalTruncatedServiceProcess(double mean, double devstd, double lb, double ub, int streamIdx) {
		this.mean = mean;
		this.devstd = devstd;
		this.lb = lb;
		this.ub = ub;
		this.streamIdx = streamIdx;
		resetRngs(new Rngs());
	}

	@Override
	public void resetRngs(Rngs rngs) {
		this.rngs = rngs;
		this.rngs.selectStream(streamIdx);
	}

	@Override
	public double getService() {
		rngs.selectStream(streamIdx);
		return idfTruncatedNormal();
	}

	private final double idfTruncatedNormal() {
		Rvms rvms = new Rvms();

		double a = rvms.cdfNormal(mean, devstd, lb - 1);
		double b = 1.0 - rvms.cdfNormal(mean, devstd, ub);
		double u = rvms.idfUniform(a,1.0-b, rngs.random());

		return rvms.idfNormal(mean, devstd, u);
	}
}
