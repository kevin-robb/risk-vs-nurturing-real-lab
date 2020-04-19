import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

/**
 * Receives data about each agent for each generation, 
 * including final learning parameter value and
 * list of every choice made and reward received.
 * Runs simulation based on setup parameters.
 * 
 * Writes to tab-separated .txt file in desktop folder CodeOutput.
 * If running on different machine, change user in outputPathName
 * 
 * @author Kevin Robb
 * @version 5/11/2018
 * Referenced code from Steven Roberts.
 */
public class AnalysisDriver {

    /**keeps track of the current generation being evaluated. */
    static int currentGeneration = 0;

    public static void main(String[] args)
    {
        //File for writing is runModifier + "output_" + identifier + ".txt""
        Calendar now = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmssSSS");
        String outputPathName = "";
        String outputPathIdentifier = sdf.format(now.getTime());
        outputPathName= "C:/Users/kevin/Desktop/CodeOutput/output_" + outputPathIdentifier + ".txt";

        BufferedWriter out = null;
        try
        {
            out = new BufferedWriter(new FileWriter(new File(outputPathName)));
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
        }

        try
        {
            Setup.setConfig(args);
        }
        /*
         * setConfig() throws an IOException when the config file is not found.
         * This is really not that important to the successful execution of the
         * program, but is important to the meaning of the output. Should probably
         * tell someone when this happens.
         */
        catch (IOException e1)
        {
            if (e1.getMessage().equals("config.txt"))
            {
                System.out.println("The configuration file was not found."
                        + " Using default parameters.");
            }
            else System.out.println(e1.getMessage());
        }
        //parseStates() throws an Exception (different from the IOException caught above) when
        //the states for choices are not the proper format. This is unrecoverable, so the program quits.
        catch (Exception e1)
        {
            try
            {
                out.write("\n" + e1.getMessage());
            }
            catch (IOException e2)
            {
                //At this point, the program is about to quit,
                //so I don't care about the IOException.
            }
            System.out.println("\n" + e1.getMessage());
            System.exit(0);
        }

        //creates first generation and begin the program
        if (Setup.graphingL)
        	runGraphing(out);
        else
        	runSummary(out);
    }

    /**
     * Runs the simulation. At top of output file, displays all seed config values
     * followed by a blank line.
     * For each generation, outputs "Gen " + gen # on the first line
     * After the agent data, or on the next line if not a mod 10 generation,
     * outputs "Summary: " + avg propA, avg fitness, avg L value, min L, and max L
     * all from end of gen, separated by commas.
     * Every 10th generation, each agent has a line containing proportion of A choices, 
     * fitness, and final L value separated by commas on one line each.
     * The end of every generation is signified by a blank line in the output.
     * @param out the writer to which output should be sent
     */
    public static void runSummary(BufferedWriter out)
    {
        //fitness of an individual at the end of its life. used to rank outputs for each gen
        double fitnessAgent = 0;
        //total of all agent fitness in generation at end. used to find average fitness
        double fitnessTotal = 0;
        //the percent an agent's choices that were A during a time period
        //excluding safe exploration. range 0-1
        double propA = -1; 
        //total of all propA in gen. used to find average propA.
        double propATotal = 0;
        //the final learningParameter value of a single agent. Correlated
        //to propA. range 0-1
        double finalLAgent = -1;
        //the total of all L values in gen. used to compute average at end
        double LForGenTotal = 0;
        //the minimum learningParameter value among the entire gen, at the end
        double minLForGen = 2;
        //the maximum learningParameter value among the entire gen, at the end
        double maxLForGen = -1;
        //obtained from Generation.choices array. in form "A100", meaning
        //A was chosen and a reward of 100 was received.
        String choiceInfo="";
        //number of times an agent chose A during a lifetime. Used for propA calculation.
        int a = 0;
        //number of trials recorded. measured this way to avoid needing
        //to perform numberOfTrials - nurturingTrials
        int numTrials = 0;

        try
        {
            //outputs config info and format info at top of file
            out.write(Setup.parameters() + String.format("%n") + "Format: \"Gen \"currentGeneration" 
                    + String.format("%n") + "\tpropA\tfitnessAgent\tfinalLAgent\t\texpectedA\texpectedB" + String.format("%n")
                    + "(previous line displays for every agent in a gen, but only every 10th gen)"
                    + String.format("%n") + "\"Summary: \"avgPropA\tavgFitness\tavgLForGen\tminLForGen\tmaxLForGen"
                    + String.format("%n") + String.format("%n"));
            //creates generation object. forms first gen of agents
            Generation g = new Generation();
            //dispays actual values of each choice on next line
            out.write("Actual Vals:\t" + Setup.stateVals[0] + "\t" + Setup.stateVals[1] + 
                    "\t" + Setup.stateVals[2] + "\t" + Setup.stateVals[3] + String.format("%n"));
            
            while (currentGeneration < Setup.numberOfGens)
            {
                g.runGeneration();
                //reset values for new gen
                minLForGen = Integer.MAX_VALUE;
                maxLForGen = Integer.MIN_VALUE;
                LForGenTotal = 0;
                propATotal = 0;
                fitnessTotal = 0;
                
                //export data then clear it for next gen
                //calculate number of times each chosen, and final learning param of each agent

                out.write(String.format("%n") + "Gen " + currentGeneration + String.format("%n"));
                //only output specific agent data every 10th generation but need to calculate every time
                for (int agentNum = 0; agentNum < Setup.numberOfAgents; agentNum++)
                {
                    //part for finding min and max L of generation
                    if (g.allAgents[agentNum].getLearningParameter() > maxLForGen)
                        maxLForGen = g.allAgents[agentNum].getLearningParameter();
                    if (g.allAgents[agentNum].getLearningParameter() < minLForGen)
                        minLForGen = g.allAgents[agentNum].getLearningParameter();

                    //part for calculating propA
                    a = 0; numTrials = 0;
                    //if trialNum < Setup.nurturingTrials, don't include in fitness calc
                    for (int trialNum = Setup.nurturingTrials; trialNum < Setup.numberOfTrials; trialNum++)
                    {
                        choiceInfo = g.choices[agentNum][trialNum];
                        if (choiceInfo.charAt(0) == 'A')
                            a++;
                        numTrials++;
                    }
                    propA = (double) a / numTrials;
                    finalLAgent = g.allAgents[agentNum].getLearningParameter();
                    fitnessAgent = g.allAgents[agentNum].getFitness();
                    propATotal += propA;
                    fitnessTotal += fitnessAgent;
                    LForGenTotal += finalLAgent;
                    //part for outputting specific agent data. only every 10th gen and only if config
                    if (currentGeneration % 10 == 0 && Setup.printAgentData)
                    {
                        //outputs all agent values at end of gen
                        out.write("\t" + String.format("%1.2f", propA) + "\t" 
                                + String.format("%.1f", fitnessAgent) + "\t" 
                                + String.format("%.5f", finalLAgent));
                        //shows expected rewards for A and B
                        out.write("\t\t" + String.format("%.5f", g.allAgents[agentNum].getExpectedRewards()[0]) 
                        + "\t" + String.format("%.5f", g.allAgents[agentNum].getExpectedRewards()[1]));
                        out.write(String.format("%n"));
                    }
                }
                out.write("Summary: " + String.format("%.2f", propATotal/Setup.numberOfAgents) + "\t" 
                        + String.format("%.3f", fitnessTotal/Setup.numberOfAgents) + "\t"
                        + String.format("%.5f", LForGenTotal/Setup.numberOfAgents) + "\t" 
                        + String.format("%.5f", minLForGen) + "\t" 
                        + String.format("%.5f", maxLForGen) + String.format("%n"));

                //don't form new gen if currently last gen
                if (currentGeneration != Setup.numberOfGens - 1)
                    g.formNewGeneration();
                fitnessAgent = 0;
                currentGeneration++;
            }
            out.flush();
            out.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }
    
    /**
     * Runs the simulation. Writes data convenient for graphing.
     * If all 3 params false, prints currentGeneration and avg L value for each gen on separate lines.
     * If Setup.printAgentData = true, prints min L 1st Q L, avg L, 
     * 3rd Q, and max L on one line.
     * If Setup.printFitness = true, prints pairs of agent L with 
     * fitness for every agent in every 50th gen.
     * If Setup.printPropAvsExpB = true, prints pairs of propA with 
     * end-of-life expected value of B for each agent in every 50th gen.
     * Gen groups of agents in the previous 2 cases are separated by a blank line.
     * @param out the writer to which output should be sent
     */
    public static void runGraphing(BufferedWriter out)
    {
        //the total of all L values in gen. used to compute average at end
        double LForGenTotal = 0;
        //the minimum learningParameter value among the entire gen, at the end
        double minLForGen = 0;
        //the maximum learningParameter value among the entire gen, at the end
        double maxLForGen = 0;
        //index of first quartile
        int firstQindex = Setup.numberOfAgents / 4;
        //index of third quartile
        int thirdQindex = Setup.numberOfAgents * 3 / 4;
        //L value at first quartile
        double LfirstQ = 0;
        //L value at third quartile
        double LthirdQ = 0;
        //stores L values and will be sorted to find quartiles, min and max
        double[] Lvalues = new double[Setup.numberOfAgents];
        //the percent an agent's choices that were A during a time period
        //excluding safe exploration. range 0-1
        double propA = -1; 
        //obtained from Generation.choices array. in form "A100", meaning
        //A was chosen and a reward of 100 was received.
        String choiceInfo="";
        //number of times an agent chose A during a lifetime. Used for propA calculation.
        int a = 0;
        //number of trials recorded. measured this way to avoid needing
        //to perform numberOfTrials - nurturingTrials
        int numTrials = 0;
        try
        {
            //creates generation object. forms first gen of agents
            Generation g = new Generation();
            
            while (currentGeneration < Setup.numberOfGens)
            {
                g.runGeneration();
                //reset values for new gen
                //minLForGen = Integer.MAX_VALUE;
                //maxLForGen = Integer.MIN_VALUE;
                LForGenTotal = 0;
                //calculate average L for the generation
                for (int agentNum = 0; agentNum < Setup.numberOfAgents; agentNum++)
                {
                	if (Setup.printAgentData && !Setup.printFitness && !Setup.printPropAvsExpB)
                	{ /*
                		//part for finding min, max, q1, q2 for gen
                        if (g.allAgents[agentNum].getLearningParameter() > maxLForGen)
                            maxLForGen = g.allAgents[agentNum].getLearningParameter();
                        if (g.allAgents[agentNum].getLearningParameter() < minLForGen)
                            minLForGen = g.allAgents[agentNum].getLearningParameter();
                        */
                        Lvalues[agentNum] = g.allAgents[agentNum].getLearningParameter();
                	}
                    LForGenTotal += g.allAgents[agentNum].getLearningParameter();
                    
                    if (Setup.printPropAvsExpB && !Setup.printFitness && !Setup.printAgentData && currentGeneration % 50 == 0) {
                    	//writes propA (x) and expected val of B (y)
                    	
                    	//part for calculating propA
	                    a = 0; numTrials = 0;
	                    //if trialNum < Setup.nurturingTrials, don't include in fitness calc
	                    for (int trialNum = Setup.nurturingTrials; trialNum < Setup.numberOfTrials; trialNum++)
	                    {
	                        choiceInfo = g.choices[agentNum][trialNum];
	                        if (choiceInfo.charAt(0) == 'A')
	                            a++;
	                        numTrials++;
	                    }
	                    propA = (double) a / numTrials;
	                    
	                    out.write(propA + "\t" + String.format("%.5f", g.allAgents[agentNum].getExpectedRewards()[1]) + String.format("%n"));
                    }
                    
                    
                    if (!Setup.printPropAvsExpB && Setup.printFitness && currentGeneration % 50 == 0) { 
                    	//writes L (x) and fitness (y) of every individual, every 50 gens
                    	out.write(String.format("%.5f", g.allAgents[agentNum].getLearningParameter()) + "\t" 
                    	+ String.format("%.3f", g.allAgents[agentNum].getFitness()) + String.format("%n"));
                    }
                }
                if (Setup.printAgentData && !Setup.printPropAvsExpB && !Setup.printFitness)
                {
                    Arrays.sort(Lvalues);
                    
                    minLForGen = Lvalues[0];
                    maxLForGen = Lvalues[Setup.numberOfAgents - 1];
                    LfirstQ = Lvalues[firstQindex];
                    LthirdQ = Lvalues[thirdQindex];
                    
                	out.write(currentGeneration + "\t"+ String.format("%.5f", minLForGen) + "\t" 
                			+ String.format("%.5f", LfirstQ) + "\t" 
                			+ String.format("%.5f", LForGenTotal/Setup.numberOfAgents) + "\t" 
                			+ String.format("%.5f", LthirdQ) + "\t" 
                            + String.format("%.5f", maxLForGen) + String.format("%n"));
                } else if(!Setup.printPropAvsExpB && !Setup.printFitness) { 
                	//use generation as x and avgLForGen as y
                	out.write(currentGeneration + "\t" + String.format("%.5f", LForGenTotal/Setup.numberOfAgents)
                	+ String.format("%n"));
                } else if ((Setup.printPropAvsExpB || Setup.printFitness) && currentGeneration % 50 == 0) { 
                	//must be printing L(x) and fitness (y) or propA (x) and expB (y),
                	// so insert blank line between sets of agents
                	out.write(String.format("%n"));
                }
                
                //don't form new gen if currently last gen
                if (currentGeneration != Setup.numberOfGens - 1)
                    g.formNewGeneration();
                currentGeneration++;
            }
            out.flush();
            out.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }
}
