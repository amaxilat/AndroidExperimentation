package eu.smartsantander.androidExperimentation;

import eu.smartsantander.androidExperimentation.entities.Experiment;

/**
 * Created with IntelliJ IDEA.
 * User: theodori
 * Date: 9/4/13
 * Time: 2:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class Main {
    public static  void main (String[] args){
        Experiment experiment = ModelManager.getExperiment();
        System.out.println(experiment.getId());
    }
}
