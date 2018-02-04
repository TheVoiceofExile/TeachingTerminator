import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

import java.util.Collections;

import pacsim.BFSPath;
import pacsim.PacAction;
import pacsim.PacCell;
import pacsim.PacFace;
import pacsim.PacSim;
import pacsim.PacUtils;
import pacsim.PacmanCell;

/**
 * University of Central Florida
 * CAP4630 - Spring 2018
 * Authors: Christopher Miller and David Jaffie
 */
public class PacSimRNNA implements PacAction
{
    private List<Point> path;
    private int simulationTime;
    private int numMovesMade;
    private boolean printedStuff = false;

    /**
     * @brief Construct the RNNA Agent and initialize it.
     */
    public PacSimRNNA(String fname)
    {
        PacSim sim = new PacSim(fname);
        sim.init(this);
    }


    /**
     * @brief Main method to generate the maze based on provided name.
     */
    public static void main(String[] args)
    {
        System.out.println("\nTSP using Repetitive Nearest Neighbor Algorithm by Christopher Miller and David Jaffie:");
        System.out.println("\nMaze : " + args[ 0 ] + "\n" );
        new PacSimRNNA(args[0]);
    }


    /**
     * @brief Resets the global variables to allow the simulation to run more
     *        than one time without restarting the program.
     */
    public void init()
    {
        numMovesMade = 0;
        path = new ArrayList<Point>();
    }


    /**
     * @brief Handles the calculations and choices for the RNNA Agent to
     *        determine what move to make.
     *
     * @todo Make pacman eat food
     */
    public PacFace action(Object state)
    {
        PacCell[][] grid = (PacCell[][]) state;
        PacmanCell pc = PacUtils.findPacman(grid);

        // Safety check to make sure Pacman is still in the game
        if (pc == null)
            return null;

        if(path.isEmpty())
        {
            List<Point> foodPellets = PacUtils.findFood(grid);

            int [][] costTable = findCostToFood(grid, pc, foodPellets);

            if(!printedStuff)
            {
                printFoodInMaze(foodPellets);
                printCostToFood(grid, costTable);
                printedStuff = true;
            }

            int numFoodPellets = 0;
            int currentCost = 0;
            int previousCost = 0;

            Point pcLocation = new Point();
            Point currentFoodTarget;

            List<FoodPellet> foodPelletsInMaze = new ArrayList<FoodPellet>();
            List<Point> previousPath = new ArrayList<Point>();

            List<Point> closestFoodPellets = new ArrayList<Point>();

            List<FoodPellet> foodPelletsInMazeCopy = new ArrayList<FoodPellet>();

            String pathTaken = "";

            long startTime = System.currentTimeMillis();

            for(int i  = 0; i < foodPellets.size(); i++)
            {
                numFoodPellets = 0;
                currentCost = 0;

                int capacity;

                System.out.println("\n\nPopulation at step " + (i + 1) + ":");

                if (foodPelletsInMaze.size() > foodPellets.size())
                {
                    capacity = foodPelletsInMaze.size();
                }
                else
                {
                    capacity = foodPellets.size();
                }

                for(int j = 0; j < capacity; j++)
                {
                    String previousPathString = "";

                    if(i == 0)
                    {
                        FoodPellet foodPellet = new FoodPellet();
                        foodPelletsInMaze.add(foodPellet);

                        pcLocation = pc.getLoc();
                        currentFoodTarget = foodPellets.get(j);
                    }
                    else
                    {
                        closestFoodPellets.clear();

                        pcLocation = foodPelletsInMaze.get(j).getPointAlongPath(i - 1);

                        closestFoodPellets = findClosestPellets(foodPelletsInMaze.get(j), foodPellets, costTable, pcLocation);
                        currentFoodTarget = closestFoodPellets.get(0);
                        System.out.println(closestFoodPellets);
                    }

                    previousPathString = foodPelletsInMaze.get(j).getPathToPellet();
                    previousPath = foodPelletsInMaze.get(j).getFullPath();
                    previousCost = foodPelletsInMaze.get(j).getCostToPellet();

                    currentCost = BFSPath.getPath(grid, pcLocation, currentFoodTarget).size();

                    foodPelletsInMaze.get(j).setCostToPellet(foodPelletsInMaze.get(j).getCostToPellet() + currentCost);

                    foodPelletsInMaze.get(j).addPointToPath(currentFoodTarget);

                    pathTaken = "[(" + (int)currentFoodTarget.getX() + "," + (int)currentFoodTarget.getY() + ")," + currentCost + "]";
                    foodPelletsInMaze.get(j).setPathToPellet(previousPathString + pathTaken);
                }

                foodPelletsInMazeCopy = foodPelletsInMaze;

                Collections.sort(foodPelletsInMazeCopy, new Comparator<FoodPellet>()
                {
                    @Override
                    public int compare(FoodPellet pellet1, FoodPellet pellet2)
                    {
                        if(pellet1.getCostToPellet() > pellet2.getCostToPellet())
                            return 1;
                        else if(pellet1.getCostToPellet() == pellet2.getCostToPellet())
                            return 0;
                        else
                            return -1;
                    }
                });

                for (int k = 0; k < foodPelletsInMazeCopy.size(); k++)
                {
                    System.out.println(numFoodPellets + " : currentCost = " + foodPelletsInMazeCopy.get(k).getCostToPellet() + " : " +
                            foodPelletsInMazeCopy.get(k).getPathToPellet());
                    numFoodPellets++;
                }
            }

            List<Point> finalSolution = null;

            path.add(foodPelletsInMazeCopy.get(0).getFullPath().get(0));

            for(int i = 0; i < foodPelletsInMazeCopy.get(0).getFullPath().size() - 1; i++)
            {
                finalSolution = BFSPath.getPath(grid, foodPelletsInMazeCopy.get(0).getFullPath().get(i), foodPelletsInMazeCopy.get(0).getFullPath().get(i + 1));

                for(int j = 0; j < finalSolution.size(); j++)
                {
                    path.add(finalSolution.get(j));
                }
            }

            long end = System.currentTimeMillis();
        }

        Point nextTarget = path.remove(0);
        PacFace face = PacUtils.direction(pc.getLoc(), nextTarget);
        numMovesMade++;

        System.out.println("\t" + numMovesMade + " : From [ " + (int)pc.getLoc().getX() + ", " + (int)pc.getLoc().getY() + " ] go " + face
                + " heading to: [ " + (int)nextTarget.getX() + ", " + (int)nextTarget.getY() + " ]");

        return face;
    }

    public void printFoodInMaze(List<Point> foodLocs)
    {
        System.out.println("\nFood Array:\n");

        for(int i = 0; i < foodLocs.size(); i++)
        {
            System.out.println((i + 1) + " : (" + (int)foodLocs.get(i).getX() + "," + (int)foodLocs.get(i).getY() + ")");
        }

        System.out.println("\n");
    }

    private int[][] findCostToFood(PacCell[][] grid, PacCell pacman, List<Point> foodLocs)
    {
        int tableSize = foodLocs.size() + 1;
        int[][] costTable = new int[tableSize][tableSize];

        for(int i  = 1; i < tableSize; i++)
        {
            int costTo = BFSPath.getPath(grid, pacman.getLoc(), foodLocs.get(i - 1)).size();

            costTable[0][i] = costTo;
            costTable[i][0] = costTo;
        }

        for(int i = 1; i < tableSize; i++)
        {
            for(int j = 1; j < tableSize; j++)
            {
                costTable[i][j] = BFSPath.getPath(grid, foodLocs.get(i - 1), foodLocs.get(j - 1)).size();
            }
        }

        return costTable;
    }

    public void printCostToFood(PacCell[][] grid, int[][] costTable)
    {
        int n = PacUtils.numFood(grid);
        System.out.println("Cost Table:\n");

        for(int i = 0; i < n + 1; i++)
        {
            for(int j = 0; j < n + 1; j++)
            {
                System.out.print(costTable[i][j] + "	");
            }
            System.out.println("\n");
        }
    }

    private List<Point> findClosestPellets(FoodPellet currentPellet, List<Point> food, int[][] costTable, Point pacmanLocation)
    {
        int minDistanceToPellet = Integer.MAX_VALUE;

        int pacmanIndex = food.indexOf(pacmanLocation) + 1;

        List<Point> selectedPoints = new ArrayList<Point>();

        for(int i = 0; i < food.size(); i++)
        {
            if(!(currentPellet.getFullPath().contains(food.get(i))))
            {
                if(costTable[i + 1][pacmanIndex] < minDistanceToPellet)
                {
                    selectedPoints.clear();
                    selectedPoints.add(food.get(i));
                    minDistanceToPellet = costTable[i + 1][pacmanIndex];
                }
                if(costTable[i + 1][pacmanIndex] == minDistanceToPellet)
                {
                    selectedPoints.add(food.get(i));
                }
            }
        }

        return selectedPoints;
    }

    public class FoodPellet
    {
        int costToPellet;
        String pathToPellet;
        List<Point> path;

        public FoodPellet()
        {
            costToPellet = 0;
            pathToPellet = "";
            path = new ArrayList<>();
        }


        public void setCostToPellet(int costToPellet)
        {
            this.costToPellet = costToPellet;
        }


        public int getCostToPellet()
        {
            return this.costToPellet;
        }


        public void setPathToPellet(String pathToPellet)
        {
            this.pathToPellet = pathToPellet;
        }


        public String getPathToPellet()
        {
            return this.pathToPellet;
        }


        public void addPointToPath(Point pointToAdd)
        {
            this.path.add(pointToAdd);
        }


        public Point getPointAlongPath(int indexOfPoint)
        {
            return path.get(indexOfPoint);
        }


        public List<Point> getFullPath()
        {
            return path;
        }


        public void setFullPath(List<Point> path)
        {
            this.path = path;
        }


        public void printFullPath()
        {
            for(Point point : path)
            {
                System.out.print(point + "  ");
            }
            System.out.println();
        }
    }

}