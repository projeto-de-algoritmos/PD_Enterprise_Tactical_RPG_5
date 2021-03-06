package game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;

import javax.swing.JPanel;

import game.entities.Enemy;
import game.entities.Entity;
import game.entities.GreedyEnemy;
import game.entities.MedianEnemy;
import game.entities.Player;
import game.entities.WISEnemy;
import game.entities.WISEnemyArmy;
import game.extra_algorithms.Quickselect;
import graphs.CheapestPath;
import graphs.GraphMatrix;
import graphs.Position;

public class Panel extends JPanel implements MouseListener, MouseMotionListener {
	private class EnemyCheapestPath {
		private final Enemy enemy;
		private final CheapestPath<Position, Integer> path;
		private final Boolean valid;

		private EnemyCheapestPath(Enemy enemy, Entity player) {
			this.enemy = enemy;
			this.path = grid.dijkstra(new Position(enemy.getGridX(), enemy.getGridY()),
					new Position(player.getGridX(), player.getGridY()));
			this.valid = this.path == null ? false : true;
		}

		/**
		 * @return the Enemy
		 */
		Enemy getEnemy() {
			return enemy;
		}

		/**
		 * @return the path
		 */
		CheapestPath<Position, Integer> getPath() {
			return path;
		}

		/**
		 * @return the valid
		 */
		Boolean getValid() {
			return valid;
		}
	}

	private class CompareEnemyCheapestPathCost implements Comparator<EnemyCheapestPath> {
		@Override
		public int compare(EnemyCheapestPath o1, EnemyCheapestPath o2) {
			if (o1.getPath().getTotalCost() < o2.getPath().getTotalCost()) {
				return -1;
			}

			if (o1.getPath().getTotalCost() > o2.getPath().getTotalCost()) {
				return 1;
			}

			return 0;
		}
	}

	private class GreedyCheapestPath extends EnemyCheapestPath implements Comparable<GreedyCheapestPath> {
		private final GreedyEnemy greedyEnemy;
		private final CheapestPath<Position, Integer> path;
		private final Integer weight;
		private final Integer value;
		private final Double specificValue;
		private final Boolean valid;

		GreedyCheapestPath(GreedyEnemy greedyEnemy, Player player) {
			super(greedyEnemy, player);
			this.greedyEnemy = greedyEnemy;
			this.path = grid.dijkstra(new Position(greedyEnemy.getGridX(), greedyEnemy.getGridY()),
					new Position(player.getGridX(), player.getGridY()));
			this.weight = this.path == null ? null : path.getPath().size();
			this.value = this.path == null ? null : path.getTotalCost();
			this.specificValue = this.path == null ? null : (double) value / (double) weight;
			this.valid = this.path == null ? false : true;
		}

		/**
		 * @return the greedyEnemy
		 */
		GreedyEnemy getGreedyEnemy() {
			return greedyEnemy;
		}

		/**
		 * @return the path
		 */
		@Override
		CheapestPath<Position, Integer> getPath() {
			return path;
		}

		/**
		 * @return the weight
		 */
		Integer getWeight() {
			return weight;
		}

		/**
		 * @return the specificValue
		 */
		Double getSpecificValue() {
			return specificValue;
		}

		/**
		 * @return the valid
		 */
		@Override
		Boolean getValid() {
			return valid;
		}

		@Override
		public int compareTo(GreedyCheapestPath o) {
			return Double.compare(this.getSpecificValue(), o.getSpecificValue());
		}
	}
	
	private class CompareWISCheapestPathEnd implements Comparator<WISCheapestPath> {
		@Override
		public int compare(WISCheapestPath o1, WISCheapestPath o2) {
			if (o1.getEnd() < o2.getEnd()) {
				return -1;
			}

			if (o1.getEnd() > o2.getEnd()) {
				return 1;
			}

			return 0;
		}
	}

	private static final long serialVersionUID = 1L;
	private static final Integer FORBIDDEN = -1;
	private static final Integer EMPTY = 0;
	private static final Integer VISITED = 1;
	private static int WIDTH;
	private static int HEIGHT;
	private Player player;
	private Map map;
	private List<Position> preview;
	private GraphMatrix<Integer, Integer> grid;
	private boolean running;
	private int lastMouseX;
	private int lastMouseY;
	private boolean inPlayer;
	private int moveCost;
	private int tileSize;

	private final int initialCost = 1;
	private final int minimumCost = 0;
	private final int maximumCost = Integer.MAX_VALUE;

	private List<Enemy> enemies = new ArrayList<Enemy>();
	private List<GreedyEnemy> greedyEnemies = new ArrayList<GreedyEnemy>();
	private List<MedianEnemy> medianEnemies = new ArrayList<MedianEnemy>();
	private List<WISEnemy> wisEnemies = new ArrayList<WISEnemy>();
	private List<Entity> allEnemies = new ArrayList<Entity>();
	
	private WISEnemyArmy wisEnemyArmy = new WISEnemyArmy();

	private int sizeX;
	private int sizeY;
	final private int playerMoves = 5;
	final private Comparator<Integer> costComparator = new Comparator<Integer>() {
		@Override
		public int compare(Integer o1, Integer o2) {
			if (o1 < o2) {
				return -1;
			}

			if (o1 > o2) {
				return 1;
			}

			return 0;
		}
	};
	final private BinaryOperator<Integer> costAdder = (Integer a, Integer b) -> a + b;

	private int enemyMoves = 3;
	private int greedyArmyMoveBudget = enemyMoves;

	int rounds = 0;
	private boolean stepMode;
	private boolean previewVisibility = true;

	private int msDelay = 60;
	private int msLongerDelay = 2 * msDelay;
	
	private SoundPlayer soundPlayer;

	public Panel(int size, int width, int height, boolean stepMode, boolean soundEnabled) {

		// Step Mode
		this.stepMode = stepMode;

		// Define os tamanhos
		WIDTH = width;
		HEIGHT = height;
		sizeX = size;
		sizeY = size;
		tileSize = HEIGHT / size;

		// Configura????es do Painel
		setFocusable(true);
		setPreferredSize(new Dimension(WIDTH, HEIGHT));

		// Mouse Listeners
		addMouseListener(this);
		addMouseMotionListener(this);

		// Inicializar Jogador
		int h = (int) (tileSize * 0.8); // 80% do tileSize
		int w = (int) (tileSize * 0.8); // 80% do tileSize
		int off = (tileSize - w) / 2; // Meio do tile
		player = new Player(playerMoves, 5, 5, tileSize, off, h, w, Color.BLUE);

		// Inicializar inimigos comuns
		h = (int) (tileSize * 0.6); // 60% do tileSize
		w = (int) (tileSize * 0.6); // 60% do tileSize
		off = (tileSize - w) / 2; // Meio do tile
		enemies.add(new Enemy(enemyMoves, 10, 10, tileSize, off, h, w, Color.RED));
		enemies.add(new Enemy(enemyMoves, 15, 15, tileSize, off, h, w, Color.RED));
		enemies.add(new Enemy(enemyMoves, 10, 15, tileSize, off, h, w, Color.RED));

		// Inicializar inimigos ambiciosos
		h = (int) (tileSize * 0.95); // 95% do tileSize
		w = (int) (tileSize * 0.95); // 95% do tileSize
		off = (tileSize - w) / 2; // Meio do tile
		greedyEnemies.add(new GreedyEnemy(enemyMoves, 9, 9, tileSize, off, h, w, Color.RED));
		greedyEnemies.add(new GreedyEnemy(enemyMoves, 14, 14, tileSize, off, h, w, Color.RED));
		greedyEnemies.add(new GreedyEnemy(enemyMoves, 9, 14, tileSize, off, h, w, Color.RED));

		// Inicializar inimigos da mediana
		h = (int) (tileSize * 0.9); // 90% do tileSize
		w = (int) (tileSize * 0.9); // 90% do tileSize
		off = (tileSize - w) / 2; // Meio do tile
		medianEnemies.add(new MedianEnemy(enemyMoves, 8, 8, tileSize, off, h, w, Color.RED));
		medianEnemies.add(new MedianEnemy(enemyMoves, 13, 13, tileSize, off, h, w, Color.RED));
		medianEnemies.add(new MedianEnemy(enemyMoves, 8, 13, tileSize, off, h, w, Color.RED));
		
		// Inicializar inimigos do agendamento com peso
		h = (int) (tileSize * 0.9); // 90% do tileSize
		w = (int) (tileSize * 0.9); // 90% do tileSize
		off = (tileSize - w) / 2; // Meio do tile
		wisEnemies.add(new WISEnemy(enemyMoves, 7, 7, tileSize, off, h, w, Color.RED));
		wisEnemies.add(new WISEnemy(enemyMoves, 12, 12, tileSize, off, h, w, Color.RED));
		wisEnemies.add(new WISEnemy(enemyMoves, 7, 12, tileSize, off, h, w, Color.RED));

		// Lista de inimigos
		allEnemies.addAll(enemies);
		allEnemies.addAll(greedyEnemies);
		allEnemies.addAll(medianEnemies);
		allEnemies.addAll(wisEnemies);
		
		// Inicializar ex??rcitos
		wisEnemyArmy.setEnemies(wisEnemies);
		wisEnemyArmy.setAllEnemies(allEnemies);

		// Inicializa Grafo do Mapa
		grid = new GraphMatrix<Integer, Integer>(sizeX, sizeY, EMPTY, VISITED, FORBIDDEN, initialCost, minimumCost,
				maximumCost, costComparator, costAdder);

		// Inicializa o Preview do movimento do Jogador
		preview = new ArrayList<Position>();

		// Cria dicion??rio de custo/cores
		HashMap<Integer, Color> hash = new HashMap<Integer, Color>();
		hash.put(initialCost, Color.GREEN);
		hash.put(initialCost + 1, Color.YELLOW);
		hash.put(initialCost + 2, Color.ORANGE);
		
		// Inicializa os Sons
		HashMap<String, String> sounds = new HashMap<String, String>();
		if(soundEnabled) {
			sounds.put("playerMove", "assets/playermove.wav");
			sounds.put("enemyMove", "assets/enemymove.wav");
			sounds.put("death", "assets/death.wav");
			
		}
		soundPlayer = new SoundPlayer(sounds);
		
		// Inicializa Mapa
		map = new Map(grid, hash, WIDTH, HEIGHT, sizeX, sizeY);
		addRandomCosts((sizeX * sizeY) / 2, hash.size() + 1);
		addRandomForbidden(sizeX);
		grid.setElementCost(player.getGridX(), player.getGridY(), initialCost);
		grid.setElementValue(player.getGridX(), player.getGridY(), EMPTY);

		// Inicia o Jogo
		start();
	}

	// Altera o custo de at?? <number> casas aleat??rias
	private void addRandomCosts(int number, int max) {
		for (int i = 0; i < number; i++) {
			int randomX = ThreadLocalRandom.current().nextInt(0, sizeX);
			int randomY = ThreadLocalRandom.current().nextInt(0, sizeY);
			int randomCost = ThreadLocalRandom.current().nextInt(initialCost, max);
			grid.setElementCost(randomX, randomY, randomCost);
		}
	}

	// Adiciona at?? <number> obst??culos intranspon??veis
	private void addRandomForbidden(int number) {
		List<Entity> entities = new ArrayList<Entity>();
		entities.addAll(allEnemies);
		entities.add(player);

		for (int i = 0; i < number; i++) {
			Boolean acceptable = true;
			int randomX = ThreadLocalRandom.current().nextInt(0, sizeX);
			int randomY = ThreadLocalRandom.current().nextInt(0, sizeY);

			// Verifica sobreposi????o com as entidades
			for (Entity entity : entities) {
				if (checkOverride(randomX, randomY, entity.getGridX(), entity.getGridY())) {
					acceptable = false;
				}
			}
			// Apenas acrescenta o obst??culo caso a casa esteja livre
			if (acceptable) {
				grid.setElementValue(randomX, randomY, FORBIDDEN);
			}
		}
	}

	private void start() {
		running = true;
	}

	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2d = (Graphics2D) g;

		// Desenha a Grade
		map.draw(g2d);

		// Desenha a Preview do movimento do Jogador
		if (previewVisibility) {
			drawPreview(g2d);
		}
		// Desenha o Jogador
		player.draw(g2d);

		// Desenha inimigos
		for (Entity enemy : allEnemies) {
			enemy.draw(g2d);
		}
	}

	public void stop() {
		running = false;
	}

	@Override
	public void mouseMoved(MouseEvent m) {
		stopOnTKO();

		// Coordenadas atuais do mouse na grade
		int mx = coordToGrid(m.getX());
		int my = coordToGrid(m.getY());

		// Atualiza as coordenadas do Mouse
		if (lastMouseX != mx || lastMouseY != my) {
			lastMouseX = mx;
			lastMouseY = my;
			if (mx == player.getGridX() && my == player.getGridY())
				inPlayer = true;
			else {
				inPlayer = false;
				try {
					encontraCaminho();
				} catch (ArrayIndexOutOfBoundsException e) {

				}
			}
			repaint();
		}
	}

	@Override
	public void mouseClicked(MouseEvent m) {
		stopOnTKO();

		// Move o Jogador
		if (moveCost <= player.getMoves() && !inPlayer && !isForbidden(m)) {
			if (stepMode) {
				// Movimenta????o passo a passo
				movePlayer();
			} else {
				// Movimenta????o direta
				player.setGridX((m.getX() - 1) / tileSize);
				player.setGridY((m.getY() - 1) / tileSize);
				soundPlayer.play("playerMove");
			}
			inPlayer = true;

			encontraCaminhoInimigos();

			rounds++;
			if (rounds % 10 == 0 && enemyMoves <= 2 * playerMoves)
				enemyMoves++;
			for (Enemy enemy : enemies) {
				enemy.setMoves(enemyMoves);
			}

			for (Entity enemy : allEnemies) {
				if (enemy.getGridX().equals(player.getGridX()) && enemy.getGridY().equals(player.getGridY())) {
					soundPlayer.play("death");
					stop();
				}
			}
			repaint();
		}
	}

	private void movePlayer() {
		int counter = 0;
		previewVisibility = false;
		for (Position pos : preview) {
			if (counter > playerMoves)
				break;
			player.setGridX(pos.getPosX());
			player.setGridY(pos.getPosY());
			soundPlayer.play("playerMove");
			delayPaint(msDelay);
			counter++;
		}
		delayPaint(msLongerDelay);
	}

	@Override
	public void mouseEntered(MouseEvent m) {
		mouseMoved(m);
	}

	@Override
	public void mouseExited(MouseEvent m) {
		stopOnTKO();
		inPlayer = true;
		lastMouseX = player.getGridX();
		lastMouseY = player.getGridY();
		repaint();
	}

	@Override
	public void mousePressed(MouseEvent m) {
	}

	@Override
	public void mouseReleased(MouseEvent m) {
	}

	@Override
	public void mouseDragged(MouseEvent m) {
	}

	// Desenha a preview do movimento do Jogador
	private void drawPreview(Graphics2D g) {
		int x = -1;
		int y = -1;
		moveCost = 0;
		g.setColor(Color.RED);
		if (!preview.isEmpty() && !inPlayer) {
			for (Position e : preview) {
				if (moveCost > player.getMoves())
					break;
				if (x != -1 && y != -1) {
					if (grid.getElementCost(e) + moveCost <= player.getMoves())
						g.drawLine(gridToCoord(e.getPosX()) + tileSize / 2, gridToCoord(e.getPosY()) + tileSize / 2,
								gridToCoord(x) + tileSize / 2, gridToCoord(y) + tileSize / 2);
					moveCost += grid.getElementCost(e);
				}
				x = e.getPosX();
				y = e.getPosY();
			}
		}
	}

	private boolean isForbidden(MouseEvent m) {
		if (grid.getElementValue(coordToGrid(m.getX()), coordToGrid(m.getY())).equals(grid.getFORBIDDEN()))
			return true;
		else
			return false;
	}

	private List<Position> cheapestPathToList(CheapestPath<Position, Integer> cpt) {
		if (cpt == null) {
			return new ArrayList<Position>();
		}

		return cpt.getPath();
	}

	private void stopOnTKO() {
		grid.setVisitedToEmpty();
		// Ajuda a n??o entrar nos inimigos
		for (Entity e : allEnemies) {
			grid.setElementValue(e.getGridX(), e.getGridY(), FORBIDDEN);
		}

		List<Position> ps = grid.visitableNeighbours(player.getGridX(), player.getGridY());

		if (ps.isEmpty()) {
			stop();
		}

		// Reverter modifica????o
		for (Entity e : allEnemies) {
			grid.setElementValue(e.getGridX(), e.getGridY(), EMPTY);
		}
	}

	private void encontraCaminho() {
		grid.setVisitedToEmpty();

		// Ajuda a n??o entrar nos inimigos
		for (Entity e : allEnemies) {
			grid.setElementValue(e.getGridX(), e.getGridY(), FORBIDDEN);
		}

		CheapestPath<Position, Integer> cpt = grid.dijkstra(new Position(player.getGridX(), player.getGridY()),
				new Position(lastMouseX, lastMouseY));
		preview = cheapestPathToList(cpt);

		// Reverter modifica????o
		for (Entity e : allEnemies) {
			grid.setElementValue(e.getGridX(), e.getGridY(), EMPTY);
		}

		grid.setVisitedToEmpty();
	}

	private void encontraCaminhoInimigos() {
		grid.setVisitedToEmpty();

		// Caminho dos inimigos comuns
		encontraCaminhoInimigosComuns();

		// Caminho dos inimigos Greedy
		encontraCaminhoInimigosGreedy();

		// Caminho dos inimigos medianos
		encontraCaminhoInimigosMedian();
		
		// Caminho dos inimigos agendados por peso
		encontraCaminhoInimigosWIS();

		// Reativa a visibilidade do preview
		previewVisibility = true;

		grid.setVisitedToEmpty();
	}

	private void encontraCaminhoInimigosComuns() {
		grid.setVisitedToEmpty();

		for (Enemy enemy : enemies) {
			// Impedir inimigos de entrarem uns nos outros
			lockOtherEnemies(enemy);

			// Caminho do inimigo
			EnemyCheapestPath enemyPath = new EnemyCheapestPath(enemy, player);
			if (enemyPath.getValid()) {
				moveEnemyToPlayerWithCost(enemyPath.getEnemy(), enemyPath.getPath());
			}

			// Reverter mudan??a
			unlockAllEnemies();

			grid.setVisitedToEmpty();

			// Atualiza a tela para mostrar o movimento individual de cada inimigo
			if (stepMode) {
				delayPaint(msDelay);
			}
		}
	}

	private void delayPaint(int delay) {
		this.paintImmediately(0, 0, WIDTH, HEIGHT);
		try {
			TimeUnit.MILLISECONDS.sleep(delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Encontra caminho para os inimigos ambiciosos usando o algoritmo da mochila
	 * com itens divis??veis (fractional knapsack) O algoritmo considera como peso o
	 * n??mero de casas a mover, o valor ?? o custo do movimento (quanto mais alto o
	 * custo, mais perto do jogador, pois o caminho tra??ado ?? o mais curto), e o
	 * valor espec??fico ?? a divis??o entre o valor e o peso (?? mais valioso um
	 * movimento que chegue o mais perto do jogador no menor n??mero de casas)
	 */
	private void encontraCaminhoInimigosGreedy() {
		grid.setVisitedToEmpty();

		Integer maxWeight = greedyArmyMoveBudget;

		// used moves
		Integer currWeight = 0;

		List<GreedyCheapestPath> items = new ArrayList<GreedyCheapestPath>();
		for (GreedyEnemy enemy : greedyEnemies) {
			// Impedir inimigos de entrarem uns nos outros
			lockOtherEnemies(enemy);

			GreedyCheapestPath item = new GreedyCheapestPath(enemy, player);

			if (item.getValid()) {
				items.add(item);
			}

			// Reverter mudan??a
			unlockAllEnemies();
		}

		items.sort(null);

		for (GreedyCheapestPath item : items) {
			Integer lastPos;
			Integer x;
			Integer y;
			if (currWeight + item.getWeight() <= maxWeight) {
				lastPos = item.getPath().getPath().size() - 1;
				if (stepMode) {
					// Movimenta????o passo a passo
					moveGreedEnemy(item, item.getPath().getPath().get(lastPos));
				} else {
					// Movimenta????o direta
					x = item.getPath().getPath().get(lastPos).getPosX();
					y = item.getPath().getPath().get(lastPos).getPosY();
					item.getGreedyEnemy().setGridX(x);
					item.getGreedyEnemy().setGridY(y);
				}

				currWeight += item.getWeight();
			} else {
				Integer remainder = maxWeight - currWeight;
				lastPos = item.getPath().getPath().size() > remainder ? remainder : item.getPath().getPath().size();
				if (stepMode) {
					// Movimenta????o passo a passo
					moveGreedEnemy(item, item.getPath().getPath().get(lastPos));
				} else {
					// Movimenta????o direta
					x = item.getPath().getPath().get(lastPos).getPosX();
					y = item.getPath().getPath().get(lastPos).getPosY();
					item.getGreedyEnemy().setGridX(x);
					item.getGreedyEnemy().setGridY(y);
				}
				break;
			}
		}

		grid.setVisitedToEmpty();
	}

	private void moveGreedEnemy(GreedyCheapestPath item, Position finish) {
		for (Position p : item.getPath().getPath()) {
			item.getGreedyEnemy().setGridX(p.getPosX());
			item.getGreedyEnemy().setGridY(p.getPosY());
			soundPlayer.play("enemyMove");
			delayPaint(msDelay);
			if (p == finish) {
				break;
			}
		}
	}

	/**
	 * Encontra caminho para o inimigo que est?? na mediana dos movimentos No caso de
	 * mediana de n??mero par de inimigos, ser?? escolhido aquele com o maior custo de
	 * movimentos
	 */
	private void encontraCaminhoInimigosMedian() {
		grid.setVisitedToEmpty();

		List<EnemyCheapestPath> items = new ArrayList<EnemyCheapestPath>();

		for (MedianEnemy enemy : medianEnemies) {
			// Impedir inimigos de entrarem uns nos outros
			lockOtherEnemies(enemy);

			EnemyCheapestPath enemyPath = new EnemyCheapestPath(enemy, player);

			if (enemyPath.getValid()) {
				items.add(enemyPath);
			}

			// Reverter mudan??a
			unlockAllEnemies();
		}

		if (!items.isEmpty()) {
			Quickselect<EnemyCheapestPath> quickselect = new Quickselect<EnemyCheapestPath>(
					new CompareEnemyCheapestPathCost());
			List<EnemyCheapestPath> median = quickselect.getMedian(items);
			EnemyCheapestPath choosen = median.get(median.size() - 1);

			moveEnemyToPlayerWithCost(choosen.getEnemy(), choosen.getPath());
		}

		grid.setVisitedToEmpty();
	}
	
	void encontraCaminhoInimigosWIS() {
		grid.setVisitedToEmpty();
		
		wisEnemyArmy.setGrid(grid);
		wisEnemyArmy.setTarget(player);
		wisEnemyArmy.findPath();
		
		for (WISCheapestPath path : wisEnemyArmy.getOrderedPaths()) {
			moveEnemyToPlayerWithCost(path.getEnemy(), path.getPath());
		}
		
		grid.setVisitedToEmpty();
	}

	/**
	 * @param enemy {@summary Marca outros inimigos como casas proibidas, evitando
	 *              que dois inimigos fiquem, ao mesmo tempo, em uma casa s??}
	 */
	public void lockOtherEnemies(Enemy enemy) {
		for (Entity otherEnemy : allEnemies) {
			grid.setElementValue(otherEnemy.getGridX(), otherEnemy.getGridY(), VISITED);
		}
		grid.setElementValue(enemy.getGridX(), enemy.getGridY(), EMPTY);
	}

	/**
	 * Libera a trava que impede os inimigos de estarem juntos em uma mesma casa.
	 * Sempre execute essa fun????o ap??s executar {@link #lockOtherEnemies(Enemy)}
	 */
	public void unlockAllEnemies() {
		for (Entity enemy : allEnemies) {
			grid.setElementValue(enemy.getGridX(), enemy.getGridY(), EMPTY);
		}
	}

	/**
	 * @param enemy
	 * @param path  {@summary Move inimigo at?? o jogador respeitando o custo m??ximo
	 *              permitido pelo inimigo.}
	 */
	private void moveEnemyToPlayerWithCost(Enemy enemy, CheapestPath<Position, Integer> path) {
		Integer actualCost = 0;
		Position endPosition = new Position(enemy.getGridX(), enemy.getGridY());
		Integer initialTileCost = grid.getElementCost(endPosition);
		Position playerPosition = new Position(player.getGridX(), player.getGridY());
		for (Position p : path.getPath()) {
			actualCost += grid.getElementCost(p);

			if (stepMode) {
				// Movimenta????o passo a passo
				enemy.setGridX(p.getPosX());
				enemy.setGridY(p.getPosY());
				soundPlayer.play("enemyMove");
				delayPaint(msDelay);
			}
			if (actualCost > enemy.getMoves() + initialTileCost) {
				break;
			} else if (p.equals(playerPosition)) {
				endPosition = playerPosition;
				break;
			} else {
				endPosition = p;
			}

		}
		if (!stepMode) {
			// Movimenta????o direta
			enemy.setGridX(endPosition.getPosX());
			enemy.setGridY(endPosition.getPosY());
		}
	}
	
	/**
	 * @return the grid
	 */
	public GraphMatrix<Integer, Integer> getGrid() {
		return grid;
	}

	private boolean checkOverride(int x1, int y1, int x2, int y2) {
		return (x1 == x2 && y1 == y2);
	}

	private int gridToCoord(int v) {
		return v * tileSize;
	}

	private int coordToGrid(int v) {
		return (v - 1) / tileSize;
	}

	public boolean getRunning() {
		return running;
	}

	public int getScore() {
		return rounds;
	}
}
