package com.chocho.bouncylegs.sensorProc;

import com.chocho.bouncylegs.Constants;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;

public class FeatureGenerator {
	// Mean, Max, Min, Magnitude, Energy, FFT, 

	final static String TAG = "FeatureGenerator";

	public FeatureGenerator() {
		;
	}	

	public static float calculateMean(float[] data){
		float sum = 0;
		for(float d : data){
			sum += d;
		}
		return sum / data.length;		
	}

	public static float calculateMean(DataInstance di){
		return calculateMean(di.getValues());
	}

	public static float calculateMax(float[] data){
		float max = data[0]; // Temporary value
		for(float d : data){
			max = (d >= max) ? d : max;
		}
		return max;		
	}

	public static float calculateMax(DataInstance di){
		return calculateMax(di.getValues());
	}

	public static float calculateMin(float[] data){
		float min = data[0]; // Temporary value
		for(float d : data){
			min = (d <= min) ? d : min;
		}
		return min;
	}

	public static float calculateMin(DataInstance di){
		return calculateMin(di.getValues());
	}

	public static float calculateVariance(float[] data){

		float mean = calculateMean(data);
		float sqDiffSum = 0;
		for(float d : data) {
		    sqDiffSum += (d - mean) * (d - mean);
        }

		return sqDiffSum / (data.length - 1);
	}

	public static float calculateVariance(DataInstance di){
		return calculateVariance(di.getValues());
	}

	public static HashMap<String, Float> processPressure(DataInstanceList dl) {

	    float[] pressureAgg = new float[dl.size()];

		double[] magAggregated = new double[dl.size()]; // Data for fft should be double type

		// Ready for calculation
		for(int i=0; i<dl.size(); i++){
			float pressure = ((DataInstance)dl.get(i)).getValues()[0];

			pressureAgg[i] = pressure;
			magAggregated[i] = Math.sqrt(pressure*pressure);
		}

		float pressure_mean = calculateMean(pressureAgg);
		float pressure_max = calculateMax(pressureAgg);
		float pressure_min = calculateMin(pressureAgg);
		float pressure_var = calculateVariance(pressureAgg);
		
		// Output variables
		HashMap<String, Float> featureMap = new HashMap<String, Float>();
		featureMap.put(Constants.HEADER_PRESSURE_MEAN, pressure_mean);
		featureMap.put(Constants.HEADER_PRESSURE_MAX, pressure_max);
		featureMap.put(Constants.HEADER_PRESSURE_MIN, pressure_min);
		featureMap.put(Constants.HEADER_PRESSURE_VARIANCE, pressure_var);

		return featureMap;

	}

	public static Instances createEmptyInstances(String[] headers, boolean isLabelRequired){

		FastVector attrs = new FastVector();

		for(String header : headers){
			attrs.addElement(new Attribute(header));
		}

		if(isLabelRequired){
			FastVector fv = new FastVector();
			for(String classLabel : Constants.CLASS_LABELS){
				fv.addElement(classLabel);
			}
			attrs.addElement(new Attribute(Constants.HEADER_CLASS_LABEL, (FastVector) fv));

		}

		String formattedDate = "";
		{
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(System.currentTimeMillis());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			formattedDate = sdf.format(cal.getTime());
		}

		Instances data = new Instances(formattedDate, attrs, 10000);

		return data;
	}
}
