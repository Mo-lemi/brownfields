package za.co.wethinkcode.robots.server;

import za.co.wethinkcode.robots.server.obstacles.MountainObstacle;
import za.co.wethinkcode.robots.server.obstacles.Obstacle;
import za.co.wethinkcode.robots.server.obstacles.PitObstacle;
import za.co.wethinkcode.robots.server.servercommands.RobotsCommand;
import za.co.wethinkcode.robots.server.servercommands.ServerCommand;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class World {
    private int width;
    private int height;
    private final int shields;
    private final int shots;
    private final int reloadTime;
    private final int repairTime;
    private final List<Robot> robots;
    private final List<Obstacle> obstacles;
    private final List<PitObstacle> pits;
    private final List<int[]> mines;
    private final int lookDistance;
    private final boolean centeredCoordinates;
    private int setMineTime;

    public World() {
        Properties config = loadConfig();
        this.robots = new ArrayList<>();
        this.obstacles = new ArrayList<>();
        this.pits = new ArrayList<>();
        this.mines = new ArrayList<>();
        this.width = getPropertyInt("width", config);
        this.height = getPropertyInt("height", config);
        this.shields = getPropertyInt("shields", config);
        this.shots = getPropertyInt("shots", config);
        this.reloadTime = getPropertyInt("reloadTime", config);
        this.repairTime = getPropertyInt("repairTime", config);
        this.lookDistance = getPropertyInt("lookDistance", config);
        this.setMineTime = getPropertyInt("setMineTime", config);
        this.centeredCoordinates = true;
        setupConfiguredObstacles(config);
        setupConfiguredPits(config);
        ensureMinimumWorldObjects();
    }

    public World(int width, int height) {
        this.width = width;
        this.height = height;
        this.robots = new ArrayList<>();
        this.obstacles = new ArrayList<>();
        this.pits = new ArrayList<>();
        this.mines = new ArrayList<>();
        this.shields = 5;
        this.shots = 5;
        this.repairTime = 3;
        this.reloadTime = 2;
        this.lookDistance = 5;
        this.setMineTime = 3;
        this.centeredCoordinates = false;
    }

    private int getPropertyInt(String key, Properties config) {
        return Integer.parseInt(System.getProperty(key, config.getProperty(key)));
    }

    private boolean isConfigured(String property) {
        return property != null && !property.isBlank() && !"none".equalsIgnoreCase(property.trim());
    }

    private void setupConfiguredObstacles(Properties config) {
        String prop = System.getProperty("obstacles", config.getProperty("obstacles"));
        if (!isConfigured(prop)) {
            return;
        }
        for (int[] coord : parseCoordinateList(prop)) {
            addObstacle(new MountainObstacle(coord[0], coord[1]));
            System.out.println("Configured obstacle added at: [" + coord[0] + "," + coord[1] + "]");
        }
    }

    private void setupConfiguredPits(Properties config) {
        String prop = System.getProperty("pits", config.getProperty("pits"));
        if (!isConfigured(prop)) {
            return;
        }
        for (int[] coord : parseCoordinateList(prop)) {
            addPit(new PitObstacle(coord[0], coord[1]));
            System.out.println("Configured pit added at: [" + coord[0] + "," + coord[1] + "]");
        }
    }

    private List<int[]> parseCoordinateList(String specList) {
        List<int[]> coordinates = new ArrayList<>();
        for (String spec : specList.split(";")) {
            try {
                coordinates.add(parseCoordinates(spec));
            } catch (Exception e) {
                System.err.println("Failed to parse coordinate spec: " + spec);
            }
        }
        return coordinates;
    }

    private void ensureMinimumWorldObjects() {
        if (!obstacles.isEmpty() || !pits.isEmpty()) {
            return;
        }
        if (isValidPosition(getMaxX(), getMaxY())) {
            addObstacle(new MountainObstacle(getMaxX(), getMaxY()));
        }
        if (isValidPosition(getMinX(), getMaxY())) {
            addPit(new PitObstacle(getMinX(), getMaxY()));
        }
    }

    private int[] parseCoordinates(String spec) {
        String[] coords = spec.trim().split(",");
        if (coords.length != 2) {
            throw new IllegalArgumentException("expected x,y coordinates: " + spec);
        }
        return new int[]{
                Integer.parseInt(coords[0].trim()),
                Integer.parseInt(coords[1].trim())
        };
    }

    private Properties loadConfig() {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("config/world_config.properties")) {
            if (inputStream == null) {
                throw new RuntimeException("Config file not found in resources/config/");
            }
            properties.load(inputStream);
            return properties;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load world config: " + e.getMessage(), e);
        }
    }

    public int getMinX() { return centeredCoordinates ? -(width / 2) : 0; }
    public int getMaxX() { return centeredCoordinates ? (width / 2) : width - 1; }
    public int getMinY() { return centeredCoordinates ? -(height / 2) : 0; }
    public int getMaxY() { return centeredCoordinates ? (height / 2) : height - 1; }

    public void restoreState(World savedWorld) {
        this.width = savedWorld.getWidth();
        this.height = savedWorld.getHeight();
        this.robots.clear();
        this.obstacles.clear();
        this.pits.clear();
        this.mines.clear();
        this.obstacles.addAll(savedWorld.getObstacles());
        this.pits.addAll(savedWorld.getPits());
        for (int[] mine : savedWorld.getMines()) {
            if (mine != null && mine.length == 2) {
                this.placeMine(mine[0], mine[1]);
            }
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getReloadTime() { return reloadTime; }
    public int getRepairTime() { return repairTime; }
    public int getSetMineTime() { return setMineTime; }
    public int getLookDistance() { return lookDistance; }
    public int getShots() { return shots; }
    public int getShields() { return shields; }

    public boolean canPlaceMineAt(int x, int y) {
        return isWithinBounds(x, y) && !hasMineAt(x, y);
    }

    public boolean placeMine(int x, int y) {
        if (!canPlaceMineAt(x, y)) {
            return false;
        }
        mines.add(new int[]{x, y});
        System.out.println("Mine placed at [" + x + "," + y + "]");
        return true;
    }

    public boolean hasMineAt(int x, int y) {
        return getMineAt(x, y) != null;
    }

    public int[] getMineAt(int x, int y) {
        for (int[] mine : mines) {
            if (mine[0] == x && mine[1] == y) {
                return mine;
            }
        }
        return null;
    }

    public void explodeMineAt(int x, int y) {
        mines.removeIf(mine -> mine[0] == x && mine[1] == y);
    }

    public List<int[]> getMines() { return mines; }
    public List<Robot> getRobots() { return robots; }
    public List<Obstacle> getObstacles() { return obstacles; }

    public Obstacle getAObstacle(int x, int y) {
        return obstacles.stream()
                .filter(o -> o.getX() == x && o.getY() == y)
                .findFirst()
                .orElse(null);
    }

    public Robot getARobot(int x, int y) {
        return robots.stream()
                .filter(r -> r.getX() == x && r.getY() == y)
                .findFirst()
                .orElse(null);
    }

    public Robot getARobot(String name) {
        if (name == null) return null;
        return robots.stream()
                .filter(r -> r.getName().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(null);
    }

    public boolean isValidRobotName(String robotName) {
        for (Robot r : robots) {
            if (r.getName().equalsIgnoreCase(robotName.toLowerCase().trim())) {
                return false;
            }
        }
        return true;
    }

    public boolean isValidPosition(int x, int y) {
        if (!isWithinBounds(x, y)) {
            return false;
        }
        if (isOccupiedByRobot(x, y)) {
            return false;
        }
        return isObstaclePassable(x, y);
    }

    private boolean isObstaclePassable(int x, int y) {
        for (Obstacle obstacle : obstacles) {
            if (obstacle.getX() == x && obstacle.getY() == y) {
                return obstacle.isPassable();
            }
        }
        return true;
    }

    private boolean isOccupiedByRobot(int x, int y) {
        return getARobot(x, y) != null;
    }

    private boolean isWithinBounds(int x, int y) {
        boolean inX = (x >= getMinX()) && (x <= getMaxX());
        boolean inY = (y >= getMinY()) && (y <= getMaxY());
        return inX && inY;
    }

    public boolean isInsideBounds(int x, int y) {
        return isWithinBounds(x, y);
    }

    public void removeRobot(Robot robot) {
        robots.remove(robot);
        System.out.println("Robot " + robot.getName() + " removed from the world.");
    }

    public void addRobot(Robot robot) {
        if (isValidPosition(robot.getX(), robot.getY())) {
            robots.add(robot);
            System.out.println("Robot " + robot.getName() + " added at position (" + robot.getX() + ", " + robot.getY() + ")");
        } else {
            System.out.println("Invalid position for robot " + robot.getName() + ". Position outside world bounds.");
        }
    }

    public void addObstacle(Obstacle obstacle) {
        if (getAObstacle(obstacle.getX(), obstacle.getY()) != null) {
            System.out.println("Obstacle rejected: it overlaps an existing obstacle.");
            return;
        }
        if (isValidPosition(obstacle.getX(), obstacle.getY())) {
            obstacles.add(obstacle);
        } else {
            System.out.println("Invalid position for obstacle. Position outside world bounds.");
        }
    }

    public void addPit(PitObstacle pit) {
        pits.add(pit);
    }

    public boolean hasPitAt(int x, int y) {
        return getPitAt(x, y) != null;
    }

    public PitObstacle getPitAt(int x, int y) {
        for (PitObstacle pit : pits) {
            if (pit.getX() == x && pit.getY() == y) {
                return pit;
            }
        }
        return null;
    }

    public List<PitObstacle> getPits() { return pits; }

    public boolean checkForOpenSpaces() {
        for (int y = getMinY(); y <= getMaxY(); y++) {
            for (int x = getMinX(); x <= getMaxX(); x++) {
                if (isValidPosition(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String checkType(int x, int y) {
        if (!isWithinBounds(x, y)) {
            return "e";
        }
        if (isOccupiedByRobot(x, y)) {
            return "r";
        }
        if (getAObstacle(x, y) != null) {
            return "o";
        }
        if (hasPitAt(x, y)) {
            return "p";
        }
        return "";
    }

    public void WorldState() {
        System.out.println("World State: " + width + "x" + height);
        ServerCommand robotsCommand = new RobotsCommand(this);
        robotsCommand.execute();
        System.out.println("Obstacles:");
        for (Obstacle obstacle : obstacles) {
            System.out.println("- " + obstacle.getType() + " at (" + obstacle.getX() + ", " + obstacle.getY() + ")");
        }
        System.out.println("Pits:");
        for (PitObstacle pit : pits) {
            System.out.println("- pit at (" + pit.getX() + ", " + pit.getY() + ")");
        }
    }
}