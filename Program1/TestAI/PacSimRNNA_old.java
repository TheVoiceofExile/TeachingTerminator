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

            Point pcLocation = new Point();
            Point currentFoodTarget;

            List<FoodPellet> foodPelletsInMaze = new ArrayList<FoodPellet>();

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

                // If we have more partial paths than there are pellets in the maze then make our capacity the bigger
                // of the two.
                if (foodPelletsInMaze.size() > foodPellets.size())
                {
                    capacity = foodPelletsInMaze.size();
                }
                else
                {
                    capacity = foodPellets.size();
                }

                // Update all paths we have thusfar
                for(int j = 0; j < capacity; j++)
                {
                    String previousPathString = "";
                    // At i == 0 we have not considered any pellets yet, this initializes it
                    if(i == 0)
                    {
                        FoodPellet foodPellet = new FoodPellet();
                        foodPelletsInMaze.add(foodPellet);

                        pcLocation = pc.getLoc();
                        currentFoodTarget = foodPellets.get(j);
                        //System.out.println(foodPelletsInMaze);
                    }
                    else // Otherwise take the pellet we are currently considering and find the closest pellets to it
                    {
                        closestFoodPellets.clear();

                        pcLocation = foodPelletsInMaze.get(j).getPointAlongPath(i - 1);

                        closestFoodPellets = findClosestPellets(foodPelletsInMaze.get(j), foodPellets, costTable, pcLocation);

                        currentFoodTarget = closestFoodPellets.get(0);

                        
                        //System.out.println(closestFoodPellets);
                    }

                    // Store our current path off to expand on later
                    previousPathString = foodPelletsInMaze.get(j).getPathToPellet();

                    // Figure out what our current cost is
                    currentCost = BFSPath.getPath(grid, pcLocation, currentFoodTarget).size();

                    // Store the cost to the pellet
                    foodPelletsInMaze.get(j).setCostToPellet(foodPelletsInMaze.get(j).getCostToPellet() + currentCost);

                    // Add the point to our path to get to that point
                    // This may be related to partial paths
                    foodPelletsInMaze.get(j).addPointToPath(currentFoodTarget);

                    // Print out the point we're considering and the cost to get there
                    pathTaken = "[(" + (int)currentFoodTarget.getX() + "," + (int)currentFoodTarget.getY() + ")," + currentCost + "]";

                    // Store path considered for this population level
                    foodPelletsInMaze.get(j).setPathToPellet(previousPathString + pathTaken);
                }

                // Copy it out so we don't sort our actual path
                foodPelletsInMazeCopy = foodPelletsInMaze;

                // Comparator to allow the comparison of FoodPellet objects
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

                // Print the cost for each path in consdieration, where it is, and how we currently plan on getting there.
                for (int k = 0; k < foodPelletsInMazeCopy.size(); k++)
                {
                    System.out.println(numFoodPellets + " : currentCost = " + foodPelletsInMazeCopy.get(k).getCostToPellet() + " : " +
                        foodPelletsInMazeCopy.get(k).getPathToPellet());
                    numFoodPellets++;
                }
            }

            // List for our best solution
            List<Point> finalSolution = null;

            // 0 index is our best because it is sorted
            path.add(foodPelletsInMazeCopy.get(0).getFullPath().get(0));

            // Get the total path for our final best solution
            for(int i = 0; i < foodPelletsInMazeCopy.get(0).getFullPath().size() - 1; i++)
            {
                finalSolution = BFSPath.getPath(grid, foodPelletsInMazeCopy.get(0).getFullPath().get(i), foodPelletsInMazeCopy.get(0).getFullPath().get(i + 1));

                for(int j = 0; j < finalSolution.size(); j++)
                {
                    path.add(finalSolution.get(j));
                }
            }

            // End timer
            long end = System.currentTimeMillis();
        }

        // As we reach a point remove it from the list so the next one can be navigated to
        Point nextTarget = path.remove(0);

        // Face pacman in the direction for the next point
        PacFace face = PacUtils.direction(pc.getLoc(), nextTarget);

        // Track number of moves made in total
        numMovesMade++;

        // Print move number, where we are, where we are going, and which direction that is in
        System.out.println("\t" + numMovesMade + " : From [ " + (int)pc.getLoc().getX() + ", " + (int)pc.getLoc().getY() + " ] go " + face
                + " heading to: [ " + (int)nextTarget.getX() + ", " + (int)nextTarget.getY() + " ]");

        return face;
    }

    // Print the location of the food in the puzzle
    public void printFoodInMaze(List<Point> foodLocs)
    {
        System.out.println("\nFood Array:\n");

        for(int i = 0; i < foodLocs.size(); i++)
        {
            System.out.println((i + 1) + " : (" + (int)foodLocs.get(i).getX() + "," + (int)foodLocs.get(i).getY() + ")");
        }

        System.out.println("\n");
    }

    // Generate our cost table
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

    // Print the cost table
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

    // Find the next closest pellet to the one we're considering and add it to the path
    private List<Point> findClosestPellets(FoodPellet currentPellet, List<Point> food, int[][] costTable, Point pacmanLocation)
    {
        // Make big number
        int minDistanceToPellet = Integer.MAX_VALUE;

        // Grab our location
        int pacmanIndex = food.indexOf(pacmanLocation) + 1;

        List<Point> selectedPoints = new ArrayList<Point>();

        for(int i = 0; i < food.size(); i++)
        {
            // If the point is already in our path we don't need to add it again
            if(!(currentPellet.getFullPath().contains(food.get(i))))
            {
                // Only if the cost is the lowest do we care
                if(costTable[i + 1][pacmanIndex] < minDistanceToPellet)
                {
                    selectedPoints.clear();
                    selectedPoints.add(food.get(i));
                    minDistanceToPellet = costTable[i + 1][pacmanIndex];
                }
                // Consideration for partial paths, NOT WORKING
                else if(costTable[i + 1][pacmanIndex] == minDistanceToPellet)
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
