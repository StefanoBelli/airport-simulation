package mbpmcsn;

import mbpmcsn.runners.FiniteHorizonRunner;
import mbpmcsn.runners.smbuilders.BaseSimulationModelBuilder;

public class App {
	public static void main(String[] args) {
		FiniteHorizonRunner fhr = new FiniteHorizonRunner(new BaseSimulationModelBuilder(), 10000, false, 100.00);
		fhr.runIt();
		System.out.println("hello world");
	}
}

