package clusterDealer;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import clusterDealer.Allocation.Location;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;




public class Clustering{
	

    public enum ClusteringAlgorithms {
		KMEANS,
		FUZZYCMEANS,
		EM
	}
	/** Distance measures */
	public enum DistanceMetric {
		EUCLIDEAN,
		AVG_PER_SAMPLE
	}
	
	
	
			
		
	
	// ---- WEKA-related Utilities ----
		public static Instances convertMatrixToWeka(List<Location> locList, int numObs, int numFeatures) {
			// convert the data to WEKA format
			ArrayList<Attribute> atts = new ArrayList<Attribute>();
			for(int i = 0; i < numFeatures; i++) {
				atts.add(new Attribute("Feature" + i, i));
			}
			
			Instances ds = new Instances("AAF Data", atts, numObs);
			for(int i = 0; i < numObs; i++) {
				ds.add(new DenseInstance(numFeatures));
			}
			for(int i = 0; i < numFeatures; i++) {
				for(int j = 0; j < numObs; j++) {
					// to set lat and long attributes
					if(i==0)
						ds.instance(j).setValue(i, locList.get(j).latitude);
					else
						ds.instance(j).setValue(i, locList.get(j).longitude);
						
				}
			}
			return ds;
		}
		
	 

	 
	 
	 //clustering
	 
		
	 public static Cluster[] kmeansClusters(List<Allocation.Location> locList, int numObs, int numFeatures, int numClusters) throws Exception {
			Instances ds = convertMatrixToWeka(locList, numObs, numFeatures);
			Clustering cl = new Clustering();
			
			// uses Euclidean distance by default
			SimpleKMeans clusterer = new SimpleKMeans();
			clusterer.setMaxIterations(10);
			
			try {
				clusterer.setPreserveInstancesOrder(true);
				clusterer.setNumClusters(numClusters);
				clusterer.buildClusterer(ds);
				
				// cluster centers
				Instances centers = clusterer.getClusterCentroids();
				Cluster[] clusters = new Cluster[centers.numInstances()];
				for(int i = 0; i < centers.numInstances(); i++) {
					Instance inst = centers.instance(i);
					double[] mean = new double[inst.numAttributes()];
					for(int j = 0; j < mean.length; j++) {
						mean[j] = inst.value(j);
					}
					 clusters[i] = cl.new Cluster(mean, i);
				}
				
				// cluster members
				int[] assignments = clusterer.getAssignments();	
				
				for(int i = 0; i < assignments.length; i++) {
					clusters[assignments[i]].addMember(i);
				}
				return clusters;
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
				return null;
			}
			
		}
	 
	 
	 
	 



	public class Cluster implements Serializable {
		
		private static final long serialVersionUID = 1L;

		/** Cluster id, unique per group */
		private int id;
		
		/** Cluster centroid */
		private double[] centroid;
		
		/** Cluster standard deviation */
		private double[] stdDev = null;
		
		/** List of observations assigned to this cluster */
		private ArrayList<Integer> members;
		
		private boolean robust = false;
		
		public Cluster(double[] clusterCentroid, int clusterId) {
			centroid = clusterCentroid;
			members = new ArrayList<Integer>();
			id = clusterId;
		}
		
		public Cluster(double[] clusterCentroid, ArrayList<Integer> assignments, int clusterId) {
			centroid = clusterCentroid;
			members = assignments;
			id = clusterId;
			
		}
		
		
		/**
		 * Add a new observation to the cluster
		 * @param obsId - Id of the observation (index in the data matrix)
		 */
		public void addMember(int obsId) {
			members.add(new Integer(obsId));
		}
		
		/** 
		 * Computes the mean of all the members of the cluster
		 */
		public void recomputeCentroidAndStdDev(double[][] data, int numObs, int numFeatures) {
			double[] newCentroid = new double[numFeatures];
			for(int i = 0; i < members.size(); i++) {
				for(int j = 0; j < numFeatures; j++) {
					newCentroid[j] += data[members.get(i)][j];
				}
			}
			for(int j = 0; j < numFeatures; j++) {
				newCentroid[j] = newCentroid[j]/members.size();
			}
			centroid = newCentroid;
			
			double[] clusterStdDev = new double[numFeatures];
			for(int i = 0; i < members.size(); i++) {
				for(int j = 0; j < numFeatures; j++) {
					clusterStdDev[j] += Math.pow(data[members.get(i)][j] - centroid[j], 2);
				}
			}
			for(int j = 0; j < numFeatures; j++) {
				clusterStdDev[j] =  Math.sqrt(clusterStdDev[j]/members.size());
			}
			stdDev = clusterStdDev;
		}
		
		/**
		 * Returns the cluster centroid (mean) per sample
		 */
		public double[] getCentroid() {
			return centroid;
		}
		
		/**
		 * Returns the standard deviation per sample
		 * @requires setStdDev() method to have been called (currently implemented for EM only),
		 * will return null otherwise
		 */
		public double[] getStdDev() {
			return stdDev;
		}
		
		public void setStdDev(double[] dev) {
			stdDev = dev;
		}
		
		public ArrayList<Integer> getMembership() {
			return members;
		}
		
		public int getId() {
			return id;
		}
		
		public boolean isRobust() {
			return robust;
		}
		
		public void setRobust() {
			robust = true;
		}
		
		public String toString() {
			String c = "";
			c += "Size: " + members.size() + "\n";
			DecimalFormat df = new DecimalFormat("#.##");
			c += "VAF Mean: [";
			for(int i = 0; i < centroid.length; i++) {
				c += " " + df.format(centroid[i]) + " ";
			}
			c += "] \n";
			c += "       Stdev:";
			if(stdDev != null) {
				c += " [";
				for(int i = 0; i < stdDev.length; i++) {
					c += " " + df.format(stdDev[i]) + " ";
				}
				c += "]";
			}
			return c;
		}
		
		
	}


}



	