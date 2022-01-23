/*
 * https://github.com/valenpe7/bentley-ottmann
 */

package com.angmolin.livechess2fen.chessboard.algorithm.bentleyottmann;

import com.angmolin.livechess2fen.chessboard.algorithm.bentleyottmann.types.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeSet;

public class BentleyOttmann {

    private Queue<BOEvent> events;
    private NavigableSet<BOSegment> segments;
    private ArrayList<BOPoint> points;

    public BentleyOttmann(List<BOSegment> segments) {
        this.events = new PriorityQueue<>(new BOEventComparator());
        this.segments = new TreeSet<>(new BOSegmentComparator());
        this.points = new ArrayList<>();

        for (BOSegment s : segments) {
            this.events.add(new BOEvent(s.first(), s, 0));
            this.events.add(new BOEvent(s.second(), s, 1));
        }
    }

    public void findIntersections() {
        while(!this.events.isEmpty()) {
            BOEvent e = this.events.poll();
            double L = e.getValue();
            
            switch(e.getType()) {
                case 0:
                    for (BOSegment s : e.getSegments()) {
                        this.recalculate(L);
                        this.segments.add(s);
                        
                        if (this.segments.lower(s) != null) {
                            BOSegment r = this.segments.lower(s);
                            this.reportIntersection(r, s, L);
                        }
                        
                        if (this.segments.higher(s) != null) {
                            BOSegment t = this.segments.higher(s);
                            this.reportIntersection(t, s, L);
                        }
                        
                        if (this.segments.lower(s) != null && this.segments.higher(s) != null) {
                            BOSegment r = this.segments.lower(s);
                            BOSegment t = this.segments.higher(s);
                            this.removeFuture(r, t);
                        }
                    }
                    break;
                case 1:
                    for (BOSegment s : e.getSegments()) {
                        if (this.segments.lower(s) != null && this.segments.higher(s) != null) {
                            BOSegment r = this.segments.lower(s);
                            BOSegment t = this.segments.higher(s);
                            this.reportIntersection(r, t, L);
                        }
                        
                        this.segments.remove(s);
                    }
                    break;
                case 2:
                    BOSegment s_1 = e.getSegments().get(0);
                    BOSegment s_2 = e.getSegments().get(1);
                    this.swap(s_1, s_2);
                    
                    if (s_1.getValue() < s_2.getValue()) {
                        if (this.segments.higher(s_1) != null) {
                            BOSegment t = this.segments.higher(s_1);
                            this.reportIntersection(t, s_1, L);
                            this.removeFuture(t, s_2);
                        }
                        
                        if (this.segments.lower(s_2) != null) {
                            BOSegment r = this.segments.lower(s_2);
                            this.reportIntersection(r, s_2, L);
                            this.removeFuture(r, s_1);
                        }
                    }
                    else {
                        if (this.segments.higher(s_2) != null) {
                            BOSegment t = this.segments.higher(s_2);
                            this.reportIntersection(t, s_2, L);
                            this.removeFuture(t, s_1);
                        }
                        
                        if (this.segments.lower(s_1) != null) {
                            BOSegment r = this.segments.lower(s_1);
                            this.reportIntersection(r, s_1, L);
                            this.removeFuture(r, s_2);
                        }
                    }
                    
                    this.points.add(e.getPoint());
                    break;
            }
        }
    }

    private boolean reportIntersection(BOSegment s1, BOSegment s2, double l) {
        double x1 = s1.first().x;
        double y1 = s1.first().y;
        double x2 = s1.second().x;
        double y2 = s1.second().y;
        double x3 = s2.first().x;
        double y3 = s2.first().y;
        double x4 = s2.second().x;
        double y4 = s2.second().y;

        double r = (x2 - x1) * (y4 - y3) - (y2 - y1) * (x4 - x3);

        if (r != 0) {
            double t = ((x3 - x1) * (y4 - y3) - (y3 - y1) * (x4 - x3)) / r;
            double u = ((x3 - x1) * (y2 - y1) - (y3 - y1) * (x2 - x1)) / r;

            if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
                double xC = x1 + t * (x2 - x1);
                double yC = y1 + t * (y2 - y1);

                if (xC > l) {
                    this.events.add(new BOEvent(new BOPoint(xC, yC), new ArrayList<>(Arrays.asList(s1, s2)), 2));
                    return true;
                }
            }
        }

        return false;
    }

    private boolean removeFuture(BOSegment s1, BOSegment s2) {
        for (BOEvent e : this.events) {
            if (e.getType() == 2) {
                if((e.getSegments().get(0) == s1 && e.getSegments().get(1) == s2) || (e.getSegments().get(0) == s2 && e.getSegments().get(1) == s1)) {
                    this.events.remove(e);
                    return true;
                }
            }
        }

        return false;
    }

    private void swap(BOSegment s1, BOSegment s2) {
        this.segments.remove(s1);
        this.segments.remove(s2);

        double value = s1.getValue();

        s1.setValue(s2.getValue());
        s2.setValue(value);

        this.segments.add(s1);
        this.segments.add(s2);
    }

    private void recalculate(double l) {
        Iterator<BOSegment> iterator = this.segments.iterator();

        while(iterator.hasNext()) {
            iterator.next().calculateValue(l);
        }
    }

    public ArrayList<BOPoint> getIntersections() {
        return this.points;
    }

    private class BOEventComparator implements Comparator<BOEvent> {
        @Override
        public int compare(BOEvent e1, BOEvent e2) {
            if (e1.getValue() > e2.getValue()) {
                return 1;
            }

            if (e1.getValue() < e2.getValue()) {
                return -1;
            }

            return 0;
        }
    }

    private class BOSegmentComparator implements Comparator<BOSegment> {
        @Override
        public int compare(BOSegment s1, BOSegment s2) {
            if (s1.getValue() < s2.getValue()) {
                return 1;
            }

            if (s1.getValue() > s2.getValue()) {
                return -1;
            }

            return 0;
        }
    }
    
}
