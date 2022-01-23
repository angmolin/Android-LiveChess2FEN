/*
 * https://github.com/chrfrantz/DBSCAN/
 */

package com.angmolin.livechess2fen.chessboard.algorithm.dbscan;

import com.angmolin.livechess2fen.chessboard.algorithm.dbscan.exceptions.DBException;

import java.util.LinkedList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class DBSCAN<T> {

    private double epsilon = 1f;
    private int minimumNumberOfClusterMembers = 2;
    private DBDistanceMetric<T> metric = null;
    private LinkedList<T> inputValues = null;
    private HashSet<T> visitedPoints = new HashSet<T>();

    public DBSCAN(final Collection<T> inputValues, int minNumElements, double maxDistance, DBDistanceMetric<T> metric) throws DBException {
        setInputValues(inputValues);
        setMinimalNumberOfMembersForCluster(minNumElements);
        setMaximalDistanceOfClusterMembers(maxDistance);
        setDBDistanceMetric(metric);
    }

    public void setDBDistanceMetric(final DBDistanceMetric<T> metric) throws DBException {
        if (metric == null) {
            throw new DBException("DBSCAN: Distance metric has not been specified (null).");
        }

        this.metric = metric;
    }

    public void setInputValues(final Collection<T> collection) throws DBException {
        if (collection == null) {
            throw new DBException("DBSCAN: List of input values is null.");
        }
        this.inputValues = new LinkedList<T>(collection);
    }

    public void setMinimalNumberOfMembersForCluster(final int minimalNumberOfMembers) {
        this.minimumNumberOfClusterMembers = minimalNumberOfMembers;
    }

    public void setMaximalDistanceOfClusterMembers(final double maximalDistance) {
        this.epsilon = maximalDistance;
    }

    private LinkedList<T> getNeighbours(final T inputValue) throws DBException {
        LinkedList<T> neighbours = new LinkedList<T>();
        for(int i=0; i<inputValues.size(); i++) {
            T candidate = inputValues.get(i);
            if (metric.calculateDistance(inputValue, candidate) <= epsilon) {
                neighbours.add(candidate);
            }
        }
        return neighbours;
    }

    private List<T> mergeRightToLeftCollection(final List<T> neighbours1,
                                                    final List<T> neighbours2) {
        for (int i = 0; i < neighbours2.size(); i++) {
            T tempPt = neighbours2.get(i);
            if (!neighbours1.contains(tempPt)) {
                neighbours1.add(tempPt);
            }
        }
        return neighbours1;
    }

    public List<List<T>> performClustering() throws DBException {

        if (inputValues == null) {
            throw new DBException("DBSCAN: List of input values is null.");
        }

        if (inputValues.isEmpty()) {
            throw new DBException("DBSCAN: List of input values is empty.");
        }

        if (inputValues.size() < 2) {
            throw new DBException("DBSCAN: Less than two input values cannot be clustered. Number of input values: " + inputValues.size());
        }

        if (epsilon < 0) {
            throw new DBException("DBSCAN: Maximum distance of input values cannot be negative. Current value: " + epsilon);
        }

        if (minimumNumberOfClusterMembers < 2) {
            throw new DBException("DBSCAN: Clusters with less than 2 members don't make sense. Current value: " + minimumNumberOfClusterMembers);
        }

        List<List<T>> resultList = new LinkedList<>();
        visitedPoints.clear();

        List<T> neighbours;
        int index = 0;

        while (inputValues.size() > index) {
            T p = inputValues.get(index);
            
            if (!visitedPoints.contains(p)) {
                visitedPoints.add(p);
                neighbours = getNeighbours(p);

                if (neighbours.size() >= minimumNumberOfClusterMembers) {
                    int ind = 0;
                    
                    while (neighbours.size() > ind) {
                        T r = neighbours.get(ind);
                        if (!visitedPoints.contains(r)) {
                            visitedPoints.add(r);
                            LinkedList<T> individualNeighbours = getNeighbours(r);
                            if (individualNeighbours.size() >= minimumNumberOfClusterMembers) {
                                neighbours = mergeRightToLeftCollection(
                                        neighbours,
                                        individualNeighbours);
                            }
                        }
                        
                        ind++;
                    }
                    
                    resultList.add(neighbours);
                }
            }
            
            index++;
        }
        
        return resultList;
    }

}
