package edu.umich.cse.audioanalysis;

import java.util.LinkedList;
import java.util.Queue;

public class DataQueue {
	private Queue<Float> dataQueue;
	private double sum;
	private double squareSum;
	private int queueLen = -1;

	public static final int QUEUE_LEN_DEFAULT = 10000;
	
	public DataQueue(int queueLen) {
		dataQueue = new LinkedList<Float>();
		this.queueLen = queueLen;
		sum = 0;
		squareSum = 0;
	}
	
	public int getSize(){
		return dataQueue.size(); 
	}
	
	public void add(float newData){
		add((double) newData);
	}
	
	public void add(double newData){
		dataQueue.add((float)(newData));
		sum += newData;
		squareSum += newData*newData;
		
		// remove elements if necessary
		if(dataQueue.size()>queueLen) {
			remove();
		}
	}
	
	public void remove(){
		double dataToRemove = (Float) dataQueue.peek();
		sum -= dataToRemove;
		squareSum -= dataToRemove*dataToRemove;
		dataQueue.poll();
	}
	
	public double getMean(){
		if(dataQueue.size() > 0){
			return sum/((double) dataQueue.size());
		} else {
			return 0;
		}
	}
	
	public double getStd(){
		if(dataQueue.size() > 0){
			double mean = getMean();
			return Math.sqrt(squareSum / ((double) dataQueue.size()) - mean * mean);
		} else {
			return 0;
		}
	}
}
