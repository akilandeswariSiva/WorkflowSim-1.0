/*
 * 
 *  Copyright 2012-2013 University Of Southern California
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */
package org.workflowsim.clustering.balancing.methods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import org.workflowsim.Task;
import org.workflowsim.clustering.TaskSet;

/**
 *  HorizontalDistanceBalancing is a method that merges tasks based on distance metric
 * @author Weiwei Chen
 * @since WorkflowSim Toolkit 1.0
 * @date Apr 9, 2013
 */
public class HorizontalDistanceBalancing extends HorizontalImpactBalancing {

    /**
     * Initialize a HorizontalDistanceBalancing object
     * @param levelMap the level map
     * @param taskMap the task map
     * @param clusterNum the clusters.num
     */
    public HorizontalDistanceBalancing(Map levelMap, Map taskMap, int clusterNum) {
        super(levelMap, taskMap, clusterNum);
    }

    /**
     * The main function
     */
    @Override
    public void run() {
        Map<Integer, ArrayList<TaskSet>> map = getLevelMap();
        for (Iterator it = map.values().iterator(); it.hasNext();) {
            ArrayList<TaskSet> taskList = (ArrayList) it.next();
            process(taskList);
        }

    }
    

    /**
     * Sort taskSet based on their impact factors and then merge similar taskSet together
     * @param taskList 
     */
    public void process(ArrayList<TaskSet> taskList) {

        if (taskList.size() > getClusterNum()) {
            ArrayList<TaskSet> jobList = new ArrayList<TaskSet>();
            
            
            
                for (int i = 0; i < getClusterNum(); i++) {
                    jobList.add(new TaskSet());
                }
                int clusters_size = taskList.size() / getClusterNum();
                if(clusters_size * getClusterNum() < taskList.size()){
                    clusters_size ++;
                }

                //If you have vertical before horizontal make sure adjust this
                //for LIGO it is 2 for Genome  & CyberShake it is 4
                //clusters_size *= 2;
                //sortListDecreasing(taskList);
                preprocessing(taskList, jobList);

                for (TaskSet set : taskList) {
                    //sortListIncreasing(jobList);
                    TaskSet job = getCandidateTastSet(jobList, set, clusters_size);
                    addTaskSet2TaskSet(set, job);
                    job.addTask(set.getTaskList());
                    job.setImpactFafctor(set.getImpactFactor());
                    //update dependency
                    for (Task task : set.getTaskList()) {
                        getTaskMap().put(task, job);//this is enough
                        //impact factor is not updated
                    }

                }

                taskList.clear();//you sure?
            
        } else {
            //do nothing since 
        }


    }

        /**
     * Sort taskSet in an ascending order of impact factor
     * @param taskList taskSets to be sorted
     */

    private ArrayList sortDistanceIncreasing(int[][] distances, int size, int num) {
        ArrayList newList = new ArrayList<Integer>();
        //first two 
        int max = 0;
        int max_i = 0;
        int max_j = 0;
        for(int i = 0; i < size; i++){
            for(int j = 0; j < i; j++){
                if(distances[i][j] > max){
                    max = distances[i][j];
                    max_i = i;
                    max_j = j;
                }
            }
        }
        newList.add(max_i);
        newList.add(max_j);
        

        double max_dist = 0;
        max_i = 0;
        for(int id = 0; id < num - 2 ; id++){
            max_dist = 0;//bug fixed here
            for(int i = 0; i < size; i++){
                    double dist = getAvgDistance(i, newList, distances);
                    if(max_dist < dist){
                        max_dist = dist;
                        max_i = i;
                    }
            }
            if(max_dist == max){
                newList.add(max_i);
            }
        }
        return newList;
    }
    
    private double getAvgDistance(int id, ArrayList<Integer> list, int[][] distances){
        if(list.isEmpty()){
            double avg = 0.0;
            return avg;
        }
        double avg = 0.0;
        for(Iterator it = list.iterator();it.hasNext();){
            int nId = (Integer)it.next();
            if(nId > id){
                avg += distances[nId][id];
            }else if(nId == id){
                avg += 0.0;
            }
            else{
                avg += distances[id][nId];
            }
        }
        //now it is not zero
        avg = avg / list.size();
        return avg;
    }
    private ArrayList preprocessing(ArrayList<TaskSet> taskList, ArrayList<TaskSet> jobList){
        int size = taskList.size();
        int [] record  = new int[size];
        for(int i = 0; i < size; i++){
            record[i]= -1;
        }
        int index_record = 0;
        
        int[][] distances = new int[size][size];
        

        for(int i = 0; i < size; i++){
            for(int j = 0; j <i; j++){
                TaskSet setA = (TaskSet)taskList.get(i);
                TaskSet setB = (TaskSet)taskList.get(j);
                int distance = calDistance(setA, setB);
                
                distances[i][j] = distance;

            }
        }
        int job_index = 0;
        //boolean [] popped = new boolean[size];
        ArrayList idList = sortDistanceIncreasing(distances, size, jobList.size());
        for(int i = 0; i < idList.size(); i++){
            int max_i = (Integer)idList.get(i);
                
                record[index_record] = max_i;
                index_record ++;
                TaskSet set = (TaskSet)taskList.get(max_i);
                TaskSet job = jobList.get(job_index);
                addTaskSet2TaskSet(set, job);
                job.addTask(set.getTaskList());
                job.setImpactFafctor(set.getImpactFactor());
                //update dependency
                for (Task task : set.getTaskList()) {
                    getTaskMap().put(task, job);//this is enough
                    //impact factor is not updated
                }
                job_index ++;
                if(job_index == jobList.size()){
                    break;
                }
            
        }


        /**
         * Actually not really necessary because record[i] is already empty
         */
        Arrays.sort(record);
        for(int i = size -1 ;i>=0 && record[i]>=0;i--){
            taskList.remove(record[i]);
            
            
        }
        
        return taskList;
        
    }

    private ArrayList<TaskSet> getNextPotentialTaskSets(ArrayList<TaskSet> taskList, 
                                            TaskSet checkSet, int clusters_size){
        int dis = Integer.MAX_VALUE;

        HashMap map = new HashMap<Integer, ArrayList>();
        for (TaskSet set : taskList) {
                int distance = calDistance(checkSet, set);
                if(distance < dis){
                    dis = distance;
                }
                if(!map.containsKey(distance)){
                    map.put(distance, new ArrayList<TaskSet>());
                }
                ArrayList<TaskSet> list = (ArrayList)map.get(distance);
                if(!list.contains(set)){
                    list.add(set);
                }
        }
        ArrayList returnList = new ArrayList<TaskSet> ();
        for(TaskSet set: (ArrayList<TaskSet>)map.get(dis)){
            if(set.getTaskList().size() < clusters_size){
                returnList.add(set);
            }
        }
        
        if(returnList.isEmpty()){
            returnList.clear();
            for (TaskSet set : taskList) {
                if(set.getTaskList().isEmpty()){
                    returnList.add(set);
                    return returnList;
                }
            }
            
            //no empty available
            while(returnList.isEmpty() ){
                map.remove(dis);
                ArrayList<Integer> keys = new ArrayList(map.keySet());
                int min = Integer.MAX_VALUE;
                for(int i: keys){
                    if (min > i){
                        min = i;
                    }
                }
                dis = min;
                
                for(TaskSet set: (ArrayList<TaskSet>)map.get(dis)){
                    if(set.getTaskList().size() < clusters_size){
                        returnList.add(set);
                    }
                }
            }
            
            return returnList;
            
        }else{
            
            return returnList;
        }
        

    }
    
    
    /**
     * Gets the potential candidate taskSets to merge
     * @param taskList
     * @param checkSet
     * @return 
     */
    protected TaskSet getCandidateTastSet(ArrayList<TaskSet> taskList, 
                                            TaskSet checkSet, 
                                            int clusters_size) {
        
        
        
        ArrayList<TaskSet> potential = getNextPotentialTaskSets(taskList, checkSet,  clusters_size);
        TaskSet task = null;
        long min = Long.MAX_VALUE;
        for(TaskSet set: potential){
            if(set.getJobRuntime() < min){
                min = set.getJobRuntime();
                task = set;
            }
        }

        if (task != null) {
            return task;
        } else {
            return taskList.get(0);
        }
        

    }

    /**
     * Calculate the distance between two taskSet
     * one assumption here taskA and taskB are at the same level 
     * because it is horizontal clustering
     * does not work with arbitary workflows
     * @param taskA
     * @param taskB
     * @return 
     */
    private int calDistance(TaskSet taskA, TaskSet taskB) {
        if (taskA == null || taskB == null || taskA == taskB) {
            return 0;
        }
        LinkedList<TaskSet> listA = new LinkedList<TaskSet>();
        LinkedList<TaskSet> listB = new LinkedList<TaskSet>();
        int distance = 0;
        listA.add(taskA);
        listB.add(taskB);

        if (taskA.getTaskList().isEmpty() || taskB.getTaskList().isEmpty()) {
            return Integer.MAX_VALUE;
        }
        do {

            LinkedList<TaskSet> copyA = (LinkedList) listA.clone();
            listA.clear();
            for (TaskSet set : copyA) {
                for (TaskSet child : set.getChildList()) {
                    if (!listA.contains(child)) {
                        listA.add(child);
                    }
                }
            }
            LinkedList<TaskSet> copyB = (LinkedList) listB.clone();
            listB.clear();
            for (TaskSet set : copyB) {
                for (TaskSet child : set.getChildList()) {
                    if (!listB.contains(child)) {
                        listB.add(child);
                    }
                }
            }

            for (TaskSet set : listA) {
                if (listB.contains(set)) {
                    return distance * 2;
                }
            }

            distance++;

        } while (!listA.isEmpty() && !listB.isEmpty());

        return distance * 2;
    }
}
/*
 * An abstracion of Distance
 */
class Distance{
    int distance;
    int index_i;
    int index_j;
    public Distance(int distance, int index_i, int index_j){
        this.distance = distance;
        this.index_i = index_i;
        this.index_j = index_j;
    }
    public int getIndexI(){
        return this.index_i;
    }
    public int getIndexJ(){
        return this.index_j;
    }
    public int getDistance(){
        return this.distance;
    }
    
}