import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import pacsim.BFSPath;
import pacsim.PacAction;
import pacsim.PacCell;
import pacsim.PacFace;
import pacsim.PacSim;
import pacsim.PacUtils;
import pacsim.PacmanCell;

/**
 * University of Central Florida
 * CAP4630 - Fall 2017
 * Authors: Christopher Miller and David Jaffie
 */
public class PacSimRNNA implements PacAction
{
    private List<Point> path;
    private int numMovesMade;
    
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
     * @todo Make pacman eat balls
     */
    public PacFace action(Object state)
    {
        PacCell[][] grid = (PacCell[][]) state;
        PacmanCell pc = PacUtils.findPacman(grid);

        // Safety check to make sure Pacman is still in the game
        if (pc == null)
            return null;

        
    }
}