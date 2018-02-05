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
    private int numMovesMade;
    private boolean printedStuff = false;
    private Point nextTarget = null;
    private long startTime;
    private long endTime;

    /**
     * @brief Construct the RNNA Agent and initialize it.
     *
     * @param[in] fname Name of the simulation.
     */
    public PacSimRNNA(String fname)
    {
        PacSim sim = new PacSim(fname);
        sim.init(this);
    }


    /**
     * @brief Main method to generate the maze based on provided name.
     *
     * @param[in] args Program arguments from the command line.
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
     * @param[in] state State of the current game board.
     */
    public PacFace action(Object state)
    {
        PacCell[][] grid = (PacCell[][]) state; // The grid as it is currently
        PacmanCell pc = PacUtils.findPacman(grid); // Keep pacman object held for later use

        // Safety check to make sure Pacman is still in the game
        if (pc == null)
            return null;

        // Don't run calculations if we've already generated the path.
        if(path.isEmpty())
        {
            List<Point> foodPellets = PacUtils.findFood(grid); // All food pellets on the board.

            int [][] costTable = findCostToFood(grid, pc, foodPellets); // Cost from each pellet to every other pellet.

            // We only need to print information this once.
            if(!printedStuff)
            {
                printCostToFood(grid, costTable);
                printFoodInMaze(foodPellets);
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

            startTime = System.currentTimeMillis();

            for(int i  = 0; i < foodPellets.size(); i++)
            {
                numFoodPellets = 0;
                currentCost = 0;

                int capacity; // Amount of paths we're currently looking at.

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

                        // Our location.
                        pcLocation = pc.getLoc();

                        // Where we want to be.
                        currentFoodTarget = foodPellets.get(j);

                        // Find cost to our target.
                        currentCost = BFSPath.getPath(grid, pcLocation, currentFoodTarget).size();

                        // Save the information relative to this pellet.
                        foodPelletsInMaze.get(j).setCostToPellet(foodPelletsInMaze.get(j).getCostToPellet() + currentCost);

                        // Add it to the path.
                        foodPelletsInMaze.get(j).addPointToPath(currentFoodTarget);

                        // Save as string for printing later
                        pathTaken = "[(" + (int)currentFoodTarget.getX() + "," + (int)currentFoodTarget.getY() + ")," + currentCost + "]";

                        foodPelletsInMaze.get(j).setPathToPellet(previousPathString + pathTaken);
                    }
                    else // Otherwise take the pellet we are currently considering and find the closest pellets to it
                    {
                        // For safety check.
                        closestFoodPellets.clear();

                        pcLocation = foodPelletsInMaze.get(j).getPointAlongPath(i - 1);

                        // Get the closests pellets to our current pellet and include pellets of same cost.
                        closestFoodPellets = findClosestPellets(foodPelletsInMaze.get(j), foodPellets, costTable, pcLocation);
                    }

                    if(closestFoodPellets.size() >= 1)
                    {
                        List<Point> templatePath = null; // For branching.
                        for(int k = 0; k < closestFoodPellets.size(); k++)
                        {
                            if(k > 0)
                            {
                                // Add the new branch as a path.
                                FoodPellet foodPellet = new FoodPellet(templatePath);
                                foodPelletsInMaze.add(foodPellet);

                                // Figure out where the new path is added.
                                int index = foodPelletsInMaze.lastIndexOf(foodPellet);

                                // Grab a path pellet.
                                currentFoodTarget = closestFoodPellets.get(k);

                                // Find cost to that pellet.
                                currentCost = BFSPath.getPath(grid, pcLocation, currentFoodTarget).size();

                                // Store our current path off to expand on later
                                previousPathString = foodPelletsInMaze.get(j).getPathToPellet();

                                // Store cost to pellet in new pellet.
                                foodPelletsInMaze.get(index).setCostToPellet(foodPelletsInMaze.get(j).getCostToPellet() + currentCost);

                                // Add pellet to path.
                                foodPelletsInMaze.get(index).addPointToPath(currentFoodTarget);

                                String templatePathString = ""; // Path in string to print later.

                                // Generate full path to store in string.
                                for (int m = 0; m < templatePath.size(); m++)
                                {
                                    templatePathString += "[(" + (int)templatePath.get(m).getX() + ","
                                                               + (int)templatePath.get(m).getY() + "),"
                                                               + foodPelletsInMaze.get(index).getCostToPellet() + "]";
                                }

                                // Path to current pellet.
                                pathTaken = "[(" + (int)currentFoodTarget.getX() + "," + (int)currentFoodTarget.getY() + ")," + currentCost + "]";

                                // Total path from first pellet to current pellet.
                                foodPelletsInMaze.get(index).setPathToPellet(templatePathString + pathTaken);
                            }
                            else
                            {
                                // Grab the first pellet found that is of lowest cost.
                                currentFoodTarget = closestFoodPellets.get(0);

                                // Get cost to pellet.
                                currentCost = BFSPath.getPath(grid, pcLocation, currentFoodTarget).size();

                                // Save away the path from our original pellet in case pellets of equal cost are found.
                                templatePath = new ArrayList<Point>(foodPelletsInMaze.get(j).getFullPath());

                                // Store our current path off to expand on later
                                previousPathString = foodPelletsInMaze.get(j).getPathToPellet();

                                // Set the cost to get to the pellet.
                                foodPelletsInMaze.get(j).setCostToPellet(foodPelletsInMaze.get(j).getCostToPellet() + currentCost);

                                // Add this pellet to the path.
                                foodPelletsInMaze.get(j).addPointToPath(currentFoodTarget);

                                // Generate string for new pellet in path.
                                pathTaken = "[(" + (int)currentFoodTarget.getX() + "," + (int)currentFoodTarget.getY() + "),"
                                             + currentCost + "]";

                                // Update the full path to the pellet.
                                foodPelletsInMaze.get(j).setPathToPellet(previousPathString + pathTaken);
                            }
                        }
                    }
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
                    System.out.println("\t" + numFoodPellets + " : currentCost = " + foodPelletsInMazeCopy.get(k).getCostToPellet() + " : " +
                        foodPelletsInMazeCopy.get(k).getPathToPellet());
                    numFoodPellets++;
                }
            }

            // List for our best solution
            List<Point> finalSolution = null;

            // Add points to solution.
            PacUtils.appendPointList(path, BFSPath.getPath(grid, pc.getLoc(), foodPelletsInMazeCopy.get(0).getFullPath().get(0)));

            // Get the total path for our final best solution
            for(int i = 1; i < foodPelletsInMazeCopy.get(0).getFullPath().size(); i++)
            {
                PacUtils.appendPointList(path, BFSPath.getPath(grid, foodPelletsInMazeCopy.get(0).getFullPath().get(i - 1), foodPelletsInMazeCopy.get(0).getFullPath().get(i)));
            }

            // End timer
            endTime = System.currentTimeMillis();

            long time = endTime - startTime;

            System.out.println("\n\nTime to Generate Plan: " + time + " msec\n");

            System.out.println("Solution Moves:\n");
        }

        if(!path.isEmpty())
            nextTarget = path.remove(0);

        // Face pacman in the direction for the next point
        PacFace face = PacUtils.direction(pc.getLoc(), nextTarget);

        // Track number of moves made in total
        numMovesMade++;

        // Print move number, where we are, where we are going, and which direction that is in
        System.out.printf("\t %3d : From [%3d, %3d] go %s\n", numMovesMade, (int)pc.getLoc().getX(), (int)pc.getLoc().getY(), face);

        return face;
    }

    // Print the location of the food in the puzzle

    /**
     * @brief Prints the food array for the maze.
     *
     * @param[in] foodLocs List of points where the food is located in the maze.
     */
    public void printFoodInMaze(List<Point> foodLocs)
    {
        System.out.println("\nFood Array:\n");

        for(int i = 0; i < foodLocs.size(); i++)
        {
            System.out.println((i) + " : (" + (int)foodLocs.get(i).getX() + "," + (int)foodLocs.get(i).getY() + ")");
        }

        System.out.println("\n");
    }

    /**
     * @brief Generates the cost table for all food in the maze.
     *
     * @param grid The maze to generate cost table for.
     * @param pacman Pacman object to find locaton of pacman.
     * @param foodLocs Location of all food in maze.
     *
     * @return 2D Cost table.
     */
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

    /**
     * @brief Prints the cost table.
     *
     * @param grid The maze to read the number of food pellets from.
     * @param costTable The cost table to be printed.
     */
    public void printCostToFood(PacCell[][] grid, int[][] costTable)
    {
        int n = PacUtils.numFood(grid);
        System.out.println("Cost Table:\n");

        for(int i = 0; i < n + 1; i++)
        {
            for(int j = 0; j < n + 1; j++)
            {
                System.out.printf("\t %3d", costTable[i][j]);
            }
            System.out.println("\n");
        }
    }

    // Find the next closest pellet to the one we're considering and add it to the path

    /**
     * @brief Finds a list of points that are the closest to the point we're looking at.
     *
     * @param currentPellet The pellet we're looking for pellets close to.
     * @param food The pellets contained within the maze.
     * @param costTable Cost table of all pellets to every other pellet.
     * @param pacmanLocation Pacman location.
     *
     * @return List of points representing the pellets closest to the current pellet.
     */
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

    /**
     * @class FoodPellet Represents a food pellet that is contained inside of the maze.
     */
    public class FoodPellet
    {
        int costToPellet; // How much it costs to get here
        String pathToPellet; // Path to get here in string form for printing
        List<Point> path; // List of points representing the path to get here.

        public FoodPellet()
        {
            costToPellet = 0;
            pathToPellet = "";
            path = new ArrayList<Point>();
        }

        public FoodPellet(List<Point> copyPath)
        {
            costToPellet = 0;
            pathToPellet = "";
            path = new ArrayList<Point>(copyPath);
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
    }

}
