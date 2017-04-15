
import java.util.Arrays;

import net.sourceforge.jswarm_pso.*;

public class PSO {
	public static final int NB_FEATURES = 6;//8;
	public static final int GAMES = 16;
	public static final int MOVE_CUTOFF_NUM = 10_000_000;//Integer.MAX_VALUE;
	public static final int NB_PARTICULES = 25;
	public static final int NB_EVOLUTION = 20;
	private final Swarm swarm;

	public static class TetrisFitnessFunction extends FitnessFunction {

		@Override
		public double evaluate(double[] weights) {
			PlayerSkeletonTrain player = new PlayerSkeletonTrain(weights, GAMES, MOVE_CUTOFF_NUM);
			return player.play();
			/*double score = player.play();
			System.out.println(Arrays.toString(weights) + " " + score);
			return score;*/
		}

	}

	public static class TetrisParticle extends Particle {
		public TetrisParticle() {
			super(NB_FEATURES);
		}
	}

	public PSO() {
		swarm = new Swarm(NB_PARTICULES, new TetrisParticle(), new TetrisFitnessFunction());
		//double max[] = {-2, 9, -1, -7, -9.5, -1.5};
		double max[] = {0, 10, 0, 0 , 0, 0};
		//double min[] = {-3, 6.5, -3, -8.5, -10, -3};
		double min[] = {-10, 5, -10, -10, -10, -10};
		swarm.setMaxPosition(max);
		swarm.setMinPosition(min);
		swarm.setMaxMinVelocity(0.5);
	}

	private void evolve(int i) {
		for (int j = 0; j < i; ++j) {
			System.out.println("Iteration " + (j + 1));
			swarm.evolve();
			Particle particles[] = swarm.getParticles();
			String str = "";
			for (int k = 0; k < particles.length; k++) {
				str += k + ";" + particles[k].getFitness() + ";" + Arrays.toString(particles[k].getPosition()) + "\n";
			}
			System.out.println(str);
		}
		System.out.println(swarm.toStringStats());
	}

	public static void main(String[] args) {
		PSO trainer = new PSO();
		trainer.evolve(NB_EVOLUTION);
	}
}
