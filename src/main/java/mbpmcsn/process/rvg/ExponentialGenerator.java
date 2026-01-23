package mbpmcsn.process.rvg;

import mbpmcsn.desbook.Rngs;

public final class ExponentialGenerator implements RandomVariateGenerator {
	private final double mean;

	public ExponentialGenerator(double mean) {
		this.mean = mean;
	}

	@Override
	public double generate(Rngs rngs) {
		return -mean * Math.log(1.0 - rngs.random());
	}
}
