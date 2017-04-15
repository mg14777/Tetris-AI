import java.util.*;
public class ReinforcementLearner {
	
	private WeightVector[] weight_vectors;

	public static final int WEIGHT_VECTOR_NUM = 500;
	public static final int WEIGHT_VECTOR_DIMENSIONS = 4;

	public static final int OFFSPRING_NUMBER = 150;
	public static final int GAMES = 10;
	public static final int LEARNING_SESSIONS = 100;
	public static final int MOVE_CUTOFF_NUM = 100000;
	public static final int NB_PROCESSORS = Runtime.getRuntime().availableProcessors();
	public static final int VECTOR_PER_CORE = WEIGHT_VECTOR_NUM / NB_PROCESSORS;


	public ReinforcementLearner() {
		//weight_vectors = new double[WEIGHT_VECTOR_NUM][WEIGHT_VECTOR_DIMENSIONS];
		//best_scores = new double[WEIGHT_VECTOR_NUM];
		weight_vectors = new WeightVector[WEIGHT_VECTOR_NUM];
	}
	public WeightVector breedParents(WeightVector w1,WeightVector w2) {
		double[] offspring_weights = new double[WEIGHT_VECTOR_DIMENSIONS];
		for(int i=0; i < WEIGHT_VECTOR_DIMENSIONS;i++)
			offspring_weights[i] = w1.best_score*w1.weights[i] + w2.best_score*w2.weights[i];
		return normalize(new WeightVector(offspring_weights));
	}
	public void breeder() {
		WeightVector p1 = weight_vectors[0];
		for (int i = 1; i < OFFSPRING_NUMBER; ++i) {
			WeightVector p2 = weight_vectors[i];
			weight_vectors[WEIGHT_VECTOR_NUM - i] = breedParents(p1,p2);
			p1 = p2;
		}
	}
	
	public WeightVector normalize(WeightVector w) {
		double magnitude = 0.0;
		for(int i=0; i < WEIGHT_VECTOR_DIMENSIONS;i++) {
			magnitude += Math.pow(w.weights[i],2);
		}
		if(magnitude == 0.0) {
			System.out.println("Zero vector!!");
			return w;
		}
		magnitude = Math.sqrt(magnitude);
		for(int i=0; i < WEIGHT_VECTOR_DIMENSIONS;i++) {
			w.weights[i] = w.weights[i]/magnitude;
		}
		return w;
	}
	public void initWeightVectors() {
		Random randomNum = new Random();
		for(int i=0; i < WEIGHT_VECTOR_NUM; i++) {
			double[] weights = new double[WEIGHT_VECTOR_DIMENSIONS];
			for(int j=0; j < WEIGHT_VECTOR_DIMENSIONS; j++) {
				weights[j] = Math.random();
				int sign = randomNum.nextInt(2);
				if(sign == 0)
					weights[j] = -1*weights[j];
			}
			weight_vectors[i] = normalize(new WeightVector(weights));
			
		}
	}
	
	public void learnHelper(int start, int end) {
		for(int j=start;  j < end; j++) {
			PlayerSkeletonTrain player = new PlayerSkeletonTrain(weight_vectors[j].weights,GAMES,MOVE_CUTOFF_NUM);
			weight_vectors[j].best_score = player.play();
		}
	}
	
	public void learn() {
		for(int i=0; i < LEARNING_SESSIONS; i++) {
			List<Thread> threads = new ArrayList<>(NB_PROCESSORS);
			for (int k = 0; k < NB_PROCESSORS; ++k) {
				final int index = k;
				Thread t = k == NB_PROCESSORS - 1 ? new Thread(() -> {
					learnHelper(index*VECTOR_PER_CORE, WEIGHT_VECTOR_NUM);
				}) : new Thread(() -> {
					learnHelper(index*VECTOR_PER_CORE, (index + 1) * VECTOR_PER_CORE);
				});
				t.start();
				threads.add(t);
			}
			for (int k = 0; k < NB_PROCESSORS; ++k) {
				try {
					threads.get(k).join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			Arrays.sort(weight_vectors);
			System.out.println("Learning session: "+(i+1)+"   Best Score: "+weight_vectors[0].best_score);
			System.out.println("Weight Vector : "+weight_vectors[0].weights[0]+" "+weight_vectors[0].weights[1]+" "+weight_vectors[0].weights[2]+" "+weight_vectors[0].weights[3]);
			breeder();	// Breeds current population to produce fitter weight vectors
		}
		//System.out.println("Optimal parameters: "+weight_vectors.get(0).weights[0]+" "+weight_vectors.get(0).weights[1]+" "+weight_vectors.get(0).weights[2]+" "+weight_vectors.get(0).weights[3]+"		Best Score: "+weight_vectors.get(0).best_score);
	}

	public static void main(String[] args) {
		ReinforcementLearner learner = new ReinforcementLearner();
		learner.initWeightVectors();
		learner.learn();
	}
	class WeightVector implements Comparable<WeightVector> {
		double[] weights;
		double best_score;

		public WeightVector(double[] weights) {
			this.weights = new double[4];
			this.weights = weights;
			best_score = 0;
		}
		public int compareTo(WeightVector w) {
			if(this.best_score > w.best_score)
				return -1;
			else if(this.best_score < w.best_score)
				return 1;
			else
				return 0;
		}
	}
}