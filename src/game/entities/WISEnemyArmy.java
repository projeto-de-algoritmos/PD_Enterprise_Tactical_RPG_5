package game.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import game.WISCheapestPath;
import game.extra_algorithms.WeightedIntervalScheduling;
import graphs.GraphMatrix;

public class WISEnemyArmy {
	private List<WISEnemy> enemies;
	private GraphMatrix<Integer, Integer> grid;
	private Consumer<Enemy> lockOtherEnemies;
	private Runnable unlockAllEnemies;
	private Entity target;
	private List<WISCheapestPath> orderedPaths;

	public WISEnemyArmy(Consumer<Enemy> lockOtherEnemies, Runnable unlockAllEnemies) {
		this.setLockOtherEnemies(lockOtherEnemies);
		this.setUnlockAllEnemies(unlockAllEnemies);
		this.setOrderedPaths(new ArrayList<WISCheapestPath>());
	}

	public void findPath() {

		List<WISCheapestPath> paths = new ArrayList<WISCheapestPath>();

		for (WISEnemy enemy : enemies) {
			// Impedir inimigos de entrarem uns nos outros
			lockOtherEnemies.accept(enemy);

			WISCheapestPath enemyPath = new WISCheapestPath(enemy, getTarget(),
					paths.isEmpty() ? 0 : paths.get(paths.size() - 1).getEnd() + 1, grid);

			if (enemyPath.getValid()) {
				paths.add(enemyPath);
			}

			// Reverter mudança
			unlockAllEnemies.run();
		}

		if (!paths.isEmpty()) {
			WeightedIntervalScheduling<WISCheapestPath, Integer, Integer> wis = new WeightedIntervalScheduling<WISCheapestPath, Integer, Integer>(
					paths, 0);
			wis.compute();
			setOrderedPaths(wis.getOrderedTasks());
		} else {
			orderedPaths.clear();
		}
	}

	/**
	 * @return the enemies
	 */
	public List<WISEnemy> getEnemies() {
		return enemies;
	}

	/**
	 * @param enemies the enemies to set
	 */
	public void setEnemies(List<WISEnemy> enemies) {
		this.enemies = enemies;
	}

	/**
	 * @return the grid
	 */
	public GraphMatrix<Integer, Integer> getGrid() {
		return grid;
	}

	/**
	 * @param grid the grid to set
	 */
	public void setGrid(GraphMatrix<Integer, Integer> grid) {
		this.grid = grid;
	}

	/**
	 * @param lockOtherEnemies the lockOtherEnemies to set
	 */
	public void setLockOtherEnemies(Consumer<Enemy> lockOtherEnemies) {
		this.lockOtherEnemies = lockOtherEnemies;
	}

	/**
	 * @param unlockAllEnemies the unlockAllEnemies to set
	 */
	public void setUnlockAllEnemies(Runnable unlockAllEnemies) {
		this.unlockAllEnemies = unlockAllEnemies;
	}

	/**
	 * @return the target
	 */
	public Entity getTarget() {
		return target;
	}

	/**
	 * @param target the target to set
	 */
	public void setTarget(Entity target) {
		this.target = target;
	}

	/**
	 * @return the orderedPaths
	 */
	public List<WISCheapestPath> getOrderedPaths() {
		return orderedPaths;
	}

	/**
	 * @param orderedPaths the orderedPaths to set
	 */
	private void setOrderedPaths(List<WISCheapestPath> orderedPaths) {
		this.orderedPaths = orderedPaths;
	}
}
